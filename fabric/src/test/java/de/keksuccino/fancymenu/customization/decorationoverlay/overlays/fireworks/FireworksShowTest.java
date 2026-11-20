package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireworksShowTest {

    private static final float STEP = 1.0F / 60.0F;

    private static FireworksShow burst(int shape, int seed) {
        FireworksShow show = new FireworksShow();
        show.burst(240.0F, 80.0F, shape, new PixelRandom(seed), 1.0F);
        return show;
    }

    @Test
    void everyShapeHasItsNumberOfStarsAndOneFlash() {
        assertEquals(26 + 14, burst(FireworksShow.SHAPE_DOUBLE_RING, 1).starCount);
        assertEquals(28 + 9, burst(FireworksShow.SHAPE_RING_WITH_CORE, 1).starCount);
        assertEquals(3 * 12, burst(FireworksShow.SHAPE_WILLOW, 1).starCount);
        assertEquals(36, burst(FireworksShow.SHAPE_HEART, 1).starCount);
        assertEquals(10 * 4 + 8, burst(FireworksShow.SHAPE_STAR, 1).starCount);
        assertEquals(44, burst(FireworksShow.SHAPE_GLITTER, 1).starCount);
        assertEquals(1, burst(FireworksShow.SHAPE_HEART, 1).flashCount);
    }

    @Test
    void ringStarsAreSpacedEvenlyAndFlyAtTheSameSpeed() {
        FireworksShow show = burst(FireworksShow.SHAPE_DOUBLE_RING, 4);
        float[] angles = new float[26];
        float speed = speed(show.stars[0]);
        for (int i = 0; i < 26; i++) {
            FireworksShow.Star star = show.stars[i];
            assertEquals(speed, speed(star), 1.0E-3F);
            angles[i] = (float)Math.atan2(star.vy, star.vx);
        }
        Arrays.sort(angles);
        float expectedGap = PixelMath.TAU / 26.0F;
        for (int i = 1; i < 26; i++) assertEquals(expectedGap, angles[i] - angles[i - 1], 0.081F, "gap " + i);
        assertTrue((speed >= 95.0F) && (speed < 135.0F));
    }

    @Test
    void theHeartKeepsItsShapeInsteadOfBecomingARing() {
        FireworksShow show = burst(FireworksShow.SHAPE_HEART, 2);
        // Star 0 starts at the dip between the lobes and flies up slowly, star 18 is the tip and flies down at the full speed
        FireworksShow.Star dip = show.stars[0];
        FireworksShow.Star tip = show.stars[18];
        assertEquals(0.0F, dip.vx, 1.0E-3F);
        assertTrue(dip.vy < 0.0F);
        assertEquals(0.0F, tip.vx, 1.0E-2F);
        assertTrue(tip.vy > 0.0F);
        assertEquals(5.0F / 17.0F, -dip.vy / tip.vy, 1.0E-3F);
        int ramp = dip.ramp;
        assertTrue((ramp == FireworksArt.RED) || (ramp == FireworksArt.PINK));
    }

    @Test
    void theStarShellHasFivePointsAtFullSpeedAndInnerCornersAt42Percent() {
        FireworksShow show = burst(FireworksShow.SHAPE_STAR, 3);
        float full = speed(show.stars[0]);
        for (int k = 0; k < 10; k++) {
            float corner = speed(show.stars[k * 4]);
            assertEquals(((k & 1) == 1) ? full * 0.42F : full, corner, 1.0E-2F, "corner " + k);
        }
        // The first point is straight up
        assertEquals(0.0F, show.stars[0].vx, 1.0E-2F);
        assertTrue(show.stars[0].vy < 0.0F);
        for (int i = 40; i < 48; i++) assertEquals(FireworksArt.GOLD, show.stars[i].ramp);
    }

    @Test
    void willowAndGlitterShellsHaveTheirOwnStars() {
        FireworksShow willow = burst(FireworksShow.SHAPE_WILLOW, 5);
        for (int i = 0; i < willow.starCount; i++) {
            FireworksShow.Star star = willow.stars[i];
            assertEquals(FireworksArt.GOLD, star.ramp);
            assertEquals(1.1F, star.drag);
            assertEquals(34.0F, star.gravity);
            assertEquals(0.2F, star.trail);
            assertTrue(star.maxLife >= 2.0F);
            assertEquals(i < 12, star.glitter, "star " + i);
        }
        FireworksShow glitter = burst(FireworksShow.SHAPE_GLITTER, 5);
        for (int i = 0; i < glitter.starCount; i++) assertTrue(glitter.stars[i].glitter);
    }

    @Test
    void rocketsBurstInTheUpperHalfOfTheScreen() {
        FireworksShow show = new FireworksShow();
        PixelRandom random = new PixelRandom(11);
        int bursts = 0;
        for (int frame = 0; frame < 60 * 30; frame++) {
            int flashesBefore = show.flashCount;
            show.update(STEP, random, 480, 270, 0.8F, 1.0F, true);
            for (int i = 0; i < show.rocketCount; i++) {
                FireworksShow.Rocket rocket = show.rockets[i];
                assertTrue((rocket.startX >= 480 * 0.15F) && (rocket.startX <= 480 * 0.85F));
                assertTrue((rocket.targetY >= 270 * 0.12F) && (rocket.targetY <= 270 * 0.5F));
            }
            if (show.flashCount > flashesBefore) {
                bursts++;
                FireworksShow.Flash flash = show.flashes[show.flashCount - 1];
                assertTrue(flash.y <= 270 * 0.5F + 2.0F, "burst at " + flash.y);
            }
        }
        // 0.8 rockets per second over 30 seconds
        assertTrue((bursts >= 22) && (bursts <= 25), bursts + " bursts");
    }

    @Test
    void starsBurnOutAndTheirObjectsGetReused() {
        FireworksShow show = burst(FireworksShow.SHAPE_DOUBLE_RING, 6);
        FireworksShow.Star first = show.stars[0];
        PixelRandom random = new PixelRandom(1);
        for (int frame = 0; frame < 60 * 3; frame++) show.update(STEP, random, 480, 270, 0.0F, 1.0F, true);
        assertEquals(0, show.starCount);
        assertEquals(0, show.flashCount);
        show.burst(100.0F, 50.0F, FireworksShow.SHAPE_GLITTER, random, 1.0F);
        boolean reused = false;
        for (int i = 0; i < show.starCount; i++) reused |= show.stars[i] == first;
        assertTrue(reused);
    }

    @Test
    void hiddenRocketsLeaveNoSparks() {
        FireworksShow show = new FireworksShow();
        PixelRandom random = new PixelRandom(2);
        show.launch(random, 480, 270);
        // Rockets climb for at least 0.9 seconds, so this one is still on its way after 40 frames
        for (int frame = 0; frame < 30; frame++) show.update(STEP, random, 480, 270, 0.0F, 1.0F, false);
        assertEquals(0, show.sparkCount);
        assertEquals(1, show.rocketCount);
        for (int frame = 0; frame < 10; frame++) show.update(STEP, random, 480, 270, 0.0F, 1.0F, true);
        assertTrue(show.sparkCount > 0);
        assertEquals(1, show.rocketCount);
    }

    private static float speed(FireworksShow.Star star) {
        return (float)Math.hypot(star.vx, star.vy);
    }

}
