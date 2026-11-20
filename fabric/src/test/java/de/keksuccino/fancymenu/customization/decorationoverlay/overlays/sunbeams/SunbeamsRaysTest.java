package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams;

import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SunbeamsRaysTest {

    private static final int WIDTH = 480;
    private static final int HEIGHT = 270;
    private static final float GLOW = 0.75F;
    private static final float GLOW_RADIUS = 330.0F;

    /**
     * The mockup's rig, with the sun glow.
     */
    private static SunbeamsRig rig() {
        SunbeamsRig rig = new SunbeamsRig(-70.0F, -120.0F, WIDTH, HEIGHT);
        rig.setGlow(GLOW, GLOW_RADIUS);
        return rig;
    }

    private static SunbeamsRays rays(SunbeamsRig rig, float seconds) {
        SunbeamsRays rays = new SunbeamsRays();
        PixelRandom random = new PixelRandom(7);
        rays.setup(rig.fanWidth, 1.0F, random);
        for (float t = 0.0F; t < seconds; t += 1.0F / 60.0F) rays.update(1.0F / 60.0F, t, rig, 1.0F, 1.0F, random);
        return rays;
    }

    @Test
    void theMockupFanHoldsFourteenRays() {
        SunbeamsRays rays = new SunbeamsRays();
        rays.setup((float)Math.toRadians(67.5), 1.0F, new PixelRandom(1));
        assertEquals(14, rays.count());
        rays.setup((float)Math.toRadians(135.0), 1.0F, new PixelRandom(1));
        assertEquals(28, rays.count());
    }

    @Test
    void lightStepsStartAtTheirThresholds() {
        assertEquals(0, SunbeamsRays.step(0.0F));
        assertEquals(0, SunbeamsRays.step(0.139F));
        assertEquals(1, SunbeamsRays.step(0.14F));
        assertEquals(2, SunbeamsRays.step(0.3F));
        assertEquals(3, SunbeamsRays.step(0.48F));
        assertEquals(4, SunbeamsRays.step(0.68F));
        assertEquals(5, SunbeamsRays.step(0.9F));
        assertEquals(5, SunbeamsRays.step(3.0F));
    }

    @Test
    void someDirectionsShineAndOthersStayDark() {
        SunbeamsRig rig = rig();
        SunbeamsRays rays = rays(rig, 3.0F);
        int lit = 0;
        for (float s : rays.strength) {
            assertTrue(s >= 0.0F);
            if (s > 0.0F) lit++;
        }
        assertTrue((lit > SunbeamsRig.BINS / 10) && (lit < SunbeamsRig.BINS), "lit bins " + lit);
    }

    @Test
    void lightFadesWithTheDistanceFromTheSun() {
        SunbeamsRig rig = rig();
        SunbeamsRays rays = rays(rig, 2.0F);
        // Within one direction, light never grows with the distance
        Integer[] order = new Integer[WIDTH * HEIGHT];
        for (int j = 0; j < order.length; j++) order[j] = j;
        Arrays.sort(order, Comparator.comparingInt((Integer j) -> rig.bins[j]).thenComparingDouble(j -> rig.distances[j]));
        int checked = 0;
        for (int i = 1; i < order.length; i++) {
            int previous = order[i - 1];
            int current = order[i];
            if (rig.bins[previous] != rig.bins[current]) continue;
            assertTrue(rays.lightAt(rig, current) <= rays.lightAt(rig, previous) + 1.0E-5F);
            checked++;
        }
        assertTrue(checked > WIDTH * HEIGHT / 2);
        // Without rays and glow there is no light at all
        assertEquals(0.0F, new SunbeamsRays().lightAt(new SunbeamsRig(-70.0F, -120.0F, WIDTH, HEIGHT), 0));
    }

    @Test
    void theGlowLightsTheCornerNearTheSun() {
        SunbeamsRig rig = rig();
        SunbeamsRays rays = new SunbeamsRays();
        assertTrue(SunbeamsRays.step(rays.lightAt(rig, 0)) >= 1);
        assertEquals(0, SunbeamsRays.step(rays.lightAt(rig, WIDTH * HEIGHT - 1)));
    }

    @Test
    void paintedStepsMatchTheLightAndTheShade() {
        SunbeamsRig rig = rig();
        rig.buildShade(List.of(new CollisionAreaBounds(165, 92, 150, 18)));
        SunbeamsRays rays = rays(rig, 4.0F);
        int[] colors = SunbeamsPalette.NATURAL.light;
        byte[] levels = new byte[WIDTH * HEIGHT];
        int[] pixels = new int[WIDTH * HEIGHT];
        rays.paint(rig, 0, WIDTH * HEIGHT, true, levels, pixels, colors);
        for (int j = 0; j < WIDTH * HEIGHT; j++) {
            int expected = SunbeamsRays.step(rays.lightAt(rig, j) * SunbeamsRig.SHADE_LIGHT[rig.shade[j]]);
            assertEquals(expected, levels[j]);
            assertEquals(colors[expected], pixels[j]);
        }
        // No light on the face of the element, so its label stays clean
        assertEquals(0, levels[100 * WIDTH + 240]);
        assertEquals(0, pixels[100 * WIDTH + 240]);
        rays.paint(rig, 0, WIDTH * HEIGHT, false, levels, pixels, colors);
        assertEquals(SunbeamsRays.step(rays.lightAt(rig, 100 * WIDTH + 240)), levels[100 * WIDTH + 240]);
    }

    @Test
    void raysComeBackAtNewAnglesAfterTheyFadedOut() {
        SunbeamsRig rig = rig();
        SunbeamsRays early = rays(rig, 1.0F);
        float[] before = early.strength.clone();
        SunbeamsRays late = rays(rig, 40.0F);
        int different = 0;
        for (int b = 0; b < SunbeamsRig.BINS; b++) {
            if ((before[b] > 0.0F) != (late.strength[b] > 0.0F)) different++;
        }
        assertTrue(different > SunbeamsRig.BINS / 20, "changed bins " + different);
    }

}
