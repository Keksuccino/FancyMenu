package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import org.jetbrains.annotations.NotNull;

/**
 * Interpolates keyframe values without owning playback state.
 *
 * <p>Anchor properties are intentionally taken from the upcoming frame, matching the controller's established
 * behavior. They are discrete layout modes and cannot be meaningfully blended.</p>
 */
public final class AnimationKeyframeInterpolator {

    private AnimationKeyframeInterpolator() {
    }

    @NotNull
    public static Values interpolate(@NotNull AnimationKeyframe current, @NotNull AnimationKeyframe next, float progress) {
        float clampedProgress = Math.max(0.0F, Math.min(1.0F, progress));
        ElementAnchorPoint anchorPoint = next.anchorPoint != null ? next.anchorPoint : (current.anchorPoint != null ? current.anchorPoint : ElementAnchorPoints.TOP_LEFT);
        return new Values(interpolate(current.posOffsetX, next.posOffsetX, clampedProgress), interpolate(current.posOffsetY, next.posOffsetY, clampedProgress), interpolate(current.baseWidth, next.baseWidth, clampedProgress), interpolate(current.baseHeight, next.baseHeight, clampedProgress), anchorPoint, next.stickyAnchor);
    }

    public static void apply(@NotNull Values values, @NotNull AbstractElement target, boolean offsetMode, boolean ignorePosition, boolean ignoreSize) {
        if (!ignorePosition) {
            if (offsetMode) {
                target.animatedOffsetX = values.posOffsetX();
                target.animatedOffsetY = values.posOffsetY();
            } else {
                target.posOffsetX = values.posOffsetX();
                target.posOffsetY = values.posOffsetY();
                target.anchorPoint = values.anchorPoint();
                target.stickyAnchor = values.stickyAnchor();
            }
        }
        if (!ignoreSize) {
            target.baseWidth = values.baseWidth();
            target.baseHeight = values.baseHeight();
        }
    }

    private static int interpolate(int start, int end, float progress) {
        return (int)(start + (((double)end - start) * progress));
    }

    public record Values(int posOffsetX, int posOffsetY, int baseWidth, int baseHeight, @NotNull ElementAnchorPoint anchorPoint, boolean stickyAnchor) {
    }

}
