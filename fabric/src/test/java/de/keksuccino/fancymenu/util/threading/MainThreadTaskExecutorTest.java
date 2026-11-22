package de.keksuccino.fancymenu.util.threading;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MainThreadTaskExecutorTest {

    private static final MainThreadTaskExecutor.ExecuteTiming PRE = MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK;
    private static final MainThreadTaskExecutor.ExecuteTiming POST = MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK;
    private static final int PRODUCER_COUNT = 8;
    private static final int TASKS_PER_PRODUCER = 25_000;

    @BeforeEach
    void setUp() {
        clearQueues();
    }

    @AfterEach
    void tearDown() {
        clearQueues();
    }

    @Test
    void drainReturnsTheQueuedTasksInOrderAndEmptiesOnlyThatQueue() {
        Runnable firstPreTask = () -> {};
        Runnable secondPreTask = () -> {};
        Runnable postTask = () -> {};
        MainThreadTaskExecutor.executeInMainThread(firstPreTask, PRE);
        MainThreadTaskExecutor.executeInMainThread(postTask, POST);
        MainThreadTaskExecutor.executeInMainThread(secondPreTask, PRE);

        assertEquals(List.of(firstPreTask, secondPreTask), MainThreadTaskExecutor.getAndClearQueue(PRE));
        assertTrue(MainThreadTaskExecutor.getAndClearQueue(PRE).isEmpty());
        assertEquals(List.of(postTask), MainThreadTaskExecutor.getAndClearQueue(POST));
    }

    @Test
    void tasksQueuedByARunningTaskWaitForTheNextDrain() {
        Runnable followUpTask = () -> {};
        MainThreadTaskExecutor.executeInMainThread(() -> MainThreadTaskExecutor.executeInMainThread(followUpTask, PRE), PRE);

        List<Runnable> firstDrain = MainThreadTaskExecutor.getAndClearQueue(PRE);
        firstDrain.forEach(Runnable::run);

        assertEquals(1, firstDrain.size());
        assertEquals(List.of(followUpTask), MainThreadTaskExecutor.getAndClearQueue(PRE));
    }

    @Test
    @Timeout(30)
    void tasksQueuedFromManyThreadsWhileTheMainThreadDrainsRunExactlyOnce() throws InterruptedException {
        // Only the test thread runs the tasks, so the counters need no synchronization
        int[] runCounts = new int[PRODUCER_COUNT * TASKS_PER_PRODUCER];
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch producersDone = new CountDownLatch(PRODUCER_COUNT);
        for (int producerIndex = 0; producerIndex < PRODUCER_COUNT; producerIndex++) {
            int firstTaskIndex = producerIndex * TASKS_PER_PRODUCER;
            Thread producer = new Thread(() -> {
                try {
                    start.await();
                    for (int taskIndex = firstTaskIndex; taskIndex < (firstTaskIndex + TASKS_PER_PRODUCER); taskIndex++) {
                        // Alternating timings lets every producer race the drains of both queues
                        MainThreadTaskExecutor.executeInMainThread(new CountedTask(runCounts, taskIndex), ((taskIndex & 1) == 0) ? PRE : POST);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    producersDone.countDown();
                }
            }, "MainThreadTaskExecutorTest producer " + producerIndex);
            producer.setDaemon(true);
            producer.start();
        }

        start.countDown();
        // The test thread acts as the client thread, which drains both queues every tick while the producers keep adding tasks
        while (!producersDone.await(0L, TimeUnit.NANOSECONDS)) {
            runQueuedCountedTasks();
        }
        runQueuedCountedTasks();

        int lostTasks = 0;
        int repeatedTasks = 0;
        for (int runCount : runCounts) {
            if (runCount == 0) lostTasks++;
            if (runCount > 1) repeatedTasks++;
        }
        assertEquals(0, lostTasks, "Tasks that never ran");
        assertEquals(0, repeatedTasks, "Tasks that ran more than once");
    }

    @Test
    void shutdownDropsQueuedTasksAndIgnoresNewOnes() throws Exception {
        // Shutting down is permanent, so it happens on an own copy of the executor that the other tests never see
        Class<?> executorClass = Class.forName(MainThreadTaskExecutor.class.getName(), true, new IsolatedExecutorClassLoader());
        assertNotSame(MainThreadTaskExecutor.class, executorClass);
        Class<?> timingClass = Class.forName(MainThreadTaskExecutor.ExecuteTiming.class.getName(), true, executorClass.getClassLoader());
        Method executeInMainThread = executorClass.getMethod("executeInMainThread", Runnable.class, timingClass);
        Method getAndClearQueue = executorClass.getMethod("getAndClearQueue", timingClass);
        Object[] timings = timingClass.getEnumConstants();
        for (Object timing : timings) {
            Runnable task = () -> {};
            executeInMainThread.invoke(null, task, timing);
            assertEquals(List.of(task), getAndClearQueue.invoke(null, timing));
            executeInMainThread.invoke(null, task, timing);
        }

        executorClass.getMethod("shutdown").invoke(null);

        for (Object timing : timings) {
            assertEquals(List.of(), getAndClearQueue.invoke(null, timing));
            executeInMainThread.invoke(null, (Runnable) () -> {}, timing);
            assertEquals(List.of(), getAndClearQueue.invoke(null, timing));
        }
    }

    private static void clearQueues() {
        MainThreadTaskExecutor.getAndClearQueue(PRE);
        MainThreadTaskExecutor.getAndClearQueue(POST);
    }

    private static void runQueuedCountedTasks() {
        runCountedTasks(MainThreadTaskExecutor.getAndClearQueue(PRE));
        runCountedTasks(MainThreadTaskExecutor.getAndClearQueue(POST));
    }

    private static void runCountedTasks(List<Runnable> tasks) {
        for (Runnable task : tasks) {
            // Tasks that other test classes left behind are skipped
            if (task instanceof CountedTask) task.run();
        }
    }

    private static final class CountedTask implements Runnable {

        private final int[] runCounts;
        private final int index;

        private CountedTask(int[] runCounts, int index) {
            this.runCounts = runCounts;
            this.index = index;
        }

        @Override
        public void run() {
            this.runCounts[this.index]++;
        }

    }

    /** Defines its own copy of the executor classes, so their static state is separate from the copy that the other tests use. */
    private static final class IsolatedExecutorClassLoader extends ClassLoader {

        private static final String EXECUTOR_CLASS_NAME = MainThreadTaskExecutor.class.getName();

        private IsolatedExecutorClassLoader() {
            super(MainThreadTaskExecutorTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(EXECUTOR_CLASS_NAME) && !name.startsWith(EXECUTOR_CLASS_NAME + "$")) return super.loadClass(name, resolve);
            synchronized (this.getClassLoadingLock(name)) {
                Class<?> loadedClass = this.findLoadedClass(name);
                if (loadedClass == null) loadedClass = this.defineExecutorClass(name);
                if (resolve) this.resolveClass(loadedClass);
                return loadedClass;
            }
        }

        private Class<?> defineExecutorClass(String name) throws ClassNotFoundException {
            try (InputStream input = this.getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                if (input == null) throw new ClassNotFoundException(name);
                byte[] bytes = input.readAllBytes();
                return this.defineClass(name, bytes, 0, bytes.length);
            } catch (IOException ex) {
                throw new ClassNotFoundException(name, ex);
            }
        }

    }

}
