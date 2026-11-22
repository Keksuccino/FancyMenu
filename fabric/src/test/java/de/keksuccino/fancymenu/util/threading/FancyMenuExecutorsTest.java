package de.keksuccino.fancymenu.util.threading;

import de.keksuccino.fancymenu.test.IsolatedClassLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FancyMenuExecutorsTest {

    @Test
    @Timeout(10)
    void executorsRunTheirTasksOnNamedDaemonThreads() throws Exception {
        ScheduledExecutorService executor = FancyMenuExecutors.newScheduledThreadPool(2, "FancyMenu-ExecutorsTest");
        try {
            AtomicReference<Thread> workerThread = new AtomicReference<>();
            CountDownLatch ran = new CountDownLatch(1);
            executor.schedule(() -> {
                workerThread.set(Thread.currentThread());
                ran.countDown();
            }, 1L, TimeUnit.MILLISECONDS);

            assertTrue(ran.await(5L, TimeUnit.SECONDS));
            assertTrue(workerThread.get().isDaemon());
            assertTrue(workerThread.get().getName().startsWith("FancyMenu-ExecutorsTest-"), workerThread.get().getName());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(10)
    void shutdownAllStopsEveryExecutorAndDropsTheirPendingTasks() throws Exception {
        // Shutting down is permanent, so it happens on an own copy of the executors that the other tests never see
        Class<?> executorsClass = new IsolatedClassLoader(FancyMenuExecutorsTest.class.getClassLoader(), FancyMenuExecutors.class).loadIsolated(FancyMenuExecutors.class);
        Method newSingleThreadScheduledExecutor = executorsClass.getMethod("newSingleThreadScheduledExecutor", String.class);
        Method shutdownAll = executorsClass.getMethod("shutdownAll");
        ScheduledExecutorService delayedTaskExecutor = (ScheduledExecutorService) newSingleThreadScheduledExecutor.invoke(null, "FancyMenu-ExecutorsTest-Delayed");
        ScheduledExecutorService periodicTaskExecutor = (ScheduledExecutorService) newSingleThreadScheduledExecutor.invoke(null, "FancyMenu-ExecutorsTest-Periodic");
        AtomicBoolean delayedTaskRan = new AtomicBoolean();
        CountDownLatch periodicTaskStarted = new CountDownLatch(1);
        delayedTaskExecutor.schedule(() -> delayedTaskRan.set(true), 1L, TimeUnit.HOURS);
        periodicTaskExecutor.scheduleWithFixedDelay(periodicTaskStarted::countDown, 0L, 1L, TimeUnit.HOURS);
        assertTrue(periodicTaskStarted.await(5L, TimeUnit.SECONDS));

        shutdownAll.invoke(null);
        // A second shutdown has nothing left to do and must not fail
        shutdownAll.invoke(null);

        assertTrue(delayedTaskExecutor.awaitTermination(5L, TimeUnit.SECONDS));
        assertTrue(periodicTaskExecutor.awaitTermination(5L, TimeUnit.SECONDS));
        assertFalse(delayedTaskRan.get());
        assertThrows(RejectedExecutionException.class, () -> delayedTaskExecutor.execute(() -> {}));
    }

    @Test
    @Timeout(10)
    void executorsCreatedAfterShutdownAllAreStoppedRightAway() throws Exception {
        Class<?> executorsClass = new IsolatedClassLoader(FancyMenuExecutorsTest.class.getClassLoader(), FancyMenuExecutors.class).loadIsolated(FancyMenuExecutors.class);
        executorsClass.getMethod("shutdownAll").invoke(null);

        ScheduledExecutorService lateExecutor = (ScheduledExecutorService) executorsClass.getMethod("newScheduledThreadPool", int.class, String.class).invoke(null, 1, "FancyMenu-ExecutorsTest-Late");

        assertTrue(lateExecutor.isShutdown());
        assertThrows(RejectedExecutionException.class, () -> lateExecutor.execute(() -> {}));
    }

}
