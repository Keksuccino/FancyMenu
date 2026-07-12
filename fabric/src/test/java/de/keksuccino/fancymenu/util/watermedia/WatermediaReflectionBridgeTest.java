package de.keksuccino.fancymenu.util.watermedia;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatermediaReflectionBridgeTest {

    @AfterEach
    void clearMainThreadFallbackQueue() {
        MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    @Test
    void renderThreadReleaseDrainsAlreadyQueuedGlWorkBeforeGfxCleanup() {
        QueuedExecutor delegate = new QueuedExecutor();
        WatermediaRenderThreadExecutor executor = new WatermediaRenderThreadExecutor(Thread.currentThread(), delegate);
        AtomicInteger glWorkCount = new AtomicInteger();
        Releasable player = new Releasable();
        Releasable gfxEngine = new Releasable();
        WatermediaReflectionBridge.ManagedModernPlayer managedPlayer = new WatermediaReflectionBridge.ManagedModernPlayer(player, gfxEngine, executor);

        executor.execute(glWorkCount::incrementAndGet);
        managedPlayer.release();
        executor.execute(glWorkCount::incrementAndGet);

        assertEquals(1, player.releaseCount.get());
        assertEquals(0, gfxEngine.releaseCount.get());
        assertEquals(1, delegate.size());
        delegate.runAll();
        assertEquals(1, glWorkCount.get());
        assertEquals(1, gfxEngine.releaseCount.get());
    }

    @Test
    void offThreadReleaseQueuesGfxCleanupAfterAcceptedGlWork() throws Exception {
        QueuedExecutor delegate = new QueuedExecutor();
        WatermediaRenderThreadExecutor executor = new WatermediaRenderThreadExecutor(Thread.currentThread(), delegate);
        AtomicInteger glWorkCount = new AtomicInteger();
        Releasable player = new Releasable();
        Releasable gfxEngine = new Releasable();
        WatermediaReflectionBridge.ManagedModernPlayer managedPlayer = new WatermediaReflectionBridge.ManagedModernPlayer(player, gfxEngine, executor);

        executor.execute(glWorkCount::incrementAndGet);
        Thread releaseThread = new Thread(managedPlayer::release);
        releaseThread.setDaemon(true);
        releaseThread.start();
        releaseThread.join(5000L);

        assertFalse(releaseThread.isAlive());
        assertEquals(1, player.releaseCount.get());
        assertEquals(0, gfxEngine.releaseCount.get());
        assertEquals(1, delegate.size());
        delegate.runAll();
        assertEquals(1, glWorkCount.get());
        assertEquals(1, gfxEngine.releaseCount.get());
    }

    @Test
    void concurrentReleaseClaimsPlayerAndGfxExactlyOnce() throws Exception {
        QueuedExecutor delegate = new QueuedExecutor();
        WatermediaRenderThreadExecutor executor = new WatermediaRenderThreadExecutor(Thread.currentThread(), delegate);
        Releasable player = new Releasable();
        Releasable gfxEngine = new Releasable();
        WatermediaReflectionBridge.ManagedModernPlayer managedPlayer = new WatermediaReflectionBridge.ManagedModernPlayer(player, gfxEngine, executor);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Thread firstRelease = new Thread(() -> releaseTogether(managedPlayer, ready, start));
        Thread secondRelease = new Thread(() -> releaseTogether(managedPlayer, ready, start));
        firstRelease.setDaemon(true);
        secondRelease.setDaemon(true);

        firstRelease.start();
        secondRelease.start();
        try {
            assertTrue(ready.await(5L, TimeUnit.SECONDS));
        } finally {
            start.countDown();
        }
        firstRelease.join(5000L);
        secondRelease.join(5000L);

        assertFalse(firstRelease.isAlive());
        assertFalse(secondRelease.isAlive());
        assertEquals(1, player.releaseCount.get());
        assertEquals(1, delegate.size());
        delegate.runAll();
        assertEquals(1, gfxEngine.releaseCount.get());
    }

    @Test
    void playerShutdownCanSubmitFinalGlWorkBeforeExecutorCloses() {
        QueuedExecutor delegate = new QueuedExecutor();
        WatermediaRenderThreadExecutor executor = new WatermediaRenderThreadExecutor(Thread.currentThread(), delegate);
        AtomicInteger finalGlWorkCount = new AtomicInteger();
        SubmittingReleasable player = new SubmittingReleasable(executor, finalGlWorkCount::incrementAndGet);
        Releasable gfxEngine = new Releasable();
        WatermediaReflectionBridge.ManagedModernPlayer managedPlayer = new WatermediaReflectionBridge.ManagedModernPlayer(player, gfxEngine, executor);

        managedPlayer.release();

        assertEquals(1, player.releaseCount.get());
        assertEquals(1, delegate.size());
        assertEquals(0, gfxEngine.releaseCount.get());
        delegate.runAll();
        assertEquals(1, finalGlWorkCount.get());
        assertEquals(1, gfxEngine.releaseCount.get());
    }

    @Test
    void failingAcceptedGlWorkStillTriggersGfxCleanup() {
        QueuedExecutor delegate = new QueuedExecutor();
        WatermediaRenderThreadExecutor executor = new WatermediaRenderThreadExecutor(Thread.currentThread(), delegate);
        Releasable player = new Releasable();
        Releasable gfxEngine = new Releasable();
        WatermediaReflectionBridge.ManagedModernPlayer managedPlayer = new WatermediaReflectionBridge.ManagedModernPlayer(player, gfxEngine, executor);
        executor.execute(() -> { throw new IllegalStateException("render task"); });

        managedPlayer.release();

        assertThrows(IllegalStateException.class, delegate::runAll);
        assertEquals(1, gfxEngine.releaseCount.get());
    }

    @Test
    void inlineTaskFailureCompletesAcceptedTaskOnlyOnce() {
        WatermediaRenderThreadExecutor executor = new WatermediaRenderThreadExecutor(Thread.currentThread(), Runnable::run);
        AtomicInteger cleanupCount = new AtomicInteger();

        assertThrows(IllegalStateException.class, () -> executor.execute(() -> { throw new IllegalStateException("render task"); }));
        executor.close(cleanupCount::incrementAndGet);

        assertEquals(1, cleanupCount.get());
    }

    @Test
    void rejectedTaskDoesNotStrandCleanup() {
        WatermediaRenderThreadExecutor executor = new WatermediaRenderThreadExecutor(Thread.currentThread(), task -> { throw new IllegalStateException("rejected"); });
        AtomicInteger cleanupCount = new AtomicInteger();

        assertThrows(IllegalStateException.class, () -> executor.execute(() -> {}));
        executor.close(cleanupCount::incrementAndGet);

        assertEquals(1, cleanupCount.get());
    }

    @Test
    void offThreadFinalDispatchUsesMainThreadFallbackWhenDelegateRejects() throws Exception {
        Executor rejectingDelegate = task -> { throw new IllegalStateException("rejected"); };
        WatermediaRenderThreadExecutor executor = new WatermediaRenderThreadExecutor(Thread.currentThread(), rejectingDelegate);
        Releasable player = new Releasable();
        Releasable gfxEngine = new Releasable();
        WatermediaReflectionBridge.ManagedModernPlayer managedPlayer = new WatermediaReflectionBridge.ManagedModernPlayer(player, gfxEngine, executor);
        Thread releaseThread = new Thread(managedPlayer::release);
        releaseThread.setDaemon(true);

        releaseThread.start();
        releaseThread.join(5000L);

        assertFalse(releaseThread.isAlive());
        assertEquals(1, player.releaseCount.get());
        assertEquals(0, gfxEngine.releaseCount.get());
        List<Runnable> fallbackTasks = MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        assertEquals(1, fallbackTasks.size());
        fallbackTasks.forEach(Runnable::run);
        assertEquals(1, gfxEngine.releaseCount.get());
    }

    @Test
    void dualFinalDispatchFailurePreservesFallbackFailureAsSuppressed() throws Exception {
        Executor rejectingDelegate = task -> { throw new IllegalStateException("delegate"); };
        Executor rejectingFallback = task -> { throw new IllegalArgumentException("fallback"); };
        WatermediaRenderThreadExecutor executor = new WatermediaRenderThreadExecutor(Thread.currentThread(), rejectingDelegate, rejectingFallback);
        AtomicInteger cleanupCount = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread closeThread = new Thread(() -> {
            try {
                executor.close(cleanupCount::incrementAndGet);
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        closeThread.setDaemon(true);

        closeThread.start();
        closeThread.join(5000L);

        assertFalse(closeThread.isAlive());
        assertTrue(failure.get() instanceof IllegalStateException);
        assertEquals("delegate", failure.get().getMessage());
        assertEquals(1, failure.get().getSuppressed().length);
        assertTrue(failure.get().getSuppressed()[0] instanceof IllegalArgumentException);
        assertEquals("fallback", failure.get().getSuppressed()[0].getMessage());
        assertEquals(0, cleanupCount.get());
    }

    private static void releaseTogether(WatermediaReflectionBridge.ManagedModernPlayer managedPlayer, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        await(start);
        managedPlayer.release();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for test latch");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    public static final class Releasable {

        private final AtomicInteger releaseCount = new AtomicInteger();

        public void release() {
            this.releaseCount.incrementAndGet();
        }
    }

    public static final class SubmittingReleasable {

        private final Executor executor;
        private final Runnable finalTask;
        private final AtomicInteger releaseCount = new AtomicInteger();

        private SubmittingReleasable(Executor executor, Runnable finalTask) {
            this.executor = executor;
            this.finalTask = finalTask;
        }

        public void release() {
            this.releaseCount.incrementAndGet();
            this.executor.execute(this.finalTask);
        }
    }

    private static final class QueuedExecutor implements Executor {

        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public synchronized void execute(Runnable task) {
            this.tasks.add(task);
        }

        synchronized int size() {
            return this.tasks.size();
        }

        void runAll() {
            while (true) {
                Runnable task;
                synchronized (this) {
                    task = this.tasks.poll();
                }
                if (task == null) return;
                task.run();
            }
        }
    }
}
