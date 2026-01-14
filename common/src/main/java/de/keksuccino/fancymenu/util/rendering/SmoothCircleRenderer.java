package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinPostChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public final class SmoothCircleRenderer {

    private static final Logger LOGGER = LogManager.getLogger();
    // Keep shader files in the default 'minecraft' namespace so the vanilla resource manager finds them for every loader.
    private static final ResourceLocation GUI_SMOOTH_CIRCLE_POST_CHAIN = ResourceLocation.withDefaultNamespace("shaders/post/fancymenu_gui_smooth_circle.json");

    private static PostChain smoothCirclePostChain;
    private static boolean smoothCirclePostChainFailed;
    private static int cachedWidth = -1;
    private static int cachedHeight = -1;

    private SmoothCircleRenderer() {
    }

    /**
     * Renders a smooth superellipse using the provided bounding rectangle.
     *
     * <p>Width/height control the aspect ratio (circle vs oval), while {@code roundness} controls the shape exponent
     * (how boxy or pointy the outline is). If you only need true circles/ovals, keep {@code roundness = 2.0}.
     * Values higher than {@code 2.0} make the shape boxier (approaching a rounded rectangle), while values lower
     * than {@code 2.0} make it pointier (approaching a diamond).
     *
     * <p>Roundness examples:
     * <br>- Perfect circle/ellipse: {@code roundness = 2.0}
     * <br>- Squircle-like (boxier): {@code roundness = 4.0}
     * <br>- Diamond-like (pointier): {@code roundness = 1.0}
     *
     * <p>Shape examples:
     * <br>- Perfect circle: width == height and {@code roundness = 2.0}
     * <br>- Oval: width != height and {@code roundness = 2.0}
     */
    public static void renderSmoothCircle(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float roundness, int color, float partial) {
        renderSmoothCircleInternal(graphics, x, y, width, height, 0.0F, roundness, color, partial);
    }

    public static void renderSmoothCircleScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float roundness, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothCircle(graphics, x * additionalScale + translationX, y * additionalScale + translationY, width * additionalScale, height * additionalScale, roundness, color, partial);
    }

    /**
     * Renders a smooth superellipse border using the provided bounding rectangle.
     *
     * <p>Width/height control the aspect ratio (circle vs oval), while {@code roundness} controls the shape exponent
     * (how boxy or pointy the outline is). If you only need true circles/ovals, keep {@code roundness = 2.0}.
     * Values higher than {@code 2.0} make the shape boxier (approaching a rounded rectangle), while values lower
     * than {@code 2.0} make it pointier (approaching a diamond).
     *
     * <p>Roundness examples:
     * <br>- Perfect circle/ellipse: {@code roundness = 2.0}
     * <br>- Squircle-like (boxier): {@code roundness = 4.0}
     * <br>- Diamond-like (pointier): {@code roundness = 1.0}
     *
     * <p>Shape examples:
     * <br>- Perfect circle: width == height and {@code roundness = 2.0}
     * <br>- Oval: width != height and {@code roundness = 2.0}
     */
    public static void renderSmoothCircleBorder(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, float roundness, int color, float partial) {
        renderSmoothCircleInternal(graphics, x, y, width, height, borderThickness, roundness, color, partial);
    }

    public static void renderSmoothCircleBorderScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, float roundness, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothCircleBorder(graphics, x * additionalScale + translationX, y * additionalScale + translationY, width * additionalScale, height * additionalScale, borderThickness * additionalScale, roundness, color, partial);
    }

    private static void renderSmoothCircleInternal(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, float roundness, int color, float partial) {
        Objects.requireNonNull(graphics);
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        float clampedRoundness = Math.max(0.1F, roundness);
        _renderSmoothCircle(graphics, partial, new CircleArea(x, y, width, height, Math.max(0.0F, borderThickness), clampedRoundness, color));
    }

    private static float resolveAdditionalRenderScale() {
        float scale = RenderScaleUtil.getCurrentAdditionalRenderScale();
        return Float.isFinite(scale) && scale > 0.0F ? scale : 1.0F;
    }

    private static float resolveAdditionalRenderTranslationX() {
        float translation = RenderTranslationUtil.getCurrentAdditionalRenderTranslationX();
        return Float.isFinite(translation) ? translation : 0.0F;
    }

    private static float resolveAdditionalRenderTranslationY() {
        float translation = RenderTranslationUtil.getCurrentAdditionalRenderTranslationY();
        return Float.isFinite(translation) ? translation : 0.0F;
    }

    private static void _renderSmoothCircle(GuiGraphics graphics, float partial, CircleArea area) {
        Minecraft minecraft = Minecraft.getInstance();
        PostChain postChain = getOrCreatePostChain(minecraft);
        if (postChain == null) {
            return;
        }
        int targetWidth = minecraft.getWindow().getWidth();
        int targetHeight = minecraft.getWindow().getHeight();
        if (targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        ensurePostChainSize(postChain, targetWidth, targetHeight);

        float guiScale = (float) minecraft.getWindow().getGuiScale();
        float scaledWidth = area.width * guiScale;
        float scaledHeight = area.height * guiScale;
        if (scaledWidth <= 0.0F || scaledHeight <= 0.0F) {
            return;
        }

        float scaledX = area.x * guiScale;
        float scaledY = targetHeight - (area.y * guiScale) - scaledHeight;
        float scaledBorderThickness = area.borderThickness * guiScale;
        float scaledRoundness = area.roundness;

        float red = (float) FastColor.ARGB32.red(area.color) / 255.0F;
        float green = (float) FastColor.ARGB32.green(area.color) / 255.0F;
        float blue = (float) FastColor.ARGB32.blue(area.color) / 255.0F;
        float alpha = (float) FastColor.ARGB32.alpha(area.color) / 255.0F;

        applyUniforms(postChain, scaledX, scaledY, scaledWidth, scaledHeight, scaledBorderThickness, scaledRoundness, red, green, blue, alpha);

        graphics.flush();
        RenderSystem.disableBlend();
        postChain.process(partial);
        RenderTarget finalTarget = getFinalTarget(postChain);
        minecraft.getMainRenderTarget().bindWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (finalTarget != null) {
            finalTarget.blitToScreen(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), false);
        }
        RenderingUtils.resetShaderColor(graphics);
    }

    private static PostChain getOrCreatePostChain(Minecraft minecraft) {
        if (smoothCirclePostChainFailed) {
            return null;
        }
        if (smoothCirclePostChain == null) {
            try {
                smoothCirclePostChain = new PostChain(
                        minecraft.getTextureManager(),
                        minecraft.getResourceManager(),
                        minecraft.getMainRenderTarget(),
                        GUI_SMOOTH_CIRCLE_POST_CHAIN
                );
                cachedWidth = -1;
                cachedHeight = -1;
                ensurePostChainSize(smoothCirclePostChain, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            } catch (Exception ex) {
                smoothCirclePostChainFailed = true;
                LOGGER.error("[FANCYMENU] Failed to load GUI smooth circle shader!", ex);
                return null;
            }
        }
        return smoothCirclePostChain;
    }

    private static void ensurePostChainSize(PostChain postChain, int width, int height) {
        if (width != cachedWidth || height != cachedHeight) {
            cachedWidth = width;
            cachedHeight = height;
            postChain.resize(width, height);
        }
    }

    private static void applyUniforms(PostChain postChain, float x, float y, float width, float height, float borderThickness, float roundness, float red, float green, float blue, float alpha) {
        List<PostPass> passes = ((IMixinPostChain) postChain).getPasses_FancyMenu();
        for (PostPass pass : passes) {
            if (!"fancymenu_gui_smooth_circle".equals(pass.getName())) {
                continue;
            }
            pass.getEffect().safeGetUniform("Rect").set(x, y, width, height);
            pass.getEffect().safeGetUniform("BorderThickness").set(borderThickness);
            pass.getEffect().safeGetUniform("Roundness").set(roundness);
            pass.getEffect().safeGetUniform("Color").set(red, green, blue, alpha);
        }
    }

    private static RenderTarget getFinalTarget(PostChain postChain) {
        List<PostPass> passes = ((IMixinPostChain) postChain).getPasses_FancyMenu();
        for (int i = passes.size() - 1; i >= 0; i--) {
            PostPass pass = passes.get(i);
            if ("fancymenu_gui_smooth_circle".equals(pass.getName())) {
                return pass.outTarget;
            }
        }
        return null;
    }

    private record CircleArea(float x, float y, float width, float height, float borderThickness, float roundness, int color) {
    }

}
