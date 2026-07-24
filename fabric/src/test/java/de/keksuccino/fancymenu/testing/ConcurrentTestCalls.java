package de.keksuccino.fancymenu.testing;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class ConcurrentTestCalls {

    private ConcurrentTestCalls() {
    }

    @NotNull
    public static <T> List<T> invoke(int callers, @NotNull ThrowingSupplier<T> supplier) throws Exception {
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(callers);
        List<Future<T>> futures = new ArrayList<>();
        try {
            for (int caller = 0; caller < callers; caller++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Concurrent callers did not start in time");
                    return supplier.get();
                }));
            }
            if (!ready.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Concurrent callers were not ready in time");
            start.countDown();
            List<T> values = new ArrayList<>();
            for (Future<T> future : futures) values.add(future.get(5L, TimeUnit.SECONDS));
            return values;
        } finally {
            start.countDown();
            executor.shutdownNow();
            if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) throw new AssertionError("Concurrent caller executor did not terminate in time");
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {

        T get() throws Exception;

    }

}
