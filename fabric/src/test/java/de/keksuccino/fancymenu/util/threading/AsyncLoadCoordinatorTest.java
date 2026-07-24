package de.keksuccino.fancymenu.util.threading;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncLoadCoordinatorTest {

    @Test
    void failedPublicationStillReleasesExactClaim() {
        AsyncLoadCoordinator<String> coordinator = new AsyncLoadCoordinator<>();
        AsyncLoadCoordinator.Claim<String> claim = coordinator.tryClaim("source", 0L, () -> true);
        assertNotNull(claim);

        assertThrows(IllegalStateException.class, () -> coordinator.publishIfCurrent(claim, () -> {
            throw new IllegalStateException("expected test failure");
        }));

        assertFalse(coordinator.isLoading("source"));
        assertNotNull(coordinator.tryClaim("source", 1L, () -> true));
    }

    @Test
    void duplicateTaskInvocationCannotReleaseRunningClaim() throws Exception {
        AsyncLoadCoordinator<String> coordinator = new AsyncLoadCoordinator<>();
        AsyncLoadCoordinator.Claim<String> claim = coordinator.tryClaim("source", 0L, () -> true);
        assertNotNull(claim);
        CountDownLatch firstInvocationEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstInvocation = new CountDownLatch(1);
        AtomicInteger taskExecutions = new AtomicInteger();
        Thread firstInvocation = new Thread(() -> coordinator.runClaim(claim, () -> {
            taskExecutions.incrementAndGet();
            firstInvocationEntered.countDown();
            await(releaseFirstInvocation);
        }), "AsyncLoadCoordinatorTest-first-invocation");

        firstInvocation.start();
        try {
            assertTrue(firstInvocationEntered.await(5L, TimeUnit.SECONDS));
            coordinator.runClaim(claim, taskExecutions::incrementAndGet);
            assertEquals(1, taskExecutions.get());
            assertTrue(coordinator.isLoading("source"));
            assertNull(coordinator.tryClaim("source", 1L, () -> true));
        } finally {
            releaseFirstInvocation.countDown();
            firstInvocation.join(5000L);
        }
        assertFalse(firstInvocation.isAlive());
        assertFalse(coordinator.isLoading("source"));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out while waiting to release the first invocation");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting to release the first invocation", exception);
        }
    }

}
