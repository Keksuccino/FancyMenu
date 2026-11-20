package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringLightsProgramTest {

    private static final int DIM = StringLightsProgram.DIM;
    private static final int LIT = StringLightsProgram.LIT;
    private static final int BRIGHT = StringLightsProgram.BRIGHT;

    @Test
    void cycleSwitchesProgramEverySevenSeconds() {
        StringLightsProgram cycle = StringLightsProgram.CYCLE;
        assertAll(() -> assertEquals(StringLightsProgram.TWINKLE, cycle.resolve(0.0F)), () -> assertEquals(StringLightsProgram.TWINKLE, cycle.resolve(6.99F)), () -> assertEquals(StringLightsProgram.CHASE, cycle.resolve(7.0F)), () -> assertEquals(StringLightsProgram.ALTERNATE, cycle.resolve(14.5F)), () -> assertEquals(StringLightsProgram.BREATHE, cycle.resolve(21.0F)), () -> assertEquals(StringLightsProgram.TWINKLE, cycle.resolve(28.0F)), () -> assertEquals(StringLightsProgram.CHASE, StringLightsProgram.CHASE.resolve(1.0F)));
    }

    @Test
    void chaseRunsABrightBulbWithALitTailAlongTheString() {
        StringLightsProgram chase = StringLightsProgram.CHASE;
        assertAll(() -> assertEquals(BRIGHT, chase.level(0, 0, 0.0F)), () -> assertEquals(LIT, chase.level(1, 0, 0.0F)), () -> assertEquals(DIM, chase.level(2, 0, 0.0F)), () -> assertEquals(DIM, chase.level(3, 0, 0.0F)), () -> assertEquals(BRIGHT, chase.level(4, 0, 0.0F)));
        // One step later (seven steps per second) every bulb shows what its predecessor showed
        float step = 1.0F / 7.0F + 0.001F;
        for (int bulb = 1; bulb < 12; bulb++) {
            assertEquals(chase.level(bulb - 1, 0, 0.0F), chase.level(bulb, 0, step), "bulb " + bulb);
        }
    }

    @Test
    void alternateSwapsTheTwoHalves() {
        StringLightsProgram alternate = StringLightsProgram.ALTERNATE;
        for (int bulb = 0; bulb < 10; bulb++) {
            int now = alternate.level(bulb, 3, 0.0F);
            assertEquals(((bulb & 1) != 0) ? BRIGHT : DIM, now, "bulb " + bulb);
            assertEquals((now == BRIGHT) ? DIM : BRIGHT, alternate.level(bulb, 3, 0.7F), "bulb " + bulb + " after the swap");
        }
    }

    @Test
    void breatheBrightensAndDimsAllBulbsTogether() {
        StringLightsProgram breathe = StringLightsProgram.BREATHE;
        float[] seconds = {0.0F, 0.45F, 0.85F, 1.25F, 1.65F, 2.05F};
        int[] levels = {DIM, LIT, BRIGHT, BRIGHT, LIT, DIM};
        for (int i = 0; i < seconds.length; i++) {
            for (int bulb = 0; bulb < 5; bulb++) {
                assertEquals(levels[i], breathe.level(bulb, bulb, seconds[i]), "at " + seconds[i] + "s, bulb " + bulb);
            }
        }
    }

    @Test
    void twinkleUsesTheMockupHashAndIsMostlyLit() {
        StringLightsProgram twinkle = StringLightsProgram.TWINKLE;
        int[] counts = new int[3];
        for (int frame = 0; frame < 200; frame++) {
            for (int bulb = 0; bulb < 20; bulb++) {
                float h = PixelMath.hash(bulb + 2 * 40, frame);
                int expected = (h < 0.1F) ? DIM : ((h > 0.88F) ? BRIGHT : LIT);
                int level = twinkle.level(bulb, 2, frame / 4.0F + 0.01F);
                assertEquals(expected, level);
                counts[level]++;
            }
        }
        int total = counts[0] + counts[1] + counts[2];
        assertAll(() -> assertTrue(counts[LIT] > total * 0.7F), () -> assertTrue(counts[DIM] > total * 0.05F), () -> assertTrue(counts[BRIGHT] > total * 0.05F));
    }

}
