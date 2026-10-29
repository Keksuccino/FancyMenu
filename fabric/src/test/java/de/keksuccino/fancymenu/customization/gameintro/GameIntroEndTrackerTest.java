package de.keksuccino.fancymenu.customization.gameintro;

import de.keksuccino.fancymenu.util.resource.PlayableResource;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameIntroEndTrackerTest {

    private final AtomicLong clock = new AtomicLong(10_000L);
    private final GameIntroEndTracker tracker = new GameIntroEndTracker(this.clock::get);
    private final FakeIntro intro = new FakeIntro();

    @Test
    void introThatDidNotStartNeverEnds() {
        this.intro.playing = false;
        this.intro.completedPlayCycles = 3;
        this.clock.addAndGet(60_000L);

        assertFalse(this.tracker.isStarted());
        assertFalse(this.tracker.checkEndReached(this.intro, true));
        assertFalse(this.tracker.checkEndReached(this.intro, false));
    }

    @Test
    void ignoredLoopsEndIntroRightAfterFirstCompletedCycle() {
        this.tracker.markStarted();
        this.clock.addAndGet(500L);
        assertFalse(this.tracker.checkEndReached(this.intro, true));

        // A short looping animation finishes its first cycle before the start grace period is over and keeps playing afterwards
        this.intro.completedPlayCycles = 1;
        assertTrue(this.intro.isPlaying());
        assertTrue(this.tracker.checkEndReached(this.intro, true));
    }

    @Test
    void respectedLoopsKeepLoopingIntroRunning() {
        this.tracker.markStarted();
        this.intro.completedPlayCycles = 5;
        this.clock.addAndGet(60_000L);

        assertFalse(this.tracker.checkEndReached(this.intro, false));
    }

    @Test
    void resourceThatStoppedPlayingOnlyEndsAfterStartGracePeriod() {
        this.tracker.markStarted();
        this.intro.playing = false;

        this.clock.addAndGet(GameIntroEndTracker.START_GRACE_PERIOD_MS);
        assertFalse(this.tracker.checkEndReached(this.intro, false));
        assertFalse(this.tracker.checkEndReached(this.intro, true));

        this.clock.incrementAndGet();
        assertTrue(this.tracker.checkEndReached(this.intro, false));
    }

    @Test
    void finishedResourceWithoutCycleTrackingStillEndsWhenLoopsGetIgnored() {
        this.tracker.markStarted();
        this.intro.playing = false;
        this.clock.addAndGet(GameIntroEndTracker.START_GRACE_PERIOD_MS + 1L);

        assertEquals(0, this.intro.getCompletedPlayCycles());
        assertTrue(this.tracker.checkEndReached(this.intro, true));
    }

    @Test
    void reachedEndStaysReachedWhenSharedResourceRestarts() {
        this.tracker.markStarted();
        this.intro.completedPlayCycles = 1;
        assertTrue(this.tracker.checkEndReached(this.intro, true));

        // The target screen can reset the same shared resource while the intro fades out
        this.intro.completedPlayCycles = 0;
        this.intro.playing = true;
        assertTrue(this.tracker.checkEndReached(this.intro, true));
        assertTrue(this.tracker.checkEndReached(this.intro, false));
    }

    @Test
    void reachedEndStopsQueryingTheResource() {
        this.tracker.markStarted();
        this.intro.completedPlayCycles = 1;
        assertTrue(this.tracker.checkEndReached(this.intro, true));
        int queries = this.intro.queries;

        this.tracker.checkEndReached(this.intro, true);

        assertEquals(queries, this.intro.queries);
    }

    private static final class FakeIntro implements PlayableResource {

        private boolean playing = true;
        private int completedPlayCycles = 0;
        private int queries = 0;

        @Override
        public void play() {
        }

        @Override
        public boolean isPlaying() {
            this.queries++;
            return this.playing;
        }

        @Override
        public void pause() {
        }

        @Override
        public boolean isPaused() {
            return false;
        }

        @Override
        public void stop() {
        }

        @Override
        public int getCompletedPlayCycles() {
            this.queries++;
            return this.completedPlayCycles;
        }

        @Override
        public InputStream open() {
            return null;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public boolean isLoadingCompleted() {
            return true;
        }

        @Override
        public boolean isLoadingFailed() {
            return false;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() {
        }

    }

}
