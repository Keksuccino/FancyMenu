package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

public final class AfmaResidualPayloadHelper {

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

    public static int expectedDenseResidualBytes(int width, int height, int channels) {
        if ((width <= 0) || (height <= 0) || !isValidChannelCount(channels)) {
            return 0;
        }
        long totalPixels = (long) width * (long) height;
        long totalBytes = totalPixels * channels;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    public static int expectedSparseMaskBytes(int width, int height) {
        if ((width <= 0) || (height <= 0)) {
            return 0;
        }
        long totalPixels = (long) width * (long) height;
        long totalBytes = (totalPixels + 7L) / 8L;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    public static int expectedSparseResidualBytes(int changedPixelCount, int channels) {
        if ((changedPixelCount < 0) || !isValidChannelCount(channels)) {
            return 0;
        }
        long totalBytes = (long) changedPixelCount * channels;
        return (totalBytes <= Integer.MAX_VALUE) ? (int) totalBytes : 0;
    }

    public static void setMaskBit(@NotNull byte[] maskBytes, int pixelIndex) {
        maskBytes[pixelIndex >>> 3] |= (byte) (1 << (7 - (pixelIndex & 7)));
    }

    public static boolean isMaskBitSet(@NotNull byte[] maskBytes, int pixelIndex) {
        return (maskBytes[pixelIndex >>> 3] & (1 << (7 - (pixelIndex & 7)))) != 0;
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

        red = (red + (residualBytes[offset++] & 0xFF)) & 0xFF;
        green = (green + (residualBytes[offset++] & 0xFF)) & 0xFF;
        blue = (blue + (residualBytes[offset++] & 0xFF)) & 0xFF;
        if (channels == RGBA_CHANNELS) {
            alpha = (alpha + (residualBytes[offset] & 0xFF)) & 0xFF;
        }

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
