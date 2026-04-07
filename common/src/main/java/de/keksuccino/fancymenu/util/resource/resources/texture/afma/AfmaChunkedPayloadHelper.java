package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class AfmaChunkedPayloadHelper {

    public static final String PAYLOAD_INDEX_ENTRY_PATH = "payload_index.bin";
    public static final String PAYLOAD_CHUNK_DIRECTORY = "payload_chunks/";
    public static final String PAYLOAD_CHUNK_EXTENSION = ".bin";
    public static final int PAYLOAD_INDEX_MAGIC = 0x41465049; // AFPI
    public static final int PAYLOAD_INDEX_VERSION = 1;
    public static final int TARGET_CHUNK_BYTES = 256 * 1024;

    private AfmaChunkedPayloadHelper() {
    }

    @NotNull
    public static PackedPayloadArchive buildArchiveLayout(@NotNull Map<String, byte[]> payloads) {
        Objects.requireNonNull(payloads);
        LinkedHashMap<String, Integer> payloadIdsByPath = new LinkedHashMap<>();
        List<PayloadLocator> payloadLocators = new ArrayList<>();
        List<ChunkPlan> chunkPlans = new ArrayList<>();

        ArrayList<String> currentChunkPayloadPaths = new ArrayList<>();
        int currentChunkLength = 0;
        for (Map.Entry<String, byte[]> entry : payloads.entrySet()) {
            String payloadPath = Objects.requireNonNull(entry.getKey());
            byte[] payloadBytes = Objects.requireNonNull(entry.getValue(), "AFMA payload bytes were NULL for " + payloadPath);
            int payloadLength = payloadBytes.length;
            int payloadId = payloadIdsByPath.size();
            payloadIdsByPath.put(payloadPath, payloadId);

            if (payloadLength >= TARGET_CHUNK_BYTES) {
                if (!currentChunkPayloadPaths.isEmpty()) {
                    addChunkPlan(chunkPlans, currentChunkPayloadPaths, currentChunkLength);
                    currentChunkPayloadPaths = new ArrayList<>();
                    currentChunkLength = 0;
                }

                int chunkId = chunkPlans.size();
                chunkPlans.add(new ChunkPlan(chunkEntryPath(chunkId), List.of(payloadPath), payloadLength));
                payloadLocators.add(new PayloadLocator(chunkId, 0, payloadLength));
                continue;
            }

            if (!currentChunkPayloadPaths.isEmpty() && ((currentChunkLength + payloadLength) > TARGET_CHUNK_BYTES)) {
                addChunkPlan(chunkPlans, currentChunkPayloadPaths, currentChunkLength);
                currentChunkPayloadPaths = new ArrayList<>();
                currentChunkLength = 0;
            }

            payloadLocators.add(new PayloadLocator(chunkPlans.size(), currentChunkLength, payloadLength));
            currentChunkPayloadPaths.add(payloadPath);
            currentChunkLength += payloadLength;
        }

        if (!currentChunkPayloadPaths.isEmpty()) {
            addChunkPlan(chunkPlans, currentChunkPayloadPaths, currentChunkLength);
        }

        return new PackedPayloadArchive(payloadIdsByPath, List.copyOf(payloadLocators), List.copyOf(chunkPlans));
    }

    @NotNull
    public static byte[] encodePayloadIndex(@NotNull PackedPayloadArchive archiveLayout) throws IOException {
        Objects.requireNonNull(archiveLayout);
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeInt(PAYLOAD_INDEX_MAGIC);
            out.writeByte(PAYLOAD_INDEX_VERSION);
            writeVarInt(out, archiveLayout.chunkPlans().size());
            writeVarInt(out, archiveLayout.payloadLocators().size());
            for (ChunkPlan chunkPlan : archiveLayout.chunkPlans()) {
                writeVarInt(out, chunkPlan.uncompressedLength());
            }
            for (PayloadLocator payloadLocator : archiveLayout.payloadLocators()) {
                writeVarInt(out, payloadLocator.chunkId());
                writeVarInt(out, payloadLocator.offset());
                writeVarInt(out, payloadLocator.length());
            }
            out.flush();
            return byteStream.toByteArray();
        }
    }

    @NotNull
    public static DecodedPayloadIndex decodePayloadIndex(@NotNull byte[] payloadIndexBytes) throws IOException {
        Objects.requireNonNull(payloadIndexBytes);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payloadIndexBytes))) {
            int magic = in.readInt();
            if (magic != PAYLOAD_INDEX_MAGIC) {
                throw new IOException("AFMA payload index is missing its magic header");
            }

            int version = in.readUnsignedByte();
            if (version != PAYLOAD_INDEX_VERSION) {
                throw new IOException("Unsupported AFMA payload index version: " + version);
            }

            int chunkCount = readVarInt(in);
            int payloadCount = readVarInt(in);
            if (chunkCount < 0 || payloadCount < 0) {
                throw new IOException("AFMA payload index has invalid counts");
            }

            int[] chunkLengths = new int[chunkCount];
            for (int i = 0; i < chunkCount; i++) {
                chunkLengths[i] = readVarInt(in);
                if (chunkLengths[i] < 0) {
                    throw new IOException("AFMA payload chunk " + i + " has an invalid uncompressed length");
                }
            }

            LinkedHashMap<String, PayloadLocator> payloadLocatorsByPath = new LinkedHashMap<>();
            for (int payloadId = 0; payloadId < payloadCount; payloadId++) {
                int chunkId = readVarInt(in);
                int offset = readVarInt(in);
                int length = readVarInt(in);
                if (chunkId < 0 || chunkId >= chunkCount) {
                    throw new IOException("AFMA payload " + payloadId + " references an invalid payload chunk");
                }
                if (offset < 0 || length < 0 || ((long) offset + (long) length) > chunkLengths[chunkId]) {
                    throw new IOException("AFMA payload " + payloadId + " has an invalid chunk range");
                }
                payloadLocatorsByPath.put(syntheticPayloadPath(payloadId).toLowerCase(Locale.ROOT), new PayloadLocator(chunkId, offset, length));
            }

            if (in.available() > 0) {
                throw new IOException("AFMA payload index contains trailing data");
            }

            return new DecodedPayloadIndex(Map.copyOf(payloadLocatorsByPath), chunkLengths);
        }
    }

    @NotNull
    public static String syntheticPayloadPath(int payloadId) {
        if (payloadId < 0) {
            throw new IllegalArgumentException("AFMA payload id is negative: " + payloadId);
        }
        return "payload/" + Integer.toUnsignedString(payloadId, 36);
    }

    @NotNull
    public static String chunkEntryPath(int chunkId) {
        if (chunkId < 0) {
            throw new IllegalArgumentException("AFMA chunk id is negative: " + chunkId);
        }
        return PAYLOAD_CHUNK_DIRECTORY + "chunk_" + Integer.toUnsignedString(chunkId, 36) + PAYLOAD_CHUNK_EXTENSION;
    }

    public static boolean isWholeChunkPayload(@NotNull PayloadLocator payloadLocator, int[] chunkLengths) {
        Objects.requireNonNull(payloadLocator);
        Objects.requireNonNull(chunkLengths);
        int chunkId = payloadLocator.chunkId();
        return chunkId >= 0
                && chunkId < chunkLengths.length
                && payloadLocator.offset() == 0
                && payloadLocator.length() == chunkLengths[chunkId];
    }

    protected static void addChunkPlan(@NotNull List<ChunkPlan> chunkPlans, @NotNull List<String> chunkPayloadPaths, int chunkLength) {
        int chunkId = chunkPlans.size();
        chunkPlans.add(new ChunkPlan(chunkEntryPath(chunkId), List.copyOf(chunkPayloadPaths), chunkLength));
    }

    protected static void writeVarInt(@NotNull DataOutputStream out, int value) throws IOException {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            out.writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        out.writeByte(remaining);
    }

    protected static int readVarInt(@NotNull DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        while (position < 32) {
            int currentByte = in.readUnsignedByte();
            value |= (currentByte & 0x7F) << position;
            if ((currentByte & 0x80) == 0) {
                return value;
            }
            position += 7;
        }
        throw new IOException("AFMA payload index varint is too large");
    }

    public record PayloadLocator(int chunkId, int offset, int length) {
    }

    public record ChunkPlan(@NotNull String entryPath, @NotNull List<String> payloadPaths, int uncompressedLength) {
    }

    public record PackedPayloadArchive(@NotNull Map<String, Integer> payloadIdsByPath,
                                       @NotNull List<PayloadLocator> payloadLocators,
                                       @NotNull List<ChunkPlan> chunkPlans) {
    }

    public record DecodedPayloadIndex(@NotNull Map<String, PayloadLocator> payloadLocatorsByPath,
                                      int[] chunkLengths) {
    }

}
