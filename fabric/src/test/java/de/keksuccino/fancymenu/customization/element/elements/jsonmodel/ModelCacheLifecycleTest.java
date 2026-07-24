package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelCacheLifecycleTest {

    @Test
    void publishesCandidateAsOneUnitAndClosesReplacedCacheOnce() {
        ModelCacheLifecycle<FakeCache> lifecycle = new ModelCacheLifecycle<>();
        FakeCache first = new FakeCache();
        FakeCache second = new FakeCache();

        lifecycle.publish(assertToken(lifecycle), first);
        lifecycle.publish(assertToken(lifecycle), second);

        assertAll(() -> assertSame(second, lifecycle.current()), () -> assertEquals(1, first.closeCalls.get()), () -> assertEquals(0, second.closeCalls.get()));
        lifecycle.destroy();
        assertEquals(1, second.closeCalls.get());
    }

    @Test
    void invalidationDetachesAndClosesCurrentCacheExactlyOnce() {
        ModelCacheLifecycle<FakeCache> lifecycle = new ModelCacheLifecycle<>();
        FakeCache cache = new FakeCache();
        lifecycle.publish(assertToken(lifecycle), cache);

        lifecycle.invalidate();
        lifecycle.invalidate();

        assertAll(() -> assertNull(lifecycle.current()), () -> assertEquals(1, cache.closeCalls.get()), () -> assertFalse(lifecycle.isDestroyed()));
    }

    @Test
    void staleBuildCandidateIsClosedInsteadOfPublished() {
        ModelCacheLifecycle<FakeCache> lifecycle = new ModelCacheLifecycle<>();
        ModelCacheLifecycle.BuildToken staleToken = assertToken(lifecycle);
        lifecycle.invalidate();
        FakeCache staleCandidate = new FakeCache();

        lifecycle.publish(staleToken, staleCandidate);

        assertAll(() -> assertNull(lifecycle.current()), () -> assertEquals(1, staleCandidate.closeCalls.get()));
    }

    @Test
    void buildTokenCanPublishOnlyOneCandidate() {
        ModelCacheLifecycle<FakeCache> lifecycle = new ModelCacheLifecycle<>();
        ModelCacheLifecycle.BuildToken token = assertToken(lifecycle);
        FakeCache accepted = new FakeCache();
        FakeCache rejected = new FakeCache();

        lifecycle.publish(token, accepted);
        lifecycle.publish(token, rejected);

        assertAll(() -> assertSame(accepted, lifecycle.current()), () -> assertEquals(0, accepted.closeCalls.get()), () -> assertEquals(1, rejected.closeCalls.get()));
        lifecycle.destroy();
        assertEquals(1, accepted.closeCalls.get());
    }

    @Test
    void destroyIsPermanentIdempotentAndRejectsInflightBuild() {
        ModelCacheLifecycle<FakeCache> lifecycle = new ModelCacheLifecycle<>();
        FakeCache current = new FakeCache();
        lifecycle.publish(assertToken(lifecycle), current);
        ModelCacheLifecycle.BuildToken inflight = assertToken(lifecycle);

        lifecycle.destroy();
        lifecycle.destroy();
        FakeCache late = new FakeCache();
        lifecycle.publish(inflight, late);

        assertAll(() -> assertTrue(lifecycle.isDestroyed()), () -> assertNull(lifecycle.beginBuild()), () -> assertNull(lifecycle.current()), () -> assertEquals(1, current.closeCalls.get()), () -> assertEquals(1, late.closeCalls.get()));
    }

    @Test
    void concurrentPublishAndInvalidationNeverLeakOrDoubleCloseCandidate() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int iteration = 0; iteration < 100; iteration++) {
                ModelCacheLifecycle<FakeCache> lifecycle = new ModelCacheLifecycle<>();
                ModelCacheLifecycle.BuildToken token = assertToken(lifecycle);
                FakeCache candidate = new FakeCache();
                CountDownLatch start = new CountDownLatch(1);
                Future<?> publish = executor.submit(() -> {
                    if (!start.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting to race model-cache publication");
                    lifecycle.publish(token, candidate);
                    return null;
                });
                Future<?> invalidate = executor.submit(() -> {
                    if (!start.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting to race model-cache invalidation");
                    lifecycle.invalidate();
                    return null;
                });
                start.countDown();
                publish.get(5L, TimeUnit.SECONDS);
                invalidate.get(5L, TimeUnit.SECONDS);
                lifecycle.destroy();
                assertAll(() -> assertNull(lifecycle.current()), () -> assertEquals(1, candidate.closeCalls.get()));
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }
    }

    @Test
    void detachedCachesCloseOutsideLifecycleMonitor() throws Exception {
        ModelCacheLifecycle<BlockingCache> lifecycle = new ModelCacheLifecycle<>();
        CountDownLatch closeStarted = new CountDownLatch(1);
        CountDownLatch allowClose = new CountDownLatch(1);
        BlockingCache cache = new BlockingCache(closeStarted, allowClose);
        lifecycle.publish(assertToken(lifecycle), cache);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> invalidation = executor.submit(lifecycle::invalidate);
            assertTrue(closeStarted.await(5L, TimeUnit.SECONDS));
            Future<ModelCacheLifecycle.BuildToken> tokenRead = executor.submit(lifecycle::beginBuild);

            assertNotNull(tokenRead.get(5L, TimeUnit.SECONDS));
            allowClose.countDown();
            invalidation.get(5L, TimeUnit.SECONDS);
        } finally {
            allowClose.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }

        assertEquals(1, cache.closeCalls.get());
    }

    private static ModelCacheLifecycle.BuildToken assertToken(ModelCacheLifecycle<?> lifecycle) {
        ModelCacheLifecycle.BuildToken token = lifecycle.beginBuild();
        assertNotNull(token);
        return token;
    }

    private static class FakeCache implements AutoCloseable {

        protected final AtomicInteger closeCalls = new AtomicInteger();

        @Override
        public void close() {
            this.closeCalls.incrementAndGet();
        }

    }

    private static final class BlockingCache extends FakeCache {

        private final CountDownLatch closeStarted;
        private final CountDownLatch allowClose;

        private BlockingCache(CountDownLatch closeStarted, CountDownLatch allowClose) {
            this.closeStarted = closeStarted;
            this.allowClose = allowClose;
        }

        @Override
        public void close() {
            super.close();
            this.closeStarted.countDown();
            try {
                if (!this.allowClose.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting to finish detached cache close");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

    }

}
