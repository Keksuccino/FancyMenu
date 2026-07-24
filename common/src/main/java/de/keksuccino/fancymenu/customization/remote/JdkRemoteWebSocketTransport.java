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
 * Java 17 WebSocket transport used on Minecraft versions whose bundled Netty omits the HTTP/WebSocket codecs.
 * Text and binary callbacks are admitted one fragment at a time, and the dedicated accumulators enforce byte and
 * fragment limits before retaining more data. This keeps the older runtime bounded without adding a second Netty.
 */
final class JdkRemoteWebSocketTransport implements RemoteWebSocketTransport {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MESSAGE_TOO_BIG_STATUS = 1009;
    private final int maxInboundMessageBytes;
    private final int maxInboundFragments;
    private final ExecutorService executor;
    private final HttpClient httpClient;
    private final AtomicBoolean shutdownStarted = new AtomicBoolean();

    JdkRemoteWebSocketTransport(int maxInboundMessageBytes, int maxInboundFragments) {
        if (maxInboundMessageBytes <= 0 || maxInboundFragments <= 0) throw new IllegalArgumentException("Inbound WebSocket limits must be positive");
        this.maxInboundMessageBytes = maxInboundMessageBytes;
        this.maxInboundFragments = maxInboundFragments;
        this.executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "FancyMenu-RemoteWebSocket");
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15L)).executor(this.executor).build();
    }

    @Override
    public @NotNull Connection connect(@NotNull URI uri, @NotNull Listener listener) {
        JdkConnection connection = new JdkConnection(listener, this.maxInboundMessageBytes, this.maxInboundFragments);
        if (this.shutdownStarted.get()) {
            connection.notifyError(new RejectedExecutionException("Remote WebSocket transport is shut down"));
            return connection;
        }
        try {
            CompletableFuture<WebSocket> connectFuture = this.httpClient.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(15L)).buildAsync(uri, connection);
            connection.attachConnectFuture(connectFuture);
            connectFuture.exceptionally(throwable -> {
                connection.notifyError(unwrapCompletionFailure(throwable));
                return null;
            });
        } catch (RuntimeException ex) {
            connection.notifyError(ex);
        }
        return connection;
    }

    @Override
    public void shutdown() {
        if (this.shutdownStarted.compareAndSet(false, true)) this.executor.shutdownNow();
        if (!RemoteShutdownSupport.awaitTerminationPreservingInterrupt(this.executor)) LOGGER.warn("[FANCYMENU] Timed out while waiting for the remote WebSocket executor to terminate");
    }

    @Override
    public boolean isTerminated() {
        return this.executor.isTerminated();
    }

    private static @NotNull Throwable unwrapCompletionFailure(@NotNull Throwable throwable) {
        Throwable cause = throwable.getCause();
        return cause != null ? cause : throwable;
    }

    private static @NotNull CompletableFuture<Void> asVoid(@NotNull CompletableFuture<WebSocket> operation) {
        return operation.thenApply(ignored -> null);
    }

    private static final class JdkConnection implements Connection, WebSocket.Listener {

        private final Listener listener;
        private final JdkInboundTextBuffer textBuffer;
        private final JdkInboundBinaryBuffer binaryBuffer;
        private final AtomicBoolean opened = new AtomicBoolean();
        private final AtomicBoolean closing = new AtomicBoolean();
        private final AtomicBoolean terminalCallbackSent = new AtomicBoolean();

        private volatile @Nullable CompletableFuture<WebSocket> connectFuture;
        private volatile @Nullable WebSocket webSocket;

        private JdkConnection(@NotNull Listener listener, int maxInboundMessageBytes, int maxInboundFragments) {
            this.listener = Objects.requireNonNull(listener);
            this.textBuffer = new JdkInboundTextBuffer(maxInboundMessageBytes, maxInboundFragments);
            this.binaryBuffer = new JdkInboundBinaryBuffer(maxInboundMessageBytes, maxInboundFragments);
        }

        @Override
        public boolean isOpen() {
            WebSocket currentWebSocket = this.webSocket;
            return this.opened.get() && !this.closing.get() && currentWebSocket != null && !currentWebSocket.isInputClosed() && !currentWebSocket.isOutputClosed();
        }

        @Override
        public @NotNull CompletableFuture<Void> sendText(@NotNull String data) {
            WebSocket currentWebSocket = this.webSocket;
            if (!isOpen() || currentWebSocket == null) return CompletableFuture.failedFuture(new IllegalStateException("Remote WebSocket is not open"));
            return asVoid(currentWebSocket.sendText(data, true));
        }

        @Override
        public @NotNull CompletableFuture<Void> sendPing(byte @NotNull [] data) {
            WebSocket currentWebSocket = this.webSocket;
            if (!isOpen() || currentWebSocket == null) return CompletableFuture.failedFuture(new IllegalStateException("Remote WebSocket is not open"));
            return asVoid(currentWebSocket.sendPing(ByteBuffer.wrap(data)));
        }

        @Override
        public @NotNull CompletableFuture<Void> close(int statusCode, @NotNull String reason) {
            WebSocket currentWebSocket = this.webSocket;
            if (currentWebSocket == null || currentWebSocket.isOutputClosed()) return CompletableFuture.failedFuture(new IllegalStateException("Remote WebSocket is not active"));
            if (!this.closing.compareAndSet(false, true)) return CompletableFuture.failedFuture(new IllegalStateException("Remote WebSocket is already closing"));
            try {
                return asVoid(currentWebSocket.sendClose(statusCode, reason));
            } catch (RuntimeException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }

        @Override
        public void abort() {
            this.resetInboundBuffers();
            this.closing.set(true);
            CompletableFuture<WebSocket> pendingConnect = this.connectFuture;
            if (pendingConnect != null) pendingConnect.cancel(true);
            WebSocket currentWebSocket = this.webSocket;
            if (currentWebSocket != null) currentWebSocket.abort();
        }

        @Override
        public void onOpen(WebSocket openedWebSocket) {
            this.webSocket = openedWebSocket;
            if (this.closing.get()) {
                openedWebSocket.abort();
                return;
            }
            if (!this.opened.compareAndSet(false, true)) return;
            try {
                this.listener.onOpen(this);
            } catch (Throwable throwable) {
                notifyError(throwable);
                return;
            }
            openedWebSocket.request(1L);
        }

        @Override
        public @Nullable CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (!this.closing.get() && !this.terminalCallbackSent.get()) {
                JdkInboundTextBuffer.Result result = this.textBuffer.accept(data, last);
                if (result.type() == JdkInboundTextBuffer.ResultType.COMPLETE) {
                    try {
                        this.listener.onText(this, Objects.requireNonNull(result.text()), result.utf8Bytes());
                    } catch (Throwable throwable) {
                        notifyError(throwable);
                        return null;
                    }
                } else if (result.type() != JdkInboundTextBuffer.ResultType.PARTIAL) {
                    closeForInboundViolation(result.type().name().toLowerCase());
                    return null;
                }
            }
            webSocket.request(1L);
            return null;
        }

        @Override
        public @Nullable CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            if (!this.closing.get() && !this.terminalCallbackSent.get() && !this.binaryBuffer.accept(data.remaining(), last)) {
                closeForInboundViolation("binary_message_limit");
                return null;
            }
            webSocket.request(1L);
            return null;
        }

        @Override
        public @Nullable CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            if (!this.closing.get() && !this.terminalCallbackSent.get()) {
                try {
                    this.listener.onPong(this);
                } catch (Throwable throwable) {
                    notifyError(throwable);
                    return null;
                }
            }
            webSocket.request(1L);
            return null;
        }

        @Override
        public @Nullable CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            this.resetInboundBuffers();
            this.closing.set(true);
            if (!this.terminalCallbackSent.compareAndSet(false, true)) return null;
            try {
                this.listener.onClose(this, statusCode, reason);
            } catch (Throwable throwable) {
                LOGGER.error("[FANCYMENU] Remote WebSocket close listener failed", throwable);
            }
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            notifyError(error);
        }

        private void attachConnectFuture(@NotNull CompletableFuture<WebSocket> connectFuture) {
            this.connectFuture = connectFuture;
            if (this.closing.get()) connectFuture.cancel(true);
        }

        private void closeForInboundViolation(@NotNull String reason) {
            this.resetInboundBuffers();
            if (!this.closing.compareAndSet(false, true)) return;
            WebSocket currentWebSocket = this.webSocket;
            if (currentWebSocket == null) {
                notifyError(new IllegalStateException("Inbound WebSocket violation before the connection opened"));
                return;
            }
            try {
                currentWebSocket.sendClose(MESSAGE_TOO_BIG_STATUS, reason).exceptionally(throwable -> {
                    notifyError(unwrapCompletionFailure(throwable));
                    currentWebSocket.abort();
                    return null;
                });
            } catch (RuntimeException exception) {
                notifyError(exception);
                currentWebSocket.abort();
            }
        }

        private void notifyError(@NotNull Throwable error) {
            this.resetInboundBuffers();
            this.closing.set(true);
            if (!this.terminalCallbackSent.compareAndSet(false, true)) return;
            try {
                this.listener.onError(this, error);
            } catch (Throwable throwable) {
                LOGGER.error("[FANCYMENU] Remote WebSocket error listener failed", throwable);
            } finally {
                WebSocket currentWebSocket = this.webSocket;
                if (currentWebSocket != null) currentWebSocket.abort();
            }
        }

        private void resetInboundBuffers() {
            this.textBuffer.reset();
            this.binaryBuffer.reset();
        }
    }

    static final class JdkInboundTextBuffer {

        private final int maxUtf8Bytes;
        private final int maxFragments;
        private final StringBuilder text = new StringBuilder();
        private int utf8Bytes;
        private int fragments;
        private boolean pendingHighSurrogate;

        JdkInboundTextBuffer(int maxUtf8Bytes, int maxFragments) {
            if (maxUtf8Bytes <= 0 || maxFragments <= 0) throw new IllegalArgumentException("Inbound text limits must be positive");
            this.maxUtf8Bytes = maxUtf8Bytes;
            this.maxFragments = maxFragments;
        }

        synchronized @NotNull Result accept(@NotNull CharSequence fragment, boolean last) {
            if (this.fragments >= this.maxFragments) return fail(ResultType.TOO_MANY_FRAGMENTS);
            this.fragments++;
            int index = 0;
            if (this.pendingHighSurrogate) {
                if (fragment.length() == 0 || !Character.isLowSurrogate(fragment.charAt(0))) return fail(ResultType.INVALID_UTF16);
                if (!addBytes(4)) return fail(ResultType.TOO_LARGE);
                this.pendingHighSurrogate = false;
                index = 1;
            }

            for (; index < fragment.length(); index++) {
                char character = fragment.charAt(index);
                if (character <= 0x7F) {
                    if (!addBytes(1)) return fail(ResultType.TOO_LARGE);
                } else if (character <= 0x7FF) {
                    if (!addBytes(2)) return fail(ResultType.TOO_LARGE);
                } else if (Character.isHighSurrogate(character)) {
                    if (index + 1 < fragment.length()) {
                        if (!Character.isLowSurrogate(fragment.charAt(index + 1))) return fail(ResultType.INVALID_UTF16);
                        if (!addBytes(4)) return fail(ResultType.TOO_LARGE);
                        index++;
                    } else {
                        this.pendingHighSurrogate = true;
                    }
                } else if (Character.isLowSurrogate(character)) {
                    return fail(ResultType.INVALID_UTF16);
                } else if (!addBytes(3)) {
                    return fail(ResultType.TOO_LARGE);
                }
            }

            this.text.append(fragment);
            if (!last) return new Result(ResultType.PARTIAL, null, 0);
            if (this.pendingHighSurrogate) return fail(ResultType.INVALID_UTF16);
            String completedText = this.text.toString();
            int completedBytes = this.utf8Bytes;
            reset();
            return new Result(ResultType.COMPLETE, completedText, completedBytes);
        }

        synchronized void reset() {
            this.text.setLength(0);
            this.utf8Bytes = 0;
            this.fragments = 0;
            this.pendingHighSurrogate = false;
        }

        private boolean addBytes(int count) {
            if (count > this.maxUtf8Bytes - this.utf8Bytes) return false;
            this.utf8Bytes += count;
            return true;
        }

        private @NotNull Result fail(@NotNull ResultType type) {
            reset();
            return new Result(type, null, 0);
        }

        enum ResultType {
            PARTIAL,
            COMPLETE,
            TOO_LARGE,
            TOO_MANY_FRAGMENTS,
            INVALID_UTF16
        }

        record Result(@NotNull ResultType type, @Nullable String text, int utf8Bytes) {
        }
    }

    private static final class JdkInboundBinaryBuffer {

        private final int maxBytes;
        private final int maxFragments;
        private int bytes;
        private int fragments;

        private JdkInboundBinaryBuffer(int maxBytes, int maxFragments) {
            this.maxBytes = maxBytes;
            this.maxFragments = maxFragments;
        }

        private synchronized boolean accept(int fragmentBytes, boolean last) {
            if (this.fragments >= this.maxFragments || fragmentBytes > this.maxBytes - this.bytes) {
                reset();
                return false;
            }
            this.fragments++;
            this.bytes += fragmentBytes;
            if (last) reset();
            return true;
        }

        private synchronized void reset() {
            this.bytes = 0;
            this.fragments = 0;
        }
    }
}
