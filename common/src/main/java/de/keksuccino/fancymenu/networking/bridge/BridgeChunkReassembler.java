package de.keksuccino.fancymenu.networking.bridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.CharacterCodingException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Thread-safe, identity-scoped storage for bridge chunk transfers.
 * <p>
 * Declared message lengths are reserved when the first chunk arrives. This prevents sparse incomplete transfers from
 * bypassing the per-session and global limits while only consuming one small chunk array each.
 */
public final class BridgeChunkReassembler {

    public static final int MAX_IN_FLIGHT_PER_SESSION = 4;
    public static final long MAX_RESERVED_BYTES_PER_SESSION = 16L * 1024L * 1024L;
    public static final long MAX_RESERVED_BYTES_GLOBAL = 64L * 1024L * 1024L;
    public static final long TRANSFER_TIMEOUT_NANOS = Duration.ofSeconds(30L).toNanos();
    private static final int MAX_TERMINATED_IDS_PER_SESSION = 1024;

    private final LongSupplier monotonicClock;
    private final IdentityHashMap<Object, SessionState> sessions = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Set<Object>> serverConnections = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Object> connectionOwners = new IdentityHashMap<>();
    @Nullable private Object clientSession;
    private long globallyReservedBytes;
    private long nextExpiryNanos = Long.MAX_VALUE;

    public BridgeChunkReassembler() {
        this(System::nanoTime);
    }

    BridgeChunkReassembler(@NotNull LongSupplier monotonicClock) {
        this.monotonicClock = Objects.requireNonNull(monotonicClock);
    }

    public synchronized void beginClientSession(@NotNull Object connection) {
        Objects.requireNonNull(connection);
        if (this.clientSession == connection) return;
        if (this.clientSession != null) this.clearSessionState(this.clientSession);
        this.clearSessionState(connection);
        this.clientSession = connection;
        this.sessions.put(connection, new SessionState());
    }

    public synchronized void beginServerConnection(@NotNull Object server, @NotNull Object connection) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(connection);
        this.clearSessionState(connection);
        this.connectionOwners.put(connection, server);
        this.serverConnections.computeIfAbsent(server, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(connection);
        this.sessions.put(connection, new SessionState());
    }

    public synchronized void endSession(@Nullable Object connection) {
        if (connection == null) return;
        if (this.clientSession == connection) this.clientSession = null;
        this.clearSessionState(connection);
    }

    public synchronized void endServer(@NotNull Object server) {
        Objects.requireNonNull(server);
        Set<Object> connections = this.serverConnections.remove(server);
        if (connections == null) return;
        for (Object connection : connections) {
            this.connectionOwners.remove(connection);
            this.releaseSession(this.sessions.remove(connection));
        }
    }

    public synchronized @NotNull Result accept(@NotNull Object connection, @NotNull BridgeChunkPayload payload) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(payload);
        long now = this.monotonicClock.getAsLong();
        if (this.nextExpiryNanos != Long.MAX_VALUE && now - this.nextExpiryNanos >= 0L) this.expireAll(now);
        SessionState session = this.sessions.get(connection);
        if (session == null) return Result.rejected();

        UUID transferId = payload.transferId();
        if (!payload.isValid() || transferId == null) {
            if (transferId != null) this.terminate(session, transferId, now);
            return Result.rejected();
        }
        if (session.terminatedTransfers.containsKey(transferId)) return Result.rejected();
        try {
            BridgeChunkPayload.validateMetadata(payload.totalLength(), payload.chunkIndex(), payload.chunkCount(), payload.chunkDataUnsafe().length);
        } catch (IllegalArgumentException ex) {
            this.terminate(session, transferId, now);
            return Result.rejected();
        }

