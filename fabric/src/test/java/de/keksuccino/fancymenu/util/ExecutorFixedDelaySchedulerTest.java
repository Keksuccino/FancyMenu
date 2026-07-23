package de.keksuccino.fancymenu.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorFixedDelaySchedulerTest {

    @Test
    void delegatesToExecutorFixedDelaySchedulingAndForwardsCancellationAndShutdown() {
        RecordingScheduledExecutor executor = new RecordingScheduledExecutor();
        ExecutorFixedDelayScheduler scheduler = new ExecutorFixedDelayScheduler(executor);
        Runnable command = () -> {};

        FixedDelayScheduler.ScheduledTask task = scheduler.scheduleWithFixedDelay(command, Duration.ZERO, Duration.ofSeconds(20L));
        task.cancel(true);
        scheduler.shutdownNow();

        assertSame(command, executor.command);
        assertEquals(0L, executor.initialDelayNanos);
        assertEquals(Duration.ofSeconds(20L).toNanos(), executor.delayNanos);
        assertEquals(1, executor.scheduleCalls);
        assertEquals(1, executor.future.cancelCalls);
        assertTrue(executor.future.mayInterruptIfRunning);
        assertEquals(1, executor.shutdownCalls);
    }

    private static final class RecordingScheduledExecutor extends ScheduledThreadPoolExecutor {

        private final RecordingScheduledFuture future = new RecordingScheduledFuture();
        private int scheduleCalls;
        private int shutdownCalls;
        private Runnable command;
        private long initialDelayNanos;
        private long delayNanos;

        private RecordingScheduledExecutor() {
            super(1);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            this.scheduleCalls++;
            this.command = command;
            this.initialDelayNanos = unit.toNanos(initialDelay);
            this.delayNanos = unit.toNanos(delay);
            return this.future;
        }

        @Override
        public List<Runnable> shutdownNow() {
            this.shutdownCalls++;
            super.shutdownNow();
            return List.of();
        }
    }

    private static final class RecordingScheduledFuture implements ScheduledFuture<Object> {

        private int cancelCalls;
        private boolean cancelled;
        private boolean mayInterruptIfRunning;

        @Override
        public long getDelay(TimeUnit unit) {
            return 0L;
        }

        @Override
        public int compareTo(Delayed other) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            this.cancelCalls++;
            this.cancelled = true;
            this.mayInterruptIfRunning = mayInterruptIfRunning;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return this.cancelled;
        }

        @Override
        public boolean isDone() {
            return this.cancelled;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }
}
