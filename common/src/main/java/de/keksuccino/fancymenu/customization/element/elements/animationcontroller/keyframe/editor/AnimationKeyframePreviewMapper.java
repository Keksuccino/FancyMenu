package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframeInterpolator;
import org.jetbrains.annotations.NotNull;

/** Maps preview element values between serialized and on-screen coordinate spaces. */
final class AnimationKeyframePreviewMapper {

    private final AnimationPreviewViewport viewport;

    public AnimationKeyframePreviewMapper(@NotNull AnimationPreviewViewport viewport) {
        this.viewport = viewport;
    }

    public void capture(@NotNull KeyframePreviewElement element, @NotNull AnimationKeyframe keyframe, boolean offsetMode) {
        if (offsetMode) {
            int screenCenterX = this.viewport.getDisplayWidth() / 2;
            int screenCenterY = this.viewport.getDisplayHeight() / 2;
            int elementCenterX = element.getAbsoluteX() + (element.getAbsoluteWidth() / 2);
            int elementCenterY = element.getAbsoluteY() + (element.getAbsoluteHeight() / 2);
            keyframe.posOffsetX = this.viewport.toSourceX(elementCenterX - screenCenterX);
            keyframe.posOffsetY = this.viewport.toSourceY(elementCenterY - screenCenterY);
        } else {
            keyframe.posOffsetX = this.viewport.toSourceX(element.posOffsetX);
            keyframe.posOffsetY = this.viewport.toSourceY(element.posOffsetY);
        }
        keyframe.baseWidth = this.viewport.toSourceX(element.baseWidth);
        keyframe.baseHeight = this.viewport.toSourceY(element.baseHeight);
        keyframe.anchorPoint = offsetMode ? ElementAnchorPoints.MID_CENTERED : element.anchorPoint;
        keyframe.stickyAnchor = offsetMode || element.stickyAnchor;
    }

    public void apply(@NotNull AnimationKeyframe keyframe, @NotNull KeyframePreviewElement element, boolean offsetMode) {
        this.apply(AnimationKeyframeInterpolator.interpolate(keyframe, keyframe, 0.0F), element, offsetMode);
    }

    public void apply(@NotNull AnimationKeyframeInterpolator.Values values, @NotNull KeyframePreviewElement element, boolean offsetMode) {
        if (offsetMode) {
            element.animatedOffsetX = this.viewport.toDisplayX(values.posOffsetX());
            element.animatedOffsetY = this.viewport.toDisplayY(values.posOffsetY());
            element.posOffsetX = 0;
            element.posOffsetY = 0;
        } else {
            element.animatedOffsetX = 0;
            element.animatedOffsetY = 0;
            element.posOffsetX = this.viewport.toDisplayX(values.posOffsetX());
            element.posOffsetY = this.viewport.toDisplayY(values.posOffsetY());
        }
        element.baseWidth = this.viewport.toDisplayX(values.baseWidth());
        element.baseHeight = this.viewport.toDisplayY(values.baseHeight());
        element.anchorPoint = offsetMode ? ElementAnchorPoints.MID_CENTERED : values.anchorPoint();
        element.stickyAnchor = offsetMode || values.stickyAnchor();
    }

}
