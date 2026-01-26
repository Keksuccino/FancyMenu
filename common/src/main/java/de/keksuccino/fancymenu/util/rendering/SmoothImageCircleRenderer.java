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

public final class SmoothImageCircleRenderer {

    private static final Logger LOGGER = LogManager.getLogger();
    // Keep shader files in the default 'minecraft' namespace so the vanilla resource manager finds them for every loader.
    private static final ResourceLocation GUI_SMOOTH_IMAGE_CIRCLE_POST_CHAIN = ResourceLocation.withDefaultNamespace("shaders/post/fancymenu_gui_smooth_image_circle.json");

    private static PostChain smoothImageCirclePostChain;
    private static boolean smoothImageCirclePostChainFailed;
    private static int cachedWidth = -1;
    private static int cachedHeight = -1;

    private SmoothImageCircleRenderer() {
    }

    /**
     * Renders a smooth superellipse image using the provided bounding rectangle.
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
    public static void renderSmoothImageCircle(@Nonnull GuiGraphics graphics, @Nonnull ResourceLocation texture, float x, float y, float width, float height, float roundness, int color, float partial) {
        renderSmoothImageCircleInternal(graphics, texture, x, y, width, height, roundness, TextureRegion.full(), color, partial);
    }

    public static void renderSmoothImageCircleScaled(@Nonnull GuiGraphics graphics, @Nonnull ResourceLocation texture, float x, float y, float width, float height, float roundness, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothImageCircle(graphics, texture, x * additionalScale + translationX, y * additionalScale + translationY, width * additionalScale, height * additionalScale, roundness, color, partial);
    }

    public static void renderSmoothImageCircle(@Nonnull GuiGraphics graphics, @Nonnull ResourceLocation texture, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight, float roundness, int color, float partial) {
        renderSmoothImageCircleInternal(graphics, texture, x, y, width, height, roundness, TextureRegion.of(uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight), color, partial);
    }

    public static void renderSmoothImageCircleScaled(@Nonnull GuiGraphics graphics, @Nonnull ResourceLocation texture, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight, float roundness, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothImageCircle(graphics, texture, x * additionalScale + translationX, y * additionalScale + translationY, width * additionalScale, height * additionalScale, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight, roundness, color, partial);
    }

    private static void renderSmoothImageCircleInternal(@Nonnull GuiGraphics graphics, @Nonnull ResourceLocation texture, float x, float y, float width, float height, float roundness, @Nonnull TextureRegion textureRegion, int color, float partial) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(texture);
        Objects.requireNonNull(textureRegion);
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        float clampedRoundness = Math.max(0.1F, roundness);
        _renderSmoothImageCircle(graphics, partial, new CircleArea(x, y, width, height, clampedRoundness, textureRegion, texture, color));
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

    private static void _renderSmoothImageCircle(GuiGraphics graphics, float partial, CircleArea area) {
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
        float scaledRoundness = area.roundness;

        float red = (float) FastColor.ARGB32.red(area.color) / 255.0F;
        float green = (float) FastColor.ARGB32.green(area.color) / 255.0F;
        float blue = (float) FastColor.ARGB32.blue(area.color) / 255.0F;
        float alpha = (float) FastColor.ARGB32.alpha(area.color) / 255.0F;

        RenderRotationUtil.Rotation2D invRotation = RenderRotationUtil.getCurrentAdditionalRenderInverseRotation2D();
        int textureId = minecraft.getTextureManager().getTexture(area.texture).getId();
        applyUniforms(postChain, area.textureRegion, scaledX, scaledY, scaledWidth, scaledHeight, scaledRoundness, invRotation, red, green, blue, alpha, textureId);

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
        if (smoothImageCirclePostChainFailed) {
            return null;
        }
        if (smoothImageCirclePostChain == null) {
            try {
                smoothImageCirclePostChain = new PostChain(
                        minecraft.getTextureManager(),
                        minecraft.getResourceManager(),
                        minecraft.getMainRenderTarget(),
                        GUI_SMOOTH_IMAGE_CIRCLE_POST_CHAIN
                );
                cachedWidth = -1;
                cachedHeight = -1;
                ensurePostChainSize(smoothImageCirclePostChain, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            } catch (Exception ex) {
                smoothImageCirclePostChainFailed = true;
                LOGGER.error("[FANCYMENU] Failed to load GUI smooth image circle shader!", ex);
                return null;
            }
        }
        return smoothImageCirclePostChain;
    }

    private static void ensurePostChainSize(PostChain postChain, int width, int height) {
        if (width != cachedWidth || height != cachedHeight) {
            cachedWidth = width;
            cachedHeight = height;
            postChain.resize(width, height);
        }
    }

    private static void applyUniforms(PostChain postChain, TextureRegion textureRegion, float x, float y, float width, float height, float roundness, RenderRotationUtil.Rotation2D invRotation, float red, float green, float blue, float alpha, int textureId) {
        List<PostPass> passes = ((IMixinPostChain) postChain).getPasses_FancyMenu();
        for (PostPass pass : passes) {
            if (!"fancymenu_gui_smooth_image_circle".equals(pass.getName())) {
                continue;
            }
            pass.getEffect().safeGetUniform("Rect").set(x, y, width, height);
            pass.getEffect().safeGetUniform("Roundness").set(roundness);
            pass.getEffect().safeGetUniform("InvRotation").set(invRotation.m00(), invRotation.m01(), invRotation.m10(), invRotation.m11());
            pass.getEffect().safeGetUniform("UvMin").set(textureRegion.minU(), textureRegion.minV());
            pass.getEffect().safeGetUniform("UvMax").set(textureRegion.maxU(), textureRegion.maxV());
            pass.getEffect().safeGetUniform("Color").set(red, green, blue, alpha);
            pass.getEffect().setSampler("ImageSampler", () -> textureId);
        }
    }

    private static RenderTarget getFinalTarget(PostChain postChain) {
        List<PostPass> passes = ((IMixinPostChain) postChain).getPasses_FancyMenu();
        for (int i = passes.size() - 1; i >= 0; i--) {
            PostPass pass = passes.get(i);
            if ("fancymenu_gui_smooth_image_circle".equals(pass.getName())) {
                return pass.outTarget;
            }
        }
        return null;
    }

    private record CircleArea(float x, float y, float width, float height, float roundness, TextureRegion textureRegion, ResourceLocation texture, int color) {
    }

    private record TextureRegion(float minU, float minV, float maxU, float maxV) {

        private static TextureRegion full() {
            return new TextureRegion(0.0F, 0.0F, 1.0F, 1.0F);
        }

        private static TextureRegion of(float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
            float safeTexWidth = textureWidth == 0.0F ? 1.0F : textureWidth;
            float safeTexHeight = textureHeight == 0.0F ? 1.0F : textureHeight;
            float minU = uOffset / safeTexWidth;
            float minV = vOffset / safeTexHeight;
            float maxU = (uOffset + uWidth) / safeTexWidth;
            float maxV = (vOffset + vHeight) / safeTexHeight;
            return new TextureRegion(minU, minV, maxU, maxV);
        }
    }
}
