package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinNativeImage;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

public final class AfmaNativeImageHelper {

    private static final int RGBA_BYTES_PER_PIXEL = 4;

    private AfmaNativeImageHelper() {
    }

    @NotNull
    public static NativeImage crop(@NotNull NativeImage source, int x, int y, int width, int height) {
        NativeImage result = new NativeImage(width, height, false);
        copyRect(source, x, y, result, 0, 0, width, height);
        return result;
    }

    public static void copyRect(@NotNull NativeImage source, int srcX, int srcY, @NotNull NativeImage target, int dstX, int dstY, int width, int height) {
        long sourcePixels = pixels(source);
        long targetPixels = pixels(target);

        int sourceWidth = source.getWidth();
        int targetWidth = target.getWidth();
        long rowSize = (long)width * RGBA_BYTES_PER_PIXEL;

        for (int row = 0; row < height; row++) {
            long sourceOffset = offset(sourcePixels, sourceWidth, srcX, srcY + row);
            long targetOffset = offset(targetPixels, targetWidth, dstX, dstY + row);
            MemoryUtil.memCopy(sourceOffset, targetOffset, rowSize);
        }
    }

    public static void blitPixels(@NotNull NativeImage target, int dstX, int dstY, int width, int height, @NotNull int[] pixels, int offset, int stride, boolean forceOpaqueAlpha) {
        long targetPixels = pixels(target);
        int targetWidth = target.getWidth();

        for (int row = 0; row < height; row++) {
            long targetOffset = offset(targetPixels, targetWidth, dstX, dstY + row);
            int sourceRowStart = offset + (row * stride);
            for (int column = 0; column < width; column++) {
                int color = pixels[sourceRowStart + column];
                if (forceOpaqueAlpha) {
                    color |= 0xFF000000;
                }
                MemoryUtil.memPutInt(targetOffset + ((long) column * RGBA_BYTES_PER_PIXEL), FastColor.ABGR32.fromArgb32(color));
            }
        }
    }

    public static void blitInterleavedBytes(@NotNull NativeImage target, int dstX, int dstY, int width, int height,
                                            @NotNull byte[] rawBytes, int rawOffset, int rawLength, int channels) {
        int expectedBytes = AfmaBlockInterPayloadHelper.expectedRawTileBytes(width, height, channels);
        if ((expectedBytes <= 0) || (rawLength != expectedBytes) || rawOffset < 0
                || ((long) rawOffset + (long) rawLength) > rawBytes.length) {
            throw new IllegalStateException("AFMA raw pixel payload size does not match the descriptor");
        }

        long targetPixels = pixels(target);
        int targetWidth = target.getWidth();
        int rawIndex = rawOffset;
        for (int row = 0; row < height; row++) {
            long targetOffset = offset(targetPixels, targetWidth, dstX, dstY + row);
            for (int column = 0; column < width; column++) {
                int red = rawBytes[rawIndex++] & 0xFF;
                int green = rawBytes[rawIndex++] & 0xFF;
                int blue = rawBytes[rawIndex++] & 0xFF;
                int alpha = (channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) ? (rawBytes[rawIndex++] & 0xFF) : 0xFF;
                MemoryUtil.memPutInt(targetOffset + ((long) column * RGBA_BYTES_PER_PIXEL), FastColor.ABGR32.color(alpha, blue, green, red));
            }
        }

        if (rawIndex != (rawOffset + expectedBytes)) {
            throw new IllegalStateException("AFMA raw pixel payload ended early");
        }
    }

    public static void applyResidualBytes(@NotNull NativeImage target, int dstX, int dstY, int width, int height, @NotNull byte[] residualBytes, int residualOffset, int residualLength, int channels) {
        int expectedBytes = AfmaResidualPayloadHelper.expectedDenseResidualBytes(width, height, channels);
        if ((expectedBytes <= 0) || (residualLength != expectedBytes) || residualOffset < 0
                || ((long) residualOffset + (long) residualLength) > residualBytes.length) {
            throw new IllegalStateException("AFMA residual payload size does not match the frame descriptor");
        }

        long targetPixels = pixels(target);
        int targetWidth = target.getWidth();
        int residualIndex = residualOffset;
        for (int row = 0; row < height; row++) {
            long targetOffset = offset(targetPixels, targetWidth, dstX, dstY + row);
            for (int column = 0; column < width; column++) {
                long pixelOffset = targetOffset + ((long) column * RGBA_BYTES_PER_PIXEL);
                int predictedAbgr = MemoryUtil.memGetInt(pixelOffset);
                int red = (((predictedAbgr) & 0xFF) + (residualBytes[residualIndex++] & 0xFF)) & 0xFF;
                int green = (((predictedAbgr >> 8) & 0xFF) + (residualBytes[residualIndex++] & 0xFF)) & 0xFF;
                int blue = (((predictedAbgr >> 16) & 0xFF) + (residualBytes[residualIndex++] & 0xFF)) & 0xFF;
                int alpha = (predictedAbgr >>> 24) & 0xFF;
                if (channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) {
                    alpha = (alpha + (residualBytes[residualIndex++] & 0xFF)) & 0xFF;
                }
                MemoryUtil.memPutInt(pixelOffset, FastColor.ABGR32.color(alpha, blue, green, red));
            }
        }
    }

