package de.keksuccino.fancymenu.networking;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerHandshakeNegotiationTrackerTest {

    private static final int MAX_ATTEMPTS = ServerHandshakeNegotiationTracker.DEFAULT_MAX_UNACCEPTED_ATTEMPTS;
    private static final long WINDOW_NANOS = ServerHandshakeNegotiationTracker.DEFAULT_WINDOW_NANOS;

    @Test
    void firstAdmittedHandshakeIsAcceptedExactlyOnce() {
        MutableClock clock = new MutableClock();
        ServerHandshakeNegotiationTracker tracker = tracker(clock);
        Object connection = new Object();

        assertAllowed(tracker.admitAttempt(connection));
        assertAllowed(tracker.accept(connection));
        assertRejectedWithWarning(tracker.accept(connection));
        assertRejected(tracker.accept(connection));
    }

    @Test
    void acceptedReplayIsRejectedBeforeAnotherNegotiationAttempt() {
        MutableClock clock = new MutableClock();
        ServerHandshakeNegotiationTracker tracker = tracker(clock);
        Object connection = new Object();

        assertAllowed(tracker.admitAttempt(connection));
        assertAllowed(tracker.accept(connection));
        assertRejectedWithWarning(tracker.admitAttempt(connection));
        assertRejected(tracker.admitAttempt(connection));

        clock.advance(WINDOW_NANOS);

        assertRejectedWithWarning(tracker.admitAttempt(connection));
    }

    @Test
    void queuedAndRawReplaysShareOneWarningBudget() {
        MutableClock clock = new MutableClock();
        ServerHandshakeNegotiationTracker tracker = tracker(clock);
        Object connection = new Object();

        assertAllowed(tracker.admitAttempt(connection));
        assertAllowed(tracker.admitAttempt(connection));
        assertAllowed(tracker.accept(connection));
        assertRejectedWithWarning(tracker.accept(connection));
        assertRejected(tracker.admitAttempt(connection));

        clock.advance(WINDOW_NANOS);

        assertRejectedWithWarning(tracker.admitAttempt(connection));
    }

    @Test
    void malformedAttemptsCanRecoverWithinTheBoundedBurst() {
        MutableClock clock = new MutableClock();
        ServerHandshakeNegotiationTracker tracker = tracker(clock);
        Object connection = new Object();
        AtomicInteger decoderCalls = new AtomicInteger();

        for (int attempt = 0; attempt < MAX_ATTEMPTS - 1; attempt++) {
            if (tracker.admitAttempt(connection).isAllowed()) decoderCalls.incrementAndGet();
        }
        assertAllowed(tracker.admitAttempt(connection));
        decoderCalls.incrementAndGet();
        assertAllowed(tracker.accept(connection));
        assertRejectedWithWarning(tracker.admitAttempt(connection));

        assertEquals(MAX_ATTEMPTS, decoderCalls.get());
    }

    @Test
    void malformedTrafficIsCappedUntilTheExactWindowBoundary() {
        MutableClock clock = new MutableClock();
        ServerHandshakeNegotiationTracker tracker = tracker(clock);
        Object connection = new Object();
        AtomicInteger decoderCalls = new AtomicInteger();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            if (tracker.admitAttempt(connection).isAllowed()) decoderCalls.incrementAndGet();
        }
        assertRejectedWithWarning(tracker.admitAttempt(connection));
        assertRejected(tracker.admitAttempt(connection));
        clock.advance(WINDOW_NANOS - 1L);
        assertRejected(tracker.admitAttempt(connection));
        assertEquals(MAX_ATTEMPTS, decoderCalls.get());

        clock.advance(1L);
        assertAllowed(tracker.admitAttempt(connection));
        decoderCalls.incrementAndGet();

        assertEquals(MAX_ATTEMPTS + 1, decoderCalls.get());
    }

    @Test
    void clockRollbackStartsANewBoundedWindowAndWarningInterval() {
        MutableClock clock = new MutableClock();
        clock.set(WINDOW_NANOS);
        ServerHandshakeNegotiationTracker tracker = tracker(clock);
        Object connection = new Object();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) assertAllowed(tracker.admitAttempt(connection));
        assertRejectedWithWarning(tracker.admitAttempt(connection));

        clock.set(0L);

        assertAllowed(tracker.admitAttempt(connection));
        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) assertAllowed(tracker.admitAttempt(connection));
        assertRejectedWithWarning(tracker.admitAttempt(connection));
        assertRejected(tracker.admitAttempt(connection));
    }

    @Test
    void distinctConnectionsHaveIndependentNegotiationStateEvenWhenEqual() {
        MutableClock clock = new MutableClock();
        ServerHandshakeNegotiationTracker tracker = tracker(clock);
        EqualConnection firstConnection = new EqualConnection();
        EqualConnection secondConnection = new EqualConnection();

        assertAllowed(tracker.admitAttempt(firstConnection));
        assertAllowed(tracker.accept(firstConnection));
        assertRejectedWithWarning(tracker.admitAttempt(firstConnection));
        assertAllowed(tracker.admitAttempt(secondConnection));
        assertAllowed(tracker.accept(secondConnection));
    }

    @Test
    void reconnectWithANewListenerGetsAFreshAcceptance() {
        MutableClock clock = new MutableClock();
        ServerHandshakeNegotiationTracker tracker = tracker(clock);
        Object oldListener = new Object();
        Object reconnectedListener = new Object();

        assertAllowed(tracker.admitAttempt(oldListener));
        assertAllowed(tracker.accept(oldListener));
        assertRejectedWithWarning(tracker.admitAttempt(oldListener));

        assertAllowed(tracker.admitAttempt(reconnectedListener));
        assertAllowed(tracker.accept(reconnectedListener));
    }

    @Test
    void concurrentMalformedAttemptsCannotExceedTheBurstLimit() throws Exception {
        MutableClock clock = new MutableClock();
        ServerHandshakeNegotiationTracker tracker = tracker(clock);
        Object connection = new Object();
        int attempts = 64;
        AtomicInteger allowed = new AtomicInteger();

        runConcurrently(attempts, () -> {
            if (tracker.admitAttempt(connection).isAllowed()) allowed.incrementAndGet();
        });

        assertEquals(MAX_ATTEMPTS, allowed.get());
    }

    @Test
    void concurrentQueuedHandshakesProduceOneAcceptedTransition() throws Exception {
        MutableClock clock = new MutableClock();
        ServerHandshakeNegotiationTracker tracker = new ServerHandshakeNegotiationTracker(64, WINDOW_NANOS, clock);
        Object connection = new Object();
        int attempts = 64;
        AtomicInteger accepted = new AtomicInteger();

        for (int attempt = 0; attempt < attempts; attempt++) assertAllowed(tracker.admitAttempt(connection));
        runConcurrently(attempts, () -> {
            if (tracker.accept(connection).isAllowed()) accepted.incrementAndGet();
        });

        assertEquals(1, accepted.get());
    }

    @Test
    void constructorRejectsInvalidRateLimits() {
        MutableClock clock = new MutableClock();

        assertThrows(IllegalArgumentException.class, () -> new ServerHandshakeNegotiationTracker(0, WINDOW_NANOS, clock));
        assertThrows(IllegalArgumentException.class, () -> new ServerHandshakeNegotiationTracker(MAX_ATTEMPTS, 0L, clock));
    }

    private static ServerHandshakeNegotiationTracker tracker(MutableClock clock) {
        return new ServerHandshakeNegotiationTracker(clock);
    }

    private static void assertAllowed(ServerHandshakeNegotiationTracker.Decision decision) {
        assertTrue(decision.isAllowed());
        assertFalse(decision.isWarningRequired());
    }

    private static void assertRejected(ServerHandshakeNegotiationTracker.Decision decision) {
        assertFalse(decision.isAllowed());
        assertFalse(decision.isWarningRequired());
    }

    private static void assertRejectedWithWarning(ServerHandshakeNegotiationTracker.Decision decision) {
        assertFalse(decision.isAllowed());
        assertTrue(decision.isWarningRequired());
    }

    private static void runConcurrently(int taskCount, ThrowingRunnable task) throws Exception {
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(taskCount);
        try {
            for (int index = 0; index < taskCount; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    task.run();
                    return null;
                }));
            }
            assertTrue(ready.await(5L, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(5L, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class MutableClock implements java.util.function.LongSupplier {

        private final AtomicLong now = new AtomicLong();

        @Override
        public long getAsLong() {
            return this.now.get();
        }

        private void set(long value) {
            this.now.set(value);
        }

        private void advance(long delta) {
            this.now.addAndGet(delta);
        }
    }

    private static final class EqualConnection {

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualConnection;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
