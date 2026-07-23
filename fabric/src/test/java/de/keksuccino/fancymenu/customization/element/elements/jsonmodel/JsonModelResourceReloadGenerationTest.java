package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonModelResourceReloadGenerationTest {

    @Test
    void advancesOnlyAfterReloadFinishes() {
        long initial = JsonModelResourceReloadGeneration.current();

        JsonModelResourceReloadGeneration.onReload(MinecraftResourceReloadObserver.ReloadAction.STARTING);
        assertEquals(initial, JsonModelResourceReloadGeneration.current());
        JsonModelResourceReloadGeneration.onReload(MinecraftResourceReloadObserver.ReloadAction.FINISHED);
        assertEquals(initial + 1L, JsonModelResourceReloadGeneration.current());
    }

    @Test
    void publishesConcurrentFinishedReloadsWithoutLostUpdates() throws Exception {
        int threadCount = 4;
        int reloadsPerThread = 100;
        long initial = JsonModelResourceReloadGeneration.current();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < threadCount; thread++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int reload = 0; reload < reloadsPerThread; reload++) JsonModelResourceReloadGeneration.onReload(MinecraftResourceReloadObserver.ReloadAction.FINISHED);
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get();
        } finally {
            executor.shutdownNow();
        }

        assertEquals(initial + (long) threadCount * reloadsPerThread, JsonModelResourceReloadGeneration.current());
    }

}
