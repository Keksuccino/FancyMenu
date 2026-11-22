package de.keksuccino.fancymenu.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskExecutorScheduleTest {

    /**
     * The executor starts tasks without delay right away, so some of them start before scheduling them returned, especially while several threads
     * schedule at once. These tasks have to run like all others.
     */
    private static final int SCHEDULING_THREADS = 4;
    private static final int TASKS_PER_THREAD = 25_000;

    @Test
    void everyTaskWithoutDelayRunsOnceWithItsFuture() throws InterruptedException {
        int tasks = SCHEDULING_THREADS * TASKS_PER_THREAD;
        CountDownLatch ran = new CountDownLatch(tasks);
        AtomicInteger runs = new AtomicInteger();
        AtomicInteger runsWithoutFuture = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < SCHEDULING_THREADS; t++) {
            Thread thread = new Thread(() -> {
                for (int i = 0; i < TASKS_PER_THREAD; i++) {
                    TaskExecutor.schedule(future -> {
                        //noinspection ConstantValue
                        if (future == null) runsWithoutFuture.incrementAndGet();
                        runs.incrementAndGet();
                        ran.countDown();
                    }, 0L, TimeUnit.MILLISECONDS, false);
                }
            }, "TaskExecutorScheduleTest-" + t);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        // Only a lost task makes this wait the whole time
        assertTrue(ran.await(30L, TimeUnit.SECONDS), () -> (tasks - ran.getCount()) + " of " + tasks + " tasks ran");
        assertEquals(0, runsWithoutFuture.get());
        assertEquals(tasks, runs.get());
    }

}
