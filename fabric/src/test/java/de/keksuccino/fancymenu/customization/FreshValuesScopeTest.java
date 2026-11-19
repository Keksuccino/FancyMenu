package de.keksuccino.fancymenu.customization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreshValuesScopeTest {

    @AfterEach
    void scopeIsClosedAfterEveryTest() {
        assertFalse(FreshValuesScope.isActive());
    }

    @Test
    void scopeIsOnlyActiveWhileItIsOpen() {
        assertFalse(FreshValuesScope.isActive());
        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            assertTrue(FreshValuesScope.isActive());
        }
        assertFalse(FreshValuesScope.isActive());
    }

    @Test
    void nestedScopesKeepTheScopeActiveUntilTheOutermostOneCloses() {
        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            try (FreshValuesScope ignored2 = FreshValuesScope.enter()) {
                try (FreshValuesScope ignored3 = FreshValuesScope.enter()) {
                    assertTrue(FreshValuesScope.isActive());
                }
                assertTrue(FreshValuesScope.isActive());
            }
            assertTrue(FreshValuesScope.isActive());
        }
        assertFalse(FreshValuesScope.isActive());
    }

    @Test
    void scopeClosesWhenTheCodeInsideThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            try (FreshValuesScope ignored = FreshValuesScope.enter()) {
                try (FreshValuesScope ignored2 = FreshValuesScope.enter()) {
                    throw new IllegalArgumentException();
                }
            }
        });
        assertFalse(FreshValuesScope.isActive());
    }

    @Test
    void closingAScopeThatIsNotOpenThrowsAndKeepsTheScopeClosed() {
        FreshValuesScope scope = FreshValuesScope.enter();
        scope.close();

        assertThrows(IllegalStateException.class, scope::close);
        assertFalse(FreshValuesScope.isActive());

        // The failed close must not have broken the depth of later scopes
        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            assertTrue(FreshValuesScope.isActive());
        }
        assertFalse(FreshValuesScope.isActive());
    }

    @Test
    void otherThreadsAreNotInTheScopeOfThisThread() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            try (FreshValuesScope ignored = FreshValuesScope.enter()) {
                assertFalse(executor.submit(FreshValuesScope::isActive).get(5L, TimeUnit.SECONDS));
                assertTrue(FreshValuesScope.isActive());
            }
        } finally {
            shutDown(executor);
        }
    }

    @Test
    void scopesOfOtherThreadsDoNotAffectThisThread() throws Exception {
        CountDownLatch otherThreadEntered = new CountDownLatch(1);
        CountDownLatch thisThreadChecked = new CountDownLatch(1);
        CountDownLatch thisThreadEntered = new CountDownLatch(1);
        CountDownLatch otherThreadClosed = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> otherThread = executor.submit(() -> {
                try (FreshValuesScope ignored = FreshValuesScope.enter()) {
                    otherThreadEntered.countDown();
                    await(thisThreadChecked);
                    await(thisThreadEntered);
                }
                otherThreadClosed.countDown();
                return FreshValuesScope.isActive();
            });

            await(otherThreadEntered);
            assertFalse(FreshValuesScope.isActive());
            thisThreadChecked.countDown();
            try (FreshValuesScope ignored = FreshValuesScope.enter()) {
                thisThreadEntered.countDown();
                await(otherThreadClosed);
                assertTrue(FreshValuesScope.isActive());
            }
            assertFalse(otherThread.get(5L, TimeUnit.SECONDS));
        } finally {
            thisThreadChecked.countDown();
            thisThreadEntered.countDown();
            shutDown(executor);
        }
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("The other thread did not get there in time");
    }

    private static void shutDown(ExecutorService executor) throws InterruptedException {
        executor.shutdownNow();
        if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) throw new AssertionError("The executor did not terminate in time");
    }

}
