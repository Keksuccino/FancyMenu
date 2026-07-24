package de.keksuccino.fancymenu.customization.remote;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

/**
 * Java 21 WebSocket transport used on Minecraft versions whose bundled Netty does not include the HTTP/WebSocket
 * codecs. Demand is requested one callback at a time, and fragmented messages are bounded before they can enter the
 * main-thread delivery queue.
 */
final class JdkRemoteWebSocketTransport implements RemoteWebSocketTransport {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MESSAGE_TOO_BIG_STATUS = 1009;
    private static final int INVALID_PAYLOAD_STATUS = 1007;
    private final int maxInboundMessageBytes;
    private final int maxInboundFragments;
    private final ExecutorService httpExecutor;
    private final HttpClient httpClient;
    private final Object shutdownLock = new Object();
    private final AtomicBoolean shutdownStarted = new AtomicBoolean();

    JdkRemoteWebSocketTransport(int maxInboundMessageBytes, int maxInboundFragments) {
        if (maxInboundMessageBytes <= 0 || maxInboundFragments <= 0) throw new IllegalArgumentException("Inbound WebSocket limits must be positive");
        this.maxInboundMessageBytes = maxInboundMessageBytes;
        this.maxInboundFragments = maxInboundFragments;
        this.httpExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "FancyMenu-RemoteServerConnection-IO");
            thread.setDaemon(true);
            return thread;
        });
        try {
            // Java 21 has no HttpClient close operation. The explicitly supplied executor is the worker resource this
            // transport owns and can synchronously quiesce after the manager aborts every outstanding WebSocket.
            this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).executor(this.httpExecutor).build();
        } catch (RuntimeException | Error throwable) {
            this.httpExecutor.shutdownNow();
            throw throwable;
        }
    }

    @Override
    public @NotNull Connection connect(@NotNull URI uri, @NotNull Listener listener) {
        JdkConnection connection = new JdkConnection(listener, this.maxInboundMessageBytes, this.maxInboundFragments);
        if (this.shutdownStarted.get()) {
            connection.receiveError(new RejectedExecutionException("Remote WebSocket transport is shut down"));
            return connection;
        }
        try {
            CompletableFuture<WebSocket> handshake = this.httpClient.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(15)).buildAsync(uri, connection);
            connection.attachHandshake(handshake);
            handshake.whenComplete((socket, throwable) -> {
                connection.clearHandshake(handshake);
                if (throwable != null) connection.receiveError(throwable);
            });
        } catch (RuntimeException ex) {
            connection.receiveError(ex);
        }
        return connection;
    }

    @Override
    public void shutdown() {
        synchronized (this.shutdownLock) {
            if (this.shutdownStarted.compareAndSet(false, true)) this.httpExecutor.shutdownNow();
        }
        if (!RemoteShutdownSupport.awaitTerminationPreservingInterrupt(this.httpExecutor)) {
            LOGGER.warn("[FANCYMENU] Timed out while waiting for the remote WebSocket I/O executor to terminate");
        }
    }

    @Override
    public boolean isTerminated() {
        return this.httpExecutor.isTerminated();
    }

    static final class JdkConnection implements Connection, WebSocket.Listener {

        private final Listener listener;
        private final int maxInboundMessageBytes;
        private final int maxInboundFragments;
        private final StringBuilder textMessage = new StringBuilder();

        @Nullable
        private volatile WebSocket socket;
        @Nullable
        private CompletableFuture<WebSocket> pendingHandshake;
        private boolean closing;
        private boolean closed;
        private int textFragments;
        private int binaryFragments;
        private int binaryBytes;

        JdkConnection(@NotNull Listener listener, int maxInboundMessageBytes, int maxInboundFragments) {
            this.listener = Objects.requireNonNull(listener);
            this.maxInboundMessageBytes = maxInboundMessageBytes;
            this.maxInboundFragments = maxInboundFragments;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            synchronized (this) {
                if (this.closed || this.closing) {
                    webSocket.abort();
                    return;
                }
                this.socket = webSocket;
                this.pendingHandshake = null;
            }
            this.listener.onOpen(this);
            webSocket.request(1L);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String completedMessage = null;
            int completedUtf8Bytes = 0;
            int violationStatus = 0;
            String violationReason = null;
            synchronized (this) {
                if (this.closed || this.closing) return CompletableFuture.completedFuture(null);
                if (this.textFragments >= this.maxInboundFragments || data.length() > this.maxInboundMessageBytes - this.textMessage.length()) {
                    violationStatus = MESSAGE_TOO_BIG_STATUS;
                    violationReason = "inbound_message_too_large";
                    this.resetTextMessage();
                } else {
                    this.textFragments++;
                    this.textMessage.append(data);
                    if (last) {
                        completedMessage = this.textMessage.toString();
                        long completedUtf8ByteCount = Utf8Length.count(completedMessage);
                        this.resetTextMessage();
                        if (completedUtf8ByteCount == Utf8Length.MALFORMED_UTF16) {
                            completedMessage = null;
                            violationStatus = INVALID_PAYLOAD_STATUS;
                            violationReason = "invalid_utf8_text_message";
                        } else if (completedUtf8ByteCount > this.maxInboundMessageBytes) {
                            completedMessage = null;
                            violationStatus = MESSAGE_TOO_BIG_STATUS;
                            violationReason = "inbound_message_too_large";
                        } else {
                            completedUtf8Bytes = (int)completedUtf8ByteCount;
                        }
                    }
                }
            }
            if (violationStatus != 0) {
                this.closeForInboundViolation(webSocket, violationStatus, Objects.requireNonNull(violationReason));
            } else if (completedMessage != null) {
                this.listener.onText(this, completedMessage, completedUtf8Bytes);
                this.requestNext(webSocket);
            } else {
                this.requestNext(webSocket);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            boolean violation;
            synchronized (this) {
                if (this.closed || this.closing) return CompletableFuture.completedFuture(null);
                violation = this.binaryFragments >= this.maxInboundFragments || data.remaining() > this.maxInboundMessageBytes - this.binaryBytes;
                if (violation || last) {
                    this.binaryFragments = 0;
                    this.binaryBytes = 0;
                } else {
                    this.binaryFragments++;
                    this.binaryBytes += data.remaining();
                }
            }
            if (violation) {
                this.closeForInboundViolation(webSocket, MESSAGE_TOO_BIG_STATUS, "inbound_message_too_large");
            } else {
                this.requestNext(webSocket);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            CompletionStage<?> response = WebSocket.Listener.super.onPing(webSocket, message);
            this.requestNext(webSocket);
            return response;
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            synchronized (this) {
                if (this.closed || this.closing) return CompletableFuture.completedFuture(null);
            }
            this.listener.onPong(this);
            this.requestNext(webSocket);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (this.markClosed()) this.listener.onClose(this, statusCode, Objects.requireNonNullElse(reason, ""));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            this.receiveError(error);
        }

        @Override
        public boolean isOpen() {
            WebSocket activeSocket = this.socket;
            return !this.closed && !this.closing && activeSocket != null && !activeSocket.isInputClosed() && !activeSocket.isOutputClosed();
        }

        @Override
        public @NotNull CompletableFuture<Void> sendText(@NotNull String data) {
            WebSocket activeSocket = this.requireSocket();
            return activeSocket.sendText(data, true).thenApply(ignored -> null);
        }

        @Override
        public @NotNull CompletableFuture<Void> sendPing(byte @NotNull [] data) {
            WebSocket activeSocket = this.requireSocket();
            return activeSocket.sendPing(ByteBuffer.wrap(data)).thenApply(ignored -> null);
        }

        @Override
        public @NotNull CompletableFuture<Void> close(int statusCode, @NotNull String reason) {
            WebSocket activeSocket = this.socket;
            if (activeSocket == null) {
                this.markClosed();
                return CompletableFuture.completedFuture(null);
            }
            if (!this.beginClosing()) return CompletableFuture.completedFuture(null);
            return activeSocket.sendClose(statusCode, reason).thenApply(ignored -> null);
        }

        @Override
        public void abort() {
            if (!this.markClosed()) return;
            WebSocket activeSocket = this.socket;
            if (activeSocket != null) activeSocket.abort();
        }

        private @NotNull WebSocket requireSocket() {
            WebSocket activeSocket = this.socket;
            if (activeSocket == null || this.closed || this.closing) throw new IllegalStateException("WebSocket connection is not open");
            return activeSocket;
        }

        private void closeForInboundViolation(@NotNull WebSocket webSocket, int statusCode, @NotNull String reason) {
            if (this.beginClosing()) webSocket.sendClose(statusCode, reason);
        }

        private void requestNext(@NotNull WebSocket webSocket) {
            synchronized (this) {
                if (this.closed || this.closing) return;
                webSocket.request(1L);
            }
        }

        private void receiveError(@NotNull Throwable error) {
            if (this.markClosed()) this.listener.onError(this, error);
        }

        private synchronized void attachHandshake(@NotNull CompletableFuture<WebSocket> handshake) {
            if (this.closed || this.closing) {
                handshake.cancel(true);
            } else if (this.socket == null && !handshake.isDone()) {
                this.pendingHandshake = handshake;
            }
        }

        private synchronized void clearHandshake(@NotNull CompletableFuture<WebSocket> handshake) {
            if (this.pendingHandshake == handshake) this.pendingHandshake = null;
        }

        private synchronized boolean markClosed() {
            if (this.closed) return false;
            this.closed = true;
            this.closing = true;
            CompletableFuture<WebSocket> handshake = this.pendingHandshake;
            this.pendingHandshake = null;
            if (handshake != null) handshake.cancel(true);
            this.resetTextMessage();
            this.binaryFragments = 0;
            this.binaryBytes = 0;
            return true;
        }

        private synchronized boolean beginClosing() {
            if (this.closed || this.closing) return false;
            this.closing = true;
            this.resetTextMessage();
            this.binaryFragments = 0;
            this.binaryBytes = 0;
            return true;
        }

        private void resetTextMessage() {
            this.textMessage.setLength(0);
            this.textFragments = 0;
        }
    }
}
