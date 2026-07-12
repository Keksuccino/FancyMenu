package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyframeTimelineTest {

    @Test
    void mapsTimestampAndPointerCoordinatesThroughOneGeometryModel() {
        KeyframeTimeline timeline = new KeyframeTimeline(List.of(keyframe(3000L)));
        timeline.updateBounds(1100, 700);

        assertEquals(5000L, timeline.getDuration());
        assertEquals(3000L, timeline.getEndTime());
        assertEquals(650, timeline.timestampToX(3000L));
        assertEquals(3000L, timeline.xToTimestamp(650));
        assertTrue(timeline.contains(650, timeline.getY() + 10));
    }

    @Test
    void durationUpdatesKeepRequiredPaddingAndMinimum() {
        KeyframeTimeline timeline = new KeyframeTimeline(List.of());

        timeline.updateDuration(0L);
        assertEquals(2000L, timeline.getDuration());
        timeline.updateDuration(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, timeline.getDuration());
    }

    private static AnimationKeyframe keyframe(long timestamp) {
        return new AnimationKeyframe(timestamp, 0, 0, 10, 10, ElementAnchorPoints.TOP_LEFT, false);
    }

}