        TransferState transfer = session.transfers.get(transferId);
        if (transfer == null) {
            if (session.transfers.size() >= MAX_IN_FLIGHT_PER_SESSION || session.reservedBytes + payload.totalLength() > MAX_RESERVED_BYTES_PER_SESSION) {
                this.recordTerminated(session, transferId, now);
                return Result.rejected();
            }
            if (this.globallyReservedBytes + payload.totalLength() > MAX_RESERVED_BYTES_GLOBAL) this.expireAll(now);
            if (this.globallyReservedBytes + payload.totalLength() > MAX_RESERVED_BYTES_GLOBAL) {
                this.recordTerminated(session, transferId, now);
                return Result.rejected();
            }
            transfer = new TransferState(payload.totalLength(), payload.chunkCount(), now);
            session.transfers.put(transferId, transfer);
            session.reservedBytes += payload.totalLength();
            this.globallyReservedBytes += payload.totalLength();
            this.scheduleExpiry(now);
        } else if (transfer.totalLength != payload.totalLength() || transfer.chunkCount != payload.chunkCount()) {
            this.terminate(session, transferId, now);
            return Result.rejected();
        }

        if (transfer.chunks[payload.chunkIndex()] != null) {
            this.terminate(session, transferId, now);
            return Result.rejected();
        }
        transfer.chunks[payload.chunkIndex()] = payload.chunkDataUnsafe();
        transfer.receivedChunks++;
        transfer.receivedBytes += payload.chunkDataUnsafe().length;
        transfer.lastActivityNanos = now;
        this.scheduleExpiry(now);
        if (transfer.receivedChunks != transfer.chunkCount) return Result.incomplete();
        if (transfer.receivedBytes != transfer.totalLength) {
            this.terminate(session, transferId, now);
            return Result.rejected();
        }

        int verifiedLength = 0;
        for (byte[] chunk : transfer.chunks) {
            if (chunk == null || verifiedLength + chunk.length > transfer.totalLength) {
                this.terminate(session, transferId, now);
                return Result.rejected();
            }
            verifiedLength += chunk.length;
        }
        if (verifiedLength != transfer.totalLength) {
            this.terminate(session, transferId, now);
            return Result.rejected();
        }

