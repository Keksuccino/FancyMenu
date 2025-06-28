package de.keksuccino.fancymenu.util;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TaskExecutor {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(20);

    public static void scheduleAtFixedRate(@NotNull Task task, long initialDelay, long period, @NotNull TimeUnit unit, boolean executeInMainThread) {
        ScheduledFuture<?>[] future = new ScheduledFuture[1];
        future[0] = EXECUTOR.scheduleAtFixedRate(() -> {
            final Runnable r = () -> {
                try {
                    ScheduledFuture<?> f = future[0];
                    if (f != null) {
                        task.run(f);
                    }
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Error while trying to execute scheduled (fixed rate) task!", ex);
                }
            };
            if (executeInMainThread) {
                MainThreadTaskExecutor.executeInMainThread(r, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                r.run();
            }
        }, initialDelay, period, unit);
    }

    public static void schedule(@NotNull Task task, long delay, @NotNull TimeUnit unit, boolean executeInMainThread) {
        ScheduledFuture<?>[] future = new ScheduledFuture[1];
        future[0] = EXECUTOR.schedule(() -> {
            final Runnable r = () -> {
                try {
                    ScheduledFuture<?> f = future[0];
                    if (f != null) {
                        task.run(f);
                    }
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Error while trying to execute scheduled task!", ex);
                }
            };
            if (executeInMainThread) {
                MainThreadTaskExecutor.executeInMainThread(r, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                r.run();
            }
        }, delay, unit);
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

}
