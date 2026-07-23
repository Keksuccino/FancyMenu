package de.keksuccino.fancymenu.networking.bridge;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Chooses the compatible legacy wire format or the negotiated v1 chunk format before constructing any payload.
 */
public final class BridgeMessageSender {

    private BridgeMessageSender() {
    }

    public static <E> @NotNull SendResult send(@NotNull E endpoint, @NotNull String direction, @NotNull String message, boolean bridgeProtocolV1Advertised, @NotNull Predicate<? super E> chunkChannelSupport, @NotNull BiPredicate<? super E, ? super BridgePacketPayload> legacyTransmitter, @NotNull BiPredicate<? super E, ? super BridgeChunkPayload> chunkTransmitter) {
        Objects.requireNonNull(endpoint);
        Objects.requireNonNull(direction);
        Objects.requireNonNull(message);
        Objects.requireNonNull(chunkChannelSupport);
        Objects.requireNonNull(legacyTransmitter);
        Objects.requireNonNull(chunkTransmitter);

        try {
            BridgeProtocol.encodedLength(direction, BridgeProtocol.MAX_LEGACY_DIRECTION_BYTES);
        } catch (IllegalArgumentException ex) {
            return SendResult.INVALID_DIRECTION;
        }

        final int encodedLength;
        try {
            encodedLength = BridgeProtocol.encodedLength(message, BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES);
        } catch (BridgeProtocol.EncodedLengthExceededException ex) {
            return SendResult.MESSAGE_TOO_LARGE;
        } catch (BridgeProtocol.MalformedTextException ex) {
            return SendResult.MALFORMED_TEXT;
        }

        if (encodedLength <= BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES) {
            BridgePacketPayload payload = new BridgePacketPayload(direction, message);
            return legacyTransmitter.test(endpoint, payload) ? SendResult.SENT : SendResult.LEGACY_CHANNEL_UNAVAILABLE;
        }
        if (!bridgeProtocolV1Advertised) return SendResult.CHUNK_PROTOCOL_UNAVAILABLE;
        if (!chunkChannelSupport.test(endpoint)) return SendResult.CHUNK_CHANNEL_UNAVAILABLE;

        List<BridgeChunkPayload> chunks = BridgeChunkEncoder.encode(message, encodedLength);
        for (BridgeChunkPayload chunk : chunks) {
            if (!chunkTransmitter.test(endpoint, chunk)) return SendResult.CHUNK_CHANNEL_UNAVAILABLE;
        }
        return SendResult.SENT;
    }

    public enum SendResult {
        SENT,
        LEGACY_CHANNEL_UNAVAILABLE,
        CHUNK_PROTOCOL_UNAVAILABLE,
        CHUNK_CHANNEL_UNAVAILABLE,
        MESSAGE_TOO_LARGE,
        MALFORMED_TEXT,
        INVALID_DIRECTION
    }
}
