package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class AfmaBlockInterPayloadHelper {

    public static final int PAYLOAD_MAGIC = 0x41464249; // AFBI
    public static final int PAYLOAD_VERSION = 1;

    private AfmaBlockInterPayloadHelper() {
    }

    @NotNull
    public static byte[] writePayload(int tileSize, int regionWidth, int regionHeight, @NotNull List<TileOperation> tileOperations) throws IOException {
        Objects.requireNonNull(tileOperations);

        int tileCountX = tileCount(regionWidth, tileSize);
        int tileCountY = tileCount(regionHeight, tileSize);
        if (tileOperations.size() != (tileCountX * tileCountY)) {
            throw new IOException("AFMA block inter payload tile count does not match the covered region");
        }

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeInt(PAYLOAD_MAGIC);
            out.writeByte(PAYLOAD_VERSION);
            out.writeShort(tileCountX);
            out.writeShort(tileCountY);

            int tileIndex = 0;
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (int tileX = 0; tileX < tileCountX; tileX++, tileIndex++) {
                    int tileWidth = tileDimension(tileX, tileCountX, tileSize, regionWidth);
                    int tileHeight = tileDimension(tileY, tileCountY, tileSize, regionHeight);
                    writeTileOperation(out, tileOperations.get(tileIndex), tileWidth, tileHeight);
                }
            }
            out.flush();
            return byteStream.toByteArray();
        }
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
                int channels = operation.channels();
                int changedPixelCount = operation.changedPixelCount();
                byte[] maskBytes = Objects.requireNonNull(operation.primaryBytes(), "AFMA block inter sparse mask bytes were NULL");
                byte[] residualBytes = Objects.requireNonNull(operation.secondaryBytes(), "AFMA block inter sparse residual bytes were NULL");
                validateChannels(channels);
                validateLength(AfmaResidualPayloadHelper.expectedSparseMaskBytes(tileWidth, tileHeight), maskBytes.length, "AFMA block inter sparse mask payload");
                validateLength(AfmaResidualPayloadHelper.expectedSparseResidualBytes(changedPixelCount, channels), residualBytes.length, "AFMA block inter sparse residual payload");
                out.writeShort(operation.dx());
                out.writeShort(operation.dy());
                out.writeByte(channels);
                out.writeShort(changedPixelCount);
                out.write(maskBytes);
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
        if (tileSize <= 0 || regionWidth <= 0 || regionHeight <= 0) {
            throw new IOException("AFMA block inter payload dimensions are invalid");
        }

        int expectedTileCountX = tileCount(regionWidth, tileSize);
        int expectedTileCountY = tileCount(regionHeight, tileSize);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payloadBytes))) {
            int magic = in.readInt();
            if (magic != PAYLOAD_MAGIC) {
                throw new IOException("AFMA block inter payload is missing its magic header");
            }

            int version = in.readUnsignedByte();
            if (version != PAYLOAD_VERSION) {
                throw new IOException("Unsupported AFMA block inter payload version: " + version);
            }

            int tileCountX = in.readUnsignedShort();
            int tileCountY = in.readUnsignedShort();
            if ((tileCountX != expectedTileCountX) || (tileCountY != expectedTileCountY)) {
                throw new IOException("AFMA block inter payload tile layout does not match the descriptor");
            }

            for (int tileY = 0; tileY < tileCountY; tileY++) {
                int localY = tileY * tileSize;
                int tileHeight = tileDimension(tileY, tileCountY, tileSize, regionHeight);
                for (int tileX = 0; tileX < tileCountX; tileX++) {
                    int localX = tileX * tileSize;
                    int tileWidth = tileDimension(tileX, tileCountX, tileSize, regionWidth);
                    TileMode mode = TileMode.byId(in.readUnsignedByte());
                    int dx = 0;
                    int dy = 0;
                    int channels = 0;
                    int changedPixelCount = 0;
                    byte[] primaryBytes = null;
                    byte[] secondaryBytes = null;
                    switch (mode) {
                        case SKIP -> {
                        }
                        case COPY -> {
                            dx = in.readShort();
                            dy = in.readShort();
                        }
                        case COPY_DENSE -> {
                            dx = in.readShort();
                            dy = in.readShort();
                            channels = in.readUnsignedByte();
                            validateChannels(channels);
                            primaryBytes = in.readNBytes(expectedDenseResidualBytes(tileWidth, tileHeight, channels));
                            validateLength(expectedDenseResidualBytes(tileWidth, tileHeight, channels), primaryBytes.length, "AFMA block inter dense residual payload");
                        }
                        case COPY_SPARSE -> {
                            dx = in.readShort();
                            dy = in.readShort();
                            channels = in.readUnsignedByte();
                            changedPixelCount = in.readUnsignedShort();
                            validateChannels(channels);
                            primaryBytes = in.readNBytes(AfmaResidualPayloadHelper.expectedSparseMaskBytes(tileWidth, tileHeight));
                            validateLength(AfmaResidualPayloadHelper.expectedSparseMaskBytes(tileWidth, tileHeight), primaryBytes.length, "AFMA block inter sparse mask payload");
                            secondaryBytes = in.readNBytes(AfmaResidualPayloadHelper.expectedSparseResidualBytes(changedPixelCount, channels));
                            validateLength(AfmaResidualPayloadHelper.expectedSparseResidualBytes(changedPixelCount, channels), secondaryBytes.length, "AFMA block inter sparse residual payload");
                        }
                        case RAW -> {
                            channels = in.readUnsignedByte();
                            validateChannels(channels);
                            primaryBytes = in.readNBytes(expectedRawTileBytes(tileWidth, tileHeight, channels));
                            validateLength(expectedRawTileBytes(tileWidth, tileHeight, channels), primaryBytes.length, "AFMA block inter raw tile payload");
                        }
                    }

                    consumer.accept(localX, localY, tileWidth, tileHeight, mode, dx, dy, channels, changedPixelCount, primaryBytes, secondaryBytes);
                }
            }

            if (in.available() > 0) {
                throw new IOException("AFMA block inter payload contains trailing data");
            }
        }
    }

    public static void validatePayload(@NotNull byte[] payloadBytes, int tileSize, int regionX, int regionY, int regionWidth, int regionHeight,
                                       int canvasWidth, int canvasHeight) throws IOException {
        walkPayload(payloadBytes, tileSize, regionWidth, regionHeight, (localX, localY, tileWidth, tileHeight, mode, dx, dy, channels, changedPixelCount, primaryBytes, secondaryBytes) -> {
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
                                @Nullable byte[] primaryBytes, @Nullable byte[] secondaryBytes) {
    }

    @FunctionalInterface
    public interface TileConsumer {
        void accept(int localX, int localY, int tileWidth, int tileHeight, @NotNull TileMode mode, int dx, int dy,
                    int channels, int changedPixelCount, @Nullable byte[] primaryBytes, @Nullable byte[] secondaryBytes) throws IOException;
    }

}
