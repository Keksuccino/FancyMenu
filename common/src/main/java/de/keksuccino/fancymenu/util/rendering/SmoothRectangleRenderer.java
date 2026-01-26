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

public final class SmoothRectangleRenderer {

    private static final Logger LOGGER = LogManager.getLogger();
    // Keep shader files in the default 'minecraft' namespace so the vanilla resource manager finds them for every loader.
    private static final ResourceLocation GUI_SMOOTH_RECT_POST_CHAIN = ResourceLocation.withDefaultNamespace("shaders/post/fancymenu_gui_smooth_rect.json");

    private static PostChain smoothRectPostChain;
    private static boolean smoothRectPostChainFailed;
    private static int cachedWidth = -1;
    private static int cachedHeight = -1;

    private SmoothRectangleRenderer() {
    }

    public static void renderSmoothRect(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.uniform(cornerRadius), color, partial);
    }

    public static void renderSmoothRectScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothRect(graphics, x * additionalScale + translationX, y * additionalScale + translationY, width * additionalScale, height * additionalScale, cornerRadius * additionalScale, color, partial);
    }

    public static void renderSmoothRectRoundTopCorners(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.topOnly(cornerRadius), color, partial);
    }

    public static void renderSmoothRectRoundTopCornersScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothRectRoundTopCorners(graphics, x * additionalScale + translationX, y * additionalScale + translationY, width * additionalScale, height * additionalScale, cornerRadius * additionalScale, color, partial);
    }

    public static void renderSmoothRectRoundBottomCorners(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.bottomOnly(cornerRadius), color, partial);
    }

    public static void renderSmoothRectRoundBottomCornersScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothRectRoundBottomCorners(graphics, x * additionalScale + translationX, y * additionalScale + translationY, width * additionalScale, height * additionalScale, cornerRadius * additionalScale, color, partial);
    }

    public static void renderSmoothRectRoundAllCorners(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), color, partial);
    }

    public static void renderSmoothRectRoundAllCornersScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothRectRoundAllCorners(
                graphics,
                x * additionalScale + translationX,
                y * additionalScale + translationY,
                width * additionalScale,
                height * additionalScale,
                topLeftRadius * additionalScale,
                topRightRadius * additionalScale,
                bottomRightRadius * additionalScale,
                bottomLeftRadius * additionalScale,
                color,
                partial
        );
    }

    public static void renderSmoothBorder(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, borderThickness, CornerRadii.uniform(cornerRadius), color, partial);
    }

    public static void renderSmoothBorderScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, float cornerRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothBorder(
                graphics,
                x * additionalScale + translationX,
                y * additionalScale + translationY,
                width * additionalScale,
                height * additionalScale,
                borderThickness * additionalScale,
                cornerRadius * additionalScale,
                color,
                partial
        );
    }

    public static void renderSmoothBorderRoundAllCorners(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, borderThickness, CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), color, partial);
    }

    public static void renderSmoothBorderRoundAllCornersScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothBorderRoundAllCorners(
                graphics,
                x * additionalScale + translationX,
                y * additionalScale + translationY,
                width * additionalScale,
                height * additionalScale,
                borderThickness * additionalScale,
                topLeftRadius * additionalScale,
                topRightRadius * additionalScale,
                bottomRightRadius * additionalScale,
                bottomLeftRadius * additionalScale,
                color,
                partial
        );
    }

    private static void renderSmoothRectInternal(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, @Nonnull CornerRadii cornerRadii, int color, float partial) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(cornerRadii);
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        _renderSmoothRect(graphics, partial, new RectArea(x, y, width, height, Math.max(0.0F, borderThickness), cornerRadii, color));
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

    private static void _renderSmoothRect(GuiGraphics graphics, float partial, RectArea area) {
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
        CornerRadii scaledRadii = area.cornerRadii.scaled(guiScale).clamped(Math.min(scaledWidth, scaledHeight) * 0.5F).flipVertical();

        float red = (float) FastColor.ARGB32.red(area.color) / 255.0F;
        float green = (float) FastColor.ARGB32.green(area.color) / 255.0F;
        float blue = (float) FastColor.ARGB32.blue(area.color) / 255.0F;
        float alpha = (float) FastColor.ARGB32.alpha(area.color) / 255.0F;

        RenderRotationUtil.Rotation2D rotation = RenderRotationUtil.getCurrentAdditionalRenderMaskRotation2D();
        applyUniforms(postChain, scaledX, scaledY, scaledWidth, scaledHeight, scaledBorderThickness, scaledRadii, rotation, red, green, blue, alpha);

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
        if (smoothRectPostChainFailed) {
            return null;
        }
        if (smoothRectPostChain == null) {
            try {
                smoothRectPostChain = new PostChain(
                        minecraft.getTextureManager(),
                        minecraft.getResourceManager(),
                        minecraft.getMainRenderTarget(),
                        GUI_SMOOTH_RECT_POST_CHAIN
                );
                cachedWidth = -1;
                cachedHeight = -1;
                ensurePostChainSize(smoothRectPostChain, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            } catch (Exception ex) {
                smoothRectPostChainFailed = true;
                LOGGER.error("[FANCYMENU] Failed to load GUI smooth rectangle shader!", ex);
                return null;
            }
        }
        return smoothRectPostChain;
    }

    private static void ensurePostChainSize(PostChain postChain, int width, int height) {
        if (width != cachedWidth || height != cachedHeight) {
            cachedWidth = width;
            cachedHeight = height;
            postChain.resize(width, height);
        }
    }

    private static void applyUniforms(PostChain postChain, float x, float y, float width, float height, float borderThickness, CornerRadii cornerRadii, RenderRotationUtil.Rotation2D rotation, float red, float green, float blue, float alpha) {
        List<PostPass> passes = ((IMixinPostChain) postChain).getPasses_FancyMenu();
        for (PostPass pass : passes) {
            if (!"fancymenu_gui_smooth_rect".equals(pass.getName())) {
                continue;
            }
            pass.getEffect().safeGetUniform("Rect").set(x, y, width, height);
            pass.getEffect().safeGetUniform("CornerRadii").set(cornerRadii.topLeft(), cornerRadii.topRight(), cornerRadii.bottomRight(), cornerRadii.bottomLeft());
            pass.getEffect().safeGetUniform("Rotation").set(rotation.m00(), rotation.m01(), rotation.m10(), rotation.m11());
            pass.getEffect().safeGetUniform("BorderThickness").set(borderThickness);
            pass.getEffect().safeGetUniform("Color").set(red, green, blue, alpha);
        }
    }

    private static RenderTarget getFinalTarget(PostChain postChain) {
        List<PostPass> passes = ((IMixinPostChain) postChain).getPasses_FancyMenu();
        for (int i = passes.size() - 1; i >= 0; i--) {
            PostPass pass = passes.get(i);
            if ("fancymenu_gui_smooth_rect".equals(pass.getName())) {
                return pass.outTarget;
            }
        }
        return null;
    }

    private record RectArea(float x, float y, float width, float height, float borderThickness, CornerRadii cornerRadii, int color) {
    }

    private record CornerRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {

        private static CornerRadii uniform(float radius) {
            return new CornerRadii(radius, radius, radius, radius);
        }

        private static CornerRadii topOnly(float radius) {
            return new CornerRadii(radius, radius, 0.0F, 0.0F);
        }

        private static CornerRadii bottomOnly(float radius) {
            return new CornerRadii(0.0F, 0.0F, radius, radius);
        }

        private static CornerRadii of(float topLeft, float topRight, float bottomRight, float bottomLeft) {
            return new CornerRadii(topLeft, topRight, bottomRight, bottomLeft);
        }

        private CornerRadii scaled(float factor) {
            return new CornerRadii(topLeft * factor, topRight * factor, bottomRight * factor, bottomLeft * factor);
        }

        private CornerRadii clamped(float maxRadius) {
            float clampedMax = Math.max(0.0F, maxRadius);
            return new CornerRadii(clampCorner(topLeft, clampedMax), clampCorner(topRight, clampedMax), clampCorner(bottomRight, clampedMax), clampCorner(bottomLeft, clampedMax));
        }

        private CornerRadii flipVertical() {
            return new CornerRadii(bottomLeft, bottomRight, topRight, topLeft);
        }

        private static float clampCorner(float value, float max) {
            if (value <= 0.0F) {
                return 0.0F;
            }
            return value > max ? max : value;
        }
    }

}
