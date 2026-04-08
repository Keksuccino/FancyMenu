package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AfmaResidualPayloadHelper {

    public static final int ALPHA_ONLY_CHANNELS = 1;
    public static final int RGB_CHANNELS = 3;
    public static final int RGBA_CHANNELS = 4;

    private AfmaResidualPayloadHelper() {
    }

    public static int channelCount(boolean includeAlpha) {
        return includeAlpha ? RGBA_CHANNELS : RGB_CHANNELS;
    }

    public static boolean isValidChannelCount(int channels) {
        return channels == RGB_CHANNELS || channels == RGBA_CHANNELS;
    }

    public static boolean isValidStreamChannelCount(int channels) {
        return channels == ALPHA_ONLY_CHANNELS || isValidChannelCount(channels);
    }

    public static int expectedDenseResidualBytes(int width, int height, int channels) {
        if ((width <= 0) || (height <= 0) || !isValidChannelCount(channels)) {
            return 0;
        }
        long totalPixels = (long) width * (long) height;
        long totalBytes = totalPixels * channels;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    public static int expectedDenseResidualBytes(int width, int height, @NotNull AfmaResidualPayload residualPayload) {
        if ((width <= 0) || (height <= 0)) {
            return 0;
        }
        return expectedResidualPayloadBytes((long) width * height, residualPayload.getChannels(),
                residualPayload.getAlphaMode(), residualPayload.getAlphaChangedPixelCount());
    }

    public static int expectedSparseResidualBytes(int changedPixelCount, int channels) {
        if ((changedPixelCount < 0) || !isValidChannelCount(channels)) {
            return 0;
        }
        long totalBytes = (long) changedPixelCount * channels;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    public static int expectedSparseResidualBytes(int changedPixelCount, @NotNull AfmaSparsePayload sparsePayload) {
        return expectedResidualPayloadBytes(changedPixelCount, sparsePayload.getChannels(),
                sparsePayload.getAlphaMode(), sparsePayload.getAlphaChangedPixelCount());
    }

    public static int expectedResidualPayloadBytes(long sampleCount, int channels,
                                                   @NotNull AfmaAlphaResidualMode alphaMode,
                                                   int alphaChangedPixelCount) {
        if ((sampleCount < 0L) || (sampleCount > Integer.MAX_VALUE) || !isValidChannelCount(channels)) {
            return 0;
        }

        long totalBytes = (long) expectedPrimaryStreamBytes((int) sampleCount, channels, alphaMode)
                + expectedAlphaMaskBytes((int) sampleCount, alphaMode)
                + expectedAlphaStreamBytes(alphaMode, alphaChangedPixelCount);
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    public static int expectedSparseMaskBytes(int width, int height) {
        if ((width <= 0) || (height <= 0)) {
            return 0;
        }
        return expectedSparseBitsetBytes((long) width * height);
    }

    public static int expectedSparseBitsetBytes(long bitCount) {
        if (bitCount <= 0L || bitCount > Integer.MAX_VALUE) {
            return 0;
        }
        long totalBytes = (bitCount + 7L) / 8L;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    public static int expectedPrimaryStreamBytes(int sampleCount, int channels, @NotNull AfmaAlphaResidualMode alphaMode) {
        if ((sampleCount < 0) || !isValidChannelCount(channels)) {
            return 0;
        }
        int primaryChannels = (alphaMode == AfmaAlphaResidualMode.FULL) ? channels : RGB_CHANNELS;
        if (!isValidStreamChannelCount(primaryChannels)) {
            return 0;
        }
        long totalBytes = (long) sampleCount * primaryChannels;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    public static int expectedAlphaMaskBytes(int sampleCount, @NotNull AfmaAlphaResidualMode alphaMode) {
        return (alphaMode == AfmaAlphaResidualMode.SPARSE) ? expectedSparseBitsetBytes(sampleCount) : 0;
    }

    public static int expectedAlphaStreamBytes(@NotNull AfmaAlphaResidualMode alphaMode, int alphaChangedPixelCount) {
        if (alphaMode != AfmaAlphaResidualMode.SPARSE) {
            return 0;
        }
        if (alphaChangedPixelCount < 0) {
            return 0;
        }
        return alphaChangedPixelCount;
    }

    public static void validateDensePayload(@NotNull byte[] payloadBytes, int offset, int length,
                                            int width, int height, @NotNull AfmaResidualPayload residualPayload) {
        residualPayload.validate("AFMA residual payload descriptor");
        validateResidualPayload(payloadBytes, offset, length, (long) width * height,
                residualPayload.getChannels(), residualPayload.getAlphaMode(), residualPayload.getAlphaChangedPixelCount());
    }

    public static void validateSparsePayload(@NotNull byte[] payloadBytes, int offset, int length,
                                             int changedPixelCount, @NotNull AfmaSparsePayload sparsePayload) {
        sparsePayload.validate("AFMA sparse payload descriptor");
        validateResidualPayload(payloadBytes, offset, length, changedPixelCount,
                sparsePayload.getChannels(), sparsePayload.getAlphaMode(), sparsePayload.getAlphaChangedPixelCount());
    }

    public static void validateResidualPayload(@NotNull byte[] payloadBytes, int offset, int length, long sampleCount,
                                               int channels, @NotNull AfmaAlphaResidualMode alphaMode,
                                               int alphaChangedPixelCount) {
        Objects.requireNonNull(payloadBytes);
        Objects.requireNonNull(alphaMode);
        if ((alphaMode == AfmaAlphaResidualMode.SPARSE) && ((alphaChangedPixelCount <= 0) || (alphaChangedPixelCount > sampleCount))) {
            throw new IllegalStateException("AFMA sparse alpha metadata does not match the frame descriptor");
        }
        int expectedBytes = expectedResidualPayloadBytes(sampleCount, channels, alphaMode, alphaChangedPixelCount);
        if ((expectedBytes <= 0) || (length != expectedBytes) || (offset < 0)
                || (((long) offset + length) > payloadBytes.length)) {
            throw new IllegalStateException("AFMA residual payload size does not match the frame descriptor");
        }
    }

    public static void validateSparseAlphaMaskPopulation(@NotNull byte[] payloadBytes, int payloadOffset, int sampleCount,
                                                         int channels, @NotNull AfmaAlphaResidualMode alphaMode,
                                                         int alphaChangedPixelCount) {
        Objects.requireNonNull(payloadBytes);
        Objects.requireNonNull(alphaMode);
        if (alphaMode != AfmaAlphaResidualMode.SPARSE) {
            return;
        }

        int alphaMaskOffset = resolveAlphaMaskOffset(payloadOffset, sampleCount, channels, alphaMode);
        int alphaMaskLength = resolveAlphaMaskLength(sampleCount, alphaMode);
        if ((alphaMaskLength <= 0) || (alphaMaskOffset < 0) || (((long) alphaMaskOffset + alphaMaskLength) > payloadBytes.length)) {
            throw new IllegalStateException("AFMA sparse alpha mask slice is invalid");
        }

        int populatedBits = countMaskBits(payloadBytes, alphaMaskOffset, sampleCount);
        if (populatedBits != alphaChangedPixelCount) {
            throw new IllegalStateException("AFMA sparse alpha mask bit population does not match its descriptor");
        }
    }

    public static void setMaskBit(@NotNull byte[] maskBytes, int pixelIndex) {
        maskBytes[pixelIndex >>> 3] |= (byte) (1 << (7 - (pixelIndex & 7)));
    }

    public static boolean isMaskBitSet(@NotNull byte[] maskBytes, int pixelIndex) {
        return (maskBytes[pixelIndex >>> 3] & (1 << (7 - (pixelIndex & 7)))) != 0;
    }

    public static int countMaskBits(@NotNull byte[] maskBytes, int offset, int bitCount) {
        Objects.requireNonNull(maskBytes);
        if ((offset < 0) || (bitCount < 0) || (((long) offset + expectedSparseBitsetBytes(bitCount)) > maskBytes.length)) {
            throw new IllegalArgumentException("AFMA sparse mask slice is invalid");
        }

        int populatedBits = 0;
        for (int bitIndex = 0; bitIndex < bitCount; bitIndex++) {
            if ((maskBytes[offset + (bitIndex >>> 3)] & (1 << (7 - (bitIndex & 7)))) != 0) {
                populatedBits++;
            }
        }
        return populatedBits;
    }

    public static int writeResidual(@NotNull byte[] target, int offset, int predictedColor, int currentColor, boolean includeAlpha) {
        target[offset++] = (byte) ((((currentColor >> 16) & 0xFF) - ((predictedColor >> 16) & 0xFF)) & 0xFF);
        target[offset++] = (byte) ((((currentColor >> 8) & 0xFF) - ((predictedColor >> 8) & 0xFF)) & 0xFF);
        target[offset++] = (byte) (((currentColor & 0xFF) - (predictedColor & 0xFF)) & 0xFF);
        if (includeAlpha) {
            target[offset++] = (byte) ((((currentColor >>> 24) & 0xFF) - ((predictedColor >>> 24) & 0xFF)) & 0xFF);
        }
        return offset;
    }

    public static int applyResidualToArgb(int predictedColor, @NotNull byte[] residualBytes, int offset, int channels) {
        int alpha = (predictedColor >>> 24) & 0xFF;
        int red = (predictedColor >> 16) & 0xFF;
        int green = (predictedColor >> 8) & 0xFF;
        int blue = predictedColor & 0xFF;

        red = (red + residualBytes[offset++]) & 0xFF;
        green = (green + residualBytes[offset++]) & 0xFF;
        blue = (blue + residualBytes[offset++]) & 0xFF;
        if (channels == RGBA_CHANNELS) {
            alpha = (alpha + residualBytes[offset]) & 0xFF;
        }

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    @NotNull
    public static EncodedResidualPayload encodeBestResidualPayload(@NotNull int[] predictedColors, @NotNull int[] currentColors,
                                                                   int sampleCount, boolean includeAlpha) {
        Objects.requireNonNull(predictedColors);
        Objects.requireNonNull(currentColors);
        if ((sampleCount <= 0) || (sampleCount > predictedColors.length) || (sampleCount > currentColors.length)) {
            throw new IllegalArgumentException("AFMA residual sample count is invalid");
        }

        int channels = includeAlpha ? RGBA_CHANNELS : RGB_CHANNELS;
        int alphaChangedPixelCount = includeAlpha ? countAlphaChanges(predictedColors, currentColors, sampleCount) : 0;
        List<EncodedResidualPayload> candidates = new ArrayList<>();
        candidates.addAll(buildCodecCandidates(predictedColors, currentColors, sampleCount, channels,
                includeAlpha ? AfmaAlphaResidualMode.FULL : AfmaAlphaResidualMode.NONE, alphaChangedPixelCount));
        if (includeAlpha && (alphaChangedPixelCount > 0) && (alphaChangedPixelCount < sampleCount)) {
            candidates.addAll(buildCodecCandidates(predictedColors, currentColors, sampleCount, channels,
                    AfmaAlphaResidualMode.SPARSE, alphaChangedPixelCount));
        }

        EncodedResidualPayload bestCandidate = null;
        for (EncodedResidualPayload candidate : candidates) {
            if ((bestCandidate == null) || candidate.isBetterThan(bestCandidate)) {
                bestCandidate = candidate;
            }
        }
        if (bestCandidate == null) {
            throw new IllegalStateException("Failed to encode AFMA residual payload");
        }
        return bestCandidate;
    }

    @NotNull
    protected static List<EncodedResidualPayload> buildCodecCandidates(@NotNull int[] predictedColors, @NotNull int[] currentColors,
                                                                       int sampleCount, int channels,
                                                                       @NotNull AfmaAlphaResidualMode alphaMode,
                                                                       int alphaChangedPixelCount) {
        byte[] colorRawStream = buildColorResidualStream(predictedColors, currentColors, sampleCount, alphaMode == AfmaAlphaResidualMode.FULL ? channels : RGB_CHANNELS);
        byte[] alphaMask = (alphaMode == AfmaAlphaResidualMode.SPARSE)
                ? buildAlphaChangeMask(predictedColors, currentColors, sampleCount)
                : null;
        byte[] alphaRawStream = (alphaMode == AfmaAlphaResidualMode.SPARSE)
                ? buildSparseAlphaResidualStream(predictedColors, currentColors, sampleCount, alphaChangedPixelCount)
                : null;

        ArrayList<EncodedResidualPayload> candidates = new ArrayList<>(AfmaResidualCodec.values().length);
        for (AfmaResidualCodec codec : AfmaResidualCodec.values()) {
            byte[] primaryBytes = transformResidualStream(colorRawStream, sampleCount,
                    (alphaMode == AfmaAlphaResidualMode.FULL) ? channels : RGB_CHANNELS, codec);
            byte[] secondaryBytes = (alphaRawStream != null)
                    ? transformResidualStream(alphaRawStream, alphaChangedPixelCount, ALPHA_ONLY_CHANNELS, codec)
                    : null;
            byte[] payloadBytes = mergeStreams(primaryBytes, alphaMask, secondaryBytes);
            candidates.add(new EncodedResidualPayload(
                    payloadBytes,
                    channels,
                    codec,
                    alphaMode,
                    alphaMode == AfmaAlphaResidualMode.SPARSE ? alphaChangedPixelCount : 0,
                    AfmaPayloadMetricsHelper.estimateArchiveBytes(payloadBytes),
                    codec.getComplexityScore() + (alphaMode == AfmaAlphaResidualMode.SPARSE ? 1 : 0)
            ));
        }
        return candidates;
    }

    @NotNull
    protected static byte[] buildColorResidualStream(@NotNull int[] predictedColors, @NotNull int[] currentColors,
                                                     int sampleCount, int channels) {
        int expectedBytes = sampleCount * channels;
        byte[] rawBytes = new byte[expectedBytes];
        int offset = 0;
        for (int i = 0; i < sampleCount; i++) {
            int predictedColor = predictedColors[i];
            int currentColor = currentColors[i];
            rawBytes[offset++] = (byte) ((((currentColor >> 16) & 0xFF) - ((predictedColor >> 16) & 0xFF)) & 0xFF);
            rawBytes[offset++] = (byte) ((((currentColor >> 8) & 0xFF) - ((predictedColor >> 8) & 0xFF)) & 0xFF);
            rawBytes[offset++] = (byte) (((currentColor & 0xFF) - (predictedColor & 0xFF)) & 0xFF);
            if (channels == RGBA_CHANNELS) {
                rawBytes[offset++] = (byte) ((((currentColor >>> 24) & 0xFF) - ((predictedColor >>> 24) & 0xFF)) & 0xFF);
            }
        }
        return rawBytes;
    }

    @NotNull
    protected static byte[] buildAlphaChangeMask(@NotNull int[] predictedColors, @NotNull int[] currentColors, int sampleCount) {
        byte[] maskBytes = new byte[expectedSparseBitsetBytes(sampleCount)];
        for (int i = 0; i < sampleCount; i++) {
            if (((predictedColors[i] ^ currentColors[i]) & 0xFF000000) != 0) {
                setMaskBit(maskBytes, i);
            }
        }
        return maskBytes;
    }

    @NotNull
    protected static byte[] buildSparseAlphaResidualStream(@NotNull int[] predictedColors, @NotNull int[] currentColors,
                                                           int sampleCount, int alphaChangedPixelCount) {
        byte[] rawBytes = new byte[alphaChangedPixelCount];
        int offset = 0;
        for (int i = 0; i < sampleCount; i++) {
            int predictedColor = predictedColors[i];
            int currentColor = currentColors[i];
            if (((predictedColor ^ currentColor) & 0xFF000000) == 0) {
                continue;
            }
            rawBytes[offset++] = (byte) ((((currentColor >>> 24) & 0xFF) - ((predictedColor >>> 24) & 0xFF)) & 0xFF);
        }
        return rawBytes;
    }

    @NotNull
    protected static byte[] transformResidualStream(@NotNull byte[] rawBytes, int sampleCount, int channels,
                                                    @NotNull AfmaResidualCodec codec) {
        if (codec == AfmaResidualCodec.INTERLEAVED) {
            return rawBytes;
        }

        if ((sampleCount < 0) || !isValidStreamChannelCount(channels) || (rawBytes.length != (sampleCount * channels))) {
            throw new IllegalArgumentException("AFMA residual stream dimensions are invalid");
        }

        byte[] transformedBytes = new byte[rawBytes.length];
        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            int interleavedOffset = sampleIndex * channels;
            for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                int transformedOffset = (channelIndex * sampleCount) + sampleIndex;
                byte value = rawBytes[interleavedOffset + channelIndex];
                transformedBytes[transformedOffset] = switch (codec) {
                    case PLANAR -> value;
                    case PLANAR_ZIGZAG, PLANAR_ZIGZAG_DELTA -> (byte) zigzagEncode(value);
                    case INTERLEAVED -> throw new IllegalStateException("INTERLEAVED residual streams should not be transformed");
                };
            }
        }

        if (codec == AfmaResidualCodec.PLANAR_ZIGZAG_DELTA) {
            for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                int planeOffset = channelIndex * sampleCount;
                int previousValue = 0;
                for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
                    int currentValue = transformedBytes[planeOffset + sampleIndex] & 0xFF;
                    transformedBytes[planeOffset + sampleIndex] = (byte) ((currentValue - previousValue) & 0xFF);
                    previousValue = currentValue;
                }
            }
        }

        return transformedBytes;
    }

    @NotNull
    protected static byte[] mergeStreams(@NotNull byte[] primaryStream, byte[] alphaMask, byte[] alphaStream) {
        int totalLength = primaryStream.length
                + ((alphaMask != null) ? alphaMask.length : 0)
                + ((alphaStream != null) ? alphaStream.length : 0);
        byte[] payloadBytes = new byte[totalLength];
        int offset = 0;
        System.arraycopy(primaryStream, 0, payloadBytes, offset, primaryStream.length);
        offset += primaryStream.length;
        if (alphaMask != null) {
            System.arraycopy(alphaMask, 0, payloadBytes, offset, alphaMask.length);
            offset += alphaMask.length;
        }
        if (alphaStream != null) {
            System.arraycopy(alphaStream, 0, payloadBytes, offset, alphaStream.length);
        }
        return payloadBytes;
    }

    protected static int countAlphaChanges(@NotNull int[] predictedColors, @NotNull int[] currentColors, int sampleCount) {
        int alphaChangedPixelCount = 0;
        for (int i = 0; i < sampleCount; i++) {
            if (((predictedColors[i] ^ currentColors[i]) & 0xFF000000) != 0) {
                alphaChangedPixelCount++;
            }
        }
        return alphaChangedPixelCount;
    }

    protected static int zigzagEncode(int value) {
        return ((value << 1) ^ (value >> 31)) & 0xFF;
    }

    protected static int zigzagDecode(int value) {
        return (value >>> 1) ^ -(value & 1);
    }

    public static int resolvePrimaryStreamLength(int sampleCount, int channels, @NotNull AfmaAlphaResidualMode alphaMode) {
        return expectedPrimaryStreamBytes(sampleCount, channels, alphaMode);
    }

    public static int resolveAlphaMaskOffset(int payloadOffset, int sampleCount, int channels, @NotNull AfmaAlphaResidualMode alphaMode) {
        return payloadOffset + resolvePrimaryStreamLength(sampleCount, channels, alphaMode);
    }

    public static int resolveAlphaMaskLength(int sampleCount, @NotNull AfmaAlphaResidualMode alphaMode) {
        return expectedAlphaMaskBytes(sampleCount, alphaMode);
    }

    public static int resolveAlphaStreamOffset(int payloadOffset, int sampleCount, int channels, @NotNull AfmaAlphaResidualMode alphaMode) {
        return resolveAlphaMaskOffset(payloadOffset, sampleCount, channels, alphaMode)
                + resolveAlphaMaskLength(sampleCount, alphaMode);
    }

    @NotNull
    public static ResidualSampleReader openReader(@NotNull byte[] payloadBytes, int offset, int length,
                                                  int sampleCount, int channels, @NotNull AfmaResidualCodec codec) {
        Objects.requireNonNull(payloadBytes);
        Objects.requireNonNull(codec);
        if (!isValidStreamChannelCount(channels)) {
            throw new IllegalArgumentException("AFMA residual stream channel count is invalid: " + channels);
        }
        int expectedBytes = sampleCount * channels;
        if ((expectedBytes < 0) || (length != expectedBytes) || (offset < 0) || (((long) offset + length) > payloadBytes.length)) {
            throw new IllegalArgumentException("AFMA residual stream slice is invalid");
        }
        return new ResidualSampleReader(payloadBytes, offset, sampleCount, channels, codec);
    }

    public record EncodedResidualPayload(@NotNull byte[] payloadBytes, int channels,
                                         @NotNull AfmaResidualCodec codec,
                                         @NotNull AfmaAlphaResidualMode alphaMode,
                                         int alphaChangedPixelCount,
                                         long estimatedArchiveBytes,
                                         int complexityScore) {

        public boolean isBetterThan(@NotNull EncodedResidualPayload other) {
            if (this.estimatedArchiveBytes != other.estimatedArchiveBytes) {
                return this.estimatedArchiveBytes < other.estimatedArchiveBytes;
            }
            if (this.complexityScore != other.complexityScore) {
                return this.complexityScore < other.complexityScore;
            }
            return this.payloadBytes.length < other.payloadBytes.length;
        }

        @NotNull
        public AfmaResidualPayload toResidualMetadata() {
            return new AfmaResidualPayload(this.channels, this.codec, this.alphaMode, this.alphaChangedPixelCount);
        }

        @NotNull
        public AfmaSparsePayload toSparseMetadata(@NotNull String pixelsPath, int changedPixelCount,
                                                  @NotNull AfmaSparseLayoutCodec layoutCodec) {
            return new AfmaSparsePayload(pixelsPath, changedPixelCount, this.channels, layoutCodec, this.codec, this.alphaMode, this.alphaChangedPixelCount);
        }

    }

    public static final class ResidualSampleReader {

        private final byte[] payloadBytes;
        private final int channels;
        private final AfmaResidualCodec codec;
        private final int[] planeOffsets;
        private final int[] planeAccumulators;
        private int interleavedOffset;

        protected ResidualSampleReader(@NotNull byte[] payloadBytes, int offset, int sampleCount, int channels, @NotNull AfmaResidualCodec codec) {
            this.payloadBytes = payloadBytes;
            this.interleavedOffset = offset;
            this.channels = channels;
            this.codec = codec;
            this.planeOffsets = new int[channels];
            this.planeAccumulators = new int[channels];
            if (codec != AfmaResidualCodec.INTERLEAVED) {
                for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                    this.planeOffsets[channelIndex] = offset + (channelIndex * sampleCount);
                }
            }
        }

        public int readNextRgbIntoAbgr(int predictedAbgr) {
            int red = (((predictedAbgr) & 0xFF) + this.nextDelta(0)) & 0xFF;
            int green = (((predictedAbgr >> 8) & 0xFF) + this.nextDelta(1)) & 0xFF;
            int blue = (((predictedAbgr >> 16) & 0xFF) + this.nextDelta(2)) & 0xFF;
            return (predictedAbgr & 0xFF000000) | (blue << 16) | (green << 8) | red;
        }

        public int readNextRgbaIntoAbgr(int predictedAbgr) {
            int red = (((predictedAbgr) & 0xFF) + this.nextDelta(0)) & 0xFF;
            int green = (((predictedAbgr >> 8) & 0xFF) + this.nextDelta(1)) & 0xFF;
            int blue = (((predictedAbgr >> 16) & 0xFF) + this.nextDelta(2)) & 0xFF;
            int alpha = (((predictedAbgr >>> 24) & 0xFF) + this.nextDelta(3)) & 0xFF;
            return (alpha << 24) | (blue << 16) | (green << 8) | red;
        }

        public int readNextAlphaIntoAbgr(int predictedAbgr) {
            int alpha = (((predictedAbgr >>> 24) & 0xFF) + this.nextDelta(0)) & 0xFF;
            return (alpha << 24) | (predictedAbgr & 0x00FFFFFF);
        }

        protected int nextDelta(int channelIndex) {
            if (this.codec == AfmaResidualCodec.INTERLEAVED) {
                return this.payloadBytes[this.interleavedOffset++];
            }

            int encodedValue = this.payloadBytes[this.planeOffsets[channelIndex]++] & 0xFF;
            if (this.codec == AfmaResidualCodec.PLANAR_ZIGZAG_DELTA) {
                encodedValue = (this.planeAccumulators[channelIndex] + encodedValue) & 0xFF;
                this.planeAccumulators[channelIndex] = encodedValue;
            }
            return switch (this.codec) {
                case PLANAR -> (byte) encodedValue;
                case PLANAR_ZIGZAG, PLANAR_ZIGZAG_DELTA -> zigzagDecode(encodedValue);
                case INTERLEAVED -> throw new IllegalStateException("AFMA interleaved residual readers should not use planar decoding");
            };
        }

    }

}
