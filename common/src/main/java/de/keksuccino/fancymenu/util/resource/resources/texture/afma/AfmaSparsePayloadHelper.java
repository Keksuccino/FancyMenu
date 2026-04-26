package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public final class AfmaSparsePayloadHelper {

    public static final int TILE_MASK_TILE_SIZE = 8;

    private AfmaSparsePayloadHelper() {
    }

    @NotNull
    public static byte[] buildBitmaskLayout(int width, int height, @NotNull int[] changedIndices, int changedPixelCount) {
        int expectedBytes = AfmaResidualPayloadHelper.expectedSparseMaskBytes(width, height);
        if ((expectedBytes <= 0) || (changedPixelCount <= 0)) {
            return new byte[0];
        }

        byte[] maskBytes = new byte[expectedBytes];
        for (int i = 0; i < changedPixelCount; i++) {
            AfmaResidualPayloadHelper.setMaskBit(maskBytes, changedIndices[i]);
        }
        return maskBytes;
    }

    @NotNull
    public static byte[] buildCoordListLayout(@NotNull int[] changedIndices, int changedPixelCount) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            int previousIndex = -1;
            for (int i = 0; i < changedPixelCount; i++) {
                int index = changedIndices[i];
                writeVarInt(out, index - previousIndex - 1);
                previousIndex = index;
            }
            out.flush();
            return byteStream.toByteArray();
        }
    }

    @NotNull
    public static byte[] buildRowSpanLayout(int width, @NotNull int[] changedIndices, int changedPixelCount) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            int changedRowCount = 0;
            int cursor = 0;
            while (cursor < changedPixelCount) {
                changedRowCount++;
                int row = changedIndices[cursor] / width;
                while ((cursor < changedPixelCount) && ((changedIndices[cursor] / width) == row)) {
                    cursor++;
                }
            }

            writeVarInt(out, changedRowCount);
            int previousRow = -1;
            cursor = 0;
            while (cursor < changedPixelCount) {
                int row = changedIndices[cursor] / width;
                writeVarInt(out, row - previousRow - 1);
                previousRow = row;

                int rowStart = cursor;
                int spanCount = 0;
                while ((cursor < changedPixelCount) && ((changedIndices[cursor] / width) == row)) {
                    int startX = changedIndices[cursor] % width;
                    int endX = startX + 1;
                    cursor++;
                    while ((cursor < changedPixelCount)
                            && ((changedIndices[cursor] / width) == row)
                            && ((changedIndices[cursor] % width) == endX)) {
                        endX++;
                        cursor++;
                    }
                    spanCount++;
                }

                writeVarInt(out, spanCount);
                int previousEndX = 0;
                for (int index = rowStart; index < cursor; ) {
                    int startX = changedIndices[index] % width;
                    int endX = startX + 1;
                    index++;
                    while ((index < cursor) && ((changedIndices[index] % width) == endX)) {
                        endX++;
                        index++;
                    }
                    writeVarInt(out, startX - previousEndX);
                    writeVarInt(out, endX - startX - 1);
                    previousEndX = endX;
                }
            }
            out.flush();
            return byteStream.toByteArray();
        }
    }

    @NotNull
    public static byte[] buildTileMaskLayout(int width, int height, @NotNull int[] changedIndices, int changedPixelCount) throws IOException {
        int tileCountX = tileCount(width, TILE_MASK_TILE_SIZE);
        int tileCountY = tileCount(height, TILE_MASK_TILE_SIZE);
        int tileCount = tileCountX * tileCountY;
        int coarseMaskBytes = AfmaResidualPayloadHelper.expectedSparseBitsetBytes(tileCount);
        byte[] coarseMask = new byte[coarseMaskBytes];
        byte[][] localMasks = new byte[tileCount][];

        for (int i = 0; i < changedPixelCount; i++) {
            int changedIndex = changedIndices[i];
            int x = changedIndex % width;
            int y = changedIndex / width;
            int tileX = x / TILE_MASK_TILE_SIZE;
            int tileY = y / TILE_MASK_TILE_SIZE;
            int tileIndex = (tileY * tileCountX) + tileX;
            AfmaResidualPayloadHelper.setMaskBit(coarseMask, tileIndex);

            int tileWidth = tileDimension(tileX, tileCountX, TILE_MASK_TILE_SIZE, width);
            int tileHeight = tileDimension(tileY, tileCountY, TILE_MASK_TILE_SIZE, height);
            byte[] localMask = localMasks[tileIndex];
            if (localMask == null) {
                localMask = new byte[AfmaResidualPayloadHelper.expectedSparseMaskBytes(tileWidth, tileHeight)];
                localMasks[tileIndex] = localMask;
            }

            int localX = x - (tileX * TILE_MASK_TILE_SIZE);
            int localY = y - (tileY * TILE_MASK_TILE_SIZE);
            AfmaResidualPayloadHelper.setMaskBit(localMask, (localY * tileWidth) + localX);
        }

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.write(coarseMask);
            for (int tileIndex = 0; tileIndex < tileCount; tileIndex++) {
                if (!AfmaResidualPayloadHelper.isMaskBitSet(coarseMask, tileIndex)) {
                    continue;
                }
                byte[] localMask = Objects.requireNonNull(localMasks[tileIndex], "AFMA tile sparse mask was NULL");
                out.write(localMask);
            }
            out.flush();
            return byteStream.toByteArray();
        }
    }

    public static void validateLayout(@NotNull byte[] layoutBytes, int offset, int length, int width, int height,
                                      @NotNull AfmaSparseLayoutCodec layoutCodec, int changedPixelCount) throws IOException {
        walkChangedPixels(layoutBytes, offset, length, width, height, layoutCodec, changedPixelCount, (localIndex, sequenceIndex) -> {
        });
    }

    public static void walkChangedPixels(@NotNull byte[] layoutBytes, int offset, int length, int width, int height,
                                         @NotNull AfmaSparseLayoutCodec layoutCodec, int changedPixelCount,
                                         @NotNull ChangedPixelConsumer consumer) throws IOException {
        Objects.requireNonNull(layoutBytes);
        Objects.requireNonNull(layoutCodec);
        Objects.requireNonNull(consumer);
        if ((width <= 0) || (height <= 0) || (changedPixelCount <= 0)) {
            throw new IOException("AFMA sparse payload dimensions are invalid");
        }
        if ((offset < 0) || (length < 0) || (((long) offset + length) > layoutBytes.length)) {
            throw new IOException("AFMA sparse layout payload slice is invalid");
        }

        switch (layoutCodec) {
            case BITMASK -> walkBitmask(layoutBytes, offset, length, width, height, changedPixelCount, consumer);
            case ROW_SPANS -> walkRowSpans(layoutBytes, offset, length, width, height, changedPixelCount, consumer);
            case TILE_MASK -> walkTileMask(layoutBytes, offset, length, width, height, changedPixelCount, consumer);
            case COORD_LIST -> walkCoordList(layoutBytes, offset, length, width, height, changedPixelCount, consumer);
        }
    }

    protected static void walkBitmask(@NotNull byte[] layoutBytes, int offset, int length, int width, int height, int changedPixelCount,
                                      @NotNull ChangedPixelConsumer consumer) throws IOException {
        int expectedMaskBytes = AfmaResidualPayloadHelper.expectedSparseMaskBytes(width, height);
        if (length != expectedMaskBytes) {
            throw new IOException("AFMA sparse bitmask payload size does not match the frame descriptor");
        }

        int emittedPixels = 0;
        int totalPixels = width * height;
        for (int localIndex = 0; localIndex < totalPixels; localIndex++) {
            if (!isMaskBitSet(layoutBytes, offset, localIndex)) {
                continue;
            }
            consumer.accept(localIndex, emittedPixels++);
        }
        if (emittedPixels != changedPixelCount) {
            throw new IOException("AFMA sparse bitmask payload changed-pixel count does not match its descriptor");
        }
    }

    protected static void walkCoordList(@NotNull byte[] layoutBytes, int offset, int length, int width, int height, int changedPixelCount,
                                        @NotNull ChangedPixelConsumer consumer) throws IOException {
        int totalPixels = width * height;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(layoutBytes, offset, length))) {
            int previousIndex = -1;
            for (int sequenceIndex = 0; sequenceIndex < changedPixelCount; sequenceIndex++) {
                int delta = readVarInt(in);
                int currentIndex = previousIndex + delta + 1;
                if ((currentIndex <= previousIndex) || (currentIndex >= totalPixels)) {
                    throw new IOException("AFMA sparse coordinate payload contains an invalid pixel index");
                }
                consumer.accept(currentIndex, sequenceIndex);
                previousIndex = currentIndex;
            }
            if (in.available() > 0) {
                throw new IOException("AFMA sparse coordinate payload contains trailing data");
            }
        }
    }

    protected static void walkRowSpans(@NotNull byte[] layoutBytes, int offset, int length, int width, int height, int changedPixelCount,
                                       @NotNull ChangedPixelConsumer consumer) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(layoutBytes, offset, length))) {
            int changedRowCount = readVarInt(in);
            int previousRow = -1;
            int emittedPixels = 0;
            for (int rowIndex = 0; rowIndex < changedRowCount; rowIndex++) {
                int rowDelta = readVarInt(in);
                int row = previousRow + rowDelta + 1;
                if ((row <= previousRow) || (row >= height)) {
                    throw new IOException("AFMA sparse row-span payload contains an invalid row index");
                }

                int spanCount = readVarInt(in);
                int previousEndX = 0;
                for (int spanIndex = 0; spanIndex < spanCount; spanIndex++) {
                    int startDelta = readVarInt(in);
                    int lengthMinusOne = readVarInt(in);
                    int startX = previousEndX + startDelta;
                    int spanLength = lengthMinusOne + 1;
                    int endX = startX + spanLength;
                    if ((startX < 0) || (spanLength <= 0) || (endX > width)) {
                        throw new IOException("AFMA sparse row-span payload contains an invalid span");
                    }

                    int rowOffset = row * width;
                    for (int x = startX; x < endX; x++) {
                        consumer.accept(rowOffset + x, emittedPixels++);
                    }
                    previousEndX = endX;
                }
                previousRow = row;
            }

            if (emittedPixels != changedPixelCount) {
                throw new IOException("AFMA sparse row-span payload changed-pixel count does not match its descriptor");
            }
            if (in.available() > 0) {
                throw new IOException("AFMA sparse row-span payload contains trailing data");
            }
        }
    }

    protected static void walkTileMask(@NotNull byte[] layoutBytes, int offset, int length, int width, int height, int changedPixelCount,
                                       @NotNull ChangedPixelConsumer consumer) throws IOException {
        int tileCountX = tileCount(width, TILE_MASK_TILE_SIZE);
        int tileCountY = tileCount(height, TILE_MASK_TILE_SIZE);
        int tileCount = tileCountX * tileCountY;
        int coarseMaskBytes = AfmaResidualPayloadHelper.expectedSparseBitsetBytes(tileCount);
        if (length < coarseMaskBytes) {
            throw new IOException("AFMA sparse tile-mask payload is truncated");
        }

        int payloadOffset = offset;
        int coarseMaskOffset = payloadOffset;
        payloadOffset += coarseMaskBytes;
        int[] localMaskOffsets = new int[tileCount];
        int emittedPixels = 0;
        for (int tileIndex = 0; tileIndex < tileCount; tileIndex++) {
            if (!isMaskBitSet(layoutBytes, coarseMaskOffset, tileIndex)) {
                localMaskOffsets[tileIndex] = -1;
                continue;
            }

            int tileX = tileIndex % tileCountX;
            int tileY = tileIndex / tileCountX;
            int tileWidth = tileDimension(tileX, tileCountX, TILE_MASK_TILE_SIZE, width);
            int tileHeight = tileDimension(tileY, tileCountY, TILE_MASK_TILE_SIZE, height);
            int localMaskBytes = AfmaResidualPayloadHelper.expectedSparseMaskBytes(tileWidth, tileHeight);
            if ((payloadOffset + localMaskBytes) > (offset + length)) {
                throw new IOException("AFMA sparse tile-mask payload is truncated");
            }
            localMaskOffsets[tileIndex] = payloadOffset;
            payloadOffset += localMaskBytes;
        }

        // Emit pixels in global row-major order so TILE_MASK uses the same changed-pixel
        // sequencing as every other sparse layout and as the residual encoder input.
        for (int tileRow = 0; tileRow < tileCountY; tileRow++) {
            int baseTileY = tileRow * TILE_MASK_TILE_SIZE;
            int tileHeight = tileDimension(tileRow, tileCountY, TILE_MASK_TILE_SIZE, height);
            for (int localY = 0; localY < tileHeight; localY++) {
                int rowOffset = (baseTileY + localY) * width;
                for (int tileX = 0; tileX < tileCountX; tileX++) {
                    int tileIndex = (tileRow * tileCountX) + tileX;
                    int localMaskOffset = localMaskOffsets[tileIndex];
                    if (localMaskOffset < 0) {
                        continue;
                    }

                    int tileWidth = tileDimension(tileX, tileCountX, TILE_MASK_TILE_SIZE, width);
                    int baseX = tileX * TILE_MASK_TILE_SIZE;
                    int rowBitIndex = localY * tileWidth;
                    for (int localX = 0; localX < tileWidth; localX++) {
                        if (!isMaskBitSet(layoutBytes, localMaskOffset, rowBitIndex + localX)) {
                            continue;
                        }
                        consumer.accept(rowOffset + baseX + localX, emittedPixels++);
                    }
                }
            }
        }

        if (emittedPixels != changedPixelCount) {
            throw new IOException("AFMA sparse tile-mask payload changed-pixel count does not match its descriptor");
        }
        if (payloadOffset != (offset + length)) {
            throw new IOException("AFMA sparse tile-mask payload contains trailing data");
        }
    }

    protected static int tileCount(int size, int tileSize) {
        return (size + tileSize - 1) / tileSize;
    }

    protected static int tileDimension(int tileIndex, int tileCount, int tileSize, int fullSize) {
        return (tileIndex == (tileCount - 1)) ? (fullSize - (tileIndex * tileSize)) : tileSize;
    }

    protected static boolean isMaskBitSet(@NotNull byte[] maskBytes, int byteOffset, int bitIndex) {
        return (maskBytes[byteOffset + (bitIndex >>> 3)] & (1 << (7 - (bitIndex & 7)))) != 0;
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
        throw new IOException("AFMA sparse payload varint is too large");
    }

    @FunctionalInterface
    public interface ChangedPixelConsumer {
        void accept(int localIndex, int sequenceIndex) throws IOException;
    }

}
