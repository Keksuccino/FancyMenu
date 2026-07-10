package de.keksuccino.fancymenu.customization.gameintro;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameIntroLifecycleTest {

    @Test
    void completedIntroIsClosedAndConsumedOnce() {
        GameIntroLifecycle lifecycle = new GameIntroLifecycle();

        assertTrue(lifecycle.complete());
        assertTrue(lifecycle.isClosed());
        assertTrue(lifecycle.isConsumed());
        assertFalse(lifecycle.complete());
        assertFalse(lifecycle.replace());
    }

    @Test
    void displacedIntroClosesWithoutBeingConsumed() {
        GameIntroLifecycle lifecycle = new GameIntroLifecycle();

        assertTrue(lifecycle.replace());
        assertTrue(lifecycle.isClosed());
        assertFalse(lifecycle.isConsumed());
        assertFalse(lifecycle.replace());
        assertFalse(lifecycle.complete());
    }

    @Test
    void introLoadsOnlyWhenNotPreviouslyConsumed() {
        assertTrue(GameIntroLifecycle.shouldLoadIntro(false));
        assertFalse(GameIntroLifecycle.shouldLoadIntro(true));
    }

    @Test
    void cleanupRunsNormallyWithoutReportingAnError() {
        AtomicInteger stops = new AtomicInteger();
        AtomicReference<Throwable> reported = new AtomicReference<>();

        GameIntroLifecycle.stopSafely(stops::incrementAndGet, reported::set);

        assertEquals(1, stops.get());
        assertNull(reported.get());
    }

    @Test
    void cleanupFailureIsReportedWithoutEscaping() {
        AssertionError failure = new AssertionError("cleanup failed");
        AtomicReference<Throwable> reported = new AtomicReference<>();

        GameIntroLifecycle.stopSafely(() -> { throw failure; }, reported::set);

        assertSame(failure, reported.get());
    }

}
