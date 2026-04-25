package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

public final class AfmaBlockInterPayloadHelper {

    public static final int PAYLOAD_MAGIC = 0x41464249; // AFBI
    public static final int PAYLOAD_VERSION = 2;

    private AfmaBlockInterPayloadHelper() {
    }

    @NotNull
    public static byte[] writePayload(int tileSize, int regionWidth, int regionHeight, @NotNull List<TileOperation> tileOperations) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            writePayload(byteStream, tileSize, regionWidth, regionHeight, tileOperations);
            return byteStream.toByteArray();
        }
    }

    public static void writePayload(@NotNull OutputStream out, int tileSize, int regionWidth, int regionHeight,
                                    @NotNull List<TileOperation> tileOperations) throws IOException {
        Objects.requireNonNull(out);
        Objects.requireNonNull(tileOperations);

        int tileCountX = tileCount(regionWidth, tileSize);
        int tileCountY = tileCount(regionHeight, tileSize);
        if (tileOperations.size() != (tileCountX * tileCountY)) {
            throw new IOException("AFMA block inter payload tile count does not match the covered region");
        }

        DataOutputStream dataOut = new DataOutputStream(out);
        dataOut.writeInt(PAYLOAD_MAGIC);
        dataOut.writeByte(PAYLOAD_VERSION);
        dataOut.writeShort(tileCountX);
        dataOut.writeShort(tileCountY);

        int tileIndex = 0;
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            for (int tileX = 0; tileX < tileCountX; tileX++, tileIndex++) {
                int tileWidth = tileDimension(tileX, tileCountX, tileSize, regionWidth);
                int tileHeight = tileDimension(tileY, tileCountY, tileSize, regionHeight);
                writeTileOperation(dataOut, tileOperations.get(tileIndex), tileWidth, tileHeight);
            }
        }
        dataOut.flush();
    }

    protected static void writeTileOperation(@NotNull DataOutputStream out, @NotNull TileOperation operation, int tileWidth, int tileHeight) throws IOException {
        Objects.requireNonNull(operation);
        TileMode mode = Objects.requireNonNull(operation.mode(), "AFMA block inter tile mode was NULL");
        out.writeByte(mode.id());
        switch (mode) {
            case SKIP -> {
            }
            case COPY -> {
                out.writeShort(operation.dx());
                out.writeShort(operation.dy());
            }
            case COPY_DENSE -> {
                int channels = operation.channels();
                byte[] residualBytes = Objects.requireNonNull(operation.primaryBytes(), "AFMA block inter dense residual bytes were NULL");
                validateChannels(channels);
                validateLength(expectedDenseResidualBytes(tileWidth, tileHeight, channels), residualBytes.length, "AFMA block inter dense residual payload");
                out.writeShort(operation.dx());
                out.writeShort(operation.dy());
                out.writeByte(channels);
                out.write(residualBytes);
            }
            case COPY_SPARSE -> {
                AfmaSparsePayload sparsePayload = Objects.requireNonNull(operation.sparsePayload(), "AFMA block inter sparse tile metadata was NULL");
                byte[] layoutBytes = Objects.requireNonNull(operation.primaryBytes(), "AFMA block inter sparse layout bytes were NULL");
                byte[] residualBytes = Objects.requireNonNull(operation.secondaryBytes(), "AFMA block inter sparse residual bytes were NULL");
                sparsePayload.validateMetadata("AFMA block inter sparse tile metadata");
                validateChannels(sparsePayload.getChannels());
                validateUnsignedShortLength(sparsePayload.getChangedPixelCount(), "AFMA block inter sparse changed-pixel count");
                validateUnsignedShortLength(layoutBytes.length, "AFMA block inter sparse layout payload");
                validateUnsignedShortLength(sparsePayload.getAlphaChangedPixelCount(), "AFMA block inter sparse alpha-change count");
                AfmaSparsePayloadHelper.validateLayout(layoutBytes, 0, layoutBytes.length, tileWidth, tileHeight,
                        sparsePayload.getLayoutCodec(), sparsePayload.getChangedPixelCount());
                validateLength(
                        AfmaResidualPayloadHelper.expectedSparseResidualBytes(sparsePayload.getChangedPixelCount(), sparsePayload),
                        residualBytes.length,
                        "AFMA block inter sparse residual payload"
                );
                out.writeShort(operation.dx());
                out.writeShort(operation.dy());
                out.writeByte(sparsePayload.getChannels());
                out.writeShort(sparsePayload.getChangedPixelCount());
                out.writeByte(sparsePayload.getLayoutCodec().getId());
                out.writeByte(sparsePayload.getResidualCodec().getId());
                out.writeByte(sparsePayload.getAlphaMode().getId());
                out.writeShort(sparsePayload.getAlphaChangedPixelCount());
                out.writeShort(layoutBytes.length);
                out.write(layoutBytes);
                out.write(residualBytes);
            }
            case RAW -> {
                int channels = operation.channels();
                byte[] rawBytes = Objects.requireNonNull(operation.primaryBytes(), "AFMA block inter raw tile bytes were NULL");
                validateChannels(channels);
                validateLength(expectedRawTileBytes(tileWidth, tileHeight, channels), rawBytes.length, "AFMA block inter raw tile payload");
                out.writeByte(channels);
                out.write(rawBytes);
            }
        }
    }

    public static void walkPayload(@NotNull byte[] payloadBytes, int tileSize, int regionWidth, int regionHeight,
                                   @NotNull TileConsumer consumer) throws IOException {
        Objects.requireNonNull(payloadBytes);
        Objects.requireNonNull(consumer);
        walkPayload(payloadBytes, 0, payloadBytes.length, tileSize, regionWidth, regionHeight, consumer);
    }

    public static void walkPayload(@NotNull byte[] payloadBytes, int offset, int length, int tileSize, int regionWidth, int regionHeight,
                                   @NotNull TileConsumer consumer) throws IOException {
        Objects.requireNonNull(payloadBytes);
        Objects.requireNonNull(consumer);
        if (offset < 0 || length < 0 || ((long) offset + (long) length) > payloadBytes.length) {
            throw new IOException("AFMA block inter payload slice is invalid");
        }
        if (tileSize <= 0 || regionWidth <= 0 || regionHeight <= 0) {
            throw new IOException("AFMA block inter payload dimensions are invalid");
        }

        int expectedTileCountX = tileCount(regionWidth, tileSize);
        int expectedTileCountY = tileCount(regionHeight, tileSize);
        PayloadReader reader = new PayloadReader(payloadBytes, offset, length);
        int magic = reader.readInt();
        if (magic != PAYLOAD_MAGIC) {
            throw new IOException("AFMA block inter payload is missing its magic header");
        }

        int version = reader.readUnsignedByte();
        if ((version != 1) && (version != PAYLOAD_VERSION)) {
            throw new IOException("Unsupported AFMA block inter payload version: " + version);
        }

        int tileCountX = reader.readUnsignedShort();
        int tileCountY = reader.readUnsignedShort();
        if ((tileCountX != expectedTileCountX) || (tileCountY != expectedTileCountY)) {
            throw new IOException("AFMA block inter payload tile layout does not match the descriptor");
        }

        for (int tileY = 0; tileY < tileCountY; tileY++) {
            int localY = tileY * tileSize;
            int tileHeight = tileDimension(tileY, tileCountY, tileSize, regionHeight);
            for (int tileX = 0; tileX < tileCountX; tileX++) {
                int localX = tileX * tileSize;
                int tileWidth = tileDimension(tileX, tileCountX, tileSize, regionWidth);
                TileMode mode = TileMode.byId(reader.readUnsignedByte());
                int dx = 0;
                int dy = 0;
                int channels = 0;
                int changedPixelCount = 0;
                AfmaSparsePayload sparsePayload = null;
                int primaryOffset = -1;
                int primaryLength = 0;
                int secondaryOffset = -1;
                int secondaryLength = 0;
                switch (mode) {
                    case SKIP -> {
                    }
                    case COPY -> {
                        dx = reader.readShort();
                        dy = reader.readShort();
                    }
                    case COPY_DENSE -> {
                        dx = reader.readShort();
                        dy = reader.readShort();
                        channels = reader.readUnsignedByte();
                        validateChannels(channels);
                        primaryLength = expectedDenseResidualBytes(tileWidth, tileHeight, channels);
                        validateExpectedLength(primaryLength, "AFMA block inter dense residual payload");
                        primaryOffset = reader.position();
                        reader.skip(primaryLength);
                    }
                    case COPY_SPARSE -> {
                        dx = reader.readShort();
                        dy = reader.readShort();
                        channels = reader.readUnsignedByte();
                        changedPixelCount = reader.readUnsignedShort();
                        validateChannels(channels);
                        if (version >= 2) {
                            AfmaSparseLayoutCodec layoutCodec = AfmaSparseLayoutCodec.byId(reader.readUnsignedByte());
                            AfmaResidualCodec residualCodec = AfmaResidualCodec.byId(reader.readUnsignedByte());
                            AfmaAlphaResidualMode alphaMode = AfmaAlphaResidualMode.byId(reader.readUnsignedByte());
                            int alphaChangedPixelCount = reader.readUnsignedShort();
                            primaryLength = reader.readUnsignedShort();
                            validateExpectedLength(primaryLength, "AFMA block inter sparse layout payload");
                            sparsePayload = new AfmaSparsePayload(null, changedPixelCount, channels,
                                    layoutCodec, residualCodec, alphaMode, alphaChangedPixelCount);
                            try {
                                sparsePayload.validateMetadata("AFMA block inter sparse tile metadata");
                            } catch (IllegalArgumentException ex) {
                                throw new IOException(ex.getMessage(), ex);
                            }
                        } else {
                            primaryLength = AfmaResidualPayloadHelper.expectedSparseMaskBytes(tileWidth, tileHeight);
                            validateExpectedLength(primaryLength, "AFMA block inter sparse mask payload");
                            sparsePayload = new AfmaSparsePayload(
                                    null,
                                    changedPixelCount,
                                    channels,
                                    AfmaSparseLayoutCodec.BITMASK,
                                    AfmaResidualCodec.INTERLEAVED,
                                    (channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) ? AfmaAlphaResidualMode.FULL : AfmaAlphaResidualMode.NONE,
                                    0
                            );
                        }
                        primaryOffset = reader.position();
                        reader.skip(primaryLength);
                        secondaryLength = AfmaResidualPayloadHelper.expectedSparseResidualBytes(changedPixelCount, Objects.requireNonNull(sparsePayload));
                        validateExpectedLength(secondaryLength, "AFMA block inter sparse residual payload");
                        secondaryOffset = reader.position();
                        reader.skip(secondaryLength);
                    }
                    case RAW -> {
                        channels = reader.readUnsignedByte();
                        validateChannels(channels);
                        primaryLength = expectedRawTileBytes(tileWidth, tileHeight, channels);
                        validateExpectedLength(primaryLength, "AFMA block inter raw tile payload");
                        primaryOffset = reader.position();
                        reader.skip(primaryLength);
                    }
                }

                consumer.accept(localX, localY, tileWidth, tileHeight, mode, dx, dy, channels, changedPixelCount, sparsePayload,
                        payloadBytes, primaryOffset, primaryLength, secondaryOffset, secondaryLength);
            }
        }

        if (reader.remaining() > 0) {
            throw new IOException("AFMA block inter payload contains trailing data");
        }
    }

    public static void validatePayload(@NotNull byte[] payloadBytes, int tileSize, int regionX, int regionY, int regionWidth, int regionHeight,
                                       int canvasWidth, int canvasHeight) throws IOException {
        validatePayload(payloadBytes, 0, Objects.requireNonNull(payloadBytes).length, tileSize, regionX, regionY, regionWidth, regionHeight, canvasWidth, canvasHeight);
    }

    public static void validatePayload(@NotNull byte[] payloadBytes, int offset, int length, int tileSize, int regionX, int regionY, int regionWidth, int regionHeight,
                                       int canvasWidth, int canvasHeight) throws IOException {
        walkPayload(payloadBytes, offset, length, tileSize, regionWidth, regionHeight, (localX, localY, tileWidth, tileHeight, mode, dx, dy, channels, changedPixelCount, sparsePayload, tilePayloadBytes, primaryOffset, primaryLength, secondaryOffset, secondaryLength) -> {
            int dstX = regionX + localX;
            int dstY = regionY + localY;
            if (dstX < 0 || dstY < 0 || (dstX + tileWidth) > canvasWidth || (dstY + tileHeight) > canvasHeight) {
                throw new IOException("AFMA block inter tile destination exceeds the canvas");
            }

            if ((mode == TileMode.COPY) || (mode == TileMode.COPY_DENSE) || (mode == TileMode.COPY_SPARSE)) {
                int srcX = dstX + dx;
                int srcY = dstY + dy;
                if (srcX < 0 || srcY < 0 || (srcX + tileWidth) > canvasWidth || (srcY + tileHeight) > canvasHeight) {
                    throw new IOException("AFMA block inter tile source exceeds the canvas");
                }
            }

            if (mode == TileMode.COPY_SPARSE) {
                int maxPixels = tileWidth * tileHeight;
                if (changedPixelCount <= 0 || changedPixelCount > maxPixels) {
                    throw new IOException("AFMA block inter sparse tile has an invalid changed pixel count");
                }
                AfmaSparsePayload resolvedSparsePayload = Objects.requireNonNull(sparsePayload, "AFMA block inter sparse tile metadata was NULL");
                try {
                    AfmaSparsePayloadHelper.validateLayout(tilePayloadBytes, primaryOffset, primaryLength, tileWidth, tileHeight,
                            resolvedSparsePayload.getLayoutCodec(), resolvedSparsePayload.getChangedPixelCount());
                    AfmaResidualPayloadHelper.validateSparsePayload(tilePayloadBytes, secondaryOffset, secondaryLength,
                            resolvedSparsePayload.getChangedPixelCount(), resolvedSparsePayload);
                    AfmaResidualPayloadHelper.validateSparseAlphaMaskPopulation(tilePayloadBytes, secondaryOffset,
                            resolvedSparsePayload.getChangedPixelCount(), resolvedSparsePayload.getChannels(),
                            resolvedSparsePayload.getAlphaMode(), resolvedSparsePayload.getAlphaChangedPixelCount());
                } catch (IllegalStateException | IllegalArgumentException ex) {
                    throw new IOException("AFMA block inter sparse tile metadata does not match its payload", ex);
                }
            }
        });
    }

    public static int tileCount(int length, int tileSize) {
        if (length <= 0 || tileSize <= 0) {
            return 0;
        }
        return (length + tileSize - 1) / tileSize;
    }

    public static int tileDimension(int tileIndex, int tileCount, int tileSize, int regionLength) {
        if (tileCount <= 0 || tileIndex < 0 || tileIndex >= tileCount) {
            return 0;
        }
        int start = tileIndex * tileSize;
        int remaining = regionLength - start;
        return Math.max(0, Math.min(tileSize, remaining));
    }

    public static int expectedDenseResidualBytes(int width, int height, int channels) {
        return AfmaResidualPayloadHelper.expectedDenseResidualBytes(width, height, channels);
    }

    public static int expectedRawTileBytes(int width, int height, int channels) {
        if ((width <= 0) || (height <= 0)) {
            return 0;
        }
        validateChannels(channels);
        long totalBytes = (long) width * (long) height * channels;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    protected static void validateChannels(int channels) {
        if (!AfmaResidualPayloadHelper.isValidChannelCount(channels)) {
            throw new IllegalArgumentException("AFMA block inter channel count is invalid: " + channels);
        }
    }

    protected static void validateLength(int expectedLength, int actualLength, @NotNull String context) throws IOException {
        if ((expectedLength <= 0) || (actualLength != expectedLength)) {
            throw new IOException(context + " size is invalid");
        }
    }

    protected static void validateExpectedLength(int expectedLength, @NotNull String context) throws IOException {
        if (expectedLength <= 0) {
            throw new IOException(context + " size is invalid");
        }
    }

    protected static void validateUnsignedShortLength(int value, @NotNull String context) throws IOException {
        if ((value < 0) || (value > 0xFFFF)) {
            throw new IOException(context + " size is invalid");
        }
    }

    public enum TileMode {
        SKIP(0),
        COPY(1),
        COPY_DENSE(2),
        COPY_SPARSE(3),
        RAW(4);

        private final int id;

        TileMode(int id) {
            this.id = id;
        }

        public int id() {
            return this.id;
        }

        @NotNull
        public static TileMode byId(int id) throws IOException {
            for (TileMode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            throw new IOException("Unknown AFMA block inter tile mode id: " + id);
        }
    }

    public record TileOperation(@NotNull TileMode mode, int dx, int dy, int channels, int changedPixelCount,
                                @Nullable byte[] primaryBytes, @Nullable byte[] secondaryBytes,
                                @Nullable AfmaSparsePayload sparsePayload) {

        public TileOperation {
            Objects.requireNonNull(mode);
            if (mode == TileMode.COPY_SPARSE) {
                AfmaSparsePayload resolvedSparsePayload = Objects.requireNonNull(sparsePayload, "AFMA block inter sparse tile metadata was NULL");
                resolvedSparsePayload.validateMetadata("AFMA block inter sparse tile metadata");
                if (channels != resolvedSparsePayload.getChannels()) {
                    throw new IllegalArgumentException("AFMA block inter sparse tile channels do not match their metadata");
                }
                if (changedPixelCount != resolvedSparsePayload.getChangedPixelCount()) {
                    throw new IllegalArgumentException("AFMA block inter sparse tile changed-pixel count does not match its metadata");
                }
            } else if (sparsePayload != null) {
                throw new IllegalArgumentException("AFMA block inter tile metadata is only valid for sparse tiles");
            }
        }
    }

    @FunctionalInterface
    public interface TileConsumer {
        void accept(int localX, int localY, int tileWidth, int tileHeight, @NotNull TileMode mode, int dx, int dy,
                    int channels, int changedPixelCount, @Nullable AfmaSparsePayload sparsePayload, @NotNull byte[] payloadBytes,
                    int primaryOffset, int primaryLength, int secondaryOffset, int secondaryLength) throws IOException;
    }

    protected static final class PayloadReader {

        @NotNull
        private final byte[] payloadBytes;
        private final int endOffset;
        private int offset;

        protected PayloadReader(@NotNull byte[] payloadBytes, int offset, int length) {
            this.payloadBytes = payloadBytes;
            this.offset = offset;
            this.endOffset = offset + length;
        }

        protected int readInt() throws IOException {
            this.ensureAvailable(4);
            int value = ((this.payloadBytes[this.offset] & 0xFF) << 24)
                    | ((this.payloadBytes[this.offset + 1] & 0xFF) << 16)
                    | ((this.payloadBytes[this.offset + 2] & 0xFF) << 8)
                    | (this.payloadBytes[this.offset + 3] & 0xFF);
            this.offset += 4;
            return value;
        }

        protected int readUnsignedByte() throws IOException {
            this.ensureAvailable(1);
            return this.payloadBytes[this.offset++] & 0xFF;
        }

        protected int readUnsignedShort() throws IOException {
            this.ensureAvailable(2);
            int value = ((this.payloadBytes[this.offset] & 0xFF) << 8)
                    | (this.payloadBytes[this.offset + 1] & 0xFF);
            this.offset += 2;
            return value;
        }

        protected int readShort() throws IOException {
            return (short) this.readUnsignedShort();
        }

        protected int position() {
            return this.offset;
        }

        protected int remaining() {
            return this.endOffset - this.offset;
        }

        protected void skip(int length) throws IOException {
            this.ensureAvailable(length);
            this.offset += length;
        }

        protected void ensureAvailable(int length) throws IOException {
            if (length < 0 || (this.offset + length) > this.endOffset) {
                throw new IOException("AFMA block inter payload ended early");
            }
        }
    }

}
