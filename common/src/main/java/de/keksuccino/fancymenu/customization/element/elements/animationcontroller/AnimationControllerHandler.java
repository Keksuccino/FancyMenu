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

    public static boolean applyAnimation(@NotNull AnimationControllerElement controller, @Nullable AbstractElement targetElement) {

        if ((targetElement == null) || !controller.loadingRequirementsMet()) {
            return false;
        }

        String targetId = targetElement.getInstanceIdentifier();
        List<AnimationKeyframe> keyframes = controller.getKeyframes();

        if (keyframes.isEmpty()) {
            return true;
        }

        // Start new animation state if not already running
        if (!RUNNING_ANIMATIONS.containsKey(targetId)) {
            AnimationState state = new AnimationState(
                keyframes,
                System.currentTimeMillis(),
                targetElement
            );
            RUNNING_ANIMATIONS.put(targetId, state);
            return true;
        }

        return false;

    }

    public static void tick() {
        Iterator<Map.Entry<String, AnimationState>> it =
                RUNNING_ANIMATIONS.entrySet().iterator();
        long currentTime = System.currentTimeMillis();

        while (it.hasNext()) {

            Map.Entry<String, AnimationState> entry = it.next();
            AnimationState state = entry.getValue();

            // Calculate animation progress
            long elapsedTime = currentTime - state.startTime;
            AnimationKeyframe current = null;
            AnimationKeyframe next = null;

            // Find current and next keyframes
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
                state.targetElement.posOffsetX = (int)lerp(current.posOffsetX, next.posOffsetX, progress);
                state.targetElement.posOffsetY = (int)lerp(current.posOffsetY, next.posOffsetY, progress);
                state.targetElement.baseWidth = (int)lerp(current.baseWidth, next.baseWidth, progress);
                state.targetElement.baseHeight = (int)lerp(current.baseHeight, next.baseHeight, progress);

                // For anchor point and sticky anchor, just use the next keyframe's value
                // (no interpolation needed)
                state.targetElement.anchorPoint = next.anchorPoint;
                state.targetElement.stickyAnchor = next.stickyAnchor;
            }

            // Remove finished animations and restore original properties
            AnimationKeyframe lastKeyframe = state.keyframes.get(state.keyframes.size() - 1);
            if (elapsedTime > lastKeyframe.timestamp) {
                // Restore original properties
                state.targetElement.posOffsetX = state.originalPosOffsetX;
                state.targetElement.posOffsetY = state.originalPosOffsetY;
                state.targetElement.baseWidth = state.originalBaseWidth;
                state.targetElement.baseHeight = state.originalBaseHeight;
                state.targetElement.anchorPoint = state.originalAnchorPoint;
                state.targetElement.stickyAnchor = state.originalStickyAnchor;

                it.remove();
            }

        }

    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static void stopAnimation(String targetElementId) {
        RUNNING_ANIMATIONS.remove(targetElementId);
    }

    public static void stopAllAnimations() {
        RUNNING_ANIMATIONS.clear();
    }

    public static boolean isAnimating(String targetElementId) {
        return RUNNING_ANIMATIONS.containsKey(targetElementId);
    }

    private static class AnimationState {

        final List<AnimationKeyframe> keyframes;
        final long startTime;
        final AbstractElement targetElement;

        // Store original element properties
        final int originalPosOffsetX;
        final int originalPosOffsetY;
        final int originalBaseWidth;
        final int originalBaseHeight;
        final ElementAnchorPoint originalAnchorPoint;
        final boolean originalStickyAnchor;

        AnimationState(List<AnimationKeyframe> keyframes, long startTime, AbstractElement targetElement) {

            this.keyframes = keyframes;
            this.startTime = startTime;
            this.targetElement = targetElement;

            // Store original properties
            this.originalPosOffsetX = targetElement.posOffsetX;
            this.originalPosOffsetY = targetElement.posOffsetY;
            this.originalBaseWidth = targetElement.baseWidth;
            this.originalBaseHeight = targetElement.baseHeight;
            this.originalAnchorPoint = targetElement.anchorPoint;
            this.originalStickyAnchor = targetElement.stickyAnchor;

        }

    }

}
