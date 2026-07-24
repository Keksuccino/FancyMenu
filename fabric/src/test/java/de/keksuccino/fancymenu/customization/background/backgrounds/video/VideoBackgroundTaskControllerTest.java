package de.keksuccino.fancymenu.customization.background.backgrounds.video;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoBackgroundTaskControllerTest {

    @Test
    void constructionIsLazyAndStartRegistersExactlyTwoTasksOnce() {
        ManualScheduler scheduler = new ManualScheduler();
        AtomicInteger watchdogRuns = new AtomicInteger();
        AtomicInteger tickerRuns = new AtomicInteger();
        VideoBackgroundTaskController controller = new VideoBackgroundTaskController(scheduler, watchdogRuns::incrementAndGet, 100L, tickerRuns::incrementAndGet, 900L);

        assertEquals(0, scheduler.scheduleCalls);
        assertFalse(controller.isRunning());

        assertTrue(controller.start());
        assertTrue(controller.start());
        assertEquals(2, scheduler.scheduleCalls);
        assertEquals(0L, scheduler.tasks.get(0).initialDelayMillis);
        assertEquals(100L, scheduler.tasks.get(0).periodMillis);
        assertEquals(900L, scheduler.tasks.get(1).periodMillis);
        scheduler.tasks.get(0).runEvenIfCancelled();
        scheduler.tasks.get(1).runEvenIfCancelled();
        assertEquals(1, watchdogRuns.get());
        assertEquals(1, tickerRuns.get());
    }

    @Test
    void stopCancelsBothTasksIdempotentlyAndAllowsRestart() {
        ManualScheduler scheduler = new ManualScheduler();
        VideoBackgroundTaskController controller = new VideoBackgroundTaskController(scheduler, () -> {}, 100L, () -> {}, 900L);

        assertTrue(controller.start());
        controller.stop();
        controller.stop();

        assertFalse(controller.isRunning());
        assertEquals(1, scheduler.tasks.get(0).cancelCalls);
        assertEquals(1, scheduler.tasks.get(1).cancelCalls);
        assertTrue(controller.start());
        assertTrue(controller.isRunning());
        assertEquals(4, scheduler.scheduleCalls);
    }

    @Test
    void closeIsTerminalAndCancelsEveryOwnedTaskOnce() {
        ManualScheduler scheduler = new ManualScheduler();
        VideoBackgroundTaskController controller = new VideoBackgroundTaskController(scheduler, () -> {}, 100L, () -> {}, 900L);

        assertTrue(controller.start());
        controller.close();
        controller.close();

        assertTrue(controller.isClosed());
        assertFalse(controller.isRunning());
        assertEquals(1, scheduler.tasks.get(0).cancelCalls);
        assertEquals(1, scheduler.tasks.get(1).cancelCalls);
        assertFalse(controller.start());
        assertEquals(2, scheduler.scheduleCalls);
    }

    @Test
    void secondRegistrationFailureCancelsTheFirstAndMakesTheControllerTerminal() {
        ManualScheduler scheduler = new ManualScheduler();
        scheduler.failureCall = 2;
        VideoBackgroundTaskController controller = new VideoBackgroundTaskController(scheduler, () -> {}, 100L, () -> {}, 900L);

        assertFalse(controller.start());

        assertEquals(2, scheduler.scheduleCalls);
        assertEquals(1, scheduler.tasks.size());
        assertEquals(1, scheduler.tasks.get(0).cancelCalls);
        assertFalse(controller.isRunning());
        assertTrue(controller.isClosed());
        assertFalse(controller.start());
        assertEquals(2, scheduler.scheduleCalls);
    }

    @Test
    void synchronousSelfStopCancelsTheJustCreatedHandleAndCanRestartLater() {
        ManualScheduler scheduler = new ManualScheduler();
        scheduler.runSynchronouslyOnCall = 1;
        AtomicReference<VideoBackgroundTaskController> controllerReference = new AtomicReference<>();
        VideoBackgroundTaskController controller = new VideoBackgroundTaskController(scheduler, () -> controllerReference.get().stop(), 100L, () -> {}, 900L);
        controllerReference.set(controller);

        assertFalse(controller.start());
        assertFalse(controller.isRunning());
        assertFalse(controller.isClosed());
        assertEquals(1, scheduler.scheduleCalls);
        assertEquals(1, scheduler.tasks.get(0).cancelCalls);

        scheduler.runSynchronouslyOnCall = -1;
        assertTrue(controller.start());
        assertTrue(controller.isRunning());
        assertEquals(3, scheduler.scheduleCalls);
    }

    @Test
    void callbacksFromACancelledGenerationCannotTouchRestartedState() {
        ManualScheduler scheduler = new ManualScheduler();
        AtomicInteger watchdogRuns = new AtomicInteger();
        AtomicInteger tickerRuns = new AtomicInteger();
        VideoBackgroundTaskController controller = new VideoBackgroundTaskController(scheduler, watchdogRuns::incrementAndGet, 100L, tickerRuns::incrementAndGet, 900L);

        assertTrue(controller.start());
        ManualTask staleWatchdog = scheduler.tasks.get(0);
        ManualTask staleTicker = scheduler.tasks.get(1);
        controller.stop();
        assertTrue(controller.start());

        staleWatchdog.runEvenIfCancelled();
        staleTicker.runEvenIfCancelled();
        scheduler.tasks.get(2).runEvenIfCancelled();
        scheduler.tasks.get(3).runEvenIfCancelled();
        assertEquals(1, watchdogRuns.get());
        assertEquals(1, tickerRuns.get());
    }

    private static final class ManualScheduler implements VideoBackgroundTaskController.FixedRateScheduler {

        private final List<ManualTask> tasks = new ArrayList<>();
        private int scheduleCalls;
        private int failureCall = -1;
        private int runSynchronouslyOnCall = -1;

        @Override
        public VideoBackgroundTaskController.ScheduledTask scheduleAtFixedRate(Runnable task, long initialDelayMillis, long periodMillis) {
            this.scheduleCalls++;
            if (this.scheduleCalls == this.failureCall) throw new RejectedExecutionException("scheduler stopped");
            ManualTask scheduledTask = new ManualTask(task, initialDelayMillis, periodMillis);
            this.tasks.add(scheduledTask);
            if (this.scheduleCalls == this.runSynchronouslyOnCall) scheduledTask.runEvenIfCancelled();
            return scheduledTask;
        }
    }

    private static final class ManualTask implements VideoBackgroundTaskController.ScheduledTask {

        private final Runnable command;
        private final long initialDelayMillis;
        private final long periodMillis;
        private int cancelCalls;

        private ManualTask(Runnable command, long initialDelayMillis, long periodMillis) {
            this.command = command;
            this.initialDelayMillis = initialDelayMillis;
            this.periodMillis = periodMillis;
        }

        @Override
        public void cancel() {
            this.cancelCalls++;
        }

        private void runEvenIfCancelled() {
            this.command.run();
        }
    }
}