        try {
            String decoded = BridgeProtocol.decodeChunks(transfer.chunks, transfer.totalLength);
            this.terminate(session, transferId, now);
            return Result.complete(decoded);
        } catch (CharacterCodingException ex) {
            this.terminate(session, transferId, now);
            return Result.rejected();
        }
    }

    public synchronized void expire() {
        this.expireAll(this.monotonicClock.getAsLong());
    }

    synchronized int inFlightCount(@NotNull Object connection) {
        SessionState session = this.sessions.get(Objects.requireNonNull(connection));
        return session == null ? 0 : session.transfers.size();
    }

    synchronized long reservedBytes(@NotNull Object connection) {
        SessionState session = this.sessions.get(Objects.requireNonNull(connection));
        return session == null ? 0L : session.reservedBytes;
    }

    synchronized long globallyReservedBytes() {
        return this.globallyReservedBytes;
    }

    private void expireAll(long now) {
        this.nextExpiryNanos = Long.MAX_VALUE;
        for (SessionState session : this.sessions.values()) {
            this.expireSession(session, now);
            for (TransferState transfer : session.transfers.values()) this.scheduleExpiry(transfer.lastActivityNanos);
            for (long terminatedAt : session.terminatedTransfers.values()) this.scheduleExpiry(terminatedAt);
        }
    }

    private void expireSession(@NotNull SessionState session, long now) {
        Iterator<Map.Entry<UUID, TransferState>> transferIterator = session.transfers.entrySet().iterator();
        while (transferIterator.hasNext()) {
            Map.Entry<UUID, TransferState> entry = transferIterator.next();
            TransferState transfer = entry.getValue();
            if (!hasExpired(transfer.lastActivityNanos, now)) continue;
            transferIterator.remove();
            this.releaseReservation(session, transfer);
            this.recordTerminated(session, entry.getKey(), now);
        }
        Iterator<Map.Entry<UUID, Long>> terminatedIterator = session.terminatedTransfers.entrySet().iterator();
        while (terminatedIterator.hasNext()) {
            if (hasExpired(terminatedIterator.next().getValue(), now)) terminatedIterator.remove();
        }
    }

    private void terminate(@NotNull SessionState session, @NotNull UUID transferId, long now) {
        TransferState removed = session.transfers.remove(transferId);
        if (removed != null) this.releaseReservation(session, removed);
        this.recordTerminated(session, transferId, now);
    }

    private void recordTerminated(@NotNull SessionState session, @NotNull UUID transferId, long now) {
        if (session.terminatedTransfers.containsKey(transferId)) return;
        while (session.terminatedTransfers.size() >= MAX_TERMINATED_IDS_PER_SESSION) {
            Iterator<UUID> iterator = session.terminatedTransfers.keySet().iterator();
            if (!iterator.hasNext()) break;
            iterator.next();
            iterator.remove();
        }
        session.terminatedTransfers.put(transferId, now);
        this.scheduleExpiry(now);
    }

    private void clearSessionState(@NotNull Object connection) {
        Object owner = this.connectionOwners.remove(connection);
        if (owner != null) {
            Set<Object> connections = this.serverConnections.get(owner);
            if (connections != null) {
                connections.remove(connection);
                if (connections.isEmpty()) this.serverConnections.remove(owner);
            }
        }
        this.releaseSession(this.sessions.remove(connection));
    }

    private void releaseSession(@Nullable SessionState session) {
        if (session == null) return;
        this.globallyReservedBytes -= session.reservedBytes;
        session.reservedBytes = 0L;
        session.transfers.clear();
        session.terminatedTransfers.clear();
        if (this.globallyReservedBytes < 0L) throw new IllegalStateException("Bridge reassembly accounting became negative");
    }

    private void releaseReservation(@NotNull SessionState session, @NotNull TransferState transfer) {
        session.reservedBytes -= transfer.totalLength;
        this.globallyReservedBytes -= transfer.totalLength;
        if (session.reservedBytes < 0L || this.globallyReservedBytes < 0L) throw new IllegalStateException("Bridge reassembly accounting became negative");
    }

    private static boolean hasExpired(long lastActivityNanos, long now) {
        return now - lastActivityNanos >= TRANSFER_TIMEOUT_NANOS;
    }

    private void scheduleExpiry(long activityNanos) {
        long expiryNanos = activityNanos + TRANSFER_TIMEOUT_NANOS;
        if (this.nextExpiryNanos == Long.MAX_VALUE || expiryNanos < this.nextExpiryNanos) this.nextExpiryNanos = expiryNanos;
    }

    public record Result(@NotNull Status status, @Nullable String message) {

        private static @NotNull Result incomplete() {
            return new Result(Status.INCOMPLETE, null);
        }

        private static @NotNull Result complete(@NotNull String message) {
            return new Result(Status.COMPLETE, Objects.requireNonNull(message));
        }

        private static @NotNull Result rejected() {
            return new Result(Status.REJECTED, null);
        }
    }

    public enum Status {
        INCOMPLETE,
        COMPLETE,
        REJECTED
    }

    private static final class SessionState {

        private final Map<UUID, TransferState> transfers = new HashMap<>();
        private final LinkedHashMap<UUID, Long> terminatedTransfers = new LinkedHashMap<>();
        private long reservedBytes;
    }

    private static final class TransferState {

        private final int totalLength;
        private final int chunkCount;
        private final byte[][] chunks;
        private int receivedChunks;
        private int receivedBytes;
        private long lastActivityNanos;

        private TransferState(int totalLength, int chunkCount, long now) {
            this.totalLength = totalLength;
            this.chunkCount = chunkCount;
            this.chunks = new byte[chunkCount][];
            this.lastActivityNanos = now;
        }
    }
}
