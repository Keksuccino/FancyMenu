package de.keksuccino.fancymenu.networking.bridge;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BridgeMessageSenderTest {

    @Test
    void exactLegacyLimitUsesOnlyTheCompatibleLegacyTransmitter() {
        Object endpoint = new Object();
        AtomicInteger chunkChecks = new AtomicInteger();
        List<BridgePacketPayload> sent = new ArrayList<>();

        BridgeMessageSender.SendResult result = BridgeMessageSender.send(endpoint, BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, "x".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES), false, ignored -> {
            chunkChecks.incrementAndGet();
            return false;
        }, (exactEndpoint, payload) -> {
            sent.add(payload);
            return true;
        }, (exactEndpoint, payload) -> false);

        assertEquals(BridgeMessageSender.SendResult.SENT, result);
        assertEquals(0, chunkChecks.get());
        assertEquals(1, sent.size());
        assertEquals(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES, sent.get(0).dataWithIdentifier().length());
    }

    @Test
    void encodedMultibyteLengthSelectsChunkingEvenBelowTheLegacyCharacterCount() {
        String message = "€".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES / 3 + 1);
        List<BridgeChunkPayload> chunks = new ArrayList<>();

        BridgeMessageSender.SendResult result = BridgeMessageSender.send(new Object(), BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, message, true, ignored -> true, (endpoint, payload) -> false, (endpoint, payload) -> {
            chunks.add(payload);
            return true;
        });

        assertEquals(BridgeMessageSender.SendResult.SENT, result);
        assertEquals(2, chunks.size());
        assertEquals(message.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, chunks.get(0).totalLength());
    }

    @Test
    void oversizedSendRequiresHandshakeAdvertisementBeforeCheckingTheChannel() {
        AtomicInteger supportCalls = new AtomicInteger();
        AtomicInteger sendCalls = new AtomicInteger();

        BridgeMessageSender.SendResult result = BridgeMessageSender.send(new Object(), BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, chunkedMessage(), false, ignored -> {
            supportCalls.incrementAndGet();
            return true;
        }, (endpoint, payload) -> {
            sendCalls.incrementAndGet();
            return true;
        }, (endpoint, payload) -> {
            sendCalls.incrementAndGet();
            return true;
        });

        assertEquals(BridgeMessageSender.SendResult.CHUNK_PROTOCOL_UNAVAILABLE, result);
        assertEquals(0, supportCalls.get());
        assertEquals(0, sendCalls.get());
    }

    @Test
    void absentNegotiatedChunkChannelDoesNotConstructOrTransmitChunks() {
        AtomicInteger supportCalls = new AtomicInteger();
        AtomicInteger sendCalls = new AtomicInteger();

        BridgeMessageSender.SendResult result = BridgeMessageSender.send(new Object(), BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, chunkedMessage(), true, ignored -> {
            supportCalls.incrementAndGet();
            return false;
        }, (endpoint, payload) -> {
            sendCalls.incrementAndGet();
            return true;
        }, (endpoint, payload) -> {
            sendCalls.incrementAndGet();
            return true;
        });

        assertEquals(BridgeMessageSender.SendResult.CHUNK_CHANNEL_UNAVAILABLE, result);
        assertEquals(1, supportCalls.get());
        assertEquals(0, sendCalls.get());
    }

    @Test
    void disappearingChannelStopsAtTheFirstRejectedChunk() {
        AtomicInteger chunkCalls = new AtomicInteger();

        BridgeMessageSender.SendResult result = BridgeMessageSender.send(new Object(), BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, "x".repeat(BridgeProtocol.MAX_CHUNK_DATA_BYTES * 2 + 1), true, ignored -> true, (endpoint, payload) -> false, (endpoint, payload) -> chunkCalls.incrementAndGet() < 2);

        assertEquals(BridgeMessageSender.SendResult.CHUNK_CHANNEL_UNAVAILABLE, result);
        assertEquals(2, chunkCalls.get());
    }

    @Test
    void legacyChannelAbsenceIsAControlledNoSendResult() {
        AtomicInteger chunkChecks = new AtomicInteger();

        BridgeMessageSender.SendResult result = BridgeMessageSender.send(new Object(), BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, "small", false, ignored -> {
            chunkChecks.incrementAndGet();
            return true;
        }, (endpoint, payload) -> false, (endpoint, payload) -> true);

        assertEquals(BridgeMessageSender.SendResult.LEGACY_CHANNEL_UNAVAILABLE, result);
        assertEquals(0, chunkChecks.get());
    }

    @Test
    void messageAndUtf16FailuresReturnBeforeAnyNegotiationOrTransmission() {
        AtomicInteger calls = new AtomicInteger();

        BridgeMessageSender.SendResult tooLarge = BridgeMessageSender.send(new Object(), BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, "x".repeat(BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES + 1), true, ignored -> {
            calls.incrementAndGet();
            return true;
        }, (endpoint, payload) -> {
            calls.incrementAndGet();
            return true;
        }, (endpoint, payload) -> {
            calls.incrementAndGet();
            return true;
        });
        BridgeMessageSender.SendResult malformed = BridgeMessageSender.send(new Object(), BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, "bad\uD800", true, ignored -> {
            calls.incrementAndGet();
            return true;
        }, (endpoint, payload) -> {
            calls.incrementAndGet();
            return true;
        }, (endpoint, payload) -> {
            calls.incrementAndGet();
            return true;
        });

        assertEquals(BridgeMessageSender.SendResult.MESSAGE_TOO_LARGE, tooLarge);
        assertEquals(BridgeMessageSender.SendResult.MALFORMED_TEXT, malformed);
        assertEquals(0, calls.get());
    }

    @Test
    void invalidDirectionReturnsBeforeMessageEncodingOrTransmission() {
        AtomicInteger calls = new AtomicInteger();

        BridgeMessageSender.SendResult result = BridgeMessageSender.send(new Object(), "d".repeat(BridgeProtocol.MAX_LEGACY_DIRECTION_BYTES + 1), chunkedMessage(), true, ignored -> {
            calls.incrementAndGet();
            return true;
        }, (endpoint, payload) -> {
            calls.incrementAndGet();
            return true;
        }, (endpoint, payload) -> {
            calls.incrementAndGet();
            return true;
        });

        assertEquals(BridgeMessageSender.SendResult.INVALID_DIRECTION, result);
        assertEquals(0, calls.get());
    }

    @Test
    void supportAndTransmissionExceptionsRemainVisibleToThePacketHandler() {
        RuntimeException supportFailure = new RuntimeException("support");
        RuntimeException legacyFailure = new RuntimeException("legacy");
        RuntimeException chunkFailure = new RuntimeException("chunk");

        assertEquals(supportFailure, assertThrows(RuntimeException.class, () -> BridgeMessageSender.send(new Object(), BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, chunkedMessage(), true, ignored -> {
            throw supportFailure;
        }, (endpoint, payload) -> true, (endpoint, payload) -> true)));
        assertEquals(legacyFailure, assertThrows(RuntimeException.class, () -> BridgeMessageSender.send(new Object(), BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, "small", true, ignored -> true, (endpoint, payload) -> {
            throw legacyFailure;
        }, (endpoint, payload) -> true)));
        assertEquals(chunkFailure, assertThrows(RuntimeException.class, () -> BridgeMessageSender.send(new Object(), BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, chunkedMessage(), true, ignored -> true, (endpoint, payload) -> true, (endpoint, payload) -> {
            throw chunkFailure;
        })));
    }

    @Test
    void requiredArgumentsAreRejectedBeforeCallbacks() {
        assertThrows(NullPointerException.class, () -> BridgeMessageSender.send(null, BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, "small", false, ignored -> false, (endpoint, payload) -> false, (endpoint, payload) -> false));
        assertThrows(NullPointerException.class, () -> BridgeMessageSender.send(new Object(), null, "small", false, ignored -> false, (endpoint, payload) -> false, (endpoint, payload) -> false));
        assertThrows(NullPointerException.class, () -> BridgeMessageSender.send(new Object(), BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, null, false, ignored -> false, (endpoint, payload) -> false, (endpoint, payload) -> false));
    }

    private static String chunkedMessage() {
        return "x".repeat(BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES + 1);
    }
}
