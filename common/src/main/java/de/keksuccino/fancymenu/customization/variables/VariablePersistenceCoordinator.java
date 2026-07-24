package de.keksuccino.fancymenu.customization.variables;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Serializes debounced writes, explicit flushes, and database-replacement transactions without keeping the variable
 * state monitor across filesystem I/O. Revisions are monotonic: a late dirty notification at or below an already
 * durable replacement revision is intentionally ignored.
 */
final class VariablePersistenceCoordinator {

    static final long DEFAULT_DEBOUNCE_MILLIS = 250L;
    static final long DEFAULT_RETRY_MILLIS = 1_000L;

    private static final Logger LOGGER = LogManager.getLogger();

    private final Object persistenceLock = new Object();
    private final SnapshotSource snapshotSource;
    private final SnapshotWriter snapshotWriter;
    private final Scheduler scheduler;
    private final long debounceMillis;
    private final long retryMillis;

    private long requestedRevision;
    private long durableRevision;
    private long scheduleGeneration;
    @NotNull
    private String requestedOperation = "updating variables";
    private boolean writeInProgress;
    private boolean exclusiveInProgress;
    private boolean closed;
    private ScheduledTask scheduledTask;

    VariablePersistenceCoordinator(@NotNull SnapshotSource snapshotSource, @NotNull SnapshotWriter snapshotWriter, @NotNull Scheduler scheduler) {
        this(snapshotSource, snapshotWriter, scheduler, DEFAULT_DEBOUNCE_MILLIS, DEFAULT_RETRY_MILLIS);
    }

    VariablePersistenceCoordinator(@NotNull SnapshotSource snapshotSource, @NotNull SnapshotWriter snapshotWriter, @NotNull Scheduler scheduler, long debounceMillis, long retryMillis) {
        this.snapshotSource = Objects.requireNonNull(snapshotSource);
        this.snapshotWriter = Objects.requireNonNull(snapshotWriter);
        this.scheduler = Objects.requireNonNull(scheduler);
        if (debounceMillis < 0L) throw new IllegalArgumentException("debounceMillis must not be negative");
        if (retryMillis < 0L) throw new IllegalArgumentException("retryMillis must not be negative");
        this.debounceMillis = debounceMillis;
        this.retryMillis = retryMillis;
    }

    void markDirty(long revision, @NotNull String operation) {
        synchronized (this.persistenceLock) {
            if (this.closed || revision <= this.durableRevision) return;
            this.recordRequestLocked(revision, operation);
            if (!this.writeInProgress && !this.exclusiveInProgress) this.scheduleLocked(this.debounceMillis);
        }
    }

    boolean flush(long revision, @NotNull String operation) {
        boolean interrupted = false;
        synchronized (this.persistenceLock) {
            if (this.closed) return revision <= this.durableRevision;
            this.recordRequestLocked(revision, operation);
            this.cancelScheduledLocked();
            while (this.writeInProgress || this.exclusiveInProgress) {
                interrupted |= this.waitLocked();
            }
            if (revision <= this.durableRevision) {
                // A newer mutation can race the store's out-of-lock flush call after it captured this older target.
                if (this.requestedRevision > this.durableRevision) this.scheduleLocked(this.debounceMillis);
                if (interrupted) Thread.currentThread().interrupt();
                return true;
            }
            this.writeInProgress = true;
        }

        boolean success = this.performClaimedWrite(false);
        synchronized (this.persistenceLock) {
            boolean durable = revision <= this.durableRevision;
            // The requested flush target can be durable while a concurrently admitted newer revision is still dirty.
            if (this.requestedRevision > this.durableRevision && !this.closed) this.scheduleLocked(success ? this.debounceMillis : this.retryMillis);
            if (interrupted) Thread.currentThread().interrupt();
            return durable;
        }
    }

