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
    void steadyProgramsKeepEveryBulbAtItsLevel() {
        for (int frame = 0; frame < 2000; frame++) {
            float seconds = frame * 0.037F;
            for (int bulb = 0; bulb < 30; bulb++) {
                int b = bulb;
                assertAll(() -> assertEquals(BRIGHT, StringLightsProgram.ON.level(b, 2, seconds)), () -> assertEquals(BRIGHT, StringLightsProgram.FULLY_LIT.level(b, 2, seconds)), () -> assertEquals(LIT, StringLightsProgram.HALF_LIT.level(b, 2, seconds)), () -> assertEquals(DIM, StringLightsProgram.OFF.level(b, 2, seconds)), () -> assertEquals(0.0F, StringLightsProgram.FULLY_LIT.sag(b, 2, seconds)), () -> assertEquals(0.0F, StringLightsProgram.HALF_LIT.sag(b, 2, seconds)), () -> assertEquals(0.0F, StringLightsProgram.OFF.sag(b, 2, seconds)));
            }
        }
    }

    @Test
    void onlyOnSags() {
        for (StringLightsProgram program : StringLightsProgram.values()) {
            if (program == StringLightsProgram.ON) continue;
            for (int frame = 0; frame < 600; frame++) {
                assertEquals(0.0F, program.sag(frame % 20, frame % 3, frame * 0.1F), program.getName());
            }
        }
    }

    @Test
    void onSagsSingleBulbsSlowlyAndOnlyNowAndThen() {
        StringLightsProgram on = StringLightsProgram.ON;
        int strings = 3;
        int bulbs = 20;
        float step = 1.0F / 30.0F;
        int frames = 18000;
        float[] previous = new float[strings * bulbs];
        boolean[] sagged = new boolean[strings * bulbs];
        int saggingSamples = 0;
        int mostSaggingAtOnce = 0;
        float steepestStep = 0.0F;
        for (int frame = 0; frame <= frames; frame++) {
            float seconds = frame * step;
            int saggingNow = 0;
            for (int string = 0; string < strings; string++) {
                for (int bulb = 0; bulb < bulbs; bulb++) {
                    int i = string * bulbs + bulb;
                    float sag = on.sag(bulb, string, seconds);
                    assertTrue((sag >= 0.0F) && (sag <= 1.0F), "sag " + sag + " of bulb " + i + " at " + seconds + "s");
                    if (frame > 0) steepestStep = Math.max(steepestStep, Math.abs(sag - previous[i]));
                    previous[i] = sag;
                    if (sag > 0.0F) {
                        sagged[i] = true;
                        saggingNow++;
                    }
                }
            }
            saggingSamples += saggingNow;
            mostSaggingAtOnce = Math.max(mostSaggingAtOnce, saggingNow);
        }
        float saggingShare = saggingSamples / (float)((frames + 1) * strings * bulbs);
        int mostAtOnce = mostSaggingAtOnce;
        float steepest = steepestStep;
        // Sags ease in and out over seconds, so they never jump between frames, not even where one slot ends and the next begins
        assertAll(() -> assertTrue(steepest < 0.05F, "steepest step " + steepest), () -> assertTrue((saggingShare > 0.05F) && (saggingShare < 0.2F), "sagging share " + saggingShare), () -> assertTrue(mostAtOnce < strings * bulbs / 3, "most sagging at once " + mostAtOnce));
        for (int i = 0; i < sagged.length; i++) {
            assertTrue(sagged[i], "bulb " + i + " sags now and then");
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
