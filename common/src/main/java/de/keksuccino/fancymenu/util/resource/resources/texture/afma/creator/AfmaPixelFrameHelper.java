package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public final class AfmaPixelFrameHelper {

    private AfmaPixelFrameHelper() {
    }

    public static void ensureSameSize(@NotNull AfmaPixelFrame first, @NotNull AfmaPixelFrame second) {
        if ((first.getWidth() != second.getWidth()) || (first.getHeight() != second.getHeight())) {
            throw new IllegalArgumentException("AFMA frame dimensions do not match");
        }
    }

    public static boolean isIdentical(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next) {
        ensureSameSize(previous, next);
        return Arrays.equals(previous.getPixelsUnsafe(), next.getPixelsUnsafe());
    }

    public static @Nullable AfmaRect findDifferenceBounds(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next) {
        ensureSameSize(previous, next);

        int width = previous.getWidth();
        int height = previous.getHeight();
        int[] previousPixels = previous.getPixelsUnsafe();
        int[] nextPixels = next.getPixelsUnsafe();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                if (previousPixels[rowOffset + x] == nextPixels[rowOffset + x]) continue;
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

    public static @NotNull AfmaPixelFrame crop(@NotNull AfmaPixelFrame source, int x, int y, int width, int height) {
        int[] sourcePixels = source.getPixelsUnsafe();
        int sourceWidth = source.getWidth();
        int[] croppedPixels = new int[width * height];
        for (int row = 0; row < height; row++) {
            System.arraycopy(sourcePixels, ((y + row) * sourceWidth) + x, croppedPixels, row * width, width);
        }
        return new AfmaPixelFrame(width, height, croppedPixels);
    }

    public static @Nullable AfmaRect findDirtyBoundsAfterCopy(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next, @NotNull AfmaCopyRect copyRect) {
        ensureSameSize(previous, next);

        int width = previous.getWidth();
        int height = previous.getHeight();
        int[] previousPixels = previous.getPixelsUnsafe();
        int[] nextPixels = next.getPixelsUnsafe();
        int dstLeft = copyRect.getDstX();
        int dstTop = copyRect.getDstY();
        int dstRight = dstLeft + copyRect.getWidth();
        int dstBottom = dstTop + copyRect.getHeight();
        int srcLeft = copyRect.getSrcX();
        int srcTop = copyRect.getSrcY();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            boolean copiedRow = (y >= dstTop) && (y < dstBottom);
            int copiedSourceRowOffset = copiedRow ? ((srcTop + (y - dstTop)) * width) : 0;
            for (int x = 0; x < width; x++) {
                int pixelIndex = rowOffset + x;
                int expected = previousPixels[pixelIndex];
                if (copiedRow && (x >= dstLeft) && (x < dstRight)) {
                    expected = previousPixels[copiedSourceRowOffset + srcLeft + (x - dstLeft)];
                }

                if (expected == nextPixels[pixelIndex]) continue;
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

    public static void applyCopyRect(@NotNull int[] pixels, int frameWidth, @NotNull AfmaCopyRect copyRect) {
        int startRow = 0;
        int endRow = copyRect.getHeight();
        int rowStep = 1;
        if (copyRect.getDstY() > copyRect.getSrcY()) {
            startRow = copyRect.getHeight() - 1;
            endRow = -1;
            rowStep = -1;
        }

        for (int row = startRow; row != endRow; row += rowStep) {
            int sourceOffset = ((copyRect.getSrcY() + row) * frameWidth) + copyRect.getSrcX();
            int targetOffset = ((copyRect.getDstY() + row) * frameWidth) + copyRect.getDstX();
            System.arraycopy(pixels, sourceOffset, pixels, targetOffset, copyRect.getWidth());
        }
    }

    public static void applyCopyRects(@NotNull int[] pixels, int frameWidth, @NotNull List<AfmaCopyRect> copyRects) {
        for (AfmaCopyRect copyRect : copyRects) {
            applyCopyRect(pixels, frameWidth, copyRect);
        }
    }

}
