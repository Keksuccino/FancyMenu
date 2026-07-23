package de.keksuccino.fancymenu.networking;

import com.google.common.collect.MapMaker;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Tracks server-side handshake negotiation per live play packet listener.
 * The caller keys this by {@code ServerPlayer.connection}: Minecraft keeps that listener when a respawn replaces the
 * player entity, while a genuine reconnect creates a new listener that must be allowed to negotiate independently.
 * Weak keys are important here: disconnect cleanup for FancyMenu's capability lists is a separate lifecycle concern,
 * and this defensive admission state must not retain old Minecraft connections on its own.
 */
final class ServerHandshakeNegotiationTracker {

    // A normal client sends exactly one handshake. This small burst still permits recovery from a few malformed packets
    // without allowing an unaccepted connection to deserialize and enqueue negotiation traffic indefinitely.
    static final int DEFAULT_MAX_UNACCEPTED_ATTEMPTS = 4;
    static final long DEFAULT_WINDOW_NANOS = TimeUnit.SECONDS.toNanos(10);

    // MapMaker's weak keys deliberately use identity equality. Value equality would conflate separate live connections,
    // while strong identity keys would retain every disconnected listener until an explicit lifecycle cleanup happened.
    private final Map<Object, ConnectionState> statesByConnection = new MapMaker().weakKeys().makeMap();
    private final int maxUnacceptedAttempts;
    private final long windowNanos;
    private final LongSupplier monotonicClock;

    ServerHandshakeNegotiationTracker() {
        this(System::nanoTime);
    }

    ServerHandshakeNegotiationTracker(@NotNull LongSupplier monotonicClock) {
        this(DEFAULT_MAX_UNACCEPTED_ATTEMPTS, DEFAULT_WINDOW_NANOS, monotonicClock);
    }

    ServerHandshakeNegotiationTracker(int maxUnacceptedAttempts, long windowNanos, @NotNull LongSupplier monotonicClock) {
        if (maxUnacceptedAttempts <= 0) throw new IllegalArgumentException("Maximum unaccepted attempts must be positive");
        if (windowNanos <= 0L) throw new IllegalArgumentException("Handshake attempt window must be positive");
        this.maxUnacceptedAttempts = maxUnacceptedAttempts;
        this.windowNanos = windowNanos;
        this.monotonicClock = Objects.requireNonNull(monotonicClock);
    }

    /**
     * Must run before extracting/deserializing the handshake body or creating a server task.
     */
    synchronized @NotNull Decision admitAttempt(@NotNull Object connection) {
        Objects.requireNonNull(connection);
        long now = this.monotonicClock.getAsLong();
        ConnectionState state = this.statesByConnection.computeIfAbsent(connection, ignored -> new ConnectionState(now));
        if (state.accepted) return this.reject(state, now);

        long elapsed = now - state.attemptWindowStartedAt;
        if (elapsed < 0L || elapsed >= this.windowNanos) {
            state.attemptWindowStartedAt = now;
            state.attempts = 0;
        }
        if (state.attempts >= this.maxUnacceptedAttempts) return this.reject(state, now);

        state.attempts++;
        return Decision.ALLOW;
    }

    /**
     * Atomically changes one live connection from negotiating to accepted. Already accepted handshakes are no-ops.
     */
    synchronized @NotNull Decision accept(@NotNull Object connection) {
        Objects.requireNonNull(connection);
        long now = this.monotonicClock.getAsLong();
        ConnectionState state = this.statesByConnection.computeIfAbsent(connection, ignored -> new ConnectionState(now));
        if (state.accepted) return this.reject(state, now);

        state.accepted = true;
        return Decision.ALLOW;
    }

    @NotNull
    private Decision reject(@NotNull ConnectionState state, long now) {
        long elapsed = now - state.lastWarningAt;
        if (!state.warningEmitted || elapsed < 0L || elapsed >= this.windowNanos) {
            state.warningEmitted = true;
            state.lastWarningAt = now;
            return Decision.REJECT_AND_WARN;
        }
        return Decision.REJECT;
    }

    enum Decision {
        ALLOW(true, false),
        REJECT(false, false),
        REJECT_AND_WARN(false, true);

        private final boolean allowed;
        private final boolean warningRequired;

        Decision(boolean allowed, boolean warningRequired) {
            this.allowed = allowed;
            this.warningRequired = warningRequired;
        }

        boolean isAllowed() {
            return this.allowed;
        }

        boolean isWarningRequired() {
            return this.warningRequired;
        }
    }

    private static final class ConnectionState {

        private long attemptWindowStartedAt;
        private int attempts;
        private boolean accepted;
        private boolean warningEmitted;
        private long lastWarningAt;

        private ConnectionState(long now) {
            this.attemptWindowStartedAt = now;
        }
    }
}
