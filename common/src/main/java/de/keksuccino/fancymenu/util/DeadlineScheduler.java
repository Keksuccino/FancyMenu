package de.keksuccino.fancymenu.util;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

interface DeadlineScheduler {

    @NotNull ScheduledTask schedule(@NotNull Runnable task, @NotNull Duration delay);

    void shutdownNow();

    interface ScheduledTask {

        void cancel(boolean mayInterruptIfRunning);
    }
}

final class ExecutorDeadlineScheduler implements DeadlineScheduler {

    private final ScheduledExecutorService executor;

    ExecutorDeadlineScheduler(@NotNull ScheduledExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public @NotNull ScheduledTask schedule(@NotNull Runnable task, @NotNull Duration delay) {
        Duration checkedDelay = Objects.requireNonNull(delay, "delay");
        if (checkedDelay.isNegative() || checkedDelay.isZero()) throw new IllegalArgumentException("delay must be positive");
        ScheduledFuture<?> future = this.executor.schedule(Objects.requireNonNull(task, "task"), checkedDelay.toNanos(), TimeUnit.NANOSECONDS);
        return future::cancel;
    }

    @Override
    public void shutdownNow() {
        this.executor.shutdownNow();
    }
}
