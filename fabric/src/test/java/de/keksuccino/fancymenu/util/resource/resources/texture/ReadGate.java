package de.keksuccino.fancymenu.util.resource.resources.texture;

import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Holds a thread in the middle of a read until the test opens the gate, like a slow disk. Interrupts don't let the thread pass, because reads of files don't stop for them either.
 */
public final class ReadGate {

    /** How long tests wait for other threads before they fail, so a broken test never hangs. **/
    public static final long TIMEOUT_SECONDS = 10L;

    private final CountDownLatch reached = new CountDownLatch(1);
    private final CountDownLatch opened = new CountDownLatch(1);

    /**
     * Waits until the test opens the gate.
     */
    public void pass() {
        this.reached.countDown();
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    if (!this.opened.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) throw new IllegalStateException("The test never opened the read gate");
                    return;
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    /**
     * Waits until a thread waits at the gate.
     */
    public void awaitReached() throws InterruptedException {
        assertTrue(this.reached.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "No thread reached the read gate");
    }

    public void open() {
        this.opened.countDown();
    }

    /**
     * Waits until the thread waits for a lock, like a close() that waits for the read of another thread.
     */
    public static void awaitBlocked(@NotNull Thread thread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (thread.getState() != Thread.State.BLOCKED) {
            if (!thread.isAlive()) fail("The thread finished without waiting for the lock");
            if (System.nanoTime() > deadline) fail("The thread didn't wait for the lock, it is " + thread.getState());
            Thread.sleep(1L);
        }
    }

    /**
     * Waits for the thread to finish and fails if it doesn't.
     */
    public static void join(@NotNull Thread thread) throws InterruptedException {
        thread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        assertTrue(!thread.isAlive(), "The thread " + thread.getName() + " didn't finish");
    }

}
