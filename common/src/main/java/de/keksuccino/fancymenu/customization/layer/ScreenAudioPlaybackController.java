package de.keksuccino.fancymenu.customization.layer;

import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.util.TaskExecutor;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Owns the one deferred menu-transition audio cue associated with a cached screen layer. Polls are intentionally
 * one-shot and self-rescheduling so a stalled client thread can retain at most one queued callback per layer.
 */
final class ScreenAudioPlaybackController implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long DEFAULT_POLL_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(100L);
    private static final long DEFAULT_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(10L);

    private final Object lifecycleLock = new Object();
    private final Object playbackBarrier = new Object();
    private final Scheduler scheduler;
    private final LongSupplier nanoTimeSource;
    private final long pollIntervalNanos;
    private final long timeoutNanos;

    private boolean closed;
    @Nullable
    private Attempt currentAttempt;

    ScreenAudioPlaybackController() {
        this(new MainThreadScheduler(), System::nanoTime, DEFAULT_POLL_INTERVAL_NANOS, DEFAULT_TIMEOUT_NANOS);
    }

    ScreenAudioPlaybackController(@NotNull Scheduler scheduler, @NotNull LongSupplier nanoTimeSource, long pollIntervalNanos, long timeoutNanos) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.nanoTimeSource = Objects.requireNonNull(nanoTimeSource);
        this.pollIntervalNanos = requirePositive(pollIntervalNanos, "pollIntervalNanos");
        this.timeoutNanos = requirePositive(timeoutNanos, "timeoutNanos");
    }

    /** Replaces any older cue from this layer and immediately checks whether the new audio is already playable. */
    void playWhenReady(@NotNull Cue cue, @NotNull ResourceSupplier<IAudio> supplier, @Nullable Layout owner) {
        Attempt previous;
        Attempt attempt;
        synchronized (this.playbackBarrier) {
            synchronized (this.lifecycleLock) {
                if (this.closed) return;
                previous = this.currentAttempt;
                attempt = new Attempt(Objects.requireNonNull(cue), Objects.requireNonNull(supplier), owner, this.nanoTimeSource.getAsLong());
                this.currentAttempt = attempt;
            }
        }
        cancelAttempt(previous);
        this.poll(attempt);
    }

    /**
     * Keeps a slow valid load through an in-place resize, but detaches it when stacking selected a different supplier
     * or exact layout instance. Identity is required because reloaded layouts may serialize to identical values.
     */
    void retainConfigured(@Nullable ResourceSupplier<IAudio> openSupplier, @Nullable Layout openOwner, @Nullable ResourceSupplier<IAudio> closeSupplier, @Nullable Layout closeOwner) {
        Attempt attempt;
        boolean retained;
        synchronized (this.playbackBarrier) {
            synchronized (this.lifecycleLock) {
                attempt = this.currentAttempt;
                if (attempt == null) return;
                ResourceSupplier<IAudio> expectedSupplier = attempt.cue == Cue.OPEN ? openSupplier : closeSupplier;
                Layout expectedOwner = attempt.cue == Cue.OPEN ? openOwner : closeOwner;
                retained = attempt.supplier == expectedSupplier && attempt.owner == expectedOwner && isOwnerValid(attempt.owner);
                if (!retained) this.currentAttempt = null;
            }
        }
        if (!retained) cancelAttempt(attempt);
    }

    void cancelIfOwnedBy(@NotNull Collection<Layout> owners) {
        Objects.requireNonNull(owners);
        Attempt attempt;
        synchronized (this.playbackBarrier) {
            synchronized (this.lifecycleLock) {
                attempt = this.currentAttempt;
                if (attempt == null || attempt.owner == null || !containsIdentity(owners, attempt.owner)) return;
                this.currentAttempt = null;
            }
        }
        cancelAttempt(attempt);
    }

    void cancel() {
        Attempt attempt;
        synchronized (this.playbackBarrier) {
            synchronized (this.lifecycleLock) {
                attempt = this.currentAttempt;
                this.currentAttempt = null;
            }
        }
        cancelAttempt(attempt);
    }

    boolean isPending() {
        synchronized (this.lifecycleLock) {
            return this.currentAttempt != null;
        }
    }

    boolean isClosed() {
        synchronized (this.lifecycleLock) {
            return this.closed;
        }
    }

    @Override
    public void close() {
        Attempt attempt;
        synchronized (this.playbackBarrier) {
            synchronized (this.lifecycleLock) {
                if (this.closed) return;
                this.closed = true;
                attempt = this.currentAttempt;
                this.currentAttempt = null;
            }
        }
        cancelAttempt(attempt);
    }

    private void poll(@NotNull Attempt attempt) {
        if (!this.isCurrent(attempt)) return;
        if (!isOwnerValid(attempt.owner) || this.hasTimedOut(attempt)) {
            this.finish(attempt);
            return;
        }

        IAudio audio;
        try {
            audio = attempt.supplier.get();
        } catch (Exception ex) {
            this.finish(attempt);
            LOGGER.error("[FANCYMENU] Failed to get deferred screen audio resource!", ex);
            return;
        }

        if (!this.isCurrent(attempt)) return;
        if (audio == null) {
            this.finish(attempt);
            return;
        }

        try {
            // A resource is allowed to report ready and failed together, so playable state must win.
            if (audio.isReady()) {
                this.restartIfCurrent(attempt, audio);
                return;
            }

            boolean loadingFailed = audio.isLoadingFailed();
            if (loadingFailed && !audio.isLoadingFailureRetryable()) {
                this.finish(attempt);
                return;
            }
            // Retryable OpenAL failures deliberately self-close once OpenAL returns. Fetching from the supplier on the
            // next poll is what replaces that closed instance, so do not treat its temporary failure as terminal here.
            if (!loadingFailed && (audio.isLoadingCompleted() || audio.isClosed())) {
                this.finish(attempt);
                return;
            }
        } catch (Exception ex) {
            this.finish(attempt);
            LOGGER.error("[FANCYMENU] Failed to inspect deferred screen audio resource!", ex);
            return;
        }

        this.scheduleNextPoll(attempt);
    }

    private void restartIfCurrent(@NotNull Attempt attempt, @NotNull IAudio audio) {
        // Cancellation and replacement take this barrier too. The lifecycle claim is still made under lifecycleLock,
        // but external audio code runs without that state lock so callbacks cannot observe a half-mutated attempt.
        synchronized (this.playbackBarrier) {
            if (!isOwnerValid(attempt.owner) || this.hasTimedOut(attempt) || !this.finish(attempt)) return;
            restart(audio);
        }
    }

    private void scheduleNextPoll(@NotNull Attempt attempt) {
        long remainingNanos = this.remainingNanos(attempt);
        if (remainingNanos <= 0L || !isOwnerValid(attempt.owner)) {
            this.finish(attempt);
            return;
        }

        ScheduledSlot slot = new ScheduledSlot();
        synchronized (this.lifecycleLock) {
            if (this.closed || this.currentAttempt != attempt) return;
            attempt.scheduledSlot = slot;
        }

        ScheduledTask scheduledTask;
        try {
            scheduledTask = Objects.requireNonNull(this.scheduler.schedule(() -> this.runScheduledPoll(attempt, slot), Math.min(this.pollIntervalNanos, remainingNanos)));
        } catch (RuntimeException ex) {
            this.finishScheduledFailure(attempt, slot);
            LOGGER.error("[FANCYMENU] Failed to schedule deferred screen audio polling!", ex);
            return;
        }

        boolean accepted;
        synchronized (this.lifecycleLock) {
            accepted = !this.closed && this.currentAttempt == attempt && attempt.scheduledSlot == slot;
            if (accepted) slot.task = scheduledTask;
        }
        if (!accepted) cancelScheduledTask(scheduledTask);
    }

    private void runScheduledPoll(@NotNull Attempt attempt, @NotNull ScheduledSlot slot) {
        synchronized (this.lifecycleLock) {
            if (this.closed || this.currentAttempt != attempt || attempt.scheduledSlot != slot) return;
            attempt.scheduledSlot = null;
        }
        this.poll(attempt);
    }

    private void finishScheduledFailure(@NotNull Attempt attempt, @NotNull ScheduledSlot slot) {
        synchronized (this.lifecycleLock) {
            if (this.currentAttempt == attempt && attempt.scheduledSlot == slot) {
                attempt.scheduledSlot = null;
                this.currentAttempt = null;
            }
        }
    }

    private boolean finish(@NotNull Attempt attempt) {
        ScheduledTask task;
        synchronized (this.lifecycleLock) {
            if (this.currentAttempt != attempt) return false;
            this.currentAttempt = null;
            task = detachScheduledTask(attempt);
        }
        cancelScheduledTask(task);
        return true;
    }

    private boolean isCurrent(@NotNull Attempt attempt) {
        synchronized (this.lifecycleLock) {
            return !this.closed && this.currentAttempt == attempt;
        }
    }

    private boolean hasTimedOut(@NotNull Attempt attempt) {
        return this.nanoTimeSource.getAsLong() - attempt.startedAtNanos >= this.timeoutNanos;
    }

    private long remainingNanos(@NotNull Attempt attempt) {
        long elapsedNanos = this.nanoTimeSource.getAsLong() - attempt.startedAtNanos;
        if (elapsedNanos < 0L) return this.timeoutNanos;
        return this.timeoutNanos - elapsedNanos;
    }

    private static boolean isOwnerValid(@Nullable Layout owner) {
        return owner == null || !owner.isDestroyed();
    }

    private static boolean containsIdentity(@NotNull Collection<Layout> owners, @NotNull Layout expected) {
        for (Layout owner : owners) {
            if (owner == expected) return true;
        }
        return false;
    }

    private static void restart(@NotNull IAudio audio) {
        try {
            audio.stop();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to stop screen audio before restarting it!", ex);
        }
        try {
            audio.play();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to play screen audio!", ex);
        }
    }

    private void cancelAttempt(@Nullable Attempt attempt) {
        if (attempt == null) return;
        ScheduledTask task;
        synchronized (this.lifecycleLock) {
            task = detachScheduledTask(attempt);
        }
        cancelScheduledTask(task);
    }

    @Nullable
    private static ScheduledTask detachScheduledTask(@NotNull Attempt attempt) {
        ScheduledSlot slot = attempt.scheduledSlot;
        attempt.scheduledSlot = null;
        return slot == null ? null : slot.task;
    }

    private static void cancelScheduledTask(@Nullable ScheduledTask task) {
        if (task == null) return;
        try {
            task.cancel();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to cancel deferred screen audio polling!", ex);
        }
    }

    private static long requirePositive(long value, @NotNull String name) {
        if (value <= 0L) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    enum Cue {
        OPEN,
        CLOSE
    }

    interface Scheduler {

        @NotNull ScheduledTask schedule(@NotNull Runnable task, long delayNanos);
    }

    interface ScheduledTask {

        void cancel();
    }

    private static final class Attempt {

        private final Cue cue;
        private final ResourceSupplier<IAudio> supplier;
        @Nullable
        private final Layout owner;
        private final long startedAtNanos;
        @Nullable
        private ScheduledSlot scheduledSlot;

        private Attempt(@NotNull Cue cue, @NotNull ResourceSupplier<IAudio> supplier, @Nullable Layout owner, long startedAtNanos) {
            this.cue = cue;
            this.supplier = supplier;
            this.owner = owner;
            this.startedAtNanos = startedAtNanos;
        }
    }

    private static final class ScheduledSlot {

        @Nullable
        private ScheduledTask task;
    }

    private static final class MainThreadScheduler implements Scheduler {

        @Override
        public @NotNull ScheduledTask schedule(@NotNull Runnable task, long delayNanos) {
            TaskExecutor.CancellableTask scheduledTask = TaskExecutor.scheduleCancellable(task, delayNanos, TimeUnit.NANOSECONDS, true);
            return scheduledTask::cancel;
        }
    }
}
