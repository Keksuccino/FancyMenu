package de.keksuccino.fancymenu.customization.profile;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ProfileStatsWalksTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        ProfileStatsWalks.stopAll();
        MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    @Test
    void stoppingCancelsRunningWalksAndWaitsUntilTheyEnded() throws Exception {
        Path profile = Files.createDirectories(this.tempDir.resolve("profile").resolve("customization"));
        for (int i = 0; i < 300; i++) Files.writeString(profile.resolve("layout_" + i + ".txt"), "x");
        CountDownLatch walking = new CountDownLatch(1);
        List<Object> results = new ArrayList<>();
        // A slow walk, that would take seconds without getting stopped
        ProfileStatsWalks.start(profile.getParent(), path -> {
            walking.countDown();
            sleep(20);
            return true;
        }, results::add);
        assertTrue(walking.await(5, TimeUnit.SECONDS));

        long start = System.nanoTime();
        ProfileStatsWalks.stopAll();
        long stopMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(stopMs < 400, "Stopping took " + stopMs + " ms");
        // The walk delivers its result right before it ends, so it is there once stopping returned
        this.runMainThreadTasks();
        assertEquals(1, results.size());
        assertNull(results.get(0));
    }

    @Test
    void finishedWalksDeliverTheStats() throws Exception {
        Path profile = Files.createDirectories(this.tempDir.resolve("profile").resolve("customization"));
        Files.writeString(profile.resolve("first.txt"), "12345");
        Files.writeString(profile.resolve("second.txt"), "123");
        List<ProfileTasks.ProfileStats> results = new ArrayList<>();

        ProfileStatsWalks.start(profile.getParent(), path -> true, results::add);
        long deadline = System.currentTimeMillis() + 5000L;
        while (results.isEmpty() && (System.currentTimeMillis() < deadline)) {
            this.runMainThreadTasks();
            sleep(5);
        }

        assertEquals(1, results.size());
        assertEquals(8L, results.get(0).sizeBytes());
        assertEquals(2, results.get(0).layoutCount());
    }

    @Test
    void stoppingWithoutWalksReturnsAtOnce() {
        long start = System.nanoTime();
        ProfileStatsWalks.stopAll();
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 50);
    }

    private void runMainThreadTasks() {
        MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK).forEach(Runnable::run);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

}
