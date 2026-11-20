package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringLightsArtTest {

    /**
     * The bulb palettes of the mockup (warm white, then red, green, blue, amber, pink), per level: h, l, b, d of the detailed bulb,
     * then b, h, d of the simple lamp.
     */
    private static final int[][][] MOCKUP_BULBS = {
            {{0x997E55, 0x8E7551, 0x725F47, 0x503925, 0x826C4D, 0x997E55, 0x5A4027}, {0xFFFFFF, 0xFFF3C4, 0xFFD27A, 0xE09A3A, 0xFFD27A, 0xFFF3C4, 0xE09A3A}, {0xFFFFFF, 0xFFFAE4, 0xFFF3C4, 0xFFD27A, 0xFFF3C4, 0xFFFFFF, 0xFFD27A}},
            {{0x913234, 0x863033, 0x6D2A30, 0x3F1524, 0x7B2E32, 0x913234, 0x471626}, {0xFFFFFF, 0xFF9A8A, 0xF0483E, 0xA82238, 0xF0483E, 0xFF9A8A, 0xA82238}, {0xFFFFFF, 0xFFD2CA, 0xFF9A8A, 0xF0483E, 0xFF9A8A, 0xFFFFFF, 0xF0483E}},
            {{0x2E7947, 0x2C7044, 0x285B3D, 0x16322C, 0x2A6741, 0x2E7947, 0x16382F}, {0xFFFFFF, 0xA8F08A, 0x3CC860, 0x1E8452, 0x3CC860, 0xA8F08A, 0x1E8452}, {0xFFFFFF, 0xD8F8CA, 0xA8F08A, 0x3CC860, 0xA8F08A, 0xFFFFFF, 0x3CC860}},
            {{0x2F5896, 0x2D528C, 0x294474, 0x192248, 0x2B4C82, 0x2F5896, 0x1A2550}, {0xFFFFFF, 0x9AD2FF, 0x3E8CF0, 0x2A4EB0, 0x3E8CF0, 0x9AD2FF, 0x2A4EB0}, {0xFFFFFF, 0xD2EBFF, 0x9AD2FF, 0x3E8CF0, 0x9AD2FF, 0xFFFFFF, 0x3E8CF0}},
            {{0x997633, 0x8E6D32, 0x725930, 0x4E2F1D, 0x826431, 0x997633, 0x58341D}, {0xFFFFFF, 0xFFE9A0, 0xFFC23C, 0xD97A1E, 0xFFC23C, 0xFFE9A0, 0xD97A1E}, {0xFFFFFF, 0xFFF5D4, 0xFFE9A0, 0xFFC23C, 0xFFE9A0, 0xFFFFFF, 0xFFC23C}},
            {{0x913D6E, 0x863A68, 0x6D3259, 0x3F1839, 0x7B3762, 0x913D6E, 0x471A3E}, {0xFFFFFF, 0xFFB0DA, 0xF05CA8, 0xA82E7E, 0xF05CA8, 0xFFB0DA, 0xA82E7E}, {0xFFFFFF, 0xFFDBEE, 0xFFB0DA, 0xF05CA8, 0xFFB0DA, 0xFFFFFF, 0xF05CA8}}
    };

    @Test
    void bulbPalettesMatchTheMockup() {
        StringLightsArt.BulbRamp[] ramps = {StringLightsArt.WARM, StringLightsArt.CHRISTMAS[0], StringLightsArt.CHRISTMAS[1], StringLightsArt.CHRISTMAS[2], StringLightsArt.CHRISTMAS[3], StringLightsArt.CHRISTMAS[4]};
        for (int r = 0; r < ramps.length; r++) {
            for (int level = 0; level < 3; level++) {
                int[] expected = MOCKUP_BULBS[r][level];
                PixelPalette bulb = ramps[r].bulb[level];
                PixelPalette lamp = ramps[r].simpleBulb[level];
                String where = "ramp " + r + " level " + level;
                assertColor(expected[0], bulb.color('h'), where + " h");
                assertColor(expected[1], bulb.color('l'), where + " l");
                assertColor(expected[2], bulb.color('b'), where + " b");
                assertColor(expected[3], bulb.color('d'), where + " d");
                assertColor(0x1E2A22, bulb.color('k'), where + " socket");
                assertColor(expected[4], lamp.color('b'), where + " simple b");
                assertColor(expected[5], lamp.color('h'), where + " simple h");
                assertColor(expected[6], lamp.color('d'), where + " simple d");
            }
        }
    }

    @Test
    void dimBulbsHaveNoHaloAndBrightOnesTheStrongerOne() {
        StringLightsArt.BulbRamp warm = StringLightsArt.WARM;
        assertAll(() -> assertNull(warm.halo[0]), () -> assertEquals(52, warm.halo[1].color('a') >>> 24), () -> assertEquals(84, warm.halo[2].color('a') >>> 24), () -> assertEquals(62, warm.simpleHalo[1].color('a') >>> 24), () -> assertEquals(88, warm.simpleHalo[2].color('a') >>> 24), () -> assertEquals(0xFFD27A, warm.halo[2].color('b') & 0xFFFFFF));
    }

    @Test
    void lightColorsGetTheirRamps() {
        StringLightsArt.BulbRamp custom = StringLightsArt.rampOf(0xFF3366CC);
        StringLightsArt.BulbRamp translucent = StringLightsArt.rampOf(0x803366CC);
        assertAll(() -> assertSame(StringLightsArt.WARM, StringLightsArt.rampOf(0xFFFFD27A)), () -> assertSame(StringLightsArt.WARM, StringLightsArt.rampOf(0x00FFD27A)), () -> assertNotSame(StringLightsArt.WARM, StringLightsArt.rampOf(0x80FFD27A)), () -> assertEquals(0x3366CC, custom.base), () -> assertEquals(-1, custom.tint), () -> assertEquals(0x80FFFFFF, translucent.tint), () -> assertTrue(brightness(custom.bulb[1].color('l')) > brightness(custom.bulb[1].color('b'))), () -> assertTrue(brightness(custom.bulb[1].color('d')) < brightness(custom.bulb[1].color('b'))), () -> assertTrue(brightness(custom.bulb[0].color('b')) < brightness(custom.bulb[1].color('b'))));
    }

    private static int brightness(int argb) {
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
    }

    /**
     * Colors mixed in floats can round one step away from the mockup's doubles.
     */
    private static void assertColor(int expectedRgb, int actualArgb, String where) {
        assertEquals(255, actualArgb >>> 24, where + " alpha");
        for (int shift = 0; shift <= 16; shift += 8) {
            int expected = (expectedRgb >> shift) & 0xFF;
            int actual = (actualArgb >> shift) & 0xFF;
            assertTrue(Math.abs(expected - actual) <= 1, where + ": expected " + Integer.toHexString(expectedRgb) + " but was " + Integer.toHexString(actualArgb & 0xFFFFFF));
        }
    }

}
