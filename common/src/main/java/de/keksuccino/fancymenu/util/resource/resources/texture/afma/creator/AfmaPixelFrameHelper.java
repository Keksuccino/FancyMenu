package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaCopyRect;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaRect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        for (int y = 0; y < previous.getHeight(); y++) {
            for (int x = 0; x < previous.getWidth(); x++) {
                if (previous.getPixelRGBA(x, y) != next.getPixelRGBA(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static @Nullable AfmaRect findDifferenceBounds(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next) {
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

    public static @NotNull AfmaPixelFrame crop(@NotNull AfmaPixelFrame source, int x, int y, int width, int height) {
        AfmaPixelFrame result = new AfmaPixelFrame(width, height, new int[width * height]);
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                result.setPixelRGBA(column, row, source.getPixelRGBA(x + column, y + row));
            }
        }
        return result;
    }

    public static @Nullable AfmaRect findDirtyBoundsAfterCopy(@NotNull AfmaPixelFrame previous, @NotNull AfmaPixelFrame next, @NotNull AfmaCopyRect copyRect) {
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

}
