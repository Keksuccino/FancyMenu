package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.TiledCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrassBedTest {

    private static GrassBed ground() {
        GrassBed ground = new GrassBed();
        ground.build(new PixelRandom(607), 808, 480, 270);
        return ground;
    }

    @Test
    void flowerStemsKeepTheirDistance() {
        GrassBed ground = ground();
        for (int i = 0; i < ground.stemCount(); i++) {
            for (int j = i + 1; j < ground.stemCount(); j++) {
                if (!ground.stem(i).spare && !ground.stem(j).spare) continue;
                assertTrue(Math.abs(ground.stem(i).position - ground.stem(j).position) >= 5, "stems too close");
            }
        }
    }

    @Test
    void flowersInTheGrassFollowTheFlowerAmount() {
        GrassBed ground = ground();
        int normal = countFlowers(ground, 1.0F / 3.0F);
        int quarter = countFlowers(ground, 0.25F / 3.0F);
        int triple = countFlowers(ground, 1.0F);
        assertTrue((quarter > 0) && (quarter < normal) && (normal < triple), quarter + " < " + normal + " < " + triple);
    }

    private static int countFlowers(GrassBed ground, float threshold) {
        ground.update(50.0F, 50.0F, 0.0F, true, true, threshold);
        int count = 0;
        for (int i = 0; i < ground.stemCount(); i++) {
            if (ground.stemAge(i) > 0.0F) count++;
        }
        return count;
    }

    @Test
    void flowersStandOnTheBareFloorWithoutGrass() {
        GrassBed ground = ground();
        assertTrue(ground.update(0.0F, 50.0F, 0.0F, false, true, 1.0F / 3.0F));
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resize(0, 270 - GrassBed.STRIP_HEIGHT, 480, GrassBed.STRIP_HEIGHT, 480, GrassBed.STRIP_HEIGHT);
        canvas.invalidateAll();
        canvas.repaint(new CanvasPainter(), ground::paint);
        int bottomRow = 0;
        for (int x = 0; x < 480; x++) {
            if (canvas.get(x, 269) != 0) bottomRow++;
        }
        // Only the stems touch the floor, there is no grass foot
        assertTrue((bottomRow > 0) && (bottomRow < 120), "pixels on the bottom row: " + bottomRow);
    }

    @Test
    void nothingShowsWithGrassAndFlowersOff() {
        GrassBed ground = ground();
        assertFalse(ground.update(0.0F, 0.0F, 0.0F, false, false, 1.0F / 3.0F));
    }

    @Test
    void theWindMovesTheGrass() {
        GrassBed ground = ground();
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resize(0, 270 - GrassBed.STRIP_HEIGHT, 480, GrassBed.STRIP_HEIGHT, 480, GrassBed.STRIP_HEIGHT);
        ground.update(50.0F, 50.0F, 0.0F, true, true, 1.0F / 3.0F);
        canvas.invalidateAll();
        canvas.repaint(new CanvasPainter(), ground::paint);
        int[] before = snapshot(canvas);
        ground.update(51.0F, 51.0F, 1.0F, true, true, 1.0F / 3.0F);
        canvas.invalidateAll();
        canvas.repaint(new CanvasPainter(), ground::paint);
        int[] after = snapshot(canvas);
        int changed = 0;
        for (int i = 0; i < before.length; i++) {
            if (before[i] != after[i]) changed++;
        }
        assertNotEquals(0, changed);
    }

    private static int[] snapshot(TiledCanvas canvas) {
        int[] pixels = new int[480 * GrassBed.STRIP_HEIGHT];
        for (int y = 0; y < GrassBed.STRIP_HEIGHT; y++) {
            for (int x = 0; x < 480; x++) pixels[y * 480 + x] = canvas.get(x, 270 - GrassBed.STRIP_HEIGHT + y);
        }
        return pixels;
    }

}
