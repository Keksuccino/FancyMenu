package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.frost;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IceLayerTest {

    @Test
    void freezingASpotTwiceKeepsTheEarlierTimeAndTheStrongerIce() {
        IceLayer layer = new IceLayer(20, 20);
        layer.glaze(5, 5, 2.0, 0);
        layer.glaze(5, 5, 1.0, 2);
        layer.glaze(5, 5, 3.0, 1);
        layer.crystal(6, 5, 1.0, 3);
        layer.crystal(6, 5, 2.0, 1);

        assertEquals(1, layer.glaze.count);
        int glaze = layer.glazeAt(layer.pixelIndex(5, 5));
        assertEquals(1.0F, layer.glaze.birth[glaze]);
        assertEquals(2, layer.glaze.value[glaze]);
        int crystal = layer.crystalAt(layer.pixelIndex(6, 5));
        assertEquals(1.0F, layer.crystals.birth[crystal]);
        assertEquals(1, layer.crystals.value[crystal]);
    }

    @Test
    void iceOutsideOfTheScreenGetsDropped() {
        IceLayer layer = new IceLayer(20, 10);
        layer.glaze(-1, 5, 0.0, 1);
        layer.glaze(20, 5, 0.0, 1);
        layer.crystal(5, 10, 0.0, 1);
        layer.crystal(5, -3, 0.0, 1);
        assertEquals(0, layer.glaze.count);
        assertEquals(0, layer.crystals.count);
    }

    @Test
    void newIceShowsFreshAndThenSettles() {
        IceLayer layer = new IceLayer(20, 20);
        layer.crystal(3, 3, 1.0, 2);
        layer.glaze(4, 3, 1.0, 1);
        layer.glaze.markAllPending();
        layer.crystals.markAllPending();
        List<int[]> changed = new ArrayList<>();

        layer.settle(0.5F, (x, y) -> changed.add(new int[]{x, y}));
        assertTrue(changed.isEmpty());

        layer.settle(1.1F, (x, y) -> changed.add(new int[]{x, y}));
        assertEquals(2, changed.size());
        assertEquals(IceLayer.FRESH, layer.crystals.shown[0]);
        assertEquals(IceLayer.FRESH, layer.glaze.shown[0]);

        // Crystals settle after 0.3 seconds, glaze after 0.5
        layer.settle(1.4F, (x, y) -> changed.add(new int[]{x, y}));
        assertEquals(IceLayer.SETTLED, layer.crystals.shown[0]);
        assertEquals(IceLayer.FRESH, layer.glaze.shown[0]);
        layer.settle(1.6F, (x, y) -> changed.add(new int[]{x, y}));
        assertEquals(IceLayer.SETTLED, layer.glaze.shown[0]);
        assertEquals(0, layer.glaze.pendingCount + layer.crystals.pendingCount);
        assertEquals(4, changed.size());
    }

    @Test
    void aBlowKnocksTheIceOutAndTheHoleFreezesOverFromItsRim() {
        int width = 40;
        IceLayer layer = new IceLayer(width, 40);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < width; x++) layer.glaze(x, y, 0.0, 1);
        }
        layer.glaze.markAllPending();
        layer.settle(10.0F, (x, y) -> {
        });
        int hitX = 20;
        int hitY = 20;
        double limit = 15.0;
        float clock = 10.0F;
        List<Integer> knocked = new ArrayList<>();
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < width; x++) {
                double distance = Math.hypot(x - hitX, y - hitY);
                if (distance >= limit) continue;
                int index = layer.glazeAt(layer.pixelIndex(x, y));
                assertTrue(layer.glaze.knock(index, clock, distance, limit));
                knocked.add(index);
            }
        }
        // Knocked out ice is no ice to knock out again
        assertFalse(layer.glaze.knock(knocked.get(0), clock, 0.0, limit));
        layer.settle(clock, (x, y) -> {
        });
        for (int index : knocked) assertEquals(IceLayer.HIDDEN, layer.glaze.shown[index]);

        double rimTimes = 0.0;
        int rimCount = 0;
        double middleTimes = 0.0;
        int middleCount = 0;
        for (int index : knocked) {
            // When it comes back: the knock pushed its time back past the clock
            double back = layer.glaze.birth[index] + layer.glaze.knocked[index];
            assertTrue(back >= clock + 6.0F - 1.0E-3F);
            double distance = Math.hypot(layer.glaze.x[index] - hitX, layer.glaze.y[index] - hitY);
            if (distance > 12.0) {
                rimTimes += back;
                rimCount++;
            } else if (distance < 3.0) {
                middleTimes += back;
                middleCount++;
            }
        }
        assertTrue(rimTimes / rimCount < middleTimes / middleCount, "the rim refreezes first");

        // After long enough, the hole is frozen over again
        layer.settle(clock + 20.0F, (x, y) -> {
        });
        for (int index : knocked) assertEquals(IceLayer.SETTLED, layer.glaze.shown[index]);
    }

    @Test
    void stepsFollowPixelPerfectSlopes() {
        FrostArt.Walker walker = new FrostArt.Walker(0, 0);
        for (int i = 0; i < 4; i++) FrostArt.step(walker, 1);
        assertEquals(4, walker.x);
        assertEquals(2, walker.y);
        walker.reset(0, 0);
        for (int i = 0; i < 4; i++) FrostArt.step(walker, 3);
        assertEquals(2, walker.x);
        assertEquals(4, walker.y);
        walker.reset(0, 0);
        for (int i = 0; i < 4; i++) FrostArt.step(walker, FrostArt.UP);
        assertEquals(0, walker.x);
        assertEquals(-4, walker.y);
        walker.reset(0, 0);
        // Index + 16 is the same direction
        for (int i = 0; i < 3; i++) FrostArt.step(walker, 2 + 16);
        assertEquals(3, walker.x);
        assertEquals(3, walker.y);
    }

    @Test
    void anIcicleIsLitOnOneEdgeAndShadedOnTheOther() {
        IceLayer layer = new IceLayer(100, 100);
        FrostArt.shard(layer, 50, 10, FrostArt.DOWN, 10, 2, 0.0, 1.0);
        // The first row of the icicle is its full width, from the lit left edge to the shaded right edge
        assertEquals(0, layer.crystals.value[layer.crystalAt(layer.pixelIndex(48, 11))]);
        assertEquals(1, layer.crystals.value[layer.crystalAt(layer.pixelIndex(49, 11))]);
        assertEquals(2, layer.crystals.value[layer.crystalAt(layer.pixelIndex(51, 11))]);
        assertEquals(3, layer.crystals.value[layer.crystalAt(layer.pixelIndex(52, 11))]);
        // It tapers to a point and grows along its length
        assertEquals(-1, layer.crystalAt(layer.pixelIndex(48, 20)));
        assertTrue(layer.crystals.birth[layer.crystalAt(layer.pixelIndex(50, 20))] > layer.crystals.birth[layer.crystalAt(layer.pixelIndex(50, 11))]);
    }

    @Test
    void teethStayWithinTheirHeights() {
        int[] teeth = FrostArt.teeth(new PixelRandom(5), 300, 1, 9);
        int highest = 0;
        for (int depth : teeth) {
            assertTrue((depth >= 0) && (depth <= 9));
            highest = Math.max(highest, depth);
        }
        assertTrue(highest >= 5);
    }

    @Test
    void theScreenFreezesTheSameWayEveryTime() {
        IceLayer first = new IceLayer(480, 270);
        IceLayer second = new IceLayer(480, 270);
        FrostShaper.edges(first, new PixelRandom(4242), 480, 270);
        FrostShaper.edges(second, new PixelRandom(4242), 480, 270);
        assertEquals(first.glaze.count, second.glaze.count);
        assertEquals(first.crystals.count, second.crystals.count);
        // Every edge has ice along its whole length
        for (int x = 0; x < 480; x++) {
            assertTrue(first.glazeAt(first.pixelIndex(x, 0)) >= 0);
            assertTrue(first.glazeAt(first.pixelIndex(x, 269)) >= 0);
        }
    }

    @Test
    void elementIceStaysWithinTheFreeRows() {
        IceLayer layer = new IceLayer(480, 270);
        FrostShaper.element(layer, new PixelRandom(1), 165, 100, 150, 18, 3, 4);
        for (int i = 0; i < layer.crystals.count; i++) {
            assertTrue(layer.crystals.y[i] >= 100 - 3, "above at " + layer.crystals.y[i]);
            assertTrue(layer.crystals.y[i] < 118 + 4, "below at " + layer.crystals.y[i]);
        }
        // The label in the middle of the element stays clear
        assertEquals(-1, layer.glazeAt(layer.pixelIndex(240, 109)));
        assertEquals(-1, layer.crystalAt(layer.pixelIndex(240, 109)));
    }

}
