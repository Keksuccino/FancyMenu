package de.keksuccino.fancymenu.customization.remote;

import de.keksuccino.fancymenu.customization.listener.RevisionSafeListenerDispatch;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class RemoteServerConnectionManager {

    private static final Logger LOGGER = LogManager.getLogger();

    static final int MAX_INBOUND_MESSAGE_UTF8_BYTES = 1024 * 1024;
    static final int MAX_INBOUND_MESSAGE_FRAGMENTS = 1024;
    static final int MAX_OUTBOUND_MESSAGE_UTF8_BYTES = 1024 * 1024;
    static final int MAX_PENDING_OUTBOUND_COUNT_PER_CONNECTION = 128;
    static final long MAX_PENDING_OUTBOUND_UTF8_BYTES_PER_CONNECTION = 2L * 1024L * 1024L;
    static final int MAX_PENDING_INBOUND_DELIVERY_COUNT_PER_CONNECTION = 64;
    static final long MAX_PENDING_INBOUND_DELIVERY_UTF8_BYTES_PER_CONNECTION = 2L * 1024L * 1024L;
    static final int MAX_ACTIVE_CONNECTION_STATES = 32;
    static final int MAX_CACHED_REQUEST_IDS = 256;
    static final int MAX_REMOTE_SERVER_URL_UTF8_BYTES = 8 * 1024;

    private static final int REQUEST_ID_LENGTH = 64;
    private static final int OUTGOING_MESSAGE_ENVELOPE_UTF8_BYTES = "request_id=".length() + REQUEST_ID_LENGTH + 1;
    private static final int MAX_INBOUND_DELIVERIES_PER_DRAIN = 256;
    private static final int NORMAL_CLOSURE_STATUS = 1000;
    private static final long RECONNECT_DELAY_MILLIS = 10_000L;
    private static final long QUEUED_PAYLOAD_MAX_AGE_MILLIS = 30_000L;
    private static final long HEARTBEAT_TICK_INTERVAL_MILLIS = 5_000L;
    private static final long HEARTBEAT_PING_INTERVAL_MILLIS = 20_000L;
    private static final long HEARTBEAT_PONG_TIMEOUT_MILLIS = 70_000L;
    private static final long REJECTION_LOG_INTERVAL_MILLIS = 5_000L;
    private static final byte[] HEARTBEAT_PING_DATA = "fm_remote_ping".getBytes(StandardCharsets.UTF_8);

    private static final RemoteWebSocketTransport TRANSPORT = new JdkRemoteWebSocketTransport(MAX_INBOUND_MESSAGE_UTF8_BYTES, MAX_INBOUND_MESSAGE_FRAGMENTS);
    private static final BoundedConnectionRegistry<ConnectionState> CONNECTIONS = new BoundedConnectionRegistry<>(MAX_ACTIVE_CONNECTION_STATES, MAX_CACHED_REQUEST_IDS);
    private static final CoalescingTaskGate INBOUND_DELIVERY_DRAIN_GATE = new CoalescingTaskGate();
    private static final AtomicLong LAST_REJECTION_LOG_MILLIS = new AtomicLong(Long.MIN_VALUE);

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu-RemoteServerConnectionManager");
        thread.setDaemon(true);
        return thread;
    });

    static {
        EXECUTOR.scheduleAtFixedRate(RemoteServerConnectionManager::runHeartbeatAndReconnectTick, HEARTBEAT_TICK_INTERVAL_MILLIS, HEARTBEAT_TICK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    private RemoteServerConnectionManager() {
    }

    public static void sendData(@NotNull String remoteServerUrl, @Nullable String data) {
        String payload = Objects.requireNonNullElse(data, "");
        PendingPayloadBuffer.PayloadValidation validation = PendingPayloadBuffer.validatePayload(payload, MAX_OUTBOUND_MESSAGE_UTF8_BYTES, OUTGOING_MESSAGE_ENVELOPE_UTF8_BYTES);
        if (validation.result() != PendingPayloadBuffer.AdmissionResult.ACCEPTED) {
            logOutboundRejection(null, validation.result());
            return;
        }

        ConnectionState state = getOrCreateConnectionState(remoteServerUrl);
        if (state == null) {
            return;
        }

        PendingPayloadBuffer.AdmissionResult admissionResult;
        synchronized (state.lock) {
            if (!state.registered || !state.reconnectRequested) {
                return;
            }
            admissionResult = state.pendingPayloads.offer(validation, System.currentTimeMillis());
        }
        if (admissionResult != PendingPayloadBuffer.AdmissionResult.ACCEPTED) {
            logOutboundRejection(state, admissionResult);
            return;
        }
        connectIfNeededAndFlush(state);
    }

    public static void connect(@NotNull String remoteServerUrl) {
        ConnectionState state = getOrCreateConnectionState(remoteServerUrl);
        if (state != null) {
            connectIfNeededAndFlush(state);
        }
    }

    public static void closeAllConnections() {
        for (ConnectionState state : CONNECTIONS.snapshot()) {
            closeConnectionState(state);
        }
    }

    public static void closeConnectionByRequestId(@NotNull String requestId) {
        String trimmedRequestId = requestId.trim();
        if (trimmedRequestId.isBlank()) {
            return;
        }
        ConnectionState state = CONNECTIONS.getByRequestId(trimmedRequestId);
        if (state != null) {
            closeConnectionState(state);
        }
    }

    static int activeConnectionStateCount() {
        return CONNECTIONS.activeStateCount();
    }

    @Nullable
    private static String normalizeRemoteServerUrl(@Nullable String rawUrl) {
        if (rawUrl == null) {
            return null;
        }

        String candidate = rawUrl.trim();
        long candidateUtf8Bytes = Utf8Length.count(candidate);
        if (candidate.isBlank() || candidateUtf8Bytes == Utf8Length.MALFORMED_UTF16 || candidateUtf8Bytes > MAX_REMOTE_SERVER_URL_UTF8_BYTES) {
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
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
            if ((!"ws".equals(scheme) && !"wss".equals(scheme)) || uri.getHost() == null || uri.getHost().isBlank()) {
                return null;
            }
            String normalized = uri.toString();
            return Utf8Length.count(normalized) <= MAX_REMOTE_SERVER_URL_UTF8_BYTES ? normalized : null;
        } catch (Exception ex) {
            return null;
        }
    }

    @Nullable
    private static ConnectionState getOrCreateConnectionState(@Nullable String remoteServerUrl) {
        String normalizedUrl = normalizeRemoteServerUrl(remoteServerUrl);
        if (normalizedUrl == null) {
            logRejectedRequest("Ignoring remote server request due to an invalid or oversized URL");
            return null;
        }

        while (true) {
            BoundedConnectionRegistry.Admission<ConnectionState> admission = CONNECTIONS.getOrCreate(normalizedUrl, RemoteServerConnectionManager::createRequestId, ConnectionState::new);
            if (admission.type() == BoundedConnectionRegistry.AdmissionType.CAPACITY_EXCEEDED) {
                logRejectedRequest("Ignoring remote server request because the active connection-state limit of " + MAX_ACTIVE_CONNECTION_STATES + " has been reached");
                return null;
            }
            if (admission.type() == BoundedConnectionRegistry.AdmissionType.REQUEST_ID_EXHAUSTED) {
                logRejectedRequest("Ignoring remote server request because a unique request ID could not be allocated");
                return null;
            }

            ConnectionState state = Objects.requireNonNull(admission.state());
            synchronized (state.lock) {
                if (!state.registered) {
                    continue;
                }
                state.reconnectRequested = true;
                if (state.intentionallyClosing) {
                    // A new action may legitimately reuse an endpoint while its previous close frame is still flushing.
                    // Keep the state registered and reconnect only after the old channel reaches its terminal callback.
                    state.removeAfterClose = false;
                }
                if (state.nextReconnectAttemptAtMillis <= 0L) {
                    state.nextReconnectAttemptAtMillis = System.currentTimeMillis();
                }
                return state;
            }
        }
    }

    @NotNull
    private static String createRequestId() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private static void closeConnectionState(@NotNull ConnectionState state) {
        RemoteWebSocketTransport.Connection connectionToClose;
        boolean closeGracefully;
        boolean notifyImmediately;

        synchronized (state.lock) {
            if (!state.registered) {
                return;
            }
            state.reconnectRequested = false;
            state.intentionallyClosing = true;
            state.removeAfterClose = true;
            state.pendingPayloads.clear();
            state.inboundDeliveries.clear();

            connectionToClose = state.connection;
            closeGracefully = isConnectionOpen(connectionToClose);
            notifyImmediately = !closeGracefully;
            if (notifyImmediately) {
                state.connectionGeneration++;
                state.connecting = false;
                state.connection = null;
                state.nextReconnectAttemptAtMillis = 0L;
                state.intentionallyClosing = false;
                state.removeAfterClose = false;
                deregisterStateLocked(state);
            }
        }

        if (notifyImmediately) {
            if (connectionToClose != null) {
                connectionToClose.abort();
            }
            notifyConnectionClosed(state.requestId, state.remoteServerUrl, true, false, false);
            return;
        }

        Objects.requireNonNull(connectionToClose).close(NORMAL_CLOSURE_STATUS, "fancymenu_close_action").exceptionally(throwable -> {
            LOGGER.warn("[FANCYMENU] Failed to gracefully close remote server connection. Aborting socket: {}", state.remoteServerUrl, throwable);
            connectionToClose.abort();
            return null;
        });
    }

    private static void connectIfNeededAndFlush(@NotNull ConnectionState state) {
        RemoteWebSocketTransport.Connection activeConnection;
        boolean shouldConnect = false;
        long generation = 0L;

        synchronized (state.lock) {
            if (!state.registered) {
                return;
            }
            activeConnection = state.connection;
            if (!isConnectionOpen(activeConnection) && !state.connecting && !state.intentionallyClosing && state.reconnectRequested) {
                state.connecting = true;
                state.removeAfterClose = false;
                generation = ++state.connectionGeneration;
                shouldConnect = true;
            }
        }

        if (isConnectionOpen(activeConnection)) {
            flushPendingPayloads(state, Objects.requireNonNull(activeConnection));
            return;
        }
        if (!shouldConnect) {
            return;
        }

        URI uri;
        try {
            uri = URI.create(state.remoteServerUrl);
        } catch (Exception ex) {
            onConnectionAttemptFailed(state, generation, null, ex);
            return;
        }

        RemoteWebSocketTransport.Connection connection = TRANSPORT.connect(uri, new TransportListener(state, generation));
        boolean abortConnection = false;
        synchronized (state.lock) {
            if (!state.registered || generation != state.connectionGeneration || (!state.connecting && state.connection != connection)) {
                abortConnection = true;
            } else if (state.connection == null) {
                state.connection = connection;
            } else if (state.connection != connection) {
                abortConnection = true;
            }
        }
        if (abortConnection) {
            connection.abort();
        }
    }

    private static void onConnectionAttemptFailed(@NotNull ConnectionState state, long generation, @Nullable RemoteWebSocketTransport.Connection connection, @NotNull Throwable throwable) {
        synchronized (state.lock) {
            if (!state.registered || generation != state.connectionGeneration || (state.connection != null && state.connection != connection)) {
                return;
            }
            state.connecting = false;
            state.connection = null;
            if (state.reconnectRequested) {
                state.nextReconnectAttemptAtMillis = System.currentTimeMillis() + RECONNECT_DELAY_MILLIS;
            }
        }
    }

    private static void flushPendingPayloads(@NotNull ConnectionState state, @NotNull RemoteWebSocketTransport.Connection connection) {
        PendingPayloadBuffer.Payload payload;
        long generation;
        synchronized (state.lock) {
            if (!state.registered || state.connection != connection || !isConnectionOpen(connection)) {
                return;
            }
            payload = state.pendingPayloads.pollForSend(System.currentTimeMillis());
            if (payload == null) {
                return;
            }
            generation = state.connectionGeneration;
        }

        CompletableFuture<Void> sendFuture;
        try {
            sendFuture = connection.sendText(buildOutgoingMessage(state.requestId, payload.payload()));
        } catch (Throwable throwable) {
            handleSendCompletion(state, connection, generation, payload, throwable);
            return;
        }
        sendFuture.whenComplete((ignored, throwable) -> handleSendCompletion(state, connection, generation, payload, throwable));
    }

    private static void handleSendCompletion(@NotNull ConnectionState state, @NotNull RemoteWebSocketTransport.Connection connection, long generation, @NotNull PendingPayloadBuffer.Payload payload, @Nullable Throwable throwable) {
        if (throwable == null) {
            boolean flushAgain;
            synchronized (state.lock) {
                if (!state.pendingPayloads.completeSend(payload)) {
                    return;
                }
                flushAgain = state.registered && state.connectionGeneration == generation && state.connection == connection && isConnectionOpen(connection);
            }
            if (flushAgain) {
                scheduleSendPump(state, connection, generation);
            }
            return;
        }

        boolean crashConnection;
        synchronized (state.lock) {
            boolean retry = state.registered && state.connectionGeneration == generation && state.connection == connection && state.reconnectRequested && !state.removeAfterClose;
            boolean handled = state.pendingPayloads.retryOrDiscardSend(payload, System.currentTimeMillis(), retry);
            crashConnection = handled && retry;
        }
        if (crashConnection) {
            onSocketCrashed(state, generation, connection, "send_failed", throwable);
        }
    }

    private static void scheduleSendPump(@NotNull ConnectionState state, @NotNull RemoteWebSocketTransport.Connection connection, long generation) {
        synchronized (state.lock) {
            if (state.sendPumpScheduled || !state.registered || state.connectionGeneration != generation || state.connection != connection) {
                return;
            }
            state.sendPumpScheduled = true;
        }
        try {
            EXECUTOR.execute(() -> {
                synchronized (state.lock) {
                    state.sendPumpScheduled = false;
                    if (!state.registered || state.connectionGeneration != generation || state.connection != connection) {
                        return;
                    }
                }
                flushPendingPayloads(state, connection);
            });
        } catch (RuntimeException ex) {
            synchronized (state.lock) {
                state.sendPumpScheduled = false;
            }
            onSocketCrashed(state, generation, connection, "send_pump_rejected", ex);
        }
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

        for (ConnectionState state : CONNECTIONS.snapshot()) {
            state.pendingPayloads.pruneExpiredQueued(now);

            RemoteWebSocketTransport.Connection connection;
            long generation;
            boolean shouldReconnect = false;
            boolean shouldSendPing = false;
            boolean shouldCrashForTimeout = false;

            synchronized (state.lock) {
                if (!state.registered) {
                    continue;
                }
                connection = state.connection;
                generation = state.connectionGeneration;

                if (isConnectionOpen(connection)) {
                    if (now - state.lastHeartbeatPingMillis >= HEARTBEAT_PING_INTERVAL_MILLIS) {
                        state.lastHeartbeatPingMillis = now;
                        shouldSendPing = true;
                    }
                    if (state.lastHeartbeatPongMillis > 0L && now - state.lastHeartbeatPongMillis >= HEARTBEAT_PONG_TIMEOUT_MILLIS) {
                        shouldCrashForTimeout = true;
                    }
                } else if (state.reconnectRequested && !state.connecting && !state.intentionallyClosing && now >= state.nextReconnectAttemptAtMillis) {
                    shouldReconnect = true;
                }
            }

            if (shouldCrashForTimeout && connection != null) {
                onSocketCrashed(state, generation, connection, "heartbeat_timeout", null);
                continue;
            }
            if (shouldSendPing && isConnectionOpen(connection)) {
                Objects.requireNonNull(connection).sendPing(HEARTBEAT_PING_DATA).exceptionally(throwable -> {
                    onSocketCrashed(state, generation, connection, "heartbeat_ping_failed", throwable);
                    return null;
                });
            }
            if (shouldReconnect) {
                connectIfNeededAndFlush(state);
            }
        }
    }

    private static boolean onSocketOpened(@NotNull ConnectionState state, long generation, @NotNull RemoteWebSocketTransport.Connection connection) {
        boolean restoredAfterCrash;
        synchronized (state.lock) {
            if (!state.registered || generation != state.connectionGeneration || !state.reconnectRequested || state.intentionallyClosing || state.removeAfterClose || (state.connection != null && state.connection != connection)) {
                return false;
            }
            restoredAfterCrash = state.awaitingCrashRecoveryLog;
            state.connection = connection;
            state.connecting = false;
            state.lastHeartbeatPongMillis = System.currentTimeMillis();
            state.lastHeartbeatPingMillis = 0L;
            state.nextReconnectAttemptAtMillis = 0L;
            state.awaitingCrashRecoveryLog = false;
        }

        if (restoredAfterCrash) {
            LOGGER.info("[FANCYMENU] Restored crashed remote server connection: {} (request_id={})", state.remoteServerUrl, state.requestId);
        }
        notifyConnected(state.requestId, state.remoteServerUrl);
        flushPendingPayloads(state, connection);
        return true;
    }

    private static void onSocketTextReceived(@NotNull ConnectionState state, long generation, @NotNull RemoteWebSocketTransport.Connection connection, @NotNull String rawMessage, int rawMessageUtf8Bytes) {
        synchronized (state.lock) {
            if (!state.registered || generation != state.connectionGeneration || state.connection != connection || !isConnectionOpen(connection)) {
                return;
            }
            state.lastHeartbeatPongMillis = System.currentTimeMillis();
        }

        long listenerRevision = Listeners.ON_REMOTE_SERVER_DATA_RECEIVED.getActiveInstanceRevision();
        if (listenerRevision < 0L) {
            return;
        }
        IncomingMessage incoming = parseIncomingMessage(state, rawMessage);

        boolean admitted;
        synchronized (state.lock) {
            if (!state.registered || generation != state.connectionGeneration || state.connection != connection || !isConnectionOpen(connection)) {
                return;
            }
            admitted = state.inboundDeliveries.offer(new InboundDelivery(incoming, listenerRevision), rawMessageUtf8Bytes);
        }
        if (admitted) {
            scheduleInboundDeliveryDrain();
        } else {
            logRejectedRequest("Dropping newest remote WebSocket message because its main-thread delivery queue is full for " + state.remoteServerUrl);
        }
    }

    private static void onSocketPongReceived(@NotNull ConnectionState state, long generation, @NotNull RemoteWebSocketTransport.Connection connection) {
        synchronized (state.lock) {
            if (state.registered && generation == state.connectionGeneration && state.connection == connection) {
                state.lastHeartbeatPongMillis = System.currentTimeMillis();
            }
        }
    }

    private static void onSocketClosed(@NotNull ConnectionState state, long generation, @NotNull RemoteWebSocketTransport.Connection connection, int statusCode, @NotNull String reason) {
        boolean intentionalClose;
        boolean crashed;
        boolean unknownCloseReason;

        synchronized (state.lock) {
            if (!state.registered || generation != state.connectionGeneration || state.connection != connection) {
                return;
            }

            state.connection = null;
            state.connecting = false;
            state.pendingPayloads.discardInFlight();
            state.lastHeartbeatPongMillis = 0L;
            state.lastHeartbeatPingMillis = 0L;

            intentionalClose = state.intentionallyClosing || state.removeAfterClose || !state.reconnectRequested;
            crashed = !intentionalClose && statusCode != NORMAL_CLOSURE_STATUS;
            unknownCloseReason = !intentionalClose && !crashed;
            boolean removeStateAfterClose = state.removeAfterClose || !state.reconnectRequested;

            if (!removeStateAfterClose && state.reconnectRequested) {
                state.nextReconnectAttemptAtMillis = intentionalClose ? System.currentTimeMillis() : System.currentTimeMillis() + RECONNECT_DELAY_MILLIS;
            }
            state.awaitingCrashRecoveryLog = crashed;
            state.intentionallyClosing = false;
            state.removeAfterClose = false;
            state.connectionGeneration++;
            if (removeStateAfterClose) {
                state.inboundDeliveries.clear();
                deregisterStateLocked(state);
            }
        }

        if (crashed) {
            LOGGER.warn("[FANCYMENU] Remote server connection crashed. URL={}, statusCode={}, reason={}", state.remoteServerUrl, statusCode, reason);
        }
        notifyConnectionClosed(state.requestId, state.remoteServerUrl, intentionalClose, crashed, unknownCloseReason);
    }

    private static void onSocketCrashed(@NotNull ConnectionState state, long generation, @NotNull RemoteWebSocketTransport.Connection connection, @NotNull String crashType, @Nullable Throwable throwable) {
        boolean intentionalClose;

        synchronized (state.lock) {
            if (!state.registered || generation != state.connectionGeneration || state.connection != connection) {
                return;
            }

            state.connection = null;
            state.connecting = false;
            state.pendingPayloads.discardInFlight();
            state.lastHeartbeatPongMillis = 0L;
            state.lastHeartbeatPingMillis = 0L;
            intentionalClose = state.intentionallyClosing || state.removeAfterClose || !state.reconnectRequested;
            boolean removeStateAfterClose = state.removeAfterClose || !state.reconnectRequested;
            boolean shouldReconnect = !intentionalClose && state.reconnectRequested;
            state.nextReconnectAttemptAtMillis = shouldReconnect ? System.currentTimeMillis() + RECONNECT_DELAY_MILLIS : 0L;
            state.awaitingCrashRecoveryLog = !intentionalClose;
            state.intentionallyClosing = false;
            state.removeAfterClose = false;
            state.connectionGeneration++;
            if (removeStateAfterClose) {
                state.inboundDeliveries.clear();
                deregisterStateLocked(state);
            }
        }

        connection.abort();
        if (!intentionalClose) {
            if (throwable != null) {
                LOGGER.warn("[FANCYMENU] Remote server connection crashed ({}). URL={}", crashType, state.remoteServerUrl, throwable);
            } else {
                LOGGER.warn("[FANCYMENU] Remote server connection crashed ({}). URL={}", crashType, state.remoteServerUrl);
            }
        }
        notifyConnectionClosed(state.requestId, state.remoteServerUrl, intentionalClose, !intentionalClose, false);
    }

    private static void scheduleInboundDeliveryDrain() {
        if (!INBOUND_DELIVERY_DRAIN_GATE.tryAcquire()) {
            return;
        }
        try {
            MainThreadTaskExecutor.executeInMainThread(RemoteServerConnectionManager::drainInboundDeliveries, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        } catch (RuntimeException | Error throwable) {
            INBOUND_DELIVERY_DRAIN_GATE.release();
            throw throwable;
        }
    }

    private static void drainInboundDeliveries() {
        try {
            List<ConnectionState> states = CONNECTIONS.snapshot();
            int delivered = 0;
            boolean madeProgress;
            do {
                madeProgress = false;
                for (ConnectionState state : states) {
                    InboundDelivery delivery = state.inboundDeliveries.poll();
                    if (delivery == null) {
                        continue;
                    }
                    madeProgress = true;
                    delivered++;
                    if (Listeners.ON_REMOTE_SERVER_DATA_RECEIVED.isActiveAtRevision(delivery.listenerRevision())) {
                        IncomingMessage incoming = delivery.message();
                        Listeners.ON_REMOTE_SERVER_DATA_RECEIVED.onRemoteServerDataReceived(incoming.requestId(), state.remoteServerUrl, incoming.data());
                    }
                    if (delivered >= MAX_INBOUND_DELIVERIES_PER_DRAIN) {
                        return;
                    }
                }
            } while (madeProgress);
        } catch (Throwable throwable) {
            LOGGER.error("[FANCYMENU] Failed to deliver queued remote WebSocket messages on the main thread", throwable);
        } finally {
            INBOUND_DELIVERY_DRAIN_GATE.release();
            if (hasPendingInboundDeliveries()) {
                scheduleInboundDeliveryDrain();
            }
        }
    }

    private static boolean hasPendingInboundDeliveries() {
        for (ConnectionState state : CONNECTIONS.snapshot()) {
            if (!state.inboundDeliveries.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void notifyConnected(@NotNull String requestId, @NotNull String remoteServerUrl) {
        RevisionSafeListenerDispatch.scheduleIfActive(Listeners.ON_REMOTE_SERVER_CONNECTED, task -> MainThreadTaskExecutor.executeInMainThread(task, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK), () -> Listeners.ON_REMOTE_SERVER_CONNECTED.onRemoteServerConnected(requestId, remoteServerUrl));
    }

    private static void notifyConnectionClosed(@NotNull String requestId, @NotNull String remoteServerUrl, boolean intentionallyClosed, boolean crashed, boolean unknownCloseReason) {
        RevisionSafeListenerDispatch.scheduleIfActive(Listeners.ON_REMOTE_SERVER_CONNECTION_CLOSED, task -> MainThreadTaskExecutor.executeInMainThread(task, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK), () -> Listeners.ON_REMOTE_SERVER_CONNECTION_CLOSED.onRemoteServerConnectionClosed(requestId, remoteServerUrl, intentionallyClosed, crashed, unknownCloseReason));
    }

    private static void deregisterStateLocked(@NotNull ConnectionState state) {
        if (!Thread.holdsLock(state.lock)) {
            throw new IllegalStateException("Connection state must be locked while deregistering");
        }
        if (!state.registered) {
            return;
        }
        state.registered = false;
        CONNECTIONS.remove(state.remoteServerUrl, state.requestId, state);
    }

    private static boolean isConnectionOpen(@Nullable RemoteWebSocketTransport.Connection connection) {
        return connection != null && connection.isOpen();
    }

    private static void logOutboundRejection(@Nullable ConnectionState state, @NotNull PendingPayloadBuffer.AdmissionResult admissionResult) {
        String reason = switch (admissionResult) {
            case MALFORMED_UTF16 -> "the payload contains malformed UTF-16";
            case PAYLOAD_TOO_LARGE -> "the enveloped message exceeds " + MAX_OUTBOUND_MESSAGE_UTF8_BYTES + " UTF-8 bytes";
            case CAPACITY_EXCEEDED -> "the pending queue reached " + MAX_PENDING_OUTBOUND_COUNT_PER_CONNECTION + " messages or " + MAX_PENDING_OUTBOUND_UTF8_BYTES_PER_CONNECTION + " UTF-8 bytes";
            case ACCEPTED -> "an unknown admission error occurred";
        };
        String endpoint = state == null ? "" : " for " + state.remoteServerUrl;
        logRejectedRequest("Dropping newest remote WebSocket payload" + endpoint + " because " + reason);
    }

    private static void logRejectedRequest(@NotNull String message) {
        long now = System.currentTimeMillis();
        while (true) {
            long previous = LAST_REJECTION_LOG_MILLIS.get();
            if (previous != Long.MIN_VALUE && now >= previous && now - previous < REJECTION_LOG_INTERVAL_MILLIS) {
                return;
            }
            if (LAST_REJECTION_LOG_MILLIS.compareAndSet(previous, now)) {
                LOGGER.warn("[FANCYMENU] {}", message);
                return;
            }
        }
    }

    static final class ConnectionState {

        private final Object lock = new Object();
        private final PendingPayloadBuffer pendingPayloads = new PendingPayloadBuffer(MAX_PENDING_OUTBOUND_COUNT_PER_CONNECTION, MAX_PENDING_OUTBOUND_UTF8_BYTES_PER_CONNECTION, MAX_OUTBOUND_MESSAGE_UTF8_BYTES, OUTGOING_MESSAGE_ENVELOPE_UTF8_BYTES, QUEUED_PAYLOAD_MAX_AGE_MILLIS);
        private final InboundDeliveryBuffer<InboundDelivery> inboundDeliveries = new InboundDeliveryBuffer<>(MAX_PENDING_INBOUND_DELIVERY_COUNT_PER_CONNECTION, MAX_PENDING_INBOUND_DELIVERY_UTF8_BYTES_PER_CONNECTION);
        private final String remoteServerUrl;
        private final String requestId;

        private @Nullable RemoteWebSocketTransport.Connection connection;
        private boolean registered = true;
        private boolean connecting;
        private boolean sendPumpScheduled;
        private boolean reconnectRequested = true;
        private boolean intentionallyClosing;
        private boolean removeAfterClose;
        private boolean awaitingCrashRecoveryLog;
        private long nextReconnectAttemptAtMillis;
        private long lastHeartbeatPingMillis;
        private long lastHeartbeatPongMillis;
        private long connectionGeneration;

        ConnectionState(@NotNull String remoteServerUrl, @NotNull String requestId) {
            this.remoteServerUrl = remoteServerUrl;
            this.requestId = requestId;
            this.nextReconnectAttemptAtMillis = System.currentTimeMillis();
        }

        long currentGeneration() {
            synchronized (this.lock) {
                return this.connectionGeneration;
            }
        }

        @Nullable RemoteWebSocketTransport.Connection currentConnection() {
            synchronized (this.lock) {
                return this.connection;
            }
        }
    }

    private record IncomingMessage(@NotNull String requestId, @NotNull String data) {
    }

    private record InboundDelivery(@NotNull IncomingMessage message, long listenerRevision) {
    }

    record TransportListener(@NotNull ConnectionState state, long generation) implements RemoteWebSocketTransport.Listener {

        @Override
        public void onOpen(@NotNull RemoteWebSocketTransport.Connection connection) {
            if (!onSocketOpened(this.state, this.generation, connection)) {
                connection.abort();
            }
        }

        @Override
        public void onText(@NotNull RemoteWebSocketTransport.Connection connection, @NotNull String data, int utf8Bytes) {
            onSocketTextReceived(this.state, this.generation, connection, data, utf8Bytes);
        }

        @Override
        public void onPong(@NotNull RemoteWebSocketTransport.Connection connection) {
            onSocketPongReceived(this.state, this.generation, connection);
        }

        @Override
        public void onClose(@NotNull RemoteWebSocketTransport.Connection connection, int statusCode, @NotNull String reason) {
            onSocketClosed(this.state, this.generation, connection, statusCode, reason);
        }

        @Override
        public void onError(@NotNull RemoteWebSocketTransport.Connection connection, @NotNull Throwable error) {
            boolean connectionAttempt;
            synchronized (this.state.lock) {
                connectionAttempt = this.state.registered && this.generation == this.state.connectionGeneration && (this.state.connection == null || this.state.connection == connection) && this.state.connecting;
            }
            if (connectionAttempt) {
                onConnectionAttemptFailed(this.state, this.generation, connection, error);
            } else {
                onSocketCrashed(this.state, this.generation, connection, "socket_error", error);
            }
        }
    }
}
