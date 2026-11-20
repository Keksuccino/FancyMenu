package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.aurora;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuroraPaletteTest {

    @Test
    void theNaturalColorsGiveTheNaturalTones() {
        AuroraPalette palette = AuroraPalette.of(0xFF58F2A2, 0xFFAA52BC);
        assertSame(AuroraPalette.NATURAL, palette);
        assertArrayEquals(AuroraPalette.NATURAL_RAMP, palette.ramp);
        assertEquals(AuroraPalette.NATURAL_WASH_LOW, palette.washLow);
        assertEquals(AuroraPalette.NATURAL_WASH_HIGH, palette.washHigh);
    }

    @Test
    void hsvRoundTripsEveryColor() {
        for (int r = 0; r < 256; r += 15) {
            for (int g = 0; g < 256; g += 15) {
                for (int b = 0; b < 256; b += 15) {
                    int rgb = (r << 16) | (g << 8) | b;
                    float[] hsv = AuroraPalette.toHsv(rgb);
                    assertEquals(rgb, AuroraPalette.fromHsv(hsv[0], hsv[1], hsv[2]), Integer.toHexString(rgb));
                }
            }
        }
    }

    @Test
    void customColorsLandOnTheEdgeAndTheTip() {
        int edge = 0xFF4060;
        int tip = 0x3050FF;
        AuroraPalette palette = AuroraPalette.of(edge, tip);
        assertClose(edge, palette.ramp[2]);
        assertClose(tip, palette.ramp[8]);
    }

    @Test
    void oneColorForBothEndsGivesAOneHueAurora() {
        AuroraPalette palette = AuroraPalette.of(0xE0406A, 0xE0406A);
        float hue = AuroraPalette.toHsv(0xE0406A)[0];
        for (int tone : palette.ramp) {
            float[] hsv = AuroraPalette.toHsv(tone);
            if (hsv[1] < 0.05F) continue;
            assertTrue(Math.abs(AuroraPalette.hueDifference(hue, hsv[0])) < 2.5F, Integer.toHexString(tone));
        }
    }

    @Test
    void customRampsGoTheShorterWayAroundTheColorWheel() {
        // Red to violet goes through magenta, never through green
        AuroraPalette palette = AuroraPalette.of(0xFF2020, 0x9020FF);
        for (int i = 2; i <= 8; i++) {
            float hue = AuroraPalette.toHsv(palette.ramp[i])[0];
            assertTrue((hue >= 265.0F) || (hue <= 5.0F), "tone " + i + " has hue " + hue);
        }
    }

    @Test
    void highlightsStayLighterThanTheEdge() {
        AuroraPalette palette = AuroraPalette.of(0xFF8030, 0xC03060);
        float edgeSaturation = AuroraPalette.toHsv(palette.ramp[2])[1];
        assertTrue(AuroraPalette.toHsv(palette.ramp[0])[1] < edgeSaturation);
        assertTrue(AuroraPalette.toHsv(palette.ramp[1])[1] < edgeSaturation);
    }

    @Test
    void compositesPaintTheRayOverTheWash() {
        AuroraPalette palette = AuroraPalette.NATURAL;
        assertEquals(0, palette.composite[0]);
        int ray = AuroraPalette.rayState(4, 6);
        assertEquals(PixelMath.withAlpha(AuroraPalette.NATURAL_RAMP[4], 204), palette.composite[ray]);
        int wash = AuroraPalette.washState(false, true, false);
        assertEquals(PixelMath.withAlpha(AuroraPalette.NATURAL_WASH_LOW, 26), palette.composite[wash * AuroraPalette.RAY_STATES]);
        int both = palette.composite[wash * AuroraPalette.RAY_STATES + ray];
        assertEquals(PixelMath.blendOver(PixelMath.withAlpha(AuroraPalette.NATURAL_WASH_LOW, 26), PixelMath.withAlpha(AuroraPalette.NATURAL_RAMP[4], 204)), both);
        assertTrue(PixelMath.alpha(both) > 204);
    }

    private static void assertClose(int expected, int actual) {
        assertTrue(Math.abs(PixelMath.red(expected) - PixelMath.red(actual)) <= 1, Integer.toHexString(actual));
        assertTrue(Math.abs(PixelMath.green(expected) - PixelMath.green(actual)) <= 1, Integer.toHexString(actual));
        assertTrue(Math.abs(PixelMath.blue(expected) - PixelMath.blue(actual)) <= 1, Integer.toHexString(actual));
    }

}
