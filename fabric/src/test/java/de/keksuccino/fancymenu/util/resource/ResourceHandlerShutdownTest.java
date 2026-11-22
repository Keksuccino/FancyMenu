package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.file.type.FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.InputStream;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResourceHandlerShutdownTest {

    private static final ResourceSource WEB_SOURCE = ResourceSource.of("[source:web]https://example.invalid/image.png");

    @Test
    void shutdownClosesEveryRegisteredResourceExactlyOnce() {
        TestResourceHandler handler = new TestResourceHandler();
        TestResource first = new TestResource();
        TestResource second = new TestResource();
        handler.registerIfKeyAbsent("first", first);
        handler.registerIfKeyAbsent("second", second);

        handler.shutdown();
        handler.shutdown();

        assertEquals(1, first.closeCount.get());
        assertEquals(1, second.closeCount.get());
        assertNull(handler.getIfRegistered("first"));
        assertFalse(handler.hasResource("second"));
        assertTrue(handler.getResourceMap().isEmpty());
    }

    @Test
    void resourcesThatGetRegisteredAfterShutdownGetClosedInsteadOfKeptAlive() {
        TestResourceHandler handler = new TestResourceHandler();
        handler.shutdown();
        TestResource lateKeyResource = new TestResource();
        TestResource lateSourceResource = new TestResource();

        handler.registerIfKeyAbsent("late", lateKeyResource);

        assertNull(handler.putAndReturn(lateSourceResource, WEB_SOURCE));
        assertEquals(1, lateKeyResource.closeCount.get());
        assertEquals(1, lateSourceResource.closeCount.get());
        assertNull(handler.get(WEB_SOURCE));
        assertTrue(handler.getResourceMap().isEmpty());
        assertTrue(handler.getFailedSourcesSet().isEmpty());
    }

    @Test
    void reloadingClosesTheResourcesButKeepsTheHandlerUsable() {
        TestResourceHandler handler = new TestResourceHandler();
        TestResource beforeReload = new TestResource();
        handler.registerIfKeyAbsent("resource", beforeReload);

        handler.releaseAll();
        TestResource afterReload = new TestResource();
        handler.registerIfKeyAbsent("resource", afterReload);

        assertEquals(1, beforeReload.closeCount.get());
        assertSame(afterReload, handler.getIfRegistered("resource"));
        assertEquals(0, afterReload.closeCount.get());
    }

    @Test
    void aSecondResourceForTheSameSourceGetsClosedAndTheFirstOneStaysRegistered() {
        TestResourceHandler handler = new TestResourceHandler();
        TestResource first = new TestResource();
        TestResource second = new TestResource();

        assertSame(first, handler.putAndReturn(first, WEB_SOURCE));
        assertSame(first, handler.putAndReturn(second, WEB_SOURCE));

        assertEquals(0, first.closeCount.get());
        assertEquals(1, second.closeCount.get());
        assertSame(first, handler.getIfRegistered(WEB_SOURCE.getSourceWithPrefix()));
    }

    @Test
    @Timeout(30)
    void registrationsRacingWithShutdownNeverLeaveAnOpenResource() throws InterruptedException {
        int registeringThreads = 4;
        TestResourceHandler handler = new TestResourceHandler();
        Queue<TestResource> createdResources = new ConcurrentLinkedQueue<>();
        AtomicBoolean shutdownDone = new AtomicBoolean();
        CountDownLatch registeredBeforeShutdown = new CountDownLatch(registeringThreads);
        Thread[] threads = new Thread[registeringThreads];
        for (int threadIndex = 0; threadIndex < registeringThreads; threadIndex++) {
            int finalThreadIndex = threadIndex;
            threads[threadIndex] = new Thread(() -> {
                int registrationsAfterShutdown = 0;
                for (int i = 0; registrationsAfterShutdown < 200; i++) {
                    boolean afterShutdown = shutdownDone.get();
                    TestResource resource = new TestResource();
                    createdResources.add(resource);
                    handler.registerIfKeyAbsent("thread_" + finalThreadIndex + "_" + i, resource);
                    if (i == 100) registeredBeforeShutdown.countDown();
                    if (afterShutdown) registrationsAfterShutdown++;
                }
            }, "ResourceHandlerShutdownTest registration " + threadIndex);
            threads[threadIndex].setDaemon(true);
            threads[threadIndex].start();
        }

        assertTrue(registeredBeforeShutdown.await(20L, TimeUnit.SECONDS));
        handler.shutdown();
        shutdownDone.set(true);
        for (Thread thread : threads) {
            thread.join(20000L);
            assertFalse(thread.isAlive());
        }

        assertTrue(handler.getResourceMap().isEmpty());
        for (TestResource resource : createdResources) {
            assertEquals(1, resource.closeCount.get(), "Every resource has to get closed exactly once, by the shutdown or because it came too late");
        }
    }

    private static final class TestResourceHandler extends ResourceHandler<TestResource, FileType<TestResource>> {

        @Override
        public @NotNull List<FileType<TestResource>> getAllowedFileTypes() {
            return List.of();
        }

        @Override
        public @Nullable FileType<TestResource> getFallbackFileType() {
            return null;
        }

    }

    private static final class TestResource implements Resource {

        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public @Nullable InputStream open() {
            return null;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public boolean isLoadingCompleted() {
            return true;
        }

        @Override
        public boolean isLoadingFailed() {
            return false;
        }

        @Override
        public boolean isClosed() {
            return this.closeCount.get() > 0;
        }

        @Override
        public void close() {
            this.closeCount.incrementAndGet();
        }

    }

}
