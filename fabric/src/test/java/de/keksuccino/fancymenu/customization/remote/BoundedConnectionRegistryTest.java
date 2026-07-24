package de.keksuccino.fancymenu.customization.remote;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedConnectionRegistryTest {

    @Test
    void capsActiveStatesButStillReturnsAnExistingState() {
        BoundedConnectionRegistry<TestState> registry = new BoundedConnectionRegistry<>(2, 2);
        AtomicInteger nextId = new AtomicInteger();

        BoundedConnectionRegistry.Admission<TestState> first = admit(registry, "ws://first", nextId);
        BoundedConnectionRegistry.Admission<TestState> second = admit(registry, "ws://second", nextId);
        BoundedConnectionRegistry.Admission<TestState> rejected = admit(registry, "ws://third", nextId);
        BoundedConnectionRegistry.Admission<TestState> existing = admit(registry, "ws://first", nextId);

        assertEquals(BoundedConnectionRegistry.AdmissionType.CREATED, first.type());
        assertEquals(BoundedConnectionRegistry.AdmissionType.CREATED, second.type());
        assertEquals(BoundedConnectionRegistry.AdmissionType.CAPACITY_EXCEEDED, rejected.type());
        assertNull(rejected.state());
        assertEquals(BoundedConnectionRegistry.AdmissionType.EXISTING, existing.type());
        assertSame(first.state(), existing.state());
        assertEquals(2, registry.activeStateCount());
        assertSame(first.state(), registry.getByRequestId(first.state().requestId()));
    }

    @Test
    void requestIdSurvivesRemovalAndRecreationForTheSameUrl() {
        BoundedConnectionRegistry<TestState> registry = new BoundedConnectionRegistry<>(1, 2);
        AtomicInteger nextId = new AtomicInteger();
        TestState first = admit(registry, "ws://same", nextId).state();

        assertTrue(registry.remove(first.url(), first.requestId(), first));
        TestState recreated = admit(registry, "ws://same", nextId).state();

        assertEquals(first.requestId(), recreated.requestId());
        assertEquals(1, nextId.get());
    }

    @Test
    void cacheUsesLruEvictionAndNeverEvictsAnActiveIdentity() {
        BoundedConnectionRegistry<TestState> registry = new BoundedConnectionRegistry<>(2, 2);
        AtomicInteger nextId = new AtomicInteger();
        TestState active = admit(registry, "ws://active", nextId).state();
        TestState inactive = admit(registry, "ws://inactive", nextId).state();
        assertTrue(registry.remove(inactive.url(), inactive.requestId(), inactive));

        TestState newest = admit(registry, "ws://newest", nextId).state();

        assertEquals(2, registry.cachedRequestIdCount());
        assertEquals(active.requestId(), registry.cachedRequestId(active.url()));
        assertEquals(newest.requestId(), registry.cachedRequestId(newest.url()));
        assertNull(registry.cachedRequestId(inactive.url()));
    }

    @Test
    void removalRequiresBothMappedIdentityAndKeys() {
        BoundedConnectionRegistry<TestState> registry = new BoundedConnectionRegistry<>(1, 1);
        AtomicInteger nextId = new AtomicInteger();
        TestState state = admit(registry, "ws://one", nextId).state();

        assertFalse(registry.remove(state.url(), state.requestId(), new TestState(state.url(), state.requestId())));
        assertFalse(registry.remove("ws://other", state.requestId(), state));
        assertFalse(registry.remove(state.url(), "other", state));
        assertEquals(1, registry.activeStateCount());
        assertTrue(registry.remove(state.url(), state.requestId(), state));
        assertEquals(0, registry.activeStateCount());
        assertNull(registry.getByRequestId(state.requestId()));
    }

    @Test
    void clearRemovesActiveIndexesAndCachedRequestIds() {
        BoundedConnectionRegistry<TestState> registry = new BoundedConnectionRegistry<>(2, 3);
        AtomicInteger nextId = new AtomicInteger();
        TestState active = admit(registry, "ws://active", nextId).state();
        TestState inactive = admit(registry, "ws://inactive", nextId).state();
        assertTrue(registry.remove(inactive.url(), inactive.requestId(), inactive));

        registry.clear();

        assertEquals(0, registry.activeStateCount());
        assertEquals(0, registry.cachedRequestIdCount());
        assertNull(registry.getByUrl(active.url()));
        assertNull(registry.getByRequestId(active.requestId()));
        assertNull(registry.cachedRequestId(active.url()));
        assertNull(registry.cachedRequestId(inactive.url()));
    }

    @Test
    void reportsExhaustionWhenSupplierCannotProduceAUniqueActiveId() {
        BoundedConnectionRegistry<TestState> registry = new BoundedConnectionRegistry<>(2, 2);
        BoundedConnectionRegistry.Admission<TestState> first = registry.getOrCreate("ws://one", () -> "duplicate", TestState::new);
        BoundedConnectionRegistry.Admission<TestState> second = registry.getOrCreate("ws://two", () -> "duplicate", TestState::new);

        assertEquals(BoundedConnectionRegistry.AdmissionType.CREATED, first.type());
        assertEquals(BoundedConnectionRegistry.AdmissionType.REQUEST_ID_EXHAUSTED, second.type());
        assertNull(second.state());
        assertEquals(1, registry.activeStateCount());
    }

    @Test
    void concurrentDistinctAdmissionsNeverExceedTheActiveCap() throws Exception {
        int attempts = 64;
        int limit = 8;
        BoundedConnectionRegistry<TestState> registry = new BoundedConnectionRegistry<>(limit, 16);
        AtomicInteger nextId = new AtomicInteger();
        AtomicInteger created = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        try {
            for (int index = 0; index < attempts; index++) {
                String url = "ws://connection-" + index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    if (admit(registry, url, nextId).type() == BoundedConnectionRegistry.AdmissionType.CREATED) {
                        created.incrementAndGet();
                    }
                    return null;
                }));
            }
            boolean allReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertTrue(allReady);
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(limit, created.get());
        assertEquals(limit, registry.activeStateCount());
        assertTrue(registry.cachedRequestIdCount() <= 16);
    }

    @Test
    void concurrentAdmissionsForOneUrlCreateExactlyOneState() throws Exception {
        int attempts = 64;
        BoundedConnectionRegistry<TestState> registry = new BoundedConnectionRegistry<>(2, 2);
        AtomicInteger suppliedIds = new AtomicInteger();
        AtomicInteger factoryCalls = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<TestState>> futures = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        try {
            for (int index = 0; index < attempts; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return registry.getOrCreate("ws://same", () -> "id-" + suppliedIds.incrementAndGet(), (url, requestId) -> {
                        factoryCalls.incrementAndGet();
                        return new TestState(url, requestId);
                    }).state();
                }));
            }
            boolean allReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertTrue(allReady);
            TestState expected = futures.get(0).get(5, TimeUnit.SECONDS);
            for (Future<TestState> future : futures) {
                assertSame(expected, future.get(5, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, suppliedIds.get());
        assertEquals(1, factoryCalls.get());
        assertEquals(1, registry.activeStateCount());
    }

    private static BoundedConnectionRegistry.Admission<TestState> admit(BoundedConnectionRegistry<TestState> registry, String url, AtomicInteger nextId) {
        return registry.getOrCreate(url, () -> "request-" + nextId.incrementAndGet(), TestState::new);
    }

    private record TestState(String url, String requestId) {
    }
}
