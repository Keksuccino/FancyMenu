package de.keksuccino.fancymenu.networking.bridge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgePacketPayloadTest {

    private static final String SERVERBOUND_DATA = "handshake:{\"value\":\"Grüße 世界\"}";
    private static final String CLIENTBOUND_DATA = "server_config:{\"enabled\":true}";

    @Test
    void identifierIsExplicitAndSafeForBothLoaderRegistries() {
        assertEquals("fancymenu", BridgePacketPayload.TYPE.id().getNamespace());
        assertEquals("fancymenu_bridge_packet", BridgePacketPayload.TYPE.id().getPath());
        assertEquals("fancymenu:fancymenu_bridge_packet", BridgePacketPayload.TYPE.id().toString());
        assertNotEquals("minecraft", BridgePacketPayload.TYPE.id().getNamespace());
    }

    @Test
    void everyPayloadReportsTheOneSharedRegistrationType() {
        BridgePacketPayload serverbound = new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, SERVERBOUND_DATA);
        BridgePacketPayload clientbound = new BridgePacketPayload(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, CLIENTBOUND_DATA);

        assertSame(BridgePacketPayload.TYPE, serverbound.type());
        assertSame(BridgePacketPayload.TYPE, clientbound.type());
    }

    @Test
    void legacyEncodingPreservesTheExactHistoricalTwoUtfLayout() {
        BridgePacketPayload payload = new BridgePacketPayload(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, CLIENTBOUND_DATA);

        assertArrayEquals(encodeReferenceWireBody(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, CLIENTBOUND_DATA), encodeSharedPayload(payload));
    }

    @Test
    void bothPacketDirectionsRoundTripWithoutChangingOpaqueData() {
        assertRoundTrip(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, SERVERBOUND_DATA);
        assertRoundTrip(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, CLIENTBOUND_DATA);
    }

    @Test
    void serializedDirectionRemainsOpaqueUntrustedMetadataWithinItsByteLimit() {
        String forgedDirection = "server\u0000client";

        BridgePacketPayload decoded = decodeSharedPayload(encodeReferenceWireBody(forgedDirection, SERVERBOUND_DATA));

        assertTrue(decoded.isValid());
        assertEquals(forgedDirection, decoded.direction());
        assertEquals(SERVERBOUND_DATA, decoded.dataWithIdentifier());
    }

    @Test
    void exactAsciiByteCeilingIsAcceptedAndOneMoreByteIsRejected() {
        String exact = "a".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES);

        assertRoundTrip(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, exact);
        assertTrue(encodeSharedPayload(new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, exact)).length < 32767);
        assertThrows(BridgeProtocol.EncodedLengthExceededException.class, () -> new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, exact + "a"));
    }

    @Test
    void multibyteCeilingUsesEncodedBytesInsteadOfJavaCharacterCount() {
        String exact = "€".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES / 3);

        assertEquals(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES / 3, exact.length());
        assertRoundTrip(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, exact);
        assertThrows(BridgeProtocol.EncodedLengthExceededException.class, () -> new BridgePacketPayload(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, exact + "€"));
    }

    @Test
    void supplementaryCodePointsAreCountedAsFourBytesWithoutSplittingSurrogates() {
        String exact = "😀".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES / 4);

        assertEquals(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES / 2, exact.length());
        assertRoundTrip(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, exact);
        assertThrows(BridgeProtocol.EncodedLengthExceededException.class, () -> new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, exact + "😀"));
        assertThrows(BridgeProtocol.MalformedTextException.class, () -> new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, "broken\uD83D"));
        assertThrows(BridgeProtocol.MalformedTextException.class, () -> new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, "broken\uDE00"));
    }

    @Test
    void encodeRevalidatesEveryFieldBeforeWritingAnything() throws Exception {
        BridgePacketPayload payload = new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, "valid");
        Field messageField = BridgePacketPayload.class.getDeclaredField("dataWithIdentifier");
        messageField.setAccessible(true);
        messageField.set(payload, "x".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES + 1));
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            assertThrows(BridgeProtocol.EncodedLengthExceededException.class, () -> BridgePacketPayload.CODEC.encode(byteBuf, payload));
            assertEquals(0, byteBuf.writerIndex());
        } finally {
            byteBuf.release();
        }
    }

    @Test
    void truncatedWireBodyBecomesAQuietFullyConsumedDrop() {
        byte[] complete = encodeSharedPayload(new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, SERVERBOUND_DATA));
        assertInvalidAndFullyConsumed(Arrays.copyOf(complete, complete.length - 1));
    }

    @Test
    void malformedUtf8BecomesAQuietFullyConsumedDrop() {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            byteBuf.writeVarInt(6);
            byteBuf.writeBytes(new byte[]{'s', 'e', 'r', 'v', 'e', 'r'});
            byteBuf.writeVarInt(2);
            byteBuf.writeBytes(new byte[]{(byte) 0xC3, 0x28});
            assertInvalidAndFullyConsumed(copyReadableBytes(byteBuf));
        } finally {
            byteBuf.release();
        }
    }

    @Test
    void oversizedOrTrailingBodiesAreConsumedWithoutProducingData() {
        assertInvalidAndFullyConsumed(new byte[BridgeProtocol.MAX_PAYLOAD_BODY_BYTES + 1]);
        byte[] valid = encodeSharedPayload(new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, "payload"));
        byte[] withTrailingByte = Arrays.copyOf(valid, valid.length + 1);
        assertInvalidAndFullyConsumed(withTrailingByte);
    }

    private static void assertRoundTrip(String direction, String dataWithIdentifier) {
        BridgePacketPayload decoded = decodeSharedPayload(encodeSharedPayload(new BridgePacketPayload(direction, dataWithIdentifier)));

        assertTrue(decoded.isValid());
        assertEquals(direction, decoded.direction());
        assertEquals(dataWithIdentifier, decoded.dataWithIdentifier());
        assertSame(BridgePacketPayload.TYPE, decoded.type());
    }

    private static void assertInvalidAndFullyConsumed(byte[] bytes) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            BridgePacketPayload payload = BridgePacketPayload.CODEC.decode(byteBuf);
            assertFalse(payload.isValid());
            assertNull(payload.direction());
            assertNull(payload.dataWithIdentifier());
            assertEquals(0, byteBuf.readableBytes());
        } finally {
            byteBuf.release();
        }
    }

    private static byte[] encodeSharedPayload(BridgePacketPayload payload) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BridgePacketPayload.CODEC.encode(byteBuf, payload);
            return copyReadableBytes(byteBuf);
        } finally {
            byteBuf.release();
        }
    }

    private static BridgePacketPayload decodeSharedPayload(byte[] bytes) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            BridgePacketPayload payload = BridgePacketPayload.CODEC.decode(byteBuf);
            assertEquals(0, byteBuf.readableBytes());
            return payload;
        } finally {
            byteBuf.release();
        }
    }

    private static byte[] encodeReferenceWireBody(String direction, String dataWithIdentifier) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            byteBuf.writeUtf(direction);
            byteBuf.writeUtf(dataWithIdentifier);
            return copyReadableBytes(byteBuf);
        } finally {
            byteBuf.release();
        }
    }

    private static byte[] copyReadableBytes(FriendlyByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(byteBuf.readerIndex(), bytes);
        return bytes;
    }
}
