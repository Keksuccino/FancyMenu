package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebTextPlaceholderCacheTest {

    private static final String PLACEHOLDER = "{\"placeholder\":\"webtext\",\"values\":{\"link\":\"https://example.invalid/text\"}}";
    private static final String LINK = "https://example.invalid/text";

    @Test
    void loaderThreadPublishesAnImmutableVisibleSnapshot() throws Exception {
        ManualTaskLauncher launcher = new ManualTaskLauncher();
        List<String> mutableLines = new ArrayList<>(List.of("first", "second"));
        WebTextPlaceholderCache cache = new WebTextPlaceholderCache(launcher, link -> WebTextPlaceholderCache.LoadResult.valid(mutableLines));

        assertEquals(WebTextPlaceholderCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, LINK).status());
        assertTrue(cache.isLoading(PLACEHOLDER));
        Thread loader = new Thread(launcher.removeNext(), "WebTextPlaceholderCacheTest-loader");
        loader.start();
        loader.join(5000L);
        assertFalse(loader.isAlive());

        mutableLines.set(0, "mutated");
        mutableLines.add("third");
        WebTextPlaceholderCache.Lookup lookup = cache.getOrLoad(PLACEHOLDER, LINK);
        assertEquals(WebTextPlaceholderCache.Status.LOADED, lookup.status());
        assertEquals(List.of("first", "second"), lookup.lines());
        assertThrows(UnsupportedOperationException.class, () -> lookup.lines().add("not allowed"));
        assertFalse(cache.isLoading(PLACEHOLDER));
        assertEquals(0, launcher.size());
    }

    @Test
    void concurrentMissesAdmitExactlyOneLoad() throws Exception {
        int callers = 32;
        ManualTaskLauncher launcher = new ManualTaskLauncher();
        AtomicInteger loads = new AtomicInteger();
        WebTextPlaceholderCache cache = new WebTextPlaceholderCache(launcher, link -> {
            loads.incrementAndGet();
            return WebTextPlaceholderCache.LoadResult.valid(List.of("value"));
        });
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(callers);
        List<Future<WebTextPlaceholderCache.Lookup>> futures = new ArrayList<>();
        try {
            for (int caller = 0; caller < callers; caller++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    await(start);
                    return cache.getOrLoad(PLACEHOLDER, LINK);
                }));
            }
            assertTrue(ready.await(5L, TimeUnit.SECONDS));
            start.countDown();
            for (Future<WebTextPlaceholderCache.Lookup> future : futures) assertEquals(WebTextPlaceholderCache.Status.LOADING, future.get(5L, TimeUnit.SECONDS).status());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }

        assertEquals(1, launcher.size());
        assertEquals(0, loads.get());
        launcher.runNext();
        assertEquals(1, loads.get());
        assertEquals(List.of("value"), cache.getOrLoad(PLACEHOLDER, LINK).lines());
    }

    @Test
    void staleCompletionAfterReloadCannotReplaceCurrentData() throws Exception {
        ManualTaskLauncher launcher = new ManualTaskLauncher();
        CountDownLatch oldLoadEntered = new CountDownLatch(1);
        CountDownLatch oldLoadInterrupted = new CountDownLatch(1);
        CountDownLatch releaseOldLoad = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();
        WebTextPlaceholderCache cache = new WebTextPlaceholderCache(launcher, link -> {
            if (loads.incrementAndGet() == 1) {
                oldLoadEntered.countDown();
                awaitIgnoringInterrupt(releaseOldLoad, oldLoadInterrupted);
                return WebTextPlaceholderCache.LoadResult.valid(List.of("stale"));
            }
            return WebTextPlaceholderCache.LoadResult.valid(List.of("current"));
        });

        assertEquals(WebTextPlaceholderCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, LINK).status());
        Thread oldLoader = new Thread(launcher.removeNext(), "WebTextPlaceholderCacheTest-stale-loader");
        oldLoader.start();
        try {
            assertTrue(oldLoadEntered.await(5L, TimeUnit.SECONDS));
            long oldGeneration = cache.generationNumber();
            cache.reload();
            assertEquals(oldGeneration + 1L, cache.generationNumber());
            assertTrue(oldLoadInterrupted.await(5L, TimeUnit.SECONDS));

            assertEquals(WebTextPlaceholderCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, LINK).status());
            launcher.runNext();
            assertEquals(List.of("current"), cache.getOrLoad(PLACEHOLDER, LINK).lines());

            releaseOldLoad.countDown();
            oldLoader.join(5000L);
            assertFalse(oldLoader.isAlive());
            assertEquals(List.of("current"), cache.getOrLoad(PLACEHOLDER, LINK).lines());
            assertEquals(2, loads.get());
        } finally {
            releaseOldLoad.countDown();
            oldLoader.interrupt();
            oldLoader.join(5000L);
        }
    }

    @Test
    void reloadCancelsAClaimBeforeItsTaskStartsWithoutTouchingTheNewClaim() {
        ManualTaskLauncher launcher = new ManualTaskLauncher();
        AtomicInteger loads = new AtomicInteger();
        WebTextPlaceholderCache cache = new WebTextPlaceholderCache(launcher, link -> {
            loads.incrementAndGet();
            return WebTextPlaceholderCache.LoadResult.valid(List.of("current"));
        });

        cache.getOrLoad(PLACEHOLDER, LINK);
        Runnable cancelledOldTask = launcher.removeNext();
        cache.reload();
        assertFalse(cache.isLoading(PLACEHOLDER));

        cache.getOrLoad(PLACEHOLDER, LINK);
        assertTrue(cache.isLoading(PLACEHOLDER));
        Runnable currentTask = launcher.removeNext();
        cancelledOldTask.run();
        assertTrue(cache.isLoading(PLACEHOLDER));
        assertEquals(0, loads.get());

        currentTask.run();
        assertEquals(1, loads.get());
        assertFalse(cache.isLoading(PLACEHOLDER));
        assertEquals(List.of("current"), cache.getOrLoad(PLACEHOLDER, LINK).lines());
    }

    @Test
    void loadFailureReleasesTheClaimForRetry() {
        ManualTaskLauncher launcher = new ManualTaskLauncher();
        AtomicInteger attempts = new AtomicInteger();
        WebTextPlaceholderCache cache = new WebTextPlaceholderCache(launcher, link -> {
            if (attempts.incrementAndGet() == 1) throw new IOException("expected test failure");
            return WebTextPlaceholderCache.LoadResult.valid(List.of("recovered"));
        });

        cache.getOrLoad(PLACEHOLDER, LINK);
        launcher.runNext();
        assertFalse(cache.isLoading(PLACEHOLDER));
        assertEquals(WebTextPlaceholderCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, LINK).status());
        assertEquals(1, launcher.size());

        launcher.runNext();
        assertEquals(2, attempts.get());
        assertEquals(List.of("recovered"), cache.getOrLoad(PLACEHOLDER, LINK).lines());
    }

    @Test
    void loaderErrorPropagatesAfterReleasingTheClaim() {
        ManualTaskLauncher launcher = new ManualTaskLauncher();
        WebTextPlaceholderCache cache = new WebTextPlaceholderCache(launcher, link -> {
            throw new AssertionError("expected test error");
        });

        cache.getOrLoad(PLACEHOLDER, LINK);
        assertThrows(AssertionError.class, launcher::runNext);
        assertFalse(cache.isLoading(PLACEHOLDER));
        assertEquals(WebTextPlaceholderCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, LINK).status());
        assertEquals(1, launcher.size());
    }

    @Test
    void launcherFailureReleasesTheClaimForRetry() {
        Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        AtomicBoolean rejectFirst = new AtomicBoolean(true);
        AtomicInteger loads = new AtomicInteger();
        WebTextPlaceholderCache cache = new WebTextPlaceholderCache(task -> {
            if (rejectFirst.compareAndSet(true, false)) throw new IllegalStateException("expected launcher rejection");
            tasks.add(task);
        }, link -> {
            loads.incrementAndGet();
            return WebTextPlaceholderCache.LoadResult.valid(List.of("recovered"));
        });

        assertEquals(WebTextPlaceholderCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, LINK).status());
        assertFalse(cache.isLoading(PLACEHOLDER));
        assertEquals(WebTextPlaceholderCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, LINK).status());
        assertTrue(cache.isLoading(PLACEHOLDER));
        Runnable retry = tasks.remove();
        retry.run();

        assertEquals(1, loads.get());
        assertFalse(cache.isLoading(PLACEHOLDER));
        assertEquals(List.of("recovered"), cache.getOrLoad(PLACEHOLDER, LINK).lines());
    }

    @Test
    void invalidLinkRemainsTerminalUntilReload() {
        ManualTaskLauncher launcher = new ManualTaskLauncher();
        AtomicInteger validations = new AtomicInteger();
        WebTextPlaceholderCache cache = new WebTextPlaceholderCache(launcher, link -> {
            validations.incrementAndGet();
            return WebTextPlaceholderCache.LoadResult.invalid();
        });

        cache.getOrLoad(PLACEHOLDER, LINK);
        launcher.runNext();
        assertTrue(cache.isInvalidLink(LINK));
        assertEquals(WebTextPlaceholderCache.Status.INVALID, cache.getOrLoad(PLACEHOLDER, LINK).status());
        assertEquals(0, launcher.size());
        assertEquals(1, validations.get());

        cache.reload();
        assertFalse(cache.isInvalidLink(LINK));
        assertEquals(WebTextPlaceholderCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, LINK).status());
        assertEquals(1, launcher.size());
    }

    @Test
    void emptyValidContentRemainsTerminalUntilReload() {
        ManualTaskLauncher launcher = new ManualTaskLauncher();
        AtomicInteger loads = new AtomicInteger();
        WebTextPlaceholderCache cache = new WebTextPlaceholderCache(launcher, link -> {
            loads.incrementAndGet();
            return WebTextPlaceholderCache.LoadResult.valid(List.of());
        });

        cache.getOrLoad(PLACEHOLDER, LINK);
        launcher.runNext();
        WebTextPlaceholderCache.Lookup empty = cache.getOrLoad(PLACEHOLDER, LINK);
        assertEquals(WebTextPlaceholderCache.Status.LOADED, empty.status());
        assertTrue(empty.lines().isEmpty());
        assertEquals(0, launcher.size());
        assertEquals(1, loads.get());

        cache.reload();
        assertEquals(WebTextPlaceholderCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, LINK).status());
        assertEquals(1, launcher.size());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for coordinated Web Text placeholder work");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for coordinated Web Text placeholder work", ex);
        }
    }

    private static void awaitIgnoringInterrupt(CountDownLatch release, CountDownLatch interrupted) {
        boolean restoreInterrupt = false;
        while (true) {
            try {
                if (!release.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting to release the stale Web Text placeholder load");
                break;
            } catch (InterruptedException ex) {
                restoreInterrupt = true;
                interrupted.countDown();
            }
        }
        if (restoreInterrupt) Thread.currentThread().interrupt();
    }

    private static final class ManualTaskLauncher implements WebTextPlaceholderCache.TaskLauncher {

        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

        @Override
        public void launch(Runnable task) {
            this.tasks.add(task);
        }

        private int size() {
            return this.tasks.size();
        }

        private Runnable removeNext() {
            return this.tasks.remove();
        }

        private void runNext() {
            this.removeNext().run();
        }

    }

}
