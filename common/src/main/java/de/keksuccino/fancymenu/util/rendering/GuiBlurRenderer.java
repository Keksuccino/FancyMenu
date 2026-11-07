package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphicsExtractor;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Renders blurred, tinted areas of whatever the GUI rendered below them.
 *
 * <p>Blur radii are in GUI pixels and keep their look at every GUI scale. Areas get queued into the GUI render state and run
 * through {@link GuiBlurPasses} when the GUI renderer reaches them.</p>
 */
public final class GuiBlurRenderer {

    private static final float SHAPE_TYPE_ROUNDED_RECT = 0.0F;
    private static final float SHAPE_TYPE_SUPERELLIPSE = 1.0F;
    private static final GuiBlurPlan PLAN = new GuiBlurPlan();
    private static final GuiBlurPlan.Region COMPOSITE_REGION = new GuiBlurPlan.Region();
    private static final GuiBlurKernel.Kernel NO_BLUR = GuiBlurKernel.resolve(0.0F);

    private GuiBlurRenderer() {
    }

    /**
     * Queues a blur area that samples the current framebuffer contents when the GUI render state reaches it.
     *
     * @param graphics The GuiGraphicsExtractor for the current render pass.
     * @param x The X position in GUI pixels (top-left origin). Recommended range: 0 to screen width.
     * @param y The Y position in GUI pixels (top-left origin). Recommended range: 0 to screen height.
     * @param width The width in GUI pixels. Recommended range: 1 to screen width.
     * @param height The height in GUI pixels. Recommended range: 1 to screen height.
     * @param blurRadius The blur radius in GUI pixels (4 is a good default). Recommended range: 0 to {@link GuiBlurRadius#MAX_RADIUS}; the GPU work stays bounded by {@link GuiBlurRadius#MAX_FRAMEBUFFER_RADIUS} framebuffer pixels.
     * @param cornerRadius The rounded corner radius in GUI pixels. Recommended range: 0 to min(width, height) / 2 (6 is a good default).
     * @param tint The tint color that is mixed into the blurred area. Use alpha to control strength (0.15 is a good default).
     * @param partial Partial tick for the frame; pass the current render partial.
     *
     * Example defaults: x=0, y=0, width=200, height=100, blurRadius=4, cornerRadius=6, tint=DrawableColor.of(0, 0, 0, 38)
     */
    public static void renderBlurArea(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, blurRadius, CornerRadii.uniform(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurArea(graphics, area.x(), area.y(), area.width(), area.height(), blurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Renders a blur area with only the top-left and top-right corners rounded.
     */
    public static void renderBlurAreaRoundTopCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, blurRadius, CornerRadii.topOnly(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaRoundTopCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaRoundTopCorners(graphics, area.x(), area.y(), area.width(), area.height(), blurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Renders a blur area with only the bottom-left and bottom-right corners rounded.
     */
    public static void renderBlurAreaRoundBottomCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, blurRadius, CornerRadii.bottomOnly(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaRoundBottomCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaRoundBottomCorners(graphics, area.x(), area.y(), area.width(), area.height(), blurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Renders a blur area with individually specified corner radii (top-left, top-right, bottom-right, bottom-left).
     */
    public static void renderBlurAreaRoundAllCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, blurRadius, CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), tint, partial);
    }

    public static void renderBlurAreaRoundAllCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaRoundAllCornersScaled(graphics, x, y, width, height, blurRadius, topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius, tint, 1.0F, partial);
    }

    /**
     * Renders a blur area with individually specified corner radii (top-left, top-right, bottom-right, bottom-left) that fades with {@code opacity}.
     *
     * @param opacity Fades the whole blur area, including its tint, in and out (0 to 1). It keeps the blur radius, so fades don't make the blur look sharper.
     */
    public static void renderBlurAreaRoundAllCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @Nonnull DrawableColor tint, float opacity, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        float scale = transform.scale();
        CornerRadii cornerRadii = CornerRadii.of(topLeftRadius * scale, topRightRadius * scale, bottomRightRadius * scale, bottomLeftRadius * scale);
        queueBlurArea(graphics, new BlurArea(area.x(), area.y(), area.width(), area.height(), blurRadius * scale, cornerRadii, SHAPE_TYPE_ROUNDED_RECT, 2.0F, tint, 0.0F, DrawableColor.FULLY_TRANSPARENT, opacity));
    }

    /**
     * Renders a blur area as a smooth superellipse (circle/oval) using the provided bounding rectangle.
     *
     * <p>Roundness examples:
     * <br>- Perfect circle/ellipse: {@code roundness = 2.0}
     * <br>- Squircle-like (boxier): {@code roundness = 4.0}
     * <br>- Diamond-like (pointier): {@code roundness = 1.0}
     */
    public static void renderBlurAreaCircle(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float roundness, @Nonnull DrawableColor tint, float partial) {
        queueBlurArea(graphics, new BlurArea(x, y, width, height, blurRadius, CornerRadii.uniform(0.0F), SHAPE_TYPE_SUPERELLIPSE, clampRoundness(roundness), tint, 0.0F, DrawableColor.FULLY_TRANSPARENT, 1.0F));
    }

    public static void renderBlurAreaCircleScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float roundness, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaCircleScaled(graphics, x, y, width, height, blurRadius, roundness, tint, 1.0F, partial);
    }

    /**
     * Renders a superellipse blur area that fades with {@code opacity}.
     *
     * @param opacity Fades the whole blur area, including its tint, in and out (0 to 1). It keeps the blur radius, so fades don't make the blur look sharper.
     */
    public static void renderBlurAreaCircleScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, float roundness, @Nonnull DrawableColor tint, float opacity, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        queueBlurArea(graphics, new BlurArea(area.x(), area.y(), area.width(), area.height(), blurRadius * transform.scale(), CornerRadii.uniform(0.0F), SHAPE_TYPE_SUPERELLIPSE, clampRoundness(roundness), tint, 0.0F, DrawableColor.FULLY_TRANSPARENT, opacity));
    }

