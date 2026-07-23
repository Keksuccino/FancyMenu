package de.keksuccino.fancymenu.networking.bridge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BridgeChunkReassemblerTest {

    @Test
    void completeMessageDispatchesOnlyAfterEveryChunk() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        Object connection = openServerConnection(reassembler);
        String message = "packet:" + "x".repeat(BridgeProtocol.MAX_CHUNK_DATA_BYTES * 2);
        List<BridgeChunkPayload> chunks = chunks(message, UUID.randomUUID());

        assertEquals(BridgeChunkReassembler.Status.INCOMPLETE, reassembler.accept(connection, chunks.get(0)).status());
        assertEquals(BridgeChunkReassembler.Status.INCOMPLETE, reassembler.accept(connection, chunks.get(1)).status());
        BridgeChunkReassembler.Result complete = reassembler.accept(connection, chunks.get(2));

        assertEquals(BridgeChunkReassembler.Status.COMPLETE, complete.status());
        assertEquals(message, complete.message());
        assertEquals(0, reassembler.inFlightCount(connection));
        assertEquals(0L, reassembler.reservedBytes(connection));
        assertEquals(0L, reassembler.globallyReservedBytes());
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(connection, chunks.get(0)).status());
    }

    @Test
    void chunksCanArriveOutOfOrderEvenAcrossUtf8CodePointBoundaries() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        Object connection = openServerConnection(reassembler);
        String message = "a".repeat(BridgeProtocol.MAX_CHUNK_DATA_BYTES - 1) + "😀" + "tail";
        List<BridgeChunkPayload> chunks = new ArrayList<>(chunks(message, UUID.randomUUID()));
        Collections.reverse(chunks);

        BridgeChunkReassembler.Result result = null;
        for (BridgeChunkPayload chunk : chunks) result = reassembler.accept(connection, chunk);

        assertEquals(BridgeChunkReassembler.Status.COMPLETE, result.status());
        assertEquals(message, result.message());
    }

    @Test
    void concurrentDistinctChunksHaveExactlyOneCompleteResult() throws Exception {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        Object connection = openServerConnection(reassembler);
        String message = "packet:" + "x".repeat(BridgeProtocol.MAX_CHUNK_DATA_BYTES * 3);
        List<BridgeChunkPayload> chunks = chunks(message, UUID.randomUUID());
        CountDownLatch ready = new CountDownLatch(chunks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<BridgeChunkReassembler.Result>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (BridgeChunkPayload chunk : chunks) futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return reassembler.accept(connection, chunk);
            }));
            ready.await();
            start.countDown();
            int completeResults = 0;
            for (Future<BridgeChunkReassembler.Result> future : futures) {
                BridgeChunkReassembler.Result result = future.get();
                if (result.status() == BridgeChunkReassembler.Status.COMPLETE) {
                    completeResults++;
                    assertEquals(message, result.message());
                } else {
                    assertEquals(BridgeChunkReassembler.Status.INCOMPLETE, result.status());
                }
            }
            assertEquals(1, completeResults);
        }
        assertEquals(0L, reassembler.globallyReservedBytes());
    }

    @Test
    void duplicateChunkTerminatesTransferAndReleasesReservationExactlyOnce() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        Object connection = openServerConnection(reassembler);
        List<BridgeChunkPayload> chunks = chunks("x".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES + 1), UUID.randomUUID());

        assertEquals(BridgeChunkReassembler.Status.INCOMPLETE, reassembler.accept(connection, chunks.get(0)).status());
        assertEquals(chunks.get(0).totalLength(), reassembler.globallyReservedBytes());
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(connection, chunks.get(0)).status());
        assertEquals(0L, reassembler.globallyReservedBytes());
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(connection, chunks.get(1)).status());
        assertEquals(0L, reassembler.globallyReservedBytes());
    }

    @Test
    void conflictingMetadataTerminatesTheExistingTransfer() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        Object connection = openServerConnection(reassembler);
        UUID transferId = UUID.randomUUID();
        BridgeChunkPayload original = firstChunk(transferId, BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1);
        BridgeChunkPayload conflict = firstChunk(transferId, BridgeProtocol.MAX_CHUNK_DATA_BYTES + 2);

        assertEquals(BridgeChunkReassembler.Status.INCOMPLETE, reassembler.accept(connection, original).status());
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(connection, conflict).status());
        assertEquals(0, reassembler.inFlightCount(connection));
        assertEquals(0L, reassembler.globallyReservedBytes());
    }

    @Test
    void malformedPayloadWithKnownIdTerminatesAndReleasesItsTransfer() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        Object connection = openServerConnection(reassembler);
        UUID transferId = UUID.randomUUID();

        assertEquals(BridgeChunkReassembler.Status.INCOMPLETE, reassembler.accept(connection, firstChunk(transferId, BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1)).status());
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(connection, decodeWrongVersionPayload(transferId)).status());
        assertEquals(0L, reassembler.reservedBytes(connection));
        assertEquals(0L, reassembler.globallyReservedBytes());
    }

    @Test
    void missingChunkExpiresLazilyAndCanNeverCompleteLater() {
        AtomicLong clock = new AtomicLong(100L);
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler(clock::get);
        Object connection = openServerConnection(reassembler);
        List<BridgeChunkPayload> chunks = chunks("x".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES + 1), UUID.randomUUID());

        reassembler.accept(connection, chunks.get(0));
        clock.addAndGet(BridgeChunkReassembler.TRANSFER_TIMEOUT_NANOS - 1L);
        reassembler.expire();
        assertEquals(1, reassembler.inFlightCount(connection));
        clock.incrementAndGet();
        reassembler.expire();

        assertEquals(0, reassembler.inFlightCount(connection));
        assertEquals(0L, reassembler.globallyReservedBytes());
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(connection, chunks.get(1)).status());
        assertNull(reassembler.accept(connection, chunks.get(1)).message());
    }

    @Test
    void trafficOnAnotherSessionLazilyExpiresStaleGlobalReservations() {
        AtomicLong clock = new AtomicLong(500L);
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler(clock::get);
        Object staleConnection = openServerConnection(reassembler);
        Object activeConnection = openServerConnection(reassembler);
        reassembler.accept(staleConnection, firstChunk(UUID.randomUUID(), BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES));
        clock.addAndGet(BridgeChunkReassembler.TRANSFER_TIMEOUT_NANOS);

        reassembler.accept(activeConnection, firstChunk(UUID.randomUUID(), BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1));

        assertEquals(0, reassembler.inFlightCount(staleConnection));
        assertEquals(BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1L, reassembler.globallyReservedBytes());
    }

    @Test
    void invalidUtf8NeverProducesALogicalMessageAndReleasesAccounting() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        Object connection = openServerConnection(reassembler);
        UUID transferId = UUID.randomUUID();
        byte[] first = new byte[BridgeProtocol.MAX_CHUNK_DATA_BYTES];
        byte[] invalidLast = new byte[]{(byte) 0xC3};
        BridgeChunkPayload firstChunk = new BridgeChunkPayload(transferId, BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1, 0, 2, first);
        BridgeChunkPayload lastChunk = new BridgeChunkPayload(transferId, BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1, 1, 2, invalidLast);

        reassembler.accept(connection, firstChunk);
        BridgeChunkReassembler.Result result = reassembler.accept(connection, lastChunk);

        assertEquals(BridgeChunkReassembler.Status.REJECTED, result.status());
        assertNull(result.message());
        assertEquals(0L, reassembler.reservedBytes(connection));
        assertEquals(0L, reassembler.globallyReservedBytes());
    }

    @Test
    void fourInFlightTransfersAreAllowedAndTheFifthIsRejected() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        Object connection = openServerConnection(reassembler);
        int totalLength = BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1;

        for (int index = 0; index < BridgeChunkReassembler.MAX_IN_FLIGHT_PER_SESSION; index++) assertEquals(BridgeChunkReassembler.Status.INCOMPLETE, reassembler.accept(connection, firstChunk(UUID.randomUUID(), totalLength)).status());
        assertEquals(BridgeChunkReassembler.MAX_IN_FLIGHT_PER_SESSION, reassembler.inFlightCount(connection));
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(connection, firstChunk(UUID.randomUUID(), totalLength)).status());
        assertEquals((long) totalLength * BridgeChunkReassembler.MAX_IN_FLIGHT_PER_SESSION, reassembler.reservedBytes(connection));

        reassembler.endSession(connection);
        assertEquals(0L, reassembler.globallyReservedBytes());
    }

    @Test
    void declaredTotalsReserveExactlyTheSixteenMibSessionLimit() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        Object connection = openServerConnection(reassembler);

        assertEquals(BridgeChunkReassembler.Status.INCOMPLETE, reassembler.accept(connection, firstChunk(UUID.randomUUID(), BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES)).status());
        assertEquals(BridgeChunkReassembler.Status.INCOMPLETE, reassembler.accept(connection, firstChunk(UUID.randomUUID(), BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES)).status());
        assertEquals(BridgeChunkReassembler.MAX_RESERVED_BYTES_PER_SESSION, reassembler.reservedBytes(connection));
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(connection, firstChunk(UUID.randomUUID(), BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1)).status());

        reassembler.endSession(connection);
        assertEquals(0L, reassembler.globallyReservedBytes());
    }

    @Test
    void globalSixtyFourMibReservationIsSharedAcrossExactSessions() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        List<Object> connections = new ArrayList<>();

        for (int sessionIndex = 0; sessionIndex < 4; sessionIndex++) {
            Object connection = openServerConnection(reassembler);
            connections.add(connection);
            for (int transferIndex = 0; transferIndex < 2; transferIndex++) assertEquals(BridgeChunkReassembler.Status.INCOMPLETE, reassembler.accept(connection, firstChunk(UUID.randomUUID(), BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES)).status());
        }
        assertEquals(BridgeChunkReassembler.MAX_RESERVED_BYTES_GLOBAL, reassembler.globallyReservedBytes());
        Object rejectedConnection = openServerConnection(reassembler);
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(rejectedConnection, firstChunk(UUID.randomUUID(), BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1)).status());
        assertEquals(BridgeChunkReassembler.MAX_RESERVED_BYTES_GLOBAL, reassembler.globallyReservedBytes());

        for (Object connection : connections) reassembler.endSession(connection);
        reassembler.endSession(rejectedConnection);
        assertEquals(0L, reassembler.globallyReservedBytes());
    }

    @Test
    void equalButDistinctConnectionsReassembleIndependentlyEvenWithTheSameTransferId() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        EqualIdentity firstConnection = new EqualIdentity();
        EqualIdentity secondConnection = new EqualIdentity();
        reassembler.beginServerConnection(new Object(), firstConnection);
        reassembler.beginServerConnection(new Object(), secondConnection);
        UUID transferId = UUID.randomUUID();
        String firstMessage = "a".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES + 1);
        String secondMessage = "b".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES + 1);
        List<BridgeChunkPayload> firstChunks = chunks(firstMessage, transferId);
        List<BridgeChunkPayload> secondChunks = chunks(secondMessage, transferId);

        reassembler.accept(firstConnection, firstChunks.get(0));
        reassembler.accept(secondConnection, secondChunks.get(0));

        assertEquals(firstMessage, reassembler.accept(firstConnection, firstChunks.get(1)).message());
        assertEquals(secondMessage, reassembler.accept(secondConnection, secondChunks.get(1)).message());
    }

    @Test
    void clientReplacementAndDisconnectPreventStaleTransferReuse() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        Object oldConnection = new Object();
        Object replacementConnection = new Object();
        UUID transferId = UUID.randomUUID();
        List<BridgeChunkPayload> chunks = chunks("x".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES + 1), transferId);
        reassembler.beginClientSession(oldConnection);
        reassembler.accept(oldConnection, chunks.get(0));

        reassembler.beginClientSession(replacementConnection);

        assertEquals(0L, reassembler.globallyReservedBytes());
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(oldConnection, chunks.get(1)).status());
        assertEquals(BridgeChunkReassembler.Status.INCOMPLETE, reassembler.accept(replacementConnection, chunks.get(0)).status());
        assertEquals(BridgeChunkReassembler.Status.COMPLETE, reassembler.accept(replacementConnection, chunks.get(1)).status());
        reassembler.endSession(replacementConnection);
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(replacementConnection, chunks.get(0)).status());
        assertEquals(0L, reassembler.globallyReservedBytes());
    }

    @Test
    void serverStopClearsEveryOwnedConnectionAndAllReservations() {
        BridgeChunkReassembler reassembler = new BridgeChunkReassembler();
        Object server = new Object();
        Object firstConnection = new Object();
        Object secondConnection = new Object();
        reassembler.beginServerConnection(server, firstConnection);
        reassembler.beginServerConnection(server, secondConnection);
        reassembler.accept(firstConnection, firstChunk(UUID.randomUUID(), BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES));
        reassembler.accept(secondConnection, firstChunk(UUID.randomUUID(), BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES));

        reassembler.endServer(server);

        assertEquals(0L, reassembler.globallyReservedBytes());
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(firstConnection, firstChunk(UUID.randomUUID(), BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1)).status());
        assertEquals(BridgeChunkReassembler.Status.REJECTED, reassembler.accept(secondConnection, firstChunk(UUID.randomUUID(), BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1)).status());
    }

    private static Object openServerConnection(BridgeChunkReassembler reassembler) {
        Object connection = new Object();
        reassembler.beginServerConnection(new Object(), connection);
        return connection;
    }

    private static List<BridgeChunkPayload> chunks(String message, UUID transferId) {
        int length = BridgeProtocol.encodedLength(message, BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES);
        return BridgeChunkEncoder.encode(message, length, transferId);
    }

    private static BridgeChunkPayload firstChunk(UUID transferId, int totalLength) {
        int chunkCount = (totalLength + BridgeProtocol.MAX_CHUNK_DATA_BYTES - 1) / BridgeProtocol.MAX_CHUNK_DATA_BYTES;
        return new BridgeChunkPayload(transferId, totalLength, 0, chunkCount, new byte[BridgeProtocol.MAX_CHUNK_DATA_BYTES]);
    }

    private static BridgeChunkPayload decodeWrongVersionPayload(UUID transferId) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            byteBuf.writeByte(BridgeProtocol.VERSION + 1);
            byteBuf.writeLong(transferId.getMostSignificantBits());
            byteBuf.writeLong(transferId.getLeastSignificantBits());
            byteBuf.writeInt(BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1);
            byteBuf.writeInt(0);
            byteBuf.writeInt(2);
            byteBuf.writeInt(BridgeProtocol.MAX_CHUNK_DATA_BYTES);
            byteBuf.writeZero(BridgeProtocol.MAX_CHUNK_DATA_BYTES);
            BridgeChunkPayload payload = BridgeChunkPayload.CODEC.decode(byteBuf);
            assertEquals(0, byteBuf.readableBytes());
            return payload;
        } finally {
            byteBuf.release();
        }
    }

    private static final class EqualIdentity {

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualIdentity;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
