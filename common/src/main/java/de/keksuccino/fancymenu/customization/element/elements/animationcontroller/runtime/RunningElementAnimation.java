package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.runtime;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframeInterpolator;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframeSequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Runtime state for one controller-target pair. */
final class RunningElementAnimation {

    private final List<AnimationKeyframe> keyframes;
    private final long startTime;
    private final AnimationControllerElement controller;
    private AbstractElement targetElement;
    private OriginalElementState originalState;
    private boolean absolutePositionAnimated;
    private boolean offsetPositionAnimated;
    private boolean sizeAnimated;

    RunningElementAnimation(@NotNull List<AnimationKeyframe> keyframes, long startTime, @NotNull AbstractElement targetElement, @NotNull AnimationControllerElement controller) {
        List<AnimationKeyframe> sortedKeyframes = new ArrayList<>(keyframes);
        AnimationKeyframeSequence.sort(sortedKeyframes);
        this.keyframes = List.copyOf(sortedKeyframes);
        this.startTime = startTime;
        this.controller = controller;
        this.targetElement = targetElement;
        this.originalState = OriginalElementState.capture(targetElement);
    }

    @NotNull
    List<AnimationKeyframe> getKeyframes() {
        return this.keyframes;
    }

    long getElapsedTime(long currentTime) {
        return currentTime - this.startTime;
    }

    @NotNull
    AnimationControllerElement getController() {
        return this.controller;
    }

    @NotNull
    AbstractElement getTargetElement() {
        return this.targetElement;
    }

    void updateTargetElement(@NotNull AbstractElement targetElement) {
        if (this.targetElement == targetElement) return;
        this.restoreOriginalState();
        this.targetElement = targetElement;
        this.originalState = OriginalElementState.capture(targetElement);
    }

    void apply(@NotNull AnimationKeyframeInterpolator.Values values) {
        if (!this.controller.ignorePosition) {
            this.absolutePositionAnimated |= !this.controller.offsetMode;
            this.offsetPositionAnimated |= this.controller.offsetMode;
        }
        this.sizeAnimated |= !this.controller.ignoreSize;
        AnimationKeyframeInterpolator.apply(values, this.targetElement, this.controller.offsetMode, this.controller.ignorePosition, this.controller.ignoreSize);
    }

    void apply(@NotNull AnimationKeyframe keyframe) {
        this.apply(AnimationKeyframeInterpolator.interpolate(keyframe, keyframe, 0.0F));
    }

    void restoreOriginalState() {
        this.originalState.restore(this.targetElement, this.absolutePositionAnimated, this.offsetPositionAnimated, this.sizeAnimated);
        this.absolutePositionAnimated = false;
        this.offsetPositionAnimated = false;
        this.sizeAnimated = false;
    }

    private record OriginalElementState(int posOffsetX, int posOffsetY, int baseWidth, int baseHeight, @NotNull ElementAnchorPoint anchorPoint, boolean stickyAnchor) {

        @NotNull
        private static OriginalElementState capture(@NotNull AbstractElement element) {
            return new OriginalElementState(element.posOffsetX, element.posOffsetY, element.baseWidth, element.baseHeight, element.anchorPoint, element.stickyAnchor);
        }

        private void restore(@NotNull AbstractElement element, boolean absolutePositionAnimated, boolean offsetPositionAnimated, boolean sizeAnimated) {
            if (absolutePositionAnimated) {
                element.posOffsetX = this.posOffsetX;
                element.posOffsetY = this.posOffsetY;
                element.anchorPoint = this.anchorPoint;
                element.stickyAnchor = this.stickyAnchor;
            }
            if (sizeAnimated) {
                element.baseWidth = this.baseWidth;
                element.baseHeight = this.baseHeight;
            }
            if (offsetPositionAnimated) {
                element.animatedOffsetX = 0;
                element.animatedOffsetY = 0;
            }
        }

    }

}
