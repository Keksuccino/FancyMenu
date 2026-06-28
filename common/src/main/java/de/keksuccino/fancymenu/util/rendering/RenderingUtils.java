package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinBlendMode;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinMinecraft;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinPostChain;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.gui.MatrixUtils;
import de.keksuccino.fancymenu.util.rendering.gui.ScreenRectangle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import de.keksuccino.fancymenu.util.rendering.gui.GuiRenderTypes;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import com.mojang.math.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RenderingUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final DrawableColor MISSING_TEXTURE_COLOR_MAGENTA = DrawableColor.of(Color.MAGENTA);
    public static final DrawableColor MISSING_TEXTURE_COLOR_BLACK = DrawableColor.BLACK;
    public static final ResourceLocation FULLY_TRANSPARENT_TEXTURE = new ResourceLocation("fancymenu", "textures/fully_transparent.png");

    private static final String ALPHA_TEXTURE_SHADER_NAME_FANCYMENU = "fancymenu_gui_alpha_texture";
    private static final BlendMode OPAQUE_BLEND_MODE_FANCYMENU = new BlendMode();
    private static final int GL_NEAREST_FANCYMENU = GL11.GL_NEAREST;
    private static final int GL_LINEAR_FANCYMENU = GL11.GL_LINEAR;
    private static final List<RenderingTask> PRE_RENDER_CONTEXTS = new ArrayList<>();
    private static final List<RenderingTask> POST_RENDER_CONTEXTS = new ArrayList<>();
    private static final List<RenderingTask> DEFERRED_SCREEN_RENDERING_TASKS = new ArrayList<>();
    private static ShaderInstance alphaTextureShader_FancyMenu;
    private static boolean alphaTextureShaderFailed_FancyMenu;
    private static boolean alphaTextureShaderReloadListenerRegistered_FancyMenu;
    private static int depthTestLockDepth = 0;
    private static int blurBlockDepth = 0;
    private static int tooltipRenderingBlockDepth = 0;
    private static int overrideBackgroundBlurRadius = -1000;

    public static void postPreRenderTask(@NotNull RenderingTask context) {
        PRE_RENDER_CONTEXTS.add(Objects.requireNonNull(context));
    }

    public static void postPostRenderTask(@NotNull RenderingTask context) {
        POST_RENDER_CONTEXTS.add(Objects.requireNonNull(context));
    }

    @ApiStatus.Internal
    public static void executeAllPreRenderTasks(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        List<RenderingTask> copy = new ArrayList<>(PRE_RENDER_CONTEXTS);
        PRE_RENDER_CONTEXTS.clear();
        for (RenderingTask context : copy) {
            try {
                context.render(graphics, mouseX, mouseY, partial);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to execute pre-screen-render task!", ex);
            }
        }
        graphics.flush();
    }

    @ApiStatus.Internal
    public static void executeAllPostRenderTasks(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        List<RenderingTask> copy = new ArrayList<>(POST_RENDER_CONTEXTS);
        POST_RENDER_CONTEXTS.clear();
        for (RenderingTask context : copy) {
            try {
                context.render(graphics, mouseX, mouseY, partial);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to execute post-screen-render task!", ex);
            }
        }
        graphics.flush();
    }

    public static void renderMissing(@NotNull GuiGraphics graphics, int x, int y, int width, int height) {
        int partW = width / 2;
        int partH = height / 2;
        //Top-left
        graphics.fill(x, y, x + partW, y + partH, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
        //Top-right
        graphics.fill(x + partW, y, x + width, y + partH, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-left
        graphics.fill(x, y + partH, x + partW, y + height, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-right
        graphics.fill(x + partW, y + partH, x + width, y + height, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
    }

    public static void setOverrideBackgroundBlurRadius(int radius) {
        overrideBackgroundBlurRadius = radius;
    }

    public static void resetOverrideBackgroundBlurRadius() {
        overrideBackgroundBlurRadius = -1000;
    }

    public static boolean shouldOverrideBackgroundBlurRadius() {
        return overrideBackgroundBlurRadius != -1000;
    }

    public static int getOverrideBackgroundBlurRadius() {
        return overrideBackgroundBlurRadius;
    }

    public static void setDepthTestLocked(boolean locked) {
        if (locked) {
            depthTestLockDepth++;
            return;
        }
        if (depthTestLockDepth > 0) {
            depthTestLockDepth--;
        }
    }

    public static boolean isDepthTestLocked() {
        return depthTestLockDepth > 0;
    }

    public static void setVanillaMenuBlurringBlocked(boolean blocked) {
        if (blocked) {
            blurBlockDepth++;
            return;
        }
        if (blurBlockDepth > 0) {
            blurBlockDepth--;
        }
    }

    public static boolean isVanillaMenuBlurringBlocked() {
        return blurBlockDepth > 0;
    }

    public static void setTooltipRenderingBlocked(boolean blocked) {
        if (blocked) {
            tooltipRenderingBlockDepth++;
            return;
        }
        if (tooltipRenderingBlockDepth > 0) {
            tooltipRenderingBlockDepth--;
        }
    }

    public static boolean isTooltipRenderingBlocked() {
        return tooltipRenderingBlockDepth > 0;
    }

    public static void addDeferredScreenRenderingTask(@NotNull RenderingUtils.RenderingTask task) {
        DEFERRED_SCREEN_RENDERING_TASKS.add(task);
    }

    @NotNull
    public static List<RenderingTask> getDeferredScreenRenderingTasks() {
        return new ArrayList<>(DEFERRED_SCREEN_RENDERING_TASKS);
    }

    public static void executeAndClearDeferredScreenRenderingTasks(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        List<RenderingTask> tasks = getDeferredScreenRenderingTasks();
        DEFERRED_SCREEN_RENDERING_TASKS.clear();
        tasks.forEach(task -> task.render(graphics, mouseX, mouseY, partial));
    }

    /**
     * Draws a textured quad with the texture mirrored horizontally by explicitly flipping the texture coordinates.
     * Uses the standard sprite width and height for both texture coordinates and rendering dimensions.
     *
     * @param graphics       The GuiGraphics context.
     * @param atlasLocation  The texture resource location.
     * @param x              The x coordinate on screen.
     * @param y              The y coordinate on screen.
     * @param u              The u coordinate in the texture (top-left of the sprite).
     * @param v              The v coordinate in the texture (top-left of the sprite).
     * @param spriteWidth    The width of the sprite quad on screen and in the texture.
     * @param spriteHeight   The height of the sprite quad on screen and in the texture.
     * @param textureWidth   The total width of the texture atlas.
     * @param textureHeight  The total height of the texture atlas.
     */
    public static void blitMirrored(@NotNull GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int u, int v, int spriteWidth, int spriteHeight, int textureWidth, int textureHeight) {
        blitMirroredScaled(graphics, atlasLocation, x, y, u, v, spriteWidth, spriteHeight, spriteWidth, spriteHeight, textureWidth, textureHeight, -1);
    }

    /**
     * Draws a textured quad with the texture mirrored horizontally by explicitly flipping the texture coordinates.
     * Uses the standard sprite width and height for both texture coordinates and rendering dimensions.
     *
     * @param graphics       The GuiGraphics context.
     * @param atlasLocation  The texture resource location.
     * @param x              The x coordinate on screen.
     * @param y              The y coordinate on screen.
     * @param u              The u coordinate in the texture (top-left of the sprite).
     * @param v              The v coordinate in the texture (top-left of the sprite).
     * @param spriteWidth    The width of the sprite quad on screen and in the texture.
     * @param spriteHeight   The height of the sprite quad on screen and in the texture.
     * @param textureWidth   The total width of the texture atlas.
     * @param textureHeight  The total height of the texture atlas.
     */
    public static void blitMirrored(@NotNull GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int u, int v, int spriteWidth, int spriteHeight, int textureWidth, int textureHeight, int colorTint) {
        blitMirroredScaled(graphics, atlasLocation, x, y, u, v, spriteWidth, spriteHeight, spriteWidth, spriteHeight, textureWidth, textureHeight, colorTint);
    }

    /**
     * Draws a textured quad scaled to a specific render size, with the texture mirrored horizontally
     * by explicitly flipping the texture coordinates.
     *
     * @param graphics       The GuiGraphics context.
     * @param atlasLocation  The texture resource location.
     * @param x              The x coordinate on screen (top-left of the rendered quad).
     * @param y              The y coordinate on screen (top-left of the rendered quad).
     * @param u              The u coordinate in the texture (top-left of the source sprite region).
     * @param v              The v coordinate in the texture (top-left of the source sprite region).
     * @param spriteWidth    The width of the source sprite region in the texture atlas.
     * @param spriteHeight   The height of the source sprite region in the texture atlas.
     * @param renderWidth    The desired width of the quad to render on screen.
     * @param renderHeight   The desired height of the quad to render on screen.
     * @param textureWidth   The total width of the texture atlas.
     * @param textureHeight  The total height of the texture atlas.
     * @param color          The color tint to apply (ARGB format, -1 for white/no tint).
     */
    public static void blitMirroredScaled(
            @NotNull GuiGraphics graphics,
            ResourceLocation atlasLocation,
            int x, int y,          // Screen position
            int u, int v,          // Texture region origin in atlas
            int spriteWidth, int spriteHeight, // Original sprite size in atlas for UV calculation
            int renderWidth, int renderHeight, // Target size on screen
            int textureWidth, int textureHeight, // Total atlas size
            int color             // Tint color
    ) {
        // Calculate texture coordinates based on the original sprite dimensions
        float minU = (float)u / (float)textureWidth;
        float maxU = (float)(u + spriteWidth) / (float)textureWidth; // Use spriteWidth for UVs
        float minV = (float)v / (float)textureHeight;
        float maxV = (float)(v + spriteHeight) / (float)textureHeight; // Use spriteHeight for UVs

        graphics.flush();
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(RenderingUtils::getAlphaTextureShader_FancyMenu);
        setupAlphaBlend();

        // Access rendering internals
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder consumer = Tesselator.getInstance().getBuilder();
        consumer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        // Add vertices with screen dimensions using renderWidth/renderHeight,
        // but texture coordinates (UVs) swapped horizontally (minU/maxU flipped)
        consumer.vertex(matrix4f, (float)x,                 (float)y,                  0.0F).uv(maxU, minV).color(color).endVertex(); // Top-left screen -> Top-right texture UV (maxU, minV)
        consumer.vertex(matrix4f, (float)x,                 (float)(y + renderHeight), 0.0F).uv(maxU, maxV).color(color).endVertex(); // Bottom-left screen -> Bottom-right texture UV (maxU, maxV)
        consumer.vertex(matrix4f, (float)(x + renderWidth), (float)(y + renderHeight), 0.0F).uv(minU, maxV).color(color).endVertex(); // Bottom-right screen -> Bottom-left texture UV (minU, maxV)
        consumer.vertex(matrix4f, (float)(x + renderWidth), (float)y,                  0.0F).uv(minU, minV).color(color).endVertex(); // Top-right screen -> Top-left texture UV (minU, minV)

        BufferUploader.drawWithShader(consumer.end());
        RenderSystem.disableBlend();

    }

    /**
     * Repeatedly renders a tileable (seamless) texture inside an area. Fills the area with the texture.
     *
     * @param graphics The {@link GuiGraphics} instance.
     * @param location The {@link ResourceLocation} of the texture.
     * @param x The X position the area should get rendered at.
     * @param y The Y position the area should get rendered at.
     * @param areaRenderWidth The width of the area.
     * @param areaRenderHeight The height of the area.
     * @param texWidth The full width (in pixels) of the texture.
     * @param texHeight The full height (in pixels) of the texture.
     */
    public static void blitRepeat(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texWidth, int texHeight) {
        blitAlphaTexture(graphics, location, x, y, 0.0F, 0.0F, areaRenderWidth, areaRenderHeight, texWidth, texHeight);
    }

    /**
     * Renders a texture using nine-slice scaling with tiled edges and center.
     *
     * @param graphics The GuiGraphics instance to use for rendering
     * @param texture The texture ResourceLocation to render
     * @param x The x position to render at
     * @param y The y position to render at
     * @param width The desired width to render
     * @param height The desired height to render
     * @param textureWidth The actual width of the texture
     * @param textureHeight The actual height of the texture
     * @param borderTop The size of the top border
     * @param borderRight The size of the right border
     * @param borderBottom The size of the bottom border
     * @param borderLeft The size of the left border
     */
    public static void blitNineSlicedTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                                             int textureWidth, int textureHeight,
                                             int borderTop, int borderRight, int borderBottom, int borderLeft) {

        // Correct border sizes if they're too large
        if (borderLeft + borderRight >= textureWidth) {
            float scale = (float)(textureWidth - 2) / (borderLeft + borderRight);
            borderLeft = (int)(borderLeft * scale);
            borderRight = (int)(borderRight * scale);
        }
        if (borderTop + borderBottom >= textureHeight) {
            float scale = (float)(textureHeight - 2) / (borderTop + borderBottom);
            borderTop = (int)(borderTop * scale);
            borderBottom = (int)(borderBottom * scale);
        }

        // Corner pieces
        // Top left
        blitAlphaTexture(graphics, texture, x, y, 0.0F, 0.0F, borderLeft, borderTop, textureWidth, textureHeight);
        // Top right
        blitAlphaTexture(graphics, texture, x + width - borderRight, y, textureWidth - borderRight, 0.0F, borderRight, borderTop, textureWidth, textureHeight);
        // Bottom left
        blitAlphaTexture(graphics, texture, x, y + height - borderBottom, 0.0F, textureHeight - borderBottom, borderLeft, borderBottom, textureWidth, textureHeight);
        // Bottom right
        blitAlphaTexture(graphics, texture, x + width - borderRight, y + height - borderBottom, textureWidth - borderRight, textureHeight - borderBottom, borderRight, borderBottom, textureWidth, textureHeight);

        // Edges - Tiled
        int centerWidth = textureWidth - borderLeft - borderRight;
        int centerHeight = textureHeight - borderTop - borderBottom;

        // Top edge
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            blitAlphaTexture(graphics, texture, x + i, y, borderLeft, 0.0F, pieceWidth, borderTop, textureWidth, textureHeight);
        }

        // Bottom edge
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            blitAlphaTexture(graphics, texture, x + i, y + height - borderBottom, borderLeft, textureHeight - borderBottom, pieceWidth, borderBottom, textureWidth, textureHeight);
        }

        // Left edge
        for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
            int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
            blitAlphaTexture(graphics, texture, x, y + j, 0.0F, borderTop, borderLeft, pieceHeight, textureWidth, textureHeight);
        }

        // Right edge
        for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
            int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
            blitAlphaTexture(graphics, texture, x + width - borderRight, y + j, textureWidth - borderRight, borderTop, borderRight, pieceHeight, textureWidth, textureHeight);
        }

        // Center - Tiled
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
                int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
                blitAlphaTexture(graphics, texture, x + i, y + j, borderLeft, borderTop, pieceWidth, pieceHeight, textureWidth, textureHeight);
            }
        }

    }

    public static float getPartialTick() {
        return Minecraft.getInstance().isPaused() ? ((IMixinMinecraft)Minecraft.getInstance()).getPausePartialTickFancyMenu() : Minecraft.getInstance().getFrameTime();
    }

    public static boolean isXYInArea(int targetX, int targetY, int x, int y, int width, int height) {
        return isXYInArea((double)targetX, targetY, x, y, width, height);
    }

    public static boolean isXYInArea(double targetX, double targetY, double x, double y, double width, double height) {
        return (targetX >= x) && (targetX < (x + width)) && (targetY >= y) && (targetY < (y + height));
    }

    public static void resetGuiScale() {
        Window m = Minecraft.getInstance().getWindow();
        m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get()));
    }

    public static void resetShaderColor(GuiGraphics graphics) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void setupAlphaBlend() {
        RenderSystem.enableBlend();
        RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
    }

    public static void assumeOpaqueShaderBlendMode() {
        IMixinBlendMode.set_lastApplied_FancyMenu(OPAQUE_BLEND_MODE_FANCYMENU);
    }

    public static void processPostChainRestoringRenderState(@NotNull PostChain postChain, float partial) {
        RenderStateSnapshot renderState = captureRenderState();
        Map<RenderTarget, Integer> originalFilterModes = capturePostChainFilterModes(postChain);
        try {
            // Minecraft 1.20.1 parses post chains before "use_linear_filter" existed.
            // Mirror the 1.21.x behavior by applying linear filtering to the screen and
            // temporary targets, then restoring every target to a valid filter afterwards.
            setPostChainFilterMode(postChain, GL_LINEAR_FANCYMENU);
            postChain.process(partial);
        } finally {
            try {
                restoreFilterModes(originalFilterModes);
            } finally {
                renderState.restore();
            }
        }
    }

    private static Map<RenderTarget, Integer> capturePostChainFilterModes(@NotNull PostChain postChain) {
        Map<RenderTarget, Integer> filterModes = new IdentityHashMap<>();
        IMixinPostChain accessor = (IMixinPostChain) postChain;
        rememberFilterMode(filterModes, accessor.getScreenTarget_FancyMenu());
        for (RenderTarget target : accessor.getCustomRenderTargets_FancyMenu().values()) {
            rememberFilterMode(filterModes, target);
        }
        return filterModes;
    }

    private static void rememberFilterMode(@NotNull Map<RenderTarget, Integer> filterModes, RenderTarget target) {
        if (target != null && !filterModes.containsKey(target)) {
            filterModes.put(target, normalizeFilterMode(target.filterMode));
        }
    }

    private static void setPostChainFilterMode(@NotNull PostChain postChain, int filterMode) {
        IMixinPostChain accessor = (IMixinPostChain) postChain;
        setFilterMode(accessor.getScreenTarget_FancyMenu(), filterMode);
        for (RenderTarget target : accessor.getCustomRenderTargets_FancyMenu().values()) {
            setFilterMode(target, filterMode);
        }
    }

    private static void restoreFilterModes(@NotNull Map<RenderTarget, Integer> filterModes) {
        for (Map.Entry<RenderTarget, Integer> entry : filterModes.entrySet()) {
            setFilterMode(entry.getKey(), entry.getValue());
        }
    }

    private static void setFilterMode(RenderTarget target, int filterMode) {
        if (target != null) {
            int normalizedFilterMode = normalizeFilterMode(filterMode);
            if (target.filterMode != normalizedFilterMode) {
                target.setFilterMode(normalizedFilterMode);
            }
        }
    }

    private static int normalizeFilterMode(int filterMode) {
        if (filterMode == GL_LINEAR_FANCYMENU || filterMode == GL_NEAREST_FANCYMENU) {
            return filterMode;
        }

        // Minecraft 1.20.1's MainTarget initializes its GL texture filter directly,
        // leaving RenderTarget.filterMode at 0. Restoring that value would poison the
        // main framebuffer texture with an invalid filter enum and make later blits black.
        return GL_NEAREST_FANCYMENU;
    }

    public static void blitRenderTargetToScreenImmediate(@NotNull RenderTarget renderTarget) {
        Objects.requireNonNull(renderTarget);
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        int screenWidth = window.getWidth();
        int screenHeight = window.getHeight();
        if (screenWidth <= 0 || screenHeight <= 0 || renderTarget.width <= 0 || renderTarget.height <= 0) {
            return;
        }
        if (!RenderSystem.isOnRenderThread()) {
            renderTarget.blitToScreen(screenWidth, screenHeight, false);
            return;
        }

        RenderStateSnapshot renderState = captureRenderState();
        try {
            float guiWidth = (float)((double)screenWidth / window.getGuiScale());
            float guiHeight = (float)((double)screenHeight / window.getGuiScale());
            float maxU = (float)renderTarget.viewWidth / (float)renderTarget.width;
            float maxV = (float)renderTarget.viewHeight / (float)renderTarget.height;

            minecraft.getMainRenderTarget().bindWrite(false);
            GlStateManager._colorMask(true, true, true, false);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask(false);
            GlStateManager._viewport(0, 0, screenWidth, screenHeight);
            setupAlphaBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, renderTarget.getColorTextureId());
            RenderSystem.setShader(RenderingUtils::getAlphaTextureShader_FancyMenu);

            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            bufferBuilder.vertex(0.0D, guiHeight, 0.0D).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
            bufferBuilder.vertex(guiWidth, guiHeight, 0.0D).uv(maxU, 0.0F).color(255, 255, 255, 255).endVertex();
            bufferBuilder.vertex(guiWidth, 0.0D, 0.0D).uv(maxU, maxV).color(255, 255, 255, 255).endVertex();
            bufferBuilder.vertex(0.0D, 0.0D, 0.0D).uv(0.0F, maxV).color(255, 255, 255, 255).endVertex();
            BufferUploader.drawWithShader(bufferBuilder.end());
        } finally {
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
            renderState.restore();
        }
    }

    public static void clearRenderTargetIgnoringScissor(RenderTarget renderTarget) {
        if (renderTarget == null) {
            return;
        }
        RenderStateSnapshot renderState = captureRenderState();
        try {
            RenderSystem.disableScissor();
            GlStateManager._colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            renderTarget.clear(Minecraft.ON_OSX);
        } finally {
            renderState.restore();
        }
    }

    public static RenderStateSnapshot captureRenderState() {
        return RenderStateSnapshot.capture();
    }

    public static final class RenderStateSnapshot {

        private static final int SHADER_TEXTURE_COUNT_FANCYMENU = 12;

        private final int framebuffer;
        private final int activeTexture;
        private final int[] viewport = new int[4];
        private final int[] scissorBox = new int[4];
        private final int[] textureBindings = new int[SHADER_TEXTURE_COUNT_FANCYMENU];
        private final int[] shaderTextures = new int[SHADER_TEXTURE_COUNT_FANCYMENU];
        private final boolean[] colorMask = new boolean[4];
        private final float[] clearColor = new float[4];
        private final boolean scissorEnabled;
        private final boolean blendEnabled;
        private final int blendEquation;
        private final int blendSrcRgb;
        private final int blendDstRgb;
        private final int blendSrcAlpha;
        private final int blendDstAlpha;
        private final BlendMode blendMode;
        private final boolean depthTestEnabled;
        private final int depthFunc;
        private final boolean depthMask;
        private final float clearDepth;
        private final boolean cullEnabled;
        private final float[] shaderColor = new float[4];
        private final float shaderFogStart;
        private final float shaderFogEnd;
        private final float[] shaderFogColor = new float[4];
        private final FogShape shaderFogShape;
        private final ShaderInstance shader;

        private RenderStateSnapshot() {
            this.framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            this.activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, this.scissorBox);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer colorMaskBuffer = stack.malloc(4);
                GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMaskBuffer);
                for (int i = 0; i < this.colorMask.length; i++) {
                    this.colorMask[i] = colorMaskBuffer.get(i) != 0;
                }

                FloatBuffer clearColorBuffer = stack.mallocFloat(4);
                GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColorBuffer);
                for (int i = 0; i < this.clearColor.length; i++) {
                    this.clearColor[i] = clearColorBuffer.get(i);
                }
            }
            this.scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            this.blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            this.blendEquation = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
            this.blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            this.blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            this.blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            this.blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            this.blendMode = IMixinBlendMode.get_lastApplied_FancyMenu();
            this.depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            this.depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            this.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            this.clearDepth = GL11.glGetFloat(GL11.GL_DEPTH_CLEAR_VALUE);
            this.cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            System.arraycopy(RenderSystem.getShaderColor(), 0, this.shaderColor, 0, this.shaderColor.length);
            this.shaderFogStart = RenderSystem.getShaderFogStart();
            this.shaderFogEnd = RenderSystem.getShaderFogEnd();
            System.arraycopy(RenderSystem.getShaderFogColor(), 0, this.shaderFogColor, 0, this.shaderFogColor.length);
            this.shaderFogShape = RenderSystem.getShaderFogShape();
            this.shader = RenderSystem.getShader();

            for (int i = 0; i < SHADER_TEXTURE_COUNT_FANCYMENU; i++) {
                this.shaderTextures[i] = RenderSystem.getShaderTexture(i);
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
                this.textureBindings[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
            RenderSystem.activeTexture(this.activeTexture);
        }

        private static RenderStateSnapshot capture() {
            return new RenderStateSnapshot();
        }

        public void restore() {
            for (int i = 0; i < SHADER_TEXTURE_COUNT_FANCYMENU; i++) {
                RenderSystem.setShaderTexture(i, this.shaderTextures[i]);
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
                RenderSystem.bindTexture(this.textureBindings[i]);
            }
            RenderSystem.activeTexture(this.activeTexture);

            RenderSystem.setShader(() -> this.shader);
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebuffer);
            GlStateManager._viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);

            if (this.scissorEnabled) {
                RenderSystem.enableScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
            } else {
                RenderSystem.disableScissor();
            }

            if (this.blendEnabled) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
            RenderSystem.blendEquation(this.blendEquation);
            RenderSystem.blendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
            IMixinBlendMode.set_lastApplied_FancyMenu(this.blendMode);

            if (this.depthTestEnabled) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            RenderSystem.depthFunc(this.depthFunc);
            RenderSystem.depthMask(this.depthMask);
            if (this.cullEnabled) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
            RenderSystem.setShaderColor(this.shaderColor[0], this.shaderColor[1], this.shaderColor[2], this.shaderColor[3]);
            RenderSystem.setShaderFogStart(this.shaderFogStart);
            RenderSystem.setShaderFogEnd(this.shaderFogEnd);
            RenderSystem.setShaderFogColor(this.shaderFogColor[0], this.shaderFogColor[1], this.shaderFogColor[2], this.shaderFogColor[3]);
            RenderSystem.setShaderFogShape(this.shaderFogShape);
            GlStateManager._clearColor(this.clearColor[0], this.clearColor[1], this.clearColor[2], this.clearColor[3]);
            GlStateManager._clearDepth(this.clearDepth);
            GlStateManager._colorMask(this.colorMask[0], this.colorMask[1], this.colorMask[2], this.colorMask[3]);
        }

    }

    public static void blitAlphaTexture(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, float x, float y, float width, float height) {
        blitAlphaTexture(graphics, location, x, y, 0.0F, 0.0F, width, height, width, height);
    }

    public static void blitAlphaTexture(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, float x, float y, float width, float height, int color) {
        blitAlphaTexture(graphics, location, x, y, 0.0F, 0.0F, width, height, width, height, color);
    }

    public static void blitAlphaTexture(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, float x, float y, float u, float v, float width, float height, float textureWidth, float textureHeight) {
        blitAlphaTexture(graphics, location, x, y, u, v, width, height, textureWidth, textureHeight, -1);
    }

    public static void blitAlphaTexture(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, float x, float y, float u, float v, float width, float height, float textureWidth, float textureHeight, int color) {
        blitAlphaTextureRegion(graphics, location, x, y, width, height, u, v, width, height, textureWidth, textureHeight, color);
    }

    public static void blitAlphaTextureRegion(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, float x, float y, float renderWidth, float renderHeight, float u, float v, float regionWidth, float regionHeight, float textureWidth, float textureHeight) {
        blitAlphaTextureRegion(graphics, location, x, y, renderWidth, renderHeight, u, v, regionWidth, regionHeight, textureWidth, textureHeight, -1);
    }

    public static void blitAlphaTextureRegion(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, float x, float y, float renderWidth, float renderHeight, float u, float v, float regionWidth, float regionHeight, float textureWidth, float textureHeight, int color) {
        if (renderWidth <= 0.0F || renderHeight <= 0.0F || regionWidth <= 0.0F || regionHeight <= 0.0F || textureWidth <= 0.0F || textureHeight <= 0.0F) {
            return;
        }
        float minU = u / textureWidth;
        float maxU = (u + regionWidth) / textureWidth;
        float minV = v / textureHeight;
        float maxV = (v + regionHeight) / textureHeight;
        innerBlitAlphaTexture(graphics, location, x, x + renderWidth, y, y + renderHeight, 0.0F, minU, maxU, minV, maxV, color);
    }

    public static void setShaderColor(GuiGraphics graphics, DrawableColor color) {
        Color c = color.getColor();
        float a = Math.min(1F, Math.max(0F, (float)c.getAlpha() / 255.0F));
        setShaderColor(graphics, color, a);
    }

    public static void setShaderColor(GuiGraphics graphics, DrawableColor color, float alpha) {
        Color c = color.getColor();
        float r = Math.min(1F, Math.max(0F, (float)c.getRed() / 255.0F));
        float g = Math.min(1F, Math.max(0F, (float)c.getGreen() / 255.0F));
        float b = Math.min(1F, Math.max(0F, (float)c.getBlue() / 255.0F));
        graphics.setColor(r, g, b, alpha);
    }

    public static void setShaderColor(GuiGraphics graphics, int color, float alpha) {
        float red = (float) FastColor.ARGB32.red(color) / 255.0F;
        float green = (float)FastColor.ARGB32.green(color) / 255.0F;
        float blue = (float)FastColor.ARGB32.blue(color) / 255.0F;
        graphics.setColor(red, green, blue, alpha);
    }

    public static void setShaderColor(GuiGraphics graphics, int color) {
        float red = (float) FastColor.ARGB32.red(color) / 255.0F;
        float green = (float)FastColor.ARGB32.green(color) / 255.0F;
        float blue = (float)FastColor.ARGB32.blue(color) / 255.0F;
        float alpha = (float) FastColor.ARGB32.alpha(color) / 255.0F;
        graphics.setColor(red, green, blue, alpha);
    }

    /**
     * @param color The color.
     * @param newAlpha Value between 0 and 255.
     * @return The given color with new alpha.
     */
    public static int replaceAlphaInColor(int color, int newAlpha) {
        newAlpha = Math.min(newAlpha, 255);
        return color & 16777215 | newAlpha << 24;
    }

    /**
     * @param color The color.
     * @param newAlpha Value between 0.0F and 1.0F.
     * @return The given color with new alpha.
     */
    public static int replaceAlphaInColor(int color, float newAlpha) {
        return replaceAlphaInColor(color, (int)(newAlpha * 255.0F));
    }

    public static int getMinecraftFps() {
        try {
            String fpsString = Minecraft.getInstance().fpsString;
            if (fpsString == null || fpsString.isBlank()) return 0;
            String fps = fpsString.contains(" ") ? fpsString.split(" ", 2)[0] : fpsString;
            return Integer.parseInt(fps);
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static void fillF(@NotNull GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int color) {
        fillF(graphics, minX, minY, maxX, maxY, 0F, color);
    }

    public static void fillF(@NotNull GuiGraphics graphics, float minX, float minY, float maxX, float maxY, float z, int color) {
        Matrix4f matrix4f = graphics.pose().last().pose();
        if (minX < maxX) {
            float i = minX;
            minX = maxX;
            maxX = i;
        }
        if (minY < maxY) {
            float i = minY;
            minY = maxY;
            maxY = i;
        }
        VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(GuiRenderTypes.gui());
        vertexConsumer.vertex(matrix4f, (float)minX, (float)minY, (float)z).color(color).endVertex();
        vertexConsumer.vertex(matrix4f, (float)minX, (float)maxY, (float)z).color(color).endVertex();
        vertexConsumer.vertex(matrix4f, (float)maxX, (float)maxY, (float)z).color(color).endVertex();
        vertexConsumer.vertex(matrix4f, (float)maxX, (float)minY, (float)z).color(color).endVertex();
        // Flush GuiRenderTypes.gui() so later textured blits do not get overdrawn by this untextured quad.
        graphics.flush();
    }

    public static void blitF(@NotNull GuiGraphics graphics, ResourceLocation location, float x, float y, float f3, float f4, float width, float height, float width2, float height2, int color) {
        float red = (float)FastColor.ARGB32.red(color) / 255.0F;
        float green = (float)FastColor.ARGB32.green(color) / 255.0F;
        float blue = (float)FastColor.ARGB32.blue(color) / 255.0F;
        float alpha = (float)FastColor.ARGB32.alpha(color) / 255.0F;
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(graphics, location, x, y, width, height, f3, f4, width, height, width2, height2, red, green, blue, alpha);
    }

    public static void blitF(@NotNull GuiGraphics graphics, ResourceLocation location, float x, float y, float f3, float f4, float width, float height, float width2, float height2) {
        blit(graphics, location, x, y, width, height, f3, f4, width, height, width2, height2);
    }

    private static void blit(GuiGraphics $$0, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10) {
        blit($$0, location, $$1, $$1 + $$3, $$2, $$2 + $$4, 0, $$7, $$8, $$5, $$6, $$9, $$10);
    }

    private static void blit(GuiGraphics $$0, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float red, float green, float blue, float alpha) {
        blit($$0, location, $$1, $$1 + $$3, $$2, $$2 + $$4, 0, $$7, $$8, $$5, $$6, $$9, $$10, red, green, blue, alpha);
    }

    private static void blit(GuiGraphics graphics, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float $$11) {
        innerBlit(
                graphics,
                location,
                $$1,
                $$2,
                $$3,
                $$4,
                $$5,
                ($$8 + 0.0F) / (float)$$10,
                ($$8 + (float)$$6) / (float)$$10,
                ($$9 + 0.0F) / (float)$$11,
                ($$9 + (float)$$7) / (float)$$11
        );
    }

    private static void blit(GuiGraphics graphics, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float $$11, float red, float green, float blue, float alpha) {
        innerBlit(
                graphics,
                location,
                $$1,
                $$2,
                $$3,
                $$4,
                $$5,
                ($$8 + 0.0F) / (float)$$10,
                ($$8 + (float)$$6) / (float)$$10,
                ($$9 + 0.0F) / (float)$$11,
                ($$9 + (float)$$7) / (float)$$11,
                red,
                green,
                blue,
                alpha
        );
    }

    private static ShaderInstance getAlphaTextureShader_FancyMenu() {
        ensureAlphaTextureShaderReloadListener_FancyMenu();
        if (alphaTextureShaderFailed_FancyMenu) {
            return GameRenderer.getPositionTexColorShader();
        }
        if (alphaTextureShader_FancyMenu == null) {
            try {
                alphaTextureShader_FancyMenu = new ShaderInstance(Minecraft.getInstance().getResourceManager(), ALPHA_TEXTURE_SHADER_NAME_FANCYMENU, DefaultVertexFormat.POSITION_TEX_COLOR);
            } catch (Exception ex) {
                alphaTextureShaderFailed_FancyMenu = true;
                LOGGER.error("[FANCYMENU] Failed to load GUI alpha texture shader!", ex);
                return GameRenderer.getPositionTexColorShader();
            }
        }
        return alphaTextureShader_FancyMenu;
    }

    private static void ensureAlphaTextureShaderReloadListener_FancyMenu() {
        if (alphaTextureShaderReloadListenerRegistered_FancyMenu) {
            return;
        }
        alphaTextureShaderReloadListenerRegistered_FancyMenu = true;
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                RenderSystem.recordRenderCall(RenderingUtils::clearAlphaTextureShader_FancyMenu);
            }
        });
    }

    private static void clearAlphaTextureShader_FancyMenu() {
        if (alphaTextureShader_FancyMenu != null) {
            alphaTextureShader_FancyMenu.close();
            alphaTextureShader_FancyMenu = null;
        }
        alphaTextureShaderFailed_FancyMenu = false;
    }

    private static void innerBlitAlphaTexture(GuiGraphics graphics, ResourceLocation location, float minX, float maxX, float minY, float maxY, float z, float minU, float maxU, float minV, float maxV, int color) {
        graphics.flush();
        setupAlphaBlend();
        RenderSystem.setShaderTexture(0, location);
        RenderSystem.setShader(RenderingUtils::getAlphaTextureShader_FancyMenu);
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.vertex(matrix4f, minX, minY, z).uv(minU, minV).color(color).endVertex();
        bufferBuilder.vertex(matrix4f, minX, maxY, z).uv(minU, maxV).color(color).endVertex();
        bufferBuilder.vertex(matrix4f, maxX, maxY, z).uv(maxU, maxV).color(color).endVertex();
        bufferBuilder.vertex(matrix4f, maxX, minY, z).uv(maxU, minV).color(color).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static void innerBlit(GuiGraphics graphics, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9) {
        innerBlitAlphaTexture(graphics, location, $$1, $$2, $$3, $$4, $$5, $$6, $$7, $$8, $$9, -1);
    }

    private static void innerBlit(GuiGraphics graphics, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float red, float green, float blue, float alpha) {
        graphics.flush();
        setupAlphaBlend();
        RenderSystem.setShaderTexture(0, location);
        RenderSystem.setShader(RenderingUtils::getAlphaTextureShader_FancyMenu);
        Matrix4f $$10 = graphics.pose().last().pose();
        BufferBuilder $$11 = Tesselator.getInstance().getBuilder();
        $$11.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        $$11.vertex($$10, $$1, $$3, $$5).uv($$6, $$8).color(red, green, blue, alpha).endVertex();
        $$11.vertex($$10, $$1, $$4, $$5).uv($$6, $$9).color(red, green, blue, alpha).endVertex();
        $$11.vertex($$10, $$2, $$4, $$5).uv($$7, $$9).color(red, green, blue, alpha).endVertex();
        $$11.vertex($$10, $$2, $$3, $$5).uv($$7, $$8).color(red, green, blue, alpha).endVertex();
        BufferUploader.drawWithShader($$11.end());
    }

    public static void enableScissor(@NotNull GuiGraphics graphics, int minX, int minY, int maxX, int maxY) {
        ScreenRectangle r = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY);
        r = transformAxisAligned(r, graphics.pose().last().pose());
        graphics.enableScissor(r.left(), r.top(), r.right(), r.bottom());
    }

    public static void disableScissor(@NotNull GuiGraphics graphics) {
        graphics.disableScissor();
    }

    @NotNull
    public static ScreenRectangle transformAxisAligned(@NotNull ScreenRectangle toTransform, @NotNull Matrix4f pose) {
        if (isMatrixIdentity(pose)) {
            return toTransform;
        } else {
            org.joml.Matrix4f jomlPose = MatrixUtils.convertToJoml(pose);
            Vector3f vector3f = jomlPose.transformPosition((float)toTransform.left(), (float)toTransform.top(), 0.0F, new Vector3f());
            Vector3f vector3f2 = jomlPose.transformPosition((float)toTransform.right(), (float)toTransform.bottom(), 0.0F, new Vector3f());
            return new ScreenRectangle(Mth.floor(vector3f.x), Mth.floor(vector3f.y), Mth.floor(vector3f2.x - vector3f.x), Mth.floor(vector3f2.y - vector3f.y));
        }
    }

    public static boolean isMatrixIdentity(Matrix4f matrix) {
        return MatrixUtils.isMatrixIdentityMojang(matrix);
    }

    @FunctionalInterface
    public interface RenderingTask {
        void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);
    }

}
