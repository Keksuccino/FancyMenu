package de.keksuccino.fancymenu.customization.profile;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Walks profiles in the background to measure them for the profile manager.<br>
 * A walk keeps the directories it is in open, and on Windows, open directories can't get moved or deleted. So everything that moves or deletes
 * profile files calls {@link #stopAll()} first, and the profile manager measures again afterwards.
 */
final class ProfileStatsWalks {

    private static final Logger LOGGER = LogManager.getLogger();
    /** Walks check for cancellation at every entry, so they end almost at once. Only a hanging file system makes stopping wait this long. */
    private static final long STOP_TIMEOUT_MS = 500L;
    /** The threads of all running walks and their cancellation. */
    private static final Map<Thread, ProfileTaskProgress> RUNNING = new ConcurrentHashMap<>();

    private ProfileStatsWalks() {
    }

    /**
     * @param callback gets called on the main thread with the result, or with NULL if the walk got stopped.
     */
    static void start(@NotNull Path directory, @NotNull Predicate<Path> filter, @NotNull Consumer<ProfileTasks.ProfileStats> callback) {
        ProfileTaskProgress cancellation = new ProfileTaskProgress();
        Thread thread = new Thread(() -> {
            ProfileTasks.ProfileStats stats = null;
            try {
                long size = 0L;
                try {
                    size = ProfileFiles.measureSize(directory, filter, cancellation);
                } catch (CancellationException ex) {
                    throw ex;
                } catch (Exception ex) {
                    LOGGER.warn("[FANCYMENU] Failed to measure profile: " + directory, ex);
                }
                stats = new ProfileTasks.ProfileStats(size, ProfileTasks.countLayouts(directory, cancellation));
            } catch (CancellationException ignored) {
            } finally {
                RUNNING.remove(Thread.currentThread());
            }
            ProfileTasks.ProfileStats result = stats;
            MainThreadTaskExecutor.executeInMainThread(() -> callback.accept(result), MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }, "FancyMenu-ProfileStats");
        thread.setDaemon(true);
        RUNNING.put(thread, cancellation);
        thread.start();
    }

    /**
     * Cancels all running walks and waits until they ended, so they don't keep any profile directory open anymore.
     */
    static void stopAll() {
        if (RUNNING.isEmpty()) return;
        List<Map.Entry<Thread, ProfileTaskProgress>> walks = new ArrayList<>(RUNNING.entrySet());
        walks.forEach(walk -> walk.getValue().requestCancel());
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(STOP_TIMEOUT_MS);
        for (Map.Entry<Thread, ProfileTaskProgress> walk : walks) {
            Thread thread = walk.getKey();
            long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            if (remainingMs > 0L) {
                try {
                    thread.join(remainingMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (thread.isAlive()) LOGGER.warn("[FANCYMENU] A profile walk didn't stop in time, so it might still have profile files open.");
        }
    }

}
