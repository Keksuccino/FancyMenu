package de.keksuccino.fancymenu.util.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainThreadTaskExecutor {

    private static final List<Runnable> QUEUED_TASKS_PRE_CLIENT_TICK = Collections.synchronizedList(new ArrayList<>());
    private static final List<Runnable> QUEUED_TASKS_POST_CLIENT_TICK = Collections.synchronizedList(new ArrayList<>());

    public static void executeInMainThread(Runnable task, ExecuteTiming when) {
        if (when == ExecuteTiming.PRE_CLIENT_TICK) {
            QUEUED_TASKS_PRE_CLIENT_TICK.add(task);
        } else {
            QUEUED_TASKS_POST_CLIENT_TICK.add(task);
        }
    }

    public static List<Runnable> getAndClearQueue(ExecuteTiming executeTiming) {
        List<Runnable> queue = (executeTiming == ExecuteTiming.PRE_CLIENT_TICK) ? QUEUED_TASKS_PRE_CLIENT_TICK : QUEUED_TASKS_POST_CLIENT_TICK;
        return drainQueue(queue);
    }

    /**
     * Snapshot and clear must share the queue monitor so a task enqueued between those operations is never erased without being returned for execution.
     */
    static List<Runnable> drainQueue(List<Runnable> queue) {
        synchronized (queue) {
            List<Runnable> tasks = new ArrayList<>(queue);
            queue.clear();
            return tasks;
        }
    }

    public static enum ExecuteTiming {
        PRE_CLIENT_TICK,
        POST_CLIENT_TICK
    }

}
