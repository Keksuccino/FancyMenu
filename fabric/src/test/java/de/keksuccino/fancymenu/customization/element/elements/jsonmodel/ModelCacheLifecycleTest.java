package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelCacheLifecycleTest {

    @Test
    void replacementPublishesAsOneUnitAndClosesOldCache() {
        ModelCacheLifecycle<FakeCache> lifecycle = new ModelCacheLifecycle<>();
        FakeCache first = new FakeCache();
        FakeCache second = new FakeCache();
        lifecycle.publish(lifecycle.beginBuild(), first);

        lifecycle.publish(lifecycle.beginBuild(), second);

        assertSame(second, lifecycle.current());
        assertEquals(1, first.closes.get());
        assertEquals(0, second.closes.get());
    }

    @Test
    void staleCandidateIsClosedInsteadOfPublished() {
        ModelCacheLifecycle<FakeCache> lifecycle = new ModelCacheLifecycle<>();
        ModelCacheLifecycle.BuildToken stale = lifecycle.beginBuild();
        lifecycle.invalidate();
        FakeCache candidate = new FakeCache();

        lifecycle.publish(stale, candidate);

        assertNull(lifecycle.current());
        assertEquals(1, candidate.closes.get());
    }

    @Test
    void tokenCanPublishOnlyOnce() {
        ModelCacheLifecycle<FakeCache> lifecycle = new ModelCacheLifecycle<>();
        ModelCacheLifecycle.BuildToken token = lifecycle.beginBuild();
        FakeCache accepted = new FakeCache();
        FakeCache rejected = new FakeCache();

        lifecycle.publish(token, accepted);
        lifecycle.publish(token, rejected);

        assertSame(accepted, lifecycle.current());
        assertEquals(1, rejected.closes.get());
    }

    @Test
    void destructionIsPermanentAndClosesCurrentAndLateCandidates() {
        ModelCacheLifecycle<FakeCache> lifecycle = new ModelCacheLifecycle<>();
        FakeCache current = new FakeCache();
        lifecycle.publish(lifecycle.beginBuild(), current);
        ModelCacheLifecycle.BuildToken inflight = lifecycle.beginBuild();
        lifecycle.destroy();
        FakeCache late = new FakeCache();
        lifecycle.publish(inflight, late);

        assertTrue(lifecycle.isDestroyed());
        assertNull(lifecycle.beginBuild());
        assertEquals(1, current.closes.get());
        assertEquals(1, late.closes.get());
    }

    private static final class FakeCache implements AutoCloseable {
        private final AtomicInteger closes = new AtomicInteger();

        @Override
        public void close() {
            this.closes.incrementAndGet();
        }
    }
}
