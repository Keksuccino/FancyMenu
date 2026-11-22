package de.keksuccino.fancymenu.util.threading;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FancyMenuThreadsTest {

    @Test
    @Timeout(10)
    void startsTheRunnableOnANamedDaemonThread() throws InterruptedException {
        AtomicReference<Thread> runningThread = new AtomicReference<>();
        CountDownLatch ran = new CountDownLatch(1);

        Thread thread = FancyMenuThreads.startDaemonThread(() -> {
            runningThread.set(Thread.currentThread());
            ran.countDown();
        }, "ThreadsTest-Role");

        assertTrue(ran.await(5L, TimeUnit.SECONDS));
        thread.join(5000L);
        assertSame(thread, runningThread.get());
        assertTrue(thread.isDaemon());
        assertTrue(thread.getName().startsWith("FancyMenu-ThreadsTest-Role-"), thread.getName());
        assertFalse(thread.isAlive());
    }

    @Test
    @Timeout(10)
    void everyThreadGetsItsOwnNameAndHandlesItsUncaughtExceptions() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        Thread first = FancyMenuThreads.startDaemonThread(() -> {}, "ThreadsTest-Role");
        Thread second = FancyMenuThreads.startDaemonThread(() -> {
            awaitQuietly(release);
            throw new IllegalStateException("Expected test failure");
        }, "ThreadsTest-Role");

        try {
            assertNotEquals(first.getName(), second.getName());
            // Only checkable while the thread runs: a thread without an own handler reports its thread group, a finished thread reports none
            assertTrue(second.isAlive());
            Thread.UncaughtExceptionHandler handler = second.getUncaughtExceptionHandler();
            assertNotNull(handler);
            assertFalse(handler instanceof ThreadGroup);
        } finally {
            release.countDown();
        }
        first.join(5000L);
        second.join(5000L);
        assertFalse(second.isAlive());
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5L, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

}
