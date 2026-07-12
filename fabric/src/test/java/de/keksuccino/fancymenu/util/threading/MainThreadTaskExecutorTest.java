package de.keksuccino.fancymenu.util.threading;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainThreadTaskExecutorTest {

    @Test
    void drainHoldsTheQueueMonitorAcrossSnapshotAndClear() {
        LockCheckingList queue = new LockCheckingList();
        queue.add(() -> { });

        List<Runnable> drained = MainThreadTaskExecutor.drainQueue(queue);

        assertEquals(1, drained.size());
        assertTrue(queue.monitorHeldDuringSnapshot);
        assertTrue(queue.monitorHeldDuringClear);
    }

    @Test
    void enqueueRacingDrainRemainsQueuedWhenNotInSnapshot() throws Exception {
        CountDownLatch snapshotStarted = new CountDownLatch(1);
        CountDownLatch enqueueAttempted = new CountDownLatch(1);
        HookedList queue = new HookedList(snapshotStarted, enqueueAttempted);
        Runnable initialTask = () -> { };
        Runnable racingTask = () -> { };
        queue.add(initialTask);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<List<Runnable>> drain = executor.submit(() -> MainThreadTaskExecutor.drainQueue(queue));
        Future<?> enqueue = executor.submit(() -> {
            assertTrue(snapshotStarted.await(5, TimeUnit.SECONDS));
            enqueueAttempted.countDown();
            queue.add(racingTask);
            return null;
        });

        try {
            assertEquals(List.of(initialTask), drain.get(10, TimeUnit.SECONDS));
            enqueue.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(List.of(racingTask), queue);
    }

    private static final class LockCheckingList extends ArrayList<Runnable> {

        private boolean monitorHeldDuringSnapshot;
        private boolean monitorHeldDuringClear;

        @Override
        public Object[] toArray() {
            this.monitorHeldDuringSnapshot = Thread.holdsLock(this);
            return super.toArray();
        }

        @Override
        public void clear() {
            this.monitorHeldDuringClear = Thread.holdsLock(this);
            super.clear();
        }

    }

    private static final class HookedList extends ArrayList<Runnable> {

        private final CountDownLatch snapshotStarted;
        private final CountDownLatch enqueueAttempted;

        private HookedList(CountDownLatch snapshotStarted, CountDownLatch enqueueAttempted) {
            this.snapshotStarted = snapshotStarted;
            this.enqueueAttempted = enqueueAttempted;
        }

        @Override
        public synchronized boolean add(Runnable task) {
            return super.add(task);
        }

        @Override
        public synchronized Object[] toArray() {
            this.snapshotStarted.countDown();
            try {
                if (!this.enqueueAttempted.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent enqueue did not reach the queue");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while coordinating queue drain", ex);
            }
            return super.toArray();
        }

        @Override
        public synchronized void clear() {
            super.clear();
        }

    }

}
