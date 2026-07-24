package de.keksuccino.fancymenu.testing;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ManualTaskQueue {

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public void add(@NotNull Runnable task) {
        this.tasks.add(Objects.requireNonNull(task, "task"));
    }

    @NotNull
    public Runnable removeNext() {
        Runnable task = this.tasks.poll();
        if (task == null) throw new AssertionError("Expected a queued task");
        return task;
    }

    public void runNext() {
        this.removeNext().run();
    }

    public int size() {
        return this.tasks.size();
    }

}