    public static void applySparseResidualBytes(@NotNull NativeImage target, int dstX, int dstY, int width, int height, @NotNull byte[] maskBytes, int maskOffset, int maskLength, @NotNull byte[] residualBytes, int residualOffset, int residualLength, int changedPixelCount, int channels) {
        int expectedMaskBytes = AfmaResidualPayloadHelper.expectedSparseMaskBytes(width, height);
        if ((expectedMaskBytes <= 0) || (maskLength != expectedMaskBytes) || maskOffset < 0
                || ((long) maskOffset + (long) maskLength) > maskBytes.length) {
            throw new IllegalStateException("AFMA sparse mask payload size does not match the frame descriptor");
        }
        int expectedResidualBytes = AfmaResidualPayloadHelper.expectedSparseResidualBytes(changedPixelCount, channels);
        if ((expectedResidualBytes <= 0) || (residualLength != expectedResidualBytes) || residualOffset < 0
                || ((long) residualOffset + (long) residualLength) > residualBytes.length) {
            throw new IllegalStateException("AFMA sparse residual payload size does not match the frame descriptor");
        }

        long targetPixels = pixels(target);
        int targetWidth = target.getWidth();
        int residualIndex = residualOffset;
        int bitIndex = 0;
        for (int row = 0; row < height; row++) {
            long targetOffset = offset(targetPixels, targetWidth, dstX, dstY + row);
            for (int column = 0; column < width; column++, bitIndex++) {
                if ((maskBytes[maskOffset + (bitIndex >>> 3)] & (1 << (7 - (bitIndex & 7)))) == 0) {
                    continue;
                }

                long pixelOffset = targetOffset + ((long) column * RGBA_BYTES_PER_PIXEL);
                int predictedAbgr = MemoryUtil.memGetInt(pixelOffset);
                int red = (((predictedAbgr) & 0xFF) + (residualBytes[residualIndex++] & 0xFF)) & 0xFF;
                int green = (((predictedAbgr >> 8) & 0xFF) + (residualBytes[residualIndex++] & 0xFF)) & 0xFF;
                int blue = (((predictedAbgr >> 16) & 0xFF) + (residualBytes[residualIndex++] & 0xFF)) & 0xFF;
                int alpha = (predictedAbgr >>> 24) & 0xFF;
                if (channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) {
                    alpha = (alpha + (residualBytes[residualIndex++] & 0xFF)) & 0xFF;
                }
                MemoryUtil.memPutInt(pixelOffset, FastColor.ABGR32.color(alpha, blue, green, red));
            }
        }
        if (residualIndex != (residualOffset + expectedResidualBytes)) {
            throw new IllegalStateException("AFMA sparse residual payload ended before the mask data");
        }
    }

    public static void copyRectMemmove(@NotNull NativeImage image, @NotNull AfmaCopyRect copyRect) {
        long pixels = pixels(image);
        int imageWidth = image.getWidth();
        long rowSize = (long)copyRect.getWidth() * RGBA_BYTES_PER_PIXEL;
        int startRow = 0;
        int endRow = copyRect.getHeight();
        int rowStep = 1;

        // Copy from bottom to top when moving downward so source rows are not overwritten
        // before they are read.
        if (copyRect.getDstY() > copyRect.getSrcY()) {
            startRow = copyRect.getHeight() - 1;
            endRow = -1;
            rowStep = -1;
        }

        long scratchRow = MemoryUtil.nmemAlloc(rowSize);
        try {
            for (int row = startRow; row != endRow; row += rowStep) {
                long sourceOffset = offset(pixels, imageWidth, copyRect.getSrcX(), copyRect.getSrcY() + row);
                long targetOffset = offset(pixels, imageWidth, copyRect.getDstX(), copyRect.getDstY() + row);
                MemoryUtil.memCopy(sourceOffset, scratchRow, rowSize);
                MemoryUtil.memCopy(scratchRow, targetOffset, rowSize);
            }
        } finally {
            MemoryUtil.nmemFree(scratchRow);
        }
    }

    private static long pixels(@NotNull NativeImage image) {
        return ((IMixinNativeImage)(Object)image).get_pixels_FancyMenu();
    }

    private static long offset(long pixels, int imageWidth, int x, int y) {
        return pixels + ((((long)y * imageWidth) + x) * RGBA_BYTES_PER_PIXEL);
    }

}
