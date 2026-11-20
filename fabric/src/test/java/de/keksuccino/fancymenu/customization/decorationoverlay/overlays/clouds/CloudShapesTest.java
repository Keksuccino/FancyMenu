package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.clouds;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudShapesTest {

    @Test
    void theSameSeedBuildsTheSameCloud() {
        PixelSprite first = CloudShapes.cumulus(new PixelRandom(77), 120, 44, 5);
        PixelSprite second = CloudShapes.cumulus(new PixelRandom(77), 120, 44, 5);
        assertEquals(codes(first), codes(second));
    }

    @Test
    void cloudsOnlyUseTheirTones() {
        for (int tones = 3; tones <= 5; tones++) {
            for (int seed = 0; seed < 12; seed++) {
                PixelSprite cloud = CloudShapes.cumulus(new PixelRandom(seed), 40 + seed * 9, 14 + seed * 3, tones);
                for (int y = 0; y < cloud.height(); y++) {
                    for (int x = 0; x < cloud.width(); x++) {
                        char c = cloud.codeAt(x, y);
                        assertTrue((c == '.') || ((c >= 'a') && (c < 'a' + tones)), "tones " + tones + " code " + c);
                    }
                }
            }
        }
    }

    @Test
    void cloudsHaveASunlitTopAndAFullBase() {
        for (int seed = 0; seed < 12; seed++) {
            PixelSprite cloud = CloudShapes.cumulus(new PixelRandom(seed), 104 + seed * 4, 40, 5);
            int filledColumns = 0;
            for (int x = 0; x < cloud.width(); x++) {
                for (int y = 0; y < cloud.height(); y++) {
                    char c = cloud.codeAt(x, y);
                    if (c == '.') continue;
                    filledColumns++;
                    // The top edge catches the light
                    assertTrue(c <= 'b', "seed " + seed + " column " + x + " starts with " + c);
                    break;
                }
            }
            assertTrue(filledColumns > cloud.width() * 0.8F, "seed " + seed);
            // The flat base is the darkest part
            int bottomDark = 0;
            int bottomFilled = 0;
            for (int x = 0; x < cloud.width(); x++) {
                char c = cloud.codeAt(x, cloud.height() - 3);
                if (c == '.') continue;
                bottomFilled++;
                if (c >= 'd') bottomDark++;
            }
            assertTrue(bottomDark > bottomFilled / 2, "seed " + seed);
        }
    }

    @Test
    void cloudsHaveNoStrayPixels() {
        for (int seed = 0; seed < 12; seed++) {
            PixelSprite cloud = CloudShapes.cumulus(new PixelRandom(seed), 90, 32, 5);
            for (int y = 0; y < cloud.height(); y++) {
                for (int x = 0; x < cloud.width(); x++) {
                    if (cloud.codeAt(x, y) == '.') continue;
                    boolean neighbor = false;
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int nx = x + d[0];
                        int ny = y + d[1];
                        if ((nx >= 0) && (ny >= 0) && (nx < cloud.width()) && (ny < cloud.height()) && (cloud.codeAt(nx, ny) != '.')) neighbor = true;
                    }
                    assertTrue(neighbor, "seed " + seed + " pixel " + x + "," + y);
                }
            }
        }
    }

    @Test
    void wispsAreThinStrokes() {
        for (int seed = 0; seed < 12; seed++) {
            PixelSprite wisp = CloudShapes.wisp(new PixelRandom(seed), 36 + seed * 3);
            assertEquals(CloudShapes.WISP_HEIGHT, wisp.height());
            int strokes = 0;
            for (int y = 0; y < wisp.height(); y++) {
                for (int x = 0; x < wisp.width(); x++) {
                    char c = wisp.codeAt(x, y);
                    assertTrue((c == '.') || (c == 'a') || (c == 'b'));
                    if (c == 'a') strokes++;
                }
            }
            assertTrue(strokes > wisp.width() * 0.6F, "seed " + seed);
        }
    }

    private static String codes(PixelSprite sprite) {
        StringBuilder builder = new StringBuilder();
        for (int y = 0; y < sprite.height(); y++) {
            for (int x = 0; x < sprite.width(); x++) builder.append(sprite.codeAt(x, y));
            builder.append('\n');
        }
        return builder.toString();
    }

}
