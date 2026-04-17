package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AfmaPayloadArchiveLayout {

    public static final int PAYLOAD_TABLE_MAGIC = 0x41465054; // AFPT
    public static final int PAYLOAD_TABLE_VERSION = 1;
    public static final int TARGET_CHUNK_BYTES = 256 * 1024;

    @NotNull
    protected final Map<String, Integer> payloadIdsByPath;
    @NotNull
    protected final List<ChunkPlan> chunkPlans;
    @NotNull
    protected final List<AfmaChunkedPayloadHelper.PayloadLocator> payloadLocatorsById;
    @NotNull
    protected final int[] chunkLengths;

    protected AfmaPayloadArchiveLayout(@NotNull Map<String, Integer> payloadIdsByPath,
                                       @NotNull List<ChunkPlan> chunkPlans,
                                       @NotNull List<AfmaChunkedPayloadHelper.PayloadLocator> payloadLocatorsById,
                                       @NotNull int[] chunkLengths) {
        this.payloadIdsByPath = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payloadIdsByPath)));
        this.chunkPlans = List.copyOf(Objects.requireNonNull(chunkPlans));
        this.payloadLocatorsById = List.copyOf(Objects.requireNonNull(payloadLocatorsById));
        this.chunkLengths = Objects.requireNonNull(chunkLengths).clone();
    }

    @NotNull
    public static AfmaPayloadArchiveLayout build(@NotNull Map<String, AfmaStoredPayload> payloads) {
        Objects.requireNonNull(payloads);
        LinkedHashMap<String, Integer> payloadIdsByPath = new LinkedHashMap<>();
        ArrayList<ChunkPlan> chunkPlans = new ArrayList<>();
        ArrayList<AfmaChunkedPayloadHelper.PayloadLocator> payloadLocatorsById = new ArrayList<>();
        ArrayList<Integer> chunkLengths = new ArrayList<>();

        int nextChunkId = 0;
        ChunkBuilder currentChunk = new ChunkBuilder(nextChunkId);
        for (Map.Entry<String, AfmaStoredPayload> entry : payloads.entrySet()) {
            String payloadPath = entry.getKey();
            AfmaStoredPayload payload = Objects.requireNonNull(entry.getValue(), "AFMA payload was NULL for " + payloadPath);
            payloadIdsByPath.put(payloadPath, payloadIdsByPath.size());

            int payloadLength = Math.max(0, payload.length());
            if (shouldStartNewChunk(currentChunk, payloadLength)) {
                chunkPlans.add(currentChunk.build());
                chunkLengths.add(currentChunk.uncompressedLength());
                currentChunk = new ChunkBuilder(++nextChunkId);
            }

            int offset = currentChunk.uncompressedLength();
            currentChunk.addPayload(payloadPath, payloadLength);
            payloadLocatorsById.add(new AfmaChunkedPayloadHelper.PayloadLocator(currentChunk.chunkId(), offset, payloadLength));
        }

        if (!currentChunk.isEmpty()) {
            chunkPlans.add(currentChunk.build());
            chunkLengths.add(currentChunk.uncompressedLength());
        } else if (payloadIdsByPath.isEmpty()) {
            chunkPlans.add(currentChunk.build());
            chunkLengths.add(0);
        }

        return new AfmaPayloadArchiveLayout(payloadIdsByPath, chunkPlans, payloadLocatorsById, toIntArray(chunkLengths));
    }

    @NotNull
    public static AfmaPayloadArchiveLayout build(@NotNull Map<String, AfmaStoredPayload> payloads,
                                                 @NotNull AfmaFrameIndex frameIndex,
                                                 int loopCount) {
        Objects.requireNonNull(frameIndex);
        if (payloads.isEmpty()) {
            return build(payloads);
        }

        AfmaChunkedPayloadHelper.ArchivePackingHints packingHints = AfmaChunkedPayloadHelper.buildPackingHints(frameIndex, loopCount);
        AfmaChunkedPayloadHelper.PackedPayloadArchive packedArchive = AfmaChunkedPayloadHelper.buildArchiveLayout(payloads, packingHints);
        return fromPackedArchive(packedArchive);
    }

    @NotNull
    protected static AfmaPayloadArchiveLayout fromPackedArchive(@NotNull AfmaChunkedPayloadHelper.PackedPayloadArchive packedArchive) {
        Objects.requireNonNull(packedArchive);
        LinkedHashMap<String, Integer> payloadIdsByPath = new LinkedHashMap<>(packedArchive.payloadIdsByPath());
        ArrayList<ChunkPlan> chunkPlans = new ArrayList<>(packedArchive.chunkPlans().size());
        ArrayList<Integer> chunkLengths = new ArrayList<>(packedArchive.chunkPlans().size());
        int chunkId = 0;
        for (AfmaChunkedPayloadHelper.ChunkPlan chunkPlan : packedArchive.chunkPlans()) {
            chunkPlans.add(new ChunkPlan(chunkId++, chunkPlan.payloadPaths(), chunkPlan.uncompressedLength()));
            chunkLengths.add(chunkPlan.uncompressedLength());
        }
        return new AfmaPayloadArchiveLayout(
                payloadIdsByPath,
                chunkPlans,
                new ArrayList<>(packedArchive.payloadLocators()),
                toIntArray(chunkLengths)
        );
    }

    protected static boolean shouldStartNewChunk(@NotNull ChunkBuilder currentChunk, int payloadLength) {
        if (currentChunk.isEmpty()) {
            return false;
        }
        return shouldStartNewChunk(currentChunk.uncompressedLength(), payloadLength);
    }

    public static boolean shouldStartNewChunk(int currentChunkLength, int payloadLength) {
        if (payloadLength > TARGET_CHUNK_BYTES) {
            return true;
        }
        return (currentChunkLength + payloadLength) > TARGET_CHUNK_BYTES;
    }

    @NotNull
    public Map<String, Integer> payloadIdsByPath() {
        return this.payloadIdsByPath;
    }

    @NotNull
    public List<ChunkPlan> chunkPlans() {
        return this.chunkPlans;
    }

    @NotNull
    public List<AfmaChunkedPayloadHelper.PayloadLocator> payloadLocatorsById() {
        return this.payloadLocatorsById;
    }

    @NotNull
    public int[] chunkLengths() {
        return this.chunkLengths.clone();
    }

    @NotNull
    public byte[] encodePayloadTable() throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeInt(PAYLOAD_TABLE_MAGIC);
            out.writeByte(PAYLOAD_TABLE_VERSION);
            writeVarInt(out, this.payloadLocatorsById.size());
            writeVarInt(out, this.chunkLengths.length);
            for (AfmaChunkedPayloadHelper.PayloadLocator locator : this.payloadLocatorsById) {
                writeVarInt(out, locator.chunkId());
                writeVarInt(out, locator.offset());
                writeVarInt(out, locator.length());
            }
            for (int chunkLength : this.chunkLengths) {
                writeVarInt(out, chunkLength);
            }
            out.flush();
            return byteStream.toByteArray();
        }
    }

    @NotNull
    public static DecodedPayloadTable decodePayloadTable(@NotNull byte[] payloadTableBytes) throws IOException {
        Objects.requireNonNull(payloadTableBytes);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payloadTableBytes))) {
            int magic = in.readInt();
            if (magic != PAYLOAD_TABLE_MAGIC) {
                throw new IOException("AFMA payload table is missing its magic header");
            }

            int version = in.readUnsignedByte();
            if (version != PAYLOAD_TABLE_VERSION) {
                throw new IOException("Unsupported AFMA payload table version: " + version);
            }

            int payloadCount = readVarInt(in);
            int chunkCount = readVarInt(in);
            if (payloadCount < 0 || chunkCount < 0) {
                throw new IOException("AFMA payload table counts are invalid");
            }

            LinkedHashMap<String, AfmaChunkedPayloadHelper.PayloadLocator> payloadLocatorsByPath = new LinkedHashMap<>();
            for (int payloadId = 0; payloadId < payloadCount; payloadId++) {
                int chunkId = readVarInt(in);
                int offset = readVarInt(in);
                int length = readVarInt(in);
                payloadLocatorsByPath.put(
                        AfmaChunkedPayloadHelper.syntheticPayloadPath(payloadId).toLowerCase(java.util.Locale.ROOT),
                        new AfmaChunkedPayloadHelper.PayloadLocator(chunkId, offset, length)
                );
            }

            int[] chunkLengths = new int[chunkCount];
            for (int chunkId = 0; chunkId < chunkCount; chunkId++) {
                chunkLengths[chunkId] = readVarInt(in);
            }

            int payloadId = 0;
            for (AfmaChunkedPayloadHelper.PayloadLocator locator : payloadLocatorsByPath.values()) {
                if (locator.chunkId() < 0 || locator.chunkId() >= chunkLengths.length) {
                    throw new IOException("AFMA payload table references an invalid chunk id for payload " + payloadId);
                }
                if (locator.offset() < 0 || locator.length() < 0
                        || ((long) locator.offset() + (long) locator.length()) > chunkLengths[locator.chunkId()]) {
                    throw new IOException("AFMA payload table references an invalid chunk range for payload " + payloadId);
                }
                payloadId++;
            }

            if (in.available() > 0) {
                throw new IOException("AFMA payload table contains trailing data");
            }
            return new DecodedPayloadTable(payloadLocatorsByPath, chunkLengths);
        }
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
        int result = 0;
        int shift = 0;
        for (int i = 0; i < 5; i++) {
            int value = in.readUnsignedByte();
            result |= (value & 0x7F) << shift;
            if ((value & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw new IOException("AFMA payload table varint is too large");
    }

    @NotNull
    protected static int[] toIntArray(@NotNull List<Integer> values) {
        int[] array = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = Objects.requireNonNull(values.get(i));
        }
        return array;
    }

    public record ChunkPlan(int chunkId, @NotNull List<String> payloadPaths, int uncompressedLength) {

        public ChunkPlan {
            payloadPaths = List.copyOf(Objects.requireNonNull(payloadPaths));
        }
    }

    public record DecodedPayloadTable(@NotNull Map<String, AfmaChunkedPayloadHelper.PayloadLocator> payloadLocatorsByPath,
                                      @NotNull int[] chunkLengths) {

        public DecodedPayloadTable {
            payloadLocatorsByPath = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payloadLocatorsByPath)));
            chunkLengths = Objects.requireNonNull(chunkLengths).clone();
        }
    }

    protected static final class ChunkBuilder {

        protected final int chunkId;
        @NotNull
        protected final ArrayList<String> payloadPaths = new ArrayList<>();
        protected int uncompressedLength = 0;

        protected ChunkBuilder(int chunkId) {
            this.chunkId = chunkId;
        }

        protected void addPayload(@NotNull String payloadPath, int payloadLength) {
            this.payloadPaths.add(Objects.requireNonNull(payloadPath));
            this.uncompressedLength += Math.max(0, payloadLength);
        }

        protected boolean isEmpty() {
            return this.payloadPaths.isEmpty();
        }

        protected int chunkId() {
            return this.chunkId;
        }

        protected int uncompressedLength() {
            return this.uncompressedLength;
        }

        @NotNull
        protected ChunkPlan build() {
            return new ChunkPlan(this.chunkId, this.payloadPaths, this.uncompressedLength);
        }
    }

}
