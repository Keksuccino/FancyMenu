package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.runtime;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframeInterpolator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RunningElementAnimationTest {

    @Test
    void restoresEveryPropertyActuallyAnimatedInAbsoluteMode() {
        AnimationControllerElement controller = element(10, 20, 30, 40);
        RunningElementAnimation animation = animation(controller);

        animation.apply(values(100, 200, 300, 400));
        animation.restoreOriginalState();

        assertEquals(10, controller.posOffsetX);
        assertEquals(20, controller.posOffsetY);
        assertEquals(30, controller.baseWidth);
        assertEquals(40, controller.baseHeight);
    }

    @Test
    void restoresSizeAndAnimatedOffsetsInOffsetMode() {
        AnimationControllerElement controller = element(10, 20, 30, 40);
        controller.offsetMode = true;
        RunningElementAnimation animation = animation(controller);

        animation.apply(values(100, 200, 300, 400));
        animation.restoreOriginalState();

        assertEquals(0, controller.animatedOffsetX);
        assertEquals(0, controller.animatedOffsetY);
        assertEquals(30, controller.baseWidth);
        assertEquals(40, controller.baseHeight);
    }

    @Test
    void doesNotRestorePropertiesTheControllerIgnored() {
        AnimationControllerElement controller = element(10, 20, 30, 40);
        controller.ignorePosition = true;
        controller.ignoreSize = true;
        RunningElementAnimation animation = animation(controller);
        controller.posOffsetX = 50;
        controller.baseWidth = 60;

        animation.apply(values(100, 200, 300, 400));
        animation.restoreOriginalState();

        assertEquals(50, controller.posOffsetX);
        assertEquals(60, controller.baseWidth);
    }

    @Test
    void replacingTargetRestoresOldTargetAndCapturesReplacementBaseline() {
        AnimationControllerElement controller = element(0, 0, 0, 0);
        AnimationControllerElement originalTarget = element(10, 20, 30, 40);
        AnimationControllerElement replacementTarget = element(50, 60, 70, 80);
        AnimationKeyframe keyframe = new AnimationKeyframe(0L, 0, 0, 0, 0, ElementAnchorPoints.TOP_LEFT, false);
        RunningElementAnimation animation = new RunningElementAnimation(List.of(keyframe), 0L, originalTarget, controller);

        animation.apply(values(100, 200, 300, 400));
        animation.updateTargetElement(replacementTarget);

        assertEquals(10, originalTarget.posOffsetX);
        assertEquals(20, originalTarget.posOffsetY);
        assertEquals(30, originalTarget.baseWidth);
        assertEquals(40, originalTarget.baseHeight);

        animation.apply(values(500, 600, 700, 800));
        animation.restoreOriginalState();

        assertEquals(50, replacementTarget.posOffsetX);
        assertEquals(60, replacementTarget.posOffsetY);
        assertEquals(70, replacementTarget.baseWidth);
        assertEquals(80, replacementTarget.baseHeight);
    }

    private static RunningElementAnimation animation(AnimationControllerElement controller) {
        AnimationKeyframe keyframe = new AnimationKeyframe(0L, 0, 0, 0, 0, ElementAnchorPoints.TOP_LEFT, false);
        return new RunningElementAnimation(List.of(keyframe), 0L, controller, controller);
    }

    private static AnimationControllerElement element(int x, int y, int width, int height) {
        AnimationControllerElement element = new AnimationControllerElement(new AnimationControllerElementBuilder());
        element.posOffsetX = x;
        element.posOffsetY = y;
        element.baseWidth = width;
        element.baseHeight = height;
        element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        return element;
    }

    private static AnimationKeyframeInterpolator.Values values(int x, int y, int width, int height) {
        return new AnimationKeyframeInterpolator.Values(x, y, width, height, ElementAnchorPoints.BOTTOM_RIGHT, true);
    }

}
