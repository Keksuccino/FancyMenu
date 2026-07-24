package de.keksuccino.fancymenu.customization.remote;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

interface RemoteWebSocketTransport {

    @NotNull Connection connect(@NotNull URI uri, @NotNull Listener listener);

    /** Permanently rejects new connections and waits a bounded time for owned I/O workers to terminate. */
    void shutdown();

    boolean isTerminated();

    interface Connection {

        boolean isOpen();

        @NotNull CompletableFuture<Void> sendText(@NotNull String data);

        @NotNull CompletableFuture<Void> sendPing(byte @NotNull [] data);

        @NotNull CompletableFuture<Void> close(int statusCode, @NotNull String reason);

        void abort();
    }

    interface Listener {

        void onOpen(@NotNull Connection connection);

        void onText(@NotNull Connection connection, @NotNull String data, int utf8Bytes);

        void onPong(@NotNull Connection connection);

        void onClose(@NotNull Connection connection, int statusCode, @NotNull String reason);

        void onError(@NotNull Connection connection, @NotNull Throwable error);
    }
}
