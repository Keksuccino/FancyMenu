package de.keksuccino.fancymenu.networking.bridge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeChunkPayloadTest {

    @Test
    void chunkIdentifierIsDistinctAndExplicit() {
        assertEquals("fancymenu:fancymenu_bridge_chunk", BridgeChunkPayload.ID.toString());
        assertNotEquals(BridgePacketPayload.ID, BridgeChunkPayload.ID);
    }

    @Test
    void encoderCreatesExactThirtyKibChunksWithOneTransferId() {
        String message = "x".repeat(BridgeProtocol.MAX_CHUNK_DATA_BYTES * 2 + 17);

        List<BridgeChunkPayload> chunks = BridgeChunkEncoder.encode(message, message.length(), new UUID(7L, 11L));

        assertEquals(3, chunks.size());
        assertEquals(BridgeProtocol.MAX_CHUNK_DATA_BYTES, chunks.get(0).chunkData().length);
        assertEquals(BridgeProtocol.MAX_CHUNK_DATA_BYTES, chunks.get(1).chunkData().length);
        assertEquals(17, chunks.get(2).chunkData().length);
        for (int index = 0; index < chunks.size(); index++) {
            assertEquals(new UUID(7L, 11L), chunks.get(index).transferId());
            assertEquals(message.length(), chunks.get(index).totalLength());
            assertEquals(index, chunks.get(index).chunkIndex());
            assertEquals(chunks.size(), chunks.get(index).chunkCount());
        }
    }

    @Test
    void productionTransfersUseIndependentCollisionResistantIds() {
        String message = "x".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES + 1);

        UUID first = BridgeChunkEncoder.encode(message, message.length()).get(0).transferId();
        UUID second = BridgeChunkEncoder.encode(message, message.length()).get(0).transferId();

        assertNotEquals(first, second);
    }

    @Test
    void chunkCodecRoundTripsBinaryDataAndKeepsBodiesBelowVanillaCap() {
        byte[] data = new byte[BridgeProtocol.MAX_CHUNK_DATA_BYTES];
        for (int index = 0; index < data.length; index++) data[index] = (byte) index;
        BridgeChunkPayload original = new BridgeChunkPayload(new UUID(13L, 17L), BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1, 0, 2, data);
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            original.write(encoded);
            assertEquals(BridgeChunkPayload.HEADER_BYTES + BridgeProtocol.MAX_CHUNK_DATA_BYTES, encoded.readableBytes());
            assertTrue(encoded.readableBytes() < 32767);
            BridgeChunkPayload decoded = new BridgeChunkPayload(encoded);
            assertTrue(decoded.isValid());
            assertEquals(original.transferId(), decoded.transferId());
            assertEquals(original.totalLength(), decoded.totalLength());
            assertEquals(original.chunkIndex(), decoded.chunkIndex());
            assertEquals(original.chunkCount(), decoded.chunkCount());
            assertArrayEquals(data, decoded.chunkData());
            assertEquals(0, encoded.readableBytes());
        } finally {
            encoded.release();
        }
    }

    @Test
    void accessorCannotMutateThePayloadsOwnedChunkData() {
        byte[] input = new byte[BridgeProtocol.MAX_CHUNK_DATA_BYTES];
        BridgeChunkPayload payload = new BridgeChunkPayload(UUID.randomUUID(), BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1, 0, 2, input);
        byte[] exposed = payload.chunkData();

        assertNotSame(input, exposed);
        exposed[0] = 12;
        assertEquals(0, payload.chunkData()[0]);
    }

    @Test
    void exactChunkLengthsAndMessageBoundsAreEnforced() {
        UUID transferId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new BridgeChunkPayload(transferId, BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES, 0, 1, new byte[BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES]));
        assertThrows(IllegalArgumentException.class, () -> new BridgeChunkPayload(transferId, BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES + 1, 0, BridgeChunkPayload.MAX_CHUNK_COUNT + 1, new byte[BridgeProtocol.MAX_CHUNK_DATA_BYTES]));
        assertThrows(IllegalArgumentException.class, () -> new BridgeChunkPayload(transferId, BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1, 0, 2, new byte[BridgeProtocol.MAX_CHUNK_DATA_BYTES - 1]));
        assertThrows(IllegalArgumentException.class, () -> new BridgeChunkPayload(transferId, BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1, 1, 2, new byte[2]));
        assertThrows(IllegalArgumentException.class, () -> new BridgeChunkPayload(transferId, BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1, 2, 2, new byte[1]));
    }

    @Test
    void malformedHeaderIsAQuietFullyConsumedDropWithoutAllocatingDeclaredData() {
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            UUID transferId = UUID.randomUUID();
            encoded.writeByte(BridgeProtocol.VERSION);
            encoded.writeLong(transferId.getMostSignificantBits());
            encoded.writeLong(transferId.getLeastSignificantBits());
            encoded.writeInt(BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES);
            encoded.writeInt(0);
            encoded.writeInt(BridgeChunkPayload.MAX_CHUNK_COUNT);
            encoded.writeInt(Integer.MAX_VALUE);
            BridgeChunkPayload decoded = new BridgeChunkPayload(encoded);

            assertFalse(decoded.isValid());
            assertEquals(transferId, decoded.transferId());
            assertEquals(0, encoded.readableBytes());
        } finally {
            encoded.release();
        }
    }

    @Test
    void chunkEncodeRevalidatesAllMetadataBeforeWriting() throws Exception {
        BridgeChunkPayload payload = new BridgeChunkPayload(UUID.randomUUID(), BridgeProtocol.MAX_CHUNK_DATA_BYTES + 1, 0, 2, new byte[BridgeProtocol.MAX_CHUNK_DATA_BYTES]);
        Field totalLength = BridgeChunkPayload.class.getDeclaredField("totalLength");
        totalLength.setAccessible(true);
        totalLength.setInt(payload, BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES + 1);
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            assertThrows(IllegalArgumentException.class, () -> payload.write(byteBuf));
            assertEquals(0, byteBuf.writerIndex());
        } finally {
            byteBuf.release();
        }
    }
}
