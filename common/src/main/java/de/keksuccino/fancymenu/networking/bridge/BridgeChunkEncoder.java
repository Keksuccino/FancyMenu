package de.keksuccino.fancymenu.networking.bridge;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

final class BridgeChunkEncoder {

    private BridgeChunkEncoder() {
    }

    static @NotNull List<BridgeChunkPayload> encode(@NotNull String message, int encodedLength) {
        return encode(message, encodedLength, UUID.randomUUID());
    }

    static @NotNull List<BridgeChunkPayload> encode(@NotNull String message, int encodedLength, @NotNull UUID transferId) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(transferId);
        if (encodedLength <= BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES || encodedLength > BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES) throw new IllegalArgumentException("Logical bridge message is outside the chunked range");
        byte[] encoded = BridgeProtocol.encode(message, encodedLength);
        int chunkCount = (encodedLength + BridgeProtocol.MAX_CHUNK_DATA_BYTES - 1) / BridgeProtocol.MAX_CHUNK_DATA_BYTES;
        List<BridgeChunkPayload> chunks = new ArrayList<>(chunkCount);
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            int start = chunkIndex * BridgeProtocol.MAX_CHUNK_DATA_BYTES;
            int end = Math.min(start + BridgeProtocol.MAX_CHUNK_DATA_BYTES, encodedLength);
            chunks.add(BridgeChunkPayload.trusted(transferId, encodedLength, chunkIndex, chunkCount, Arrays.copyOfRange(encoded, start, end)));
        }
        return List.copyOf(chunks);
    }
}
