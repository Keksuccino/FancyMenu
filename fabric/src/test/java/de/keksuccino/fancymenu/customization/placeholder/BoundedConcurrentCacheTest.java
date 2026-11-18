package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
    void oldQueuePositionOfARefreshedKeyNeverEvictsItsNewerValue() {
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(3, 100L, (key, value) -> 1L);
        cache.put("a", "a1");
        cache.put("b", "b1");
        cache.put("c", "c1");
        for (int refresh = 2; refresh <= 1000; refresh++) cache.put("a", "a" + refresh);

        // The first two evictions reach the old position of "a", which must not take its newer value with it
        cache.put("d", "d1");
        assertNull(cache.get("b"));
        cache.put("e", "e1");
        assertNull(cache.get("c"));
        assertEquals("a1000", cache.get("a"));

        cache.put("f", "f1");
        assertNull(cache.get("a"));
        assertEquals("d1", cache.get("d"));
        assertEquals("e1", cache.get("e"));
        assertEquals("f1", cache.get("f"));
        assertEquals(3, cache.size());
        assertEquals(3, cache.evictionQueueLength());
    }

    @Test
    void repeatedRefreshesNeverGrowTheEvictionQueue() {
        int maximumEntries = 512;
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(maximumEntries, 1_000_000L, (key, value) -> key.length() + value.length());
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < maximumEntries; i++) keys.add("key-" + i);
        for (String key : keys) cache.put(key, "value");

        for (int refresh = 0; refresh < 100_000; refresh++) {
            cache.put(keys.get(7), "value");
            cache.put(keys.get(refresh % maximumEntries), "value");
            cache.compute(keys.get((refresh * 31) % maximumEntries), (key, current) -> current);
        }

        assertEquals(maximumEntries, cache.size());
        assertEquals(maximumEntries, cache.evictionQueueLength());
        long expectedWeight = 0L;
        for (String key : keys) expectedWeight += key.length() + "value".length();
        assertEquals(expectedWeight, cache.weightedSize());
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
        assertEquals(1, cache.evictionQueueLength());
    }

    @Test
    void heavierValueForACachedKeyEvictsOtherEntriesButKeepsTheKey() {
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(10, 10L, (key, value) -> value.length());
        cache.put("a", "aaa");
        cache.put("b", "bbb");
        cache.put("c", "ccc");

        cache.put("a", "AAAAAAA");

        assertEquals("AAAAAAA", cache.get("a"));
        assertNull(cache.get("b"));
        assertEquals("ccc", cache.get("c"));
        assertEquals(10L, cache.weightedSize());
        assertEquals(2, cache.size());

        // "a" got written last, so "c" is the oldest entry now
        cache.put("d", "ddd");
        assertNull(cache.get("c"));
        assertEquals("AAAAAAA", cache.get("a"));
        assertEquals("ddd", cache.get("d"));
    }

    @Test
    void weightsBelowOneCountAsOne() {
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(10, 2L, (key, value) -> -5L);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");

        assertNull(cache.get("a"));
        assertEquals(2L, cache.weightedSize());
        assertEquals(2, cache.size());
    }

    @Test
    void putIfAbsentKeepsTheCachedValueAndItsQueuePosition() {
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(2, 100L, (key, value) -> 1L);

        assertNull(cache.putIfAbsent("first", "original"));
        cache.put("second", "stable");
        assertEquals("original", cache.putIfAbsent("first", "ignored"));
        assertEquals("original", cache.get("first"));

        // Unlike put, putIfAbsent did not move "first" to the end of the queue
        cache.put("third", "latest");
        assertNull(cache.get("first"));
        assertEquals("stable", cache.get("second"));
        assertEquals("latest", cache.get("third"));
    }

    @Test
    void putIfAbsentDoesNotRetainOversizedValues() {
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(10, 3L, (key, value) -> value.length());

        assertNull(cache.putIfAbsent("key", "1234"));
        assertNull(cache.get("key"));
        assertEquals(0, cache.size());
        assertEquals(0L, cache.weightedSize());
    }

    @Test
    void computeCanKeepReplaceAndRemoveValues() {
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(2, 100L, (key, value) -> value.length());

        assertNull(cache.compute("key", (key, current) -> null));
        assertEquals(0, cache.size());
        assertEquals("one", cache.compute("key", (key, current) -> (current == null) ? "one" : current + "!"));
        assertEquals("one!", cache.compute("key", (key, current) -> (current == null) ? "one" : current + "!"));
        assertEquals(4L, cache.weightedSize());

        assertNull(cache.compute("key", (key, current) -> null));
        assertNull(cache.get("key"));
        assertEquals(0, cache.size());
        assertEquals(0L, cache.weightedSize());
        assertEquals(0, cache.evictionQueueLength());
    }

    @Test
    void clearRemovesEverythingAndTheCacheStaysUsable() {
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(2, 100L, (key, value) -> 1L);
        cache.put("a", "1");
        cache.put("b", "2");

        cache.clear();

        assertNull(cache.get("a"));
        assertNull(cache.get("b"));
        assertEquals(0, cache.size());
        assertEquals(0L, cache.weightedSize());
        assertEquals(0, cache.evictionQueueLength());

        cache.put("c", "3");
        cache.put("d", "4");
        cache.put("e", "5");
        assertNull(cache.get("c"));
        assertEquals("4", cache.get("d"));
        assertEquals("5", cache.get("e"));
        assertEquals(2, cache.evictionQueueLength());
    }

    @Test
    void randomOperationsMatchAFirstInFirstOutReferenceModel() {
        Random random = new Random(20261119L);
        String[] keys = new String[12];
        for (int i = 0; i < keys.length; i++) keys[i] = "k" + i;
        for (int run = 0; run < 20; run++) {
            int maximumEntries = 1 + random.nextInt(6);
            long maximumWeight = 1L + random.nextInt(30);
            BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(maximumEntries, maximumWeight, (key, value) -> value.length());
            ReferenceCache reference = new ReferenceCache(maximumEntries, maximumWeight);
            for (int operation = 0; operation < 5_000; operation++) {
                String key = keys[random.nextInt(keys.length)];
                String value = "v".repeat(random.nextInt(8));
                int kind = random.nextInt(100);
                if (kind < 45) {
                    cache.put(key, value);
                    reference.put(key, value);
                } else if (kind < 65) {
                    assertEquals(reference.putIfAbsent(key, value), cache.putIfAbsent(key, value));
                } else if (kind < 75) {
                    assertEquals(reference.compute(key, null), cache.compute(key, (ignored, current) -> null));
                } else if (kind < 85) {
                    assertEquals(reference.compute(key, reference.get(key)), cache.compute(key, (ignored, current) -> current));
                } else if (kind < 99) {
                    String computed = reference.compute(key, (reference.get(key) == null) ? value : reference.get(key) + "v");
                    assertEquals(computed, cache.compute(key, (ignored, current) -> (current == null) ? value : current + "v"));
                } else {
                    cache.clear();
                    reference.clear();
                }
                for (String checkedKey : keys) assertEquals(reference.get(checkedKey), cache.get(checkedKey), () -> "Different value for " + checkedKey);
                assertEquals(reference.size(), cache.size());
                assertEquals(reference.weightedSize(), cache.weightedSize());
                assertEquals(reference.size(), cache.evictionQueueLength());
            }
        }
    }

    @Test
    void readersNeverMissACachedKeyWhileItsValueGetsReplaced() throws Exception {
        int readers = 4;
        int writes = 200_000;
        BoundedConcurrentCache<String, Integer> cache = new BoundedConcurrentCache<>(4, 1_000L, (key, value) -> 1L + (value % 3));
        cache.put("watched", 0);
        AtomicBoolean writing = new AtomicBoolean(true);
        AtomicLong misses = new AtomicLong();
        AtomicLong reads = new AtomicLong();
        ExecutorService executor = Executors.newFixedThreadPool(readers + 1);
        CountDownLatch ready = new CountDownLatch(readers + 1);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int reader = 0; reader < readers; reader++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    await(start);
                    long localReads = 0L;
                    while (writing.get()) {
                        if (cache.get("watched") == null) misses.incrementAndGet();
                        localReads++;
                    }
                    reads.addAndGet(localReads);
                }));
            }
            futures.add(executor.submit(() -> {
                ready.countDown();
                await(start);
                try {
                    for (int write = 1; write <= writes; write++) {
                        int value = write;
                        if ((write % 2) == 0) {
                            cache.put("watched", value);
                        } else {
                            cache.compute("watched", (key, current) -> value);
                        }
                        // Other keys keep the eviction queue busy around the watched entry
                        cache.put("other-" + (write % 8), value);
                    }
                } finally {
                    writing.set(false);
                }
            }));
            assertTrue(ready.await(5L, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(30L, TimeUnit.SECONDS);
        } finally {
            writing.set(false);
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }

        assertTrue(reads.get() > 0L);
        assertEquals(0L, misses.get());
        assertEquals(writes, cache.get("watched"));
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
        assertEquals(cache.size(), cache.evictionQueueLength());
    }

    @Test
    void concurrentMixedOperationsKeepMapQueueAndWeightConsistent() throws Exception {
        int workers = 8;
        int operationsPerWorker = 20_000;
        int maximumEntries = 16;
        long maximumWeight = 64L;
        String[] keys = new String[24];
        for (int i = 0; i < keys.length; i++) keys[i] = "key-" + i;
        BoundedConcurrentCache<String, String> cache = new BoundedConcurrentCache<>(maximumEntries, maximumWeight, (key, value) -> value.length());
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int worker = 0; worker < workers; worker++) {
                long seed = 7919L * (worker + 1);
                futures.add(executor.submit(() -> {
                    Random random = new Random(seed);
                    ready.countDown();
                    await(start);
                    for (int operation = 0; operation < operationsPerWorker; operation++) {
                        String key = keys[random.nextInt(keys.length)];
                        String value = "v".repeat(1 + random.nextInt(9));
                        int kind = random.nextInt(100);
                        if (kind < 40) {
                            cache.put(key, value);
                        } else if (kind < 60) {
                            cache.putIfAbsent(key, value);
                        } else if (kind < 75) {
                            cache.compute(key, (ignored, current) -> (current == null) ? value : null);
                        } else if (kind < 99) {
                            cache.get(key);
                        } else {
                            cache.clear();
                        }
                    }
                }));
            }
            assertTrue(ready.await(5L, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(30L, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }

        long presentWeight = 0L;
        int presentEntries = 0;
        for (String key : keys) {
            String value = cache.get(key);
            if (value != null) {
                presentWeight += value.length();
                presentEntries++;
            }
        }
        assertTrue(presentEntries <= maximumEntries);
        assertEquals(presentEntries, cache.size());
        assertEquals(presentEntries, cache.evictionQueueLength());
        assertEquals(presentWeight, cache.weightedSize());
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

    /**
     * The eviction rules of the cache written the simple way: every write of a value removes the old entry of its key
     * and appends the new one, and the oldest entries get evicted until the new one fits.
     */
    private static final class ReferenceCache {

        private final LinkedHashMap<String, String> entries = new LinkedHashMap<>();
        private final int maximumEntries;
        private final long maximumWeight;
        private long weightedSize;

        private ReferenceCache(int maximumEntries, long maximumWeight) {
            this.maximumEntries = maximumEntries;
            this.maximumWeight = maximumWeight;
        }

        @Nullable
        private String get(String key) {
            return this.entries.get(key);
        }

        private void put(String key, String value) {
            long weight = Math.max(1L, value.length());
            this.remove(key);
            if (weight > this.maximumWeight) return;
            Iterator<Map.Entry<String, String>> oldestFirst = this.entries.entrySet().iterator();
            while (((this.entries.size() >= this.maximumEntries) || (weight > (this.maximumWeight - this.weightedSize))) && oldestFirst.hasNext()) {
                this.weightedSize -= Math.max(1L, oldestFirst.next().getValue().length());
                oldestFirst.remove();
            }
            this.entries.put(key, value);
            this.weightedSize += weight;
        }

        @Nullable
        private String putIfAbsent(String key, String value) {
            String existing = this.entries.get(key);
            if (existing != null) return existing;
            this.put(key, value);
            return null;
        }

        @Nullable
        private String compute(String key, @Nullable String replacement) {
            if (replacement == null) {
                this.remove(key);
            } else {
                this.put(key, replacement);
            }
            return replacement;
        }

        private void remove(String key) {
            String removed = this.entries.remove(key);
            if (removed != null) this.weightedSize -= Math.max(1L, removed.length());
        }

        private void clear() {
            this.entries.clear();
            this.weightedSize = 0L;
        }

        private int size() {
            return this.entries.size();
        }

        private long weightedSize() {
            return this.weightedSize;
        }

    }

}
