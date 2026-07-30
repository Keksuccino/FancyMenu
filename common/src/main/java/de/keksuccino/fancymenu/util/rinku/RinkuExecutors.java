package de.keksuccino.fancymenu.util.rinku;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Owns every daemon worker used by the optional Rinku integration so client shutdown can stop them together. */
public final class RinkuExecutors {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Registry REGISTRY = new Registry();

    private RinkuExecutors() {}

    public static @NotNull ScheduledExecutorService newSingleThreadScheduledExecutor(@NotNull String threadName) {
        return REGISTRY.newSingleThreadScheduledExecutor(threadName);
    }

    public static @NotNull Thread startDaemonWorker(@NotNull Runnable runnable, @NotNull String threadName) {
        return REGISTRY.startDaemonWorker(runnable, threadName);
    }

    public static void shutdownAll() {
        REGISTRY.shutdownAll();
    }

    static @NotNull ScheduledThreadPoolExecutor createExecutor(@NotNull String threadName) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, createDaemonThreadFactory(threadName));
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        return executor;
    }

    private static @NotNull ThreadFactory createDaemonThreadFactory(@NotNull String threadName) {
        String resolvedThreadName = Objects.requireNonNull(threadName, "threadName");
        AtomicInteger threadCounter = new AtomicInteger();
        return runnable -> createDaemonThread(runnable, resolvedThreadName + "-" + threadCounter.incrementAndGet());
    }

    private static @NotNull Thread createDaemonThread(@NotNull Runnable runnable, @NotNull String threadName) {
        Thread thread = new Thread(Objects.requireNonNull(runnable, "runnable"), Objects.requireNonNull(threadName, "threadName"));
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((failedThread, throwable) -> LOGGER.error("[FANCYMENU] Uncaught exception in Rinku worker thread '{}'.", failedThread.getName(), throwable));
        return thread;
    }

    /** Keeps executor registration, standalone-worker startup, and shutdown atomic with respect to each other. */
    static final class Registry {

        private final Object lifecycleLock = new Object();
        private final List<ExecutorService> executors = new ArrayList<>();
        private final List<Thread> workers = new ArrayList<>();
        private boolean shutdown;

        @NotNull ScheduledExecutorService newSingleThreadScheduledExecutor(@NotNull String threadName) {
            ScheduledThreadPoolExecutor executor = createExecutor(threadName);
            synchronized (lifecycleLock) {
                executors.add(executor);
                if (!shutdown) return executor;
            }
            shutdownExecutor(executor);
            return executor;
        }

        @NotNull Thread startDaemonWorker(@NotNull Runnable runnable, @NotNull String threadName) {
            Thread worker = createDaemonThread(runnable, threadName);
            synchronized (lifecycleLock) {
                if (shutdown) throw new RejectedExecutionException("Rinku workers are already shut down");
                workers.add(worker);
                worker.start();
            }
            return worker;
        }

        void shutdownAll() {
            List<ExecutorService> executorSnapshot;
            List<Thread> workerSnapshot;
            synchronized (lifecycleLock) {
                if (shutdown) return;
                shutdown = true;
                executorSnapshot = new ArrayList<>(executors);
                workerSnapshot = new ArrayList<>(workers);
                executors.clear();
                workers.clear();
            }
            for (ExecutorService executor : executorSnapshot) shutdownExecutor(executor);
            workerSnapshot.forEach(Thread::interrupt);
        }

        private void shutdownExecutor(@NotNull ExecutorService executor) {
            try {
                executor.shutdownNow();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to shut down a Rinku executor.", ex);
            }
        }
    }
}
