package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.runtime;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframeInterpolator;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframeSequence;
import de.keksuccino.fancymenu.util.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Owns all live animation state while the public handler remains a small integration facade. */
public final class AnimationControllerRuntime {

    private static final Map<String, RunningElementAnimation> RUNNING_ANIMATIONS = new HashMap<>();
    private static final Set<String> ANIMATED_MEMORY = new HashSet<>();
    private static final Set<String> FINISHED_ANIMATIONS = new HashSet<>();

    private AnimationControllerRuntime() {
    }

    public static boolean applyAnimation(@NotNull AnimationControllerElement controller, @NotNull AnimationControllerElement.TargetElement targetConfig, @Nullable AbstractElement targetElement) {
        if ((targetElement == null) || !controller.shouldRender()) return false;

        String targetId = targetElement.getInstanceIdentifier();
        RunningElementAnimation animation = RUNNING_ANIMATIONS.get(targetId);
        if (animation != null) {
            animation.updateTargetElement(targetElement);
            return true;
        }
        List<AnimationKeyframe> keyframes = controller.getKeyframes();
        if (keyframes.isEmpty()) return true;

        ANIMATED_MEMORY.add(targetId);
        int timingOffsetMs = resolveTimingOffsetMs(controller, targetConfig);
        animation = new RunningElementAnimation(keyframes, System.currentTimeMillis() + timingOffsetMs, targetElement, controller);
        RUNNING_ANIMATIONS.put(targetId, animation);
        return true;
    }

    public static void tick() {
        Iterator<Map.Entry<String, RunningElementAnimation>> iterator = RUNNING_ANIMATIONS.entrySet().iterator();
        long currentTime = System.currentTimeMillis();
        while (iterator.hasNext()) {
            Map.Entry<String, RunningElementAnimation> entry = iterator.next();
            RunningElementAnimation animation = entry.getValue();
            AnimationControllerElement controller = animation.getController();
            boolean controllerActive = controller.shouldRender();
            if (!controllerActive) {
                animation.restoreOriginalState();
                iterator.remove();
                continue;
            }

            List<AnimationKeyframe> keyframes = animation.getKeyframes();
            AnimationKeyframe firstKeyframe = keyframes.getFirst();
            AnimationKeyframe lastKeyframe = keyframes.getLast();
            long elapsedTime = animation.getElapsedTime(currentTime);

            if (controller.loop && (elapsedTime > lastKeyframe.timestamp)) {
                long loopDuration = lastKeyframe.timestamp;
                if (loopDuration <= 0L) {
                    animation.apply(lastKeyframe);
                    continue;
                }
                elapsedTime = Math.floorMod(elapsedTime, loopDuration);
                if ((firstKeyframe.timestamp > 0L) && (elapsedTime < firstKeyframe.timestamp)) {
                    float progress = (float)elapsedTime / (float)firstKeyframe.timestamp;
                    animation.apply(AnimationKeyframeInterpolator.interpolate(lastKeyframe, firstKeyframe, progress));
                    continue;
                }
            }

            AnimationKeyframeSequence.Segment segment = AnimationKeyframeSequence.findSegment(keyframes, elapsedTime);
            if (segment != null) {
                animation.apply(AnimationKeyframeInterpolator.interpolate(segment.current(), segment.next(), segment.progress()));
            } else if (elapsedTime == lastKeyframe.timestamp) {
                animation.apply(lastKeyframe);
            }

            if (!controller.loop && (elapsedTime > lastKeyframe.timestamp)) {
                animation.restoreOriginalState();
                iterator.remove();
                FINISHED_ANIMATIONS.add(animation.getTargetElement().getInstanceIdentifier());
            }
        }
    }

    public static void resetAnimationState(@NotNull String targetElementId) {
        RunningElementAnimation animation = RUNNING_ANIMATIONS.remove(targetElementId);
        if (animation != null) animation.restoreOriginalState();
        ANIMATED_MEMORY.remove(targetElementId);
        FINISHED_ANIMATIONS.remove(targetElementId);
    }

    public static void resetController(@NotNull AnimationControllerElement controller) {
        for (AnimationControllerElement.TargetElement target : controller.targetElements) {
            if ((target.targetElementId != null) && !target.targetElementId.isEmpty()) resetAnimationState(target.targetElementId);
            target.animationApplied = false;
        }
    }

    public static void stopAnimation(@NotNull String targetElementId) {
        RUNNING_ANIMATIONS.remove(targetElementId);
    }

    public static void stopAllAnimations() {
        RUNNING_ANIMATIONS.clear();
    }

    public static void clearMemory() {
        ANIMATED_MEMORY.clear();
        FINISHED_ANIMATIONS.clear();
    }

    public static boolean wasAnimatedInThePast(@NotNull String targetElementId) {
        return ANIMATED_MEMORY.contains(targetElementId);
    }

    public static boolean isAnimating(@NotNull String targetElementId) {
        return RUNNING_ANIMATIONS.containsKey(targetElementId);
    }

    public static boolean isFinished(@NotNull String targetElementId) {
        return FINISHED_ANIMATIONS.contains(targetElementId);
    }

    private static int resolveTimingOffsetMs(@NotNull AnimationControllerElement controller, @NotNull AnimationControllerElement.TargetElement targetConfig) {
        int timingOffsetMs = targetConfig.timingOffsetMs;
        if (!controller.randomTimingOffsetMode) return timingOffsetMs;
        int min = controller.randomTimingOffsetMinMs.getInteger();
        int max = controller.randomTimingOffsetMaxMs.getInteger();
        if (min > max) {
            int temporaryMin = min;
            min = max;
            max = temporaryMin;
        }
        return timingOffsetMs + MathUtils.getRandomNumberInRange(min, max);
    }

}
