package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.aurora;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.junit.jupiter.api.Test;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuroraCurtainTest {

    private static final int WIDTH = 480;
    private static final AuroraCurtain CURTAIN = new AuroraCurtain(79.0F, 10.0F, 0.011F, 0.17F, 72.0F, 0.0F, 1, 1.0F);

    private static int rows(float scaleY) {
        return (int)Math.ceil(CURTAIN.lowestStageRow() * scaleY) + 3;
    }

    @Test
    void theCurtainFitsIntoItsRows() {
        for (float scaleY : new float[]{240.0F / 270.0F, 1.0F, 2.0F}) {
            int rows = rows(scaleY);
            // One row more than the curtain needs, which has to stay empty
            int[] pixels = new int[WIDTH * (rows + 1)];
            for (float time = 0.0F; time < 60.0F; time += 2.5F) {
                CURTAIN.render(pixels, WIDTH, rows + 1, time, scaleY, AuroraPalette.NATURAL);
                for (int x = 0; x < WIDTH; x++) assertEquals(0, pixels[rows * WIDTH + x], "scale " + scaleY + " time " + time);
            }
        }
    }

    @Test
    void renderingIsRepeatableAndClearsWhatWasThere() {
        int rows = rows(1.0F);
        int[] first = new int[WIDTH * rows];
        CURTAIN.render(first, WIDTH, rows, 12.0F, 1.0F, AuroraPalette.NATURAL);
        int[] second = new int[WIDTH * rows];
        Arrays.fill(second, 0xFF123456);
        CURTAIN.render(second, WIDTH, rows, 12.0F, 1.0F, AuroraPalette.NATURAL);
        assertArrayEquals(first, second);
    }

    @Test
    void everyPixelIsAWashOrRayCombination() {
        int rows = rows(1.0F);
        int[] pixels = new int[WIDTH * rows];
        CURTAIN.render(pixels, WIDTH, rows, 33.0F, 1.0F, AuroraPalette.NATURAL);
        int painted = 0;
        for (int color : pixels) {
            if (color == 0) continue;
            painted++;
            boolean known = false;
            for (int c : AuroraPalette.NATURAL.composite) {
                if (c == color) {
                    known = true;
                    break;
                }
            }
            assertTrue(known, Integer.toHexString(color));
            // Flat steps, no opaque pixels: the sky always shows through
            assertTrue(PixelMath.alpha(color) < 255);
        }
        assertTrue(painted > WIDTH * 10, "painted " + painted);
    }

    @Test
    void theLowerEdgeIsTheBrightestPartOfARay() {
        int rows = rows(1.0F);
        int[] pixels = new int[WIDTH * rows];
        CURTAIN.render(pixels, WIDTH, rows, 5.0F, 1.0F, AuroraPalette.NATURAL);
        int columns = 0;
        for (int x = 0; x < WIDTH; x++) {
            int lowest = -1;
            int highest = -1;
            for (int y = rows - 1; y >= 0; y--) {
                if (PixelMath.alpha(pixels[y * WIDTH + x]) > 100) {
                    if (lowest < 0) lowest = y;
                    highest = y;
                }
            }
            if ((lowest < 0) || (lowest - highest < 10)) continue;
            columns++;
            assertTrue(PixelMath.alpha(pixels[lowest * WIDTH + x]) >= PixelMath.alpha(pixels[highest * WIDTH + x]), "column " + x);
        }
        assertTrue(columns > 50, "tall rays: " + columns);
    }

}
