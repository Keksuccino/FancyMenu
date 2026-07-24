package de.keksuccino.fancymenu.customization.placeholder;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedConcurrentCacheTest {

    @Test
    void rejectsInvalidBounds() {
        assertThrows(IllegalArgumentException.class, () -> new BoundedConcurrentCache<>(0, 1L, (key, value) -> 1L));
        assertThrows(IllegalArgumentException.class, () -> new BoundedConcurrentCache<>(1, 0L, (key, value) -> 1L));
    }

    @Test
    void updatedKeyHasOnlyOneEvictionCandidate() {
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(2, 100L, (key, value) -> 1L);
        cache.put("first", "old");
        cache.put("second", "stable");

        cache.put("first", "new");
        cache.put("third", "latest");

        assertEquals("new", cache.get("first"));
        assertNull(cache.get("second"));
        assertEquals("latest", cache.get("third"));
        assertEquals(2, cache.size());
    }

    @Test
    void weightLimitIsExactAndOversizedValuesAreNotRetained() {
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(10, 5L, (key, value) -> value.length());
        cache.put("two", "12");
        cache.put("three", "345");

        assertEquals(5L, cache.weightedSize());

        cache.put("one", "1");
        assertNull(cache.get("two"));
        assertEquals("345", cache.get("three"));
        assertEquals("1", cache.get("one"));
        assertEquals(4L, cache.weightedSize());

        cache.put("three", "123456");
        assertNull(cache.get("three"));
        assertEquals(1, cache.size());
        assertEquals(1L, cache.weightedSize());
    }

    @Test
    void computeSerializesConcurrentUpdates() throws Exception {
        int workers = 8;
        int updatesPerWorker = 100;
        BoundedConcurrentCache<String, Integer> cache = new BoundedConcurrentCache<>(2, 2L, (key, value) -> 1L);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int worker = 0; worker < workers; worker++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    await(start);
                    for (int update = 0; update < updatesPerWorker; update++) {
                        cache.compute("counter", (key, current) -> (current == null) ? 1 : current + 1);
                    }
                }));
            }
            assertTrue(ready.await(5L, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(5L, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }

        assertEquals(workers * updatesPerWorker, cache.get("counter"));
        assertEquals(1, cache.size());
        assertEquals(1L, cache.weightedSize());
    }

    @Test
    void concurrentDistinctWritesStayWithinBothBounds() throws Exception {
        int workers = 8;
        int writesPerWorker = 200;
        int maximumEntries = 32;
        long maximumWeight = 256L;
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(maximumEntries, maximumWeight, (key, value) -> key.length() + value.length());
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int worker = 0; worker < workers; worker++) {
                int workerId = worker;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    await(start);
                    for (int write = 0; write < writesPerWorker; write++) {
                        String key = workerId + ":" + write;
                        cache.put(key, "value");
                    }
                }));
            }
            assertTrue(ready.await(5L, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(5L, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }

        assertTrue(cache.size() <= maximumEntries);
        assertTrue(cache.weightedSize() <= maximumWeight);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for coordinated test work");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for coordinated test work", ex);
        }
    }

}