    /**
     * Converts a blur radius in framebuffer pixels to the GUI coordinate space used by blur areas.
     */
    public static float convertFramebufferBlurRadiusToGui(float framebufferBlurRadius) {
        return (float)(framebufferBlurRadius / getEffectiveGuiScale_FancyMenu());
    }

    /**
     * Renders a blur area using FancyMenu's blur intensity setting (a normalized multiplier, e.g., 0.25–3.0).
     * Callers provide the base radius they would normally use; this helper applies the current intensity
     * and renders the blur so UI code doesn’t need to recompute the radius everywhere.
     */
    public static void renderBlurAreaWithIntensity(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, applyBlurIntensity(baseBlurRadius), CornerRadii.uniform(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaWithIntensityScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaWithIntensity(graphics, area.x(), area.y(), area.width(), area.height(), baseBlurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Blur area using FancyMenu's blur intensity with only the top corners rounded.
     */
    public static void renderBlurAreaWithIntensityRoundTopCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, applyBlurIntensity(baseBlurRadius), CornerRadii.topOnly(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaWithIntensityRoundTopCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaWithIntensityRoundTopCorners(graphics, area.x(), area.y(), area.width(), area.height(), baseBlurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Blur area using FancyMenu's blur intensity with only the bottom corners rounded.
     */
    public static void renderBlurAreaWithIntensityRoundBottomCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, applyBlurIntensity(baseBlurRadius), CornerRadii.bottomOnly(cornerRadius), tint, partial);
    }

    public static void renderBlurAreaWithIntensityRoundBottomCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        renderBlurAreaWithIntensityRoundBottomCorners(graphics, area.x(), area.y(), area.width(), area.height(), baseBlurRadius * transform.scale(), cornerRadius * transform.scale(), tint, partial);
    }

    /**
     * Blur area using FancyMenu's blur intensity with individually specified corner radii (top-left, top-right, bottom-right, bottom-left).
     */
    public static void renderBlurAreaWithIntensityRoundAllCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @Nonnull DrawableColor tint, float partial) {
        renderBlurAreaInternal(graphics, x, y, width, height, applyBlurIntensity(baseBlurRadius), CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), tint, partial);
    }

