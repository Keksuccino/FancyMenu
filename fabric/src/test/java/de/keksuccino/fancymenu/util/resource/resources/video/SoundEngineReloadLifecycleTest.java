package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundEngineReloadLifecycleTest {

    @Test
    void startsBeforeInitialSoundEngineReload() {
        SoundEngineReloadLifecycle<Object> lifecycle = new SoundEngineReloadLifecycle<>();

        assertFalse(lifecycle.isReloading());
        assertFalse(lifecycle.hasReloadCompleted());
    }

    @Test
    void beforeCallbacksObserveReloadInProgress() {
        SoundEngineReloadLifecycle<Object> lifecycle = new SoundEngineReloadLifecycle<>();
        Object resource = new Object();
        lifecycle.register(resource);

        lifecycle.beforeReload(instance -> {
            assertSame(resource, instance);
            assertTrue(lifecycle.isReloading());
            assertFalse(lifecycle.hasReloadCompleted());
        }, (instance, throwable) -> {});

        assertTrue(lifecycle.isReloading());
        assertFalse(lifecycle.hasReloadCompleted());
    }

    @Test
    void afterCallbacksObserveCompletedReload() {
        SoundEngineReloadLifecycle<Object> lifecycle = new SoundEngineReloadLifecycle<>();
        Object resource = new Object();
        lifecycle.register(resource);
        lifecycle.beforeReload(instance -> {}, (instance, throwable) -> {});

        lifecycle.afterReload(instance -> {
            assertSame(resource, instance);
            assertFalse(lifecycle.isReloading());
            assertTrue(lifecycle.hasReloadCompleted());
        }, (instance, throwable) -> {});

        assertFalse(lifecycle.isReloading());
        assertTrue(lifecycle.hasReloadCompleted());
    }

    @Test
    void resourceRegisteredDuringReloadIsRetriedAfterward() {
        SoundEngineReloadLifecycle<Object> lifecycle = new SoundEngineReloadLifecycle<>();
        Object existingResource = new Object();
        Object lateResource = new Object();
        List<Object> released = new ArrayList<>();
        List<Object> retried = new ArrayList<>();
        lifecycle.register(existingResource);

        lifecycle.beforeReload(resource -> {
            released.add(resource);
            lifecycle.register(lateResource);
        }, (instance, throwable) -> {});
        lifecycle.afterReload(retried::add, (instance, throwable) -> {});

        assertEquals(List.of(existingResource), released);
        assertEquals(2, retried.size());
        assertTrue(retried.contains(existingResource));
        assertTrue(retried.contains(lateResource));
    }

    @Test
    void callbackFailuresAreReportedWithoutSkippingOtherResources() {
        SoundEngineReloadLifecycle<Object> lifecycle = new SoundEngineReloadLifecycle<>();
        Object failingResource = new Object();
        Object healthyResource = new Object();
        AssertionError failure = new AssertionError("cleanup failed");
        AtomicInteger healthyCallbacks = new AtomicInteger();
        List<Throwable> reportedFailures = new ArrayList<>();
        lifecycle.register(failingResource);
        lifecycle.register(healthyResource);

        lifecycle.beforeReload(resource -> {
            if (resource == failingResource) throw failure;
            healthyCallbacks.incrementAndGet();
        }, (resource, throwable) -> reportedFailures.add(throwable));

        assertEquals(1, healthyCallbacks.get());
        assertEquals(List.of(failure), reportedFailures);
        assertTrue(lifecycle.isReloading());
    }

    @Test
    void repeatedReloadsRunEveryLifecycleCallback() {
        SoundEngineReloadLifecycle<Object> lifecycle = new SoundEngineReloadLifecycle<>();
        AtomicInteger releases = new AtomicInteger();
        AtomicInteger retries = new AtomicInteger();
        Object resource = new Object();
        lifecycle.register(resource);

        lifecycle.beforeReload(instance -> releases.incrementAndGet(), (instance, throwable) -> {});
        lifecycle.afterReload(instance -> retries.incrementAndGet(), (instance, throwable) -> {});
        lifecycle.beforeReload(instance -> releases.incrementAndGet(), (instance, throwable) -> {});
        lifecycle.afterReload(instance -> retries.incrementAndGet(), (instance, throwable) -> {});

        assertEquals(2, releases.get());
        assertEquals(2, retries.get());
        assertFalse(lifecycle.isReloading());
        assertTrue(lifecycle.hasReloadCompleted());
    }
}
