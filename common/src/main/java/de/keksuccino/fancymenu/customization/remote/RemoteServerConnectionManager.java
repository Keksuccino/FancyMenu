package de.keksuccino.fancymenu.customization.remote;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class RemoteServerConnectionManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final long RECONNECT_DELAY_MILLIS = 10_000L;
    private static final long HEARTBEAT_TICK_INTERVAL_MILLIS = 5_000L;
    private static final long HEARTBEAT_PING_INTERVAL_MILLIS = 20_000L;
    private static final long HEARTBEAT_PONG_TIMEOUT_MILLIS = 70_000L;
    private static final byte[] HEARTBEAT_PING_DATA = "fm_remote_ping".getBytes(StandardCharsets.UTF_8);

    private static final ExecutorService HTTP_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu-RemoteServerConnection-IO");
        thread.setDaemon(true);
        return thread;
    });

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .executor(HTTP_EXECUTOR)
            .build();

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu-RemoteServerConnectionManager");
        thread.setDaemon(true);
        return thread;
    });

    private static final Map<String, String> CACHED_REQUEST_IDS_BY_URL = new ConcurrentHashMap<>();
    private static final Map<String, ConnectionState> CONNECTIONS_BY_URL = new ConcurrentHashMap<>();
    private static final Map<String, ConnectionState> CONNECTIONS_BY_REQUEST_ID = new ConcurrentHashMap<>();

    static {
        EXECUTOR.scheduleAtFixedRate(
                RemoteServerConnectionManager::runHeartbeatAndReconnectTick,
                HEARTBEAT_TICK_INTERVAL_MILLIS,
                HEARTBEAT_TICK_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    private RemoteServerConnectionManager() {
    }

    public static void sendData(@NotNull String remoteServerUrl, @Nullable String data) {
        String normalizedUrl = normalizeRemoteServerUrl(remoteServerUrl);
        if (normalizedUrl == null) {
            LOGGER.warn("[FANCYMENU] Ignoring remote data send request due to invalid remote server URL: {}", remoteServerUrl);
            return;
        }

        ConnectionState state = CONNECTIONS_BY_URL.computeIfAbsent(normalizedUrl, RemoteServerConnectionManager::createAndRegisterConnectionState);
        state.pendingPayloads.add(Objects.requireNonNullElse(data, ""));
        synchronized (state.lock) {
            state.reconnectRequested = true;
            if (state.nextReconnectAttemptAtMillis <= 0L) {
                state.nextReconnectAttemptAtMillis = System.currentTimeMillis();
            }
        }
        connectIfNeededAndFlush(state);
    }

    public static void closeAllConnections() {
        for (ConnectionState state : CONNECTIONS_BY_URL.values()) {
            closeConnectionState(state);
        }
    }

    public static void closeConnectionByRequestId(@NotNull String requestId) {
        String trimmedRequestId = requestId.trim();
        if (trimmedRequestId.isBlank()) {
            return;
        }
        ConnectionState state = CONNECTIONS_BY_REQUEST_ID.get(trimmedRequestId);
        if (state != null) {
            closeConnectionState(state);
        }
    }

    @Nullable
    private static String normalizeRemoteServerUrl(@Nullable String rawUrl) {
        if (rawUrl == null) {
            return null;
        }

        String candidate = rawUrl.trim();
        if (candidate.isBlank()) {
            return null;
        }

        String lower = candidate.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://")) {
            candidate = "ws://" + candidate.substring("http://".length());
        } else if (lower.startsWith("https://")) {
            candidate = "wss://" + candidate.substring("https://".length());
        } else if (!lower.startsWith("ws://") && !lower.startsWith("wss://")) {
            if (candidate.contains("://")) {
                return null;
            }
            candidate = "wss://" + candidate;
        }

        try {
            URI uri = URI.create(candidate);
            String scheme = (uri.getScheme() != null) ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
            if (!"ws".equals(scheme) && !"wss".equals(scheme)) {
                return null;
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return null;
            }
            return uri.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    @NotNull
    private static ConnectionState createAndRegisterConnectionState(@NotNull String normalizedUrl) {
        String cachedRequestId = CACHED_REQUEST_IDS_BY_URL.get(normalizedUrl);
        if (cachedRequestId != null) {
            ConnectionState cachedState = new ConnectionState(normalizedUrl, cachedRequestId);
            if (CONNECTIONS_BY_REQUEST_ID.putIfAbsent(cachedRequestId, cachedState) == null) {
                return cachedState;
            }
        }

        while (true) {
            String requestId = createRequestId();
            ConnectionState state = new ConnectionState(normalizedUrl, requestId);
            if (CONNECTIONS_BY_REQUEST_ID.putIfAbsent(requestId, state) == null) {
                CACHED_REQUEST_IDS_BY_URL.put(normalizedUrl, requestId);
                return state;
            }
        }
    }

    @NotNull
    private static String createRequestId() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private static void closeConnectionState(@NotNull ConnectionState state) {
        WebSocket socketToClose;
        boolean notifyImmediately = false;

        synchronized (state.lock) {
            state.reconnectRequested = false;
            state.intentionallyClosing = true;
            state.removeAfterClose = true;
            state.pendingPayloads.clear();

            socketToClose = state.webSocket;
            if (!isSocketOpen(socketToClose)) {
                state.connectionGeneration++;
                state.connecting = false;
                state.webSocket = null;
                state.nextReconnectAttemptAtMillis = 0L;
                notifyImmediately = true;
            }
        }

        if (notifyImmediately) {
            deregisterState(state);
            notifyConnectionClosed(state.requestId, state.remoteServerUrl, true, false, false);
            return;
        }

        Objects.requireNonNull(socketToClose).sendClose(WebSocket.NORMAL_CLOSURE, "fancymenu_close_action").exceptionally(ex -> {
            LOGGER.warn("[FANCYMENU] Failed to gracefully close remote server connection. Aborting socket: {}", state.remoteServerUrl, ex);
            socketToClose.abort();
            return null;
        });
    }

    private static void connectIfNeededAndFlush(@NotNull ConnectionState state) {
        WebSocket activeSocket;
        boolean shouldConnect = false;
        long generation = 0L;

        synchronized (state.lock) {
            activeSocket = state.webSocket;
            if (!isSocketOpen(activeSocket) && !state.connecting && state.reconnectRequested) {
                state.connecting = true;
                state.intentionallyClosing = false;
                state.removeAfterClose = false;
                generation = ++state.connectionGeneration;
                shouldConnect = true;
            }
        }

        if (isSocketOpen(activeSocket)) {
            flushPendingPayloads(state, Objects.requireNonNull(activeSocket));
            return;
        }

        if (!shouldConnect) {
            return;
        }

        URI uri;
        try {
            uri = URI.create(state.remoteServerUrl);
        } catch (Exception ex) {
            onConnectionAttemptFailed(state, generation, ex);
            return;
        }

        long finalGeneration = generation;
        HTTP_CLIENT.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .buildAsync(uri, new RemoteWebSocketListener(state, generation))
                .whenComplete((socket, throwable) -> {
                    if (throwable != null) {
                        onConnectionAttemptFailed(state, finalGeneration, throwable);
                    }
                });
    }

    private static void onConnectionAttemptFailed(@NotNull ConnectionState state, long generation, @NotNull Throwable throwable) {
        synchronized (state.lock) {
            if (generation != state.connectionGeneration) {
                return;
            }
            state.connecting = false;
            state.webSocket = null;
            if (state.reconnectRequested) {
                state.nextReconnectAttemptAtMillis = System.currentTimeMillis() + RECONNECT_DELAY_MILLIS;
            }
        }
    }

    private static void flushPendingPayloads(@NotNull ConnectionState state, @NotNull WebSocket socket) {
        if (!isSocketOpen(socket)) {
            return;
        }

        String payload;
        while ((payload = state.pendingPayloads.poll()) != null) {
            sendPayload(state, socket, payload);
        }
    }

    private static void sendPayload(@NotNull ConnectionState state, @NotNull WebSocket socket, @NotNull String payload) {
        final long generation;
        synchronized (state.lock) {
            generation = state.connectionGeneration;
        }

        String outgoing = buildOutgoingMessage(state.requestId, payload);
        socket.sendText(outgoing, true).exceptionally(ex -> {
            state.pendingPayloads.add(payload);
            onSocketCrashed(state, generation, "send_failed", ex);
            return null;
        });
    }

    @NotNull
    private static String buildOutgoingMessage(@NotNull String requestId, @NotNull String payload) {
        return "request_id=" + requestId + "\n" + payload;
    }

    @NotNull
    private static IncomingMessage parseIncomingMessage(@NotNull ConnectionState state, @NotNull String rawMessage) {
        if (rawMessage.startsWith("request_id=")) {
            int lineBreak = rawMessage.indexOf('\n');
            if (lineBreak > "request_id=".length()) {
                String parsedRequestId = rawMessage.substring("request_id=".length(), lineBreak).trim();
                if (!parsedRequestId.isBlank()) {
                    return new IncomingMessage(parsedRequestId, rawMessage.substring(lineBreak + 1));
                }
            }
        }
        return new IncomingMessage(state.requestId, rawMessage);
    }

    private static void runHeartbeatAndReconnectTick() {
        long now = System.currentTimeMillis();

        for (ConnectionState state : CONNECTIONS_BY_URL.values()) {
            WebSocket socket;
            long generation;
            boolean shouldReconnect = false;
            boolean shouldSendPing = false;
            boolean shouldCrashForTimeout = false;

            synchronized (state.lock) {
                socket = state.webSocket;
                generation = state.connectionGeneration;

                if (isSocketOpen(socket)) {
                    if (now - state.lastHeartbeatPingMillis >= HEARTBEAT_PING_INTERVAL_MILLIS) {
                        state.lastHeartbeatPingMillis = now;
                        shouldSendPing = true;
                    }
                    if (state.lastHeartbeatPongMillis > 0L && now - state.lastHeartbeatPongMillis >= HEARTBEAT_PONG_TIMEOUT_MILLIS) {
                        shouldCrashForTimeout = true;
                    }
                } else if (state.reconnectRequested && !state.connecting && now >= state.nextReconnectAttemptAtMillis) {
                    shouldReconnect = true;
                }
            }

            if (shouldCrashForTimeout) {
                onSocketCrashed(state, generation, "heartbeat_timeout", null);
                continue;
            }

            if (shouldSendPing && isSocketOpen(socket)) {
                Objects.requireNonNull(socket).sendPing(ByteBuffer.wrap(HEARTBEAT_PING_DATA)).exceptionally(ex -> {
                    onSocketCrashed(state, generation, "heartbeat_ping_failed", ex);
                    return null;
                });
            }

            if (shouldReconnect) {
                connectIfNeededAndFlush(state);
            }
        }
    }

    private static boolean onSocketOpened(@NotNull ConnectionState state, long generation, @NotNull WebSocket socket) {
        boolean restoredAfterCrash;
        synchronized (state.lock) {
            if (generation != state.connectionGeneration || !state.reconnectRequested || state.removeAfterClose) {
                socket.abort();
                return false;
            }
            restoredAfterCrash = state.awaitingCrashRecoveryLog;
            state.webSocket = socket;
            state.connecting = false;
            state.intentionallyClosing = false;
            state.lastHeartbeatPongMillis = System.currentTimeMillis();
            state.lastHeartbeatPingMillis = 0L;
            state.nextReconnectAttemptAtMillis = 0L;
            state.awaitingCrashRecoveryLog = false;
        }

        if (restoredAfterCrash) {
            LOGGER.info("[FANCYMENU] Restored crashed remote server connection: {} (request_id={})", state.remoteServerUrl, state.requestId);
        }

        notifyConnected(state.requestId, state.remoteServerUrl);
        flushPendingPayloads(state, socket);
        return true;
    }

    private static void onSocketTextReceived(@NotNull ConnectionState state, long generation, @NotNull String rawMessage) {
        synchronized (state.lock) {
            if (generation != state.connectionGeneration) {
                return;
            }
            state.lastHeartbeatPongMillis = System.currentTimeMillis();
        }

        IncomingMessage incoming = parseIncomingMessage(state, rawMessage);
        notifyDataReceived(incoming.requestId(), state.remoteServerUrl, incoming.data());
    }

    private static void onSocketPongReceived(@NotNull ConnectionState state, long generation) {
        synchronized (state.lock) {
            if (generation != state.connectionGeneration) {
                return;
            }
            state.lastHeartbeatPongMillis = System.currentTimeMillis();
        }
    }

    private static void onSocketClosed(@NotNull ConnectionState state, long generation, int statusCode, @NotNull String reason) {
        boolean intentionalClose;
        boolean crashed;
        boolean unknownCloseReason;
        boolean removeStateAfterClose;

        synchronized (state.lock) {
            if (generation != state.connectionGeneration) {
                return;
            }

            state.webSocket = null;
            state.connecting = false;
            state.lastHeartbeatPongMillis = 0L;
            state.lastHeartbeatPingMillis = 0L;

            intentionalClose = state.intentionallyClosing || state.removeAfterClose || !state.reconnectRequested;
            crashed = !intentionalClose && statusCode != WebSocket.NORMAL_CLOSURE;
            unknownCloseReason = !intentionalClose && !crashed;
            removeStateAfterClose = state.removeAfterClose || !state.reconnectRequested;

            if (!intentionalClose && state.reconnectRequested) {
                state.nextReconnectAttemptAtMillis = System.currentTimeMillis() + RECONNECT_DELAY_MILLIS;
            }
            if (crashed) {
                state.awaitingCrashRecoveryLog = true;
            } else if (intentionalClose) {
                state.awaitingCrashRecoveryLog = false;
            }

            state.intentionallyClosing = false;
            state.removeAfterClose = false;
        }

        if (removeStateAfterClose) {
            deregisterState(state);
        }

        if (crashed) {
            LOGGER.warn("[FANCYMENU] Remote server connection crashed. URL={}, statusCode={}, reason={}", state.remoteServerUrl, statusCode, reason);
        }

        notifyConnectionClosed(state.requestId, state.remoteServerUrl, intentionalClose, crashed, unknownCloseReason);
    }

    private static void onSocketCrashed(@NotNull ConnectionState state, long generation, @NotNull String crashType, @Nullable Throwable throwable) {
        WebSocket socketToAbort = null;
        boolean intentionalClose = false;
        boolean removeStateAfterClose = false;
        boolean shouldReconnect = false;
        boolean shouldNotify = false;

        synchronized (state.lock) {
            if (generation != state.connectionGeneration) {
                return;
            }

            socketToAbort = state.webSocket;
            state.webSocket = null;
            state.connecting = false;
            state.lastHeartbeatPongMillis = 0L;
            state.lastHeartbeatPingMillis = 0L;
            intentionalClose = state.intentionallyClosing || state.removeAfterClose || !state.reconnectRequested;
            shouldReconnect = !intentionalClose && state.reconnectRequested;
            state.nextReconnectAttemptAtMillis = shouldReconnect ? System.currentTimeMillis() + RECONNECT_DELAY_MILLIS : 0L;
            removeStateAfterClose = state.removeAfterClose || !state.reconnectRequested;
            if (!intentionalClose) {
                state.awaitingCrashRecoveryLog = true;
            } else {
                state.awaitingCrashRecoveryLog = false;
            }
            state.intentionallyClosing = false;
            state.removeAfterClose = false;
            state.connectionGeneration++;
            shouldNotify = true;
        }

        if (socketToAbort != null) {
            socketToAbort.abort();
        }

        if (removeStateAfterClose) {
            deregisterState(state);
        }

        if (!intentionalClose) {
            if (throwable != null) {
                LOGGER.warn("[FANCYMENU] Remote server connection crashed ({}). URL={}", crashType, state.remoteServerUrl, throwable);
            } else {
                LOGGER.warn("[FANCYMENU] Remote server connection crashed ({}). URL={}", crashType, state.remoteServerUrl);
            }
        }

        if (shouldNotify) {
            notifyConnectionClosed(state.requestId, state.remoteServerUrl, intentionalClose, !intentionalClose, false);
        }
    }

    private static void notifyConnected(@NotNull String requestId, @NotNull String remoteServerUrl) {
        MainThreadTaskExecutor.executeInMainThread(
                () -> Listeners.ON_REMOTE_SERVER_CONNECTED.onRemoteServerConnected(requestId, remoteServerUrl),
                MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK
        );
    }

    private static void notifyDataReceived(@NotNull String requestId, @NotNull String remoteServerUrl, @NotNull String data) {
        MainThreadTaskExecutor.executeInMainThread(
                () -> Listeners.ON_REMOTE_SERVER_DATA_RECEIVED.onRemoteServerDataReceived(requestId, remoteServerUrl, data),
                MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK
        );
    }

    private static void notifyConnectionClosed(@NotNull String requestId, @NotNull String remoteServerUrl, boolean intentionallyClosed, boolean crashed, boolean unknownCloseReason) {
        MainThreadTaskExecutor.executeInMainThread(
                () -> Listeners.ON_REMOTE_SERVER_CONNECTION_CLOSED.onRemoteServerConnectionClosed(requestId, remoteServerUrl, intentionallyClosed, crashed, unknownCloseReason),
                MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK
        );
    }

    private static void deregisterState(@NotNull ConnectionState state) {
        CONNECTIONS_BY_URL.remove(state.remoteServerUrl, state);
        CONNECTIONS_BY_REQUEST_ID.remove(state.requestId, state);
    }

    private static boolean isSocketOpen(@Nullable WebSocket socket) {
        return socket != null && !socket.isInputClosed() && !socket.isOutputClosed();
    }

    private static final class ConnectionState {

        private final Object lock = new Object();
        private final Queue<String> pendingPayloads = new ConcurrentLinkedQueue<>();
        private final String remoteServerUrl;
        private final String requestId;

        private @Nullable WebSocket webSocket;
        private boolean connecting;
        private boolean reconnectRequested = true;
        private boolean intentionallyClosing;
        private boolean removeAfterClose;
        private boolean awaitingCrashRecoveryLog;
        private long nextReconnectAttemptAtMillis;
        private long lastHeartbeatPingMillis;
        private long lastHeartbeatPongMillis;
        private long connectionGeneration;

        private ConnectionState(@NotNull String remoteServerUrl, @NotNull String requestId) {
            this.remoteServerUrl = remoteServerUrl;
            this.requestId = requestId;
            this.nextReconnectAttemptAtMillis = System.currentTimeMillis();
        }
    }

    private record IncomingMessage(@NotNull String requestId, @NotNull String data) {
    }

    private static final class RemoteWebSocketListener implements WebSocket.Listener {

        private final ConnectionState state;
        private final long generation;
        private final StringBuilder textBuffer = new StringBuilder();

        private RemoteWebSocketListener(@NotNull ConnectionState state, long generation) {
            this.state = state;
            this.generation = generation;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            if (onSocketOpened(this.state, this.generation, webSocket)) {
                webSocket.request(1);
            }
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            this.textBuffer.append(data);
            if (last) {
                String completedMessage = this.textBuffer.toString();
                this.textBuffer.setLength(0);
                onSocketTextReceived(this.state, this.generation, completedMessage);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            onSocketPongReceived(this.state, this.generation);
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            onSocketClosed(this.state, this.generation, statusCode, Objects.requireNonNullElse(reason, ""));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            onSocketCrashed(this.state, this.generation, "socket_error", error);
        }
    }

}
