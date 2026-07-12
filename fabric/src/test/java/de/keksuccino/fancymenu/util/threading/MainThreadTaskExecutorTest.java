package de.keksuccino.fancymenu.util.threading;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainThreadTaskExecutorTest {

    @Test
    void drainMovesEveryQueuedTaskExactlyOnce() {
        MainThreadTaskExecutor.TaskQueue queue = new MainThreadTaskExecutor.TaskQueue();
        AtomicInteger executions = new AtomicInteger();
        queue.add(executions::incrementAndGet);
        queue.add(executions::incrementAndGet);

        queue.drain().forEach(Runnable::run);
        queue.drain().forEach(Runnable::run);

        assertEquals(2, executions.get());
    }

    @Test
    void concurrentProducersAndDrainsCannotLoseTasksAtTheDrainBoundary() throws Exception {
        int producerCount = 4;
        int tasksPerProducer = 5000;
        MainThreadTaskExecutor.TaskQueue queue = new MainThreadTaskExecutor.TaskQueue();
        AtomicInteger executions = new AtomicInteger();
        AtomicBoolean producersDone = new AtomicBoolean();
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> producers = new ArrayList<>();

        for (int producerIndex = 0; producerIndex < producerCount; producerIndex++) {
            Thread producer = daemonThread(() -> {
                await(start);
                for (int taskIndex = 0; taskIndex < tasksPerProducer; taskIndex++) {
                    queue.add(executions::incrementAndGet);
                }
            });
            producers.add(producer);
            producer.start();
        }

        Thread consumer = daemonThread(() -> {
            await(start);
            while (!producersDone.get()) {
                queue.drain().forEach(Runnable::run);
            }
            queue.drain().forEach(Runnable::run);
        });
        consumer.start();
        start.countDown();

        for (Thread producer : producers) {
            producer.join(5000L);
            assertTrue(!producer.isAlive());
        }
        producersDone.set(true);
        consumer.join(5000L);
        assertTrue(!consumer.isAlive());

        assertEquals(producerCount * tasksPerProducer, executions.get());
        assertTrue(queue.drain().isEmpty());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static Thread daemonThread(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        return thread;
    }
}
