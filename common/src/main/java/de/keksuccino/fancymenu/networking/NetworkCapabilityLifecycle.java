package de.keksuccino.fancymenu.networking;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns negotiated FancyMenu capability state for live client and server networking sessions.
 * Connection and server keys deliberately use identity semantics: equal addresses, UUIDs, or wrapper objects do not
 * prove that two play sessions are the same session.
 */
final class NetworkCapabilityLifecycle {

    private final AtomicReference<ClientSession> clientSession = new AtomicReference<>();
    private final ConcurrentMap<IdentityKey, ServerSession> serverSessions = new ConcurrentHashMap<>();

    /**
     * Replaces the client session atomically. Repeated callbacks for the same connection are idempotent and must not
     * erase a handshake that was already accepted for that connection.
     */
    boolean beginClientSession(@NotNull Object connection) {
        Objects.requireNonNull(connection);
        while (true) {
            ClientSession current = this.clientSession.get();
            if (current != null && current.connection == connection) return false;
            if (this.clientSession.compareAndSet(current, new ClientSession(connection, false))) return true;
        }
    }

    /**
     * Marks only the expected live session. This prevents a delayed packet from an old connection from granting
     * capability to a replacement connection.
     */
    boolean markClientServerCapable(@NotNull Object connection) {
        Objects.requireNonNull(connection);
        while (true) {
            ClientSession current = this.clientSession.get();
            if (current == null || current.connection != connection || current.serverCapable) return false;
            if (this.clientSession.compareAndSet(current, new ClientSession(connection, true))) return true;
        }
    }

    boolean isClientSessionActive(@NotNull Object connection) {
        Objects.requireNonNull(connection);
        ClientSession current = this.clientSession.get();
        return current != null && current.connection == connection;
    }

    boolean isClientServerCapable(@NotNull Object connection) {
        Objects.requireNonNull(connection);
        ClientSession current = this.clientSession.get();
        return current != null && current.connection == connection && current.serverCapable;
    }

    boolean endClientSession(@Nullable Object connection) {
        if (connection == null) return false;
        while (true) {
            ClientSession current = this.clientSession.get();
            if (current == null || current.connection != connection) return false;
            if (this.clientSession.compareAndSet(current, null)) return true;
        }
    }

    boolean beginServerSession(@NotNull Object server) {
        Objects.requireNonNull(server);
        return this.serverSessions.putIfAbsent(new IdentityKey(server), new ServerSession()) == null;
    }

    boolean beginServerConnection(@NotNull Object server, @NotNull Object connection) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        ServerSession session = this.serverSessions.get(new IdentityKey(server));
        return session != null && session.beginConnection(connection);
    }

    boolean markServerClientCapable(@NotNull Object server, @NotNull Object connection) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        ServerSession session = this.serverSessions.get(new IdentityKey(server));
        return session != null && session.markClientCapable(connection);
    }

    boolean isServerClientCapable(@NotNull Object server, @NotNull Object connection) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        ServerSession session = this.serverSessions.get(new IdentityKey(server));
        return session != null && session.isClientCapable(connection);
    }

    boolean endServerConnection(@NotNull Object server, @NotNull Object connection) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        ServerSession session = this.serverSessions.get(new IdentityKey(server));
        return session != null && session.endConnection(connection);
    }

    boolean endServerSession(@NotNull Object server) {
        Objects.requireNonNull(server);
        ServerSession session = this.serverSessions.remove(new IdentityKey(server));
        if (session == null) return false;
        session.close();
        return true;
    }

    int serverSessionCount() {
        return this.serverSessions.size();
    }

    int liveServerConnectionCount(@NotNull Object server) {
        ServerSession session = this.serverSessions.get(new IdentityKey(Objects.requireNonNull(server)));
        return session == null ? 0 : session.liveConnectionCount();
    }

    int capableServerConnectionCount(@NotNull Object server) {
        ServerSession session = this.serverSessions.get(new IdentityKey(Objects.requireNonNull(server)));
        return session == null ? 0 : session.capableConnectionCount();
    }

    private record ClientSession(Object connection, boolean serverCapable) {
    }

    /**
     * Synchronizing each server session makes connection admission, logout, and stop one ordered transition. In
     * particular, a handshake racing logout can either complete before cleanup or be rejected after cleanup, but it
     * can never recreate capability state for a connection that is no longer live.
     */
    private static final class ServerSession {

        private final Set<IdentityKey> liveConnections = new HashSet<>();
        private final Set<IdentityKey> capableConnections = new HashSet<>();
        private boolean active = true;

        private synchronized boolean beginConnection(@NotNull Object connection) {
            if (!this.active) return false;
            return this.liveConnections.add(new IdentityKey(connection));
        }

        private synchronized boolean markClientCapable(@NotNull Object connection) {
            if (!this.active) return false;
            IdentityKey connectionKey = new IdentityKey(connection);
            if (!this.liveConnections.contains(connectionKey)) return false;
            return this.capableConnections.add(connectionKey);
        }

        private synchronized boolean isClientCapable(@NotNull Object connection) {
            return this.active && this.capableConnections.contains(new IdentityKey(connection));
        }

        private synchronized boolean endConnection(@NotNull Object connection) {
            IdentityKey connectionKey = new IdentityKey(connection);
            boolean removed = this.liveConnections.remove(connectionKey);
            return this.capableConnections.remove(connectionKey) || removed;
        }

        private synchronized void close() {
            this.active = false;
            this.liveConnections.clear();
            this.capableConnections.clear();
        }

        private synchronized int liveConnectionCount() {
            return this.liveConnections.size();
        }

        private synchronized int capableConnectionCount() {
            return this.capableConnections.size();
        }
    }

    private static final class IdentityKey {

        @NotNull private final Object value;
        private final int hashCode;

        private IdentityKey(@NotNull Object value) {
            this.value = Objects.requireNonNull(value);
            this.hashCode = System.identityHashCode(value);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return this == other || other instanceof IdentityKey otherKey && this.value == otherKey.value;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
