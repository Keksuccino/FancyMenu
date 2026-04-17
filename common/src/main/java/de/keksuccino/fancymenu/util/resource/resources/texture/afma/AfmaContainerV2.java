package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AfmaContainerV2 {

    public static final int MAGIC = 0x41464D32; // AFM2
    public static final int VERSION = 1;
    public static final int HEADER_BYTES = 21;
    public static final int CHUNK_DESCRIPTOR_BYTES = 17;
    public static final int COMPRESSION_STORED = 0;
    public static final int COMPRESSION_RAW_DEFLATE = 1;

    private AfmaContainerV2() {
    }

    public static boolean isMagic(int magic) {
        return magic == MAGIC;
    }

    @NotNull
    public static Header readHeader(@NotNull DataInput in) throws IOException {
        Objects.requireNonNull(in);
        int magic = in.readInt();
        if (magic != MAGIC) {
            throw new IOException("AFMA v2 container is missing its magic header");
        }

        int version = in.readUnsignedByte();
        if (version != VERSION) {
            throw new IOException("Unsupported AFMA v2 container version: " + version);
        }

        int metadataLength = in.readInt();
        int frameIndexLength = in.readInt();
        int payloadTableLength = in.readInt();
        int chunkCount = in.readInt();
        if (metadataLength < 0 || frameIndexLength < 0 || payloadTableLength < 0 || chunkCount < 0) {
            throw new IOException("AFMA v2 container header contains invalid lengths");
        }
        return new Header(metadataLength, frameIndexLength, payloadTableLength, chunkCount);
    }

    public static void writeHeader(@NotNull DataOutput out, @NotNull Header header) throws IOException {
        Objects.requireNonNull(out);
        Objects.requireNonNull(header);
        out.writeInt(MAGIC);
        out.writeByte(VERSION);
        out.writeInt(header.metadataLength());
        out.writeInt(header.frameIndexLength());
        out.writeInt(header.payloadTableLength());
        out.writeInt(header.chunkCount());
    }

    @NotNull
    public static List<ChunkDescriptor> readChunkDescriptors(@NotNull DataInput in, int chunkCount) throws IOException {
        Objects.requireNonNull(in);
        ArrayList<ChunkDescriptor> descriptors = new ArrayList<>(chunkCount);
        for (int i = 0; i < chunkCount; i++) {
            long fileOffset = in.readLong();
            int compressedLength = in.readInt();
            int uncompressedLength = in.readInt();
            int compressionMode = in.readUnsignedByte();
            if (fileOffset < 0L || compressedLength < 0 || uncompressedLength < 0) {
                throw new IOException("AFMA v2 chunk descriptor contains invalid lengths");
            }
            if ((compressionMode != COMPRESSION_STORED) && (compressionMode != COMPRESSION_RAW_DEFLATE)) {
                throw new IOException("Unsupported AFMA v2 chunk compression mode: " + compressionMode);
            }
            descriptors.add(new ChunkDescriptor(fileOffset, compressedLength, uncompressedLength, compressionMode));
        }
        return descriptors;
    }

    public static void writeChunkDescriptor(@NotNull DataOutput out, @NotNull ChunkDescriptor descriptor) throws IOException {
        Objects.requireNonNull(out);
        Objects.requireNonNull(descriptor);
        out.writeLong(descriptor.fileOffset());
        out.writeInt(descriptor.compressedLength());
        out.writeInt(descriptor.uncompressedLength());
        out.writeByte(descriptor.compressionMode());
    }

    public record Header(int metadataLength, int frameIndexLength, int payloadTableLength, int chunkCount) {
    }

    public record ChunkDescriptor(long fileOffset, int compressedLength, int uncompressedLength, int compressionMode) {
    }

}
