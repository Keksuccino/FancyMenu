package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinNativeImage;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public final class AfmaNativeImageHelper {

    private static final int RGBA_BYTES_PER_PIXEL = 4;

    private AfmaNativeImageHelper() {
    }

    public static void ensureSameSize(@NotNull NativeImage first, @NotNull NativeImage second) {
        if ((first.getWidth() != second.getWidth()) || (first.getHeight() != second.getHeight())) {
            throw new IllegalArgumentException("NativeImage dimensions do not match");
        }
    }

    public static boolean isIdentical(@NotNull NativeImage previous, @NotNull NativeImage next) {
        ensureSameSize(previous, next);
        int width = previous.getWidth();
        int height = previous.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (previous.getPixelRGBA(x, y) != next.getPixelRGBA(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Nullable
    public static AfmaRect findDifferenceBounds(@NotNull NativeImage previous, @NotNull NativeImage next) {
        ensureSameSize(previous, next);

        int width = previous.getWidth();
        int height = previous.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (previous.getPixelRGBA(x, y) == next.getPixelRGBA(x, y)) continue;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }

        if (maxX < minX || maxY < minY) {
            return null;
        }
        return new AfmaRect(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
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

    public static void blitPixels(@NotNull NativeImage target, int dstX, int dstY, int width, int height,
                                  @NotNull int[] pixels, int offset, int stride, boolean forceOpaqueAlpha) {
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

    public static void blitSparsePixels(@NotNull NativeImage target, int dstX, int dstY, int width, int height,
                                        @NotNull int[] maskPixels, int maskOffset, int maskStride,
                                        @NotNull int[] packedPixels, int packedOffset, int packedStride,
                                        int packedWidth, int packedHeight, boolean forceOpaqueAlpha) {
        long targetPixels = pixels(target);
        int targetWidth = target.getWidth();
        int packedIndex = 0;
        int packedCapacity = packedWidth * packedHeight;

        for (int row = 0; row < height; row++) {
            long targetOffset = offset(targetPixels, targetWidth, dstX, dstY + row);
            int maskRowStart = maskOffset + (row * maskStride);
            for (int column = 0; column < width; column++) {
                int maskColor = maskPixels[maskRowStart + column];
                if ((maskColor & 0x00FFFFFF) == 0) {
                    continue;
                }
                if (packedIndex >= packedCapacity) {
                    throw new IllegalStateException("AFMA sparse delta packed payload ended before the mask data");
                }

                int packedRow = packedIndex / packedWidth;
                int packedColumn = packedIndex % packedWidth;
                int color = packedPixels[packedOffset + (packedRow * packedStride) + packedColumn];
                if (forceOpaqueAlpha) {
                    color |= 0xFF000000;
                }
                MemoryUtil.memPutInt(targetOffset + ((long) column * RGBA_BYTES_PER_PIXEL), FastColor.ABGR32.fromArgb32(color));
                packedIndex++;
            }
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

    @Nullable
    public static AfmaRect findDirtyBoundsAfterCopy(@NotNull NativeImage previous, @NotNull NativeImage next, @NotNull AfmaCopyRect copyRect) {
        ensureSameSize(previous, next);

        int width = previous.getWidth();
        int height = previous.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int expected = previous.getPixelRGBA(x, y);
                if (inside(x, y, copyRect.getDstX(), copyRect.getDstY(), copyRect.getWidth(), copyRect.getHeight())) {
                    int srcX = copyRect.getSrcX() + (x - copyRect.getDstX());
                    int srcY = copyRect.getSrcY() + (y - copyRect.getDstY());
                    expected = previous.getPixelRGBA(srcX, srcY);
                }

                if (expected == next.getPixelRGBA(x, y)) continue;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }

        if (maxX < minX || maxY < minY) {
            return null;
        }
        return new AfmaRect(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < (left + width) && y < (top + height);
    }

    private static long pixels(@NotNull NativeImage image) {
        return ((IMixinNativeImage)(Object)image).get_pixels_FancyMenu();
    }

    private static long offset(long pixels, int imageWidth, int x, int y) {
        return pixels + ((((long)y * imageWidth) + x) * RGBA_BYTES_PER_PIXEL);
    }

}
