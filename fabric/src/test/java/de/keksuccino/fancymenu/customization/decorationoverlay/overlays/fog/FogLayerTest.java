package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fog;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import java.util.Arrays;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("FogLayer noise builder")
class FogLayerTest {

    private static final int WIDTH = 480;
    private static final int HEIGHT = 270;
    private static Executor originalBuilder;

    /**
     * Builds the noise textures right away, so layers paint with their first update.
     */
    @BeforeAll
    static void buildNoiseRightAway() {
        originalBuilder = FogLayer.noiseBuilder;
        FogLayer.noiseBuilder = Runnable::run;
    }

    @AfterAll
    static void restoreNoiseBuilder() {
        FogLayer.noiseBuilder = originalBuilder;
    }

    @Test
    void theNoiseRepeatsSeamlesslyAlsoLeftOfZero() {
        for (int cell : new int[]{32, 64, 128}) {
            for (float y = -6.0F; y < 40.0F; y += 3.7F) {
                for (float x = -60.0F; x < 30.0F; x += 1.3F) {
                    assertEquals(FogLayer.tiledNoise(x, y, cell, 13.0F, 5), FogLayer.tiledNoise(x + FogLayer.TEXTURE_WIDTH, y, cell, 13.0F, 5), 1.0E-4F, "cell " + cell + " at " + x + "," + y);
                }
            }
        }
    }

    @Test
    void noiseTexturesAreRepeatableAndBetweenZeroAndOne() {
        float[] first = FogLayer.noiseTexture(15, 17.0F, 40);
        float[] second = FogLayer.noiseTexture(15, 17.0F, 40);
        assertArrayEquals(first, second);
        for (float v : first) assertTrue((v >= 0.0F) && (v <= 1.0F));
        // Rows don't depend on how many there are, so a taller texture serves a shorter layer
        float[] taller = FogLayer.noiseTexture(15, 17.0F, 60);
        for (int i = 0; i < first.length; i++) assertEquals(first[i], taller[i]);
    }

    @Test
    void layersCoverTheirShareOfTheScreen() {
        assertEquals(92, FogLayer.LAYERS[0].top(HEIGHT));
        assertEquals(198, FogLayer.LAYERS[0].bottom(HEIGHT));
        assertEquals(128, FogLayer.LAYERS[1].top(HEIGHT));
        assertEquals(248, FogLayer.LAYERS[1].bottom(HEIGHT));
        assertEquals(170, FogLayer.LAYERS[2].top(HEIGHT));
        assertEquals(HEIGHT, FogLayer.LAYERS[2].bottom(HEIGHT));
        assertEquals(540, FogLayer.LAYERS[2].bottom(540));
    }

    @Test
    void theFogPartsAroundThePointerAndNowhereElse() {
        FogLayerState open = new FogLayerState(FogLayer.LAYERS[2], 2);
        FogLayerState parted = new FogLayerState(FogLayer.LAYERS[2], 2);
        open.resize(WIDTH, HEIGHT);
        parted.resize(WIDTH, HEIGHT);
        open.update(4.0F, Float.NaN, Float.NaN, true);
        parted.update(4.0F, 240.0F, 230.0F, true);
        int[] a = open.canvas.pixels();
        int[] b = parted.canvas.pixels();
        int top = open.top();
        long alphaOpen = 0;
        long alphaParted = 0;
        float radiusSquared = FogLayer.CLEAR_RADIUS * FogLayer.CLEAR_RADIUS;
        for (int r = 0; r < open.canvas.height(); r++) {
            for (int x = 0; x < WIDTH; x++) {
                int i = r * WIDTH + x;
                float dx = x - 240.0F;
                float dy = top + r - 230.0F;
                boolean inside = dx * dx + dy * dy < radiusSquared;
                // A pixel right below the clearing can change the tone of its top edge, everything else stays the same
                boolean belowInside = dx * dx + (dy - 1.0F) * (dy - 1.0F) < radiusSquared;
                if (inside) {
                    alphaOpen += PixelMath.alpha(a[i]);
                    alphaParted += PixelMath.alpha(b[i]);
                } else if (!belowInside) {
                    assertEquals(a[i], b[i], "pixel " + x + "," + r);
                }
            }
        }
        assertTrue(alphaParted < alphaOpen * 0.8, "open " + alphaOpen + " parted " + alphaParted);
    }

