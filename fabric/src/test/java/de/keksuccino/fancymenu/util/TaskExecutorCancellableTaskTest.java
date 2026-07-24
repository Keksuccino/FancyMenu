package de.keksuccino.fancymenu.util;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskExecutorCancellableTaskTest {

    @Test
    void publicHandleCancelsBeforeSchedulerDispatch() {
        AtomicInteger executions = new AtomicInteger();
        TaskExecutor.CancellableTask task = TaskExecutor.scheduleCancellable(executions::incrementAndGet, 1L, TimeUnit.DAYS, false);

        task.cancel();

        assertTrue(task.isCancelled());
        assertEquals(0, executions.get());
    }

    @Test
    void cancellationAfterDispatchStillGuardsQueuedMainThreadCallback() {
        AtomicInteger executions = new AtomicInteger();
        TaskExecutor.CancellableOneShotTask task = new TaskExecutor.CancellableOneShotTask(executions::incrementAndGet, true);

        task.dispatch();
        List<Runnable> queuedTasks = MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        task.cancel();
        queuedTasks.forEach(Runnable::run);

        assertFalse(queuedTasks.isEmpty());
        assertTrue(task.isCancelled());
        assertEquals(0, executions.get());
    }
}
