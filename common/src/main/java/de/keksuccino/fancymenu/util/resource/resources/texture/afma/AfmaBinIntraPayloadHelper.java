package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
        List<PixelCandidateContext> pixelCandidates = collectPixelCandidates(width, height, sourcePixels, normalizedPreferences);
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

        int[] pixels = pixelCandidate.pixels();
        List<Map.Entry<Integer, Integer>> paletteEntries = new ArrayList<>(colorCounts.entrySet());
        sortPaletteEntries(paletteEntries, paletteOrdering);

        int[] palette = new int[paletteEntries.size()];
        Map<Integer, Integer> colorToIndex = new HashMap<>(paletteEntries.size() * 2);
        boolean paletteHasAlpha = pixelCandidate.hasAlpha();
        for (int i = 0; i < paletteEntries.size(); i++) {
            int color = paletteEntries.get(i).getKey();
            palette[i] = color;
            colorToIndex.put(color, i);
        }

        int bitsPerIndex = paletteBits(palette.length);
        for (int i = 0; i < pixels.length; i++) {
            Integer paletteIndex = colorToIndex.get(pixels[i]);
            if (paletteIndex == null) {
                throw new IOException("AFMA BIN_INTRA palette index lookup failed");
            }
        }

        int packedRowBytes = packedRowBytes(width, bitsPerIndex);
        return scoreCandidate(Mode.INDEXED, pixelCandidate.pixels(), pixelCandidate.lossless(),
                paletteOrdering == PaletteOrdering.STABLE_COLOR ? 0 : 1, out -> {
            writeHeader(out, Mode.INDEXED, width, height);
            out.writeByte(paletteHasAlpha ? 1 : 0);
            out.writeByte(bitsPerIndex);
            out.writeShort(palette.length);
            for (int color : palette) {
                out.writeByte((color >> 16) & 0xFF);
                out.writeByte((color >> 8) & 0xFF);
                out.writeByte(color & 0xFF);
                if (paletteHasAlpha) {
                    out.writeByte((color >>> 24) & 0xFF);
                }
            }
            writeFilteredRows(out, packedRowBytes, height, 1, (rowIndex, rowBuffer) -> fillPaletteIndexRow(
                    pixels,
                    width,
                    rowIndex,
                    bitsPerIndex,
                    colorToIndex,
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

        int[] pixels = pixelCandidate.pixels();
        List<Map.Entry<Integer, Integer>> paletteEntries = new ArrayList<>(rgbCounts.entrySet());
        sortPaletteEntries(paletteEntries, paletteOrdering);

        int[] palette = new int[paletteEntries.size()];
        Map<Integer, Integer> colorToIndex = new HashMap<>(paletteEntries.size() * 2);
        for (int i = 0; i < paletteEntries.size(); i++) {
            int rgb = paletteEntries.get(i).getKey() & 0x00FFFFFF;
            palette[i] = rgb;
            colorToIndex.put(rgb, i);
        }

        int bitsPerIndex = paletteBits(palette.length);
        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            Integer paletteIndex = colorToIndex.get(color & 0x00FFFFFF);
            if (paletteIndex == null) {
                throw new IOException("AFMA BIN_INTRA indexed-color palette index lookup failed");
            }
        }

        int packedRowBytes = packedRowBytes(width, bitsPerIndex);
        int alphaMode;
        if (pixelCandidate.binaryAlpha()) {
            alphaMode = AlphaMaskMode.BINARY.id();
        } else {
            alphaMode = AlphaMaskMode.FULL.id();
        }

        return scoreCandidate(Mode.INDEXED_COLOR_PLUS_ALPHA, pixelCandidate.pixels(), pixelCandidate.lossless(),
                paletteOrdering == PaletteOrdering.STABLE_COLOR ? 0 : 1, out -> {
            writeHeader(out, Mode.INDEXED_COLOR_PLUS_ALPHA, width, height);
            out.writeByte(alphaMode);
            out.writeByte(bitsPerIndex);
            out.writeShort(palette.length);
            for (int rgb : palette) {
                out.writeByte((rgb >> 16) & 0xFF);
                out.writeByte((rgb >> 8) & 0xFF);
                out.writeByte(rgb & 0xFF);
            }
            writeFilteredRows(out, packedRowBytes, height, 1, (rowIndex, rowBuffer) -> fillPaletteIndexRow(
                    pixels,
                    width,
                    rowIndex,
                    bitsPerIndex,
                    colorToIndex,
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
        for (PerceptualProfile profile : PerceptualProfile.values()) {
            int[] quantizedPixels = buildPerceptualPaletteCandidate(width, height, pixels, profile, preferences);
            if (quantizedPixels != null) {
                addPixelCandidate(pixelCandidates, quantizedPixels, false);
            }
        }
        return pixelCandidates;
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
        BucketAccumulator[] paletteBuckets = new BucketAccumulator[0];
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

        int paletteSize = Math.min(MAX_PALETTE_COLORS, sortedBuckets.size());
        paletteBuckets = new BucketAccumulator[paletteSize];
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

        int[] quantizedPixels = new int[pixels.length];
        for (int i = 0; i < bucketColors.length; i++) {
            Integer paletteIndex = paletteMapping.get(bucketColors[i]);
            if (paletteIndex == null) {
                return null;
            }
            quantizedPixels[i] = paletteBuckets[paletteIndex].representativeColor();
        }

        PerceptualErrorStats errorStats = measurePerceptualError(pixels, quantizedPixels);
        if (!errorStats.isWithin(preferences)) {
            return null;
        }
        return quantizedPixels;
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
            applyFilter(filter, rawBytes, rowOffset, rowBytes, previousRow, bytesPerPixel, candidateRow);
            long score = scoreFilteredRow(candidateRow, rowBytes);
            if (score < bestScore) {
                bestScore = score;
                bestFilter = filter;
                System.arraycopy(candidateRow, 0, bestRow, 0, rowBytes);
            }
        }
        return new FilterSelection(bestFilter, bestScore);
    }

    protected static void applyFilter(@NotNull Filter filter, @NotNull byte[] rawBytes, int rowOffset, int rowBytes,
                                      @NotNull byte[] previousRow, int bytesPerPixel, @NotNull byte[] output) {
        int prefixBytes = Math.min(rowBytes, bytesPerPixel);
        switch (filter) {
            case NONE -> System.arraycopy(rawBytes, rowOffset, output, 0, rowBytes);
            case SUB -> {
                for (int i = 0; i < prefixBytes; i++) {
                    output[i] = rawBytes[rowOffset + i];
                }
                for (int i = prefixBytes; i < rowBytes; i++) {
                    output[i] = (byte) ((rawBytes[rowOffset + i] & 0xFF) - (rawBytes[rowOffset + i - bytesPerPixel] & 0xFF));
                }
            }
            case UP -> {
                for (int i = 0; i < rowBytes; i++) {
                    output[i] = (byte) ((rawBytes[rowOffset + i] & 0xFF) - (previousRow[i] & 0xFF));
                }
            }
            case AVERAGE -> {
                for (int i = 0; i < prefixBytes; i++) {
                    output[i] = (byte) ((rawBytes[rowOffset + i] & 0xFF) - ((previousRow[i] & 0xFF) >>> 1));
                }
                for (int i = prefixBytes; i < rowBytes; i++) {
                    output[i] = (byte) ((rawBytes[rowOffset + i] & 0xFF)
                            - (((rawBytes[rowOffset + i - bytesPerPixel] & 0xFF) + (previousRow[i] & 0xFF)) >>> 1));
                }
            }
            case PAETH -> {
                for (int i = 0; i < prefixBytes; i++) {
                    output[i] = (byte) ((rawBytes[rowOffset + i] & 0xFF) - paethPredictor(0, previousRow[i] & 0xFF, 0));
                }
                for (int i = prefixBytes; i < rowBytes; i++) {
                    output[i] = (byte) ((rawBytes[rowOffset + i] & 0xFF)
                            - paethPredictor(rawBytes[rowOffset + i - bytesPerPixel] & 0xFF,
                            previousRow[i] & 0xFF,
                            previousRow[i - bytesPerPixel] & 0xFF));
                }
            }
        }
    }

    protected static long scoreFilteredRow(@NotNull byte[] filteredRow, int rowBytes) {
        long score = 0L;
        for (int i = 0; i < rowBytes; i++) {
            score += Math.abs(filteredRow[i]);
        }
        return score;
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

    protected record FilterSelection(@NotNull Filter filter, long score) {
    }

    @FunctionalInterface
    protected interface DecodedRowConsumer {
        void accept(int rowIndex, @NotNull byte[] decodedRow, int rowBytes) throws IOException;
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

    protected enum PerceptualProfile {
        LIGHT(5, 5, 5, 5, 4, 4),
        MEDIUM(4, 4, 4, 4, 8, 8);

        private final int redBits;
        private final int greenBits;
        private final int blueBits;
        private final int alphaBits;
        private final int transparentSnapThreshold;
        private final int opaqueSnapThreshold;

        PerceptualProfile(int redBits, int greenBits, int blueBits, int alphaBits,
                          int transparentSnapThreshold, int opaqueSnapThreshold) {
            this.redBits = redBits;
            this.greenBits = greenBits;
            this.blueBits = blueBits;
            this.alphaBits = alphaBits;
            this.transparentSnapThreshold = transparentSnapThreshold;
            this.opaqueSnapThreshold = opaqueSnapThreshold;
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
