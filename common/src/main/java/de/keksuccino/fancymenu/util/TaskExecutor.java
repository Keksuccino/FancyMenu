package de.keksuccino.fancymenu.util;

import de.keksuccino.fancymenu.util.threading.FancyMenuExecutors;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskExecutor {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ScheduledExecutorService EXECUTOR = FancyMenuExecutors.newScheduledThreadPool(20, "FancyMenu-TaskExecutor");

    public static void scheduleAtFixedRate(@NotNull Task task, long initialDelay, long period, @NotNull TimeUnit unit, boolean executeInMainThread) {
        CompletableFuture<ScheduledFuture<?>> future = new CompletableFuture<>();
        future.complete(EXECUTOR.scheduleAtFixedRate(() -> runScheduled(task, future, executeInMainThread, "[FANCYMENU] Error while trying to execute scheduled (fixed rate) task!"), initialDelay, period, unit));
    }

    public static void schedule(@NotNull Task task, long delay, @NotNull TimeUnit unit, boolean executeInMainThread) {
        CompletableFuture<ScheduledFuture<?>> future = new CompletableFuture<>();
        future.complete(EXECUTOR.schedule(() -> runScheduled(task, future, executeInMainThread, "[FANCYMENU] Error while trying to execute scheduled task!"), delay, unit));
    }

    /**
     * Runs a task of {@link #schedule} or {@link #scheduleAtFixedRate} with the future of its schedule.<br>
     * The executor can start a task before scheduling it returned its future, like a task without delay while the thread that scheduled it
     * got paused right after scheduling it. So the executor thread waits for the future, which that thread hands over right after scheduling,
     * before the task runs. That wait is short, and it happens before the task gets queued, so the main thread never waits.
     */
    private static void runScheduled(@NotNull Task task, @NotNull CompletableFuture<ScheduledFuture<?>> future, boolean executeInMainThread, @NotNull String errorMessage) {
        ScheduledFuture<?> scheduledFuture = future.join();
        final Runnable r = () -> {
            try {
                task.run(scheduledFuture);
            } catch (Exception ex) {
                LOGGER.error(errorMessage, ex);
            }
        };
        if (executeInMainThread) {
            MainThreadTaskExecutor.executeInMainThread(r, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        } else {
            r.run();
        }
    }

    /**
     * Schedules cancellable one-shot work. The cancellation guard is checked again inside the main-thread wrapper because
     * the scheduler future is already complete by the time an enqueued callback is eventually drained.
     */
    @NotNull
    public static CancellableTask scheduleCancellable(@NotNull Runnable task, long delay, @NotNull TimeUnit unit, boolean executeInMainThread) {
        CancellableOneShotTask scheduledTask = new CancellableOneShotTask(Objects.requireNonNull(task), executeInMainThread);
        ScheduledFuture<?> future = EXECUTOR.schedule(scheduledTask::dispatch, delay, Objects.requireNonNull(unit));
        scheduledTask.attach(future);
        return scheduledTask;
    }

    public static void execute(@NotNull Runnable task, boolean executeInMainThread) {
        EXECUTOR.execute(() -> {
            final Runnable r = () -> {
                try {
                    task.run();
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Error while trying to execute task!", ex);
                }
            };
            if (executeInMainThread) {
                MainThreadTaskExecutor.executeInMainThread(r, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                r.run();
            }
        });
    }

    @FunctionalInterface
    public interface Task {
        void run(@NotNull ScheduledFuture<?> future);
    }

    public interface CancellableTask {

        void cancel();

        boolean isCancelled();
    }

    static final class CancellableOneShotTask implements CancellableTask {

        private final Runnable task;
        private final boolean executeInMainThread;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile ScheduledFuture<?> future;

        CancellableOneShotTask(@NotNull Runnable task, boolean executeInMainThread) {
            this.task = task;
            this.executeInMainThread = executeInMainThread;
        }

        private void attach(@NotNull ScheduledFuture<?> future) {
            this.future = future;
            if (this.cancelled.get()) future.cancel(false);
        }

        void dispatch() {
            if (this.cancelled.get()) return;
            Runnable guardedTask = () -> {
                if (this.cancelled.get()) return;
                try {
                    this.task.run();
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Error while trying to execute cancellable scheduled task!", ex);
                }
            };
            if (this.executeInMainThread) {
                MainThreadTaskExecutor.executeInMainThread(guardedTask, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                guardedTask.run();
            }
        }

        @Override
        public void cancel() {
            if (!this.cancelled.compareAndSet(false, true)) return;
            ScheduledFuture<?> scheduledFuture = this.future;
            if (scheduledFuture != null) scheduledFuture.cancel(false);
        }

        @Override
        public boolean isCancelled() {
            return this.cancelled.get();
        }
    }

}
