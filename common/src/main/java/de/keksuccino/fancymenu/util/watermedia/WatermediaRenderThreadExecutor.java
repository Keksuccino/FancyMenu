package de.keksuccino.fancymenu.util.watermedia;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Tracks render work accepted for one Watermedia graphics engine. Closing rejects new work but lets every accepted task
 * finish before the final cleanup runs on the render thread. This ordering is essential because Watermedia 3.0.0.19
 * keeps queued GL submissions valid until they execute, even after its player and audio engine have been released.
 */
final class WatermediaRenderThreadExecutor implements Executor {

    private final Object lock = new Object();
    private final Thread renderThread;
    private final Executor delegate;
    private final Executor cleanupFallbackExecutor;
    private boolean closed = false;
    private int pendingTaskCount = 0;
    private Runnable cleanupTask = null;

    WatermediaRenderThreadExecutor(@NotNull Thread renderThread, @NotNull Executor delegate) {
        this(renderThread, delegate, task -> MainThreadTaskExecutor.executeInMainThread(task, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK));
    }

    WatermediaRenderThreadExecutor(@NotNull Thread renderThread, @NotNull Executor delegate, @NotNull Executor cleanupFallbackExecutor) {
        this.renderThread = Objects.requireNonNull(renderThread);
        this.delegate = Objects.requireNonNull(delegate);
        this.cleanupFallbackExecutor = Objects.requireNonNull(cleanupFallbackExecutor);
    }

    @Override
    public void execute(@NotNull Runnable task) {
        Objects.requireNonNull(task);
        synchronized (this.lock) {
            if (this.closed) return;
            this.pendingTaskCount++;
        }
        TrackedTask trackedTask = new TrackedTask(this, task);
        try {
            this.delegate.execute(trackedTask);
        } catch (RuntimeException | Error throwable) {
            // An inline executor propagates the task's own failure through execute(). Its finally block already
            // completed the task, while a true delegate rejection still needs to undo the accepted-task count here.
            if (!trackedTask.started) this.onTaskCompleted();
            throw throwable;
        }
    }

    void close(@NotNull Runnable cleanupTask) {
        Objects.requireNonNull(cleanupTask);
        Runnable claimedCleanupTask;
        synchronized (this.lock) {
            if (this.closed) return;
            this.closed = true;
            this.cleanupTask = cleanupTask;
            claimedCleanupTask = this.claimCleanupIfReady();
        }
        if (claimedCleanupTask == null) return;
        this.dispatchCleanup(claimedCleanupTask);
    }

    private void onTaskCompleted() {
        Runnable claimedCleanupTask;
        synchronized (this.lock) {
            this.pendingTaskCount--;
            claimedCleanupTask = this.claimCleanupIfReady();
        }
        if (claimedCleanupTask != null) this.dispatchCleanup(claimedCleanupTask);
    }

    private Runnable claimCleanupIfReady() {
        if (!this.closed || this.pendingTaskCount != 0 || this.cleanupTask == null) return null;
        Runnable claimedCleanupTask = this.cleanupTask;
        this.cleanupTask = null;
        return claimedCleanupTask;
    }

    private void dispatchCleanup(@NotNull Runnable claimedCleanupTask) {
        if (Thread.currentThread() == this.renderThread) {
            claimedCleanupTask.run();
        } else {
            try {
                this.delegate.execute(claimedCleanupTask);
            } catch (RuntimeException | Error delegateFailure) {
                try {
                    // The normal render executor may reject during shutdown. FancyMenu's local main-thread queue keeps
                    // ownership of the already-claimed GFX cleanup until the next render-thread PRE tick.
                    this.cleanupFallbackExecutor.execute(claimedCleanupTask);
                } catch (RuntimeException | Error fallbackFailure) {
                    delegateFailure.addSuppressed(fallbackFailure);
                    throw delegateFailure;
                }
            }
        }
    }

    private static final class TrackedTask implements Runnable {

        private final WatermediaRenderThreadExecutor owner;
        private final Runnable task;
        private volatile boolean started = false;

        private TrackedTask(@NotNull WatermediaRenderThreadExecutor owner, @NotNull Runnable task) {
            this.owner = owner;
            this.task = task;
        }

        @Override
        public void run() {
            this.started = true;
            try {
                this.task.run();
            } finally {
                this.owner.onTaskCompleted();
            }
        }
    }
}
