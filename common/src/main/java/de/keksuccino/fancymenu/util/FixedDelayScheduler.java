package de.keksuccino.fancymenu.util;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

interface FixedDelayScheduler {

    @NotNull ScheduledTask scheduleWithFixedDelay(@NotNull Runnable task, @NotNull Duration initialDelay, @NotNull Duration delay);

    void shutdownNow();

    interface ScheduledTask {

        void cancel(boolean mayInterruptIfRunning);
    }
}

final class ExecutorFixedDelayScheduler implements FixedDelayScheduler {

    private final ScheduledExecutorService executor;

    ExecutorFixedDelayScheduler(@NotNull ScheduledExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public @NotNull ScheduledTask scheduleWithFixedDelay(@NotNull Runnable task, @NotNull Duration initialDelay, @NotNull Duration delay) {
        ScheduledFuture<?> future = this.executor.scheduleWithFixedDelay(Objects.requireNonNull(task, "task"), requireNonNegative(initialDelay, "initialDelay").toNanos(), requirePositive(delay, "delay").toNanos(), TimeUnit.NANOSECONDS);
        return future::cancel;
    }

    @Override
    public void shutdownNow() {
        this.executor.shutdownNow();
    }

    private static @NotNull Duration requireNonNegative(@NotNull Duration duration, @NotNull String name) {
        Duration checked = Objects.requireNonNull(duration, name);
        if (checked.isNegative()) throw new IllegalArgumentException(name + " must not be negative");
        return checked;
    }

    private static @NotNull Duration requirePositive(@NotNull Duration duration, @NotNull String name) {
        Duration checked = requireNonNegative(duration, name);
        if (checked.isZero()) throw new IllegalArgumentException(name + " must be positive");
        return checked;
    }
}
