package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SdlCursorTrackerTest {

    @Test
    void tracksShapesAndForgetsDestroyedHandlesBeforeReuse() {
        long cursor = 123L;
        try {
            SdlCursorTracker.onCreateSystemCursor(4, cursor);
            assertEquals(4, SdlCursorTracker.getStandardCursorShape(cursor));
            SdlCursorTracker.onDestroyCursor(cursor);
            assertEquals(-1, SdlCursorTracker.getStandardCursorShape(cursor));
            SdlCursorTracker.onCreateSystemCursor(9, cursor);
            assertEquals(9, SdlCursorTracker.getStandardCursorShape(cursor));
        } finally {
            SdlCursorTracker.onDestroyCursor(cursor);
        }
    }

    @Test
    void ignoresFailedAllocationsAndUnknownHandles() {
        SdlCursorTracker.onCreateSystemCursor(4, 0L);
        assertEquals(-1, SdlCursorTracker.getStandardCursorShape(0L));
        SdlCursorTracker.onDestroyCursor(987L);
        assertEquals(-1, SdlCursorTracker.getStandardCursorShape(987L));
    }

}
