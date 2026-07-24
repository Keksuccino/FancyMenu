package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.testing.ConcurrentTestCalls;
import de.keksuccino.fancymenu.testing.ManualTaskQueue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonPlaceholderWebCacheTest {

    private static final String PLACEHOLDER = "{\"placeholder\":\"json\",\"values\":{\"source\":\"https://example.invalid/data.json\",\"json_path\":\"$.value\"}}";
    private static final String SOURCE = "https://example.invalid/data.json";
    private static final String JSON_PATH = "$.value";

    @Test
    void concurrentMissesAdmitExactlyOneLoadAndPublishAnImmutableValue() throws Exception {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicInteger loads = new AtomicInteger();
        List<String> mutableResult = new ArrayList<>(List.of("first", "second"));
        JsonPlaceholderWebCache cache = new JsonPlaceholderWebCache(tasks::add, (source, jsonPath) -> {
            assertEquals(SOURCE, source);
            assertEquals(JSON_PATH, jsonPath);
            loads.incrementAndGet();
            return JsonPlaceholderWebCache.LoadResult.loaded(mutableResult);
        }, () -> 0L, 100L);

        List<JsonPlaceholderWebCache.Lookup> results = ConcurrentTestCalls.invoke(32, () -> cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH));

        assertTrue(results.stream().allMatch(result -> result.status() == JsonPlaceholderWebCache.Status.LOADING));
        assertEquals(1, tasks.size());
        assertEquals(0, loads.get());
        assertTrue(cache.isLoading(PLACEHOLDER));
        tasks.runNext();
        mutableResult.set(0, "mutated");
        mutableResult.add("third");
        JsonPlaceholderWebCache.Lookup cached = cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH);
        assertEquals(JsonPlaceholderWebCache.Status.LOADED, cached.status());
        assertEquals(List.of("first", "second"), cached.values());
        assertThrows(UnsupportedOperationException.class, () -> cached.values().add("not allowed"));
        assertEquals(1, loads.get());
        assertFalse(cache.isLoading(PLACEHOLDER));
    }

    @Test
    void timedOutLoaderThatIgnoresInterruptionCannotOverwriteOrReleaseRetry() throws Exception {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicLong time = new AtomicLong();
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch oldLoadEntered = new CountDownLatch(1);
        CountDownLatch oldLoadInterrupted = new CountDownLatch(1);
        CountDownLatch releaseOldLoad = new CountDownLatch(1);
        JsonPlaceholderWebCache cache = new JsonPlaceholderWebCache(tasks::add, (source, jsonPath) -> {
            if (attempts.incrementAndGet() == 1) {
                oldLoadEntered.countDown();
                awaitIgnoringInterrupt(releaseOldLoad, oldLoadInterrupted);
                return JsonPlaceholderWebCache.LoadResult.loaded(List.of("stale"));
            }
            return JsonPlaceholderWebCache.LoadResult.loaded(List.of("current"));
        }, time::get, 100L);

        assertEquals(JsonPlaceholderWebCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).status());
        Thread oldLoader = new Thread(tasks.removeNext(), "JsonPlaceholderWebCacheTest-timeout-loader");
        oldLoader.start();
        try {
            assertTrue(oldLoadEntered.await(5L, TimeUnit.SECONDS));
            time.set(100L);
            assertEquals(List.of(), cache.cleanupTimedOut());
            assertTrue(cache.isLoading(PLACEHOLDER));
            time.set(101L);
            assertEquals(List.of(PLACEHOLDER), cache.cleanupTimedOut());
            assertTrue(oldLoadInterrupted.await(5L, TimeUnit.SECONDS));
            assertFalse(cache.isLoading(PLACEHOLDER));

            assertEquals(JsonPlaceholderWebCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).status());
            assertTrue(cache.isLoading(PLACEHOLDER));
            tasks.runNext();
            assertFalse(cache.isLoading(PLACEHOLDER));
            assertEquals(List.of("current"), cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).values());

            releaseOldLoad.countDown();
            oldLoader.join(5000L);
            assertFalse(oldLoader.isAlive());
            assertFalse(cache.isLoading(PLACEHOLDER));
            assertEquals(List.of("current"), cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).values());
            assertEquals(2, attempts.get());
        } finally {
            releaseOldLoad.countDown();
            oldLoader.interrupt();
            oldLoader.join(5000L);
        }
    }

    @Test
    void reloadCancelsQueuedClaimAndLateTaskCannotTouchNewGeneration() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicInteger loads = new AtomicInteger();
        JsonPlaceholderWebCache cache = new JsonPlaceholderWebCache(tasks::add, (source, jsonPath) -> JsonPlaceholderWebCache.LoadResult.loaded(List.of("value-" + loads.incrementAndGet())), () -> 0L, 100L);

        cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH);
        Runnable cancelledTask = tasks.removeNext();
        long oldGeneration = cache.generationNumber();
        cache.reload();
        assertEquals(oldGeneration + 1L, cache.generationNumber());
        assertFalse(cache.isLoading(PLACEHOLDER));
        cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH);
        Runnable currentTask = tasks.removeNext();

        cancelledTask.run();
        assertEquals(0, loads.get());
        assertTrue(cache.isLoading(PLACEHOLDER));
        currentTask.run();
        assertEquals(1, loads.get());
        assertEquals(List.of("value-1"), cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).values());
    }

    @Test
    void loadFailureReleasesClaimForImmediateRetry() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicInteger attempts = new AtomicInteger();
        JsonPlaceholderWebCache cache = new JsonPlaceholderWebCache(tasks::add, (source, jsonPath) -> {
            if (attempts.incrementAndGet() == 1) throw new IOException("expected test failure");
            return JsonPlaceholderWebCache.LoadResult.loaded(List.of("recovered"));
        }, () -> 0L, 100L);

        cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH);
        tasks.runNext();
        assertFalse(cache.isLoading(PLACEHOLDER));
        assertEquals(JsonPlaceholderWebCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).status());
        assertEquals(1, tasks.size());
        tasks.runNext();
        assertEquals(List.of("recovered"), cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).values());
        assertEquals(2, attempts.get());
    }

    @Test
    void invalidSourceIsTerminalUntilReload() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicInteger attempts = new AtomicInteger();
        JsonPlaceholderWebCache cache = new JsonPlaceholderWebCache(tasks::add, (source, jsonPath) -> {
            attempts.incrementAndGet();
            return JsonPlaceholderWebCache.LoadResult.invalid();
        }, () -> 0L, 100L);

        cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH);
        tasks.runNext();
        assertEquals(JsonPlaceholderWebCache.Status.INVALID, cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).status());
        assertTrue(cache.isInvalidSource(SOURCE));
        assertEquals(0, tasks.size());
        assertEquals(1, attempts.get());

        cache.reload();
        assertFalse(cache.isInvalidSource(SOURCE));
        assertEquals(JsonPlaceholderWebCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).status());
        assertEquals(1, tasks.size());
    }

    @Test
    void launcherRejectionReleasesClaimForImmediateRetry() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicBoolean reject = new AtomicBoolean(true);
        JsonPlaceholderWebCache cache = new JsonPlaceholderWebCache(task -> {
            if (reject.getAndSet(false)) throw new RejectedExecutionException("expected test rejection");
            tasks.add(task);
        }, (source, jsonPath) -> JsonPlaceholderWebCache.LoadResult.loaded(List.of("loaded")), () -> 0L, 100L);

        assertEquals(JsonPlaceholderWebCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).status());
        assertFalse(cache.isLoading(PLACEHOLDER));
        assertEquals(JsonPlaceholderWebCache.Status.LOADING, cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).status());
        assertEquals(1, tasks.size());
        tasks.runNext();
        assertEquals(List.of("loaded"), cache.getOrLoad(PLACEHOLDER, SOURCE, JSON_PATH).values());
    }

    private static void awaitIgnoringInterrupt(CountDownLatch latch, CountDownLatch interrupted) {
        while (true) {
            try {
                latch.await();
                return;
            } catch (InterruptedException exception) {
                interrupted.countDown();
            }
        }
    }

}
