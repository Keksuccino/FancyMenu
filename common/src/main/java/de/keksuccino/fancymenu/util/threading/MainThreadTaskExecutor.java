package de.keksuccino.fancymenu.util.threading;

import java.util.ArrayList;
import java.util.List;

public class MainThreadTaskExecutor {

    private static final TaskQueue QUEUED_TASKS_PRE_CLIENT_TICK = new TaskQueue();
    private static final TaskQueue QUEUED_TASKS_POST_CLIENT_TICK = new TaskQueue();

    public static void executeInMainThread(Runnable task, ExecuteTiming when) {
        if (when == ExecuteTiming.PRE_CLIENT_TICK) {
            QUEUED_TASKS_PRE_CLIENT_TICK.add(task);
        } else {
            QUEUED_TASKS_POST_CLIENT_TICK.add(task);
        }
    }

    public static List<Runnable> getAndClearQueue(ExecuteTiming executeTiming) {
        return ((executeTiming == ExecuteTiming.PRE_CLIENT_TICK) ? QUEUED_TASKS_PRE_CLIENT_TICK : QUEUED_TASKS_POST_CLIENT_TICK).drain();
    }

    static final class TaskQueue {

        private final List<Runnable> tasks = new ArrayList<>();

        synchronized void add(Runnable task) {
            this.tasks.add(task);
        }

        synchronized List<Runnable> drain() {
            // Snapshot and clear must share one monitor. A producer inserted between separate operations would be
            // cleared without ever reaching Minecraft's tick loop, potentially stranding lifecycle recovery forever.
            List<Runnable> drainedTasks = new ArrayList<>(this.tasks);
            this.tasks.clear();
            return drainedTasks;
        }
    }

    public static enum ExecuteTiming {
        PRE_CLIENT_TICK,
        POST_CLIENT_TICK
    }

}
