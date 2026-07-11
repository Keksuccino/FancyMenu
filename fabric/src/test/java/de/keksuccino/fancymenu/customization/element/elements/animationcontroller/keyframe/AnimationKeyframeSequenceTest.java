package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationKeyframeSequenceTest {

    @Test
    void copiesAndSortsWithoutSharingMutableFrames() {
        AnimationKeyframe later = keyframe(200L, 20);
        AnimationKeyframe earlier = keyframe(100L, 10);

        List<AnimationKeyframe> copy = AnimationKeyframeSequence.copyAndSort(List.of(later, earlier));

        assertEquals(List.of(100L, 200L), copy.stream().map(keyframe -> keyframe.timestamp).toList());
        assertNotSame(earlier, copy.get(0));
        copy.get(0).posOffsetX = 99;
        assertEquals(10, earlier.posOffsetX);
    }

    @Test
    void findsSegmentWithBinaryLookupAndSkipsDuplicateTimestamp() {
        List<AnimationKeyframe> keyframes = List.of(keyframe(0L, 0), keyframe(0L, 10), keyframe(100L, 20));

        AnimationKeyframeSequence.Segment segment = AnimationKeyframeSequence.findSegment(keyframes, 50L);

        assertEquals(10, segment.current().posOffsetX);
        assertEquals(20, segment.next().posOffsetX);
        assertEquals(0.5F, segment.progress());
        assertNull(AnimationKeyframeSequence.findSegment(keyframes, 100L));
    }

    @Test
    void interpolatesNumericValuesAndUsesUpcomingAnchorState() {
        AnimationKeyframe current = keyframe(0L, 0);
        AnimationKeyframe next = keyframe(100L, 20);
        next.anchorPoint = ElementAnchorPoints.BOTTOM_RIGHT;
        next.stickyAnchor = true;

        AnimationKeyframeInterpolator.Values values = AnimationKeyframeInterpolator.interpolate(current, next, 0.25F);

        assertEquals(5, values.posOffsetX());
        assertEquals(ElementAnchorPoints.BOTTOM_RIGHT, values.anchorPoint());
        assertTrue(values.stickyAnchor());
    }

    @Test
    void interpolationDoesNotOverflowAcrossTheFullIntegerRange() {
        AnimationKeyframe current = keyframe(0L, Integer.MIN_VALUE);
        AnimationKeyframe next = keyframe(100L, Integer.MAX_VALUE);

        AnimationKeyframeInterpolator.Values values = AnimationKeyframeInterpolator.interpolate(current, next, 0.5F);

        assertEquals(0, values.posOffsetX());
    }

    private static AnimationKeyframe keyframe(long timestamp, int x) {
        return new AnimationKeyframe(timestamp, x, 0, 10, 10, ElementAnchorPoints.TOP_LEFT, false);
    }

}
