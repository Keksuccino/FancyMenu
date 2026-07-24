package de.keksuccino.fancymenu.customization.placeholder;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogCooldownTrackerTest {

    @Test
    void cooldownBoundaryAndClockRollbackStartNewWindows() {
        LogCooldownTracker tracker = new LogCooldownTracker(100L, 4, 1000L);

        assertTrue(tracker.tryAcquire("failure", 1000L));
        assertFalse(tracker.tryAcquire("failure", 1099L));
        assertTrue(tracker.tryAcquire("failure", 1100L));
        assertFalse(tracker.tryAcquire("failure", 1101L));
        assertTrue(tracker.tryAcquire("failure", 900L));
    }

    @Test
    void concurrentClaimsForOneWindowHaveOneWinner() throws Exception {
        int workers = 32;
        LogCooldownTracker tracker = new LogCooldownTracker(100L, 4, 1000L);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int worker = 0; worker < workers; worker++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    await(start);
                    if (tracker.tryAcquire("same failure", 1000L)) winners.incrementAndGet();
                }));
            }
            assertTrue(ready.await(5L, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(5L, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }

        assertEquals(1, winners.get());
    }

    @Test
    void distinctFailuresRemainEntryAndWeightBounded() {
        LogCooldownTracker entryBound = new LogCooldownTracker(100L, 3, 1000L);
        for (int failure = 0; failure < 20; failure++) assertTrue(entryBound.tryAcquire("failure-" + failure, 1000L));
        assertEquals(3, entryBound.size());

        LogCooldownTracker weightBound = new LogCooldownTracker(100L, 3, 68L);
        assertTrue(weightBound.tryAcquire("1234", 1000L));
        assertFalse(weightBound.tryAcquire("1234", 1001L));
        assertTrue(weightBound.tryAcquire("12345", 1000L));
        assertTrue(weightBound.tryAcquire("12345", 1001L));
        assertEquals(1, weightBound.size());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for coordinated test work");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for coordinated test work", ex);
        }
    }

}
