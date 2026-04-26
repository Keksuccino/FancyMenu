package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class AfmaBinIntraPayloadHelper {

    public static final int PAYLOAD_MAGIC = 0x4146424E; // AFBN
    public static final int PAYLOAD_VERSION = 2;
    public static final int MAX_PALETTE_COLORS = 256;
    public static final int RGB_CHANNELS = 3;
    public static final int RGBA_CHANNELS = 4;
    protected static final int INDEXED_PRIMARY_MAX_PALETTE_COLORS = 96;
    protected static final int INDEXED_STABLE_MAX_PALETTE_COLORS = 24;
    protected static final int INDEXED_COLOR_ALPHA_PRIMARY_MAX_PALETTE_COLORS = 128;
    protected static final int INDEXED_COLOR_ALPHA_STABLE_MAX_PALETTE_COLORS = 32;
    protected static final int SECONDARY_TRUECOLOR_MODE_MAX_PIXELS = 16384;
    protected static final int MAX_EXHAUSTIVE_MODE_SELECTION_PIXELS = 16384;
    protected static final int MODE_ESTIMATE_SAMPLE_ROWS = 8;
    protected static final int MAX_SHORTLISTED_MODE_CANDIDATES = 4;
    protected static final int MAX_SHORTLISTED_MODE_CANDIDATES_PER_FAMILY = 2;
    protected static final int MAX_SHORTLISTED_PIXEL_CANDIDATES = 4;
    protected static final int MIN_PIXEL_CANDIDATE_SHORTLIST_PIXELS = 65536;
    protected static final int MIN_PARALLEL_PERCEPTUAL_CANDIDATE_PIXELS = 131072;
    protected static final int MAX_SELECTED_PERCEPTUAL_PROFILES = 5;
    protected static final int PERCEPTUAL_PROFILE_SAMPLE_PIXELS = 8192;
    protected static final int PERCEPTUAL_PALETTE_REFINEMENT_MIN_PALETTE_COLORS = 32;
    protected static final int PERCEPTUAL_PALETTE_REFINEMENT_MIN_EXTRA_BUCKETS = 8;
    protected static final int PERCEPTUAL_PALETTE_REFINEMENT_MAX_ITERATIONS = 3;
    protected static final ChannelQuantizationProfile[] HIGH_COLOR_CHANNEL_QUANTIZATION_PROFILES = {
            ChannelQuantizationProfile.RGB555,
            ChannelQuantizationProfile.RGB454,
            ChannelQuantizationProfile.RGB444
    };
    protected static final int MIN_PARALLEL_MODE_SELECTION_PIXELS = 65536;
    protected static final long MIN_PARALLEL_MODE_SELECTION_HEADROOM_BYTES = 192L * 1024L * 1024L;

    private AfmaBinIntraPayloadHelper() {
    }

    @NotNull
    public static byte[] encodePayload(int width, int height, @NotNull int[] pixels) throws IOException {
        return encodePayload(width, height, pixels, 0, width);
    }

    @NotNull
    public static byte[] encodePayload(int width, int height, @NotNull int[] pixels, int offset, int scanlineStride) throws IOException {
        return encodePayloadDetailed(width, height, pixels, offset, scanlineStride).payloadBytes();
    }

    @NotNull
    public static EncodedPayloadResult encodePayloadDetailed(int width, int height, @NotNull int[] pixels, int offset, int scanlineStride) throws IOException {
        return encodePayloadDetailed(width, height, pixels, offset, scanlineStride, EncodePreferences.lossless());
    }

    @NotNull
    public static EncodedPayloadResult encodePayloadDetailed(int width, int height, @NotNull int[] pixels, int offset,
                                                             int scanlineStride, @Nullable EncodePreferences preferences) throws IOException {
        ScoredPayloadResult scoredResult = scorePayloadDetailed(width, height, pixels, offset, scanlineStride, preferences);
        return new EncodedPayloadResult(
                scoredResult.payloadBytes(),
                scoredResult.reconstructedPixels(),
                scoredResult.lossless(),
                scoredResult.mode()
        );
    }

    @NotNull
    public static StoredEncodedPayloadResult encodePayloadStoredDetailed(int width, int height, @NotNull int[] pixels, int offset, int scanlineStride)
            throws IOException {
        return encodePayloadStoredDetailed(width, height, pixels, offset, scanlineStride, EncodePreferences.lossless());
    }

    @NotNull
    public static StoredEncodedPayloadResult encodePayloadStoredDetailed(int width, int height, @NotNull int[] pixels, int offset,
                                                                         int scanlineStride, @Nullable EncodePreferences preferences) throws IOException {
        ScoredPayloadResult scoredResult = scorePayloadDetailed(width, height, pixels, offset, scanlineStride, preferences);
        AfmaStoredPayload payload = scoredResult.materializePayload();
        if ((payload.length() != scoredResult.payloadSummary().length())
                || (payload.estimatedArchiveBytes() != scoredResult.payloadSummary().estimatedArchiveBytes())
                || !payload.fingerprint().equals(scoredResult.payloadSummary().fingerprint())) {
            payload.close();
            throw new IOException("AFMA BIN_INTRA payload metrics changed between scoring and materialization");
        }
        return new StoredEncodedPayloadResult(payload, scoredResult.reconstructedPixels(), scoredResult.lossless(), scoredResult.mode());
    }

    @NotNull
    public static ScoredPayloadResult scorePayloadDetailed(int width, int height, @NotNull int[] pixels, int offset,
                                                           int scanlineStride, @Nullable EncodePreferences preferences) throws IOException {
        validateDimensions(width, height);
        int[] sourcePixels = materializeDensePixels(width, height, pixels, offset, scanlineStride);

        EncodePreferences normalizedPreferences = (preferences != null) ? preferences : EncodePreferences.lossless();
        List<PixelCandidateContext> pixelCandidates = shortlistPixelCandidates(
                width,
                height,
                collectPixelCandidates(width, height, sourcePixels, normalizedPreferences)
        );
        ScoredPayloadCandidate bestCandidate = shouldParallelizeModeSelection(width, height, pixelCandidates.size())
                ? selectBestCandidateInParallel(width, height, pixelCandidates)
                : selectBestCandidateSequential(width, height, pixelCandidates);

        if (bestCandidate == null) {
            throw new IOException("Failed to build an AFMA BIN_INTRA payload");
        }
        return new ScoredPayloadResult(
                bestCandidate.payloadSummary(),
                bestCandidate.payloadBytes(),
                bestCandidate.reconstructedPixels(),
                bestCandidate.lossless(),
                bestCandidate.mode()
        );
    }

    @NotNull
    public static EstimatedPayloadResult estimatePayloadDetailed(int width, int height, @NotNull int[] pixels, int offset,
                                                                 int scanlineStride, @Nullable EncodePreferences preferences) throws IOException {
        validateDimensions(width, height);
        int[] sourcePixels = materializeDensePixels(width, height, pixels, offset, scanlineStride);

        EncodePreferences normalizedPreferences = (preferences != null) ? preferences : EncodePreferences.lossless();
        List<PixelCandidateContext> pixelCandidates = collectPixelCandidates(width, height, sourcePixels, normalizedPreferences);
        EstimatedPayloadCandidate bestCandidate = null;
        for (PixelCandidateContext pixelCandidate : pixelCandidates) {
            bestCandidate = pickBetterEstimatedCandidate(bestCandidate, estimateBestCandidate(width, height, pixelCandidate));
        }
        if (bestCandidate == null) {
            throw new IOException("Failed to estimate an AFMA BIN_INTRA payload");
        }
        return new EstimatedPayloadResult(
                bestCandidate.estimatedArchiveBytes(),
                bestCandidate.payloadLength(),
                bestCandidate.lossless(),
                bestCandidate.mode()
        );
    }

    @NotNull
    protected static int[] materializeDensePixels(int width, int height, @NotNull int[] pixels, int offset, int scanlineStride) {
        validatePixelBufferView(width, height, pixels, offset, scanlineStride);
        if ((offset == 0) && (scanlineStride == width) && (pixels.length == (width * height))) {
            return pixels;
        }

        int[] densePixels = new int[width * height];
        int sourceOffset = offset;
        int denseOffset = 0;
        for (int row = 0; row < height; row++) {
            System.arraycopy(pixels, sourceOffset, densePixels, denseOffset, width);
            sourceOffset += scanlineStride;
            denseOffset += width;
        }
        return densePixels;
    }

    @NotNull
    public static DecodedFrame decodePayload(@NotNull byte[] payloadBytes, int offset, int length) throws IOException {
        return decodePayload(payloadBytes, offset, length, null);
    }

    @NotNull
    public static DecodedFrame decodePayload(@NotNull byte[] payloadBytes, int offset, int length,
                                             @Nullable AfmaDecodeScratch scratch) throws IOException {
        Objects.requireNonNull(payloadBytes);
        if (offset < 0 || length < 0 || ((long) offset + (long) length) > payloadBytes.length) {
            throw new IOException("AFMA BIN_INTRA payload slice is invalid");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payloadBytes, offset, length))) {
            PayloadHeader header = readHeader(in);
            int[] pixels = new int[header.width() * header.height()];
            decodePayloadBodyIntoArgbBuffer(in, header, pixels, 0, header.width(), scratch);
            if (in.available() > 0) {
                throw new IOException("AFMA BIN_INTRA payload contains trailing data");
            }
            return new DecodedFrame(header.width(), header.height(), pixels);
        }
    }

    @NotNull
    public static PayloadHeader decodePayloadIntoArgbBuffer(@NotNull byte[] payloadBytes, int offset, int length,
                                                            @NotNull int[] targetPixels, int targetOffset, int scanlineStride,
                                                            @Nullable AfmaDecodeScratch scratch) throws IOException {
        Objects.requireNonNull(payloadBytes);
        Objects.requireNonNull(targetPixels);
        if (offset < 0 || length < 0 || ((long) offset + (long) length) > payloadBytes.length) {
            throw new IOException("AFMA BIN_INTRA payload slice is invalid");
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payloadBytes, offset, length))) {
            PayloadHeader header = readHeader(in);
            validatePixelBufferView(header.width(), header.height(), targetPixels, targetOffset, scanlineStride);
            decodePayloadBodyIntoArgbBuffer(in, header, targetPixels, targetOffset, scanlineStride, scratch);
            if (in.available() > 0) {
                throw new IOException("AFMA BIN_INTRA payload contains trailing data");
            }
            return header;
        }
    }

    public static void validatePayload(@NotNull byte[] payloadBytes, int offset, int length, int expectedWidth, int expectedHeight) throws IOException {
        DecodedFrame frame = decodePayload(payloadBytes, offset, length);
        if ((frame.width() != expectedWidth) || (frame.height() != expectedHeight)) {
            throw new IOException("AFMA BIN_INTRA payload dimensions do not match the descriptor");
        }
    }

    @NotNull
    protected static PayloadHeader readHeader(@NotNull DataInputStream in) throws IOException {
        int magic = in.readInt();
        if (magic != PAYLOAD_MAGIC) {
            throw new IOException("AFMA BIN_INTRA payload is missing its magic header");
        }

        int version = in.readUnsignedByte();
        if (version != PAYLOAD_VERSION) {
            throw new IOException("Unsupported AFMA BIN_INTRA payload version: " + version);
        }

        Mode mode = Mode.byId(in.readUnsignedByte());
        int width = in.readUnsignedShort();
        int height = in.readUnsignedShort();
        validateDecodedDimensions(width, height);
        return new PayloadHeader(width, height, mode);
    }

    protected static void decodePayloadBodyIntoArgbBuffer(@NotNull DataInputStream in, @NotNull PayloadHeader header,
                                                          @NotNull int[] targetPixels, int targetOffset, int scanlineStride,
                                                          @Nullable AfmaDecodeScratch scratch) throws IOException {
        switch (header.mode()) {
            case SOLID -> decodeSolidIntoArgbBuffer(in, header.width(), header.height(), targetPixels, targetOffset, scanlineStride);
            case INDEXED -> decodeIndexedIntoArgbBuffer(in, header.width(), header.height(), targetPixels, targetOffset, scanlineStride, scratch);
            case RGB_FILTERED -> decodeFilteredTruecolorIntoArgbBuffer(in, header.width(), header.height(), RGB_CHANNELS, targetPixels, targetOffset, scanlineStride, scratch);
            case RGBA_FILTERED -> decodeFilteredTruecolorIntoArgbBuffer(in, header.width(), header.height(), RGBA_CHANNELS, targetPixels, targetOffset, scanlineStride, scratch);
            case RGB_PLANAR_FILTERED -> decodePlanarTruecolorIntoArgbBuffer(in, header.width(), header.height(), RGB_CHANNELS, targetPixels, targetOffset, scanlineStride, scratch);
            case RGBA_PLANAR_FILTERED -> decodePlanarTruecolorIntoArgbBuffer(in, header.width(), header.height(), RGBA_CHANNELS, targetPixels, targetOffset, scanlineStride, scratch);
            case RGB_PLUS_ALPHA_SPLIT -> decodeSplitAlphaIntoArgbBuffer(in, header.width(), header.height(), targetPixels, targetOffset, scanlineStride, scratch);
            case INDEXED_COLOR_PLUS_ALPHA -> decodeIndexedColorPlusAlphaIntoArgbBuffer(in, header.width(), header.height(), targetPixels, targetOffset, scanlineStride, scratch);
            case COLOR_PLUS_ALPHA_MASK -> decodeColorPlusAlphaMaskIntoArgbBuffer(in, header.width(), header.height(), targetPixels, targetOffset, scanlineStride, scratch);
        }
    }

    @Nullable
    protected static ScoredPayloadCandidate buildSolidCandidate(int width, int height, @NotNull PixelCandidateContext pixelCandidate) throws IOException {
        if (!pixelCandidate.isSolidColor()) {
            return null;
        }
        int firstColor = pixelCandidate.firstColor();
        return scoreCandidate(Mode.SOLID, pixelCandidate.pixels(), pixelCandidate.lossless(), 1, out -> {
            writeHeader(out, Mode.SOLID, width, height);
            out.writeInt(firstColor);
        });
    }

    @Nullable
    protected static ScoredPayloadCandidate buildIndexedCandidate(int width, int height, @NotNull PixelCandidateContext pixelCandidate) throws IOException {
        return buildIndexedCandidate(width, height, pixelCandidate, PaletteOrdering.FREQUENCY);
    }

    @Nullable
    protected static ScoredPayloadCandidate buildIndexedCandidate(int width, int height, @NotNull PixelCandidateContext pixelCandidate,
                                                                  @NotNull PaletteOrdering paletteOrdering) throws IOException {
        Map<Integer, Integer> colorCounts = pixelCandidate.fullColorCounts();
        if (colorCounts == null) {
            return null;
        }
        boolean paletteHasAlpha = pixelCandidate.hasAlpha();
        IndexedModeContext indexedModeContext = prepareIndexedModeContext(width, colorCounts, paletteOrdering, false);
        return buildIndexedCandidate(width, height, pixelCandidate, indexedModeContext, paletteHasAlpha,
                paletteOrdering == PaletteOrdering.STABLE_COLOR ? 0 : 1);
    }

    @NotNull
    protected static ScoredPayloadCandidate buildIndexedCandidate(int width, int height, @NotNull PixelCandidateContext pixelCandidate,
                                                                  @NotNull IndexedModeContext indexedModeContext,
                                                                  boolean paletteHasAlpha, int stabilityPriority) throws IOException {
        int[] pixels = pixelCandidate.pixels();
        int[] palette = indexedModeContext.palette();
        return scoreCandidate(Mode.INDEXED, pixelCandidate.pixels(), pixelCandidate.lossless(), stabilityPriority, out -> {
            writeHeader(out, Mode.INDEXED, width, height);
            out.writeByte(paletteHasAlpha ? 1 : 0);
            out.writeByte(indexedModeContext.bitsPerIndex());
            out.writeShort(palette.length);
            for (int color : palette) {
                out.writeByte((color >> 16) & 0xFF);
                out.writeByte((color >> 8) & 0xFF);
                out.writeByte(color & 0xFF);
                if (paletteHasAlpha) {
                    out.writeByte((color >>> 24) & 0xFF);
                }
            }
            writeFilteredRows(out, indexedModeContext.packedRowBytes(), height, 1, (rowIndex, rowBuffer) -> fillPaletteIndexRow(
                    pixels,
                    width,
                    rowIndex,
                    indexedModeContext.bitsPerIndex(),
                    indexedModeContext.colorToIndex(),
                    false,
                    rowBuffer
            ));
        });
    }

    @NotNull
    protected static ScoredPayloadCandidate buildFilteredCandidate(int width, int height, @NotNull PixelCandidateContext pixelCandidate,
                                                                   @NotNull Mode mode) throws IOException {
        int channels = switch (mode) {
            case RGB_FILTERED -> RGB_CHANNELS;
            case RGBA_FILTERED -> RGBA_CHANNELS;
            default -> throw new IllegalArgumentException("Unsupported AFMA BIN_INTRA filtered mode: " + mode);
        };
        int[] pixels = pixelCandidate.pixels();
        return scoreCandidate(mode, pixelCandidate.pixels(), pixelCandidate.lossless(), 1, out -> {
            writeHeader(out, mode, width, height);
            writeFilteredRows(out, width * channels, height, channels, (rowIndex, rowBuffer) ->
                    fillInterleavedColorRow(pixels, width, rowIndex, channels, rowBuffer));
        });
    }

    @NotNull
    protected static ScoredPayloadCandidate buildPlanarFilteredCandidate(int width, int height, @NotNull PixelCandidateContext pixelCandidate,
                                                                         @NotNull Mode mode) throws IOException {
        int channels = switch (mode) {
            case RGB_PLANAR_FILTERED -> RGB_CHANNELS;
            case RGBA_PLANAR_FILTERED -> RGBA_CHANNELS;
            default -> throw new IllegalArgumentException("Unsupported AFMA BIN_INTRA planar mode: " + mode);
        };
        int[] pixels = pixelCandidate.pixels();
        return scoreCandidate(mode, pixelCandidate.pixels(), pixelCandidate.lossless(), 1, out -> {
            writeHeader(out, mode, width, height);
            writePlanarFiltered(out, pixels, width, height, channels);
        });
    }

    @NotNull
    protected static ScoredPayloadCandidate buildSplitAlphaCandidate(int width, int height, @NotNull PixelCandidateContext pixelCandidate) throws IOException {
        int[] pixels = pixelCandidate.pixels();
        return scoreCandidate(Mode.RGB_PLUS_ALPHA_SPLIT, pixelCandidate.pixels(), pixelCandidate.lossless(), 1, out -> {
            writeHeader(out, Mode.RGB_PLUS_ALPHA_SPLIT, width, height);
            writeFilteredRows(out, width * RGB_CHANNELS, height, RGB_CHANNELS, (rowIndex, rowBuffer) ->
                    fillInterleavedColorRow(pixels, width, rowIndex, RGB_CHANNELS, rowBuffer));
            writeFilteredRows(out, width, height, 1, (rowIndex, rowBuffer) ->
                    fillFullAlphaRow(pixels, width, rowIndex, rowBuffer));
        });
    }

    @Nullable
    protected static ScoredPayloadCandidate buildIndexedColorPlusAlphaCandidate(int width, int height, @NotNull PixelCandidateContext pixelCandidate,
                                                                                @NotNull PaletteOrdering paletteOrdering) throws IOException {
        Map<Integer, Integer> rgbCounts = pixelCandidate.rgbColorCounts();
        if (rgbCounts == null) {
            return null;
        }
        IndexedModeContext indexedModeContext = prepareIndexedModeContext(width, rgbCounts, paletteOrdering, true);
        return buildIndexedColorPlusAlphaCandidate(width, height, pixelCandidate, indexedModeContext,
                paletteOrdering == PaletteOrdering.STABLE_COLOR ? 0 : 1);
    }

    @NotNull
    protected static ScoredPayloadCandidate buildIndexedColorPlusAlphaCandidate(int width, int height, @NotNull PixelCandidateContext pixelCandidate,
                                                                                @NotNull IndexedModeContext indexedModeContext,
                                                                                int stabilityPriority) throws IOException {
        int[] pixels = pixelCandidate.pixels();
        int[] palette = indexedModeContext.palette();
        int alphaMode;
        if (pixelCandidate.binaryAlpha()) {
            alphaMode = AlphaMaskMode.BINARY.id();
        } else {
            alphaMode = AlphaMaskMode.FULL.id();
        }

        return scoreCandidate(Mode.INDEXED_COLOR_PLUS_ALPHA, pixelCandidate.pixels(), pixelCandidate.lossless(),
                stabilityPriority, out -> {
            writeHeader(out, Mode.INDEXED_COLOR_PLUS_ALPHA, width, height);
            out.writeByte(alphaMode);
            out.writeByte(indexedModeContext.bitsPerIndex());
            out.writeShort(palette.length);
            for (int rgb : palette) {
                out.writeByte((rgb >> 16) & 0xFF);
                out.writeByte((rgb >> 8) & 0xFF);
                out.writeByte(rgb & 0xFF);
            }
            writeFilteredRows(out, indexedModeContext.packedRowBytes(), height, 1, (rowIndex, rowBuffer) -> fillPaletteIndexRow(
                    pixels,
                    width,
                    rowIndex,
                    indexedModeContext.bitsPerIndex(),
                    indexedModeContext.colorToIndex(),
                    true,
                    rowBuffer
            ));
            if (pixelCandidate.binaryAlpha()) {
                int binaryRowBytes = packedRowBytes(width, 1);
                writeFilteredRows(out, binaryRowBytes, height, 1, (rowIndex, rowBuffer) ->
                        fillBinaryAlphaRow(pixels, width, rowIndex, rowBuffer));
            } else {
                writeFilteredRows(out, width, height, 1, (rowIndex, rowBuffer) ->
                        fillFullAlphaRow(pixels, width, rowIndex, rowBuffer));
            }
        });
    }

    @Nullable
    protected static ScoredPayloadCandidate buildColorPlusAlphaMaskCandidate(int width, int height,
                                                                             @NotNull PixelCandidateContext pixelCandidate) throws IOException {
        if (!pixelCandidate.hasUniformRgb() || !pixelCandidate.alphaVaries()) {
            return null;
        }
        int baseRgb = pixelCandidate.uniformRgbColor();
        int red = (baseRgb >> 16) & 0xFF;
        int green = (baseRgb >> 8) & 0xFF;
        int blue = baseRgb & 0xFF;

        int alphaMode;
        if (pixelCandidate.binaryAlpha()) {
            alphaMode = AlphaMaskMode.BINARY.id();
        } else {
            alphaMode = AlphaMaskMode.FULL.id();
        }

        int[] pixels = pixelCandidate.pixels();
        return scoreCandidate(Mode.COLOR_PLUS_ALPHA_MASK, pixelCandidate.pixels(), pixelCandidate.lossless(), 1, out -> {
            writeHeader(out, Mode.COLOR_PLUS_ALPHA_MASK, width, height);
            out.writeByte(red);
            out.writeByte(green);
            out.writeByte(blue);
            out.writeByte(alphaMode);
            if (pixelCandidate.binaryAlpha()) {
                int binaryRowBytes = packedRowBytes(width, 1);
                writeFilteredRows(out, binaryRowBytes, height, 1, (rowIndex, rowBuffer) ->
                        fillBinaryAlphaRow(pixels, width, rowIndex, rowBuffer));
            } else {
                writeFilteredRows(out, width, height, 1, (rowIndex, rowBuffer) ->
                        fillFullAlphaRow(pixels, width, rowIndex, rowBuffer));
            }
        });
    }

    protected static void decodeSolidIntoArgbBuffer(@NotNull DataInputStream in, int width, int height,
                                                    @NotNull int[] targetPixels, int targetOffset, int scanlineStride) throws IOException {
        int color = in.readInt();
        for (int row = 0; row < height; row++) {
            int rowOffset = targetOffset + (row * scanlineStride);
            Arrays.fill(targetPixels, rowOffset, rowOffset + width, color);
        }
    }

    protected static void decodeIndexedIntoArgbBuffer(@NotNull DataInputStream in, int width, int height,
                                                      @NotNull int[] targetPixels, int targetOffset, int scanlineStride,
                                                      @Nullable AfmaDecodeScratch scratch) throws IOException {
        boolean paletteHasAlpha = (in.readUnsignedByte() & 1) != 0;
        int bitsPerIndex = in.readUnsignedByte();
        validatePaletteBits(bitsPerIndex);

        int paletteSize = in.readUnsignedShort();
        if ((paletteSize <= 0) || (paletteSize > MAX_PALETTE_COLORS)) {
            throw new IOException("AFMA BIN_INTRA indexed payload palette size is invalid");
        }

        int[] palette = new int[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            int red = in.readUnsignedByte();
            int green = in.readUnsignedByte();
            int blue = in.readUnsignedByte();
            int alpha = paletteHasAlpha ? in.readUnsignedByte() : 0xFF;
            palette[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        int rowBytes = packedRowBytes(width, bitsPerIndex);
        decodeFilteredRows(in, rowBytes, height, 1, scratch, (rowIndex, packedRow, ignoredRowBytes) -> {
            int pixelOffset = targetOffset + (rowIndex * scanlineStride);
            for (int x = 0; x < width; x++) {
                int paletteIndex = unpackPackedValue(packedRow, 0, x, bitsPerIndex);
                if ((paletteIndex < 0) || (paletteIndex >= palette.length)) {
                    throw new IOException("AFMA BIN_INTRA indexed payload references an invalid palette index");
                }
                targetPixels[pixelOffset + x] = palette[paletteIndex];
            }
        });
    }

    protected static void decodeFilteredTruecolorIntoArgbBuffer(@NotNull DataInputStream in, int width, int height, int channels,
                                                                @NotNull int[] targetPixels, int targetOffset, int scanlineStride,
                                                                @Nullable AfmaDecodeScratch scratch) throws IOException {
        int rowBytes = width * channels;
        decodeFilteredRows(in, rowBytes, height, channels, scratch, (rowIndex, decodedRow, ignoredRowBytes) -> {
            int byteOffset = 0;
            int pixelOffset = targetOffset + (rowIndex * scanlineStride);
            for (int x = 0; x < width; x++) {
                int red = decodedRow[byteOffset++] & 0xFF;
                int green = decodedRow[byteOffset++] & 0xFF;
                int blue = decodedRow[byteOffset++] & 0xFF;
                int alpha = (channels == RGBA_CHANNELS) ? (decodedRow[byteOffset++] & 0xFF) : 0xFF;
                targetPixels[pixelOffset + x] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
        });
    }

    protected static void decodePlanarTruecolorIntoArgbBuffer(@NotNull DataInputStream in, int width, int height, int channels,
                                                              @NotNull int[] targetPixels, int targetOffset, int scanlineStride,
                                                              @Nullable AfmaDecodeScratch scratch) throws IOException {
        decodeFilteredRows(in, width, height, 1, scratch, (rowIndex, decodedRow, ignoredRowBytes) -> {
            int pixelOffset = targetOffset + (rowIndex * scanlineStride);
            for (int x = 0; x < width; x++) {
                targetPixels[pixelOffset + x] = 0xFF000000 | ((decodedRow[x] & 0xFF) << 16);
            }
        });
        decodeFilteredRows(in, width, height, 1, scratch, (rowIndex, decodedRow, ignoredRowBytes) -> {
            int pixelOffset = targetOffset + (rowIndex * scanlineStride);
            for (int x = 0; x < width; x++) {
                targetPixels[pixelOffset + x] = (targetPixels[pixelOffset + x] & 0xFFFF00FF) | ((decodedRow[x] & 0xFF) << 8);
            }
        });
        decodeFilteredRows(in, width, height, 1, scratch, (rowIndex, decodedRow, ignoredRowBytes) -> {
            int pixelOffset = targetOffset + (rowIndex * scanlineStride);
            for (int x = 0; x < width; x++) {
                targetPixels[pixelOffset + x] = (targetPixels[pixelOffset + x] & 0xFFFFFF00) | (decodedRow[x] & 0xFF);
            }
        });
        if (channels == RGBA_CHANNELS) {
            decodeFilteredRows(in, width, height, 1, scratch, (rowIndex, decodedRow, ignoredRowBytes) -> {
                int pixelOffset = targetOffset + (rowIndex * scanlineStride);
                for (int x = 0; x < width; x++) {
                    targetPixels[pixelOffset + x] = (targetPixels[pixelOffset + x] & 0x00FFFFFF) | ((decodedRow[x] & 0xFF) << 24);
                }
            });
        }
    }

    protected static void decodeSplitAlphaIntoArgbBuffer(@NotNull DataInputStream in, int width, int height,
                                                         @NotNull int[] targetPixels, int targetOffset, int scanlineStride,
                                                         @Nullable AfmaDecodeScratch scratch) throws IOException {
        decodeFilteredRows(in, width * RGB_CHANNELS, height, RGB_CHANNELS, scratch, (rowIndex, rgbRow, ignoredRowBytes) -> {
            int rgbOffset = 0;
            int pixelOffset = targetOffset + (rowIndex * scanlineStride);
            for (int x = 0; x < width; x++) {
                int red = rgbRow[rgbOffset++] & 0xFF;
                int green = rgbRow[rgbOffset++] & 0xFF;
                int blue = rgbRow[rgbOffset++] & 0xFF;
                targetPixels[pixelOffset + x] = 0xFF000000 | (red << 16) | (green << 8) | blue;
            }
        });
        decodeFilteredRows(in, width, height, 1, scratch, (rowIndex, alphaRow, ignoredRowBytes) -> {
            int pixelOffset = targetOffset + (rowIndex * scanlineStride);
            for (int x = 0; x < width; x++) {
                int alpha = alphaRow[x] & 0xFF;
                targetPixels[pixelOffset + x] = (alpha << 24) | (targetPixels[pixelOffset + x] & 0x00FFFFFF);
            }
        });
    }

    protected static void decodeIndexedColorPlusAlphaIntoArgbBuffer(@NotNull DataInputStream in, int width, int height,
                                                                    @NotNull int[] targetPixels, int targetOffset, int scanlineStride,
                                                                    @Nullable AfmaDecodeScratch scratch) throws IOException {
        AlphaMaskMode alphaMode = AlphaMaskMode.byId(in.readUnsignedByte());
        int bitsPerIndex = in.readUnsignedByte();
        validatePaletteBits(bitsPerIndex);

        int paletteSize = in.readUnsignedShort();
        if ((paletteSize <= 0) || (paletteSize > MAX_PALETTE_COLORS)) {
            throw new IOException("AFMA BIN_INTRA indexed-color payload palette size is invalid");
        }

        int[] palette = new int[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            int red = in.readUnsignedByte();
            int green = in.readUnsignedByte();
            int blue = in.readUnsignedByte();
            palette[i] = 0xFF000000 | (red << 16) | (green << 8) | blue;
        }

        int rowBytes = packedRowBytes(width, bitsPerIndex);
        decodeFilteredRows(in, rowBytes, height, 1, scratch, (rowIndex, packedRow, ignoredRowBytes) -> {
            int pixelOffset = targetOffset + (rowIndex * scanlineStride);
            for (int x = 0; x < width; x++) {
                int paletteIndex = unpackPackedValue(packedRow, 0, x, bitsPerIndex);
                if ((paletteIndex < 0) || (paletteIndex >= palette.length)) {
                    throw new IOException("AFMA BIN_INTRA indexed-color payload references an invalid palette index");
                }
                targetPixels[pixelOffset + x] = palette[paletteIndex];
            }
        });

        if (alphaMode == AlphaMaskMode.BINARY) {
            int binaryRowBytes = packedRowBytes(width, 1);
            decodeFilteredRows(in, binaryRowBytes, height, 1, scratch, (rowIndex, packedMaskRow, ignoredRowBytes) -> {
                int pixelOffset = targetOffset + (rowIndex * scanlineStride);
                for (int x = 0; x < width; x++) {
                    int alpha = unpackPackedValue(packedMaskRow, 0, x, 1) != 0 ? 0xFF : 0x00;
                    targetPixels[pixelOffset + x] = (targetPixels[pixelOffset + x] & 0x00FFFFFF) | (alpha << 24);
                }
            });
            return;
        }

        decodeFilteredRows(in, width, height, 1, scratch, (rowIndex, alphaRow, ignoredRowBytes) -> {
            int pixelOffset = targetOffset + (rowIndex * scanlineStride);
            for (int x = 0; x < width; x++) {
                targetPixels[pixelOffset + x] = (targetPixels[pixelOffset + x] & 0x00FFFFFF) | ((alphaRow[x] & 0xFF) << 24);
            }
        });
    }

    protected static void decodeColorPlusAlphaMaskIntoArgbBuffer(@NotNull DataInputStream in, int width, int height,
                                                                 @NotNull int[] targetPixels, int targetOffset, int scanlineStride,
                                                                 @Nullable AfmaDecodeScratch scratch) throws IOException {
        int red = in.readUnsignedByte();
        int green = in.readUnsignedByte();
        int blue = in.readUnsignedByte();
        AlphaMaskMode alphaMode = AlphaMaskMode.byId(in.readUnsignedByte());

        int baseRgb = (red << 16) | (green << 8) | blue;
        if (alphaMode == AlphaMaskMode.BINARY) {
            int rowBytes = packedRowBytes(width, 1);
            decodeFilteredRows(in, rowBytes, height, 1, scratch, (rowIndex, packedMaskRow, ignoredRowBytes) -> {
                int pixelOffset = targetOffset + (rowIndex * scanlineStride);
                for (int x = 0; x < width; x++) {
                    int alpha = unpackPackedValue(packedMaskRow, 0, x, 1) != 0 ? 0xFF : 0x00;
                    targetPixels[pixelOffset + x] = (alpha << 24) | baseRgb;
                }
            });
            return;
        }

        decodeFilteredRows(in, width, height, 1, scratch, (rowIndex, alphaRow, ignoredRowBytes) -> {
            int pixelOffset = targetOffset + (rowIndex * scanlineStride);
            for (int x = 0; x < width; x++) {
                int alpha = alphaRow[x] & 0xFF;
                targetPixels[pixelOffset + x] = (alpha << 24) | baseRgb;
            }
        });
    }

    protected static void writeHeader(@NotNull DataOutputStream out, @NotNull Mode mode, int width, int height) throws IOException {
        validateDimensions(width, height);
        out.writeInt(PAYLOAD_MAGIC);
        out.writeByte(PAYLOAD_VERSION);
        out.writeByte(mode.id());
        out.writeShort(width);
        out.writeShort(height);
    }

    protected static void writePlanarFiltered(@NotNull DataOutputStream out, @NotNull int[] pixels,
                                              int width, int height, int channels) throws IOException {
        for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
            int finalChannelIndex = channelIndex;
            writeFilteredRows(out, width, height, 1, (rowIndex, rowBuffer) ->
                    fillPlanarChannelRow(pixels, width, rowIndex, finalChannelIndex, rowBuffer));
        }
    }

    @NotNull
    protected static AfmaStoredPayload.BufferedPayload capturePayload(@NotNull PayloadWriter writer) throws IOException {
        Objects.requireNonNull(writer);
        return AfmaStoredPayload.capture(out -> {
            DataOutputStream dataOut = new DataOutputStream(out);
            writer.write(dataOut);
            dataOut.flush();
        });
    }

    protected static void writeFilteredRows(@NotNull DataOutputStream out, int rowBytes, int height, int bytesPerPixel,
                                            @NotNull RowWriter rowWriter) throws IOException {
        if (rowBytes < 0 || height <= 0 || bytesPerPixel <= 0) {
            throw new IllegalArgumentException("AFMA BIN_INTRA filter row dimensions are invalid");
        }

        byte[] previousRow = new byte[rowBytes];
        byte[] currentRow = new byte[rowBytes];
        byte[] bestRow = new byte[rowBytes];
        byte[] candidateRow = new byte[rowBytes];
        for (int row = 0; row < height; row++) {
            rowWriter.fillRow(row, currentRow);
            FilterSelection selection = selectBestFilter(currentRow, 0, rowBytes, previousRow, bytesPerPixel, bestRow, candidateRow);
            out.writeByte(selection.filter().id());
            out.write(bestRow, 0, rowBytes);
            System.arraycopy(currentRow, 0, previousRow, 0, rowBytes);
        }
    }

    protected static void fillInterleavedColorRow(@NotNull int[] pixels, int width, int rowIndex, int channels, @NotNull byte[] rowBuffer) {
        int pixelOffset = rowIndex * width;
        int byteOffset = 0;
        for (int x = 0; x < width; x++) {
            int color = pixels[pixelOffset + x];
            rowBuffer[byteOffset++] = (byte) ((color >> 16) & 0xFF);
            rowBuffer[byteOffset++] = (byte) ((color >> 8) & 0xFF);
            rowBuffer[byteOffset++] = (byte) (color & 0xFF);
            if (channels == RGBA_CHANNELS) {
                rowBuffer[byteOffset++] = (byte) ((color >>> 24) & 0xFF);
            }
        }
    }

    protected static void fillPlanarChannelRow(@NotNull int[] pixels, int width, int rowIndex, int channelIndex, @NotNull byte[] rowBuffer) {
        int pixelOffset = rowIndex * width;
        for (int x = 0; x < width; x++) {
            int color = pixels[pixelOffset + x];
            rowBuffer[x] = switch (channelIndex) {
                case 0 -> (byte) ((color >> 16) & 0xFF);
                case 1 -> (byte) ((color >> 8) & 0xFF);
                case 2 -> (byte) (color & 0xFF);
                case 3 -> (byte) ((color >>> 24) & 0xFF);
                default -> throw new IllegalArgumentException("Unsupported AFMA BIN_INTRA planar channel index: " + channelIndex);
            };
        }
    }

    protected static void fillPaletteIndexRow(@NotNull int[] pixels, int width, int rowIndex, int bitsPerIndex,
                                              @NotNull Map<Integer, Integer> colorToIndex, boolean rgbOnly,
                                              @NotNull byte[] rowBuffer) throws IOException {
        Arrays.fill(rowBuffer, (byte) 0);
        int pixelOffset = rowIndex * width;
        for (int x = 0; x < width; x++) {
            int color = pixels[pixelOffset + x];
            Integer paletteIndex = colorToIndex.get(rgbOnly ? (color & 0x00FFFFFF) : color);
            if (paletteIndex == null) {
                throw new IOException("AFMA BIN_INTRA palette index lookup failed");
            }
            writePackedValue(rowBuffer, 0, x, bitsPerIndex, paletteIndex);
        }
    }

    protected static void fillFullAlphaRow(@NotNull int[] pixels, int width, int rowIndex, @NotNull byte[] rowBuffer) {
        int pixelOffset = rowIndex * width;
        for (int x = 0; x < width; x++) {
            rowBuffer[x] = (byte) ((pixels[pixelOffset + x] >>> 24) & 0xFF);
        }
    }

    protected static void fillBinaryAlphaRow(@NotNull int[] pixels, int width, int rowIndex, @NotNull byte[] rowBuffer) {
        Arrays.fill(rowBuffer, (byte) 0);
        int pixelOffset = rowIndex * width;
        for (int x = 0; x < width; x++) {
            int alpha = (pixels[pixelOffset + x] >>> 24) & 0xFF;
            writePackedValue(rowBuffer, 0, x, 1, alpha >= 0x80 ? 1 : 0);
        }
    }

    protected static void sortPaletteEntries(@NotNull List<Map.Entry<Integer, Integer>> paletteEntries, @NotNull PaletteOrdering ordering) {
        paletteEntries.sort((first, second) -> switch (ordering) {
            case FREQUENCY -> {
                int countCompare = Integer.compare(second.getValue(), first.getValue());
                if (countCompare != 0) {
                    yield countCompare;
                }
                yield Integer.compareUnsigned(first.getKey(), second.getKey());
            }
            case STABLE_COLOR -> Integer.compareUnsigned(first.getKey(), second.getKey());
        });
    }

    @NotNull
    protected static IndexedModeContext prepareIndexedModeContext(int width, @NotNull Map<Integer, Integer> colorCounts,
                                                                  @NotNull PaletteOrdering paletteOrdering,
                                                                  boolean rgbOnly) {
        List<Map.Entry<Integer, Integer>> paletteEntries = new ArrayList<>(colorCounts.entrySet());
        sortPaletteEntries(paletteEntries, paletteOrdering);

        int[] palette = new int[paletteEntries.size()];
        Map<Integer, Integer> colorToIndex = new HashMap<>(paletteEntries.size() * 2);
        for (int i = 0; i < paletteEntries.size(); i++) {
            int color = paletteEntries.get(i).getKey();
            int paletteColor = rgbOnly ? (color & 0x00FFFFFF) : color;
            palette[i] = paletteColor;
            colorToIndex.put(paletteColor, i);
        }

        int bitsPerIndex = paletteBits(palette.length);
        return new IndexedModeContext(palette, colorToIndex, bitsPerIndex, packedRowBytes(width, bitsPerIndex));
    }

    protected static long payloadHeaderBytes() {
        return 10L;
    }

    protected static long filteredStreamPayloadLength(int rowBytes, int height) {
        return (long) height * ((long) rowBytes + 1L);
    }

    protected static long estimateFilteredStreamArchiveBytes(int rowBytes, int height, int bytesPerPixel,
                                                             @NotNull RowWriter rowWriter) throws IOException {
        if (rowBytes <= 0 || height <= 0 || bytesPerPixel <= 0) {
            return 0L;
        }

        // Sample a few representative rows so large regions can rank modes without serializing every filtered stream.
        int[] sampledRows = buildEstimateRowIndices(height);
        byte[] previousRow = new byte[rowBytes];
        byte[] currentRow = new byte[rowBytes];
        byte[] bestRow = new byte[rowBytes];
        byte[] candidateRow = new byte[rowBytes];
        long sampledFilterScore = 0L;
        long sampledNonZeroBytes = 0L;
        for (int rowIndex : sampledRows) {
            if (rowIndex <= 0) {
                Arrays.fill(previousRow, (byte) 0);
            } else {
                rowWriter.fillRow(rowIndex - 1, previousRow);
            }
            rowWriter.fillRow(rowIndex, currentRow);
            FilterSelection selection = selectBestFilter(currentRow, 0, rowBytes, previousRow, bytesPerPixel, bestRow, candidateRow);
            sampledFilterScore += selection.score();
            sampledNonZeroBytes += countNonZeroBytes(bestRow, rowBytes);
        }

        long rawDataBytes = (long) rowBytes * height;
        double rowScale = (double) height / sampledRows.length;
        long estimatedDataBytes = Math.round((((double) sampledFilterScore) / 32.0D
                + ((double) sampledNonZeroBytes) / 3.0D) * rowScale);
        if (estimatedDataBytes < 0L) {
            estimatedDataBytes = 0L;
        } else if (estimatedDataBytes > rawDataBytes) {
            estimatedDataBytes = rawDataBytes;
        }
        return height + estimatedDataBytes;
    }

    protected static int[] buildEstimateRowIndices(int height) {
        int sampleCount = Math.min(height, MODE_ESTIMATE_SAMPLE_ROWS);
        int[] sampledRows = new int[sampleCount];
        if (sampleCount >= height) {
            for (int rowIndex = 0; rowIndex < height; rowIndex++) {
                sampledRows[rowIndex] = rowIndex;
            }
            return sampledRows;
        }
        if (sampleCount == 1) {
            sampledRows[0] = 0;
            return sampledRows;
        }
        long rowRange = height - 1L;
        long sampleRange = sampleCount - 1L;
        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            sampledRows[sampleIndex] = (int) ((sampleIndex * rowRange) / sampleRange);
        }
        return sampledRows;
    }

    protected static int countNonZeroBytes(@NotNull byte[] bytes, int length) {
        int nonZeroBytes = 0;
        for (int i = 0; i < length; i++) {
            if (bytes[i] != 0) {
                nonZeroBytes++;
            }
        }
        return nonZeroBytes;
    }

    @NotNull
    protected static byte[] encodeFilteredRows(@NotNull byte[] rawBytes, int rowBytes, int height, int bytesPerPixel) {
        return encodeFilteredRows(rawBytes, 0, rowBytes, height, bytesPerPixel);
    }

    @NotNull
    protected static byte[] encodeFilteredRows(@NotNull byte[] rawBytes, int rawOffset, int rowBytes, int height, int bytesPerPixel) {
        if (rawOffset < 0 || rowBytes < 0 || height <= 0 || bytesPerPixel <= 0) {
            throw new IllegalArgumentException("AFMA BIN_INTRA filter row dimensions are invalid");
        }
        long requiredLength = (long) rawOffset + ((long) rowBytes * (long) height);
        if (requiredLength > rawBytes.length) {
            throw new IllegalArgumentException("AFMA BIN_INTRA filter row dimensions are invalid");
        }

        byte[] encoded = new byte[height * (rowBytes + 1)];
        byte[] previousRow = new byte[rowBytes];
        byte[] bestRow = new byte[rowBytes];
        byte[] candidateRow = new byte[rowBytes];
        int encodedOffset = 0;
        int rowOffset = rawOffset;
        for (int row = 0; row < height; row++) {
            FilterSelection selection = selectBestFilter(rawBytes, rowOffset, rowBytes, previousRow, bytesPerPixel, bestRow, candidateRow);
            encoded[encodedOffset++] = (byte) selection.filter().id();
            System.arraycopy(bestRow, 0, encoded, encodedOffset, rowBytes);
            encodedOffset += rowBytes;
            System.arraycopy(rawBytes, rowOffset, previousRow, 0, rowBytes);
            rowOffset += rowBytes;
        }
        return encoded;
    }

    @Nullable
    protected static ScoredPayloadCandidate buildBestCandidate(int width, int height, @NotNull PixelCandidateContext pixelCandidate) throws IOException {
        ScoredPayloadCandidate bestCandidate = null;
        bestCandidate = pickBetterCandidate(bestCandidate, buildSolidCandidate(width, height, pixelCandidate));
        bestCandidate = pickBetterCandidate(bestCandidate, buildIndexedCandidate(width, height, pixelCandidate, PaletteOrdering.FREQUENCY));
        bestCandidate = pickBetterCandidate(bestCandidate, buildIndexedCandidate(width, height, pixelCandidate, PaletteOrdering.STABLE_COLOR));

        if (pixelCandidate.hasAlpha()) {
            bestCandidate = pickBetterCandidate(bestCandidate, buildFilteredCandidate(width, height, pixelCandidate, Mode.RGBA_FILTERED));
            bestCandidate = pickBetterCandidate(bestCandidate, buildPlanarFilteredCandidate(width, height, pixelCandidate, Mode.RGBA_PLANAR_FILTERED));
            bestCandidate = pickBetterCandidate(bestCandidate, buildIndexedColorPlusAlphaCandidate(width, height, pixelCandidate, PaletteOrdering.FREQUENCY));
            bestCandidate = pickBetterCandidate(bestCandidate, buildIndexedColorPlusAlphaCandidate(width, height, pixelCandidate, PaletteOrdering.STABLE_COLOR));
            bestCandidate = pickBetterCandidate(bestCandidate, buildSplitAlphaCandidate(width, height, pixelCandidate));
            bestCandidate = pickBetterCandidate(bestCandidate, buildColorPlusAlphaMaskCandidate(width, height, pixelCandidate));
        } else {
            bestCandidate = pickBetterCandidate(bestCandidate, buildFilteredCandidate(width, height, pixelCandidate, Mode.RGB_FILTERED));
            bestCandidate = pickBetterCandidate(bestCandidate, buildPlanarFilteredCandidate(width, height, pixelCandidate, Mode.RGB_PLANAR_FILTERED));
        }
        return bestCandidate;
    }

    @Nullable
    protected static EstimatedPayloadCandidate estimateBestCandidate(int width, int height, @NotNull PixelCandidateContext pixelCandidate) throws IOException {
        EstimatedPayloadCandidate bestCandidate = null;
        for (ModePlan modePlan : collectModePlans(width, height, pixelCandidate)) {
            bestCandidate = pickBetterEstimatedCandidate(bestCandidate, new EstimatedPayloadCandidate(
                    modePlan.mode(),
                    modePlan.estimateBytes(),
                    modePlan.payloadLength(),
                    pixelCandidate.lossless(),
                    modePlan.stabilityPriority()
            ));
        }
        return bestCandidate;
    }

    @NotNull
    protected static List<ModePlan> collectModePlans(int width, int height, @NotNull PixelCandidateContext pixelCandidate) throws IOException {
        List<ModePlan> modePlans = new ArrayList<>(8);
        int pixelCount = width * height;
        addModePlan(modePlans, buildSolidModePlan(width, height, pixelCandidate));
        Map<Integer, Integer> fullColorCounts = pixelCandidate.fullColorCounts();
        int fullPaletteSize = (fullColorCounts != null) ? fullColorCounts.size() : Integer.MAX_VALUE;
        boolean paletteCandidateScheduled = false;
        if (shouldTryIndexedMode(fullPaletteSize, pixelCount)) {
            paletteCandidateScheduled = true;
            addModePlan(modePlans, buildIndexedModePlan(width, height, pixelCandidate, PaletteOrdering.FREQUENCY));
            if (shouldTryStableIndexedMode(fullPaletteSize, pixelCount)) {
                addModePlan(modePlans, buildIndexedModePlan(width, height, pixelCandidate, PaletteOrdering.STABLE_COLOR));
            }
        }

        if (pixelCandidate.hasAlpha()) {
            boolean preferPlanarTruecolor = pixelCandidate.alphaVaries();
            addModePlan(modePlans, preferPlanarTruecolor
                    ? buildPlanarFilteredModePlan(width, height, pixelCandidate, Mode.RGBA_PLANAR_FILTERED)
                    : buildFilteredModePlan(width, height, pixelCandidate, Mode.RGBA_FILTERED));

            Map<Integer, Integer> rgbColorCounts = pixelCandidate.rgbColorCounts();
            int rgbPaletteSize = (rgbColorCounts != null) ? rgbColorCounts.size() : Integer.MAX_VALUE;
            boolean indexedColorAlphaCandidateScheduled = false;
            if (pixelCandidate.alphaVaries() && shouldTryIndexedColorPlusAlphaMode(rgbPaletteSize, pixelCount)) {
                indexedColorAlphaCandidateScheduled = true;
                addModePlan(modePlans, buildIndexedColorPlusAlphaModePlan(width, height, pixelCandidate, PaletteOrdering.FREQUENCY));
                if (shouldTryStableIndexedColorPlusAlphaMode(rgbPaletteSize, pixelCount)) {
                    addModePlan(modePlans, buildIndexedColorPlusAlphaModePlan(width, height, pixelCandidate, PaletteOrdering.STABLE_COLOR));
                }
            }

            if (pixelCandidate.alphaVaries()) {
                addModePlan(modePlans, buildSplitAlphaModePlan(width, height, pixelCandidate));
            }
            addModePlan(modePlans, buildColorPlusAlphaMaskModePlan(width, height, pixelCandidate));

            if (shouldTrySecondaryTruecolorMode(pixelCount, paletteCandidateScheduled || indexedColorAlphaCandidateScheduled)) {
                addModePlan(modePlans, preferPlanarTruecolor
                        ? buildFilteredModePlan(width, height, pixelCandidate, Mode.RGBA_FILTERED)
                        : buildPlanarFilteredModePlan(width, height, pixelCandidate, Mode.RGBA_PLANAR_FILTERED));
            }
        } else {
            addModePlan(modePlans, buildFilteredModePlan(width, height, pixelCandidate, Mode.RGB_FILTERED));
            if (shouldTrySecondaryTruecolorMode(pixelCount, paletteCandidateScheduled)) {
                addModePlan(modePlans, buildPlanarFilteredModePlan(width, height, pixelCandidate, Mode.RGB_PLANAR_FILTERED));
            }
        }
        return modePlans;
    }

    @Nullable
    protected static ModePlan buildSolidModePlan(int width, int height, @NotNull PixelCandidateContext pixelCandidate) {
        if (!pixelCandidate.isSolidColor()) {
            return null;
        }
        long payloadLength = payloadHeaderBytes() + 4L;
        return new ModePlan(
                Mode.SOLID,
                ModeFamily.TRUECOLOR,
                payloadLength,
                1,
                () -> payloadLength,
                () -> buildSolidCandidate(width, height, pixelCandidate)
        );
    }

    protected static void addModePlan(@NotNull List<ModePlan> modePlans, @Nullable ModePlan modePlan) {
        if (modePlan != null) {
            modePlans.add(modePlan);
        }
    }

    protected static boolean shouldEvaluateAllModePlans(int width, int height, int modePlanCount) {
        long pixelCount = (long) width * height;
        return (modePlanCount <= MAX_SHORTLISTED_MODE_CANDIDATES) || (pixelCount <= MAX_EXHAUSTIVE_MODE_SELECTION_PIXELS);
    }

    @NotNull
    protected static List<ModePlan> shortlistModePlans(@NotNull List<ModePlan> modePlans) throws IOException {
        ArrayList<EstimatedModePlan> estimatedPlans = new ArrayList<>(modePlans.size());
        for (ModePlan modePlan : modePlans) {
            estimatedPlans.add(new EstimatedModePlan(modePlan, modePlan.estimateBytes()));
        }
        estimatedPlans.sort((first, second) -> {
            int compare = Long.compare(first.estimatedBytes(), second.estimatedBytes());
            if (compare != 0) {
                return compare;
            }
            compare = Long.compare(first.plan().payloadLength(), second.plan().payloadLength());
            if (compare != 0) {
                return compare;
            }
            compare = Integer.compare(first.plan().stabilityPriority(), second.plan().stabilityPriority());
            if (compare != 0) {
                return compare;
            }
            return Integer.compare(first.plan().mode().priority(), second.plan().mode().priority());
        });
        if (estimatedPlans.size() <= MAX_SHORTLISTED_MODE_CANDIDATES) {
            return extractModePlans(estimatedPlans);
        }

        ArrayList<EstimatedModePlan> shortlistedPlans = new ArrayList<>(MAX_SHORTLISTED_MODE_CANDIDATES);
        EnumMap<ModeFamily, Integer> familyCounts = new EnumMap<>(ModeFamily.class);
        // Keep a strong baseline from each family before filling the remaining shortlist slots by estimate.
        for (EstimatedModePlan estimatedPlan : estimatedPlans) {
            if (shortlistedPlans.size() >= MAX_SHORTLISTED_MODE_CANDIDATES) {
                break;
            }
            if (familyCounts.containsKey(estimatedPlan.plan().family())) {
                continue;
            }
            shortlistedPlans.add(estimatedPlan);
            familyCounts.put(estimatedPlan.plan().family(), 1);
        }

        for (EstimatedModePlan estimatedPlan : estimatedPlans) {
            if (shortlistedPlans.size() >= MAX_SHORTLISTED_MODE_CANDIDATES) {
                break;
            }
            if (shortlistedPlans.contains(estimatedPlan)) {
                continue;
            }

            ModeFamily family = estimatedPlan.plan().family();
            int familyCount = familyCounts.getOrDefault(family, 0);
            if (familyCount >= MAX_SHORTLISTED_MODE_CANDIDATES_PER_FAMILY) {
                continue;
            }

            shortlistedPlans.add(estimatedPlan);
            familyCounts.put(family, familyCount + 1);
        }
        return extractModePlans(shortlistedPlans);
    }

    @NotNull
    protected static List<ModePlan> extractModePlans(@NotNull List<EstimatedModePlan> estimatedPlans) {
        ArrayList<ModePlan> modePlans = new ArrayList<>(estimatedPlans.size());
        for (EstimatedModePlan estimatedPlan : estimatedPlans) {
            modePlans.add(estimatedPlan.plan());
        }
        return modePlans;
    }

    @Nullable
    protected static ScoredPayloadCandidate evaluateModePlans(@NotNull List<ModePlan> modePlans) throws IOException {
        ScoredPayloadCandidate bestCandidate = null;
        for (ModePlan modePlan : modePlans) {
            bestCandidate = pickBetterCandidate(bestCandidate, modePlan.buildCandidate());
        }
        return bestCandidate;
    }

    @Nullable
    protected static ModePlan buildIndexedModePlan(int width, int height, @NotNull PixelCandidateContext pixelCandidate,
                                                   @NotNull PaletteOrdering paletteOrdering) throws IOException {
        Map<Integer, Integer> colorCounts = pixelCandidate.fullColorCounts();
        if (colorCounts == null) {
            return null;
        }

        boolean paletteHasAlpha = pixelCandidate.hasAlpha();
        IndexedModeContext indexedModeContext = prepareIndexedModeContext(width, colorCounts, paletteOrdering, false);
        long metadataBytes = payloadHeaderBytes() + 1L + 1L + 2L + ((long) indexedModeContext.palette().length * (paletteHasAlpha ? 4L : 3L));
        long payloadLength = metadataBytes + filteredStreamPayloadLength(indexedModeContext.packedRowBytes(), height);
        int stabilityPriority = paletteOrdering == PaletteOrdering.STABLE_COLOR ? 0 : 1;
        int[] pixels = pixelCandidate.pixels();
        return new ModePlan(
                Mode.INDEXED,
                ModeFamily.INDEXED,
                payloadLength,
                stabilityPriority,
                () -> metadataBytes + estimateFilteredStreamArchiveBytes(indexedModeContext.packedRowBytes(), height, 1, (rowIndex, rowBuffer) ->
                        fillPaletteIndexRow(pixels, width, rowIndex, indexedModeContext.bitsPerIndex(), indexedModeContext.colorToIndex(), false, rowBuffer)),
                () -> buildIndexedCandidate(width, height, pixelCandidate, indexedModeContext, paletteHasAlpha, stabilityPriority)
        );
    }

    @NotNull
    protected static ModePlan buildFilteredModePlan(int width, int height, @NotNull PixelCandidateContext pixelCandidate,
                                                    @NotNull Mode mode) {
        int channels = switch (mode) {
            case RGB_FILTERED -> RGB_CHANNELS;
            case RGBA_FILTERED -> RGBA_CHANNELS;
            default -> throw new IllegalArgumentException("Unsupported AFMA BIN_INTRA filtered mode: " + mode);
        };
        int[] pixels = pixelCandidate.pixels();
        int rowBytes = width * channels;
        long payloadLength = payloadHeaderBytes() + filteredStreamPayloadLength(rowBytes, height);
        return new ModePlan(
                mode,
                ModeFamily.TRUECOLOR,
                payloadLength,
                1,
                () -> payloadHeaderBytes() + estimateFilteredStreamArchiveBytes(rowBytes, height, channels,
                        (rowIndex, rowBuffer) -> fillInterleavedColorRow(pixels, width, rowIndex, channels, rowBuffer)),
                () -> buildFilteredCandidate(width, height, pixelCandidate, mode)
        );
    }

    @NotNull
    protected static ModePlan buildPlanarFilteredModePlan(int width, int height, @NotNull PixelCandidateContext pixelCandidate,
                                                          @NotNull Mode mode) {
        int channels = switch (mode) {
            case RGB_PLANAR_FILTERED -> RGB_CHANNELS;
            case RGBA_PLANAR_FILTERED -> RGBA_CHANNELS;
            default -> throw new IllegalArgumentException("Unsupported AFMA BIN_INTRA planar mode: " + mode);
        };
        int[] pixels = pixelCandidate.pixels();
        long payloadLength = payloadHeaderBytes() + ((long) channels * filteredStreamPayloadLength(width, height));
        return new ModePlan(
                mode,
                ModeFamily.TRUECOLOR,
                payloadLength,
                1,
                () -> {
                    long estimatedBytes = payloadHeaderBytes();
                    for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                        int finalChannelIndex = channelIndex;
                        estimatedBytes += estimateFilteredStreamArchiveBytes(width, height, 1,
                                (rowIndex, rowBuffer) -> fillPlanarChannelRow(pixels, width, rowIndex, finalChannelIndex, rowBuffer));
                    }
                    return estimatedBytes;
                },
                () -> buildPlanarFilteredCandidate(width, height, pixelCandidate, mode)
        );
    }

    @NotNull
    protected static ModePlan buildSplitAlphaModePlan(int width, int height, @NotNull PixelCandidateContext pixelCandidate) {
        int[] pixels = pixelCandidate.pixels();
        long payloadLength = payloadHeaderBytes()
                + filteredStreamPayloadLength(width * RGB_CHANNELS, height)
                + filteredStreamPayloadLength(width, height);
        return new ModePlan(
                Mode.RGB_PLUS_ALPHA_SPLIT,
                ModeFamily.ALPHA_SPECIAL,
                payloadLength,
                1,
                () -> payloadHeaderBytes()
                        + estimateFilteredStreamArchiveBytes(width * RGB_CHANNELS, height, RGB_CHANNELS,
                        (rowIndex, rowBuffer) -> fillInterleavedColorRow(pixels, width, rowIndex, RGB_CHANNELS, rowBuffer))
                        + estimateFilteredStreamArchiveBytes(width, height, 1,
                        (rowIndex, rowBuffer) -> fillFullAlphaRow(pixels, width, rowIndex, rowBuffer)),
                () -> buildSplitAlphaCandidate(width, height, pixelCandidate)
        );
    }

    @Nullable
    protected static ModePlan buildColorPlusAlphaMaskModePlan(int width, int height,
                                                              @NotNull PixelCandidateContext pixelCandidate) {
        if (!pixelCandidate.hasUniformRgb() || !pixelCandidate.alphaVaries()) {
            return null;
        }

        int[] pixels = pixelCandidate.pixels();
        boolean binaryAlpha = pixelCandidate.binaryAlpha();
        int alphaRowBytes = binaryAlpha ? packedRowBytes(width, 1) : width;
        long metadataBytes = payloadHeaderBytes() + 4L;
        long payloadLength = metadataBytes + filteredStreamPayloadLength(alphaRowBytes, height);
        return new ModePlan(
                Mode.COLOR_PLUS_ALPHA_MASK,
                ModeFamily.ALPHA_SPECIAL,
                payloadLength,
                1,
                () -> metadataBytes + estimateFilteredStreamArchiveBytes(alphaRowBytes, height, 1, binaryAlpha
                        ? (rowIndex, rowBuffer) -> fillBinaryAlphaRow(pixels, width, rowIndex, rowBuffer)
                        : (rowIndex, rowBuffer) -> fillFullAlphaRow(pixels, width, rowIndex, rowBuffer)),
                () -> buildColorPlusAlphaMaskCandidate(width, height, pixelCandidate)
        );
    }

    @Nullable
    protected static ModePlan buildIndexedColorPlusAlphaModePlan(int width, int height, @NotNull PixelCandidateContext pixelCandidate,
                                                                 @NotNull PaletteOrdering paletteOrdering) throws IOException {
        Map<Integer, Integer> rgbCounts = pixelCandidate.rgbColorCounts();
        if (rgbCounts == null) {
            return null;
        }

        IndexedModeContext indexedModeContext = prepareIndexedModeContext(width, rgbCounts, paletteOrdering, true);
        boolean binaryAlpha = pixelCandidate.binaryAlpha();
        int alphaRowBytes = binaryAlpha ? packedRowBytes(width, 1) : width;
        long metadataBytes = payloadHeaderBytes() + 1L + 1L + 2L + ((long) indexedModeContext.palette().length * 3L);
        long payloadLength = metadataBytes
                + filteredStreamPayloadLength(indexedModeContext.packedRowBytes(), height)
                + filteredStreamPayloadLength(alphaRowBytes, height);
        int stabilityPriority = paletteOrdering == PaletteOrdering.STABLE_COLOR ? 0 : 1;
        int[] pixels = pixelCandidate.pixels();
        return new ModePlan(
                Mode.INDEXED_COLOR_PLUS_ALPHA,
                ModeFamily.INDEXED,
                payloadLength,
                stabilityPriority,
                () -> metadataBytes
                        + estimateFilteredStreamArchiveBytes(indexedModeContext.packedRowBytes(), height, 1, (rowIndex, rowBuffer) ->
                        fillPaletteIndexRow(pixels, width, rowIndex, indexedModeContext.bitsPerIndex(), indexedModeContext.colorToIndex(), true, rowBuffer))
                        + estimateFilteredStreamArchiveBytes(alphaRowBytes, height, 1, binaryAlpha
                        ? (rowIndex, rowBuffer) -> fillBinaryAlphaRow(pixels, width, rowIndex, rowBuffer)
                        : (rowIndex, rowBuffer) -> fillFullAlphaRow(pixels, width, rowIndex, rowBuffer)),
                () -> buildIndexedColorPlusAlphaCandidate(width, height, pixelCandidate, indexedModeContext, stabilityPriority)
        );
    }

    protected static boolean shouldTryIndexedMode(int paletteSize, int pixelCount) {
        return (paletteSize > 0)
                && ((paletteSize <= INDEXED_PRIMARY_MAX_PALETTE_COLORS)
                || ((paletteSize <= 160) && ((long) paletteSize * 4L <= pixelCount)));
    }

    protected static boolean shouldTryStableIndexedMode(int paletteSize, int pixelCount) {
        return (paletteSize > 0)
                && ((paletteSize <= INDEXED_STABLE_MAX_PALETTE_COLORS)
                || ((paletteSize <= 48) && ((long) paletteSize * 8L <= pixelCount)));
    }

    protected static boolean shouldTryIndexedColorPlusAlphaMode(int paletteSize, int pixelCount) {
        return (paletteSize > 0)
                && ((paletteSize <= INDEXED_COLOR_ALPHA_PRIMARY_MAX_PALETTE_COLORS)
                || ((paletteSize <= 192) && ((long) paletteSize * 3L <= pixelCount)));
    }

    protected static boolean shouldTryStableIndexedColorPlusAlphaMode(int paletteSize, int pixelCount) {
        return (paletteSize > 0)
                && ((paletteSize <= INDEXED_COLOR_ALPHA_STABLE_MAX_PALETTE_COLORS)
                || ((paletteSize <= 64) && ((long) paletteSize * 6L <= pixelCount)));
    }

    protected static boolean shouldTrySecondaryTruecolorMode(int pixelCount, boolean paletteCandidateScheduled) {
        return (pixelCount <= SECONDARY_TRUECOLOR_MODE_MAX_PIXELS) || !paletteCandidateScheduled;
    }

    @Nullable
    protected static ScoredPayloadCandidate pickBetterCandidate(@Nullable ScoredPayloadCandidate currentBest,
                                                                @Nullable ScoredPayloadCandidate candidate) {
        if (candidate == null) {
            return currentBest;
        }
        if (currentBest == null) {
            return candidate;
        }
        if (candidate.isBetterThan(currentBest)) {
            return candidate;
        }
        return currentBest;
    }

    @Nullable
    protected static EstimatedPayloadCandidate pickBetterEstimatedCandidate(@Nullable EstimatedPayloadCandidate currentBest,
                                                                            @Nullable EstimatedPayloadCandidate candidate) {
        if (candidate == null) {
            return currentBest;
        }
        if (currentBest == null) {
            return candidate;
        }
        if (candidate.isBetterThan(currentBest)) {
            return candidate;
        }
        return currentBest;
    }

    @NotNull
    protected static List<PixelCandidateContext> collectPixelCandidates(int width, int height, @NotNull int[] pixels, @NotNull EncodePreferences preferences) {
        List<PixelCandidateContext> pixelCandidates = new ArrayList<>(4);
        PixelCandidateContext sourceCandidate = addPixelCandidate(pixelCandidates, pixels, true);

        if (!preferences.perceptualCandidatesEnabled()) {
            return pixelCandidates;
        }

        if (sourceCandidate.hasHiddenTransparentRgb()) {
            addPixelCandidate(pixelCandidates, normalizeHiddenTransparentPixels(pixels), false);
        }

        int sampledPaletteBucketCount = estimateSampledPaletteBucketCount(pixels);
        for (ChannelQuantizationProfile channelProfile : selectChannelQuantizationProfiles(sampledPaletteBucketCount)) {
            int[] quantizedPixels = buildPerceptualChannelCandidate(pixels, channelProfile, preferences);
            if (quantizedPixels != null) {
                addPixelCandidate(pixelCandidates, quantizedPixels, false);
            }
        }
        List<PerceptualProfile> perceptualProfiles = selectPerceptualProfiles(sampledPaletteBucketCount);
        if (shouldParallelizePerceptualCandidates(width, height, perceptualProfiles.size())) {
            ArrayList<CompletableFuture<int[]>> profileFutures = new ArrayList<>(perceptualProfiles.size());
            for (PerceptualProfile profile : perceptualProfiles) {
                profileFutures.add(CompletableFuture.supplyAsync(() -> buildPerceptualPaletteCandidate(width, height, pixels, profile, preferences)));
            }
            for (CompletableFuture<int[]> profileFuture : profileFutures) {
                int[] quantizedPixels = joinPerceptualCandidate(profileFuture);
                if (quantizedPixels != null) {
                    addPixelCandidate(pixelCandidates, quantizedPixels, false);
                }
            }
        } else {
            for (PerceptualProfile profile : perceptualProfiles) {
                int[] quantizedPixels = buildPerceptualPaletteCandidate(width, height, pixels, profile, preferences);
                if (quantizedPixels != null) {
                    addPixelCandidate(pixelCandidates, quantizedPixels, false);
                }
            }
        }
        return pixelCandidates;
    }

    @NotNull
    protected static ChannelQuantizationProfile[] selectChannelQuantizationProfiles(int sampledPaletteBucketCount) {
        return (sampledPaletteBucketCount > 96)
                ? HIGH_COLOR_CHANNEL_QUANTIZATION_PROFILES
                : ChannelQuantizationProfile.NONE;
    }

    @Nullable
    protected static int[] buildPerceptualChannelCandidate(@NotNull int[] pixels,
                                                           @NotNull ChannelQuantizationProfile profile,
                                                           @NotNull EncodePreferences preferences) {
        int[] quantizedPixels = new int[pixels.length];
        boolean changed = false;
        for (int i = 0; i < pixels.length; i++) {
            int sourceColor = pixels[i];
            int quantizedColor = quantizeChannels(sourceColor, profile.redBits(), profile.greenBits(), profile.blueBits(), profile.alphaBits());
            quantizedPixels[i] = quantizedColor;
            changed |= quantizedColor != sourceColor;
        }
        if (!changed) {
            return null;
        }

        PerceptualErrorStats errorStats = measurePerceptualError(pixels, quantizedPixels);
        return errorStats.isWithin(preferences) ? quantizedPixels : null;
    }

    @NotNull
    protected static List<PerceptualProfile> selectPerceptualProfiles(@NotNull int[] pixels) {
        return selectPerceptualProfiles(estimateSampledPaletteBucketCount(pixels));
    }

    @NotNull
    protected static List<PerceptualProfile> selectPerceptualProfiles(int sampledPaletteBucketCount) {
        LinkedHashSet<PerceptualProfile> selectedProfiles = new LinkedHashSet<>(MAX_SELECTED_PERCEPTUAL_PROFILES);
        selectedProfiles.add(PerceptualProfile.LIGHT);
        selectedProfiles.add(PerceptualProfile.MEDIUM);

        if (sampledPaletteBucketCount <= 16) {
            selectedProfiles.add(PerceptualProfile.PALETTE_4);
            selectedProfiles.add(PerceptualProfile.PALETTE_8);
            selectedProfiles.add(PerceptualProfile.PALETTE_16);
        } else if (sampledPaletteBucketCount <= 48) {
            selectedProfiles.add(PerceptualProfile.PALETTE_8);
            selectedProfiles.add(PerceptualProfile.PALETTE_16);
            selectedProfiles.add(PerceptualProfile.PALETTE_32);
        } else if (sampledPaletteBucketCount <= 96) {
            selectedProfiles.add(PerceptualProfile.PALETTE_16);
            selectedProfiles.add(PerceptualProfile.PALETTE_32);
            selectedProfiles.add(PerceptualProfile.PALETTE_64);
        } else {
            selectedProfiles.add(PerceptualProfile.PALETTE_32);
            selectedProfiles.add(PerceptualProfile.PALETTE_64);
        }

        if (selectedProfiles.size() > MAX_SELECTED_PERCEPTUAL_PROFILES) {
            ArrayList<PerceptualProfile> limitedProfiles = new ArrayList<>(MAX_SELECTED_PERCEPTUAL_PROFILES);
            for (PerceptualProfile profile : selectedProfiles) {
                limitedProfiles.add(profile);
                if (limitedProfiles.size() >= MAX_SELECTED_PERCEPTUAL_PROFILES) {
                    break;
                }
            }
            return List.copyOf(limitedProfiles);
        }
        return List.copyOf(selectedProfiles);
    }

    protected static int estimateSampledPaletteBucketCount(@NotNull int[] pixels) {
        int sampleStep = Math.max(1, pixels.length / PERCEPTUAL_PROFILE_SAMPLE_PIXELS);
        HashMap<Integer, Boolean> sampledBuckets = new HashMap<>();
        for (int pixelIndex = 0; pixelIndex < pixels.length; pixelIndex += sampleStep) {
            int color = pixels[pixelIndex];
            int sampledBucket = ((color >>> 28) << 12)
                    | (((color >>> 20) & 0xF) << 8)
                    | (((color >>> 12) & 0xF) << 4)
                    | ((color >>> 4) & 0xF);
            sampledBuckets.put(sampledBucket, Boolean.TRUE);
            if (sampledBuckets.size() > 128) {
                return sampledBuckets.size();
            }
        }
        return sampledBuckets.size();
    }

    protected static boolean shouldParallelizePerceptualCandidates(int width, int height, int profileCount) {
        return (profileCount > 1)
                && ((long) width * (long) height >= MIN_PARALLEL_PERCEPTUAL_CANDIDATE_PIXELS)
                && (Runtime.getRuntime().availableProcessors() > 1);
    }

    @Nullable
    protected static int[] joinPerceptualCandidate(@NotNull CompletableFuture<int[]> profileFuture) {
        try {
            return profileFuture.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Failed to build an AFMA BIN_INTRA perceptual pixel candidate", cause);
        }
    }

    @NotNull
    protected static List<PixelCandidateContext> shortlistPixelCandidates(int width, int height,
                                                                          @NotNull List<PixelCandidateContext> pixelCandidates) throws IOException {
        if (pixelCandidates.size() <= MAX_SHORTLISTED_PIXEL_CANDIDATES) {
            return pixelCandidates;
        }

        int pixelCount = width * height;
        if (pixelCount < MIN_PIXEL_CANDIDATE_SHORTLIST_PIXELS) {
            return pixelCandidates;
        }

        ArrayList<EstimatedPixelCandidate> estimatedCandidates = new ArrayList<>(pixelCandidates.size());
        for (PixelCandidateContext pixelCandidate : pixelCandidates) {
            EstimatedPayloadCandidate estimatedCandidate = estimateBestCandidate(width, height, pixelCandidate);
            if (estimatedCandidate != null) {
                estimatedCandidates.add(new EstimatedPixelCandidate(pixelCandidate, estimatedCandidate));
            }
        }
        if (estimatedCandidates.size() <= MAX_SHORTLISTED_PIXEL_CANDIDATES) {
            return pixelCandidates;
        }

        estimatedCandidates.sort((first, second) -> {
            EstimatedPayloadCandidate firstEstimated = first.estimatedCandidate();
            EstimatedPayloadCandidate secondEstimated = second.estimatedCandidate();
            if (firstEstimated.estimatedArchiveBytes() != secondEstimated.estimatedArchiveBytes()) {
                return Long.compare(firstEstimated.estimatedArchiveBytes(), secondEstimated.estimatedArchiveBytes());
            }
            if (firstEstimated.stabilityPriority() != secondEstimated.stabilityPriority()) {
                return Integer.compare(firstEstimated.stabilityPriority(), secondEstimated.stabilityPriority());
            }
            if (first.pixelCandidate().lossless() != second.pixelCandidate().lossless()) {
                return first.pixelCandidate().lossless() ? -1 : 1;
            }
            return Integer.compare(
                    System.identityHashCode(first.pixelCandidate()),
                    System.identityHashCode(second.pixelCandidate())
            );
        });

        LinkedHashSet<PixelCandidateContext> shortlistedCandidates = new LinkedHashSet<>(MAX_SHORTLISTED_PIXEL_CANDIDATES);
        shortlistedCandidates.add(pixelCandidates.getFirst());
        for (EstimatedPixelCandidate estimatedCandidate : estimatedCandidates) {
            shortlistedCandidates.add(estimatedCandidate.pixelCandidate());
            if (shortlistedCandidates.size() >= MAX_SHORTLISTED_PIXEL_CANDIDATES) {
                break;
            }
        }
        if (shortlistedCandidates.size() >= pixelCandidates.size()) {
            return pixelCandidates;
        }
        return List.copyOf(shortlistedCandidates);
    }

    @NotNull
    protected static PixelCandidateContext addPixelCandidate(@NotNull List<PixelCandidateContext> pixelCandidates, @NotNull int[] pixels, boolean lossless) {
        for (int i = 0; i < pixelCandidates.size(); i++) {
            PixelCandidateContext existingCandidate = pixelCandidates.get(i);
            if (!Arrays.equals(existingCandidate.pixels(), pixels)) {
                continue;
            }
            if (lossless && !existingCandidate.lossless()) {
                PixelCandidateContext upgradedCandidate = new PixelCandidateContext(existingCandidate.pixels(), true);
                pixelCandidates.set(i, upgradedCandidate);
                return upgradedCandidate;
            }
            return existingCandidate;
        }
        PixelCandidateContext candidate = new PixelCandidateContext(pixels, lossless);
        pixelCandidates.add(candidate);
        return candidate;
    }

    @NotNull
    protected static int[] normalizeHiddenTransparentPixels(@NotNull int[] pixels) {
        int[] normalizedPixels = null;
        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            if ((color >>> 24) != 0) {
                continue;
            }
            if ((color & 0x00FFFFFF) == 0) {
                continue;
            }
            if (normalizedPixels == null) {
                normalizedPixels = Arrays.copyOf(pixels, pixels.length);
            }
            normalizedPixels[i] = 0;
        }
        return (normalizedPixels != null) ? normalizedPixels : pixels;
    }

    @Nullable
    protected static int[] buildPerceptualPaletteCandidate(int width, int height, @NotNull int[] pixels,
                                                           @NotNull PerceptualProfile profile, @NotNull EncodePreferences preferences) {
        Map<Integer, BucketAccumulator> buckets = new HashMap<>();
        int[] bucketColors = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int sourceColor = pixels[i];
            int bucketColor = quantizeBucketColor(sourceColor, profile);
            bucketColors[i] = bucketColor;
            BucketAccumulator accumulator = buckets.computeIfAbsent(bucketColor, ignored -> new BucketAccumulator(bucketColor));
            accumulator.add(normalizeColorForRepresentative(sourceColor, profile));
        }

        if (buckets.isEmpty()) {
            return null;
        }

        List<BucketAccumulator> sortedBuckets = new ArrayList<>(buckets.values());
        sortedBuckets.sort((first, second) -> {
            int countCompare = Integer.compare(second.count, first.count);
            if (countCompare != 0) {
                return countCompare;
            }
            return Integer.compareUnsigned(first.bucketColor, second.bucketColor);
        });

        int paletteSize = Math.min(profile.maxPaletteColors(), sortedBuckets.size());
        BucketAccumulator[] paletteBuckets = new BucketAccumulator[paletteSize];
        Map<Integer, Integer> paletteMapping = new HashMap<>(sortedBuckets.size() * 2);
        for (int i = 0; i < paletteSize; i++) {
            BucketAccumulator accumulator = sortedBuckets.get(i);
            paletteBuckets[i] = accumulator;
            paletteMapping.put(accumulator.bucketColor, i);
        }

        for (int i = paletteSize; i < sortedBuckets.size(); i++) {
            BucketAccumulator accumulator = sortedBuckets.get(i);
            paletteMapping.put(accumulator.bucketColor, findNearestPaletteBucket(accumulator.representativeColor(), paletteBuckets));
        }

        int[] paletteColors = new int[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            paletteColors[i] = paletteBuckets[i].representativeColor();
        }

        int[] quantizedPixels = materializePerceptualPaletteCandidate(bucketColors, paletteMapping, paletteColors);
        if (quantizedPixels == null) {
            return null;
        }
        PerceptualErrorStats errorStats = measurePerceptualError(pixels, quantizedPixels);
        if (errorStats.isWithin(preferences)) {
            return quantizedPixels;
        }

        if (!shouldRefinePerceptualPalette(sortedBuckets.size(), paletteSize)) {
            return null;
        }

        RefinedPerceptualPalette refinedPalette = refinePerceptualPalette(sortedBuckets, paletteColors);
        if (refinedPalette == null) {
            return null;
        }
        quantizedPixels = materializePerceptualPaletteCandidate(bucketColors, refinedPalette.paletteMapping(), refinedPalette.paletteColors());
        if (quantizedPixels == null) {
            return null;
        }
        errorStats = measurePerceptualError(pixels, quantizedPixels);
        return errorStats.isWithin(preferences) ? quantizedPixels : null;
    }

    protected static boolean shouldRefinePerceptualPalette(int bucketCount, int paletteSize) {
        return (paletteSize >= PERCEPTUAL_PALETTE_REFINEMENT_MIN_PALETTE_COLORS)
                && (bucketCount >= (paletteSize + PERCEPTUAL_PALETTE_REFINEMENT_MIN_EXTRA_BUCKETS));
    }

    @Nullable
    protected static int[] materializePerceptualPaletteCandidate(@NotNull int[] bucketColors,
                                                                 @NotNull Map<Integer, Integer> paletteMapping,
                                                                 @NotNull int[] paletteColors) {
        int[] quantizedPixels = new int[bucketColors.length];
        for (int i = 0; i < bucketColors.length; i++) {
            Integer paletteIndex = paletteMapping.get(bucketColors[i]);
            if ((paletteIndex == null) || (paletteIndex < 0) || (paletteIndex >= paletteColors.length)) {
                return null;
            }
            quantizedPixels[i] = paletteColors[paletteIndex];
        }
        return quantizedPixels;
    }

    @Nullable
    protected static RefinedPerceptualPalette refinePerceptualPalette(@NotNull List<BucketAccumulator> sortedBuckets,
                                                                      @NotNull int[] initialPaletteColors) {
        int paletteSize = initialPaletteColors.length;
        if (paletteSize <= 0) {
            return null;
        }

        int[] paletteColors = Arrays.copyOf(initialPaletteColors, paletteSize);
        int[] assignments = new int[sortedBuckets.size()];
        Arrays.fill(assignments, -1);

        for (int iteration = 0; iteration < PERCEPTUAL_PALETTE_REFINEMENT_MAX_ITERATIONS; iteration++) {
            long[] redSums = new long[paletteSize];
            long[] greenSums = new long[paletteSize];
            long[] blueSums = new long[paletteSize];
            long[] alphaSums = new long[paletteSize];
            long[] counts = new long[paletteSize];
            boolean assignmentsChanged = false;

            for (int bucketIndex = 0; bucketIndex < sortedBuckets.size(); bucketIndex++) {
                BucketAccumulator bucket = sortedBuckets.get(bucketIndex);
                int bestPaletteIndex = findNearestPaletteColor(bucket.representativeColor(), paletteColors);
                if (assignments[bucketIndex] != bestPaletteIndex) {
                    assignments[bucketIndex] = bestPaletteIndex;
                    assignmentsChanged = true;
                }
                redSums[bestPaletteIndex] += bucket.redSum;
                greenSums[bestPaletteIndex] += bucket.greenSum;
                blueSums[bestPaletteIndex] += bucket.blueSum;
                alphaSums[bestPaletteIndex] += bucket.alphaSum;
                counts[bestPaletteIndex] += bucket.count;
            }

            boolean paletteChanged = false;
            for (int paletteIndex = 0; paletteIndex < paletteSize; paletteIndex++) {
                if (counts[paletteIndex] <= 0L) {
                    continue;
                }
                int refinedColor = buildAverageColor(redSums[paletteIndex], greenSums[paletteIndex], blueSums[paletteIndex], alphaSums[paletteIndex], counts[paletteIndex]);
                if (paletteColors[paletteIndex] != refinedColor) {
                    paletteColors[paletteIndex] = refinedColor;
                    paletteChanged = true;
                }
            }

            if (!assignmentsChanged && !paletteChanged) {
                break;
            }
        }

        HashMap<Integer, Integer> paletteMapping = new HashMap<>(sortedBuckets.size() * 2);
        for (int bucketIndex = 0; bucketIndex < sortedBuckets.size(); bucketIndex++) {
            paletteMapping.put(sortedBuckets.get(bucketIndex).bucketColor, assignments[bucketIndex]);
        }
        return new RefinedPerceptualPalette(paletteMapping, paletteColors);
    }

    protected static int findNearestPaletteColor(int color, @NotNull int[] paletteColors) {
        int bestIndex = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < paletteColors.length; i++) {
            long distance = perceptualDistance(color, paletteColors[i]);
            if (distance >= bestDistance) {
                continue;
            }
            bestDistance = distance;
            bestIndex = i;
        }
        return bestIndex;
    }

    protected static int buildAverageColor(long redSum, long greenSum, long blueSum, long alphaSum, long count) {
        if (count <= 0L) {
            return 0;
        }
        int alpha = (int) Math.round((double) alphaSum / (double) count);
        if (alpha <= 0) {
            return 0;
        }
        int red = (int) Math.round((double) redSum / (double) count);
        int green = (int) Math.round((double) greenSum / (double) count);
        int blue = (int) Math.round((double) blueSum / (double) count);
        return (Math.min(0xFF, alpha) << 24)
                | (Math.min(0xFF, red) << 16)
                | (Math.min(0xFF, green) << 8)
                | Math.min(0xFF, blue);
    }

    protected static int quantizeBucketColor(int sourceColor, @NotNull PerceptualProfile profile) {
        int alpha = (sourceColor >>> 24) & 0xFF;
        if (alpha <= profile.transparentSnapThreshold()) {
            return 0;
        }

        int normalizedAlpha = (alpha >= (0xFF - profile.opaqueSnapThreshold()))
                ? 0xFF
                : quantizeChannel(alpha, profile.alphaBits());
        if (normalizedAlpha == 0) {
            return 0;
        }

        int red = quantizeChannel((sourceColor >> 16) & 0xFF, profile.redBits());
        int green = quantizeChannel((sourceColor >> 8) & 0xFF, profile.greenBits());
        int blue = quantizeChannel(sourceColor & 0xFF, profile.blueBits());
        return (normalizedAlpha << 24) | (red << 16) | (green << 8) | blue;
    }

    protected static int quantizeChannels(int sourceColor, int redBits, int greenBits, int blueBits, int alphaBits) {
        int alpha = quantizeChannel((sourceColor >>> 24) & 0xFF, alphaBits);
        if (alpha <= 0) {
            return 0;
        }
        int red = quantizeChannel((sourceColor >> 16) & 0xFF, redBits);
        int green = quantizeChannel((sourceColor >> 8) & 0xFF, greenBits);
        int blue = quantizeChannel(sourceColor & 0xFF, blueBits);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    protected static int normalizeColorForRepresentative(int sourceColor, @NotNull PerceptualProfile profile) {
        int alpha = (sourceColor >>> 24) & 0xFF;
        if (alpha <= profile.transparentSnapThreshold()) {
            return 0;
        }

        int normalizedAlpha = (alpha >= (0xFF - profile.opaqueSnapThreshold())) ? 0xFF : alpha;
        if (normalizedAlpha == 0) {
            return 0;
        }
        return (normalizedAlpha << 24) | (sourceColor & 0x00FFFFFF);
    }

    protected static int quantizeChannel(int value, int bits) {
        if (bits >= 8) {
            return value & 0xFF;
        }
        if (bits <= 0) {
            return 0;
        }
        int levels = (1 << bits) - 1;
        if (levels <= 0) {
            return 0;
        }
        int quantizedLevel = (int) Math.round((value & 0xFF) * (levels / 255.0D));
        return Math.min(0xFF, (int) Math.round(quantizedLevel * (255.0D / levels)));
    }

    protected static int findNearestPaletteBucket(int color, @NotNull BucketAccumulator[] paletteBuckets) {
        int bestIndex = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < paletteBuckets.length; i++) {
            long distance = perceptualDistance(color, paletteBuckets[i].representativeColor());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    @NotNull
    protected static PerceptualErrorStats measurePerceptualError(@NotNull int[] sourcePixels, @NotNull int[] candidatePixels) {
        double totalError = 0D;
        int maxVisibleColorDelta = 0;
        int maxAlphaDelta = 0;
        for (int i = 0; i < sourcePixels.length; i++) {
            int sourceColor = sourcePixels[i];
            int candidateColor = candidatePixels[i];
            int sourceAlpha = (sourceColor >>> 24) & 0xFF;
            int candidateAlpha = (candidateColor >>> 24) & 0xFF;
            int alphaDelta = Math.abs(sourceAlpha - candidateAlpha);
            if (alphaDelta > maxAlphaDelta) {
                maxAlphaDelta = alphaDelta;
            }

            int visibilityAlpha = Math.max(sourceAlpha, candidateAlpha);
            if (visibilityAlpha <= 0) {
                continue;
            }

            int redDelta = Math.abs(((sourceColor >> 16) & 0xFF) - ((candidateColor >> 16) & 0xFF));
            int greenDelta = Math.abs(((sourceColor >> 8) & 0xFF) - ((candidateColor >> 8) & 0xFF));
            int blueDelta = Math.abs((sourceColor & 0xFF) - (candidateColor & 0xFF));
            int visibleColorDelta = Math.max(redDelta, Math.max(greenDelta, blueDelta));
            if (visibleColorDelta > maxVisibleColorDelta) {
                maxVisibleColorDelta = visibleColorDelta;
            }

            double visibilityWeight = visibilityAlpha / 255.0D;
            totalError += (alphaDelta * 2.0D) + ((redDelta + greenDelta + blueDelta) * visibilityWeight);
        }
        return new PerceptualErrorStats(totalError / Math.max(1, sourcePixels.length), maxVisibleColorDelta, maxAlphaDelta);
    }

    protected static long perceptualDistance(int firstColor, int secondColor) {
        int firstAlpha = (firstColor >>> 24) & 0xFF;
        int secondAlpha = (secondColor >>> 24) & 0xFF;
        int alphaDelta = Math.abs(firstAlpha - secondAlpha);
        int visibilityAlpha = Math.max(firstAlpha, secondAlpha);
        if (visibilityAlpha <= 0) {
            return (long) alphaDelta * 4L;
        }

        int redDelta = Math.abs(((firstColor >> 16) & 0xFF) - ((secondColor >> 16) & 0xFF));
        int greenDelta = Math.abs(((firstColor >> 8) & 0xFF) - ((secondColor >> 8) & 0xFF));
        int blueDelta = Math.abs((firstColor & 0xFF) - (secondColor & 0xFF));
        long visibilityWeight = Math.max(1L, visibilityAlpha);
        return ((long) alphaDelta * 512L) + (((long) redDelta + greenDelta + blueDelta) * visibilityWeight);
    }

    protected static boolean shouldParallelizeModeSelection(int width, int height, int candidateCount) {
        long pixelCount = (long) width * height;
        if ((candidateCount <= 1) || (pixelCount < MIN_PARALLEL_MODE_SELECTION_PIXELS)) {
            return false;
        }

        int parallelTasks = Math.min(candidateCount, Math.max(1, Runtime.getRuntime().availableProcessors()));
        if (parallelTasks <= 1) {
            return false;
        }

        long estimatedAdditionalBytes = pixelCount * 12L * parallelTasks;
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        if (maxMemory <= 0L) {
            return false;
        }

        long usedMemory = Math.max(0L, runtime.totalMemory() - runtime.freeMemory());
        long headroomBytes = Math.max(0L, maxMemory - usedMemory);
        long requiredHeadroomBytes = Math.max(MIN_PARALLEL_MODE_SELECTION_HEADROOM_BYTES, estimatedAdditionalBytes);
        return headroomBytes >= requiredHeadroomBytes;
    }

    @Nullable
    protected static ScoredPayloadCandidate selectBestCandidateSequential(int width, int height,
                                                                          @NotNull List<PixelCandidateContext> pixelCandidates) throws IOException {
        ScoredPayloadCandidate bestCandidate = null;
        for (PixelCandidateContext pixelCandidate : pixelCandidates) {
            bestCandidate = pickBetterCandidate(bestCandidate, buildBestCandidate(width, height, pixelCandidate));
        }
        return bestCandidate;
    }

    @Nullable
    protected static ScoredPayloadCandidate selectBestCandidateInParallel(int width, int height,
                                                                          @NotNull List<PixelCandidateContext> pixelCandidates) throws IOException {
        // Keep parallel work coarse-grained so each task can reuse its pixel analysis across all BIN_INTRA modes.
        List<CompletableFuture<ScoredPayloadCandidate>> candidateFutures = new ArrayList<>(pixelCandidates.size());
        for (PixelCandidateContext pixelCandidate : pixelCandidates) {
            candidateFutures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return buildBestCandidate(width, height, pixelCandidate);
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }
            }));
        }

        ScoredPayloadCandidate bestCandidate = null;
        for (CompletableFuture<ScoredPayloadCandidate> candidateFuture : candidateFutures) {
            bestCandidate = pickBetterCandidate(bestCandidate, joinCandidate(candidateFuture));
        }
        return bestCandidate;
    }

    @Nullable
    protected static ScoredPayloadCandidate joinCandidate(@NotNull CompletableFuture<ScoredPayloadCandidate> candidateFuture) throws IOException {
        try {
            return candidateFuture.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException ioEx) {
                throw ioEx;
            }
            if (cause instanceof RuntimeException runtimeEx) {
                throw runtimeEx;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IOException("Failed to build an AFMA BIN_INTRA payload candidate", cause);
        }
    }

    @NotNull
    protected static FilterSelection selectBestFilter(@NotNull byte[] rawBytes, int rowOffset, int rowBytes,
                                                      @NotNull byte[] previousRow, int bytesPerPixel,
                                                      @NotNull byte[] bestRow, @NotNull byte[] candidateRow) {
        Filter bestFilter = Filter.NONE;
        long bestScore = Long.MAX_VALUE;
        for (Filter filter : Filter.values()) {
            long score = applyAndScoreFilter(filter, rawBytes, rowOffset, rowBytes, previousRow, bytesPerPixel, candidateRow, bestScore);
            if (score < bestScore) {
                bestScore = score;
                bestFilter = filter;
                System.arraycopy(candidateRow, 0, bestRow, 0, rowBytes);
            }
        }
        return new FilterSelection(bestFilter, bestScore);
    }

    protected static long applyAndScoreFilter(@NotNull Filter filter, @NotNull byte[] rawBytes, int rowOffset, int rowBytes,
                                              @NotNull byte[] previousRow, int bytesPerPixel,
                                              @NotNull byte[] output, long bestScore) {
        int prefixBytes = Math.min(rowBytes, bytesPerPixel);
        switch (filter) {
            case NONE -> {
                long score = 0L;
                for (int i = 0; i < rowBytes; i++) {
                    byte value = rawBytes[rowOffset + i];
                    output[i] = value;
                    score += Math.abs(value);
                    if (score >= bestScore) {
                        return Long.MAX_VALUE;
                    }
                }
                return score;
            }
            case SUB -> {
                long score = 0L;
                for (int i = 0; i < prefixBytes; i++) {
                    byte value = rawBytes[rowOffset + i];
                    output[i] = value;
                    score += Math.abs(value);
                    if (score >= bestScore) {
                        return Long.MAX_VALUE;
                    }
                }
                for (int i = prefixBytes; i < rowBytes; i++) {
                    byte value = (byte) ((rawBytes[rowOffset + i] & 0xFF) - (rawBytes[rowOffset + i - bytesPerPixel] & 0xFF));
                    output[i] = value;
                    score += Math.abs(value);
                    if (score >= bestScore) {
                        return Long.MAX_VALUE;
                    }
                }
                return score;
            }
            case UP -> {
                long score = 0L;
                for (int i = 0; i < rowBytes; i++) {
                    byte value = (byte) ((rawBytes[rowOffset + i] & 0xFF) - (previousRow[i] & 0xFF));
                    output[i] = value;
                    score += Math.abs(value);
                    if (score >= bestScore) {
                        return Long.MAX_VALUE;
                    }
                }
                return score;
            }
            case AVERAGE -> {
                long score = 0L;
                for (int i = 0; i < prefixBytes; i++) {
                    byte value = (byte) ((rawBytes[rowOffset + i] & 0xFF) - ((previousRow[i] & 0xFF) >>> 1));
                    output[i] = value;
                    score += Math.abs(value);
                    if (score >= bestScore) {
                        return Long.MAX_VALUE;
                    }
                }
                for (int i = prefixBytes; i < rowBytes; i++) {
                    byte value = (byte) ((rawBytes[rowOffset + i] & 0xFF)
                            - (((rawBytes[rowOffset + i - bytesPerPixel] & 0xFF) + (previousRow[i] & 0xFF)) >>> 1));
                    output[i] = value;
                    score += Math.abs(value);
                    if (score >= bestScore) {
                        return Long.MAX_VALUE;
                    }
                }
                return score;
            }
            case PAETH -> {
                long score = 0L;
                for (int i = 0; i < prefixBytes; i++) {
                    byte value = (byte) ((rawBytes[rowOffset + i] & 0xFF) - paethPredictor(0, previousRow[i] & 0xFF, 0));
                    output[i] = value;
                    score += Math.abs(value);
                    if (score >= bestScore) {
                        return Long.MAX_VALUE;
                    }
                }
                for (int i = prefixBytes; i < rowBytes; i++) {
                    byte value = (byte) ((rawBytes[rowOffset + i] & 0xFF)
                            - paethPredictor(rawBytes[rowOffset + i - bytesPerPixel] & 0xFF,
                            previousRow[i] & 0xFF,
                            previousRow[i - bytesPerPixel] & 0xFF));
                    output[i] = value;
                    score += Math.abs(value);
                    if (score >= bestScore) {
                        return Long.MAX_VALUE;
                    }
                }
                return score;
            }
        }
        return Long.MAX_VALUE;
    }

    protected static void decodeFilteredRows(@NotNull DataInputStream in, int rowBytes, int height, int bytesPerPixel,
                                             @Nullable AfmaDecodeScratch scratch, @NotNull DecodedRowConsumer consumer) throws IOException {
        if (rowBytes < 0 || height <= 0 || bytesPerPixel <= 0) {
            throw new IOException("AFMA BIN_INTRA filter row dimensions are invalid");
        }

        byte[] previousRow = (scratch != null) ? scratch.borrowPreviousRow(rowBytes) : new byte[rowBytes];
        byte[] filteredRow = (scratch != null) ? scratch.borrowFilteredRow(rowBytes) : new byte[rowBytes];
        byte[] decodedRow = (scratch != null) ? scratch.borrowDecodedRow(rowBytes) : new byte[rowBytes];
        if (scratch != null) {
            scratch.clearPreviousRow(rowBytes);
        } else if (rowBytes > 0) {
            Arrays.fill(previousRow, 0, rowBytes, (byte) 0);
        }
        for (int row = 0; row < height; row++) {
            Filter filter = Filter.byId(in.readUnsignedByte());
            in.readFully(filteredRow, 0, rowBytes);
            inverseFilter(filter, filteredRow, previousRow, bytesPerPixel, decodedRow, rowBytes);
            consumer.accept(row, decodedRow, rowBytes);
            System.arraycopy(decodedRow, 0, previousRow, 0, rowBytes);
        }
    }

    protected static void inverseFilter(@NotNull Filter filter, @NotNull byte[] filteredRow, @NotNull byte[] previousRow,
                                        int bytesPerPixel, @NotNull byte[] output, int rowBytes) {
        int prefixBytes = Math.min(rowBytes, bytesPerPixel);
        switch (filter) {
            case NONE -> System.arraycopy(filteredRow, 0, output, 0, rowBytes);
            case SUB -> {
                for (int i = 0; i < prefixBytes; i++) {
                    output[i] = filteredRow[i];
                }
                for (int i = prefixBytes; i < rowBytes; i++) {
                    output[i] = (byte) ((filteredRow[i] & 0xFF) + (output[i - bytesPerPixel] & 0xFF));
                }
            }
            case UP -> {
                for (int i = 0; i < rowBytes; i++) {
                    output[i] = (byte) ((filteredRow[i] & 0xFF) + (previousRow[i] & 0xFF));
                }
            }
            case AVERAGE -> {
                for (int i = 0; i < prefixBytes; i++) {
                    output[i] = (byte) ((filteredRow[i] & 0xFF) + ((previousRow[i] & 0xFF) >>> 1));
                }
                for (int i = prefixBytes; i < rowBytes; i++) {
                    output[i] = (byte) ((filteredRow[i] & 0xFF)
                            + (((output[i - bytesPerPixel] & 0xFF) + (previousRow[i] & 0xFF)) >>> 1));
                }
            }
            case PAETH -> {
                for (int i = 0; i < prefixBytes; i++) {
                    output[i] = (byte) ((filteredRow[i] & 0xFF) + paethPredictor(0, previousRow[i] & 0xFF, 0));
                }
                for (int i = prefixBytes; i < rowBytes; i++) {
                    output[i] = (byte) ((filteredRow[i] & 0xFF)
                            + paethPredictor(output[i - bytesPerPixel] & 0xFF,
                            previousRow[i] & 0xFF,
                            previousRow[i - bytesPerPixel] & 0xFF));
                }
            }
        }
    }

    protected static int paethPredictor(int left, int up, int upLeft) {
        int predictor = left + up - upLeft;
        int leftDistance = Math.abs(predictor - left);
        int upDistance = Math.abs(predictor - up);
        int upLeftDistance = Math.abs(predictor - upLeft);
        if ((leftDistance <= upDistance) && (leftDistance <= upLeftDistance)) {
            return left;
        }
        if (upDistance <= upLeftDistance) {
            return up;
        }
        return upLeft;
    }

    @NotNull
    protected static byte[] packIndices(@NotNull int[] indices, int width, int height, int bitsPerIndex) {
        int rowBytes = packedRowBytes(width, bitsPerIndex);
        byte[] packed = new byte[rowBytes * height];
        int indexOffset = 0;
        int rowOffset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writePackedValue(packed, rowOffset, x, bitsPerIndex, indices[indexOffset++]);
            }
            rowOffset += rowBytes;
        }
        return packed;
    }

    @NotNull
    protected static byte[] packBinaryAlpha(@NotNull byte[] alphaBytes, int width, int height) {
        int rowBytes = packedRowBytes(width, 1);
        byte[] packed = new byte[rowBytes * height];
        int alphaOffset = 0;
        int rowOffset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = alphaBytes[alphaOffset++] & 0xFF;
                writePackedValue(packed, rowOffset, x, 1, alpha >= 0x80 ? 1 : 0);
            }
            rowOffset += rowBytes;
        }
        return packed;
    }

    protected static void writePackedValue(@NotNull byte[] target, int rowOffset, int x, int bitsPerIndex, int value) {
        int bitOffset = x * bitsPerIndex;
        int byteIndex = rowOffset + (bitOffset >>> 3);
        int shift = 8 - bitsPerIndex - (bitOffset & 7);
        target[byteIndex] |= (byte) (value << shift);
    }

    protected static int unpackPackedValue(@NotNull byte[] source, int rowOffset, int x, int bitsPerIndex) {
        int bitOffset = x * bitsPerIndex;
        int byteIndex = rowOffset + (bitOffset >>> 3);
        int shift = 8 - bitsPerIndex - (bitOffset & 7);
        int mask = (1 << bitsPerIndex) - 1;
        return ((source[byteIndex] & 0xFF) >>> shift) & mask;
    }

    protected static int paletteBits(int paletteSize) {
        if (paletteSize <= 2) {
            return 1;
        }
        if (paletteSize <= 4) {
            return 2;
        }
        if (paletteSize <= 16) {
            return 4;
        }
        return 8;
    }

    protected static int packedRowBytes(int width, int bitsPerIndex) {
        return (width * bitsPerIndex + 7) >>> 3;
    }

    protected static void validatePaletteBits(int bitsPerIndex) throws IOException {
        if ((bitsPerIndex != 1) && (bitsPerIndex != 2) && (bitsPerIndex != 4) && (bitsPerIndex != 8)) {
            throw new IOException("AFMA BIN_INTRA indexed payload bits-per-index is invalid");
        }
    }

    protected static void validateDimensions(int width, int height) {
        if ((width <= 0) || (height <= 0) || (width > 0xFFFF) || (height > 0xFFFF)) {
            throw new IllegalArgumentException("AFMA BIN_INTRA payload dimensions are invalid");
        }
    }

    protected static void validateDecodedDimensions(int width, int height) throws IOException {
        if ((width <= 0) || (height <= 0) || (width > 0xFFFF) || (height > 0xFFFF)) {
            throw new IOException("AFMA BIN_INTRA payload dimensions are invalid");
        }
    }

    protected static void validatePixelBuffer(int width, int height, @NotNull int[] pixels) {
        Objects.requireNonNull(pixels);
        long expectedPixels = (long) width * (long) height;
        if ((expectedPixels <= 0L) || (expectedPixels > Integer.MAX_VALUE) || (pixels.length != (int) expectedPixels)) {
            throw new IllegalArgumentException("AFMA BIN_INTRA pixel buffer size does not match dimensions");
        }
    }

    protected static void validatePixelBufferView(int width, int height, @NotNull int[] pixels, int offset, int scanlineStride) {
        Objects.requireNonNull(pixels);
        if (offset < 0 || scanlineStride < width) {
            throw new IllegalArgumentException("AFMA BIN_INTRA pixel buffer view is invalid");
        }

        long expectedPixels = (long) width * (long) height;
        if ((expectedPixels <= 0L) || (expectedPixels > Integer.MAX_VALUE)) {
            throw new IllegalArgumentException("AFMA BIN_INTRA pixel buffer size does not match dimensions");
        }

        long requiredLength = (long) offset + ((long) (height - 1) * (long) scanlineStride) + width;
        if (requiredLength > pixels.length) {
            throw new IllegalArgumentException("AFMA BIN_INTRA pixel buffer view does not contain the requested region");
        }
    }

    public record EncodePreferences(boolean perceptualCandidatesEnabled, int maxVisibleColorDelta, int maxAlphaDelta, double maxAverageError) {

        @NotNull
        public static EncodePreferences lossless() {
            return new EncodePreferences(false, 0, 0, 0D);
        }

        @NotNull
        public static EncodePreferences perceptual(int maxVisibleColorDelta, int maxAlphaDelta, double maxAverageError) {
            return new EncodePreferences(true, Math.max(0, maxVisibleColorDelta), Math.max(0, maxAlphaDelta), Math.max(0D, maxAverageError));
        }
    }

    public record EncodedPayloadResult(@NotNull byte[] payloadBytes, @NotNull int[] reconstructedPixels, boolean lossless, @NotNull Mode mode) {
    }

    public record StoredEncodedPayloadResult(@NotNull AfmaStoredPayload payload, @NotNull int[] reconstructedPixels,
                                             boolean lossless, @NotNull Mode mode) implements AutoCloseable {

        @Override
        public void close() {
            this.payload.close();
        }
    }

    public record PayloadHeader(int width, int height, @NotNull Mode mode) {
    }

    public record DecodedFrame(int width, int height, @NotNull int[] pixels) {
    }

    protected record PerceptualErrorStats(double averageError, int maxVisibleColorDelta, int maxAlphaDelta) {

        protected boolean isWithin(@NotNull EncodePreferences preferences) {
            return this.averageError <= preferences.maxAverageError()
                    && this.maxVisibleColorDelta <= preferences.maxVisibleColorDelta()
                    && this.maxAlphaDelta <= preferences.maxAlphaDelta();
        }
    }

    protected static ScoredPayloadCandidate scoreCandidate(@NotNull Mode mode, @NotNull int[] reconstructedPixels,
                                                           boolean lossless, int stabilityPriority,
                                                           @NotNull PayloadWriter payloadWriter) throws IOException {
        AfmaStoredPayload.BufferedPayload bufferedPayload = capturePayload(payloadWriter);
        return new ScoredPayloadCandidate(mode, bufferedPayload.payloadBytes(), reconstructedPixels, lossless,
                bufferedPayload.payloadSummary(), stabilityPriority);
    }

    protected record ScoredPayloadCandidate(@NotNull Mode mode, @NotNull byte[] payloadBytes,
                                            @NotNull int[] reconstructedPixels, boolean lossless,
                                            @NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                            int stabilityPriority) {

        protected boolean isBetterThan(@NotNull ScoredPayloadCandidate other) {
            if (this.payloadSummary.estimatedArchiveBytes() != other.payloadSummary.estimatedArchiveBytes()) {
                return this.payloadSummary.estimatedArchiveBytes() < other.payloadSummary.estimatedArchiveBytes();
            }
            if (this.lossless != other.lossless) {
                return this.lossless;
            }
            if (this.payloadSummary.length() != other.payloadSummary.length()) {
                return this.payloadSummary.length() < other.payloadSummary.length();
            }
            if (this.stabilityPriority != other.stabilityPriority) {
                return this.stabilityPriority < other.stabilityPriority;
            }
            return this.mode.priority() < other.mode.priority();
        }
    }

    public record ScoredPayloadResult(@NotNull AfmaStoredPayload.PayloadSummary payloadSummary,
                                      @NotNull byte[] payloadBytes,
                                      @NotNull int[] reconstructedPixels,
                                      boolean lossless,
                                      @NotNull Mode mode) {

        @NotNull
        public AfmaStoredPayload materializePayload() throws IOException {
            return AfmaStoredPayload.fromBytes(this.payloadBytes);
        }
    }

    public record EstimatedPayloadResult(long estimatedArchiveBytes,
                                         long payloadLength,
                                         boolean lossless,
                                         @NotNull Mode mode) {
    }

    protected record ModePlan(@NotNull Mode mode, @NotNull ModeFamily family, long payloadLength, int stabilityPriority,
                              @NotNull ModeEstimateBuilder estimateBuilder, @NotNull ExactCandidateBuilder exactBuilder) {

        protected long estimateBytes() throws IOException {
            return this.estimateBuilder.estimateBytes();
        }

        @NotNull
        protected ScoredPayloadCandidate buildCandidate() throws IOException {
            return this.exactBuilder.buildCandidate();
        }
    }

    protected record EstimatedPayloadCandidate(@NotNull Mode mode,
                                               long estimatedArchiveBytes,
                                               long payloadLength,
                                               boolean lossless,
                                               int stabilityPriority) {

        protected boolean isBetterThan(@NotNull EstimatedPayloadCandidate other) {
            if (this.estimatedArchiveBytes != other.estimatedArchiveBytes) {
                return this.estimatedArchiveBytes < other.estimatedArchiveBytes;
            }
            if (this.lossless != other.lossless) {
                return this.lossless;
            }
            if (this.payloadLength != other.payloadLength) {
                return this.payloadLength < other.payloadLength;
            }
            if (this.stabilityPriority != other.stabilityPriority) {
                return this.stabilityPriority < other.stabilityPriority;
            }
            return this.mode.priority() < other.mode.priority();
        }
    }

    protected record EstimatedPixelCandidate(@NotNull PixelCandidateContext pixelCandidate,
                                             @NotNull EstimatedPayloadCandidate estimatedCandidate) {
    }

    protected record EstimatedModePlan(@NotNull ModePlan plan, long estimatedBytes) {
    }

    protected record RefinedPerceptualPalette(@NotNull Map<Integer, Integer> paletteMapping,
                                              @NotNull int[] paletteColors) {
    }

    protected record IndexedModeContext(@NotNull int[] palette, @NotNull Map<Integer, Integer> colorToIndex,
                                        int bitsPerIndex, int packedRowBytes) {
    }

    protected record FilterSelection(@NotNull Filter filter, long score) {
    }

    @FunctionalInterface
    protected interface DecodedRowConsumer {
        void accept(int rowIndex, @NotNull byte[] decodedRow, int rowBytes) throws IOException;
    }

    @FunctionalInterface
    protected interface ModeEstimateBuilder {
        long estimateBytes() throws IOException;
    }

    @FunctionalInterface
    protected interface ExactCandidateBuilder {
        @NotNull
        ScoredPayloadCandidate buildCandidate() throws IOException;
    }

    @FunctionalInterface
    protected interface PayloadWriter {
        void write(@NotNull DataOutputStream out) throws IOException;
    }

    @FunctionalInterface
    protected interface RowWriter {
        void fillRow(int rowIndex, @NotNull byte[] rowBuffer) throws IOException;
    }

    public enum Mode {
        SOLID(0, 0),
        INDEXED(1, 2),
        RGB_FILTERED(2, 1),
        RGBA_FILTERED(3, 4),
        RGB_PLUS_ALPHA_SPLIT(4, 3),
        COLOR_PLUS_ALPHA_MASK(5, 1),
        RGB_PLANAR_FILTERED(6, 2),
        RGBA_PLANAR_FILTERED(7, 5),
        INDEXED_COLOR_PLUS_ALPHA(8, 4);

        private final int id;
        private final int priority;

        Mode(int id, int priority) {
            this.id = id;
            this.priority = priority;
        }

        public int id() {
            return this.id;
        }

        public int priority() {
            return this.priority;
        }

        @NotNull
        public static Mode byId(int id) throws IOException {
            for (Mode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            throw new IOException("Unsupported AFMA BIN_INTRA payload mode: " + id);
        }
    }

    protected enum Filter {
        NONE(0),
        SUB(1),
        UP(2),
        AVERAGE(3),
        PAETH(4);

        private final int id;

        Filter(int id) {
            this.id = id;
        }

        public int id() {
            return this.id;
        }

        @NotNull
        public static Filter byId(int id) throws IOException {
            for (Filter filter : values()) {
                if (filter.id == id) {
                    return filter;
                }
            }
            throw new IOException("Unsupported AFMA BIN_INTRA row filter: " + id);
        }
    }

    protected enum AlphaMaskMode {
        BINARY(0),
        FULL(1);

        private final int id;

        AlphaMaskMode(int id) {
            this.id = id;
        }

        public int id() {
            return this.id;
        }

        @NotNull
        public static AlphaMaskMode byId(int id) throws IOException {
            for (AlphaMaskMode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            throw new IOException("Unsupported AFMA BIN_INTRA alpha mask mode: " + id);
        }
    }

    protected enum PaletteOrdering {
        FREQUENCY,
        STABLE_COLOR
    }

    protected enum ModeFamily {
        TRUECOLOR,
        INDEXED,
        ALPHA_SPECIAL
    }

    protected enum ChannelQuantizationProfile {
        RGB666(6, 6, 6, 8),
        RGB565(5, 6, 5, 8),
        RGB555(5, 5, 5, 8),
        RGB454(4, 5, 4, 8),
        RGB444(4, 4, 4, 8);

        private static final ChannelQuantizationProfile[] NONE = new ChannelQuantizationProfile[0];

        private final int redBits;
        private final int greenBits;
        private final int blueBits;
        private final int alphaBits;

        ChannelQuantizationProfile(int redBits, int greenBits, int blueBits, int alphaBits) {
            this.redBits = redBits;
            this.greenBits = greenBits;
            this.blueBits = blueBits;
            this.alphaBits = alphaBits;
        }

        public int redBits() {
            return this.redBits;
        }

        public int greenBits() {
            return this.greenBits;
        }

        public int blueBits() {
            return this.blueBits;
        }

        public int alphaBits() {
            return this.alphaBits;
        }
    }

    protected enum PerceptualProfile {
        LIGHT(5, 5, 5, 5, 4, 4, MAX_PALETTE_COLORS),
        MEDIUM(4, 4, 4, 4, 8, 8, MAX_PALETTE_COLORS),
        PALETTE_64(4, 4, 4, 4, 8, 8, 64),
        PALETTE_32(4, 4, 4, 4, 8, 8, 32),
        PALETTE_16(4, 4, 3, 4, 12, 12, 16),
        PALETTE_8(4, 4, 3, 4, 16, 16, 8),
        PALETTE_4(3, 3, 3, 4, 20, 20, 4);

        private final int redBits;
        private final int greenBits;
        private final int blueBits;
        private final int alphaBits;
        private final int transparentSnapThreshold;
        private final int opaqueSnapThreshold;
        private final int maxPaletteColors;

        PerceptualProfile(int redBits, int greenBits, int blueBits, int alphaBits,
                          int transparentSnapThreshold, int opaqueSnapThreshold,
                          int maxPaletteColors) {
            this.redBits = redBits;
            this.greenBits = greenBits;
            this.blueBits = blueBits;
            this.alphaBits = alphaBits;
            this.transparentSnapThreshold = transparentSnapThreshold;
            this.opaqueSnapThreshold = opaqueSnapThreshold;
            this.maxPaletteColors = maxPaletteColors;
        }

        public int redBits() {
            return this.redBits;
        }

        public int greenBits() {
            return this.greenBits;
        }

        public int blueBits() {
            return this.blueBits;
        }

        public int alphaBits() {
            return this.alphaBits;
        }

        public int transparentSnapThreshold() {
            return this.transparentSnapThreshold;
        }

        public int opaqueSnapThreshold() {
            return this.opaqueSnapThreshold;
        }

        public int maxPaletteColors() {
            return this.maxPaletteColors;
        }
    }

    protected static final class BucketAccumulator {

        private final int bucketColor;
        private int count;
        private long redSum;
        private long greenSum;
        private long blueSum;
        private long alphaSum;

        protected BucketAccumulator(int bucketColor) {
            this.bucketColor = bucketColor;
        }

        protected void add(int color) {
            this.count++;
            this.redSum += (color >> 16) & 0xFF;
            this.greenSum += (color >> 8) & 0xFF;
            this.blueSum += color & 0xFF;
            this.alphaSum += (color >>> 24) & 0xFF;
        }

        protected int representativeColor() {
            if (this.count <= 0) {
                return this.bucketColor;
            }

            int alpha = (int) Math.round((double) this.alphaSum / this.count);
            if (alpha <= 0) {
                return 0;
            }

            int red = (int) Math.round((double) this.redSum / this.count);
            int green = (int) Math.round((double) this.greenSum / this.count);
            int blue = (int) Math.round((double) this.blueSum / this.count);
            return (Math.min(0xFF, alpha) << 24)
                    | (Math.min(0xFF, red) << 16)
                    | (Math.min(0xFF, green) << 8)
                    | Math.min(0xFF, blue);
        }
    }

    protected static final class PixelCandidateContext {

        @NotNull
        protected final int[] pixels;
        protected final boolean lossless;
        protected boolean scanSummaryComputed;
        protected int firstColor;
        protected boolean solidColor;
        protected boolean uniformRgb;
        protected boolean hasAlpha;
        protected boolean binaryAlpha;
        protected boolean alphaVaries;
        protected boolean hiddenTransparentRgb;
        protected boolean fullColorCountsComputed;
        @Nullable
        protected Map<Integer, Integer> fullColorCounts;
        protected boolean rgbColorCountsComputed;
        @Nullable
        protected Map<Integer, Integer> rgbColorCounts;

        protected PixelCandidateContext(@NotNull int[] pixels, boolean lossless) {
            this.pixels = pixels;
            this.lossless = lossless;
        }

        @NotNull
        public int[] pixels() {
            return this.pixels;
        }

        public boolean lossless() {
            return this.lossless;
        }

        public int firstColor() {
            this.ensureScanSummary();
            return this.firstColor;
        }

        public boolean isSolidColor() {
            this.ensureScanSummary();
            return this.solidColor;
        }

        public boolean hasUniformRgb() {
            this.ensureScanSummary();
            return this.uniformRgb;
        }

        public int uniformRgbColor() {
            this.ensureScanSummary();
            return this.firstColor & 0x00FFFFFF;
        }

        public boolean hasAlpha() {
            this.ensureScanSummary();
            return this.hasAlpha;
        }

        public boolean binaryAlpha() {
            this.ensureScanSummary();
            return this.binaryAlpha;
        }

        public boolean alphaVaries() {
            this.ensureScanSummary();
            return this.alphaVaries;
        }

        public boolean hasHiddenTransparentRgb() {
            this.ensureScanSummary();
            return this.hiddenTransparentRgb;
        }

        @Nullable
        public Map<Integer, Integer> fullColorCounts() {
            if (this.fullColorCountsComputed) {
                return this.fullColorCounts;
            }
            this.fullColorCounts = collectColorCounts(this.pixels, false);
            this.fullColorCountsComputed = true;
            return this.fullColorCounts;
        }

        @Nullable
        public Map<Integer, Integer> rgbColorCounts() {
            if (this.rgbColorCountsComputed) {
                return this.rgbColorCounts;
            }
            this.rgbColorCounts = collectColorCounts(this.pixels, true);
            this.rgbColorCountsComputed = true;
            return this.rgbColorCounts;
        }

        protected void ensureScanSummary() {
            if (this.scanSummaryComputed) {
                return;
            }

            int[] candidatePixels = this.pixels;
            int firstCandidateColor = candidatePixels[0];
            int firstAlpha = (firstCandidateColor >>> 24) & 0xFF;
            int firstRgb = firstCandidateColor & 0x00FFFFFF;
            boolean solidCandidate = true;
            boolean uniformRgbCandidate = true;
            boolean hasAlphaCandidate = firstAlpha != 0xFF;
            boolean binaryAlphaCandidate = (firstAlpha == 0) || (firstAlpha == 0xFF);
            boolean alphaVariesCandidate = false;
            boolean hiddenTransparentRgbCandidate = (firstAlpha == 0) && (firstRgb != 0);

            for (int i = 1; i < candidatePixels.length; i++) {
                int color = candidatePixels[i];
                int alpha = (color >>> 24) & 0xFF;
                int rgb = color & 0x00FFFFFF;
                if (color != firstCandidateColor) {
                    solidCandidate = false;
                }
                if (rgb != firstRgb) {
                    uniformRgbCandidate = false;
                }
                if (alpha != firstAlpha) {
                    alphaVariesCandidate = true;
                }
                if (alpha != 0xFF) {
                    hasAlphaCandidate = true;
                }
                if ((alpha != 0) && (alpha != 0xFF)) {
                    binaryAlphaCandidate = false;
                }
                if ((alpha == 0) && (rgb != 0)) {
                    hiddenTransparentRgbCandidate = true;
                }
            }

            this.firstColor = firstCandidateColor;
            this.solidColor = solidCandidate;
            this.uniformRgb = uniformRgbCandidate;
            this.hasAlpha = hasAlphaCandidate;
            this.binaryAlpha = binaryAlphaCandidate;
            this.alphaVaries = alphaVariesCandidate;
            this.hiddenTransparentRgb = hiddenTransparentRgbCandidate;
            this.scanSummaryComputed = true;
        }

        @Nullable
        protected static Map<Integer, Integer> collectColorCounts(@NotNull int[] pixels, boolean ignoreAlpha) {
            Map<Integer, Integer> colorCounts = new HashMap<>(Math.min(MAX_PALETTE_COLORS, Math.max(4, pixels.length)) * 2);
            for (int color : pixels) {
                int colorKey = ignoreAlpha ? (color & 0x00FFFFFF) : color;
                colorCounts.merge(colorKey, 1, Integer::sum);
                if (colorCounts.size() > MAX_PALETTE_COLORS) {
                    return null;
                }
            }
            return colorCounts;
        }
    }

}
