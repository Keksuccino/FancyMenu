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
    void firstSampleOnlySetsStartingPoint() {
        // Joining a pass near its end means it didn't get played through from the starting point, so the jump back is no restart yet
        assertFalse(this.tracker.update(2900L, 3000L));
        assertFalse(this.tracker.update(0L, 3000L));
        assertEquals(0, this.tracker.getCompletedLoops());
    }

    @Test
    void jumpBackToStartCountsEveryPlayedThroughLoop() {
        this.tracker.update(0L, 3000L);
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
        this.tracker.update(0L, 90L);
        this.tracker.update(80L, 90L);

        assertTrue(this.tracker.update(5L, 90L));
        assertEquals(1, this.tracker.getCompletedLoops());
    }

    @Test
    void restartIsNoticedWhileDurationIsStillUnknown() {
        // Streamed animations only know their duration after the first full pass
        this.tracker.update(0L, 0L);
        this.tracker.update(2900L, 0L);

        assertTrue(this.tracker.update(0L, 3000L));
        assertEquals(1, this.tracker.getCompletedLoops());
    }

    @Test
    void resetClearsLoopsAndStartingPoint() {
        this.tracker.update(0L, 3000L);
        this.tracker.update(2900L, 3000L);
        this.tracker.update(0L, 3000L);
        this.tracker.update(2900L, 3000L);
        assertEquals(1, this.tracker.getCompletedLoops());

        this.tracker.reset();

        assertEquals(0, this.tracker.getCompletedLoops());
        assertFalse(this.tracker.update(0L, 3000L));
        assertEquals(0, this.tracker.getCompletedLoops());
    }

    @Test
    void negativeTimeIsTreatedAsStart() {
        this.tracker.update(0L, 3000L);
        this.tracker.update(2900L, 3000L);

        assertTrue(this.tracker.update(-1L, 3000L));
        assertFalse(this.tracker.update(0L, 3000L));
        assertEquals(1, this.tracker.getCompletedLoops());
    }

    @Test
    void playedThroughLoopCountsAsRestart() {
        for (long time = 0L; time <= 9990L; time += 33L) {
            assertFalse(this.tracker.update(time, 10_000L));
        }
        assertTrue(this.tracker.isPlayedThrough(10_000L));

        assertTrue(this.tracker.update(15L, 10_000L));
        assertEquals(1, this.tracker.getCompletedLoops());
        assertFalse(this.tracker.isPlayedThrough(10_000L));
    }

    @Test
    void playedThroughLoopIsNoticedWithRareSamples() {
        // Low frame rates sample the play time less often, so the last sample before the restart can be far from the end
        for (long time = 0L; time <= 9200L; time += 400L) {
            this.tracker.update(time, 10_000L);
        }

        assertTrue(this.tracker.update(300L, 10_000L));
    }

    @Test
    void stalePlayTimeAfterSeekIsNoRestart() {
        // Right after seeking to the start of a shared video, the player can still report the old position for a moment
        this.tracker.restartPass();
        this.tracker.update(12_000L, 30_000L);
        this.tracker.update(12_016L, 30_000L);

        assertFalse(this.tracker.update(0L, 30_000L));
        assertEquals(0, this.tracker.getCompletedLoops());
    }

    @Test
    void backwardSeekBeforePlayingThroughIsNoRestart() {
        for (long time = 0L; time <= 4000L; time += 33L) {
            this.tracker.update(time, 10_000L);
        }

        assertFalse(this.tracker.update(1000L, 10_000L));
        assertEquals(0, this.tracker.getCompletedLoops());
    }

    @Test
    void restartedPassKeepsLoopsAndNeedsNewStartingPoint() {
        this.tracker.update(0L, 1000L);
        this.tracker.update(990L, 1000L);
        assertTrue(this.tracker.update(0L, 1000L));

        this.tracker.restartPass();

        assertEquals(1, this.tracker.getCompletedLoops());
        assertFalse(this.tracker.update(900L, 1000L));
        assertFalse(this.tracker.isPlayedThrough(1000L));
    }

    @Test
    void passIsNeverPlayedThroughWithUnknownDuration() {
        this.tracker.update(0L, 0L);
        this.tracker.update(5000L, 0L);

        assertFalse(this.tracker.isPlayedThrough(0L));
        assertFalse(this.tracker.update(0L, 0L));
    }

}
