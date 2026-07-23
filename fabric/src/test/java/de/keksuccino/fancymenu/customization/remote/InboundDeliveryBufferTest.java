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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InboundDeliveryBufferTest {

    @Test
    void countAndByteLimitsDropNewestWhilePreservingFifo() {
        InboundDeliveryBuffer<String> countBound = new InboundDeliveryBuffer<>(2, 10);

        assertTrue(countBound.offer("first", 5));
        assertTrue(countBound.offer("second", 5));
        assertFalse(countBound.offer("newest", 0));
        assertEquals("first", countBound.poll());
        assertEquals("second", countBound.poll());
        assertNull(countBound.poll());

        InboundDeliveryBuffer<String> byteBound = new InboundDeliveryBuffer<>(3, 5);
        assertTrue(byteBound.offer("exact", 5));
        assertFalse(byteBound.offer("over", 1));
        assertEquals(5L, byteBound.queuedUtf8Bytes());
    }

    @Test
    void terminalClearReleasesAllAccounting() {
        InboundDeliveryBuffer<String> buffer = new InboundDeliveryBuffer<>(2, 10);
        buffer.offer("first", 4);
        buffer.offer("second", 6);

        buffer.clear();

        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
        assertEquals(0L, buffer.queuedUtf8Bytes());
    }

    @Test
    void concurrentOffersAreBoundedAndCoalesceToOneDrainTask() throws Exception {
        int attempts = 128;
        int limit = 32;
        InboundDeliveryBuffer<Integer> buffer = new InboundDeliveryBuffer<>(limit, limit);
        CoalescingTaskGate drainGate = new CoalescingTaskGate();
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger admitted = new AtomicInteger();
        AtomicInteger scheduledDrainTasks = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int index = 0; index < attempts; index++) {
                int value = index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    if (buffer.offer(value, 1)) {
                        admitted.incrementAndGet();
                        if (drainGate.tryAcquire()) {
                            scheduledDrainTasks.incrementAndGet();
                        }
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
        }

        assertEquals(limit, admitted.get());
        assertEquals(limit, buffer.size());
        assertEquals(limit, buffer.queuedUtf8Bytes());
        assertEquals(1, scheduledDrainTasks.get());
        assertTrue(drainGate.isAcquired());

        while (buffer.poll() != null) {
        }
        drainGate.release();
        assertFalse(drainGate.isAcquired());
        assertTrue(drainGate.tryAcquire());
        drainGate.release();
        assertThrows(IllegalStateException.class, drainGate::release);
    }
}
