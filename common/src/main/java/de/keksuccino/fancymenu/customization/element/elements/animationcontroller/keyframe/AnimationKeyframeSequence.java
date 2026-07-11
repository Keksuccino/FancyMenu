package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Shared ordering and lookup operations for animation keyframes.
 *
 * <p>Both the editor and runtime depend on chronological keyframes. Keeping the invariant here avoids subtly
 * different linear searches and copy behavior in each consumer.</p>
 */
public final class AnimationKeyframeSequence {

    public static final Comparator<AnimationKeyframe> BY_TIMESTAMP = Comparator.comparingLong(keyframe -> keyframe.timestamp);

    private AnimationKeyframeSequence() {
    }

    @NotNull
    public static List<AnimationKeyframe> copyAndSort(@NotNull Collection<AnimationKeyframe> keyframes) {
        List<AnimationKeyframe> copy = new ArrayList<>(keyframes.size());
        for (AnimationKeyframe keyframe : keyframes) {
            if (keyframe != null) copy.add(keyframe.copy());
        }
        sort(copy);
        return copy;
    }

    public static void sort(@NotNull List<AnimationKeyframe> keyframes) {
        keyframes.sort(BY_TIMESTAMP);
    }

    public static long getMaxTimestamp(@NotNull List<AnimationKeyframe> keyframes) {
        long maxTimestamp = 0L;
        for (AnimationKeyframe keyframe : keyframes) maxTimestamp = Math.max(maxTimestamp, keyframe.timestamp);
        return maxTimestamp;
    }

    @Nullable
    public static Segment findSegment(@NotNull List<AnimationKeyframe> sortedKeyframes, long timestamp) {
        int low = 0;
        int high = sortedKeyframes.size() - 2;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            AnimationKeyframe current = sortedKeyframes.get(middle);
            AnimationKeyframe next = sortedKeyframes.get(middle + 1);
            if (timestamp < current.timestamp) {
                high = middle - 1;
            } else if (timestamp >= next.timestamp) {
                low = middle + 1;
            } else {
                long duration = next.timestamp - current.timestamp;
                if (duration <= 0L) return null;
                float progress = (float)(timestamp - current.timestamp) / (float)duration;
                return new Segment(current, next, progress);
            }
        }
        return null;
    }

    public record Segment(@NotNull AnimationKeyframe current, @NotNull AnimationKeyframe next, float progress) {
    }

}
