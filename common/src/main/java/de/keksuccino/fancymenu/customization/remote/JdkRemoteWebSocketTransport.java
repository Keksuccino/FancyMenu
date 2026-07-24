package de.keksuccino.fancymenu.customization.remote;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Java 17 WebSocket transport with bounded retained message aggregation for legacy Minecraft targets. */
final class JdkRemoteWebSocketTransport implements RemoteWebSocketTransport {

    static final int MESSAGE_TOO_BIG_STATUS = 1009;
    static final int INVALID_PAYLOAD_STATUS = 1007;

    private final int maxInboundMessageUtf8Bytes;
    private final int maxInboundMessageFragments;
    private final WebSocketConnector connector;
    private final Runnable shutdownAction;
    private final java.util.function.BooleanSupplier terminationChecker;
    private final AtomicBoolean shutdownStarted = new AtomicBoolean();

    JdkRemoteWebSocketTransport(int maxInboundMessageUtf8Bytes, int maxInboundMessageFragments) {
        this(maxInboundMessageUtf8Bytes, maxInboundMessageFragments, new OwnedConnector());
    }

    private JdkRemoteWebSocketTransport(int maxInboundMessageUtf8Bytes, int maxInboundMessageFragments, @NotNull OwnedConnector connector) {
        this(maxInboundMessageUtf8Bytes, maxInboundMessageFragments, connector, connector::shutdown, connector::isTerminated);
    }

    JdkRemoteWebSocketTransport(int maxInboundMessageUtf8Bytes, int maxInboundMessageFragments, @NotNull WebSocketConnector connector) {
        this(maxInboundMessageUtf8Bytes, maxInboundMessageFragments, connector, () -> {}, () -> true);
    }

    private JdkRemoteWebSocketTransport(int maxInboundMessageUtf8Bytes, int maxInboundMessageFragments, @NotNull WebSocketConnector connector, @NotNull Runnable shutdownAction, @NotNull java.util.function.BooleanSupplier terminationChecker) {
        if (maxInboundMessageUtf8Bytes <= 0) throw new IllegalArgumentException("maxInboundMessageUtf8Bytes must be positive");
        if (maxInboundMessageFragments <= 0) throw new IllegalArgumentException("maxInboundMessageFragments must be positive");
        this.maxInboundMessageUtf8Bytes = maxInboundMessageUtf8Bytes;
        this.maxInboundMessageFragments = maxInboundMessageFragments;
        this.connector = Objects.requireNonNull(connector);
        this.shutdownAction = Objects.requireNonNull(shutdownAction);
        this.terminationChecker = Objects.requireNonNull(terminationChecker);
    }

    @Override
    public @NotNull Connection connect(@NotNull URI uri, @NotNull Listener listener) {
        JdkConnection connection = new JdkConnection(listener);
        if (this.shutdownStarted.get()) {
            connection.notifyError(new RejectedExecutionException("Remote WebSocket transport is shut down"));
            return connection;
        }
        JdkListener adapter = new JdkListener(connection, listener, this.maxInboundMessageUtf8Bytes, this.maxInboundMessageFragments);
        try {
            this.connector.connect(Objects.requireNonNull(uri), adapter).whenComplete((webSocket, throwable) -> {
                if (throwable != null) connection.notifyError(throwable);
            });
        } catch (Throwable throwable) {
            connection.notifyError(throwable);
        }
        return connection;
    }

    @Override
    public void shutdown() {
        if (this.shutdownStarted.compareAndSet(false, true)) this.shutdownAction.run();
    }

    @Override
    public boolean isTerminated() {
        return this.shutdownStarted.get() && this.terminationChecker.getAsBoolean();
    }

    @FunctionalInterface
    interface WebSocketConnector {

        @NotNull CompletableFuture<WebSocket> connect(@NotNull URI uri, @NotNull WebSocket.Listener listener);
    }

    private static final class OwnedConnector implements WebSocketConnector {