    public static void renderBlurAreaWithIntensityRoundAllCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @Nonnull DrawableColor tint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        float scale = transform.scale();
        renderBlurAreaWithIntensityRoundAllCorners(graphics, area.x(), area.y(), area.width(), area.height(), baseBlurRadius * scale, topLeftRadius * scale, topRightRadius * scale, bottomRightRadius * scale, bottomLeftRadius * scale, tint, partial);
    }

    /**
     * Blur area using FancyMenu's blur intensity with a header band, e.g. a window with its title bar. The whole area shares one blur,
     * which is cheaper than separate areas and keeps the header from blurring the content below it.
     *
     * @param headerHeight Height of the band at the area's top in GUI pixels.
     * @param headerTint Tint of the header band; {@code tint} applies to the rest of the area.
     */
    public static void renderBlurAreaWithIntensityAndHeader(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @Nonnull DrawableColor tint, float headerHeight, @Nonnull DrawableColor headerTint, float partial) {
        queueBlurArea(graphics, new BlurArea(x, y, width, height, applyBlurIntensity(baseBlurRadius), CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), SHAPE_TYPE_ROUNDED_RECT, 2.0F, tint, headerHeight, headerTint, 1.0F));
    }

    public static void renderBlurAreaWithIntensityAndHeaderScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float baseBlurRadius, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, @Nonnull DrawableColor tint, float headerHeight, @Nonnull DrawableColor headerTint, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        GuiPoseTransformUtil.TransformedArea area = transform.transformArea(x, y, width, height);
        float scale = transform.scale();
        renderBlurAreaWithIntensityAndHeader(graphics, area.x(), area.y(), area.width(), area.height(), baseBlurRadius * scale, topLeftRadius * scale, topRightRadius * scale, bottomRightRadius * scale, bottomLeftRadius * scale, tint, headerHeight * scale, headerTint, partial);
    }

    private static float applyBlurIntensity(float baseBlurRadius) {
        return Math.max(0.0F, baseBlurRadius * Math.max(0.0F, FancyMenu.getOptions().uiBlurIntensity.getValue()));
    }

    private static void renderBlurAreaInternal(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float blurRadius, @Nonnull CornerRadii cornerRadii, @Nonnull DrawableColor tint, float partial) {
        queueBlurArea(graphics, new BlurArea(x, y, width, height, blurRadius, cornerRadii, SHAPE_TYPE_ROUNDED_RECT, 2.0F, tint, 0.0F, DrawableColor.FULLY_TRANSPARENT, 1.0F));
    }

    private static void queueBlurArea(@Nonnull GuiGraphicsExtractor graphics, @Nonnull BlurArea area) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(area.tint);
        Objects.requireNonNull(area.headerTint);
        if (!(area.width > 0.0F) || !(area.height > 0.0F) || !(area.opacity > 0.0F) || !area.hasVisibleEffect()) {
            return;
        }
        IMixinGuiGraphicsExtractor accessor = (IMixinGuiGraphicsExtractor) graphics;
        RenderRotationUtil.Rotation2D maskRotation = GuiPoseTransformUtil.resolve(graphics).rotation();
        ScreenRectangle bounds = toScreenBounds(area);
        ScreenRectangle clip = accessor.get_scissorStack_FancyMenu().peek();
        if (clip != null && isRotationIdentity(maskRotation)) {
            bounds = bounds.intersection(clip);
            if (bounds == null) {
                return;
            }
        }
        GuiRenderState renderState = accessor.get_guiRenderState_FancyMenu();
        renderState.nextStratum();
        renderState.addGuiElement(new GuiBlurRenderState(new QueuedBlurArea(area, maskRotation, clip), bounds));
        renderState.nextStratum();
        RenderingUtils.resetShaderColor(graphics);
    }

    public static void executeQueuedBlurArea_FancyMenu(@Nonnull QueuedBlurArea queuedBlurArea) {
        Objects.requireNonNull(queuedBlurArea);
        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        int targetWidth = mainTarget.width;
        int targetHeight = mainTarget.height;
        if (targetWidth <= 0 || targetHeight <= 0) {
            return;
        }

        BlurArea area = queuedBlurArea.area_FancyMenu;
        double guiScale = getEffectiveGuiScale_FancyMenu();
        float scaledWidth = (float)(area.width * guiScale);
        float scaledHeight = (float)(area.height * guiScale);
        float scaledX = (float)(area.x * guiScale);
        float scaledY = (float)(targetHeight - area.y * guiScale - area.height * guiScale);
        if (!Float.isFinite(scaledX) || !Float.isFinite(scaledY) || !Float.isFinite(scaledWidth) || !Float.isFinite(scaledHeight) || scaledWidth <= 0.0F || scaledHeight <= 0.0F) {
            return;
        }

        RenderRotationUtil.Rotation2D rotation = queuedBlurArea.maskRotation_FancyMenu;
        if (!GuiBlurPlan.resolveCompositeRegion(COMPOSITE_REGION, area.x, area.y, area.width, area.height, rotation.m00(), rotation.m01(), rotation.m10(), rotation.m11(), guiScale, targetWidth, targetHeight)) {
            return;
        }
        ScreenRectangle clip = queuedBlurArea.clip_FancyMenu;
        if (clip != null && !GuiBlurPlan.clipToGuiScissor(COMPOSITE_REGION, clip.left(), clip.top(), clip.right(), clip.bottom(), guiScale, targetWidth, targetHeight)) {
            return;
        }

        DrawableColor.FloatColor tint = area.tint.getAsFloats();
        DrawableColor.FloatColor headerTint = area.headerTint.getAsFloats();
        float headerHeight = Math.min(scaledHeight, Math.max(0.0F, (float)(area.headerHeight * guiScale)));
        boolean hasHeader = headerHeight > 0.0F;
        GuiBlurKernel.Kernel kernel = GuiBlurKernel.resolve(GuiBlurRadius.resolveFramebufferRadius(area.blurRadius, guiScale));
        // A fully opaque tint hides the blur, so only sample it where some of it stays visible.
        boolean sampleBlur = kernel.hasBlur() && (tint.alpha() < 1.0F || (hasHeader && headerTint.alpha() < 1.0F));
        if (!sampleBlur && tint.alpha() <= 0.0F && (!hasHeader || headerTint.alpha() <= 0.0F)) {
            return;
        }
        if (!PLAN.prepare(COMPOSITE_REGION, sampleBlur ? kernel : NO_BLUR, targetWidth, targetHeight)) {
            return;
        }

        CornerRadii radii = area.cornerRadii.scaled(guiScale).clamped(Math.min(scaledWidth, scaledHeight) * 0.5F);
        GuiBlurPasses.render(mainTarget, PLAN, sampleBlur, new GuiBlurPasses.CompositeShape(scaledX, scaledY, scaledWidth, scaledHeight, radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft(), rotation, tint, headerTint, area.shapeType, area.roundness, headerHeight, Math.min(1.0F, area.opacity)));
    }

    /**
     * Releases the blur's GPU resources once blur areas stopped rendering for a while. Gets called once per frame.
     */
    public static void onFrameStart_FancyMenu() {
        GuiBlurPasses.releaseIfIdle();
    }

    public static void close_FancyMenu() {
        GuiBlurPasses.close();
    }

    private static ScreenRectangle toScreenBounds(BlurArea area) {
        int x0 = (int) Math.floor(area.x);
        int y0 = (int) Math.floor(area.y);
        int x1 = (int) Math.ceil(area.x + area.width);
        int y1 = (int) Math.ceil(area.y + area.height);
        return new ScreenRectangle(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
    }

    private static boolean isRotationIdentity(@Nonnull RenderRotationUtil.Rotation2D rotation) {
        return Math.abs(rotation.m00() - 1.0F) <= 1.0E-4F && Math.abs(rotation.m11() - 1.0F) <= 1.0E-4F && Math.abs(rotation.m01()) <= 1.0E-4F && Math.abs(rotation.m10()) <= 1.0E-4F;
    }

    private static float clampRoundness(float roundness) {
        if (!Float.isFinite(roundness)) {
            return 2.0F;
        }
        return Math.max(0.1F, roundness);
    }

    /**
     * Blur areas are extracted in the same logical coordinate space as FancyMenu's precise GUI projection.
     * Keep every framebuffer conversion on this scale; Window#getGuiScale() only exposes its floored integer part while a fractional scale is active.
     */
    private static double getEffectiveGuiScale_FancyMenu() {
        double guiScale = WindowHandler.getGuiScale();
        return Double.isFinite(guiScale) && guiScale > 0.0D ? guiScale : 1.0D;
    }

    private record BlurArea(float x, float y, float width, float height, float blurRadius, CornerRadii cornerRadii, float shapeType, float roundness, DrawableColor tint, float headerHeight, DrawableColor headerTint, float opacity) {

        private boolean hasVisibleEffect() {
            return this.blurRadius > 0.0F || this.tint.getColor().getAlpha() > 0 || (this.headerHeight > 0.0F && this.headerTint.getColor().getAlpha() > 0);
        }

    }

    public static final class QueuedBlurArea {

        private final BlurArea area_FancyMenu;
        private final RenderRotationUtil.Rotation2D maskRotation_FancyMenu;
        @Nullable
        private final ScreenRectangle clip_FancyMenu;

        private QueuedBlurArea(BlurArea area, RenderRotationUtil.Rotation2D maskRotation, @Nullable ScreenRectangle clip) {
            this.area_FancyMenu = area;
            this.maskRotation_FancyMenu = maskRotation;
            this.clip_FancyMenu = clip;
        }

    }

    public record GuiBlurRenderState(QueuedBlurArea queuedBlurArea, ScreenRectangle bounds) implements GuiRenderPhaseAction {

        @Override
        public void executeRender_FancyMenu() {
            executeQueuedBlurArea_FancyMenu(this.queuedBlurArea);
        }

        @Override
        @Nullable
        public ScreenRectangle scissorArea() {
            return this.queuedBlurArea.clip_FancyMenu;
        }

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

        private CornerRadii scaled(double factor) {
            return new CornerRadii((float)(topLeft * factor), (float)(topRight * factor), (float)(bottomRight * factor), (float)(bottomLeft * factor));
        }

        private CornerRadii clamped(float maxRadius) {
            float clampedMax = Math.max(0.0F, maxRadius);
            return new CornerRadii(clampCorner(topLeft, clampedMax), clampCorner(topRight, clampedMax), clampCorner(bottomRight, clampedMax), clampCorner(bottomLeft, clampedMax));
        }

        private static float clampCorner(float value, float max) {
            if (value <= 0.0F) {
                return 0.0F;
            }
            return value > max ? max : value;
        }
    }

}
