package de.keksuccino.fancymenu.customization.gameintro;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameIntroLifecycleTest {

    @Test
    void finishedOrSkippedIntroIsConsumedBeforeCleanupAndStopsExactlyOnce() {
        AtomicInteger stopCount = new AtomicInteger();
        AtomicBoolean closedDuringStop = new AtomicBoolean();
        AtomicBoolean consumedDuringStop = new AtomicBoolean();
        AtomicReference<GameIntroLifecycle> lifecycleReference = new AtomicReference<>();
        GameIntroLifecycle lifecycle = new GameIntroLifecycle(() -> {
            stopCount.incrementAndGet();
            closedDuringStop.set(lifecycleReference.get().isClosed());
            consumedDuringStop.set(lifecycleReference.get().isConsumed());
        }, throwable -> {});
        lifecycleReference.set(lifecycle);

        assertTrue(lifecycle.markFinished());
        assertFalse(lifecycle.markFinished());
        assertTrue(lifecycle.isConsumed());
        assertFalse(lifecycle.isClosed());
        assertEquals(0, stopCount.get());

        assertTrue(lifecycle.closeFinished());
        assertFalse(lifecycle.closeFinished());
        assertFalse(lifecycle.replace());
        assertTrue(lifecycle.isClosed());
        assertTrue(lifecycle.isConsumed());
        assertTrue(closedDuringStop.get());
        assertTrue(consumedDuringStop.get());
        assertEquals(1, stopCount.get());
    }

    @Test
    void replacementStopsOnceWithoutConsumptionAndAReplacementIntroCanFinish() {
        AtomicInteger displacedStopCount = new AtomicInteger();
        GameIntroLifecycle displacedLifecycle = new GameIntroLifecycle(displacedStopCount::incrementAndGet, throwable -> {});

        assertTrue(displacedLifecycle.replace());
        assertFalse(displacedLifecycle.replace());
        assertFalse(displacedLifecycle.markFinished());
        assertFalse(displacedLifecycle.closeFinished());
        assertTrue(displacedLifecycle.isClosed());
        assertFalse(displacedLifecycle.isConsumed());
        assertEquals(1, displacedStopCount.get());

        AtomicInteger retryStopCount = new AtomicInteger();
        GameIntroLifecycle retryLifecycle = new GameIntroLifecycle(retryStopCount::incrementAndGet, throwable -> {});

        assertTrue(retryLifecycle.markFinished());
        assertTrue(retryLifecycle.closeFinished());
        assertTrue(retryLifecycle.isConsumed());
        assertEquals(1, retryStopCount.get());
    }

    @Test
    void reentrantReplacementAfterFinishingStaysConsumedAndCleansUpOnce() {
        AtomicInteger stopCount = new AtomicInteger();
        GameIntroLifecycle lifecycle = new GameIntroLifecycle(stopCount::incrementAndGet, throwable -> {});

        assertTrue(lifecycle.markFinished());
        assertTrue(lifecycle.replace());
        assertTrue(lifecycle.isConsumed());
        assertTrue(lifecycle.isClosed());
        assertEquals(1, stopCount.get());
        assertFalse(lifecycle.closeFinished());
        assertFalse(lifecycle.replace());
        assertEquals(1, stopCount.get());
    }

    @Test
    void throwingCleanupOnFinishedCloseIsReportedAndLeavesLifecycleTerminal() {
        AssertionError cleanupFailure = new AssertionError("cleanup failed");
        AtomicReference<Throwable> reportedFailure = new AtomicReference<>();
        GameIntroLifecycle lifecycle = new GameIntroLifecycle(() -> {
            throw cleanupFailure;
        }, reportedFailure::set);

        assertTrue(lifecycle.markFinished());
        assertTrue(lifecycle.closeFinished());
        assertTrue(lifecycle.isClosed());
        assertTrue(lifecycle.isConsumed());
        assertSame(cleanupFailure, reportedFailure.get());
        assertFalse(lifecycle.replace());
    }

}
