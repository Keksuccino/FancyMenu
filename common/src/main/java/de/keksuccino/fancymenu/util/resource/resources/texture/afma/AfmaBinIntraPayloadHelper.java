package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    public static final int PAYLOAD_VERSION = 1;
    public static final int MAX_PALETTE_COLORS = 256;
    public static final int RGB_CHANNELS = 3;
    public static final int RGBA_CHANNELS = 4;
    protected static final int MIN_PARALLEL_MODE_SELECTION_PIXELS = 65536;

    private AfmaBinIntraPayloadHelper() {
    }

    @NotNull
    public static byte[] encodePayload(int width, int height, @NotNull int[] pixels) throws IOException {
        return encodePayloadDetailed(width, height, pixels).payloadBytes();
    }

    @NotNull
    public static EncodedPayloadResult encodePayloadDetailed(int width, int height, @NotNull int[] pixels) throws IOException {
        return encodePayloadDetailed(width, height, pixels, EncodePreferences.lossless());
    }

    @NotNull
    public static EncodedPayloadResult encodePayloadDetailed(int width, int height, @NotNull int[] pixels, @Nullable EncodePreferences preferences) throws IOException {
        validateDimensions(width, height);
        validatePixelBuffer(width, height, pixels);

        EncodePreferences normalizedPreferences = (preferences != null) ? preferences : EncodePreferences.lossless();
        List<PixelCandidate> pixelCandidates = collectPixelCandidates(width, height, pixels, normalizedPreferences);
        List<PayloadCandidateBuilder> candidateBuilders = new ArrayList<>(pixelCandidates.size() * 5);
        for (PixelCandidate pixelCandidate : pixelCandidates) {
            addCandidateBuilders(candidateBuilders, width, height, pixelCandidate);
        }

        List<EncodedPayloadCandidate> candidates = shouldParallelizeModeSelection(width, height, candidateBuilders.size())
                ? buildCandidatesInParallel(candidateBuilders)
                : buildCandidatesSequential(candidateBuilders);

        EncodedPayloadCandidate bestCandidate = null;
        for (EncodedPayloadCandidate candidate : candidates) {
            if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate)) {
                bestCandidate = candidate;
            }
        }

        if (bestCandidate == null) {
            throw new IOException("Failed to build an AFMA BIN_INTRA payload");
        }
        return new EncodedPayloadResult(bestCandidate.payloadBytes(), bestCandidate.reconstructedPixels(), bestCandidate.lossless(), bestCandidate.mode());
    }

    @NotNull
    public static DecodedFrame decodePayload(@NotNull byte[] payloadBytes) throws IOException {
        return decodePayload(payloadBytes, 0, Objects.requireNonNull(payloadBytes).length);
    }

    @NotNull
    public static DecodedFrame decodePayload(@NotNull byte[] payloadBytes, int offset, int length) throws IOException {
        Objects.requireNonNull(payloadBytes);
        if (offset < 0 || length < 0 || ((long) offset + (long) length) > payloadBytes.length) {
            throw new IOException("AFMA BIN_INTRA payload slice is invalid");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payloadBytes, offset, length))) {
            PayloadHeader header = readHeader(in);
            int[] pixels = switch (header.mode()) {
                case SOLID -> decodeSolid(in, header.width(), header.height());
                case INDEXED -> decodeIndexed(in, header.width(), header.height());
                case RGB_FILTERED -> decodeFilteredTruecolor(in, header.width(), header.height(), RGB_CHANNELS);
                case RGBA_FILTERED -> decodeFilteredTruecolor(in, header.width(), header.height(), RGBA_CHANNELS);
                case RGB_PLUS_ALPHA_SPLIT -> decodeSplitAlpha(in, header.width(), header.height());
                case COLOR_PLUS_ALPHA_MASK -> decodeColorPlusAlphaMask(in, header.width(), header.height());
            };
            if (in.available() > 0) {
                throw new IOException("AFMA BIN_INTRA payload contains trailing data");
            }
            return new DecodedFrame(header.width(), header.height(), pixels);
        }
    }

    @NotNull
    public static PayloadHeader readHeader(@NotNull byte[] payloadBytes) throws IOException {
        return readHeader(payloadBytes, 0, Objects.requireNonNull(payloadBytes).length);
    }

    @NotNull
    public static PayloadHeader readHeader(@NotNull byte[] payloadBytes, int offset, int length) throws IOException {
        Objects.requireNonNull(payloadBytes);
        if (offset < 0 || length < 0 || ((long) offset + (long) length) > payloadBytes.length) {
            throw new IOException("AFMA BIN_INTRA payload slice is invalid");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payloadBytes, offset, length))) {
            return readHeader(in);
        }
    }

    public static void validatePayload(@NotNull byte[] payloadBytes, int expectedWidth, int expectedHeight) throws IOException {
        validatePayload(payloadBytes, 0, Objects.requireNonNull(payloadBytes).length, expectedWidth, expectedHeight);
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

    @Nullable
    protected static EncodedPayloadCandidate buildSolidCandidate(int width, int height, @NotNull PixelCandidate pixelCandidate) throws IOException {
        int[] pixels = pixelCandidate.pixels();
        int firstColor = pixels[0];
        for (int i = 1; i < pixels.length; i++) {
            if (pixels[i] != firstColor) {
                return null;
            }
        }

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            writeHeader(out, Mode.SOLID, width, height);
            out.writeInt(firstColor);
            out.flush();
            return new EncodedPayloadCandidate(Mode.SOLID, byteStream.toByteArray(), pixelCandidate.pixels(), pixelCandidate.lossless());
        }
    }

    @Nullable
    protected static EncodedPayloadCandidate buildIndexedCandidate(int width, int height, @NotNull PixelCandidate pixelCandidate) throws IOException {
        int[] pixels = pixelCandidate.pixels();
        Map<Integer, Integer> colorCounts = new HashMap<>();
        for (int color : pixels) {
            colorCounts.merge(color, 1, Integer::sum);
            if (colorCounts.size() > MAX_PALETTE_COLORS) {
                return null;
            }
        }

        List<Map.Entry<Integer, Integer>> paletteEntries = new ArrayList<>(colorCounts.entrySet());
        paletteEntries.sort((first, second) -> {
            int countCompare = Integer.compare(second.getValue(), first.getValue());
            if (countCompare != 0) {
                return countCompare;
            }
            return Integer.compareUnsigned(first.getKey(), second.getKey());
        });

        int[] palette = new int[paletteEntries.size()];
        Map<Integer, Integer> colorToIndex = new HashMap<>(paletteEntries.size() * 2);
        boolean paletteHasAlpha = false;
        for (int i = 0; i < paletteEntries.size(); i++) {
            int color = paletteEntries.get(i).getKey();
            palette[i] = color;
            colorToIndex.put(color, i);
            if (((color >>> 24) & 0xFF) != 0xFF) {
                paletteHasAlpha = true;
            }
        }

        int bitsPerIndex = paletteBits(palette.length);
        int[] indices = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            Integer paletteIndex = colorToIndex.get(pixels[i]);
            if (paletteIndex == null) {
                throw new IOException("AFMA BIN_INTRA palette index lookup failed");
            }
            indices[i] = paletteIndex;
        }

        int packedRowBytes = packedRowBytes(width, bitsPerIndex);
        byte[] packedIndices = packIndices(indices, width, height, bitsPerIndex);
        byte[] filteredRows = encodeFilteredRows(packedIndices, packedRowBytes, height, 1);

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
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
            out.write(filteredRows);
            out.flush();
            return new EncodedPayloadCandidate(Mode.INDEXED, byteStream.toByteArray(), pixelCandidate.pixels(), pixelCandidate.lossless());
        }
    }

    @NotNull
    protected static EncodedPayloadCandidate buildFilteredCandidate(int width, int height, @NotNull PixelCandidate pixelCandidate, @NotNull Mode mode) throws IOException {
        int[] pixels = pixelCandidate.pixels();
        int channels = switch (mode) {
            case RGB_FILTERED -> RGB_CHANNELS;
            case RGBA_FILTERED -> RGBA_CHANNELS;
            default -> throw new IllegalArgumentException("Unsupported AFMA BIN_INTRA filtered mode: " + mode);
        };
        byte[] rawBytes = toInterleavedBytes(pixels, channels);
        byte[] filteredRows = encodeFilteredRows(rawBytes, width * channels, height, channels);
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            writeHeader(out, mode, width, height);
            out.write(filteredRows);
            out.flush();
            return new EncodedPayloadCandidate(mode, byteStream.toByteArray(), pixelCandidate.pixels(), pixelCandidate.lossless());
        }
    }

    @NotNull
    protected static EncodedPayloadCandidate buildSplitAlphaCandidate(int width, int height, @NotNull PixelCandidate pixelCandidate) throws IOException {
        int[] pixels = pixelCandidate.pixels();
        byte[] rgbBytes = new byte[width * height * RGB_CHANNELS];
        byte[] alphaBytes = new byte[width * height];
        int rgbOffset = 0;
        int alphaOffset = 0;
        for (int color : pixels) {
            rgbBytes[rgbOffset++] = (byte) ((color >> 16) & 0xFF);
            rgbBytes[rgbOffset++] = (byte) ((color >> 8) & 0xFF);
            rgbBytes[rgbOffset++] = (byte) (color & 0xFF);
            alphaBytes[alphaOffset++] = (byte) ((color >>> 24) & 0xFF);
        }

        byte[] filteredRgb = encodeFilteredRows(rgbBytes, width * RGB_CHANNELS, height, RGB_CHANNELS);
        byte[] filteredAlpha = encodeFilteredRows(alphaBytes, width, height, 1);
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            writeHeader(out, Mode.RGB_PLUS_ALPHA_SPLIT, width, height);
            out.write(filteredRgb);
            out.write(filteredAlpha);
            out.flush();
            return new EncodedPayloadCandidate(Mode.RGB_PLUS_ALPHA_SPLIT, byteStream.toByteArray(), pixelCandidate.pixels(), pixelCandidate.lossless());
        }
    }

    @Nullable
    protected static EncodedPayloadCandidate buildColorPlusAlphaMaskCandidate(int width, int height, @NotNull PixelCandidate pixelCandidate) throws IOException {
        int[] pixels = pixelCandidate.pixels();
        int firstColor = pixels[0];
        int red = (firstColor >> 16) & 0xFF;
        int green = (firstColor >> 8) & 0xFF;
        int blue = firstColor & 0xFF;

        boolean binaryAlpha = true;
        boolean alphaVaries = false;
        byte[] alphaBytes = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            if (((color >> 16) & 0xFF) != red || ((color >> 8) & 0xFF) != green || (color & 0xFF) != blue) {
                return null;
            }

            int alpha = (color >>> 24) & 0xFF;
            alphaBytes[i] = (byte) alpha;
            if (alpha != ((firstColor >>> 24) & 0xFF)) {
                alphaVaries = true;
            }
            if ((alpha != 0) && (alpha != 0xFF)) {
                binaryAlpha = false;
            }
        }

        if (!alphaVaries) {
            return null;
        }

        byte[] encodedAlpha;
        int alphaMode;
        if (binaryAlpha) {
            alphaMode = AlphaMaskMode.BINARY.id();
            byte[] packedMask = packBinaryAlpha(alphaBytes, width, height);
            encodedAlpha = encodeFilteredRows(packedMask, packedRowBytes(width, 1), height, 1);
        } else {
            alphaMode = AlphaMaskMode.FULL.id();
            encodedAlpha = encodeFilteredRows(alphaBytes, width, height, 1);
        }

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            writeHeader(out, Mode.COLOR_PLUS_ALPHA_MASK, width, height);
            out.writeByte(red);
            out.writeByte(green);
            out.writeByte(blue);
            out.writeByte(alphaMode);
            out.write(encodedAlpha);
            out.flush();
            return new EncodedPayloadCandidate(Mode.COLOR_PLUS_ALPHA_MASK, byteStream.toByteArray(), pixelCandidate.pixels(), pixelCandidate.lossless());
        }
    }

    @NotNull
    protected static int[] decodeSolid(@NotNull DataInputStream in, int width, int height) throws IOException {
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, in.readInt());
        return pixels;
    }

    @NotNull
    protected static int[] decodeIndexed(@NotNull DataInputStream in, int width, int height) throws IOException {
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
        byte[] packedRows = decodeFilteredRows(in, rowBytes, height, 1);
        int[] pixels = new int[width * height];
        int rowOffset = 0;
        int pixelOffset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int paletteIndex = unpackPackedValue(packedRows, rowOffset, x, bitsPerIndex);
                if ((paletteIndex < 0) || (paletteIndex >= palette.length)) {
                    throw new IOException("AFMA BIN_INTRA indexed payload references an invalid palette index");
                }
                pixels[pixelOffset++] = palette[paletteIndex];
            }
            rowOffset += rowBytes;
        }
        return pixels;
    }

    @NotNull
    protected static int[] decodeFilteredTruecolor(@NotNull DataInputStream in, int width, int height, int channels) throws IOException {
        int rowBytes = width * channels;
        byte[] decodedBytes = decodeFilteredRows(in, rowBytes, height, channels);
        int[] pixels = new int[width * height];
        int byteOffset = 0;
        for (int i = 0; i < pixels.length; i++) {
            int red = decodedBytes[byteOffset++] & 0xFF;
            int green = decodedBytes[byteOffset++] & 0xFF;
            int blue = decodedBytes[byteOffset++] & 0xFF;
            int alpha = (channels == RGBA_CHANNELS) ? (decodedBytes[byteOffset++] & 0xFF) : 0xFF;
            pixels[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        return pixels;
    }

    @NotNull
    protected static int[] decodeSplitAlpha(@NotNull DataInputStream in, int width, int height) throws IOException {
        byte[] rgbBytes = decodeFilteredRows(in, width * RGB_CHANNELS, height, RGB_CHANNELS);
        byte[] alphaBytes = decodeFilteredRows(in, width, height, 1);

        int[] pixels = new int[width * height];
        int rgbOffset = 0;
        int alphaOffset = 0;
        for (int i = 0; i < pixels.length; i++) {
            int red = rgbBytes[rgbOffset++] & 0xFF;
            int green = rgbBytes[rgbOffset++] & 0xFF;
            int blue = rgbBytes[rgbOffset++] & 0xFF;
            int alpha = alphaBytes[alphaOffset++] & 0xFF;
            pixels[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        return pixels;
    }

    @NotNull
    protected static int[] decodeColorPlusAlphaMask(@NotNull DataInputStream in, int width, int height) throws IOException {
        int red = in.readUnsignedByte();
        int green = in.readUnsignedByte();
        int blue = in.readUnsignedByte();
        AlphaMaskMode alphaMode = AlphaMaskMode.byId(in.readUnsignedByte());

        int[] pixels = new int[width * height];
        int baseRgb = (red << 16) | (green << 8) | blue;
        if (alphaMode == AlphaMaskMode.BINARY) {
            int rowBytes = packedRowBytes(width, 1);
            byte[] packedMask = decodeFilteredRows(in, rowBytes, height, 1);
            int rowOffset = 0;
            int pixelOffset = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int alpha = unpackPackedValue(packedMask, rowOffset, x, 1) != 0 ? 0xFF : 0x00;
                    pixels[pixelOffset++] = (alpha << 24) | baseRgb;
                }
                rowOffset += rowBytes;
            }
            return pixels;
        }

        byte[] alphaBytes = decodeFilteredRows(in, width, height, 1);
        for (int i = 0; i < pixels.length; i++) {
            int alpha = alphaBytes[i] & 0xFF;
            pixels[i] = (alpha << 24) | baseRgb;
        }
        return pixels;
    }

    protected static void writeHeader(@NotNull DataOutputStream out, @NotNull Mode mode, int width, int height) throws IOException {
        validateDimensions(width, height);
        out.writeInt(PAYLOAD_MAGIC);
        out.writeByte(PAYLOAD_VERSION);
        out.writeByte(mode.id());
        out.writeShort(width);
        out.writeShort(height);
    }

    @NotNull
    protected static byte[] toInterleavedBytes(@NotNull int[] pixels, int channels) {
        int[] normalizedPixels = Objects.requireNonNull(pixels);
        byte[] rawBytes = new byte[normalizedPixels.length * channels];
        int byteOffset = 0;
        for (int color : normalizedPixels) {
            rawBytes[byteOffset++] = (byte) ((color >> 16) & 0xFF);
            rawBytes[byteOffset++] = (byte) ((color >> 8) & 0xFF);
            rawBytes[byteOffset++] = (byte) (color & 0xFF);
            if (channels == RGBA_CHANNELS) {
                rawBytes[byteOffset++] = (byte) ((color >>> 24) & 0xFF);
            }
        }
        return rawBytes;
    }

    @NotNull
    protected static byte[] encodeFilteredRows(@NotNull byte[] rawBytes, int rowBytes, int height, int bytesPerPixel) {
        if (rowBytes < 0 || height <= 0 || bytesPerPixel <= 0) {
            throw new IllegalArgumentException("AFMA BIN_INTRA filter row dimensions are invalid");
        }

        byte[] encoded = new byte[height * (rowBytes + 1)];
        byte[] previousRow = new byte[rowBytes];
        byte[] bestRow = new byte[rowBytes];
        byte[] candidateRow = new byte[rowBytes];
        int encodedOffset = 0;
        int rowOffset = 0;
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

    protected static void addCandidateBuilders(@NotNull List<PayloadCandidateBuilder> candidateBuilders, int width, int height, @NotNull PixelCandidate pixelCandidate) {
        candidateBuilders.add(() -> buildSolidCandidate(width, height, pixelCandidate));
        candidateBuilders.add(() -> buildIndexedCandidate(width, height, pixelCandidate));

        if (hasAlpha(pixelCandidate.pixels())) {
            candidateBuilders.add(() -> buildFilteredCandidate(width, height, pixelCandidate, Mode.RGBA_FILTERED));
            candidateBuilders.add(() -> buildSplitAlphaCandidate(width, height, pixelCandidate));
            candidateBuilders.add(() -> buildColorPlusAlphaMaskCandidate(width, height, pixelCandidate));
        } else {
            candidateBuilders.add(() -> buildFilteredCandidate(width, height, pixelCandidate, Mode.RGB_FILTERED));
        }
    }

    @NotNull
    protected static List<PixelCandidate> collectPixelCandidates(int width, int height, @NotNull int[] pixels, @NotNull EncodePreferences preferences) {
        List<PixelCandidate> pixelCandidates = new ArrayList<>(4);
        addPixelCandidate(pixelCandidates, pixels, true);

        if (!preferences.perceptualCandidatesEnabled()) {
            return pixelCandidates;
        }

        addPixelCandidate(pixelCandidates, normalizeHiddenTransparentPixels(pixels), false);
        for (PerceptualProfile profile : PerceptualProfile.values()) {
            int[] quantizedPixels = buildPerceptualPaletteCandidate(width, height, pixels, profile, preferences);
            if (quantizedPixels != null) {
                addPixelCandidate(pixelCandidates, quantizedPixels, false);
            }
        }
        return pixelCandidates;
    }

    protected static void addPixelCandidate(@NotNull List<PixelCandidate> pixelCandidates, @NotNull int[] pixels, boolean lossless) {
        for (int i = 0; i < pixelCandidates.size(); i++) {
            PixelCandidate existingCandidate = pixelCandidates.get(i);
            if (!Arrays.equals(existingCandidate.pixels(), pixels)) {
                continue;
            }
            if (lossless && !existingCandidate.lossless()) {
                pixelCandidates.set(i, new PixelCandidate(existingCandidate.pixels(), true));
            }
            return;
        }
        pixelCandidates.add(new PixelCandidate(pixels, lossless));
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
        return candidateCount > 1 && ((long) width * height) >= MIN_PARALLEL_MODE_SELECTION_PIXELS;
    }

    @NotNull
    protected static List<EncodedPayloadCandidate> buildCandidatesSequential(@NotNull List<PayloadCandidateBuilder> candidateBuilders) throws IOException {
        List<EncodedPayloadCandidate> candidates = new ArrayList<>(candidateBuilders.size());
        for (PayloadCandidateBuilder candidateBuilder : candidateBuilders) {
            EncodedPayloadCandidate candidate = candidateBuilder.build();
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    @NotNull
    protected static List<EncodedPayloadCandidate> buildCandidatesInParallel(@NotNull List<PayloadCandidateBuilder> candidateBuilders) throws IOException {
        List<CompletableFuture<EncodedPayloadCandidate>> candidateFutures = new ArrayList<>(candidateBuilders.size());
        for (PayloadCandidateBuilder candidateBuilder : candidateBuilders) {
            candidateFutures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return candidateBuilder.build();
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }
            }));
        }

        List<EncodedPayloadCandidate> candidates = new ArrayList<>(candidateBuilders.size());
        for (CompletableFuture<EncodedPayloadCandidate> candidateFuture : candidateFutures) {
            EncodedPayloadCandidate candidate = joinCandidate(candidateFuture);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    @Nullable
    protected static EncodedPayloadCandidate joinCandidate(@NotNull CompletableFuture<EncodedPayloadCandidate> candidateFuture) throws IOException {
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
        for (int i = 0; i < rowBytes; i++) {
            int current = rawBytes[rowOffset + i] & 0xFF;
            int left = (i >= bytesPerPixel) ? (rawBytes[rowOffset + i - bytesPerPixel] & 0xFF) : 0;
            int up = previousRow[i] & 0xFF;
            int upLeft = (i >= bytesPerPixel) ? (previousRow[i - bytesPerPixel] & 0xFF) : 0;
            int filtered = switch (filter) {
                case NONE -> current;
                case SUB -> current - left;
                case UP -> current - up;
                case AVERAGE -> current - ((left + up) >>> 1);
                case PAETH -> current - paethPredictor(left, up, upLeft);
            };
            output[i] = (byte) filtered;
        }
    }

    protected static long scoreFilteredRow(@NotNull byte[] filteredRow, int rowBytes) {
        long score = 0L;
        for (int i = 0; i < rowBytes; i++) {
            score += Math.abs(filteredRow[i]);
        }
        return score;
    }

    @NotNull
    protected static byte[] decodeFilteredRows(@NotNull DataInputStream in, int rowBytes, int height, int bytesPerPixel) throws IOException {
        if (rowBytes < 0 || height <= 0 || bytesPerPixel <= 0) {
            throw new IOException("AFMA BIN_INTRA filter row dimensions are invalid");
        }

        byte[] decoded = new byte[rowBytes * height];
        byte[] previousRow = new byte[rowBytes];
        byte[] filteredRow = new byte[rowBytes];
        byte[] decodedRow = new byte[rowBytes];
        int decodedOffset = 0;
        for (int row = 0; row < height; row++) {
            Filter filter = Filter.byId(in.readUnsignedByte());
            byte[] rowBytesArray = in.readNBytes(rowBytes);
            if (rowBytesArray.length != rowBytes) {
                throw new IOException("AFMA BIN_INTRA row payload ended early");
            }
            System.arraycopy(rowBytesArray, 0, filteredRow, 0, rowBytes);
            inverseFilter(filter, filteredRow, previousRow, bytesPerPixel, decodedRow);
            System.arraycopy(decodedRow, 0, decoded, decodedOffset, rowBytes);
            System.arraycopy(decodedRow, 0, previousRow, 0, rowBytes);
            decodedOffset += rowBytes;
        }
        return decoded;
    }

    protected static void inverseFilter(@NotNull Filter filter, @NotNull byte[] filteredRow, @NotNull byte[] previousRow,
                                        int bytesPerPixel, @NotNull byte[] output) {
        for (int i = 0; i < filteredRow.length; i++) {
            int left = (i >= bytesPerPixel) ? (output[i - bytesPerPixel] & 0xFF) : 0;
            int up = previousRow[i] & 0xFF;
            int upLeft = (i >= bytesPerPixel) ? (previousRow[i - bytesPerPixel] & 0xFF) : 0;
            int value = switch (filter) {
                case NONE -> filteredRow[i];
                case SUB -> (filteredRow[i] & 0xFF) + left;
                case UP -> (filteredRow[i] & 0xFF) + up;
                case AVERAGE -> (filteredRow[i] & 0xFF) + ((left + up) >>> 1);
                case PAETH -> (filteredRow[i] & 0xFF) + paethPredictor(left, up, upLeft);
            };
            output[i] = (byte) value;
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

    protected static boolean hasAlpha(@NotNull int[] pixels) {
        for (int color : pixels) {
            if (((color >>> 24) & 0xFF) != 0xFF) {
                return true;
            }
        }
        return false;
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

    public record PayloadHeader(int width, int height, @NotNull Mode mode) {
    }

    public record DecodedFrame(int width, int height, @NotNull int[] pixels) {
    }

    protected record PixelCandidate(@NotNull int[] pixels, boolean lossless) {
    }

    protected record PerceptualErrorStats(double averageError, int maxVisibleColorDelta, int maxAlphaDelta) {

        protected boolean isWithin(@NotNull EncodePreferences preferences) {
            return this.averageError <= preferences.maxAverageError()
                    && this.maxVisibleColorDelta <= preferences.maxVisibleColorDelta()
                    && this.maxAlphaDelta <= preferences.maxAlphaDelta();
        }
    }

    protected record EncodedPayloadCandidate(@NotNull Mode mode, @NotNull byte[] payloadBytes,
                                             @NotNull int[] reconstructedPixels, boolean lossless,
                                             long estimatedArchiveBytes) {

        protected EncodedPayloadCandidate(@NotNull Mode mode, @NotNull byte[] payloadBytes,
                                          @NotNull int[] reconstructedPixels, boolean lossless) {
            this(mode, payloadBytes, reconstructedPixels, lossless, AfmaPayloadMetricsHelper.estimateArchiveBytes(payloadBytes));
        }

        protected boolean isBetterThan(@NotNull EncodedPayloadCandidate other) {
            if (this.estimatedArchiveBytes != other.estimatedArchiveBytes) {
                return this.estimatedArchiveBytes < other.estimatedArchiveBytes;
            }
            if (this.lossless != other.lossless) {
                return this.lossless;
            }
            if (this.payloadBytes.length != other.payloadBytes.length) {
                return this.payloadBytes.length < other.payloadBytes.length;
            }
            return this.mode.priority() < other.mode.priority();
        }
    }

    protected record FilterSelection(@NotNull Filter filter, long score) {
    }

    @FunctionalInterface
    protected interface PayloadCandidateBuilder {
        @Nullable EncodedPayloadCandidate build() throws IOException;
    }

    public enum Mode {
        SOLID(0, 0),
        INDEXED(1, 2),
        RGB_FILTERED(2, 1),
        RGBA_FILTERED(3, 4),
        RGB_PLUS_ALPHA_SPLIT(4, 3),
        COLOR_PLUS_ALPHA_MASK(5, 1);

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

}
