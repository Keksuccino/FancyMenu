package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.jetbrains.annotations.NotNull;

/**
 * The wind strokes gusts draw across the screen, shared by the Leaves and the Cherry Blossoms overlay.<br>
 * The detailed look draws a curling stroke that writes itself along a fixed path (a gentle wave, a loop, a straight tail) and fades from
 * its tail. The simple look draws three plain dashes.
 */
public final class WindStrokes {

    /** Color of the strokes of the Leaves overlay */
    public static final int LEAVES_RGB = 0xF4F8FF;
    /** Seconds a curling stroke needs to write itself and fade out again */
    public static final float CURL_SECONDS = 0.45F + 78.0F / 150.0F;

    /** The path of a curling stroke as x/y offsets: a wave, a loop and a straight tail */
    private static final int[] CURL_X = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 48, 48, 49, 49, 48, 48, 46, 45, 44, 42, 42, 41, 41, 42, 42, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59};
    private static final int[] CURL_Y = {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -2, -3, -5, -6, -7, -8, -8, -8, -7, -6, -5, -3, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    /** Pixels the stroke head moves per second */
    private static final float WRITE_SPEED = 150.0F;

    private WindStrokes() {
    }

    /**
     * Draws a curling stroke {@code seconds} after it started. Its head writes the path, its tail follows 0.45 seconds later,
     * and the pixels fade in three steps from head to tail, with every second pixel left out in the faintest step.
     * Runs of pixels in a row are drawn as one quad.
     *
     * @param direction 1 for a stroke that runs to the right, -1 for one that runs to the left
     */
    public static void drawCurl(@NotNull PixelGraphics graphics, int originX, int y, int direction, float seconds, int rgb) {
        if (seconds <= 0.0F) return;
        int head = Math.min(CURL_X.length, PixelMath.floor(seconds * WRITE_SPEED));
        int tail = Math.max(0, PixelMath.floor((seconds - 0.45F) * WRITE_SPEED));
        if (tail >= head) return;
        int runX = 0;
        int runY = 0;
        int runLength = 0;
        int runAlpha = 0;
        for (int i = tail; i < head; i++) {
            float f = (i - tail) / (float)(head - tail);
            if ((f <= 0.35F) && ((i & 1) != 0)) continue;
            int alpha = (f > 0.7F) ? 220 : ((f > 0.35F) ? 150 : 80);
            int x = originX + direction * CURL_X[i];
            int py = y + CURL_Y[i];
            if ((runLength > 0) && (py == runY) && (alpha == runAlpha) && (x == runX + direction * runLength)) {
                runLength++;
                continue;
            }
            drawRun(graphics, runX, runY, runLength, direction, runAlpha, rgb);
            runX = x;
            runY = py;
            runLength = 1;
            runAlpha = alpha;
        }
        drawRun(graphics, runX, runY, runLength, direction, runAlpha, rgb);
    }

    private static void drawRun(@NotNull PixelGraphics graphics, int startX, int y, int length, int direction, int alpha, int rgb) {
        if (length <= 0) return;
        int left = (direction > 0) ? startX : (startX - length + 1);
        graphics.rect(left, y, length, 1, PixelMath.withAlpha(rgb, alpha));
    }

    /**
     * Draws the three dashes of the simple look {@code seconds} after the stroke started, for one second.
     */
    public static void drawDashes(@NotNull PixelGraphics graphics, int originX, int y, int direction, float seconds, int rgb) {
        if ((seconds <= 0.0F) || (seconds > 1.0F)) return;
        int color = PixelMath.withAlpha(rgb, (seconds < 0.7F) ? 150 : 80);
        for (int k = 0; k < 3; k++) {
            int start = originX + direction * (k * 11);
            graphics.rect((direction > 0) ? start : (start - 7), y - (k & 1), 8, 1, color);
        }
    }

}
