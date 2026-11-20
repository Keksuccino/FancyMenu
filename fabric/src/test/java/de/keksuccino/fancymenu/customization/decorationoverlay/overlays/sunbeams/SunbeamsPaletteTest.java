package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SunbeamsPaletteTest {

    @Test
    void theNaturalSunlightGivesTheNaturalTones() {
        SunbeamsPalette palette = SunbeamsPalette.of(0xFFFFE9A6);
        assertSame(SunbeamsPalette.NATURAL, palette);
        int[] tones = {0xFFEDB4, 0xFFE9A6, 0xFFEBAA, 0xFFF2C4, 0xFFFAE2};
        int[] alphas = {28, 48, 70, 94, 122};
        assertEquals(0, palette.light[0]);
        for (int l = 1; l <= 5; l++) assertEquals(PixelMath.withAlpha(tones[l - 1], alphas[l - 1]), palette.light[l]);
        assertEquals(PixelMath.withAlpha(0xFFE08A, 150), palette.rim[1]);
        assertEquals(PixelMath.withAlpha(0xFFE08A, 255), palette.rim[5]);
        assertEquals(PixelMath.withAlpha(0xFFE08A, 89), palette.rimBevel[5]);
        assertEquals(PixelMath.withAlpha(0xFFF1C4, 255), palette.moteDot[5]);
    }

    @Test
    void anotherSunlightKeepsItsHueAndTheToneDifferences() {
        int blue = 0x9EC4FF;
        SunbeamsPalette palette = SunbeamsPalette.of(blue);
        float[] reference = hsv(blue);
        float lastSaturation = Float.MAX_VALUE;
        // From the haze (level 2 is the sunlight color itself) to the core the tones get whiter
        for (int l = 2; l <= 5; l++) {
            float[] tone = hsv(palette.light[l]);
            assertEquals(reference[0], tone[0], 3.0F, "level " + l);
            assertEquals(reference[2], tone[2], 0.01F, "level " + l);
            if (l > 2) assertTrue(tone[1] < lastSaturation, "level " + l);
            lastSaturation = tone[1];
        }
        // The lit edges are deeper than the light
        assertTrue(hsv(palette.rim[5])[1] > reference[1]);
        assertEquals(reference[0], hsv(palette.rim[5])[0], 3.0F);
    }

    @Test
    void tintLeavesTheNaturalSunlightAlone() {
        assertEquals(0xFFF2C4, SunbeamsPalette.tint(0xFFF2C4, SunbeamsPalette.NATURAL_LIGHT));
        // White light only gets white tones
        int white = SunbeamsPalette.tint(0xFFE08A, 0xFFFFFF) & 0xFFFFFF;
        assertEquals(0xFFFFFF, white);
    }

    private static float[] hsv(int argb) {
        float r = PixelMath.red(argb) / 255.0F;
        float g = PixelMath.green(argb) / 255.0F;
        float b = PixelMath.blue(argb) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue = 0.0F;
        if (delta > 0.0F) {
            if (max == r) hue = 60.0F * (((g - b) / delta) % 6.0F);
            else if (max == g) hue = 60.0F * ((b - r) / delta + 2.0F);
            else hue = 60.0F * ((r - g) / delta + 4.0F);
        }
        if (hue < 0.0F) hue += 360.0F;
        return new float[]{hue, (max > 0.0F) ? delta / max : 0.0F, max};
    }

}
