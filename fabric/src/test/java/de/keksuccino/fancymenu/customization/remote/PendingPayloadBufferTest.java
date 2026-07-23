package de.keksuccino.fancymenu.customization.remote;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingPayloadBufferTest {

    @Test
    void messageLimitUsesActualUtf8BytesAndIncludesEnvelope() {
        PendingPayloadBuffer buffer = new PendingPayloadBuffer(4, 32, 8, 4, 1_000);

        assertEquals(PendingPayloadBuffer.AdmissionResult.ACCEPTED, buffer.offer("😀", 0));
        assertEquals(8L, buffer.pendingUtf8Bytes());
        assertEquals(PendingPayloadBuffer.AdmissionResult.PAYLOAD_TOO_LARGE, buffer.offer("😀a", 0));
        assertEquals(PendingPayloadBuffer.AdmissionResult.MALFORMED_UTF16, buffer.offer("\uD83D", 0));
        assertEquals(1, buffer.pendingCount());
        assertEquals(8L, buffer.pendingUtf8Bytes());
    }

    @Test
    void countOverflowDropsNewestAndPreservesFifo() {
        PendingPayloadBuffer buffer = new PendingPayloadBuffer(2, 100, 20, 1, 1_000);

        assertEquals(PendingPayloadBuffer.AdmissionResult.ACCEPTED, buffer.offer("first", 0));
        assertEquals(PendingPayloadBuffer.AdmissionResult.ACCEPTED, buffer.offer("second", 0));
        assertEquals(PendingPayloadBuffer.AdmissionResult.CAPACITY_EXCEEDED, buffer.offer("newest", 0));
        assertEquals(List.of("first", "second"), buffer.queuedPayloadsSnapshot());
    }

    @Test
    void byteOverflowDropsNewestAndKeepsExactAccounting() {
        PendingPayloadBuffer buffer = new PendingPayloadBuffer(4, 10, 8, 2, 1_000);

        assertEquals(PendingPayloadBuffer.AdmissionResult.ACCEPTED, buffer.offer("123", 0));
        assertEquals(PendingPayloadBuffer.AdmissionResult.ACCEPTED, buffer.offer("456", 0));
        assertEquals(10L, buffer.pendingUtf8Bytes());
        assertEquals(PendingPayloadBuffer.AdmissionResult.CAPACITY_EXCEEDED, buffer.offer("7", 0));
        assertEquals(2, buffer.pendingCount());
        assertEquals(10L, buffer.pendingUtf8Bytes());
    }

    @Test
    void oneInFlightPayloadRetainsBudgetUntilCompletion() {
        PendingPayloadBuffer buffer = new PendingPayloadBuffer(2, 20, 10, 2, 1_000);
        buffer.offer("one", 0);
        buffer.offer("two", 0);

        PendingPayloadBuffer.Payload first = buffer.pollForSend(0);

        assertEquals("one", first.payload());
        assertTrue(buffer.hasInFlightPayload());
        assertEquals(2, buffer.pendingCount());
        assertEquals(10L, buffer.pendingUtf8Bytes());
        assertNull(buffer.pollForSend(0));
        assertTrue(buffer.completeSend(first));
        assertFalse(buffer.hasInFlightPayload());
        assertEquals(1, buffer.pendingCount());
        assertEquals(5L, buffer.pendingUtf8Bytes());
        assertFalse(buffer.completeSend(first));
        assertEquals(1, buffer.pendingCount());
    }

    @Test
    void failedSendRetriesAtHeadWithoutDoubleAccounting() {
        PendingPayloadBuffer buffer = new PendingPayloadBuffer(3, 30, 10, 1, 100);
        buffer.offer("first", 0);
        buffer.offer("second", 0);
        PendingPayloadBuffer.Payload firstAttempt = buffer.pollForSend(1);
        long bytesBeforeRetry = buffer.pendingUtf8Bytes();

        assertTrue(buffer.retryOrDiscardSend(firstAttempt, 50, true));
        assertEquals(List.of("first", "second"), buffer.queuedPayloadsSnapshot());
        assertEquals(bytesBeforeRetry, buffer.pendingUtf8Bytes());
        assertEquals(2, buffer.pendingCount());
        assertSame(firstAttempt, buffer.pollForSend(50));
    }

    @Test
    void expiredRetryAndQueuedEntriesReleaseTheirBudgetsAtBoundary() {
        PendingPayloadBuffer buffer = new PendingPayloadBuffer(3, 30, 10, 1, 100);
        buffer.offer("inflight", 0);
        buffer.offer("queued", 1);
        PendingPayloadBuffer.Payload inFlight = buffer.pollForSend(1);

        assertTrue(buffer.retryOrDiscardSend(inFlight, 100, true));
        assertEquals(1, buffer.pendingCount());
        assertEquals(List.of("queued"), buffer.queuedPayloadsSnapshot());
        assertEquals(1, buffer.pruneExpiredQueued(101));
        assertEquals(0, buffer.pendingCount());
        assertEquals(0L, buffer.pendingUtf8Bytes());
    }

    @Test
    void terminalClearReleasesQueuedAndInFlightPayloads() {
        PendingPayloadBuffer buffer = new PendingPayloadBuffer(3, 30, 10, 1, 100);
        buffer.offer("first", 0);
        buffer.offer("second", 0);
        buffer.pollForSend(0);

        buffer.clear();

        assertEquals(0, buffer.pendingCount());
        assertEquals(0, buffer.queuedCount());
        assertEquals(0L, buffer.pendingUtf8Bytes());
        assertFalse(buffer.hasInFlightPayload());
        assertTrue(buffer.queuedPayloadsSnapshot().isEmpty());
    }

    @Test
    void concurrentOffersNeverExceedCountOrByteBounds() throws Exception {
        int attempts = 256;
        int limit = 64;
        PendingPayloadBuffer buffer = new PendingPayloadBuffer(limit, limit, 1, 0, 1_000);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        try {
            for (int index = 0; index < attempts; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    if (buffer.offer("x", 0) == PendingPayloadBuffer.AdmissionResult.ACCEPTED) {
                        accepted.incrementAndGet();
                    }
                    return null;
                }));
            }
            boolean allReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertTrue(allReady);
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(limit, accepted.get());
        assertEquals(limit, buffer.pendingCount());
        assertEquals(limit, buffer.pendingUtf8Bytes());
    }
}
