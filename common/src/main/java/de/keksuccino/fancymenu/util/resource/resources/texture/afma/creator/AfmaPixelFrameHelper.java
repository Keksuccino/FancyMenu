package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        return new AfmaFramePairAnalysis(previous, next).isIdentical();
    }

    public static @Nullable AfmaRect findDifferenceBounds(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next) {
        return new AfmaFramePairAnalysis(previous, next).differenceBounds();
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
        return new AfmaFramePairAnalysis(previous, next).findDirtyBoundsAfterCopy(copyRect);
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
