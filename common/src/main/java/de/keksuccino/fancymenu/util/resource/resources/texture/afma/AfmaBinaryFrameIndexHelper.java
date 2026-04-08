package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AfmaBinaryFrameIndexHelper {

    public static final String FRAME_INDEX_ENTRY_PATH = "frame_index.bin";
    public static final int FRAME_INDEX_MAGIC = 0x41464958; // AFIX
    public static final int FRAME_INDEX_VERSION = 3;
    protected static final int MULTI_COPY_SHARED_WIDTH_FLAG = 1;
    protected static final int MULTI_COPY_SHARED_HEIGHT_FLAG = 1 << 1;

    private AfmaBinaryFrameIndexHelper() {
    }

    @NotNull
    public static byte[] encodeFrameIndex(@NotNull AfmaFrameIndex frameIndex, @NotNull Map<String, Integer> payloadIdsByPath) throws IOException {
        Objects.requireNonNull(frameIndex);
        Objects.requireNonNull(payloadIdsByPath);
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeInt(FRAME_INDEX_MAGIC);
            out.writeByte(FRAME_INDEX_VERSION);
            writeSequence(out, frameIndex.getFrames(), payloadIdsByPath);
            writeSequence(out, frameIndex.getIntroFrames(), payloadIdsByPath);
            out.flush();
            return byteStream.toByteArray();
        }
    }

    @NotNull
    public static AfmaFrameIndex decodeFrameIndex(@NotNull byte[] frameIndexBytes) throws IOException {
        Objects.requireNonNull(frameIndexBytes);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(frameIndexBytes))) {
            int magic = in.readInt();
            if (magic != FRAME_INDEX_MAGIC) {
                throw new IOException("AFMA binary frame index is missing its magic header");
            }

            int version = in.readUnsignedByte();
            if (version != FRAME_INDEX_VERSION) {
                throw new IOException("Unsupported AFMA binary frame index version: " + version);
            }

            List<AfmaFrameDescriptor> frames = readSequence(in);
            List<AfmaFrameDescriptor> introFrames = readSequence(in);
            if (in.available() > 0) {
                throw new IOException("AFMA binary frame index contains trailing data");
            }
            return new AfmaFrameIndex(frames, introFrames);
        }
    }

    protected static void writeSequence(@NotNull DataOutputStream out, @NotNull List<AfmaFrameDescriptor> sequence,
                                        @NotNull Map<String, Integer> payloadIdsByPath) throws IOException {
        writeVarInt(out, sequence.size());
        for (AfmaFrameDescriptor descriptor : sequence) {
            writeDescriptor(out, Objects.requireNonNull(descriptor), payloadIdsByPath);
        }
    }

    protected static @NotNull List<AfmaFrameDescriptor> readSequence(@NotNull DataInputStream in) throws IOException {
        int count = readVarInt(in);
        if (count < 0) {
            throw new IOException("AFMA binary frame sequence count is invalid");
        }
        ArrayList<AfmaFrameDescriptor> sequence = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sequence.add(readDescriptor(in));
        }
        return sequence;
    }

    protected static void writeDescriptor(@NotNull DataOutputStream out, @NotNull AfmaFrameDescriptor descriptor,
                                          @NotNull Map<String, Integer> payloadIdsByPath) throws IOException {
        AfmaFrameOperationType type = Objects.requireNonNull(descriptor.getType(), "AFMA frame descriptor type was NULL");
        out.writeByte(typeId(type));
        switch (type) {
            case FULL -> writePayloadId(out, descriptor.getPrimaryPayloadPath(), payloadIdsByPath);
            case DELTA_RECT -> {
                writePayloadId(out, descriptor.getPrimaryPayloadPath(), payloadIdsByPath);
                writePatchBounds(out, descriptor);
            }
            case RESIDUAL_DELTA_RECT -> {
                writePayloadId(out, descriptor.getPrimaryPayloadPath(), payloadIdsByPath);
                writePatchBounds(out, descriptor);
                AfmaResidualPayload residualPayload = Objects.requireNonNull(descriptor.getResidual());
                writeVarInt(out, residualPayload.getChannels());
                out.writeByte(residualPayload.getCodec().getId());
                out.writeByte(residualPayload.getAlphaMode().getId());
                writeVarInt(out, residualPayload.getAlphaChangedPixelCount());
            }
            case SPARSE_DELTA_RECT -> {
                writePayloadId(out, descriptor.getPrimaryPayloadPath(), payloadIdsByPath);
                writePayloadId(out, descriptor.getSecondaryPayloadPath(), payloadIdsByPath);
                writePatchBounds(out, descriptor);
                AfmaSparsePayload sparsePayload = Objects.requireNonNull(descriptor.getSparse());
                writeVarInt(out, sparsePayload.getChangedPixelCount());
                writeVarInt(out, sparsePayload.getChannels());
                out.writeByte(sparsePayload.getLayoutCodec().getId());
                out.writeByte(sparsePayload.getResidualCodec().getId());
                out.writeByte(sparsePayload.getAlphaMode().getId());
                writeVarInt(out, sparsePayload.getAlphaChangedPixelCount());
            }
            case SAME -> {
            }
            case COPY_RECT_PATCH -> {
                writeCopyRect(out, Objects.requireNonNull(descriptor.getCopy()));
                AfmaPatchRegion patchRegion = descriptor.getPatch();
                out.writeBoolean(patchRegion != null);
                if (patchRegion != null) {
                    writePayloadId(out, patchRegion.getPath(), payloadIdsByPath);
                    writePatchRegion(out, patchRegion);
                }
            }
            case COPY_RECT_RESIDUAL_PATCH -> {
                writeCopyRect(out, Objects.requireNonNull(descriptor.getCopy()));
                writePayloadId(out, descriptor.getPrimaryPayloadPath(), payloadIdsByPath);
                writePatchBounds(out, descriptor);
                AfmaResidualPayload residualPayload = Objects.requireNonNull(descriptor.getResidual());
                writeVarInt(out, residualPayload.getChannels());
                out.writeByte(residualPayload.getCodec().getId());
                out.writeByte(residualPayload.getAlphaMode().getId());
                writeVarInt(out, residualPayload.getAlphaChangedPixelCount());
            }
            case COPY_RECT_SPARSE_PATCH -> {
                writeCopyRect(out, Objects.requireNonNull(descriptor.getCopy()));
                writePayloadId(out, descriptor.getPrimaryPayloadPath(), payloadIdsByPath);
                writePayloadId(out, descriptor.getSecondaryPayloadPath(), payloadIdsByPath);
                writePatchBounds(out, descriptor);
                AfmaSparsePayload sparsePayload = Objects.requireNonNull(descriptor.getSparse());
                writeVarInt(out, sparsePayload.getChangedPixelCount());
                writeVarInt(out, sparsePayload.getChannels());
                out.writeByte(sparsePayload.getLayoutCodec().getId());
                out.writeByte(sparsePayload.getResidualCodec().getId());
                out.writeByte(sparsePayload.getAlphaMode().getId());
                writeVarInt(out, sparsePayload.getAlphaChangedPixelCount());
            }
            case MULTI_COPY_PATCH -> {
                writeMultiCopy(out, Objects.requireNonNull(descriptor.getMultiCopy()));
                AfmaPatchRegion patchRegion = descriptor.getPatch();
                out.writeBoolean(patchRegion != null);
                if (patchRegion != null) {
                    writePayloadId(out, patchRegion.getPath(), payloadIdsByPath);
                    writePatchRegion(out, patchRegion);
                }
            }
            case MULTI_COPY_RESIDUAL_PATCH -> {
                writeMultiCopy(out, Objects.requireNonNull(descriptor.getMultiCopy()));
                writePayloadId(out, descriptor.getPrimaryPayloadPath(), payloadIdsByPath);
                writePatchBounds(out, descriptor);
                AfmaResidualPayload residualPayload = Objects.requireNonNull(descriptor.getResidual());
                writeVarInt(out, residualPayload.getChannels());
                out.writeByte(residualPayload.getCodec().getId());
                out.writeByte(residualPayload.getAlphaMode().getId());
                writeVarInt(out, residualPayload.getAlphaChangedPixelCount());
            }
            case MULTI_COPY_SPARSE_PATCH -> {
                writeMultiCopy(out, Objects.requireNonNull(descriptor.getMultiCopy()));
                writePayloadId(out, descriptor.getPrimaryPayloadPath(), payloadIdsByPath);
                writePayloadId(out, descriptor.getSecondaryPayloadPath(), payloadIdsByPath);
                writePatchBounds(out, descriptor);
                AfmaSparsePayload sparsePayload = Objects.requireNonNull(descriptor.getSparse());
                writeVarInt(out, sparsePayload.getChangedPixelCount());
                writeVarInt(out, sparsePayload.getChannels());
                out.writeByte(sparsePayload.getLayoutCodec().getId());
                out.writeByte(sparsePayload.getResidualCodec().getId());
                out.writeByte(sparsePayload.getAlphaMode().getId());
                writeVarInt(out, sparsePayload.getAlphaChangedPixelCount());
            }
            case BLOCK_INTER -> {
                writePayloadId(out, descriptor.getPrimaryPayloadPath(), payloadIdsByPath);
                writePatchBounds(out, descriptor);
                writeVarInt(out, Objects.requireNonNull(descriptor.getBlockInter()).getTileSize());
            }
        }
    }

    protected static @NotNull AfmaFrameDescriptor readDescriptor(@NotNull DataInputStream in) throws IOException {
        AfmaFrameOperationType type = typeById(in.readUnsignedByte());
        return switch (type) {
            case FULL -> AfmaFrameDescriptor.full(readPayloadPath(in));
            case DELTA_RECT -> AfmaFrameDescriptor.deltaRect(readPayloadPath(in), readVarInt(in), readVarInt(in), readVarInt(in), readVarInt(in));
            case RESIDUAL_DELTA_RECT -> AfmaFrameDescriptor.residualDeltaRect(
                    readPayloadPath(in),
                    readVarInt(in),
                    readVarInt(in),
                    readVarInt(in),
                    readVarInt(in),
                    new AfmaResidualPayload(readVarInt(in), AfmaResidualCodec.byId(in.readUnsignedByte()),
                            AfmaAlphaResidualMode.byId(in.readUnsignedByte()), readVarInt(in))
            );
            case SPARSE_DELTA_RECT -> {
                String maskPath = readPayloadPath(in);
                String pixelsPath = readPayloadPath(in);
                int x = readVarInt(in);
                int y = readVarInt(in);
                int width = readVarInt(in);
                int height = readVarInt(in);
                int pixelCount = readVarInt(in);
                int channels = readVarInt(in);
                AfmaSparseLayoutCodec layoutCodec = AfmaSparseLayoutCodec.byId(in.readUnsignedByte());
                AfmaResidualCodec residualCodec = AfmaResidualCodec.byId(in.readUnsignedByte());
                AfmaAlphaResidualMode alphaMode = AfmaAlphaResidualMode.byId(in.readUnsignedByte());
                int alphaChangedPixelCount = readVarInt(in);
                yield AfmaFrameDescriptor.sparseDeltaRect(maskPath, x, y, width, height,
                        new AfmaSparsePayload(pixelsPath, pixelCount, channels, layoutCodec, residualCodec, alphaMode, alphaChangedPixelCount));
            }
            case SAME -> AfmaFrameDescriptor.same();
            case COPY_RECT_PATCH -> {
                AfmaCopyRect copyRect = readCopyRect(in);
                boolean hasPatch = in.readBoolean();
                if (!hasPatch) {
                    yield AfmaFrameDescriptor.copyRectPatch(copyRect, null);
                }

                String patchPath = readPayloadPath(in);
                int patchX = readVarInt(in);
                int patchY = readVarInt(in);
                int patchWidth = readVarInt(in);
                int patchHeight = readVarInt(in);
                yield AfmaFrameDescriptor.copyRectPatch(copyRect, new AfmaPatchRegion(patchPath, patchX, patchY, patchWidth, patchHeight));
            }
            case COPY_RECT_RESIDUAL_PATCH -> {
                AfmaCopyRect copyRect = readCopyRect(in);
                String payloadPath = readPayloadPath(in);
                int x = readVarInt(in);
                int y = readVarInt(in);
                int width = readVarInt(in);
                int height = readVarInt(in);
                int channels = readVarInt(in);
                AfmaResidualCodec residualCodec = AfmaResidualCodec.byId(in.readUnsignedByte());
                AfmaAlphaResidualMode alphaMode = AfmaAlphaResidualMode.byId(in.readUnsignedByte());
                int alphaChangedPixelCount = readVarInt(in);
                yield AfmaFrameDescriptor.copyRectResidualPatch(copyRect, payloadPath, x, y, width, height,
                        new AfmaResidualPayload(channels, residualCodec, alphaMode, alphaChangedPixelCount));
            }
            case COPY_RECT_SPARSE_PATCH -> {
                AfmaCopyRect copyRect = readCopyRect(in);
                String maskPath = readPayloadPath(in);
                String pixelsPath = readPayloadPath(in);
                int x = readVarInt(in);
                int y = readVarInt(in);
                int width = readVarInt(in);
                int height = readVarInt(in);
                int pixelCount = readVarInt(in);
                int channels = readVarInt(in);
                AfmaSparseLayoutCodec layoutCodec = AfmaSparseLayoutCodec.byId(in.readUnsignedByte());
                AfmaResidualCodec residualCodec = AfmaResidualCodec.byId(in.readUnsignedByte());
                AfmaAlphaResidualMode alphaMode = AfmaAlphaResidualMode.byId(in.readUnsignedByte());
                int alphaChangedPixelCount = readVarInt(in);
                yield AfmaFrameDescriptor.copyRectSparsePatch(copyRect, maskPath, x, y, width, height,
                        new AfmaSparsePayload(pixelsPath, pixelCount, channels, layoutCodec, residualCodec, alphaMode, alphaChangedPixelCount));
            }
            case MULTI_COPY_PATCH -> {
                AfmaMultiCopy multiCopy = readMultiCopy(in);
                boolean hasPatch = in.readBoolean();
                if (!hasPatch) {
                    yield AfmaFrameDescriptor.multiCopyPatch(multiCopy, null);
                }

                String patchPath = readPayloadPath(in);
                int patchX = readVarInt(in);
                int patchY = readVarInt(in);
                int patchWidth = readVarInt(in);
                int patchHeight = readVarInt(in);
                yield AfmaFrameDescriptor.multiCopyPatch(multiCopy, new AfmaPatchRegion(patchPath, patchX, patchY, patchWidth, patchHeight));
            }
            case MULTI_COPY_RESIDUAL_PATCH -> {
                AfmaMultiCopy multiCopy = readMultiCopy(in);
                String payloadPath = readPayloadPath(in);
                int x = readVarInt(in);
                int y = readVarInt(in);
                int width = readVarInt(in);
                int height = readVarInt(in);
                int channels = readVarInt(in);
                AfmaResidualCodec residualCodec = AfmaResidualCodec.byId(in.readUnsignedByte());
                AfmaAlphaResidualMode alphaMode = AfmaAlphaResidualMode.byId(in.readUnsignedByte());
                int alphaChangedPixelCount = readVarInt(in);
                yield AfmaFrameDescriptor.multiCopyResidualPatch(multiCopy, payloadPath, x, y, width, height,
                        new AfmaResidualPayload(channels, residualCodec, alphaMode, alphaChangedPixelCount));
            }
            case MULTI_COPY_SPARSE_PATCH -> {
                AfmaMultiCopy multiCopy = readMultiCopy(in);
                String maskPath = readPayloadPath(in);
                String pixelsPath = readPayloadPath(in);
                int x = readVarInt(in);
                int y = readVarInt(in);
                int width = readVarInt(in);
                int height = readVarInt(in);
                int pixelCount = readVarInt(in);
                int channels = readVarInt(in);
                AfmaSparseLayoutCodec layoutCodec = AfmaSparseLayoutCodec.byId(in.readUnsignedByte());
                AfmaResidualCodec residualCodec = AfmaResidualCodec.byId(in.readUnsignedByte());
                AfmaAlphaResidualMode alphaMode = AfmaAlphaResidualMode.byId(in.readUnsignedByte());
                int alphaChangedPixelCount = readVarInt(in);
                yield AfmaFrameDescriptor.multiCopySparsePatch(multiCopy, maskPath, x, y, width, height,
                        new AfmaSparsePayload(pixelsPath, pixelCount, channels, layoutCodec, residualCodec, alphaMode, alphaChangedPixelCount));
            }
            case BLOCK_INTER -> AfmaFrameDescriptor.blockInter(
                    readPayloadPath(in),
                    readVarInt(in),
                    readVarInt(in),
                    readVarInt(in),
                    readVarInt(in),
                    new AfmaBlockInter(readVarInt(in))
            );
        };
    }

    protected static void writeCopyRect(@NotNull DataOutputStream out, @NotNull AfmaCopyRect copyRect) throws IOException {
        writeVarInt(out, copyRect.getSrcX());
        writeVarInt(out, copyRect.getSrcY());
        writeVarInt(out, copyRect.getDstX());
        writeVarInt(out, copyRect.getDstY());
        writeVarInt(out, copyRect.getWidth());
        writeVarInt(out, copyRect.getHeight());
    }

    protected static @NotNull AfmaCopyRect readCopyRect(@NotNull DataInputStream in) throws IOException {
        return new AfmaCopyRect(readVarInt(in), readVarInt(in), readVarInt(in), readVarInt(in), readVarInt(in), readVarInt(in));
    }

    protected static void writeMultiCopy(@NotNull DataOutputStream out, @NotNull AfmaMultiCopy multiCopy) throws IOException {
        List<AfmaCopyRect> copyRects = multiCopy.getCopyRects();
        if (copyRects.size() < 2) {
            throw new IOException("AFMA multi-copy frames require at least 2 copy rectangles");
        }

        writeVarInt(out, copyRects.size() - 1);
        AfmaCopyRect previousRect = null;
        for (AfmaCopyRect copyRect : copyRects) {
            AfmaCopyRect currentRect = Objects.requireNonNull(copyRect, "AFMA multi-copy rectangle was NULL");
            int flags = 0;
            if ((previousRect != null) && (currentRect.getWidth() == previousRect.getWidth())) {
                flags |= MULTI_COPY_SHARED_WIDTH_FLAG;
            }
            if ((previousRect != null) && (currentRect.getHeight() == previousRect.getHeight())) {
                flags |= MULTI_COPY_SHARED_HEIGHT_FLAG;
            }
            out.writeByte(flags);

            if (previousRect == null) {
                writeCopyRect(out, currentRect);
            } else {
                writeSignedVarInt(out, currentRect.getSrcX() - previousRect.getSrcX());
                writeSignedVarInt(out, currentRect.getSrcY() - previousRect.getSrcY());
                writeSignedVarInt(out, currentRect.getDstX() - previousRect.getDstX());
                writeSignedVarInt(out, currentRect.getDstY() - previousRect.getDstY());
                if ((flags & MULTI_COPY_SHARED_WIDTH_FLAG) == 0) {
                    writeVarInt(out, currentRect.getWidth());
                }
                if ((flags & MULTI_COPY_SHARED_HEIGHT_FLAG) == 0) {
                    writeVarInt(out, currentRect.getHeight());
                }
            }
            previousRect = currentRect;
        }
    }

    protected static @NotNull AfmaMultiCopy readMultiCopy(@NotNull DataInputStream in) throws IOException {
        int rectCount = readVarInt(in) + 1;
        if (rectCount < 2) {
            throw new IOException("AFMA multi-copy frame count is invalid");
        }

        ArrayList<AfmaCopyRect> copyRects = new ArrayList<>(rectCount);
        AfmaCopyRect previousRect = null;
        for (int rectIndex = 0; rectIndex < rectCount; rectIndex++) {
            int flags = in.readUnsignedByte();
            AfmaCopyRect currentRect;
            if (previousRect == null) {
                currentRect = readCopyRect(in);
            } else {
                int srcX = previousRect.getSrcX() + readSignedVarInt(in);
                int srcY = previousRect.getSrcY() + readSignedVarInt(in);
                int dstX = previousRect.getDstX() + readSignedVarInt(in);
                int dstY = previousRect.getDstY() + readSignedVarInt(in);
                int width = ((flags & MULTI_COPY_SHARED_WIDTH_FLAG) != 0) ? previousRect.getWidth() : readVarInt(in);
                int height = ((flags & MULTI_COPY_SHARED_HEIGHT_FLAG) != 0) ? previousRect.getHeight() : readVarInt(in);
                currentRect = new AfmaCopyRect(srcX, srcY, dstX, dstY, width, height);
            }
            copyRects.add(currentRect);
            previousRect = currentRect;
        }
        return new AfmaMultiCopy(copyRects);
    }

    protected static void writePatchBounds(@NotNull DataOutputStream out, @NotNull AfmaFrameDescriptor descriptor) throws IOException {
        writeVarInt(out, descriptor.getX());
        writeVarInt(out, descriptor.getY());
        writeVarInt(out, descriptor.getWidth());
        writeVarInt(out, descriptor.getHeight());
    }

    protected static void writePatchRegion(@NotNull DataOutputStream out, @NotNull AfmaPatchRegion patchRegion) throws IOException {
        writeVarInt(out, patchRegion.getX());
        writeVarInt(out, patchRegion.getY());
        writeVarInt(out, patchRegion.getWidth());
        writeVarInt(out, patchRegion.getHeight());
    }

    protected static void writePayloadId(@NotNull DataOutputStream out, @Nullable String payloadPath,
                                         @NotNull Map<String, Integer> payloadIdsByPath) throws IOException {
        if ((payloadPath == null) || payloadPath.isBlank()) {
            throw new IOException("AFMA frame descriptor is missing a referenced payload path");
        }
        Integer payloadId = payloadIdsByPath.get(payloadPath);
        if (payloadId == null) {
            throw new IOException("AFMA frame descriptor references an unknown payload path: " + payloadPath);
        }
        writeVarInt(out, payloadId);
    }

    protected static @NotNull String readPayloadPath(@NotNull DataInputStream in) throws IOException {
        return AfmaChunkedPayloadHelper.syntheticPayloadPath(readVarInt(in));
    }

    protected static int typeId(@NotNull AfmaFrameOperationType type) {
        return switch (type) {
            case FULL -> 0;
            case DELTA_RECT -> 1;
            case RESIDUAL_DELTA_RECT -> 2;
            case SPARSE_DELTA_RECT -> 3;
            case SAME -> 4;
            case COPY_RECT_PATCH -> 5;
            case COPY_RECT_RESIDUAL_PATCH -> 6;
            case COPY_RECT_SPARSE_PATCH -> 7;
            case BLOCK_INTER -> 8;
            case MULTI_COPY_PATCH -> 9;
            case MULTI_COPY_RESIDUAL_PATCH -> 10;
            case MULTI_COPY_SPARSE_PATCH -> 11;
        };
    }

    protected static @NotNull AfmaFrameOperationType typeById(int typeId) throws IOException {
        return switch (typeId) {
            case 0 -> AfmaFrameOperationType.FULL;
            case 1 -> AfmaFrameOperationType.DELTA_RECT;
            case 2 -> AfmaFrameOperationType.RESIDUAL_DELTA_RECT;
            case 3 -> AfmaFrameOperationType.SPARSE_DELTA_RECT;
            case 4 -> AfmaFrameOperationType.SAME;
            case 5 -> AfmaFrameOperationType.COPY_RECT_PATCH;
            case 6 -> AfmaFrameOperationType.COPY_RECT_RESIDUAL_PATCH;
            case 7 -> AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH;
            case 8 -> AfmaFrameOperationType.BLOCK_INTER;
            case 9 -> AfmaFrameOperationType.MULTI_COPY_PATCH;
            case 10 -> AfmaFrameOperationType.MULTI_COPY_RESIDUAL_PATCH;
            case 11 -> AfmaFrameOperationType.MULTI_COPY_SPARSE_PATCH;
            default -> throw new IOException("AFMA binary frame index contains an unknown frame opcode: " + typeId);
        };
    }

    protected static void writeSignedVarInt(@NotNull DataOutputStream out, int value) throws IOException {
        writeVarInt(out, (value << 1) ^ (value >> 31));
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
        throw new IOException("AFMA binary frame index varint is too large");
    }

    protected static int readSignedVarInt(@NotNull DataInputStream in) throws IOException {
        int zigZag = readVarInt(in);
        return (zigZag >>> 1) ^ -(zigZag & 1);
    }

}
