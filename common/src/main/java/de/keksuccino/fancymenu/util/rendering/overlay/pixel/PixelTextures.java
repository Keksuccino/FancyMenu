package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinNativeImage;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
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
 * Owners notice it through {@link PixelTexture#isClosed()} and create a new texture when they draw again.<br>
 * Everything here runs on the render thread.
 */
public final class PixelTextures {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final PixelTexture.Factory FACTORY = PixelTextures::create;
    static final long MAINTENANCE_INTERVAL_NANOS = 2_000_000_000L;
    /** 10 checks of 2 seconds: a texture gets released after it was not drawn for about 20 seconds. */
    static final int IDLE_RUNS_BEFORE_RELEASE = 10;

    private static final List<GameTexture> TEXTURES = new ArrayList<>();
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
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("fancymenu", path);
        DynamicTexture texture = new DynamicTexture(width, height, true);
        // A new GPU texture holds undefined memory until its first upload. Uploads also set the texture to nearest filtering.
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

    /**
     * The render type of one texture: the same blending and depth test as the game's GUI rects ({@code RenderType.gui()}),
     * with the texture and the position-texture-color shader. Each texture has its own, so the quads of one texture form one batch.
     */
    @NotNull
    private static RenderType createRenderType(int textureId) {
        return new RenderType("fancymenu_pixel_overlay", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, false, () -> {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, textureId);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }, () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }) {};
    }

    private static final class GameTexture implements PixelTexture {

        private final ResourceLocation location;
        private final DynamicTexture texture;
        private final RenderType renderType;
        private final int width;
        private final int height;
        private boolean closed = false;
        /** Set by every draw, cleared by the maintenance */
        private boolean used = true;
        private int idleRuns = 0;

        private GameTexture(@NotNull ResourceLocation location, @NotNull DynamicTexture texture, int width, int height) {
            this.location = location;
            this.texture = texture;
            this.renderType = createRenderType(texture.getId());
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
                    MemoryUtil.memPutInt(target + ((long)column << 2), FastColor.ABGR32.fromArgb32(source[sourceIndex + column]));
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
            // Whole rows are one contiguous block of the image, so they upload without a copy (skipping the rows above them)
            this.texture.bind();
            Objects.requireNonNull(this.texture.getPixels()).upload(0, 0, firstRow, 0, firstRow, this.width, endRow - firstRow, false, false);
        }

        @Override
        public void draw(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, boolean flipX, boolean flipY, int color) {
            if (this.closed) return;
            this.used = true;
            float u0 = (float)u / this.width;
            float u1 = (float)(u + regionWidth) / this.width;
            float v0 = (float)v / this.height;
            float v1 = (float)(v + regionHeight) / this.height;
            // Swapped UVs mirror the region without another texture
            if (flipX) {
                float swap = u0;
                u0 = u1;
                u1 = swap;
            }
            if (flipY) {
                float swap = v0;
                v0 = v1;
                v1 = swap;
            }
            Matrix4f pose = graphics.pose().last().pose();
            float right = x + width;
            float bottom = y + height;
            // Same corner order as GuiGraphics#blit. The quad waits in the batch of this texture until another texture draws or the pass part ends.
            VertexConsumer buffer = graphics.bufferSource().getBuffer(this.renderType);
            buffer.addVertex(pose, x, y, 0.0F).setUv(u0, v0).setColor(color);
            buffer.addVertex(pose, x, bottom, 0.0F).setUv(u0, v1).setColor(color);
            buffer.addVertex(pose, right, bottom, 0.0F).setUv(u1, v1).setColor(color);
            buffer.addVertex(pose, right, y, 0.0F).setUv(u1, v0).setColor(color);
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }

        @Override
        public void close() {
            if (this.closed) return;
            this.closed = true;
            try {
                Minecraft.getInstance().getTextureManager().release(this.location);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to release pixel overlay texture: {}", this.location, ex);
            }
        }

    }

}
