package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams;

import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SunbeamsRigTest {

    private static final int WIDTH = 480;
    private static final int HEIGHT = 270;
    /** The mockup's sun, just outside the top left corner */
    private static final float SUN_X = -70.0F;
    private static final float SUN_Y = -120.0F;
    private static final float MARGIN = 0.343F * (float)Math.hypot(WIDTH, HEIGHT);

    @Test
    void theDefaultSunSitsWhereTheMockupHasIt() {
        float[] sun = new float[2];
        SunbeamsRig.placeSun(SunbeamsSide.TOP, -50.0F, WIDTH, HEIGHT, MARGIN, sun);
        assertEquals(SUN_X, sun[0], 8.0F);
        assertEquals(SUN_Y, sun[1], 8.0F);
    }

    @Test
    void theSunAlwaysSitsOutsideOfTheScreenOnItsSide() {
        float[] sun = new float[2];
        for (SunbeamsSide side : SunbeamsSide.values()) {
            for (int angle = -80; angle <= 80; angle += 10) {
                SunbeamsRig.placeSun(side, angle, WIDTH, HEIGHT, MARGIN, sun);
                boolean outside = (sun[0] < 0) || (sun[0] > WIDTH) || (sun[1] < 0) || (sun[1] > HEIGHT);
                assertTrue(outside, side + " " + angle);
                // At 0 degrees it sits straight behind the middle of its side
                if (angle == 0) {
                    if (side.outwardX == 0) assertEquals(WIDTH / 2.0F, sun[0], 0.01F);
                    else assertEquals(HEIGHT / 2.0F, sun[1], 0.01F);
                }
            }
        }
        // Positive angles turn the sun clockwise: from the top toward the right
        SunbeamsRig.placeSun(SunbeamsSide.TOP, 30.0F, WIDTH, HEIGHT, MARGIN, sun);
        assertTrue(sun[0] > WIDTH / 2.0F);
        SunbeamsRig.placeSun(SunbeamsSide.LEFT, 30.0F, WIDTH, HEIGHT, MARGIN, sun);
        assertTrue(sun[1] < HEIGHT / 2.0F);
    }

    @Test
    void theFanOfDirectionsCoversTheScreen() {
        SunbeamsRig rig = new SunbeamsRig(SUN_X, SUN_Y, WIDTH, HEIGHT);
        // The top right and bottom left corners are the ends of the fan. The fan runs to the corners of the screen, pixels are
        // measured at their centers, so the corner pixels sit a few bins inside of it.
        assertTrue(rig.bins[WIDTH - 1] <= 8, "top right " + (int)rig.bins[WIDTH - 1]);
        assertTrue(rig.bins[(HEIGHT - 1) * WIDTH] >= SunbeamsRig.BINS - 9, "bottom left " + (int)rig.bins[(HEIGHT - 1) * WIDTH]);
        assertEquals(Math.toRadians(67.5), rig.fanWidth, Math.toRadians(1.0));
        // Directions turn clockwise from the top right to the bottom left
        for (int x = 1; x < WIDTH; x++) assertTrue(rig.bins[100 * WIDTH + x] <= rig.bins[100 * WIDTH + x - 1]);
        for (int y = 1; y < HEIGHT; y++) assertTrue(rig.bins[y * WIDTH + 200] >= rig.bins[(y - 1) * WIDTH + 200]);
        int j = 50 * WIDTH + 30;
        assertEquals(Math.hypot(30.5 - SUN_X, 50.5 - SUN_Y), rig.distances[j], 0.01);
    }

    @Test
    void fansWorkFromEverySide() {
        float[] sun = new float[2];
        for (SunbeamsSide side : SunbeamsSide.values()) {
            for (int angle = -80; angle <= 80; angle += 40) {
                SunbeamsRig.placeSun(side, angle, WIDTH, HEIGHT, MARGIN, sun);
                SunbeamsRig rig = new SunbeamsRig(sun[0], sun[1], WIDTH, HEIGHT);
                assertTrue((rig.fanWidth > 0.2F) && (rig.fanWidth < Math.PI), side + " " + angle);
                int min = Integer.MAX_VALUE;
                int max = Integer.MIN_VALUE;
                for (char bin : rig.bins) {
                    min = Math.min(min, bin);
                    max = Math.max(max, bin);
                }
                assertTrue((min <= 8) && (max >= SunbeamsRig.BINS - 9), side + " " + angle + ": " + min + " to " + max);
            }
        }
    }

    @Test
    void elementsCastPointedShadesAndKeepTheirFacesDark() {
        SunbeamsRig rig = new SunbeamsRig(SUN_X, SUN_Y, WIDTH, HEIGHT);
        CollisionAreaBounds button = new CollisionAreaBounds(165, 92, 150, 18);
        rig.buildShade(List.of(button));
        assertEquals(SunbeamsRig.SHADE_FACE, rig.shade[100 * WIDTH + 200]);
        // Between the sun and the element there is no shade
        assertEquals(SunbeamsRig.SHADE_LIT, rig.shade[60 * WIDTH + 150]);
        // Right behind the middle of the element, away from the sun, is its core shade
        float cx = 240.5F;
        float cy = 101.5F;
        float dx = cx - SUN_X;
        float dy = cy - SUN_Y;
        float length = (float)Math.hypot(dx, dy);
        int bx = Math.round(cx + dx / length * 30.0F);
        int by = Math.round(cy + dy / length * 30.0F);
        assertEquals(SunbeamsRig.SHADE_UMBRA, rig.shade[by * WIDTH + bx]);
        // Far behind it the shade has run out into its point
        int fx = Math.round(cx + dx / length * 220.0F);
        int fy = Math.round(cy + dy / length * 220.0F);
        assertEquals(SunbeamsRig.SHADE_LIT, rig.shade[fy * WIDTH + fx]);
        // The shade narrows: its width right behind the element is bigger than further away
        assertTrue(shadeWidth(rig, cx, cy, dx / length, dy / length, 20.0F) > shadeWidth(rig, cx, cy, dx / length, dy / length, 120.0F));
    }

    private static int shadeWidth(SunbeamsRig rig, float cx, float cy, float ux, float uy, float distance) {
        float px = cx + ux * distance;
        float py = cy + uy * distance;
        int width = 0;
        for (float t = -80.0F; t <= 80.0F; t += 1.0F) {
            int x = Math.round(px - uy * t);
            int y = Math.round(py + ux * t);
            if ((x < 0) || (y < 0) || (x >= WIDTH) || (y >= HEIGHT)) continue;
            if (rig.shade[y * WIDTH + x] < SunbeamsRig.SHADE_LIT) width++;
        }
        return width;
    }

    @Test
    void theEdgesThatFaceTheSunLightUp() {
        SunbeamsRig rig = new SunbeamsRig(SUN_X, SUN_Y, WIDTH, HEIGHT);
        rig.buildShade(List.of(new CollisionAreaBounds(165, 92, 150, 18)));
        // The top row and the left column without the corner
        assertEquals(150 + 17, rig.rimCount);
        for (int k = 0; k < rig.rimCount; k++) {
            boolean top = (rig.rimY[k] == 92) && (rig.rimBevelX[k] == rig.rimX[k]) && (rig.rimBevelY[k] == 93);
            boolean left = (rig.rimX[k] == 165) && (rig.rimBevelX[k] == 166) && (rig.rimBevelY[k] == rig.rimY[k]) && (rig.rimY[k] > 92);
            assertTrue(top || left, rig.rimX[k] + "," + rig.rimY[k]);
            assertEquals(rig.rimY[k] * WIDTH + rig.rimX[k], rig.rimIndex[k]);
        }
        assertEquals(165, rig.rimLeft);
        assertEquals(92, rig.rimTop);
        assertEquals(315, rig.rimRight);
        assertEquals(110, rig.rimBottom);
    }

    @Test
    void aSunBelowAndRightLightsTheBottomAndRightEdges() {
        SunbeamsRig rig = new SunbeamsRig(WIDTH + 70.0F, HEIGHT + 120.0F, WIDTH, HEIGHT);
        rig.buildShade(List.of(new CollisionAreaBounds(100, 50, 40, 10)));
        assertEquals(40 + 9, rig.rimCount);
        for (int k = 0; k < rig.rimCount; k++) {
            boolean bottom = (rig.rimY[k] == 59) && (rig.rimBevelY[k] == 58);
            boolean right = (rig.rimX[k] == 139) && (rig.rimBevelX[k] == 138) && (rig.rimY[k] < 59);
            assertTrue(bottom || right, rig.rimX[k] + "," + rig.rimY[k]);
        }
    }

    @Test
    void edgesHiddenBehindAnotherElementStayDark() {
        SunbeamsRig rig = new SunbeamsRig(SUN_X, SUN_Y, WIDTH, HEIGHT);
        CollisionAreaBounds front = new CollisionAreaBounds(165, 92, 150, 18);
        CollisionAreaBounds behind = new CollisionAreaBounds(165, 116, 150, 18);
        rig.buildShade(List.of(front, behind));
        // The light toward the middle of the lower element's top edge passes through the upper element first
        assertFalse(hasRim(rig, 250, 116));
        // Its left edge gets light past the upper element
        assertTrue(hasRim(rig, 165, 120));
        assertTrue(hasRim(rig, 250, 92));
    }

    private static boolean hasRim(SunbeamsRig rig, int x, int y) {
        for (int k = 0; k < rig.rimCount; k++) {
            if ((rig.rimX[k] == x) && (rig.rimY[k] == y)) return true;
        }
        return false;
    }

    @Test
    void hitsFindTheEntryAndExitOfABox() {
        float[] hit = new float[2];
        assertTrue(SunbeamsRig.hitBox(10, 0, 10, 10, 0.0F, 5.0F, 1.0F, 0.0F, hit));
        assertEquals(10.0F, hit[0], 1.0E-5F);
        assertEquals(20.0F, hit[1], 1.0E-5F);
        assertFalse(SunbeamsRig.hitBox(10, 0, 10, 10, 0.0F, 5.0F, -1.0F, 0.0F, hit));
        assertFalse(SunbeamsRig.hitBox(10, 20, 10, 10, 0.0F, 5.0F, 1.0F, 0.0F, hit));
    }

    @Test
    void aNewShadeSharesTheDirectionsAndRemembersItsBoxes() {
        SunbeamsRig geometry = new SunbeamsRig(SUN_X, SUN_Y, WIDTH, HEIGHT);
        List<CollisionAreaBounds> boxes = List.of(new CollisionAreaBounds(165, 92, 150, 18));
        assertFalse(geometry.hasShadeFor(List.of()), "a rig without shade has none for any boxes");
        SunbeamsRig shaded = new SunbeamsRig(geometry);
        shaded.buildShade(boxes);
        assertSame(geometry.bins, shaded.bins);
        assertSame(geometry.distances, shaded.distances);
        assertNotSame(geometry.shade, shaded.shade);
        assertEquals(SunbeamsRig.SHADE_LIT, geometry.shade[100 * WIDTH + 200], "the first rig keeps its own shade");
        assertEquals(SunbeamsRig.SHADE_FACE, shaded.shade[100 * WIDTH + 200]);
        assertTrue(shaded.hasShadeFor(List.of(new CollisionAreaBounds(165, 92, 150, 18))));
        assertFalse(shaded.hasShadeFor(List.of(new CollisionAreaBounds(165, 93, 150, 18))));
        assertFalse(shaded.hasShadeFor(List.of()));
        SunbeamsRig fresh = new SunbeamsRig(SUN_X, SUN_Y, WIDTH, HEIGHT);
        fresh.buildShade(boxes);
        assertArrayEquals(fresh.shade, shaded.shade);
        assertEquals(fresh.rimCount, shaded.rimCount);
    }

}
