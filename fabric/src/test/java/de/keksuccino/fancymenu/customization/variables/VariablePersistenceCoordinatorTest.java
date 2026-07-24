package de.keksuccino.fancymenu.customization.variables;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariablePersistenceCoordinatorTest {

    @Test
    void capturedNewerRevisionMakesItsLateDirtyNotificationObsolete() {
        AtomicLong stateRevision = new AtomicLong(2L);
        ManualScheduler scheduler = new ManualScheduler();
        List<Long> writes = new ArrayList<>();
        VariablePersistenceCoordinator coordinator = new VariablePersistenceCoordinator(() -> snapshot(stateRevision.get()), (snapshot, operation) -> {
            writes.add(snapshot.revision());
            return true;
        }, scheduler, 10L, 20L);

        coordinator.markDirty(1L, "revision one");
        assertTrue(scheduler.runNext());
        coordinator.markDirty(2L, "late revision two notification");

        assertAll(() -> assertEquals(List.of(2L), writes), () -> assertEquals(0, scheduler.pendingTaskCount()));
    }

    @Test
    void shutdownFlushesCapturedRevisionWithoutWaitingForDirtyNotification() {
        AtomicLong stateRevision = new AtomicLong(1L);
        ManualScheduler scheduler = new ManualScheduler();
        List<Long> writes = new ArrayList<>();
        VariablePersistenceCoordinator coordinator = new VariablePersistenceCoordinator(() -> snapshot(stateRevision.get()), (snapshot, operation) -> {
            writes.add(snapshot.revision());
            return true;
        }, scheduler, 10L, 20L);

        assertTrue(coordinator.shutdown(1L, "shutdown"));
        coordinator.markDirty(1L, "late mutation notification");

        assertAll(() -> assertEquals(List.of(1L), writes), () -> assertTrue(scheduler.shutdown), () -> assertEquals(0, scheduler.pendingTaskCount()));
    }

    @Test
    void failedWriteKeepsDirtyRevisionAndUsesRetryDelay() {
        AtomicLong stateRevision = new AtomicLong(1L);
        ManualScheduler scheduler = new ManualScheduler();
        int[] attempts = {0};
        VariablePersistenceCoordinator coordinator = new VariablePersistenceCoordinator(() -> snapshot(stateRevision.get()), (snapshot, operation) -> ++attempts[0] > 1, scheduler, 10L, 20L);

        coordinator.markDirty(1L, "mutation");
        assertTrue(scheduler.runNext());

        assertAll(() -> assertEquals(List.of(20L), scheduler.pendingDelays()), () -> assertEquals(1, attempts[0]));
        assertTrue(scheduler.runNext());
        assertAll(() -> assertEquals(2, attempts[0]), () -> assertEquals(0, scheduler.pendingTaskCount()));
    }

    @Test
    void cleanExclusiveRevisionRejectsOlderLateDirtyNotification() {
        AtomicLong stateRevision = new AtomicLong(1L);
        ManualScheduler scheduler = new ManualScheduler();
        List<Long> writes = new ArrayList<>();
        VariablePersistenceCoordinator coordinator = new VariablePersistenceCoordinator(() -> snapshot(stateRevision.get()), (snapshot, operation) -> {
            writes.add(snapshot.revision());
            return true;
        }, scheduler, 10L, 20L);

        coordinator.markDirty(1L, "before reload");
        assertTrue(coordinator.runExclusive(1L, "pre-reload flush", () -> {
            stateRevision.set(2L);
            return VariablePersistenceCoordinator.ExclusiveCommit.clean(2L);
        }));
        coordinator.markDirty(1L, "late pre-reload notification");

        assertAll(() -> assertEquals(List.of(1L), writes), () -> assertEquals(0, scheduler.pendingTaskCount()), () -> assertFalse(scheduler.shutdown));
    }

    @Test
    void flushTargetCanCompleteWhileNewerConcurrentRevisionRemainsScheduled() throws Exception {
        AtomicLong stateRevision = new AtomicLong(1L);
        ManualScheduler scheduler = new ManualScheduler();
        CountDownLatch writeStarted = new CountDownLatch(1);
        CountDownLatch releaseWrite = new CountDownLatch(1);
        AtomicReference<Boolean> flushResult = new AtomicReference<>();
        VariablePersistenceCoordinator coordinator = new VariablePersistenceCoordinator(() -> snapshot(stateRevision.get()), (snapshot, operation) -> {
            writeStarted.countDown();
            try {
                releaseWrite.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        }, scheduler, 10L, 20L);
        Thread flushThread = new Thread(() -> flushResult.set(coordinator.flush(1L, "flush one")), "VariablePersistenceCoordinatorTest-Flush");

        try {
            flushThread.start();
            assertTrue(writeStarted.await(5L, TimeUnit.SECONDS));
            stateRevision.set(2L);
            coordinator.markDirty(2L, "revision two");
        } finally {
            releaseWrite.countDown();
            flushThread.join(5_000L);
        }

        assertAll(() -> assertFalse(flushThread.isAlive()), () -> assertEquals(Boolean.TRUE, flushResult.get()), () -> assertEquals(1, scheduler.pendingTaskCount()));
    }

    @Test
    void alreadyDurableOlderFlushTargetDoesNotCancelNewerDirtyRevision() {
        AtomicLong stateRevision = new AtomicLong(1L);
        ManualScheduler scheduler = new ManualScheduler();
        List<Long> writes = new ArrayList<>();
        VariablePersistenceCoordinator coordinator = new VariablePersistenceCoordinator(() -> snapshot(stateRevision.get()), (snapshot, operation) -> {
            writes.add(snapshot.revision());
            return true;
        }, scheduler, 10L, 20L);
        assertTrue(coordinator.flush(1L, "initial flush"));
        stateRevision.set(2L);
        coordinator.markDirty(2L, "newer mutation");

        assertTrue(coordinator.flush(1L, "stale flush target"));

        assertAll(() -> assertEquals(List.of(1L), writes), () -> assertEquals(1, scheduler.pendingTaskCount()));
    }

    @NotNull
    private static VariablePersistenceCoordinator.PersistenceSnapshot snapshot(long revision) {
        return new VariablePersistenceCoordinator.PersistenceSnapshot(revision, "revision=" + revision);
    }

    private static final class ManualScheduler implements VariablePersistenceCoordinator.Scheduler {

        private final Deque<ManualTask> tasks = new ArrayDeque<>();
        private boolean shutdown;

        @Override
        public synchronized @NotNull VariablePersistenceCoordinator.ScheduledTask schedule(@NotNull Runnable task, long delayMillis) {
            ManualTask scheduledTask = new ManualTask(task, delayMillis);
            this.tasks.addLast(scheduledTask);
            return scheduledTask;
        }

        @Override
        public synchronized void shutdown() {
            this.shutdown = true;
            this.tasks.clear();
        }

        boolean runNext() {
            ManualTask task;
            synchronized (this) {
                do {
                    task = this.tasks.pollFirst();
                } while (task != null && task.cancelled);
            }
            if (task == null) return false;
            task.runnable.run();
            return true;
        }

        synchronized int pendingTaskCount() {
            return (int) this.tasks.stream().filter(task -> !task.cancelled).count();
        }

        synchronized @NotNull List<Long> pendingDelays() {
            return this.tasks.stream().filter(task -> !task.cancelled).map(task -> task.delayMillis).toList();
        }

    }

    private static final class ManualTask implements VariablePersistenceCoordinator.ScheduledTask {

        private final Runnable runnable;
        private final long delayMillis;
        private boolean cancelled;

        private ManualTask(@NotNull Runnable runnable, long delayMillis) {
            this.runnable = runnable;
            this.delayMillis = delayMillis;
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }

    }

}
