package de.keksuccino.fancymenu.networking.bridge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BridgePacketPayloadTest {

    private static final String SERVERBOUND_DATA = "handshake:{\"value\":\"Grüße 世界\"}";
    private static final String CLIENTBOUND_DATA = "server_config:{\"enabled\":true}";

    @Test
    void identifierIsExplicitAndSafeForBothLoaderRegistries() {
        assertEquals("fancymenu", BridgePacketPayload.ID.getNamespace());
        assertEquals("fancymenu_bridge_packet", BridgePacketPayload.ID.getPath());
        assertNotEquals("minecraft", BridgePacketPayload.ID.getNamespace());
    }

    @Test
    void fabricServerboundWireBodyDecodesWithTheSharedCodec() {
        BridgePacketPayload decoded = decodeSharedPayload(encodeReferenceWireBody(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, SERVERBOUND_DATA));
        assertEquals(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, decoded.direction());
        assertEquals(SERVERBOUND_DATA, decoded.dataWithIdentifier());
    }

    @Test
    void sharedClientboundEncodingMatchesTheForgeWireLayout() {
        BridgePacketPayload payload = new BridgePacketPayload(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, CLIENTBOUND_DATA);
        assertArrayEquals(encodeReferenceWireBody(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, CLIENTBOUND_DATA), encodeSharedPayload(payload));
    }

    @Test
    void bothPacketDirectionsRoundTripWithoutChangingOpaqueData() {
        assertRoundTrip(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, SERVERBOUND_DATA);
        assertRoundTrip(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, CLIENTBOUND_DATA);
    }

    @Test
    void serializedDirectionRemainsOpaqueUntrustedMetadata() {
        String forgedDirection = "server\u0000client";
        BridgePacketPayload decoded = decodeSharedPayload(encodeReferenceWireBody(forgedDirection, SERVERBOUND_DATA));
        assertEquals(forgedDirection, decoded.direction());
    }

    @Test
    void truncatedWireBodyIsRejectedBeforeProducingAPayload() {
        byte[] completeWireBody = encodeSharedPayload(new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, SERVERBOUND_DATA));
        assertThrows(IndexOutOfBoundsException.class, () -> decodeSharedPayload(Arrays.copyOf(completeWireBody, completeWireBody.length - 1)));
    }

    private static void assertRoundTrip(String direction, String dataWithIdentifier) {
        BridgePacketPayload decoded = decodeSharedPayload(encodeSharedPayload(new BridgePacketPayload(direction, dataWithIdentifier)));
        assertEquals(direction, decoded.direction());
        assertEquals(dataWithIdentifier, decoded.dataWithIdentifier());
    }

    private static byte[] encodeSharedPayload(BridgePacketPayload payload) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            payload.write(byteBuf);
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            return bytes;
        } finally {
            byteBuf.release();
        }
    }

    private static BridgePacketPayload decodeSharedPayload(byte[] bytes) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            BridgePacketPayload payload = BridgePacketPayload.read(byteBuf);
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
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            return bytes;
        } finally {
            byteBuf.release();
        }
    }
}
