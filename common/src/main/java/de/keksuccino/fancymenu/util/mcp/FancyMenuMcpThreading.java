package de.keksuccino.fancymenu.util.mcp;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

final class FancyMenuMcpThreading {

    private static final long MAIN_THREAD_TIMEOUT_SECONDS = 60L;

    private FancyMenuMcpThreading() {
    }

    static <T> T callOnMainThread(@NotNull Callable<T> callable) throws Exception {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) {
            return callable.call();
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        AtomicBoolean cancelled = new AtomicBoolean(false);
        MainThreadTaskExecutor.executeInMainThread(() -> {
            if (cancelled.get()) {
                return;
            }
            try {
                future.complete(callable.call());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        try {
            return future.get(MAIN_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            cancelled.set(true);
            future.cancel(false);
            throw new TimeoutException("Timed out while waiting for main-thread MCP task completion.");
        }
    }

    static void runOnMainThread(@NotNull ThrowingRunnable runnable) throws Exception {
        callOnMainThread(() -> {
            runnable.run();
            return null;
        });
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
