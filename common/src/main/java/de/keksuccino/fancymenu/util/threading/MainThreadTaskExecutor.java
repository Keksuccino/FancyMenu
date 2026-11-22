package de.keksuccino.fancymenu.util.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainThreadTaskExecutor {

    private static final List<Runnable> QUEUED_TASKS_PRE_CLIENT_TICK = Collections.synchronizedList(new ArrayList<>());
    private static final List<Runnable> QUEUED_TASKS_POST_CLIENT_TICK = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean shuttingDown;

    public static void executeInMainThread(Runnable task, ExecuteTiming when) {
        List<Runnable> queue = when == ExecuteTiming.PRE_CLIENT_TICK ? QUEUED_TASKS_PRE_CLIENT_TICK : QUEUED_TASKS_POST_CLIENT_TICK;
        synchronized (queue) {
            if (!shuttingDown) queue.add(task);
        }
    }

    public static List<Runnable> getAndClearQueue(ExecuteTiming executeTiming) {
        List<Runnable> queue = executeTiming == ExecuteTiming.PRE_CLIENT_TICK ? QUEUED_TASKS_PRE_CLIENT_TICK : QUEUED_TASKS_POST_CLIENT_TICK;
        synchronized (queue) {
            List<Runnable> tasks = new ArrayList<>(queue);
            queue.clear();
            return tasks;
        }
    }

    public static void shutdown() {
        shuttingDown = true;
        synchronized (QUEUED_TASKS_PRE_CLIENT_TICK) {
            QUEUED_TASKS_PRE_CLIENT_TICK.clear();
        }
        synchronized (QUEUED_TASKS_POST_CLIENT_TICK) {
            QUEUED_TASKS_POST_CLIENT_TICK.clear();
        }
    }

    public static enum ExecuteTiming {
        PRE_CLIENT_TICK,
        POST_CLIENT_TICK
    }

}
