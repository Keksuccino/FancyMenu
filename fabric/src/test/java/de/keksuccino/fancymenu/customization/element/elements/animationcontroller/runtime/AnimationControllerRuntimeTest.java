package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.runtime;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationControllerRuntimeTest {

    @BeforeEach
    @AfterEach
    void clearRuntime() {
        AnimationControllerRuntime.stopAllAnimations();
        AnimationControllerRuntime.clearMemory();
    }

    @Test
    void resettingAnotherControllerDoesNotCancelTheActiveOwner() {
        TestController target = controller("target");
        TestController owner = controller("owner");
        TestController other = controller("other");
        target.posOffsetX = 7;
        target.baseWidth = 13;
        owner.targetElements.add(new AnimationControllerElement.TargetElement("target"));
        other.targetElements.add(new AnimationControllerElement.TargetElement("target"));
        other.targetElements.get(0).animationApplied = true;
        assertTrue(AnimationControllerRuntime.applyAnimation(owner, owner.targetElements.get(0), target, 1_000L));
        AnimationControllerRuntime.tick(1_050L);

        AnimationControllerRuntime.resetController(other);
        AnimationControllerRuntime.resetController(other);

        assertTrue(AnimationControllerRuntime.isAnimating("target"));
        assertTrue(AnimationControllerRuntime.wasAnimatedInThePast("target"));
        assertEquals(50, target.posOffsetX);
        assertFalse(other.targetElements.get(0).animationApplied);
        AnimationControllerRuntime.tick(1_075L);
        assertEquals(75, target.posOffsetX);
        AnimationControllerRuntime.resetController(owner);
        assertFalse(AnimationControllerRuntime.isAnimating("target"));
        assertFalse(AnimationControllerRuntime.wasAnimatedInThePast("target"));
        assertEquals(7, target.posOffsetX);
        assertEquals(13, target.baseWidth);
    }

    @Test
    void competingControllerDoesNotClaimAnAnimationItDoesNotOwn() {
        TestController target = controller("target");
        TestController owner = controller("owner");
        TestController other = controller("other");
        AnimationControllerElement.TargetElement config = new AnimationControllerElement.TargetElement("target");
        assertTrue(AnimationControllerRuntime.applyAnimation(owner, config, target));

        assertFalse(AnimationControllerRuntime.applyAnimation(other, config, target));
        assertFalse(AnimationControllerRuntime.wasAnimatedInThePast(other, "target"));
    }

    @Test
    void completionAndResetBelongToEachControllerIndependently() {
        TestController target = controller("target");
        TestController owner = controller("owner");
        TestController other = controller("other");
        AnimationControllerElement.TargetElement config = new AnimationControllerElement.TargetElement("target");
        owner.targetElements.add(config);
        other.targetElements.add(new AnimationControllerElement.TargetElement("target"));
        assertTrue(AnimationControllerRuntime.applyAnimation(owner, config, target, 1_000L));
        AnimationControllerRuntime.tick(1_100L);
        assertEquals(100, target.posOffsetX);
        assertFalse(AnimationControllerRuntime.isFinished(owner, "target"));
        AnimationControllerRuntime.tick(1_101L);

        AnimationControllerRuntime.resetController(other);
        assertTrue(AnimationControllerRuntime.wasAnimatedInThePast(owner, "target"));
        assertTrue(AnimationControllerRuntime.isFinished(owner, "target"));
        assertFalse(AnimationControllerRuntime.isFinished(other, "target"));
        assertTrue(AnimationControllerRuntime.applyAnimation(other, config, target, 2_000L));
        AnimationControllerRuntime.tick(2_101L);

        AnimationControllerRuntime.resetController(owner);
        assertFalse(AnimationControllerRuntime.isFinished(owner, "target"));
        assertTrue(AnimationControllerRuntime.isFinished(other, "target"));
        assertTrue(AnimationControllerRuntime.isFinished("target"));
        assertTrue(AnimationControllerRuntime.applyAnimation(owner, config, target, 3_000L));
        assertFalse(AnimationControllerRuntime.isFinished(owner, "target"));
        assertTrue(AnimationControllerRuntime.isFinished(other, "target"));
    }

    @Test
    void onlyTheOwnerCanReplaceTheLiveTargetAndResetItsBaseline() {
        TestController target = controller("target");
        target.posOffsetX = 7;
        TestController replacement = controller("target");
        replacement.posOffsetX = 9;
        TestController owner = controller("owner");
        TestController other = controller("other");
        AnimationControllerElement.TargetElement config = new AnimationControllerElement.TargetElement("target");
        assertTrue(AnimationControllerRuntime.applyAnimation(owner, config, target, 1_000L));
        AnimationControllerRuntime.tick(1_050L);

        assertFalse(AnimationControllerRuntime.applyAnimation(other, config, replacement, 1_050L));
        assertEquals(50, target.posOffsetX);
        assertEquals(9, replacement.posOffsetX);
        assertTrue(AnimationControllerRuntime.applyAnimation(owner, config, replacement, 1_050L));
        assertEquals(7, target.posOffsetX);
        AnimationControllerRuntime.tick(1_075L);
        assertEquals(75, replacement.posOffsetX);

        TestController rebuiltOwner = controller("owner");
        rebuiltOwner.targetElements.add(config);
        AnimationControllerRuntime.resetController(rebuiltOwner);
        assertFalse(AnimationControllerRuntime.isAnimating("target"));
        assertEquals(9, replacement.posOffsetX);
    }

    @Test
    void foreignResetPreservesDelayedAndLoopingPlayback() {
        TestController target = controller("target");
        target.posOffsetX = 7;
        TestController owner = controller("owner");
        owner.loop = true;
        TestController other = controller("other");
        AnimationControllerElement.TargetElement config = new AnimationControllerElement.TargetElement("target", 50);
        other.targetElements.add(config);
        assertTrue(AnimationControllerRuntime.applyAnimation(owner, config, target, 1_000L));
        AnimationControllerRuntime.tick(1_049L);
        assertEquals(7, target.posOffsetX);

        AnimationControllerRuntime.resetController(other);
        AnimationControllerRuntime.tick(1_100L);
        assertEquals(50, target.posOffsetX);
        AnimationControllerRuntime.resetController(other);
        AnimationControllerRuntime.tick(1_225L);
        assertEquals(75, target.posOffsetX);
        assertTrue(AnimationControllerRuntime.isAnimating("target"));
        assertFalse(AnimationControllerRuntime.isFinished(owner, "target"));
    }

    @Test
    void targetWideResetStillClearsEveryOwnerAndRestoresTheActiveTarget() {
        TestController target = controller("target");
        target.posOffsetX = 7;
        TestController first = controller("first");
        TestController second = controller("second");
        AnimationControllerElement.TargetElement config = new AnimationControllerElement.TargetElement("target");
        assertTrue(AnimationControllerRuntime.applyAnimation(first, config, target, 1_000L));
        AnimationControllerRuntime.tick(1_101L);
        assertTrue(AnimationControllerRuntime.applyAnimation(second, config, target, 2_000L));
        AnimationControllerRuntime.tick(2_050L);

        AnimationControllerRuntime.resetAnimationState("target");

        assertEquals(7, target.posOffsetX);
        assertFalse(AnimationControllerRuntime.isAnimating("target"));
        assertFalse(AnimationControllerRuntime.wasAnimatedInThePast("target"));
        assertFalse(AnimationControllerRuntime.isFinished("target"));
        assertFalse(AnimationControllerRuntime.wasAnimatedInThePast(first, "target"));
        assertFalse(AnimationControllerRuntime.wasAnimatedInThePast(second, "target"));
    }

    @Test
    void inactiveOwnerReleasesTheTargetWithoutMarkingItFinished() {
        TestController target = controller("target");
        target.posOffsetX = 7;
        TestController owner = controller("owner");
        AnimationControllerElement.TargetElement config = new AnimationControllerElement.TargetElement("target");
        assertTrue(AnimationControllerRuntime.applyAnimation(owner, config, target, 1_000L));
        AnimationControllerRuntime.tick(1_050L);
        owner.visible = false;
        AnimationControllerRuntime.tick(1_075L);

        assertEquals(7, target.posOffsetX);
        assertFalse(AnimationControllerRuntime.isAnimating("target"));
        assertFalse(AnimationControllerRuntime.isFinished(owner, "target"));
        assertTrue(AnimationControllerRuntime.applyAnimation(controller("other"), config, target, 2_000L));
    }

    @Test
    void missingTargetsInactiveControllersAndEmptySequencesDoNotClaimOwnership() {
        TestController owner = controller("owner");
        TestController target = controller("target");
        AnimationControllerElement.TargetElement config = new AnimationControllerElement.TargetElement("target");
        assertFalse(AnimationControllerRuntime.applyAnimation(owner, config, null, 1_000L));
        owner.visible = false;
        assertFalse(AnimationControllerRuntime.applyAnimation(owner, config, target, 1_000L));
        owner.visible = true;
        owner.keyframes.clear();
        assertTrue(AnimationControllerRuntime.applyAnimation(owner, config, target, 1_000L));

        assertFalse(AnimationControllerRuntime.isAnimating("target"));
        assertFalse(AnimationControllerRuntime.wasAnimatedInThePast(owner, "target"));
        owner.targetElements.add(new AnimationControllerElement.TargetElement());
        owner.targetElements.add(new AnimationControllerElement.TargetElement(""));
        AnimationControllerRuntime.resetController(owner);
    }

    private static TestController controller(String identifier) {
        TestController controller = new TestController();
        controller.setInstanceIdentifier(identifier);
        controller.keyframes.add(new AnimationKeyframe(0L, 0, 0, 10, 10, ElementAnchorPoints.TOP_LEFT, false));
        controller.keyframes.add(new AnimationKeyframe(100L, 100, 200, 30, 40, ElementAnchorPoints.TOP_LEFT, false));
        return controller;
    }

    private static final class TestController extends AnimationControllerElement {

        private TestController() {
            super(new AnimationControllerElementBuilder());
        }

        @Override
        public boolean shouldRender() {
            return this.visible;
        }

    }

}
