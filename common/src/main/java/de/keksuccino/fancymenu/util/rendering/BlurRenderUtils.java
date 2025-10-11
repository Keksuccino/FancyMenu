package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.io.IOException;

/**
 * Utility class to render blurred GUI regions that honour the current GUI scale.
 * The blur renders as a layer: capture the already rendered background, draw the blurred area(s),
 * then continue rendering normal content above the blur.
 */
public final class BlurRenderUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SHADER_LOCATION = "fancymenu/gui_blur_layer";
    private static final int DEFAULT_TINT_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);

    private static TextureTarget captureTarget;
    private static ShaderInstance blurShader;
    private static boolean shaderLoadQueued = false;
    private static boolean shaderFailed = false;

    private static BlurLayer activeLayer;

    private BlurRenderUtils() {
    }

    public static void init() {
        MinecraftResourceReloadObserver.addReloadListener(reloadAction -> {
            if (reloadAction == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                releaseShader();
            } else {
                queueShaderReload();
            }
        });
    }

    /**
     * Begins a blur layer. Capture the current GUI framebuffer so subsequent blur calls operate on an immutable background.
     * Remember to close the returned {@link BlurLayer} (try-with-resources recommended) after drawing all blur regions for the frame.
     */
    @NotNull
    public static BlurLayer beginLayer(@NotNull GuiGraphics graphics) {
        if (activeLayer != null) {
            activeLayer.close();
        }
        BlurLayer layer = new BlurLayer(graphics);
        activeLayer = layer;
        return layer;
    }

    /**
     * Renders a blurred rectangle using default tint, opacity, and corner settings.
     *
     * @param graphics    GUI rendering context the blur should draw into.
     * @param x           Left edge in GUI coordinates.
     * @param y           Top edge in GUI coordinates.
     * @param width       Width of the blur area in GUI units.
     * @param height      Height of the blur area in GUI units.
     * @param blurRadius  Blur radius in GUI units (0 disables blur; 4-12 provides a soft background blur without heavy cost).
     */
    public static void renderBlurArea(@NotNull GuiGraphics graphics, float x, float y, float width, float height, float blurRadius) {
        try (BlurLayer layer = beginLayer(graphics)) {
            layer.renderBlurArea(x, y, width, height, blurRadius);
        }
    }

    /**
     * Renders a blurred rectangle and applies rounded corners without tinting.
     *
     * @param graphics     GUI rendering context the blur should draw into.
     * @param x            Left edge in GUI coordinates.
     * @param y            Top edge in GUI coordinates.
     * @param width        Width of the blur area in GUI units.
     * @param height       Height of the blur area in GUI units.
     * @param blurRadius   Blur radius in GUI units (0 disables blur; 4-12 is a good baseline).
     * @param cornerRadius Corner radius in GUI units (0 disables rounding).
     */
    public static void renderBlurArea(@NotNull GuiGraphics graphics,
                                      float x, float y, float width, float height,
                                      float blurRadius,
                                      float cornerRadius) {
        try (BlurLayer layer = beginLayer(graphics)) {
            layer.renderBlurArea(x, y, width, height, blurRadius, cornerRadius);
        }
    }

    /**
     * Renders a blurred rectangle and mixes in a tint while keeping square corners.
     *
     * @param graphics     GUI rendering context the blur should draw into.
     * @param x            Left edge in GUI coordinates.
     * @param y            Top edge in GUI coordinates.
     * @param width        Width of the blur area in GUI units.
     * @param height       Height of the blur area in GUI units.
     * @param blurRadius   Blur radius in GUI units (0 disables blur; increase above 12 for stronger diffusion at higher GPU cost).
     * @param tintColor    ARGB tint colour applied to the blurred pixels.
     * @param tintStrength Tint mix factor between 0 (no tint) and 1 (full tint).
     */
    public static void renderBlurArea(@NotNull GuiGraphics graphics,
                                      float x, float y, float width, float height,
                                      float blurRadius,
                                      int tintColor,
                                      float tintStrength) {
        try (BlurLayer layer = beginLayer(graphics)) {
            layer.renderBlurArea(x, y, width, height, blurRadius, tintColor, tintStrength, 1.0F, 0.0F);
        }
    }

    /**
     * Renders a blurred rectangle with full control over tint, opacity, and rounded corners.
     *
     * @param graphics     GUI rendering context the blur should draw into.
     * @param x            Left edge in GUI coordinates.
     * @param y            Top edge in GUI coordinates.
     * @param width        Width of the blur area in GUI units.
     * @param height       Height of the blur area in GUI units.
     * @param blurRadius   Blur radius in GUI units (0 disables blur; 4-12 balances clarity and smoothness).
     * @param tintColor    ARGB tint colour applied to the blurred pixels.
     * @param tintStrength Tint mix factor between 0 (no tint) and 1 (full tint).
     * @param opacity      Alpha multiplier between 0 (transparent) and 1 (opaque).
     * @param cornerRadius Corner radius in GUI units (0 disables rounding).
     */
    public static void renderBlurArea(@NotNull GuiGraphics graphics,
                                      float x, float y, float width, float height,
                                      float blurRadius,
                                      int tintColor,
                                      float tintStrength,
                                      float opacity,
                                      float cornerRadius) {
        try (BlurLayer layer = beginLayer(graphics)) {
            layer.renderBlurArea(x, y, width, height, blurRadius, tintColor, tintStrength, opacity, cornerRadius);
        }
    }

    private static boolean ensureShaderReady() {
        if (blurShader != null) {
            return true;
        }
        if (shaderFailed) {
            return false;
        }
        if (!shaderLoadQueued) {
            queueShaderReload();
        }
        return false;
    }

    private static void queueShaderReload() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return;
        }
        if (shaderLoadQueued) {
            return;
        }
        shaderLoadQueued = true;
        RenderSystem.recordRenderCall(() -> {
            unloadShader();
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getResourceManager() == null) {
                shaderFailed = true;
                shaderLoadQueued = false;
                return;
            }
            try {
                shaderFailed = false;
                blurShader = new ShaderInstance(mc.getResourceManager(), SHADER_LOCATION, DefaultVertexFormat.POSITION_TEX);
            } catch (IOException e) {
                shaderFailed = true;
                LOGGER.error("[FANCYMENU] Failed to load FancyMenu GUI blur shader.", e);
            } finally {
                shaderLoadQueued = false;
            }
        });
    }

    private static void releaseShader() {
        if (blurShader == null && !shaderLoadQueued) {
            return;
        }
        RenderSystem.recordRenderCall(() -> {
            unloadShader();
            destroyCaptureTarget();
            shaderFailed = false;
            shaderLoadQueued = false;
        });
    }

    private static void unloadShader() {
        if (blurShader != null) {
            blurShader.close();
            blurShader = null;
        }
    }

    private static void ensureCaptureTarget(@NotNull RenderTarget mainTarget) {
        if (captureTarget != null && captureTarget.width == mainTarget.width && captureTarget.height == mainTarget.height) {
            return;
        }
        destroyCaptureTarget();
        captureTarget = new TextureTarget(mainTarget.width, mainTarget.height, false, Minecraft.ON_OSX);
        captureTarget.setFilterMode(9729); // GL_LINEAR
    }

    private static void destroyCaptureTarget() {
        if (captureTarget != null) {
            captureTarget.destroyBuffers();
            captureTarget = null;
        }
    }

    private static void captureBackground(@NotNull GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        graphics.flush();
        RenderSystem.assertOnRenderThread();
        ensureCaptureTarget(mainTarget);

        GlStateManager._glBindFramebuffer(36008, mainTarget.frameBufferId); // GL_READ_FRAMEBUFFER
        GlStateManager._glBindFramebuffer(36009, captureTarget.frameBufferId); // GL_DRAW_FRAMEBUFFER
        GlStateManager._glBlitFrameBuffer(
                0, 0, mainTarget.width, mainTarget.height,
                0, 0, captureTarget.width, captureTarget.height,
                16384, // GL_COLOR_BUFFER_BIT
                9729   // GL_LINEAR
        );
        GlStateManager._glBindFramebuffer(36160, 0); // GL_FRAMEBUFFER

        mainTarget.bindWrite(false);
        RenderSystem.viewport(0, 0, mainTarget.viewWidth, mainTarget.viewHeight);
    }

    private static void renderBlurAreaInternal(@NotNull GuiGraphics graphics,
                                               float x, float y, float width, float height,
                                               float blurRadius,
                                               int tintColor,
                                               float tintStrength,
                                               float opacity,
                                               float cornerRadius) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        graphics.flush();

        if (!ensureShaderReady() || blurShader == null || captureTarget == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        double guiScale = minecraft.getWindow().getGuiScale();
        if (guiScale <= 0.0D) {
            return;
        }
        float windowWidth = minecraft.getWindow().getWidth();
        float windowHeight = minecraft.getWindow().getHeight();
        if (windowWidth <= 0.0F || windowHeight <= 0.0F) {
            return;
        }

        float pixelX = (float)(x * guiScale);
        float pixelY = (float)(y * guiScale);
        float pixelWidth = (float)(width * guiScale);
        float pixelHeight = (float)(height * guiScale);

        float u0 = pixelX / windowWidth;
        float v0 = pixelY / windowHeight;
        float u1 = (pixelX + pixelWidth) / windowWidth;
        float v1 = (pixelY + pixelHeight) / windowHeight;

        ShaderInstance shader = blurShader;
        Matrix4f pose = graphics.pose().last().pose();
        shader.setSampler("DiffuseSampler", captureTarget);
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(pose);
        }
        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        }

        shader.safeGetUniform("ScreenSize").set(windowWidth, windowHeight);
        shader.safeGetUniform("Rect").set(u0, v0, u1 - u0, v1 - v0);
        shader.safeGetUniform("BlurRadius").set(Math.max(0.0F, blurRadius) * (float) guiScale);

        float r = FastColor.ARGB32.red(tintColor) / 255.0F;
        float g = FastColor.ARGB32.green(tintColor) / 255.0F;
        float b = FastColor.ARGB32.blue(tintColor) / 255.0F;

        shader.safeGetUniform("TintColor").set(r, g, b);
        shader.safeGetUniform("TintStrength").set(Mth.clamp(tintStrength, 0.0F, 1.0F));
        shader.safeGetUniform("Opacity").set(Mth.clamp(opacity, 0.0F, 1.0F));
        shader.safeGetUniform("CornerRadius").set(Math.max(0.0F, cornerRadius) * (float) guiScale);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(pose, x, y, 0.0F).setUv(u0, v0);
        builder.addVertex(pose, x, y + height, 0.0F).setUv(u0, v1);
        builder.addVertex(pose, x + width, y + height, 0.0F).setUv(u1, v1);
        builder.addVertex(pose, x + width, y, 0.0F).setUv(u1, v0);

        BufferUploader.drawWithShader(builder.buildOrThrow());

    }

    public static final class BlurLayer implements AutoCloseable {

        private final GuiGraphics graphics;
        private boolean closed = false;

        private BlurLayer(@NotNull GuiGraphics graphics) {
            this.graphics = graphics;
            captureBackground(graphics);
        }

        /**
         * Renders a blur within this layer using default tint, opacity, and corner settings.
         *
         * @param x          Left edge in GUI coordinates.
         * @param y          Top edge in GUI coordinates.
         * @param width      Width of the blur area in GUI units.
         * @param height     Height of the blur area in GUI units.
         * @param blurRadius Blur radius in GUI units (0 disables blur; 4-12 provides a soft background blur).
         */
        public void renderBlurArea(float x, float y, float width, float height, float blurRadius) {
            this.renderBlurArea(x, y, width, height, blurRadius, DEFAULT_TINT_COLOR, 0.0F, 1.0F, 0.0F);
        }

        /**
         * Renders a blur within this layer using rounded corners and default tint settings.
         *
         * @param x            Left edge in GUI coordinates.
         * @param y            Top edge in GUI coordinates.
         * @param width        Width of the blur area in GUI units.
         * @param height       Height of the blur area in GUI units.
         * @param blurRadius   Blur radius in GUI units (0 disables blur; 4-12 is a good baseline).
         * @param cornerRadius Corner radius in GUI units (0 disables rounding).
         */
        public void renderBlurArea(float x, float y, float width, float height, float blurRadius, float cornerRadius) {
            this.renderBlurArea(x, y, width, height, blurRadius, DEFAULT_TINT_COLOR, 0.0F, 1.0F, cornerRadius);
        }

        /**
         * Renders a tinted blur within this layer while keeping square corners.
         *
         * @param x            Left edge in GUI coordinates.
         * @param y            Top edge in GUI coordinates.
         * @param width        Width of the blur area in GUI units.
         * @param height       Height of the blur area in GUI units.
         * @param blurRadius   Blur radius in GUI units (0 disables blur; increase above 12 for stronger diffusion at higher GPU cost).
         * @param tintColor    ARGB tint colour applied to the blurred pixels.
         * @param tintStrength Tint mix factor between 0 (no tint) and 1 (full tint).
         */
        public void renderBlurArea(float x, float y, float width, float height,
                                   float blurRadius, int tintColor, float tintStrength) {
            this.renderBlurArea(x, y, width, height, blurRadius, tintColor, tintStrength, 1.0F, 0.0F);
        }

        /**
         * Renders a blur within this layer with complete control over tinting, opacity, and corner rounding.
         *
         * @param x            Left edge in GUI coordinates.
         * @param y            Top edge in GUI coordinates.
         * @param width        Width of the blur area in GUI units.
         * @param height       Height of the blur area in GUI units.
         * @param blurRadius   Blur radius in GUI units (0 disables blur; 4-12 balances clarity and smoothness).
         * @param tintColor    ARGB tint colour applied to the blurred pixels.
         * @param tintStrength Tint mix factor between 0 (no tint) and 1 (full tint).
         * @param opacity      Alpha multiplier between 0 (transparent) and 1 (opaque).
         * @param cornerRadius Corner radius in GUI units (0 disables rounding).
         */
        public void renderBlurArea(float x, float y, float width, float height,
                                   float blurRadius, int tintColor, float tintStrength, float opacity, float cornerRadius) {
            if (this.closed) {
                LOGGER.warn("[FANCYMENU] Attempted to render a blur area on a closed blur layer.");
                return;
            }
            renderBlurAreaInternal(this.graphics, x, y, width, height, blurRadius, tintColor, tintStrength, opacity, cornerRadius);
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (activeLayer == this) {
                activeLayer = null;
            }
        }
    }
}
