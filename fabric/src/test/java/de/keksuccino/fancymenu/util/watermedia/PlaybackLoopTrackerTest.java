package de.keksuccino.fancymenu.util.watermedia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaybackLoopTrackerTest {

    private final PlaybackLoopTracker tracker = new PlaybackLoopTracker();

    @Test
    void forwardPlaybackIsNoRestart() {
        for (long time = 0L; time <= 3000L; time += 16L) {
            assertFalse(this.tracker.update(time, 3000L));
        }
        assertEquals(0, this.tracker.getCompletedLoops());
    }

    @Test
    void jumpBackToStartCountsEveryLoop() {
        this.tracker.update(2900L, 3000L);
        assertTrue(this.tracker.update(20L, 3000L));
        this.tracker.update(2990L, 3000L);
        assertTrue(this.tracker.update(0L, 3000L));

        assertEquals(2, this.tracker.getCompletedLoops());
    }

    @Test
    void smallBackwardJitterIsNoRestart() {
        this.tracker.update(1500L, 3000L);

        assertFalse(this.tracker.update(1500L - PlaybackLoopTracker.MAX_RESTART_TOLERANCE_MS, 3000L));
        assertEquals(0, this.tracker.getCompletedLoops());
    }

    @Test
    void restartOfVeryShortLoopIsNoticed() {
        // The whole loop is shorter than the normal jitter tolerance
        this.tracker.update(80L, 90L);

        assertTrue(this.tracker.update(5L, 90L));
        assertEquals(1, this.tracker.getCompletedLoops());
    }

    @Test
    void restartIsNoticedWhileDurationIsStillUnknown() {
        // Streamed animations only know their duration after the first full pass
        this.tracker.update(2900L, 0L);

        assertTrue(this.tracker.update(0L, 3000L));
        assertEquals(1, this.tracker.getCompletedLoops());
    }

    @Test
    void resetClearsLoopsAndLastTime() {
        this.tracker.update(2900L, 3000L);
        this.tracker.update(0L, 3000L);
        this.tracker.update(2900L, 3000L);

        this.tracker.reset();

        assertEquals(0, this.tracker.getCompletedLoops());
        assertFalse(this.tracker.update(0L, 3000L));
        assertEquals(0, this.tracker.getCompletedLoops());
    }

    @Test
    void negativeTimeIsTreatedAsStart() {
        this.tracker.update(2900L, 3000L);

        assertTrue(this.tracker.update(-1L, 3000L));
        assertFalse(this.tracker.update(0L, 3000L));
        assertEquals(1, this.tracker.getCompletedLoops());
    }

}
