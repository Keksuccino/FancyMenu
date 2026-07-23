package de.keksuccino.fancymenu.customization.remote;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Allows at most one queued or running task for work that can be drained in batches. Callers must release the gate
 * only after the task has finished examining the shared work queues, then reacquire it if a final check finds work.
 */
final class CoalescingTaskGate {

    private final AtomicBoolean acquired = new AtomicBoolean();

    boolean tryAcquire() {
        return this.acquired.compareAndSet(false, true);
    }

    void release() {
        if (!this.acquired.compareAndSet(true, false)) {
            throw new IllegalStateException("Task gate was not acquired");
        }
    }

    boolean isAcquired() {
        return this.acquired.get();
    }
}
