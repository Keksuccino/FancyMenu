package de.keksuccino.fancymenu.customization.background.backgrounds.video;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Owns the two recurring tasks used by one video background. The controller is intentionally restartable after
 * {@link #stop()} because an inactive background can be rendered again, while {@link #close()} is terminal.
 */
public final class VideoBackgroundTaskController implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Object lifecycleLock = new Object();
    private final FixedRateScheduler scheduler;
    private final Runnable watchdogTask;
    private final Runnable asyncTickerTask;
    private final long watchdogPeriodMillis;
    private final long asyncTickerPeriodMillis;

    private long generation;
    private boolean closed;
    @Nullable
    private ScheduledRun scheduledRun;

    public VideoBackgroundTaskController(@NotNull ScheduledExecutorService executor, @NotNull Runnable watchdogTask, long watchdogPeriodMillis, @NotNull Runnable asyncTickerTask, long asyncTickerPeriodMillis) {
        this(new ExecutorFixedRateScheduler(executor), watchdogTask, watchdogPeriodMillis, asyncTickerTask, asyncTickerPeriodMillis);
    }

    VideoBackgroundTaskController(@NotNull FixedRateScheduler scheduler, @NotNull Runnable watchdogTask, long watchdogPeriodMillis, @NotNull Runnable asyncTickerTask, long asyncTickerPeriodMillis) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.watchdogTask = Objects.requireNonNull(watchdogTask);
        this.asyncTickerTask = Objects.requireNonNull(asyncTickerTask);
        this.watchdogPeriodMillis = requirePositivePeriod(watchdogPeriodMillis, "watchdogPeriodMillis");
        this.asyncTickerPeriodMillis = requirePositivePeriod(asyncTickerPeriodMillis, "asyncTickerPeriodMillis");
    }

    /** Lazily starts both recurring tasks. Repeated calls never register duplicate work. */
    public boolean start() {
        ScheduledRun run;
        synchronized (this.lifecycleLock) {
            if (this.closed) return false;
            if (this.scheduledRun != null) return true;
            run = new ScheduledRun(++this.generation);
            this.scheduledRun = run;
        }

        try {
            ScheduledTask watchdog = Objects.requireNonNull(this.scheduler.scheduleAtFixedRate(() -> this.runIfCurrent(run, this.watchdogTask), 0L, this.watchdogPeriodMillis));
            if (!this.publishTask(run, watchdog, true)) return false;
            ScheduledTask asyncTicker = Objects.requireNonNull(this.scheduler.scheduleAtFixedRate(() -> this.runIfCurrent(run, this.asyncTickerTask), 0L, this.asyncTickerPeriodMillis));
            return this.publishTask(run, asyncTicker, false);
        } catch (RuntimeException ex) {
            this.handleSchedulingFailure(run, ex);
            return false;
        }
    }

    /** Cancels both tasks without making the controller terminal, allowing a later render to restart them. */
    public void stop() {
        ScheduledRun run;
        synchronized (this.lifecycleLock) {
            run = this.scheduledRun;
            if (run == null) return;
            this.scheduledRun = null;
            this.generation++;
        }
        cancelRun(run);
    }

    public boolean isRunning() {
        synchronized (this.lifecycleLock) {
            return this.scheduledRun != null;
        }
    }

    public boolean isClosed() {
        synchronized (this.lifecycleLock) {
            return this.closed;
        }
    }

    /** Permanently cancels both tasks and rejects later restart attempts. */
    @Override
    public void close() {
        ScheduledRun run;
        synchronized (this.lifecycleLock) {
            if (this.closed) return;
            this.closed = true;
            run = this.scheduledRun;
            this.scheduledRun = null;
            this.generation++;
        }
        cancelRun(run);
    }

    private boolean publishTask(@NotNull ScheduledRun run, @NotNull ScheduledTask task, boolean watchdog) {
        boolean accepted;
        synchronized (this.lifecycleLock) {
            accepted = !this.closed && this.scheduledRun == run && this.generation == run.generation;
            if (accepted) {
                if (watchdog) run.watchdog = task;
                else run.asyncTicker = task;
            }
        }
        if (!accepted) cancelTask(task);
        return accepted;
    }

    private void runIfCurrent(@NotNull ScheduledRun run, @NotNull Runnable task) {
        synchronized (this.lifecycleLock) {
            if (this.closed || this.scheduledRun != run || this.generation != run.generation) return;
        }
        try {
            task.run();
        } catch (Exception ex) {
            // ScheduledExecutorService suppresses every later fixed-rate execution when one invocation escapes with an exception.
            LOGGER.error("[FANCYMENU] Error while running a video background lifecycle task!", ex);
        }
    }

    private void handleSchedulingFailure(@NotNull ScheduledRun run, @NotNull RuntimeException failure) {
        boolean ownedFailure;
        synchronized (this.lifecycleLock) {
            ownedFailure = this.scheduledRun == run;
            if (ownedFailure) {
                this.closed = true;
                this.scheduledRun = null;
                this.generation++;
            }
        }
        cancelRun(run);
        if (ownedFailure) LOGGER.error("[FANCYMENU] Failed to start video background lifecycle tasks!", failure);
    }

    private static void cancelRun(@Nullable ScheduledRun run) {
        if (run == null) return;
        cancelTask(run.watchdog);
        cancelTask(run.asyncTicker);
    }

    private static void cancelTask(@Nullable ScheduledTask task) {
        if (task == null) return;
        try {
            task.cancel();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to cancel a video background lifecycle task!", ex);
        }
    }

    private static long requirePositivePeriod(long periodMillis, @NotNull String name) {
        if (periodMillis <= 0L) throw new IllegalArgumentException(name + " must be positive");
        return periodMillis;
    }

    interface FixedRateScheduler {

        @NotNull ScheduledTask scheduleAtFixedRate(@NotNull Runnable task, long initialDelayMillis, long periodMillis);
    }

    interface ScheduledTask {

        void cancel();
    }

    private static final class ScheduledRun {

        private final long generation;
        @Nullable
        private ScheduledTask watchdog;
        @Nullable
        private ScheduledTask asyncTicker;

        private ScheduledRun(long generation) {
            this.generation = generation;
        }
    }

    private record ExecutorFixedRateScheduler(@NotNull ScheduledExecutorService executor) implements FixedRateScheduler {

        private ExecutorFixedRateScheduler {
            Objects.requireNonNull(executor);
        }

        @Override
        public @NotNull ScheduledTask scheduleAtFixedRate(@NotNull Runnable task, long initialDelayMillis, long periodMillis) {
            ScheduledFuture<?> future = this.executor.scheduleAtFixedRate(Objects.requireNonNull(task), initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS);
            return () -> future.cancel(false);
        }
    }
}
