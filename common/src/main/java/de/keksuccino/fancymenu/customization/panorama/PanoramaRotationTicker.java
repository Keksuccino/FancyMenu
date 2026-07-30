package de.keksuccino.fancymenu.customization.panorama;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Controls one panorama's rotation cadence without owning a thread. All panorama controllers share one scheduler,
 * while each controller owns only its cancellable task and lifecycle state.
 */
final class PanoramaRotationTicker implements AutoCloseable {

    static final long INACTIVITY_TIMEOUT_MILLIS = 5_000L;
    static final long MIN_TICK_DELAY_MILLIS = 2L;
    static final long BASE_TICK_DELAY_MILLIS = 20L;

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long INACTIVITY_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(INACTIVITY_TIMEOUT_MILLIS);

    private final Object lock = new Object();
    private final LongSupplier nanoTimeSupplier;
    private final Scheduler scheduler;
    private final Runnable tickAction;

    private float speed = 1.0F;
    private boolean active;
    private boolean closed;
    private boolean tickDeadlineSet;
    private long lastRenderNanos;
    private long nextTickNanos;
    private long scheduleGeneration;
    @Nullable private ScheduledTask scheduledTask;

    PanoramaRotationTicker(@NotNull LongSupplier nanoTimeSupplier, @NotNull Scheduler scheduler, @NotNull Runnable tickAction) {
        this.nanoTimeSupplier = Objects.requireNonNull(nanoTimeSupplier);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.tickAction = Objects.requireNonNull(tickAction);
    }

    static @NotNull Scheduler sharedScheduler() {
        return SharedSchedulerHolder.INSTANCE;
    }

    /** Records visible use and lazily starts the ticker. Repeated render calls never create duplicate tasks. */
    void onRender() {
        synchronized (this.lock) {
            if (this.closed) return;
            long now = this.nanoTimeSupplier.getAsLong();
            this.lastRenderNanos = now;
            if (this.active) return;
            this.active = true;
            this.tickDeadlineSet = this.speed > 0.0F;
            if (this.tickDeadlineSet) this.nextTickNanos = now; // Preserve the renderer's immediate first rotation step.
            this.scheduleNextLocked(now);
        }
    }

    /**
     * Applies a new cadence to an already-active ticker. A paused ticker resumes immediately, while other positive
     * changes start a fresh interval so rapid updates cannot add rotation steps of their own.
     */
    float setSpeed(float requestedSpeed) {
        float normalizedSpeed = normalizeSpeed(requestedSpeed);
        synchronized (this.lock) {
            float previousSpeed = this.speed;
            this.speed = normalizedSpeed;
            if (!this.active || Float.floatToIntBits(previousSpeed) == Float.floatToIntBits(normalizedSpeed)) return normalizedSpeed;
            long now = this.nanoTimeSupplier.getAsLong();
            this.cancelScheduledTaskLocked();
            this.tickDeadlineSet = normalizedSpeed > 0.0F;
            if (this.tickDeadlineSet) this.nextTickNanos = previousSpeed > 0.0F ? now + tickDelayNanos(normalizedSpeed) : now;
            this.scheduleNextLocked(now);
            return normalizedSpeed;
        }
    }

    static float normalizeSpeed(float speed) {
        // The property contract is non-negative. NaN has no meaningful cadence and is therefore stationary too.
        return Float.isNaN(speed) || speed <= 0.0F ? 0.0F : speed;
    }

