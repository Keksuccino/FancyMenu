package de.keksuccino.fancymenu.util.rinku;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RinkuExecutorsTest {

    @Test
    void createsNamedDaemonWorkersWithShutdownSafeSchedulingPolicies() throws Exception {
        ScheduledThreadPoolExecutor executor = RinkuExecutors.createExecutor("FancyMenu-Rinku-Test");
        try {
            assertTrue(executor.getRemoveOnCancelPolicy());
            assertFalse(executor.getExecuteExistingDelayedTasksAfterShutdownPolicy());
            assertFalse(executor.getContinueExistingPeriodicTasksAfterShutdownPolicy());
            WorkerProperties properties = executor.submit(() -> new WorkerProperties(Thread.currentThread().getName(), Thread.currentThread().isDaemon())).get(5L, TimeUnit.SECONDS);
            assertEquals("FancyMenu-Rinku-Test-1", properties.name());
            assertTrue(properties.daemon());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shutdownStopsOwnedExecutorsAndStandaloneWorkers() throws Exception {
        RinkuExecutors.Registry registry = new RinkuExecutors.Registry();
        ScheduledExecutorService executor = registry.newSingleThreadScheduledExecutor("FancyMenu-Rinku-Lifecycle-Test");
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch workerStopped = new CountDownLatch(1);
        Thread worker = registry.startDaemonWorker(() -> awaitInterruption(workerStarted, workerStopped), "FancyMenu-Rinku-Lifecycle-Worker");

        assertTrue(workerStarted.await(5L, TimeUnit.SECONDS));
        registry.shutdownAll();

        assertTrue(executor.isShutdown());
        assertTrue(workerStopped.await(5L, TimeUnit.SECONDS));
        assertTrue(worker.isDaemon());
        assertThrows(RejectedExecutionException.class, () -> registry.startDaemonWorker(() -> {}, "FancyMenu-Rinku-Rejected-Worker"));
        assertTrue(registry.newSingleThreadScheduledExecutor("FancyMenu-Rinku-Rejected-Executor").isShutdown());
    }

    @Test
    void concurrentRegistrationCannotEscapeShutdown() throws Exception {
        RinkuExecutors.Registry registry = new RinkuExecutors.Registry();
        CountDownLatch registrationStarted = new CountDownLatch(1);
        List<ScheduledExecutorService> executors = new ArrayList<>();
        Thread registrationThread = new Thread(() -> {
            registrationStarted.countDown();
            for (int i = 0; i < 100; i++) {
                synchronized (executors) {
                    executors.add(registry.newSingleThreadScheduledExecutor("FancyMenu-Rinku-Concurrency-Test-" + i));
                }
            }
        });
        registrationThread.start();
        assertTrue(registrationStarted.await(5L, TimeUnit.SECONDS));

        registry.shutdownAll();
        registrationThread.join(TimeUnit.SECONDS.toMillis(5L));

        assertFalse(registrationThread.isAlive());
        synchronized (executors) {
            assertEquals(100, executors.size());
            assertTrue(executors.stream().allMatch(ScheduledExecutorService::isShutdown));
        }
    }

    private static void awaitInterruption(CountDownLatch started, CountDownLatch stopped) {
        started.countDown();
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(30L));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            stopped.countDown();
        }
    }

    private record WorkerProperties(String name, boolean daemon) {}
}
