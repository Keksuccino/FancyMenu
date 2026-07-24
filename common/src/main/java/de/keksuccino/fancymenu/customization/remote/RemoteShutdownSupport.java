package de.keksuccino.fancymenu.customization.remote;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/** Shared bounded-wait policy for the executor resources owned by remote networking. */
final class RemoteShutdownSupport {

    static final long TERMINATION_TIMEOUT_MILLIS = 5_000L;

    private RemoteShutdownSupport() {
    }

    static boolean awaitTerminationPreservingInterrupt(@NotNull ExecutorService executor) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TERMINATION_TIMEOUT_MILLIS);
        boolean interrupted = false;
        try {
            while (!executor.isTerminated()) {
                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0L) {
                    return false;
                }
                try {
                    if (!executor.awaitTermination(remainingNanos, TimeUnit.NANOSECONDS)) {
                        return false;
                    }
                } catch (InterruptedException ex) {
                    // Shutdown must continue after interruption, but callers must retain their interrupt signal.
                    interrupted = true;
                }
            }
            return true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