    @Test
    void movingThePointerPaintsTheSameAsPaintingEverything() {
        for (int index = 0; index < FogLayer.LAYERS.length; index++) {
            FogLayerState moving = new FogLayerState(FogLayer.LAYERS[index], index);
            moving.resize(WIDTH, HEIGHT);
            // Small and big jumps, leaving and coming back, all within one texture step
            float[][] path = {{200, 150}, {205, 152}, {260, 190}, {Float.NaN, Float.NaN}, {420, 250}, {30, 120}, {31, 121}, {300, 210}};
            float time = 2.0F;
            moving.update(time, path[0][0], path[0][1], true);
            for (float[] point : path) moving.update(time, point[0], point[1], true);
            float[] last = path[path.length - 1];
            FogLayerState fresh = new FogLayerState(FogLayer.LAYERS[index], index);
            fresh.resize(WIDTH, HEIGHT);
            fresh.update(time, last[0], last[1], true);
            assertArrayEquals(fresh.canvas.pixels(), moving.canvas.pixels(), "layer " + index);
        }
    }

    @Test
    void layersOnlyRepaintWhenSomethingChanged() {
        FogLayerState state = new FogLayerState(FogLayer.LAYERS[0], 0);
        state.resize(WIDTH, HEIGHT);
        state.update(1.0F, Float.NaN, Float.NaN, true);
        int[] before = state.canvas.pixels().clone();
        // The first layer slides 3.5 pixels per second, so nothing changes within the same pixel step
        state.update(1.1F, Float.NaN, Float.NaN, true);
        assertArrayEquals(before, state.canvas.pixels());
        state.update(1.0F + 1.0F / 3.5F + 0.01F, Float.NaN, Float.NaN, true);
        assertFalse(Arrays.equals(before, state.canvas.pixels()));
    }

    @Test
    void groundFogThickensTowardTheBottom() {
        FogLayerState ground = new FogLayerState(FogLayer.LAYERS[2], 2);
        ground.resize(WIDTH, HEIGHT);
        ground.update(7.0F, Float.NaN, Float.NaN, true);
        int[] pixels = ground.canvas.pixels();
        int rows = ground.canvas.height();
        long upper = 0;
        long lower = 0;
        for (int r = 0; r < rows; r++) {
            for (int x = 0; x < WIDTH; x++) {
                int alpha = PixelMath.alpha(pixels[r * WIDTH + x]);
                if (r < rows / 3) upper += alpha;
                else if (r >= rows * 2 / 3) lower += alpha;
            }
        }
        assertTrue(lower > upper * 1.5, "upper " + upper + " lower " + lower);
    }

    @Test
    void aLayerThatMayNotPaintEverythingWaitsForTheNextFrame() {
        FogLayerState state = new FogLayerState(FogLayer.LAYERS[1], 1);
        state.resize(WIDTH, HEIGHT);
        assertFalse(state.update(1.0F, Float.NaN, Float.NaN, false), "a new layer waits for its turn too");
        assertTrue(state.update(1.0F, Float.NaN, Float.NaN, true));
        int[] before = state.canvas.pixels().clone();
        float later = 1.0F + 1.0F / 7.0F + 0.01F;
        assertFalse(state.update(later, Float.NaN, Float.NaN, false));
        assertArrayEquals(before, state.canvas.pixels());
        assertTrue(state.update(later, Float.NaN, Float.NaN, true));
        assertFalse(Arrays.equals(before, state.canvas.pixels()));
    }

}