        private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "FancyMenu-RemoteServerConnection-IO");
            thread.setDaemon(true);
            return thread;
        });
        private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).executor(this.executor).build();

        @Override
        public @NotNull CompletableFuture<WebSocket> connect(@NotNull URI uri, @NotNull WebSocket.Listener listener) {
            return this.client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(15)).buildAsync(uri, listener);
        }

        private void shutdown() {
            this.executor.shutdownNow();
            RemoteShutdownSupport.awaitTerminationPreservingInterrupt(this.executor);
        }

        private boolean isTerminated() {
            return this.executor.isTerminated();
        }
    }

    private static final class JdkConnection implements Connection {

        private final Listener listener;
        private final AtomicBoolean opened = new AtomicBoolean();
        private final AtomicBoolean terminal = new AtomicBoolean();
        private volatile WebSocket webSocket;

        private JdkConnection(@NotNull Listener listener) {
            this.listener = Objects.requireNonNull(listener);
        }

        private void notifyOpen(@NotNull WebSocket webSocket) {
            this.webSocket = webSocket;
            if (this.terminal.get()) {
                webSocket.abort();
                return;
            }
            if (this.opened.compareAndSet(false, true)) this.listener.onOpen(this);
        }

        private void notifyClose(int statusCode, @NotNull String reason) {
            this.opened.set(false);
            if (this.terminal.compareAndSet(false, true)) this.listener.onClose(this, statusCode, reason);
        }

        private void notifyError(@NotNull Throwable throwable) {
            this.opened.set(false);
            if (this.terminal.compareAndSet(false, true)) this.listener.onError(this, throwable);
        }

        private void closeForViolation(int statusCode, @NotNull String reason) {
            WebSocket activeWebSocket = this.webSocket;
            if (activeWebSocket != null) activeWebSocket.sendClose(statusCode, reason);
            this.notifyClose(statusCode, reason);
        }

        @Override
        public boolean isOpen() {
            return this.opened.get() && !this.terminal.get() && !Objects.requireNonNull(this.webSocket).isOutputClosed();
        }

        @Override
        public @NotNull CompletableFuture<Void> sendText(@NotNull String data) {
            WebSocket activeWebSocket = this.webSocket;
            if (!this.isOpen() || activeWebSocket == null) return CompletableFuture.failedFuture(new IllegalStateException("WebSocket is not open"));
            return activeWebSocket.sendText(data, true).thenAccept(ignored -> {});
        }

        @Override
        public @NotNull CompletableFuture<Void> sendPing(byte @NotNull [] data) {
            WebSocket activeWebSocket = this.webSocket;
            if (!this.isOpen() || activeWebSocket == null) return CompletableFuture.failedFuture(new IllegalStateException("WebSocket is not open"));
            return activeWebSocket.sendPing(ByteBuffer.wrap(data)).thenAccept(ignored -> {});
        }

        @Override
        public @NotNull CompletableFuture<Void> close(int statusCode, @NotNull String reason) {
            WebSocket activeWebSocket = this.webSocket;
            if (activeWebSocket == null || this.terminal.get()) return CompletableFuture.completedFuture(null);
            return activeWebSocket.sendClose(statusCode, reason).thenAccept(ignored -> {});
        }

        @Override
        public void abort() {
            this.opened.set(false);
            this.terminal.set(true);
            WebSocket activeWebSocket = this.webSocket;
            if (activeWebSocket != null) activeWebSocket.abort();
        }
    }

    private static final class JdkListener implements WebSocket.Listener {

        private final JdkConnection connection;
        private final Listener listener;
        private final int maxInboundMessageUtf8Bytes;
        private final int maxInboundMessageFragments;
        private final StringBuilder textMessage = new StringBuilder();
        private int messageFragments;
        private long messageUtf8Bytes;

        private JdkListener(@NotNull JdkConnection connection, @NotNull Listener listener, int maxInboundMessageUtf8Bytes, int maxInboundMessageFragments) {
            this.connection = connection;
            this.listener = listener;
            this.maxInboundMessageUtf8Bytes = maxInboundMessageUtf8Bytes;
            this.maxInboundMessageFragments = maxInboundMessageFragments;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            this.connection.notifyOpen(webSocket);
            webSocket.request(1L);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String fragment = data.toString();
            long fragmentUtf8Bytes = Utf8Length.count(fragment);
            synchronized (this) {
                this.messageFragments++;
                if (fragmentUtf8Bytes == Utf8Length.MALFORMED_UTF16) return this.reject(webSocket, INVALID_PAYLOAD_STATUS, "invalid_utf8_text_message");
                this.messageUtf8Bytes += fragmentUtf8Bytes;
                if (this.messageFragments > this.maxInboundMessageFragments) return this.reject(webSocket, MESSAGE_TOO_BIG_STATUS, "too_many_message_fragments");
                if (this.messageUtf8Bytes > this.maxInboundMessageUtf8Bytes) return this.reject(webSocket, MESSAGE_TOO_BIG_STATUS, "inbound_message_too_large");
                this.textMessage.append(fragment);
                if (last) {
                    String completeMessage = this.textMessage.toString();
                    int completeUtf8Bytes = Math.toIntExact(this.messageUtf8Bytes);
                    this.resetMessage();
                    this.listener.onText(this.connection, completeMessage, completeUtf8Bytes);
                }
            }
            webSocket.request(1L);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            synchronized (this) {
                this.messageFragments++;
                this.messageUtf8Bytes += data.remaining();
                if (this.messageFragments > this.maxInboundMessageFragments) return this.reject(webSocket, MESSAGE_TOO_BIG_STATUS, "too_many_message_fragments");
                if (this.messageUtf8Bytes > this.maxInboundMessageUtf8Bytes) return this.reject(webSocket, MESSAGE_TOO_BIG_STATUS, "inbound_message_too_large");
                if (last) this.resetMessage();
            }
            webSocket.request(1L);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1L);
            return webSocket.sendPong(message);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            this.listener.onPong(this.connection);
            webSocket.request(1L);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            synchronized (this) {
                this.resetMessage();
            }
            this.connection.notifyClose(statusCode, reason);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            synchronized (this) {
                this.resetMessage();
            }
            this.connection.notifyError(error);
        }

        private CompletionStage<?> reject(@NotNull WebSocket webSocket, int statusCode, @NotNull String reason) {
            this.resetMessage();
            this.connection.closeForViolation(statusCode, reason);
            webSocket.request(1L);
            return CompletableFuture.completedFuture(null);
        }

        private void resetMessage() {
            this.textMessage.setLength(0);
            this.messageFragments = 0;
            this.messageUtf8Bytes = 0L;
        }
    }
}