    boolean runExclusive(long revisionToFlushFirst, @NotNull String flushOperation, @NotNull ExclusiveOperation operation) {
        Objects.requireNonNull(flushOperation);
        Objects.requireNonNull(operation);
        boolean interrupted = false;
        synchronized (this.persistenceLock) {
            if (this.closed) return false;
            this.cancelScheduledLocked();
            while (this.writeInProgress || this.exclusiveInProgress) {
                interrupted |= this.waitLocked();
            }
            this.exclusiveInProgress = true;
            if (revisionToFlushFirst >= 0L) this.recordRequestLocked(revisionToFlushFirst, flushOperation);
        }

        boolean completed = false;
        try {
            if (revisionToFlushFirst >= 0L && !this.writeUntilDurableExclusive(revisionToFlushFirst)) return false;
            ExclusiveCommit commit = Objects.requireNonNull(operation.run());
            synchronized (this.persistenceLock) {
                if (commit.cleanRevision() >= 0L) {
                    this.durableRevision = Math.max(this.durableRevision, commit.cleanRevision());
                    this.requestedRevision = Math.max(this.requestedRevision, commit.cleanRevision());
                }
                if (commit.dirtyRevision() >= 0L) this.recordRequestLocked(commit.dirtyRevision(), commit.dirtyOperation());
            }
            if (commit.dirtyRevision() >= 0L && !this.writeUntilDurableExclusive(commit.dirtyRevision())) return false;
            completed = true;
            return true;
        } finally {
            synchronized (this.persistenceLock) {
                this.exclusiveInProgress = false;
                this.persistenceLock.notifyAll();
                if (this.requestedRevision > this.durableRevision && !this.closed) this.scheduleLocked(completed ? this.debounceMillis : this.retryMillis);
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    boolean shutdown(long revision, @NotNull String operation) {
        boolean durable = this.flush(revision, operation);
        synchronized (this.persistenceLock) {
            if (this.closed) return revision <= this.durableRevision;
            this.closed = true;
            this.cancelScheduledLocked();
        }
        this.scheduler.shutdown();
        return durable;
    }

    private boolean writeUntilDurableExclusive(long revision) {
        while (true) {
            synchronized (this.persistenceLock) {
                if (revision <= this.durableRevision) return true;
                this.writeInProgress = true;
            }
            if (!this.performClaimedWrite(false)) return false;
        }
    }

    private void runScheduled(long generation) {
        synchronized (this.persistenceLock) {
            if (this.closed || generation != this.scheduleGeneration) return;
            this.scheduledTask = null;
            if (this.writeInProgress || this.exclusiveInProgress || this.requestedRevision <= this.durableRevision) return;
            this.writeInProgress = true;
        }
        this.performClaimedWrite(true);
    }

    private boolean performClaimedWrite(boolean scheduleAfter) {
        PersistenceSnapshot snapshot = null;
        boolean success = false;
        try {
            snapshot = Objects.requireNonNull(this.snapshotSource.capture());
            success = this.snapshotWriter.write(snapshot, this.operationForSnapshot(snapshot.revision()));
            return success;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to capture a complete variable snapshot for persistence.", ex);
            return false;
        } finally {
            synchronized (this.persistenceLock) {
                if (success && snapshot != null) this.durableRevision = Math.max(this.durableRevision, snapshot.revision());
                this.writeInProgress = false;
                this.persistenceLock.notifyAll();
                if (scheduleAfter && this.requestedRevision > this.durableRevision && !this.closed && !this.exclusiveInProgress) this.scheduleLocked(success ? this.debounceMillis : this.retryMillis);
            }
        }
    }

    @NotNull
    private String operationForSnapshot(long snapshotRevision) {
        synchronized (this.persistenceLock) {
            return (snapshotRevision >= this.requestedRevision) ? this.requestedOperation : "persisting an earlier variable snapshot";
        }
    }

    private void recordRequestLocked(long revision, @NotNull String operation) {
        if (revision >= this.requestedRevision) {
            this.requestedRevision = revision;
            this.requestedOperation = Objects.requireNonNull(operation);
        }
    }

    private void scheduleLocked(long delayMillis) {
        this.cancelScheduledLocked();
        long generation = ++this.scheduleGeneration;
        this.scheduledTask = this.scheduler.schedule(() -> this.runScheduled(generation), delayMillis);
    }

    private void cancelScheduledLocked() {
        this.scheduleGeneration++;
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel();
            this.scheduledTask = null;
        }
    }

    private boolean waitLocked() {
        try {
            this.persistenceLock.wait();
            return false;
        } catch (InterruptedException ex) {
            return true;
        }
    }

    record PersistenceSnapshot(long revision, @NotNull String serializedVariables) {
    }

    record ExclusiveCommit(long cleanRevision, long dirtyRevision, @NotNull String dirtyOperation) {

        static @NotNull ExclusiveCommit unchanged() {
            return new ExclusiveCommit(-1L, -1L, "updating variables");
        }

        static @NotNull ExclusiveCommit clean(long revision) {
            return new ExclusiveCommit(revision, -1L, "updating variables");
        }

        static @NotNull ExclusiveCommit dirty(long revision, @NotNull String operation) {
            return new ExclusiveCommit(-1L, revision, operation);
        }

    }

    @FunctionalInterface
    interface SnapshotSource {

        @NotNull PersistenceSnapshot capture() throws Exception;

    }

    @FunctionalInterface
    interface SnapshotWriter {

        boolean write(@NotNull PersistenceSnapshot snapshot, @NotNull String operation);

    }

    @FunctionalInterface
    interface ExclusiveOperation {

        @NotNull ExclusiveCommit run();

    }

    interface Scheduler {

        @NotNull ScheduledTask schedule(@NotNull Runnable task, long delayMillis);

        void shutdown();

    }

    @FunctionalInterface
    interface ScheduledTask {

        void cancel();

    }

    static final class ExecutorScheduler implements Scheduler {

        private static final long TERMINATION_TIMEOUT_SECONDS = 5L;

        private final ScheduledThreadPoolExecutor executor;

        ExecutorScheduler(@NotNull String threadName) {
            ThreadFactory threadFactory = runnable -> {
                Thread thread = new Thread(runnable, Objects.requireNonNull(threadName));
                thread.setDaemon(true);
                thread.setUncaughtExceptionHandler((failedThread, throwable) -> LOGGER.error("[FANCYMENU] Uncaught exception in variable persistence thread '{}'.", failedThread.getName(), throwable));
                return thread;
            };
            this.executor = new ScheduledThreadPoolExecutor(1, threadFactory);
            this.executor.setRemoveOnCancelPolicy(true);
            this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            this.executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        }

        @Override
        public @NotNull ScheduledTask schedule(@NotNull Runnable task, long delayMillis) {
            ScheduledFuture<?> future = this.executor.schedule(Objects.requireNonNull(task), delayMillis, TimeUnit.MILLISECONDS);
            return () -> future.cancel(false);
        }

        @Override
        public void shutdown() {
            this.executor.shutdownNow();
            boolean interrupted = false;
            try {
                while (!this.executor.isTerminated()) {
                    try {
                        if (!this.executor.awaitTermination(TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                            LOGGER.error("[FANCYMENU] Variable persistence executor did not terminate within {} seconds.", TERMINATION_TIMEOUT_SECONDS);
                            break;
                        }
                    } catch (InterruptedException ex) {
                        interrupted = true;
                    }
                }
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
        }

    }

}
