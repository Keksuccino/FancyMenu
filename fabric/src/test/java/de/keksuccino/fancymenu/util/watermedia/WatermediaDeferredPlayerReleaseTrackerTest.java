package de.keksuccino.fancymenu.util.watermedia;

import de.keksuccino.fancymenu.test.IsolatedClassLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class WatermediaDeferredPlayerReleaseTrackerTest {

    @Test
    @Timeout(10)
    void playersGetReleasedOnceEitherByTheirDeferredReleaseOrByTheShutdown() throws Exception {
        // Shutting down is permanent, so it happens on an own copy of the tracker that the other tests never see
        Class<?> trackerClass = new IsolatedClassLoader(WatermediaDeferredPlayerReleaseTrackerTest.class.getClassLoader(), WatermediaDeferredPlayerReleaseTracker.class).loadIsolated(WatermediaDeferredPlayerReleaseTracker.class);
        Method track = trackerClass.getMethod("track", Object.class);
        Method release = trackerClass.getMethod("release", Object.class);
        Method shutdown = trackerClass.getMethod("shutdown");
        FakePlayer releasedNormally = new FakePlayer();
        FakePlayer untracked = new FakePlayer();
        FakePlayer pendingAtShutdown = new FakePlayer();
        FakePlayer trackedAfterShutdown = new FakePlayer();

        assertTrue((boolean) track.invoke(null, releasedNormally));
        release.invoke(null, releasedNormally);
        release.invoke(null, releasedNormally);
        release.invoke(null, untracked);
        assertEquals(1, releasedNormally.releases.get());
        assertEquals(0, untracked.releases.get());

        assertTrue((boolean) track.invoke(null, pendingAtShutdown));
        shutdown.invoke(null);
        // Players that are not created by the reflection bridge get released on a daemon thread, so a hanging release can't block the shutdown
        assertTrue(pendingAtShutdown.released.await(5L, TimeUnit.SECONDS));
        assertEquals(1, pendingAtShutdown.stops.get());
        assertTrue(pendingAtShutdown.releaseThread.get().isDaemon());

        assertFalse((boolean) track.invoke(null, trackedAfterShutdown));
        shutdown.invoke(null);
        release.invoke(null, pendingAtShutdown);
        assertEquals(1, pendingAtShutdown.releases.get());
        assertEquals(1, releasedNormally.releases.get());
        assertEquals(0, trackedAfterShutdown.releases.get());
    }

    /** Stands in for a WaterMedia player, which the reflection bridge controls through its public methods. */
    public static final class FakePlayer {

        private final AtomicInteger stops = new AtomicInteger();
        private final AtomicInteger releases = new AtomicInteger();
        private final AtomicReference<Thread> releaseThread = new AtomicReference<>();
        private final CountDownLatch released = new CountDownLatch(1);

        public void stop() {
            this.stops.incrementAndGet();
        }

        public void release() {
            this.releases.incrementAndGet();
            this.releaseThread.set(Thread.currentThread());
            this.released.countDown();
        }

    }

}