    static long tickDelayMillis(float speed) {
        float normalizedSpeed = normalizeSpeed(speed);
        if (normalizedSpeed == 0.0F) throw new IllegalArgumentException("Panorama tick speed must be positive");
        float requestedDelay = (float)BASE_TICK_DELAY_MILLIS / normalizedSpeed;
        if (requestedDelay >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(MIN_TICK_DELAY_MILLIS, (long)requestedDelay);
    }

    boolean isActive() {
        synchronized (this.lock) {
            return this.active;
        }
    }

    boolean hasScheduledTask() {
        synchronized (this.lock) {
            return this.scheduledTask != null;
        }
    }

    @Override
    public void close() {
        synchronized (this.lock) {
            if (this.closed) return;
            this.closed = true;
            this.active = false;
            this.tickDeadlineSet = false;
            this.cancelScheduledTaskLocked();
        }
    }

    private void runScheduledTask(long generation) {
        synchronized (this.lock) {
            if (this.closed || !this.active || generation != this.scheduleGeneration) return;
            this.scheduledTask = null;
            long now = this.nanoTimeSupplier.getAsLong();
            long inactivityRemaining = this.inactivityRemainingNanos(now);
            if (inactivityRemaining == 0L) {
                this.active = false;
                this.tickDeadlineSet = false;
                this.scheduleGeneration++;
                return;
            }
            if (this.speed == 0.0F) {
                this.tickDeadlineSet = false;
                this.scheduleLocked(inactivityRemaining);
                return;
            }
            if (this.tickDeadlineSet) {
                long tickRemaining = remainingNanos(now, this.nextTickNanos);
                if (tickRemaining > 0L) {
                    this.scheduleLocked(Math.min(tickRemaining, inactivityRemaining));
                    return;
                }
            }
            try {
                this.tickAction.run();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Error while ticking panorama!", ex);
            }
            this.tickDeadlineSet = true;
            this.nextTickNanos = now + tickDelayNanos(this.speed);
            this.scheduleLocked(Math.min(tickDelayNanos(this.speed), inactivityRemaining));
        }
    }

    private void scheduleNextLocked(long now) {
        long inactivityRemaining = this.inactivityRemainingNanos(now);
        if (inactivityRemaining == 0L) {
            this.active = false;
            this.tickDeadlineSet = false;
            return;
        }
        long delayNanos = inactivityRemaining;
        if (this.speed > 0.0F && this.tickDeadlineSet) delayNanos = Math.min(delayNanos, remainingNanos(now, this.nextTickNanos));
        this.scheduleLocked(delayNanos);
    }

    private void scheduleLocked(long delayNanos) {
        long generation = ++this.scheduleGeneration;
        try {
            this.scheduledTask = Objects.requireNonNull(this.scheduler.schedule(() -> this.runScheduledTask(generation), Math.max(0L, delayNanos)));
        } catch (RuntimeException ex) {
            // The shared executor can reject only during global client shutdown. Disable this controller to avoid render-loop retries.
            this.scheduledTask = null;
            this.active = false;
            this.tickDeadlineSet = false;
            this.closed = true;
            LOGGER.error("[FANCYMENU] Unable to schedule panorama ticker!", ex);
        }
    }

    private void cancelScheduledTaskLocked() {
        this.scheduleGeneration++;
        ScheduledTask task = this.scheduledTask;
        this.scheduledTask = null;
        if (task != null) task.cancel();
    }

    private long inactivityRemainingNanos(long now) {
        long elapsed = now - this.lastRenderNanos;
        if (elapsed < 0L) elapsed = 0L;
        return elapsed >= INACTIVITY_TIMEOUT_NANOS ? 0L : INACTIVITY_TIMEOUT_NANOS - elapsed;
    }

    private static long remainingNanos(long now, long deadline) {
        long remaining = deadline - now;
        return Math.max(0L, remaining);
    }

    private static long tickDelayNanos(float speed) {
        return TimeUnit.MILLISECONDS.toNanos(tickDelayMillis(speed));
    }

    interface Scheduler {

        @NotNull ScheduledTask schedule(@NotNull Runnable task, long delayNanos);

    }

    interface ScheduledTask {

        void cancel();

    }

    private static final class SharedSchedulerHolder {

        private static final Scheduler INSTANCE = new ExecutorScheduler(createExecutor());

        private static @NotNull ScheduledExecutorService createExecutor() {
            return Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "FancyMenu-Panorama-Ticker");
                thread.setDaemon(true);
                return thread;
            });
        }

    }

    private record ExecutorScheduler(@NotNull ScheduledExecutorService executor) implements Scheduler {

        private ExecutorScheduler {
            Objects.requireNonNull(executor);
        }

        @Override
        public @NotNull ScheduledTask schedule(@NotNull Runnable task, long delayNanos) {
            ScheduledFuture<?> future = this.executor.schedule(Objects.requireNonNull(task), delayNanos, TimeUnit.NANOSECONDS);
            return () -> future.cancel(false);
        }

    }

}
