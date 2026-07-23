package de.keksuccino.fancymenu.util.threading;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/** Starts named daemon workers for target backports that must not delay client shutdown. */
public final class FancyMenuThreads {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private FancyMenuThreads() {}

    public static @NotNull Thread startDaemonThread(@NotNull Runnable runnable, @NotNull String roleName) {
        String threadName = "FancyMenu-" + Objects.requireNonNull(roleName, "roleName") + "-" + THREAD_COUNTER.incrementAndGet();
        Thread thread = new Thread(Objects.requireNonNull(runnable, "runnable"), threadName);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((failedThread, throwable) -> LOGGER.error("[FANCYMENU] Uncaught exception in daemon thread '{}'.", failedThread.getName(), throwable));
        thread.start();
        return thread;
    }
}
