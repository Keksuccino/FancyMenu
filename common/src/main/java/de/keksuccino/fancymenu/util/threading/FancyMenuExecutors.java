package de.keksuccino.fancymenu.util.threading;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class FancyMenuExecutors {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Object SHUTDOWN_LOCK = new Object();
    private static final List<ExecutorService> EXECUTORS = new ArrayList<>();

    private static boolean shutdown;

    private FancyMenuExecutors() {
    }

    public static @NotNull ScheduledExecutorService newSingleThreadScheduledExecutor(@NotNull String name) {
        return newScheduledThreadPool(1, name);
    }

    public static @NotNull ScheduledExecutorService newScheduledThreadPool(int size, @NotNull String name) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(size, createDaemonThreadFactory(name));
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        return register(executor);
    }

    public static void shutdownAll() {
        List<ExecutorService> executors;
        synchronized (SHUTDOWN_LOCK) {
            if (shutdown) return;
            shutdown = true;
            executors = new ArrayList<>(EXECUTORS);
        }
        for (ExecutorService executor : executors) {
            shutdownNow(executor);
        }
    }

    private static @NotNull ThreadFactory createDaemonThreadFactory(@NotNull String name) {
        String threadName = Objects.requireNonNull(name, "name");
        AtomicInteger threadCounter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, threadName + "-" + threadCounter.incrementAndGet());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((failedThread, throwable) -> LOGGER.error("[FANCYMENU] Uncaught exception in executor thread '{}'.", failedThread.getName(), throwable));
            return thread;
        };
    }

    private static <T extends ExecutorService> @NotNull T register(@NotNull T executor) {
        // Registration and the shutdown state share one lock so no executor can escape a concurrent shutdown snapshot.
        synchronized (SHUTDOWN_LOCK) {
            EXECUTORS.add(executor);
            if (!shutdown) {
                return executor;
            }
        }
        // Classes can initialize during client teardown; executors registered after the snapshot must stop immediately.
        shutdownNow(executor);
        return executor;
    }

    private static void shutdownNow(@NotNull ExecutorService executor) {
        try {
            executor.shutdownNow();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to shut down managed executor.", ex);
        }
    }
}
