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
    // One controller can write to a target at a time, but each controller owns its own playback history.
    // Stable element identifiers preserve that ownership when a layout rebuilds its element instances.
    private static final Set<AnimationKey> ANIMATED_MEMORY = new HashSet<>();
    private static final Set<AnimationKey> FINISHED_ANIMATIONS = new HashSet<>();

    private AnimationControllerRuntime() {
    }

    public static boolean applyAnimation(@NotNull AnimationControllerElement controller, @NotNull AnimationControllerElement.TargetElement targetConfig, @Nullable AbstractElement targetElement) {
        return applyAnimation(controller, targetConfig, targetElement, System.currentTimeMillis());
    }

    static boolean applyAnimation(@NotNull AnimationControllerElement controller, @NotNull AnimationControllerElement.TargetElement targetConfig, @Nullable AbstractElement targetElement, long currentTime) {
        if ((targetElement == null) || !controller.shouldRender()) return false;

        String targetId = targetElement.getInstanceIdentifier();
        RunningElementAnimation animation = RUNNING_ANIMATIONS.get(targetId);
        if (animation != null) {
            if (!isOwnedBy(animation, controller)) return false;
            animation.updateTargetElement(targetElement);
            return true;
        }
        List<AnimationKeyframe> keyframes = controller.getKeyframes();
        if (keyframes.isEmpty()) return true;

        AnimationKey key = new AnimationKey(controller.getInstanceIdentifier(), targetId);
        ANIMATED_MEMORY.add(key);
        FINISHED_ANIMATIONS.remove(key);
        int timingOffsetMs = resolveTimingOffsetMs(controller, targetConfig);
        animation = new RunningElementAnimation(keyframes, currentTime + timingOffsetMs, targetElement, controller);
        RUNNING_ANIMATIONS.put(targetId, animation);
        return true;
    }

    public static void tick() {
        tick(System.currentTimeMillis());
    }

    static void tick(long currentTime) {
        Iterator<Map.Entry<String, RunningElementAnimation>> iterator = RUNNING_ANIMATIONS.entrySet().iterator();
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
            AnimationKeyframe firstKeyframe = keyframes.get(0);
            AnimationKeyframe lastKeyframe = keyframes.get(keyframes.size() - 1);
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
                FINISHED_ANIMATIONS.add(new AnimationKey(controller.getInstanceIdentifier(), entry.getKey()));
            }
        }
    }

    public static void resetAnimationState(@NotNull String targetElementId) {
        RunningElementAnimation animation = RUNNING_ANIMATIONS.remove(targetElementId);
        if (animation != null) animation.restoreOriginalState();
        ANIMATED_MEMORY.removeIf(key -> key.targetId().equals(targetElementId));
        FINISHED_ANIMATIONS.removeIf(key -> key.targetId().equals(targetElementId));
    }

    public static void resetController(@NotNull AnimationControllerElement controller) {
        for (AnimationControllerElement.TargetElement target : controller.targetElements) {
            if ((target.targetElementId != null) && !target.targetElementId.isEmpty()) {
                RunningElementAnimation animation = RUNNING_ANIMATIONS.get(target.targetElementId);
                if ((animation != null) && isOwnedBy(animation, controller)) {
                    animation.restoreOriginalState();
                    RUNNING_ANIMATIONS.remove(target.targetElementId);
                }
                AnimationKey key = new AnimationKey(controller.getInstanceIdentifier(), target.targetElementId);
                ANIMATED_MEMORY.remove(key);
                FINISHED_ANIMATIONS.remove(key);
            }
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
        return ANIMATED_MEMORY.stream().anyMatch(key -> key.targetId().equals(targetElementId));
    }

    public static boolean wasAnimatedInThePast(@NotNull AnimationControllerElement controller, @NotNull String targetElementId) {
        return ANIMATED_MEMORY.contains(new AnimationKey(controller.getInstanceIdentifier(), targetElementId));
    }

    public static boolean isAnimating(@NotNull String targetElementId) {
        return RUNNING_ANIMATIONS.containsKey(targetElementId);
    }

    public static boolean isFinished(@NotNull String targetElementId) {
        return FINISHED_ANIMATIONS.stream().anyMatch(key -> key.targetId().equals(targetElementId));
    }

    public static boolean isFinished(@NotNull AnimationControllerElement controller, @NotNull String targetElementId) {
        return FINISHED_ANIMATIONS.contains(new AnimationKey(controller.getInstanceIdentifier(), targetElementId));
    }

    private static boolean isOwnedBy(@NotNull RunningElementAnimation animation, @NotNull AnimationControllerElement controller) {
        return animation.getController().getInstanceIdentifier().equals(controller.getInstanceIdentifier());
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

    private record AnimationKey(@NotNull String controllerId, @NotNull String targetId) {

    }

}
