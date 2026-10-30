package de.keksuccino.fancymenu.util.watermedia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaybackLoopTrackerTest {

    private final PlaybackLoopTracker tracker = new PlaybackLoopTracker(false);
    private final PlaybackLoopTracker playedThroughTracker = new PlaybackLoopTracker(true);

    @Test
    void forwardPlaybackIsNoRestart() {
        for (long time = 0L; time <= 3000L; time += 16L) {
            assertFalse(this.tracker.update(time, 3000L));
        }
        assertEquals(0, this.tracker.getCompletedLoops());
    }

    @Test
    void firstSampleOnlySetsStartingPoint() {
        assertFalse(this.tracker.update(2900L, 3000L));
        assertTrue(this.tracker.update(0L, 3000L));
        assertEquals(1, this.tracker.getCompletedLoops());
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

    @Test
    void playedThroughLoopCountsAsRestart() {
        for (long time = 0L; time <= 9990L; time += 33L) {
            assertFalse(this.playedThroughTracker.update(time, 10_000L));
        }
        assertTrue(this.playedThroughTracker.isPlayedThrough(10_000L));

        assertTrue(this.playedThroughTracker.update(15L, 10_000L));
        assertEquals(1, this.playedThroughTracker.getCompletedLoops());
        assertFalse(this.playedThroughTracker.isPlayedThrough(10_000L));
    }

    @Test
    void playedThroughLoopIsNoticedWithRareSamples() {
        // Low frame rates sample the play time less often, so the last sample before the restart can be far from the end
        for (long time = 0L; time <= 9200L; time += 400L) {
            this.playedThroughTracker.update(time, 10_000L);
        }

        assertTrue(this.playedThroughTracker.update(300L, 10_000L));
    }

    @Test
    void stalePlayTimeAfterSeekIsNoRestart() {
        // Right after seeking to the start of a shared video, the player can still report the old position for a moment
        this.playedThroughTracker.restartPass();
        this.playedThroughTracker.update(12_000L, 30_000L);
        this.playedThroughTracker.update(12_016L, 30_000L);

        assertFalse(this.playedThroughTracker.update(0L, 30_000L));
        assertEquals(0, this.playedThroughTracker.getCompletedLoops());
    }

    @Test
    void backwardSeekBeforePlayingThroughIsNoRestart() {
        for (long time = 0L; time <= 4000L; time += 33L) {
            this.playedThroughTracker.update(time, 10_000L);
        }

        assertFalse(this.playedThroughTracker.update(1000L, 10_000L));
        assertEquals(0, this.playedThroughTracker.getCompletedLoops());
    }

    @Test
    void restartedPassKeepsLoopsAndNeedsNewStartingPoint() {
        this.playedThroughTracker.update(0L, 1000L);
        this.playedThroughTracker.update(990L, 1000L);
        assertTrue(this.playedThroughTracker.update(0L, 1000L));

        this.playedThroughTracker.restartPass();

        assertEquals(1, this.playedThroughTracker.getCompletedLoops());
        assertFalse(this.playedThroughTracker.update(900L, 1000L));
        assertFalse(this.playedThroughTracker.isPlayedThrough(1000L));
    }

    @Test
    void passIsNeverPlayedThroughWithUnknownDuration() {
        this.playedThroughTracker.update(0L, 0L);
        this.playedThroughTracker.update(5000L, 0L);

        assertFalse(this.playedThroughTracker.isPlayedThrough(0L));
        assertFalse(this.playedThroughTracker.update(0L, 0L));
    }

}
