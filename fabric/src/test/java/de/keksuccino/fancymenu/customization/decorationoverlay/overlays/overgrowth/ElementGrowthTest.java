package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.TiledCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementGrowthTest {

    private static ElementGrowth button() {
        ElementGrowth growth = new ElementGrowth(165, 92, 150, 18);
        growth.build(0);
        return growth;
    }

    private static TiledCanvas canvas() {
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resizeToScreen(480, 270);
        return canvas;
    }

    @Test
    void mossGrowsInRoundCushionsWithBareFeetBetweenThem() {
        int n = 154;
        float[] heights = ElementGrowth.mossHeights(new PixelRandom(17), n, 5.6F);
        int bare = 0;
        for (int i = 0; i < n; i++) {
            if (Float.isNaN(heights[i])) {
                bare++;
                continue;
            }
            assertTrue(heights[i] <= 5.6F);
            // The ends of the cap round off
            assertTrue(heights[i] <= 1 + Math.min(i, n - 1 - i));
        }
        assertTrue(bare > 0);
        assertTrue(bare < n / 4);
    }

    @Test
    void theSameElementAlwaysGrowsTheSameWay() {
        ElementGrowth first = button();
        ElementGrowth second = button();
        assertEquals(first.flowerCount(), second.flowerCount());
        for (int f = 0; f < first.flowerCount(); f++) {
            assertEquals(first.flower(f).position, second.flower(f).position);
            assertEquals(first.flower(f).kind, second.flower(f).kind);
        }
    }

    @Test
    void growthComesAndGoesWithThePart() {
        ElementGrowth growth = button();
        TiledCanvas canvas = canvas();

        growth.update(0.0F, 0.0F, true, 1.0F / 3.0F, canvas);
        assertFalse(canvas.hasPending());

        growth.update(40.0F, 0.0F, true, 1.0F / 3.0F, canvas);
        assertTrue(canvas.hasPending());
        assertTrue(growth.isSettled(40.0F));
        int mossy = 0;
        for (int i = 0; i < growth.mossColumns(); i++) {
            if (growth.shownMossHeight(i) > 0) mossy++;
        }
        assertTrue(mossy > growth.mossColumns() * 3 / 4);
        canvas.repaint(new CanvasPainter(), growth::paint);
        assertTrue(canvas.tileCount() > 0);

        growth.update(40.0F, 0.0F, false, 1.0F / 3.0F, canvas);
        for (int i = 0; i < growth.mossColumns(); i++) assertEquals(0, growth.shownMossHeight(i));
        for (int f = 0; f < growth.flowerCount(); f++) assertEquals(OvergrowthArt.BLOOM_HIDDEN, growth.flowerStage(f));
        canvas.repaint(new CanvasPainter(), growth::paint);
        // Everything got erased again
        for (int y = 0; y < 270; y++) {
            for (int x = 0; x < 480; x++) assertEquals(0, canvas.get(x, y), "pixel " + x + "," + y);
        }
    }

    @Test
    void anElementThatShowsUpLaterGrowsInFromScratch() {
        ElementGrowth growth = button();
        growth.startClock = 50.0F;
        TiledCanvas canvas = canvas();
        growth.update(50.5F, 0.0F, true, 1.0F / 3.0F, canvas);
        assertFalse(growth.isSettled(50.5F));
        int tallest = 0;
        for (int i = 0; i < growth.mossColumns(); i++) tallest = Math.max(tallest, growth.shownMossHeight(i));
        assertTrue(tallest <= 2);
    }

    @Test
    void flowersOnTheMossFollowTheFlowerAmount() {
        ElementGrowth growth = button();
        TiledCanvas canvas = canvas();
        int none = countFlowers(growth, 0.0F, canvas);
        int normal = countFlowers(growth, 1.0F / 3.0F, canvas);
        int all = countFlowers(growth, 1.0F, canvas);
        assertEquals(0, none);
        int first = 0;
        for (int f = 0; f < growth.flowerCount(); f++) {
            if (!growth.flower(f).spare) first++;
        }
        assertEquals(first, normal);
        assertEquals(growth.flowerCount(), all);
        assertTrue(all > normal);
    }

    private static int countFlowers(ElementGrowth growth, float threshold, TiledCanvas canvas) {
        growth.update(100.0F, 0.0F, true, threshold, canvas);
        growth.update(200.0F, 0.0F, true, threshold, canvas);
        int count = 0;
        for (int f = 0; f < growth.flowerCount(); f++) {
            if (growth.flowerStage(f) != OvergrowthArt.BLOOM_HIDDEN) count++;
        }
        return count;
    }

    @Test
    void flowersStandOnCushionsAndKeepTheirDistance() {
        ElementGrowth growth = button();
        for (int f = 0; f < growth.flowerCount(); f++) {
            OvergrowthFlower flower = growth.flower(f);
            for (int g = f + 1; g < growth.flowerCount(); g++) {
                assertTrue(Math.abs(flower.position - growth.flower(g).position) >= 4, "flowers too close");
            }
        }
    }

    @Test
    void paintingOnlyWhatChangedGivesTheSamePictureAsPaintingEverything() {
        ElementGrowth upper = new ElementGrowth(165, 92, 150, 18);
        upper.build(0);
        ElementGrowth lower = new ElementGrowth(165, 116, 150, 18);
        lower.build(1);
        TiledCanvas canvas = canvas();
        CanvasPainter painter = new CanvasPainter();
        TiledCanvas.Content content = p -> {
            upper.paint(p);
            lower.paint(p);
        };
        for (int frame = 0; frame < 1200; frame++) {
            float time = frame / 30.0F;
            float clock = time * 1.25F;
            boolean show = (frame < 700) || (frame > 760);
            float partClock = (frame > 760) ? ((frame - 760) / 30.0F * 1.25F) : clock;
            float threshold = (frame < 500) ? (0.25F / 3.0F) : 1.0F;
            upper.update(partClock, time, show, threshold, canvas);
            lower.update(partClock, time, show, threshold, canvas);
            canvas.repaint(painter, content);
        }
        TiledCanvas full = canvas();
        full.invalidateAll();
        full.repaint(painter, content);
        for (int y = 0; y < 270; y++) {
            for (int x = 0; x < 480; x++) assertEquals(full.get(x, y), canvas.get(x, y), "pixel " + x + "," + y);
        }
    }

}
