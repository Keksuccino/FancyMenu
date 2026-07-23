package de.keksuccino.fancymenu.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternetAvailabilityMonitorTest {

    @Test
    void schedulesOneImmediateRefreshWithTwentySecondFixedDelayAndPublishesEveryResult() {
        RecordingProbe probe = new RecordingProbe(false, true, false);
        ManualFixedDelayScheduler scheduler = new ManualFixedDelayScheduler();
        AtomicBoolean availability = new AtomicBoolean(false);
        InternetAvailabilityMonitor monitor = new InternetAvailabilityMonitor(probe, () -> scheduler, Duration.ofSeconds(20L), availability::set);

        monitor.init();
        monitor.init();

        assertEquals(1, scheduler.scheduleCalls);
        assertEquals(Duration.ZERO, scheduler.initialDelay);
        assertEquals(Duration.ofSeconds(20L), scheduler.delay);
        scheduler.runNext();
        assertFalse(availability.get());
        scheduler.runNext();
        assertTrue(availability.get());
        scheduler.runNext();
        assertFalse(availability.get());
        assertEquals(3, probe.attempts.get());
    }

    @Test
    void unexpectedProbeFailurePublishesUnavailableAndDoesNotSuppressTheNextRefresh() {
        RecordingProbe probe = new RecordingProbe(new IllegalStateException("probe failed"), true);
        ManualFixedDelayScheduler scheduler = new ManualFixedDelayScheduler();
        AtomicBoolean availability = new AtomicBoolean(true);
        InternetAvailabilityMonitor monitor = new InternetAvailabilityMonitor(probe, () -> scheduler, Duration.ofSeconds(20L), availability::set);

        monitor.init();
        scheduler.runNext();
        assertFalse(availability.get());
        assertFalse(scheduler.task.cancelled);

        scheduler.runNext();
        assertTrue(availability.get());
        assertEquals(2, probe.attempts.get());
    }

    @Test
    void initAndShutdownAreIdempotentAndReleaseEveryOwnedComponentOnce() {
        RecordingProbe probe = new RecordingProbe(true);
        ManualFixedDelayScheduler scheduler = new ManualFixedDelayScheduler();
        AtomicInteger schedulerCreations = new AtomicInteger();
        Supplier<FixedDelayScheduler> schedulerFactory = () -> {
            schedulerCreations.incrementAndGet();
            return scheduler;
        };
        InternetAvailabilityMonitor monitor = new InternetAvailabilityMonitor(probe, schedulerFactory, Duration.ofSeconds(20L), available -> {});

        monitor.init();
        monitor.init();
        monitor.shutdown();
        monitor.shutdown();
        monitor.init();

        assertEquals(1, schedulerCreations.get());
        assertEquals(1, scheduler.scheduleCalls);
        assertEquals(1, scheduler.task.cancelCalls);
        assertTrue(scheduler.task.mayInterruptIfRunning);
        assertEquals(1, scheduler.shutdownCalls);
        assertEquals(1, probe.closeCalls.get());
    }

    @Test
    void shutdownBeforeInitIsTerminalAndDoesNotCreateAScheduler() {
        RecordingProbe probe = new RecordingProbe(true);
        AtomicInteger schedulerCreations = new AtomicInteger();
        Supplier<FixedDelayScheduler> schedulerFactory = () -> {
            schedulerCreations.incrementAndGet();
            return new ManualFixedDelayScheduler();
        };
        InternetAvailabilityMonitor monitor = new InternetAvailabilityMonitor(probe, schedulerFactory, Duration.ofSeconds(20L), available -> {});

        monitor.shutdown();
        monitor.shutdown();
        monitor.init();

        assertEquals(0, schedulerCreations.get());
        assertEquals(1, probe.closeCalls.get());
        assertEquals(0, probe.attempts.get());
    }

    @Test
    void schedulingFailureClosesTheProbeAndSchedulerAndCannotPartiallyReinitialize() {
        RecordingProbe probe = new RecordingProbe(true);
        ManualFixedDelayScheduler scheduler = new ManualFixedDelayScheduler();
        scheduler.scheduleFailure = new RejectedExecutionException("stopped");
        AtomicInteger schedulerCreations = new AtomicInteger();
        Supplier<FixedDelayScheduler> schedulerFactory = () -> {
            schedulerCreations.incrementAndGet();
            return scheduler;
        };
        InternetAvailabilityMonitor monitor = new InternetAvailabilityMonitor(probe, schedulerFactory, Duration.ofSeconds(20L), available -> {});

        monitor.init();
        monitor.init();
        monitor.shutdown();

        assertEquals(1, schedulerCreations.get());
        assertEquals(1, scheduler.scheduleCalls);
        assertEquals(1, scheduler.shutdownCalls);
        assertEquals(1, probe.closeCalls.get());
        assertEquals(0, probe.attempts.get());
    }

    @Test
    void reentrantShutdownFromSchedulerFactoryCannotLeakTheCreatedScheduler() {
        RecordingProbe probe = new RecordingProbe(true);
        ManualFixedDelayScheduler scheduler = new ManualFixedDelayScheduler();
        AtomicReference<InternetAvailabilityMonitor> monitorReference = new AtomicReference<>();
        Supplier<FixedDelayScheduler> schedulerFactory = () -> {
            monitorReference.get().shutdown();
            return scheduler;
        };
        InternetAvailabilityMonitor monitor = new InternetAvailabilityMonitor(probe, schedulerFactory, Duration.ofSeconds(20L), available -> {});
        monitorReference.set(monitor);

        monitor.init();

        assertEquals(0, scheduler.scheduleCalls);
        assertEquals(1, scheduler.shutdownCalls);
        assertEquals(1, probe.closeCalls.get());
    }

    @Test
    void reentrantShutdownFromSchedulerRegistrationCancelsAndClosesLocalOwnership() {
        RecordingProbe probe = new RecordingProbe(true);
        ManualFixedDelayScheduler scheduler = new ManualFixedDelayScheduler();
        AtomicReference<InternetAvailabilityMonitor> monitorReference = new AtomicReference<>();
        scheduler.onSchedule = () -> monitorReference.get().shutdown();
        InternetAvailabilityMonitor monitor = new InternetAvailabilityMonitor(probe, () -> scheduler, Duration.ofSeconds(20L), available -> {});
        monitorReference.set(monitor);

        monitor.init();

        assertEquals(1, scheduler.scheduleCalls);
        assertEquals(1, scheduler.task.cancelCalls);
        assertTrue(scheduler.task.mayInterruptIfRunning);
        assertEquals(1, scheduler.shutdownCalls);
        assertEquals(1, probe.closeCalls.get());
    }

    @Test
    void synchronousZeroDelayExecutionRunsBeforeSchedulerOwnershipIsPublished() {
        RecordingProbe probe = new RecordingProbe(true);
        ManualFixedDelayScheduler scheduler = new ManualFixedDelayScheduler();
        scheduler.runImmediately = true;
        AtomicBoolean availability = new AtomicBoolean(false);
        InternetAvailabilityMonitor monitor = new InternetAvailabilityMonitor(probe, () -> scheduler, Duration.ofSeconds(20L), availability::set);

        monitor.init();

        assertTrue(availability.get());
        assertEquals(1, probe.attempts.get());
        assertEquals(0, scheduler.task.cancelCalls);
        assertEquals(0, scheduler.shutdownCalls);
        monitor.shutdown();
    }

    @Test
    void inFlightResultCannotPublishAfterShutdown() throws Exception {
        BlockingProbe probe = new BlockingProbe();
        ManualFixedDelayScheduler scheduler = new ManualFixedDelayScheduler();
        AtomicBoolean availability = new AtomicBoolean(false);
        InternetAvailabilityMonitor monitor = new InternetAvailabilityMonitor(probe, () -> scheduler, Duration.ofSeconds(20L), availability::set);
        monitor.init();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> refresh = executor.submit(scheduler::runNext);
            assertTrue(probe.started.await(5L, TimeUnit.SECONDS));

            monitor.shutdown();
            refresh.get(5L, TimeUnit.SECONDS);
        } finally {
            probe.release.countDown();
        }

        assertFalse(availability.get());
        assertEquals(1, probe.closeCalls.get());
        assertEquals(1, scheduler.task.cancelCalls);
        assertEquals(1, scheduler.shutdownCalls);
    }

    private static final class RecordingProbe implements InternetAvailabilityProbe {

        private final Deque<Object> results = new ArrayDeque<>();
        private final AtomicInteger attempts = new AtomicInteger();
        private final AtomicInteger closeCalls = new AtomicInteger();

        private RecordingProbe(Object... results) {
            for (Object result : results) this.results.addLast(result);
        }

        @Override
        public boolean isAvailable() throws Exception {
            this.attempts.incrementAndGet();
            Object result = this.results.removeFirst();
            if (result instanceof Exception exception) throw exception;
            return (boolean) result;
        }

        @Override
        public void close() {
            this.closeCalls.incrementAndGet();
        }
    }

    private static final class BlockingProbe implements InternetAvailabilityProbe {

        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger closeCalls = new AtomicInteger();

        @Override
        public boolean isAvailable() throws InterruptedException {
            this.started.countDown();
            this.release.await();
            return true;
        }

        @Override
        public void close() {
            this.closeCalls.incrementAndGet();
            this.release.countDown();
        }
    }

    private static final class ManualFixedDelayScheduler implements FixedDelayScheduler {

        private final ManualScheduledTask task = new ManualScheduledTask();
        private int scheduleCalls;
        private int shutdownCalls;
        private Runnable command;
        private Duration initialDelay;
        private Duration delay;
        private RuntimeException scheduleFailure;
        private Runnable onSchedule = () -> {};
        private boolean runImmediately;

        @Override
        public ScheduledTask scheduleWithFixedDelay(Runnable task, Duration initialDelay, Duration delay) {
            this.scheduleCalls++;
            if (this.scheduleFailure != null) throw this.scheduleFailure;
            this.command = task;
            this.initialDelay = initialDelay;
            this.delay = delay;
            if (this.runImmediately) task.run();
            this.onSchedule.run();
            return this.task;
        }

        @Override
        public void shutdownNow() {
            this.shutdownCalls++;
        }

        private void runNext() {
            if (this.command == null) throw new IllegalStateException("No task was scheduled");
            if (this.task.cancelled) throw new IllegalStateException("The task was cancelled");
            this.command.run();
        }
    }

    private static final class ManualScheduledTask implements FixedDelayScheduler.ScheduledTask {

        private int cancelCalls;
        private boolean cancelled;
        private boolean mayInterruptIfRunning;

        @Override
        public void cancel(boolean mayInterruptIfRunning) {
            this.cancelCalls++;
            this.cancelled = true;
            this.mayInterruptIfRunning = mayInterruptIfRunning;
        }
    }
}
