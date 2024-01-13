
package de.keksuccino.fancymenu.thread;

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
        List<Runnable> l = new ArrayList<>((executeTiming == ExecuteTiming.PRE_CLIENT_TICK) ? QUEUED_TASKS_PRE_CLIENT_TICK : QUEUED_TASKS_POST_CLIENT_TICK);
        if (executeTiming == ExecuteTiming.PRE_CLIENT_TICK) {
            QUEUED_TASKS_PRE_CLIENT_TICK.clear();
        } else {
            QUEUED_TASKS_POST_CLIENT_TICK.clear();
        }
        return l;
    }

    public static enum ExecuteTiming {
        PRE_CLIENT_TICK,
        POST_CLIENT_TICK
    }

}
