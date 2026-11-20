package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinNativeImage;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Creates the game textures of the pixel toolkit and releases the ones nobody draws anymore.<br>
 * Together with {@link PixelGraphics}, this is the only class of the toolkit that calls version-specific texture and rendering code,
 * so porting the toolkit to another Minecraft version only means porting these two classes.<br><br>
 *
 * Every few seconds (on the client tick), textures that were not drawn since the last {@value #IDLE_RUNS_BEFORE_RELEASE} checks get released.
 * That keeps textures of overlays that got thrown away without closing them (for example after a layout reload) from staying in memory.
 * Owners notice it through {@link PixelTexture#isClosed()} and create a new texture when they draw again.<br><br>
 *
 * The game draws GUI quads one by one here, so the quads of the toolkit go into a batch instead ({@link QuadBatch}) and every run of
 * quads with the same texture and tint becomes one draw call. {@link PixelGraphics} draws the batch at the end of every draw method.<br>
 * Everything here runs on the render thread.
 */
public final class PixelTextures {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final PixelTexture.Factory FACTORY = PixelTextures::create;
    static final long MAINTENANCE_INTERVAL_NANOS = 2_000_000_000L;
    /** 10 checks of 2 seconds: a texture gets released after it was not drawn for about 20 seconds. */
    static final int IDLE_RUNS_BEFORE_RELEASE = 10;

    private static final List<GameTexture> TEXTURES = new ArrayList<>();
    private static final QuadBatch BATCH = new QuadBatch();
    private static long nextTextureId = 0L;
    private static boolean maintenanceRegistered = false;
    private static long lastMaintenanceNanos = 0L;

    private PixelTextures() {
    }

    /**
     * Creates a transparent texture of the given size.
     *
     * @param name a short name for debugging, only {@code [a-z0-9_]} are kept
     */
    @NotNull
    public static PixelTexture create(@NotNull String name, int width, int height) {
        if ((width <= 0) || (height <= 0)) throw new IllegalArgumentException("Pixel textures need a size of at least 1x1, got " + width + "x" + height);
        registerMaintenanceIfNeeded();
        String path = "pixel_overlay/" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_") + "_" + (nextTextureId++);
        ResourceLocation location = new ResourceLocation("fancymenu", path);
        DynamicTexture texture = new DynamicTexture(width, height, true);
        // A new GPU texture holds undefined memory until its first upload. Every upload also sets nearest filtering, so art pixels stay sharp.
        texture.upload();
        Minecraft.getInstance().getTextureManager().register(location, texture);
        GameTexture gameTexture = new GameTexture(location, texture, width, height);
        TEXTURES.add(gameTexture);
        return gameTexture;
    }

    /**
     * Returns how many toolkit textures are alive right now. For checks and debugging.
     */
    public static int getLiveTextureCount() {
        return TEXTURES.size();
    }

    /**
     * Adds a filled rectangle to the batch. For {@link PixelGraphics}.
     */
    static void fill(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int argb) {
        BATCH.fill(graphics.pose().last().pose(), x, y, x + width, y + height, argb);
    }

    /**
     * Draws the quads that wait in the batch. For {@link PixelGraphics}, which calls it before anything else draws.
     */
    static void flush() {
        BATCH.flush();
    }

    private static void registerMaintenanceIfNeeded() {
        if (maintenanceRegistered) return;
        maintenanceRegistered = true;
        lastMaintenanceNanos = System.nanoTime();
        EventHandler.INSTANCE.registerListener(event -> onClientTick(), ClientTickEvent.Post.class);
    }

    private static void onClientTick() {
        long now = System.nanoTime();
        if ((now - lastMaintenanceNanos) < MAINTENANCE_INTERVAL_NANOS) return;
        lastMaintenanceNanos = now;
        PixelAtlas.get().maintain();
        releaseIdleTextures();
    }

    private static void releaseIdleTextures() {
        for (int i = TEXTURES.size() - 1; i >= 0; i--) {
            GameTexture texture = TEXTURES.get(i);
            if (texture.closed) {
                TEXTURES.remove(i);
                continue;
            }
            if (texture.used) {
                texture.used = false;
                texture.idleRuns = 0;
            } else if (++texture.idleRuns >= IDLE_RUNS_BEFORE_RELEASE) {
                texture.close();
                TEXTURES.remove(i);
            }
        }
    }

    private static int argbToAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb >>> 16) & 0xFF) | ((argb & 0xFF) << 16);
    }

    private static final class GameTexture implements PixelTexture {

        private final ResourceLocation location;
        private final DynamicTexture texture;
        private final int width;
        private final int height;
        private boolean closed = false;
        /** Set by every draw, cleared by the maintenance */
        private boolean used = true;
        private int idleRuns = 0;

        private GameTexture(@NotNull ResourceLocation location, @NotNull DynamicTexture texture, int width, int height) {
            this.location = location;
            this.texture = texture;
            this.width = width;
            this.height = height;
        }

        @Override
        public int width() {
            return this.width;
        }

        @Override
        public int height() {
            return this.height;
        }

        @Override
        public void write(@NotNull int[] source, int sourceOffset, int sourceStride, int x, int y, int width, int height) {
            if (this.closed) return;
            // These are raw native memory writes, so a block outside of the image would corrupt memory instead of throwing
            if ((x < 0) || (y < 0) || (width < 0) || (height < 0) || (x + width > this.width) || (y + height > this.height)) {
                throw new IllegalArgumentException("Block " + x + "," + y + " " + width + "x" + height + " is outside of the " + this.width + "x" + this.height + " texture");
            }
            if ((sourceOffset < 0) || ((height > 0) && (sourceOffset + (long)(height - 1) * sourceStride + width > source.length))) {
                throw new IllegalArgumentException("The source array is too small for a " + width + "x" + height + " block");
            }
            NativeImage image = Objects.requireNonNull(this.texture.getPixels());
            // NativeImage stores ABGR. The pointer is only valid while the image is open, which the closed check above guarantees.
            long pointer = ((IMixinNativeImage)(Object)image).get_pixels_FancyMenu();
            for (int row = 0; row < height; row++) {
                int sourceIndex = sourceOffset + row * sourceStride;
                long target = pointer + (((long)(y + row) * this.width + x) << 2);
                for (int column = 0; column < width; column++) {
                    MemoryUtil.memPutInt(target + ((long)column << 2), argbToAbgr(source[sourceIndex + column]));
                }
            }
        }

        @Override
        public void upload(int firstRow, int endRow) {
            if (this.closed) return;
            firstRow = Math.max(0, firstRow);
            endRow = Math.min(this.height, endRow);
            if (endRow <= firstRow) return;
            // Quads of this texture that wait in the batch have to show the pixels they were drawn with
            if (BATCH.holds(this)) BATCH.flush();
            if ((firstRow == 0) && (endRow == this.height)) {
                this.texture.upload();
                return;
            }
            // Whole rows are one contiguous block of the image, so they upload straight from it
            this.texture.bind();
            Objects.requireNonNull(this.texture.getPixels()).upload(0, 0, firstRow, 0, firstRow, this.width, endRow - firstRow, false, false, false, false);
        }

        @Override
        public void draw(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, boolean flipX, boolean flipY, int color) {
            if (this.closed) return;
            this.used = true;
            float u0 = (float)u / this.width;
            float u1 = (float)(u + regionWidth) / this.width;
            float v0 = (float)v / this.height;
            float v1 = (float)(v + regionHeight) / this.height;
            // Swapped UVs run the region backwards, which mirrors it without another texture
            BATCH.textured(graphics.pose().last().pose(), this, x, y, x + width, y + height, flipX ? u1 : u0, flipY ? v1 : v0, flipX ? u0 : u1, flipY ? v0 : v1, color);
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }

        @Override
        public void close() {
            if (this.closed) return;
            if (BATCH.holds(this)) BATCH.flush();
            this.closed = true;
            try {
                Minecraft.getInstance().getTextureManager().release(this.location);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to release pixel overlay texture: {}", this.location, ex);
            }
        }

    }

    /**
     * Collects the quads of the toolkit in their order and draws every run of quads of the same kind (filled or textured), texture and tint
     * with one draw call. The game's GUI rendering of this version would draw every quad with a draw call of its own.<br><br>
     *
     * Textured quads use the game's position-texture shader with the tint as the shader color, so only fully transparent texture pixels
     * get discarded (the position-texture-color shader of this version would also drop faint pixels below 10% alpha).
     * Both kinds blend like the game's GUI rects and leave the depth test as it is, like the game's GUI blits.
     * The batch has its own buffer, so a failed draw never leaves the game's shared buffer in a broken state.
     */
    private static final class QuadBatch {

        private static final int NONE = 0;
        private static final int FILLED = 1;
        private static final int TEXTURED = 2;
        /** About 24 KB (the builder reserves 6 bytes per unit), room for 300 textured quads. The buffer grows when a batch needs more. */
        private static final int INITIAL_CAPACITY = 4096;

        @Nullable
        private BufferBuilder builder = null;
        private int kind = NONE;
        @Nullable
        private GameTexture texture = null;
        private int tint = -1;

        private void fill(@NotNull Matrix4f pose, float x0, float y0, float x1, float y1, int argb) {
            if (this.kind != FILLED) this.begin(FILLED, null, -1);
            BufferBuilder buffer = Objects.requireNonNull(this.builder);
            int alpha = argb >>> 24;
            int red = (argb >> 16) & 0xFF;
            int green = (argb >> 8) & 0xFF;
            int blue = argb & 0xFF;
            buffer.vertex(pose, x0, y0, 0.0F).color(red, green, blue, alpha).endVertex();
            buffer.vertex(pose, x0, y1, 0.0F).color(red, green, blue, alpha).endVertex();
            buffer.vertex(pose, x1, y1, 0.0F).color(red, green, blue, alpha).endVertex();
            buffer.vertex(pose, x1, y0, 0.0F).color(red, green, blue, alpha).endVertex();
        }

        private void textured(@NotNull Matrix4f pose, @NotNull GameTexture texture, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int tint) {
            if ((this.kind != TEXTURED) || (this.texture != texture) || (this.tint != tint)) this.begin(TEXTURED, texture, tint);
            BufferBuilder buffer = Objects.requireNonNull(this.builder);
            // Same corner order as GuiGraphics#blit
            buffer.vertex(pose, x0, y0, 0.0F).uv(u0, v0).endVertex();
            buffer.vertex(pose, x0, y1, 0.0F).uv(u0, v1).endVertex();
            buffer.vertex(pose, x1, y1, 0.0F).uv(u1, v1).endVertex();
            buffer.vertex(pose, x1, y0, 0.0F).uv(u1, v0).endVertex();
        }

        private boolean holds(@NotNull GameTexture texture) {
            return (this.kind == TEXTURED) && (this.texture == texture);
        }

        private void begin(int kind, @Nullable GameTexture texture, int tint) {
            this.flush();
            if (this.builder == null) this.builder = new BufferBuilder(INITIAL_CAPACITY);
            this.builder.begin(VertexFormat.Mode.QUADS, (kind == FILLED) ? DefaultVertexFormat.POSITION_COLOR : DefaultVertexFormat.POSITION_TEX);
            this.kind = kind;
            this.texture = texture;
            this.tint = tint;
        }

        private void flush() {
            if (this.kind == NONE) return;
            int kind = this.kind;
            GameTexture texture = this.texture;
            int tint = this.tint;
            this.kind = NONE;
            this.texture = null;
            BufferBuilder.RenderedBuffer quads = Objects.requireNonNull(this.builder).end();
            float[] shaderColor = RenderSystem.getShaderColor();
            float previousRed = shaderColor[0];
            float previousGreen = shaderColor[1];
            float previousBlue = shaderColor[2];
            float previousAlpha = shaderColor[3];
            try {
                if (kind == FILLED) {
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                } else {
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderTexture(0, Objects.requireNonNull(texture).texture.getId());
                    RenderSystem.setShaderColor(((tint >> 16) & 0xFF) / 255.0F, ((tint >> 8) & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F, (tint >>> 24) / 255.0F);
                }
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                BufferUploader.drawWithShader(quads);
            } finally {
                RenderSystem.setShaderColor(previousRed, previousGreen, previousBlue, previousAlpha);
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
        }

    }

}
