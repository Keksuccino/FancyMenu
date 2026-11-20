package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinNativeImage;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Creates the game textures of the pixel toolkit, draws its quads and releases the textures nobody draws anymore.<br>
 * Together with {@link PixelGraphics}, this is the only class of the toolkit that calls version-specific texture and rendering code,
 * so porting the toolkit to another Minecraft version only means porting these two classes.<br><br>
 *
 * Every few seconds (on the client tick), textures that were not drawn since the last {@value #IDLE_RUNS_BEFORE_RELEASE} checks get released.
 * That keeps textures of overlays that got thrown away without closing them (for example after a layout reload) from staying in memory.
 * Owners notice it through {@link PixelTexture#isClosed()} and create a new texture when they draw again.<br><br>
 *
 * GUI rendering of this Minecraft version draws every call right away, so the toolkit collects its quads in a batch instead:
 * consecutive quads of the same kind (filled, or textured with the same texture) become one draw call, and a quad of another kind
 * draws the batch first, so the drawing order stays as it is. {@link PixelGraphics#end()} draws what is left at the end of every part.
 * The textures don't get registered in the texture manager, whose release of this version keeps the entry and its image in memory.
 * The batch binds them by their GL id instead, and closing a texture frees its image and GL texture.<br>
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
        String debugName = "pixel_overlay/" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_") + "_" + (nextTextureId++);
        // The image starts zeroed (transparent), and the upload also sets the nearest-neighbor filter
        DynamicTexture texture = new DynamicTexture(width, height, true);
        // A new GPU texture holds undefined memory until its first upload
        texture.upload();
        GameTexture gameTexture = new GameTexture(debugName, texture, width, height);
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
     * Adds a filled quad to the batch, see the class description.
     */
    static void drawRect(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int argb) {
        BATCH.rect(graphics, x, y, width, height, argb);
    }

    /**
     * Draws the quads waiting in the batch. Does nothing if there are none.
     */
    static void flushBatch() {
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

    private static final class GameTexture implements PixelTexture {

        @NotNull
        private final String name;
        @NotNull
        private final DynamicTexture texture;
        private final int width;
        private final int height;
        private boolean closed = false;
        /** Set by every draw, cleared by the maintenance */
        private boolean used = true;
        private int idleRuns = 0;

        private GameTexture(@NotNull String name, @NotNull DynamicTexture texture, int width, int height) {
            this.name = name;
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
            NativeImage image = this.image();
            // NativeImage stores ABGR. The pointer is only valid while the image is open, which the closed check above guarantees.
            long pointer = ((IMixinNativeImage)(Object)image).get_pixels_FancyMenu();
            for (int row = 0; row < height; row++) {
                int sourceIndex = sourceOffset + row * sourceStride;
                long target = pointer + (((long)(y + row) * this.width + x) << 2);
                for (int column = 0; column < width; column++) {
                    int argb = source[sourceIndex + column];
                    MemoryUtil.memPutInt(target + ((long)column << 2), (argb & 0xFF00FF00) | ((argb >>> 16) & 0xFF) | ((argb & 0xFF) << 16));
                }
            }
        }

        @Override
        public void upload(int firstRow, int endRow) {
            if (this.closed) return;
            firstRow = Math.max(0, firstRow);
            endRow = Math.min(this.height, endRow);
            if (endRow <= firstRow) return;
            if ((firstRow == 0) && (endRow == this.height)) {
                this.texture.upload();
                return;
            }
            // Whole rows are one contiguous block of the image, so they upload without a copy. No blur keeps the nearest-neighbor filter.
            this.texture.bind();
            this.image().upload(0, 0, firstRow, 0, firstRow, this.width, endRow - firstRow, false, false, false, false);
        }

        @Override
        public void draw(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, boolean flipX, boolean flipY, int color) {
            if (this.closed) return;
            this.used = true;
            float u0 = (float)u / this.width;
            float u1 = (float)(u + regionWidth) / this.width;
            float v0 = (float)v / this.height;
            float v1 = (float)(v + regionHeight) / this.height;
            // Swapped texture coordinates mirror the region without another texture
            BATCH.texturedQuad(graphics, this.texture.getId(), x, y, x + width, y + height, flipX ? u1 : u0, flipY ? v1 : v0, flipX ? u0 : u1, flipY ? v0 : v1, color);
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }

        @Override
        public void close() {
            if (this.closed) return;
            // The batch may still hold quads of this texture, and they have to get drawn before its GL id goes away
            BATCH.flush();
            this.closed = true;
            try {
                this.texture.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to release pixel overlay texture: {}", this.name, ex);
            }
        }

        @NotNull
        private NativeImage image() {
            NativeImage image = this.texture.getPixels();
            if (image == null) throw new IllegalStateException("The image of pixel overlay texture " + this.name + " is gone");
            return image;
        }

    }

    /**
     * Collects consecutive quads of one kind and draws them with one draw call, see the class description.
     */
    private static final class QuadBatch {

        private static final int NONE = 0;
        private static final int FILLED = 1;
        private static final int TEXTURED = 2;
        /** Initial vertex buffer size in units of 6 bytes (see BufferBuilder), enough for about 1000 textured quads. It grows on its own. */
        private static final int INITIAL_CAPACITY = 16384;

        @Nullable
        private BufferBuilder builder = null;
        @Nullable
        private GuiGraphics graphics = null;
        private int kind = NONE;
        private int textureId = -1;

        private void rect(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int argb) {
            BufferBuilder buffer = this.begin(graphics, FILLED, -1);
            Matrix4f pose = graphics.pose().last().pose();
            int a = argb >>> 24;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            float x0 = x;
            float y0 = y;
            float x1 = x + width;
            float y1 = y + height;
            buffer.vertex(pose, x0, y0, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(pose, x0, y1, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(pose, x1, y1, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(pose, x1, y0, 0.0F).color(r, g, b, a).endVertex();
        }

        private void texturedQuad(@NotNull GuiGraphics graphics, int textureId, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int argb) {
            BufferBuilder buffer = this.begin(graphics, TEXTURED, textureId);
            Matrix4f pose = graphics.pose().last().pose();
            int a = argb >>> 24;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            buffer.vertex(pose, x0, y0, 0.0F).uv(u0, v0).color(r, g, b, a).endVertex();
            buffer.vertex(pose, x0, y1, 0.0F).uv(u0, v1).color(r, g, b, a).endVertex();
            buffer.vertex(pose, x1, y1, 0.0F).uv(u1, v1).color(r, g, b, a).endVertex();
            buffer.vertex(pose, x1, y0, 0.0F).uv(u1, v0).color(r, g, b, a).endVertex();
        }

        /**
         * Returns the buffer for a quad of the given kind, drawing the batch first if it holds quads of another kind.
         */
        @NotNull
        private BufferBuilder begin(@NotNull GuiGraphics graphics, int kind, int textureId) {
            if ((this.kind != kind) || (this.textureId != textureId) || (this.graphics != graphics)) this.flush();
            BufferBuilder buffer = this.builder;
            if (buffer == null) {
                buffer = new BufferBuilder(INITIAL_CAPACITY);
                this.builder = buffer;
            }
            if (this.kind == NONE) {
                buffer.begin(VertexFormat.Mode.QUADS, (kind == TEXTURED) ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR);
                this.kind = kind;
                this.textureId = textureId;
                this.graphics = graphics;
            }
            return buffer;
        }

        private void flush() {
            if (this.kind == NONE) return;
            BufferBuilder buffer = this.builder;
            GuiGraphics batchGraphics = this.graphics;
            int batchKind = this.kind;
            int batchTexture = this.textureId;
            this.kind = NONE;
            this.textureId = -1;
            this.graphics = null;
            if ((buffer == null) || (batchGraphics == null)) return;
            BufferBuilder.RenderedBuffer rendered = buffer.end();
            // Things drawn before the batch that still wait in the buffer source belong behind it
            batchGraphics.flush();
            // The quads carry their own colors, so a shader color left behind by other GUI code must not tint them
            float[] shaderColor = RenderSystem.getShaderColor();
            float red = shaderColor[0];
            float green = shaderColor[1];
            float blue = shaderColor[2];
            float alpha = shaderColor[3];
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderingUtils.setupAlphaBlend();
            if (batchKind == TEXTURED) {
                RenderSystem.setShaderTexture(0, batchTexture);
                // Unlike the vanilla texture shader, it keeps translucent pixels below 10% alpha, like fog and light rays
                RenderSystem.setShader(RenderingUtils::getAlphaTextureShader);
            } else {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
            }
            BufferUploader.drawWithShader(rendered);
            RenderSystem.setShaderColor(red, green, blue, alpha);
        }

    }

}
