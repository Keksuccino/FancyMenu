package de.keksuccino.fancymenu.util.rinku;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/** Owns the daemon schedulers used by the optional Rinku integration so client shutdown can stop them together. */
public final class RinkuExecutors {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<ScheduledExecutorService> EXECUTORS = new CopyOnWriteArrayList<>();

    private RinkuExecutors() {}

    @NotNull
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(@NotNull String threadName) {
        ScheduledThreadPoolExecutor executor = createExecutor(threadName);
        EXECUTORS.add(executor);
        return executor;
    }

    @NotNull
    static ScheduledThreadPoolExecutor createExecutor(@NotNull String threadName) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, runnable -> createWorkerThread(runnable, threadName));
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        return executor;
    }

    private static Thread createWorkerThread(@NotNull Runnable runnable, @NotNull String threadName) {
        Thread thread = new Thread(runnable, threadName);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((failedThread, throwable) -> LOGGER.error("[FANCYMENU] Uncaught exception in Rinku worker thread '{}'.", failedThread.getName(), throwable));
        return thread;
    }

    public static void shutdownAll() {
        EXECUTORS.forEach(ScheduledExecutorService::shutdownNow);
        EXECUTORS.clear();
    }
}
