package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class AnimationControllerHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, AnimationState> RUNNING_ANIMATIONS = new HashMap<>();
    private static final List<String> ANIMATED_MEMORY = new ArrayList<>();

    public static boolean applyAnimation(@NotNull AnimationControllerElement controller, @Nullable AbstractElement targetElement) {

        if ((targetElement == null) || !controller.loadingRequirementsMet()) {
            return false;
        }

        String targetId = targetElement.getInstanceIdentifier();
        List<AnimationKeyframe> keyframes = controller.getKeyframes();

        if (keyframes.isEmpty()) {
            return true;
        }

        if (!ANIMATED_MEMORY.contains(targetId)) ANIMATED_MEMORY.add(targetId);

        // Start new animation state if not already running or update state if already running
        AnimationState state;
        if (!RUNNING_ANIMATIONS.containsKey(targetId)) {
            state = new AnimationState(keyframes, System.currentTimeMillis(), targetElement, controller);
            RUNNING_ANIMATIONS.put(targetId, state);
        } else {
            state = RUNNING_ANIMATIONS.get(targetId);
            state.targetElement = targetElement;
        }
        state.storeOriginalProperties();

        return true;

    }

    public static void tick() {

        Iterator<Map.Entry<String, AnimationState>> it = RUNNING_ANIMATIONS.entrySet().iterator();
        long currentTime = System.currentTimeMillis();

        while (it.hasNext()) {

            Map.Entry<String, AnimationState> entry = it.next();
            AnimationState state = entry.getValue();

            // Calculate animation progress
            long elapsedTime = currentTime - state.startTime;
            AnimationKeyframe current = null;
            AnimationKeyframe next = null;

            // Special handling for the loop transition period
            AnimationKeyframe lastKeyframe = state.keyframes.get(state.keyframes.size() - 1);
            AnimationKeyframe firstKeyframe = state.keyframes.get(0);

            if (state.controller.loop && elapsedTime > lastKeyframe.timestamp) {
                // Calculate how far we are into the current loop
                long loopDuration = lastKeyframe.timestamp;
                long timeIntoLoop = elapsedTime % loopDuration;

                // If we're between the last and first keyframe
                if (timeIntoLoop < firstKeyframe.timestamp) {
                    current = lastKeyframe;
                    next = firstKeyframe;
                    // Calculate progress for transition between last and first frame
                    float progress = (float)timeIntoLoop / firstKeyframe.timestamp;

                    // Apply interpolated values
                    if (!state.controller.ignorePosition) {
                        if (!state.controller.offsetMode) {
                            state.targetElement.posOffsetX = (int)lerp(current.posOffsetX, next.posOffsetX, progress);
                            state.targetElement.posOffsetY = (int)lerp(current.posOffsetY, next.posOffsetY, progress);
                        } else {
                            state.targetElement.animatedOffsetX = (int)lerp(current.posOffsetX, next.posOffsetX, progress);
                            state.targetElement.animatedOffsetY = (int)lerp(current.posOffsetY, next.posOffsetY, progress);
                        }
                    }
                    if (!state.controller.ignoreSize) {
                        state.targetElement.baseWidth = (int)lerp(current.baseWidth, next.baseWidth, progress);
                        state.targetElement.baseHeight = (int)lerp(current.baseHeight, next.baseHeight, progress);
                    }
                    if (!state.controller.offsetMode && !state.controller.ignorePosition) {
                        state.targetElement.anchorPoint = next.anchorPoint;
                        state.targetElement.stickyAnchor = next.stickyAnchor;
                    }
                    continue;
                }

                // Adjust elapsed time to be within the loop duration
                elapsedTime = timeIntoLoop;
            }

            // Find current and next keyframes for normal playback
            for (int i = 0; i < state.keyframes.size() - 1; i++) {
                AnimationKeyframe k1 = state.keyframes.get(i);
                AnimationKeyframe k2 = state.keyframes.get(i + 1);

                if (elapsedTime >= k1.timestamp && elapsedTime < k2.timestamp) {
                    current = k1;
                    next = k2;
                    break;
                }
            }

            if (current != null && next != null) {
                float progress = (float)(elapsedTime - current.timestamp) / (next.timestamp - current.timestamp);

                // Interpolate and apply element properties
                if (!state.controller.ignorePosition) {
                    if (!state.controller.offsetMode) {
                        state.targetElement.posOffsetX = (int) lerp(current.posOffsetX, next.posOffsetX, progress);
                        state.targetElement.posOffsetY = (int) lerp(current.posOffsetY, next.posOffsetY, progress);
                    } else {
                        state.targetElement.animatedOffsetX = (int) lerp(current.posOffsetX, next.posOffsetX, progress);
                        state.targetElement.animatedOffsetY = (int) lerp(current.posOffsetY, next.posOffsetY, progress);
                    }
                }
                if (!state.controller.ignoreSize) {
                    state.targetElement.baseWidth = (int)lerp(current.baseWidth, next.baseWidth, progress);
                    state.targetElement.baseHeight = (int)lerp(current.baseHeight, next.baseHeight, progress);
                }
                if (!state.controller.offsetMode && !state.controller.ignorePosition) {
                    state.targetElement.anchorPoint = next.anchorPoint;
                    state.targetElement.stickyAnchor = next.stickyAnchor;
                }
            }

            // Remove non-looping animations once they finish
            if (!state.controller.loop && elapsedTime > lastKeyframe.timestamp) {
                // Restore original properties
                if (!state.controller.offsetMode) {
                    state.targetElement.posOffsetX = state.originalPosOffsetX;
                    state.targetElement.posOffsetY = state.originalPosOffsetY;
                    state.targetElement.baseWidth = state.originalBaseWidth;
                    state.targetElement.baseHeight = state.originalBaseHeight;
                    state.targetElement.anchorPoint = state.originalAnchorPoint;
                    state.targetElement.stickyAnchor = state.originalStickyAnchor;
                }
                state.targetElement.animatedOffsetX = 0;
                state.targetElement.animatedOffsetY = 0;
                it.remove();
            }

        }

    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static void stopAnimation(@NotNull String targetElementId) {
        RUNNING_ANIMATIONS.remove(targetElementId);
    }

    public static void stopAllAnimations() {
        RUNNING_ANIMATIONS.clear();
    }

    public static void eraseAnimatedMemory() {
        ANIMATED_MEMORY.clear();
    }

    public static boolean wasAnimatedInThePast(@NotNull String targetElementId) {
        return ANIMATED_MEMORY.contains(targetElementId);
    }

    public static boolean isAnimating(@NotNull String targetElementId) {
        return RUNNING_ANIMATIONS.containsKey(targetElementId);
    }

    protected static class AnimationState {

        protected List<AnimationKeyframe> keyframes;
        protected long startTime;
        protected AbstractElement targetElement;
        protected AnimationControllerElement controller;

        // Store original element properties
        protected int originalPosOffsetX;
        protected int originalPosOffsetY;
        protected int originalBaseWidth;
        protected int originalBaseHeight;
        protected ElementAnchorPoint originalAnchorPoint;
        protected boolean originalStickyAnchor;

        protected AnimationState(List<AnimationKeyframe> keyframes, long startTime, AbstractElement targetElement, AnimationControllerElement controller) {

            this.keyframes = keyframes;
            this.startTime = startTime;
            this.targetElement = targetElement;
            this.controller = controller;

        }

        protected void storeOriginalProperties() {
            this.originalPosOffsetX = targetElement.posOffsetX;
            this.originalPosOffsetY = targetElement.posOffsetY;
            this.originalBaseWidth = targetElement.baseWidth;
            this.originalBaseHeight = targetElement.baseHeight;
            this.originalAnchorPoint = targetElement.anchorPoint;
            this.originalStickyAnchor = targetElement.stickyAnchor;
        }

    }

}
