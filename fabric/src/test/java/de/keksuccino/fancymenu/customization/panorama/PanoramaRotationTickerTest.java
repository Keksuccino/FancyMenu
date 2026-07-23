package de.keksuccino.fancymenu.customization.panorama;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanoramaRotationTickerTest {

    @Test
    void zeroSpeedStaysStationaryAndStopsAtTheInactivityDeadline() {
        Harness harness = new Harness();
        harness.ticker.setSpeed(0.0F);

        harness.ticker.onRender();

        assertTrue(harness.ticker.isActive());
        assertEquals(0, harness.tickCount.get());
        assertEquals(1, harness.scheduler.pendingTaskCount());
        assertEquals(PanoramaRotationTicker.INACTIVITY_TIMEOUT_MILLIS, harness.scheduler.nextDelayMillis());

        harness.scheduler.advanceMillis(PanoramaRotationTicker.INACTIVITY_TIMEOUT_MILLIS - 1L);
        assertTrue(harness.ticker.isActive());
        assertEquals(0, harness.tickCount.get());

        harness.scheduler.advanceMillis(1L);
        assertFalse(harness.ticker.isActive());
        assertFalse(harness.ticker.hasScheduledTask());
        assertEquals(0, harness.scheduler.pendingTaskCount());
        assertEquals(0, harness.tickCount.get());
    }

    @Test
    void zeroToPositiveWakesAnActiveTickerImmediately() {
        Harness harness = new Harness();
        harness.ticker.setSpeed(0.0F);
        harness.ticker.onRender();

        harness.ticker.setSpeed(1.0F);

        assertEquals(0L, harness.scheduler.nextDelayMillis());
        harness.scheduler.runDueTasks();
        assertEquals(1, harness.tickCount.get());
        assertEquals(20L, harness.scheduler.nextDelayMillis());
    }

    @Test
    void positiveToZeroPausesWithoutAResidualTick() {
        Harness harness = new Harness();
        harness.ticker.onRender();
        harness.scheduler.runDueTasks();
        harness.scheduler.advanceMillis(10L);

        harness.ticker.setSpeed(0.0F);
        harness.scheduler.advanceMillis(PanoramaRotationTicker.INACTIVITY_TIMEOUT_MILLIS - 11L);

        assertTrue(harness.ticker.isActive());
        assertEquals(1, harness.tickCount.get());
        harness.scheduler.advanceMillis(1L);
        assertFalse(harness.ticker.isActive());
        assertEquals(1, harness.tickCount.get());
    }

    @Test
    void ordinarySpeedsPreserveTheExistingTickIntervals() {
        Harness harness = new Harness();
        harness.ticker.onRender();
        harness.scheduler.runDueTasks();
        assertEquals(1, harness.tickCount.get());

        harness.scheduler.advanceMillis(19L);
        assertEquals(1, harness.tickCount.get());
        harness.scheduler.advanceMillis(1L);
        assertEquals(2, harness.tickCount.get());

        harness.ticker.setSpeed(0.5F);
        harness.scheduler.advanceMillis(39L);
        assertEquals(2, harness.tickCount.get());
        harness.scheduler.advanceMillis(1L);
        assertEquals(3, harness.tickCount.get());

        harness.ticker.setSpeed(10.0F);
        harness.scheduler.advanceMillis(1L);
        assertEquals(3, harness.tickCount.get());
        harness.scheduler.advanceMillis(1L);
        assertEquals(4, harness.tickCount.get());
    }

    @Test
    void rapidSpeedUpdatesKeepOnlyTheFinalSchedule() {
        Harness harness = new Harness();
        harness.ticker.setSpeed(0.0F);
        harness.ticker.onRender();

        harness.ticker.setSpeed(1.0F);
        harness.ticker.setSpeed(0.5F);
        harness.ticker.setSpeed(10.0F);

        assertEquals(1, harness.scheduler.pendingTaskCount());
        assertEquals(2L, harness.scheduler.nextDelayMillis());
        harness.scheduler.advanceMillis(2L);
        assertEquals(1, harness.tickCount.get());

        harness.ticker.setSpeed(0.0F);
        harness.ticker.setSpeed(1.0F);
        harness.ticker.setSpeed(0.0F);
        harness.scheduler.runDueTasks();
        assertEquals(1, harness.scheduler.pendingTaskCount());
        assertEquals(1, harness.tickCount.get());
    }

    @Test
    void staleCancelledCallbackCannotTickOrReplaceTheCurrentSchedule() {
        Harness harness = new Harness();
        harness.ticker.setSpeed(0.0F);
        harness.ticker.onRender();

        harness.ticker.setSpeed(1.0F);
        harness.scheduler.runCancelledTasks();

        assertEquals(0, harness.tickCount.get());
        assertEquals(1, harness.scheduler.pendingTaskCount());
        assertEquals(0L, harness.scheduler.nextDelayMillis());
        harness.scheduler.runDueTasks();
        assertEquals(1, harness.tickCount.get());
        assertEquals(20L, harness.scheduler.nextDelayMillis());
    }

    @Test
    void repeatedStartStopCyclesNeverCreateDuplicateTasks() {
        Harness harness = new Harness();
        harness.ticker.setSpeed(0.0F);
        harness.ticker.onRender();
        harness.ticker.onRender();
        harness.ticker.onRender();
        assertEquals(1, harness.scheduler.pendingTaskCount());

        harness.scheduler.advanceMillis(PanoramaRotationTicker.INACTIVITY_TIMEOUT_MILLIS);
        assertFalse(harness.ticker.isActive());
        harness.ticker.setSpeed(1.0F);
        assertEquals(0, harness.scheduler.pendingTaskCount());

        harness.ticker.onRender();
        harness.ticker.onRender();
        assertEquals(1, harness.scheduler.pendingTaskCount());
        harness.scheduler.runDueTasks();
        assertEquals(1, harness.tickCount.get());

        harness.ticker.setSpeed(0.0F);
        harness.scheduler.advanceMillis(PanoramaRotationTicker.INACTIVITY_TIMEOUT_MILLIS);
        assertFalse(harness.ticker.isActive());
        harness.ticker.setSpeed(1.0F);
        harness.ticker.onRender();
        harness.scheduler.runDueTasks();
        assertEquals(2, harness.tickCount.get());
        assertEquals(1, harness.scheduler.pendingTaskCount());
    }

    @Test
    void closeWhileWaitingIsImmediateAndIdempotent() {
        Harness harness = new Harness();
        harness.ticker.onRender();
        harness.scheduler.runDueTasks();
        assertEquals(1, harness.tickCount.get());

        harness.ticker.close();
        harness.ticker.close();
        harness.ticker.setSpeed(10.0F);
        harness.ticker.onRender();
        harness.scheduler.advanceMillis(100L);

        assertFalse(harness.ticker.isActive());
        assertFalse(harness.ticker.hasScheduledTask());
        assertEquals(0, harness.scheduler.pendingTaskCount());
        assertEquals(1, harness.tickCount.get());
    }

    @Test
    void invalidAndBoundarySpeedsHaveExplicitSafeSemantics() {
        assertEquals(0.0F, PanoramaRotationTicker.normalizeSpeed(-1.0F));
        assertEquals(0.0F, PanoramaRotationTicker.normalizeSpeed(Float.NEGATIVE_INFINITY));
        assertEquals(Float.floatToIntBits(0.0F), Float.floatToIntBits(PanoramaRotationTicker.normalizeSpeed(-0.0F)));
        assertEquals(0.0F, PanoramaRotationTicker.normalizeSpeed(Float.NaN));
        assertEquals(Float.MIN_VALUE, PanoramaRotationTicker.normalizeSpeed(Float.MIN_VALUE));
        assertEquals(Float.MAX_VALUE, PanoramaRotationTicker.normalizeSpeed(Float.MAX_VALUE));
        assertEquals(Float.POSITIVE_INFINITY, PanoramaRotationTicker.normalizeSpeed(Float.POSITIVE_INFINITY));

        assertThrows(IllegalArgumentException.class, () -> PanoramaRotationTicker.tickDelayMillis(0.0F));
        assertThrows(IllegalArgumentException.class, () -> PanoramaRotationTicker.tickDelayMillis(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> PanoramaRotationTicker.tickDelayMillis(Float.NEGATIVE_INFINITY));
        assertEquals(Integer.MAX_VALUE, PanoramaRotationTicker.tickDelayMillis(Float.MIN_VALUE));
        assertEquals(40L, PanoramaRotationTicker.tickDelayMillis(0.5F));
        assertEquals(20L, PanoramaRotationTicker.tickDelayMillis(1.0F));
        assertEquals(10L, PanoramaRotationTicker.tickDelayMillis(2.0F));
        assertEquals(6L, PanoramaRotationTicker.tickDelayMillis(3.0F));
        assertEquals(2L, PanoramaRotationTicker.tickDelayMillis(10.0F));
        assertEquals(2L, PanoramaRotationTicker.tickDelayMillis(Float.MAX_VALUE));
        assertEquals(2L, PanoramaRotationTicker.tickDelayMillis(Float.POSITIVE_INFINITY));
    }

    @Test
    void tinyPositiveSpeedKeepsItsLongTickDeadlineButStillStopsPromptly() {
        Harness harness = new Harness();
        harness.ticker.setSpeed(Float.MIN_VALUE);
        harness.ticker.onRender();
        harness.scheduler.runDueTasks();
        assertEquals(1, harness.tickCount.get());
        assertEquals(PanoramaRotationTicker.INACTIVITY_TIMEOUT_MILLIS, harness.scheduler.nextDelayMillis());

        harness.scheduler.advanceMillis(4_000L);
        harness.ticker.onRender();
        harness.scheduler.advanceMillis(1_000L);

        assertTrue(harness.ticker.isActive());
        assertEquals(1, harness.tickCount.get());
        assertEquals(4_000L, harness.scheduler.nextDelayMillis());
        harness.scheduler.advanceMillis(4_000L);
        assertFalse(harness.ticker.isActive());
        assertEquals(1, harness.tickCount.get());
    }

    private static final class Harness {

        private final ManualClock clock = new ManualClock();
        private final ManualScheduler scheduler = new ManualScheduler(this.clock);
        private final AtomicInteger tickCount = new AtomicInteger();
        private final PanoramaRotationTicker ticker = new PanoramaRotationTicker(this.clock::nanoTime, this.scheduler, this.tickCount::incrementAndGet);

    }

    private static final class ManualClock {

        private long nowNanos;

        private long nanoTime() {
            return this.nowNanos;
        }

        private void advanceMillis(long millis) {
            this.nowNanos += TimeUnit.MILLISECONDS.toNanos(millis);
        }

    }

    private static final class ManualScheduler implements PanoramaRotationTicker.Scheduler {

        private final ManualClock clock;
        private final PriorityQueue<ScheduledEntry> entries = new PriorityQueue<>(Comparator.comparingLong(ScheduledEntry::deadlineNanos).thenComparingLong(ScheduledEntry::sequence));
        private long sequence;

        private ManualScheduler(ManualClock clock) {
            this.clock = clock;
        }

        @Override
        public PanoramaRotationTicker.ScheduledTask schedule(Runnable task, long delayNanos) {
            ScheduledEntry entry = new ScheduledEntry(this.clock.nanoTime() + delayNanos, this.sequence++, task);
            this.entries.add(entry);
            return () -> entry.cancelled = true;
        }

        private void advanceMillis(long millis) {
            this.clock.advanceMillis(millis);
            this.runDueTasks();
        }

        private void runDueTasks() {
            ScheduledEntry entry;
            while ((entry = this.nextActiveEntry()) != null && entry.deadlineNanos <= this.clock.nanoTime()) {
                this.entries.remove();
                entry.completed = true;
                entry.task.run();
            }
        }

        private void runCancelledTasks() {
            for (ScheduledEntry entry : this.entries.toArray(ScheduledEntry[]::new)) {
                if (entry.cancelled && !entry.completed) {
                    entry.completed = true;
                    entry.task.run();
                }
            }
        }

        private int pendingTaskCount() {
            return (int)this.entries.stream().filter(entry -> !entry.cancelled && !entry.completed).count();
        }

        private long nextDelayMillis() {
            ScheduledEntry entry = this.nextActiveEntry();
            if (entry == null) return -1L;
            return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, entry.deadlineNanos - this.clock.nanoTime()));
        }

        private ScheduledEntry nextActiveEntry() {
            while (!this.entries.isEmpty() && (this.entries.peek().cancelled || this.entries.peek().completed)) this.entries.remove();
            return this.entries.peek();
        }

    }

    private static final class ScheduledEntry {

        private final long deadlineNanos;
        private final long sequence;
        private final Runnable task;
        private boolean cancelled;
        private boolean completed;

        private ScheduledEntry(long deadlineNanos, long sequence, Runnable task) {
            this.deadlineNanos = deadlineNanos;
            this.sequence = sequence;
            this.task = task;
        }

        private long deadlineNanos() {
            return this.deadlineNanos;
        }

        private long sequence() {
            return this.sequence;
        }

    }

}
