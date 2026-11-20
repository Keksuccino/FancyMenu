package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.TiledCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreeperGrowerTest {

    private static final int WIDTH = 480;
    private static final int HEIGHT = 270;

    private static List<Creeper> grow(boolean right, int width, int height) {
        return CreeperGrower.grow(new PixelRandom(606), 5353, right, width, height);
    }

    @Test
    void theSameScreenAlwaysGrowsTheSamePlants() {
        List<Creeper> first = grow(false, WIDTH, HEIGHT);
        List<Creeper> second = grow(false, WIDTH, HEIGHT);
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertArrayEquals(first.get(i).xs, second.get(i).xs);
            assertArrayEquals(first.get(i).ys, second.get(i).ys);
        }
    }

    @Test
    void everyVineIsACleanOnePixelLine() {
        for (Creeper creeper : grow(false, WIDTH, HEIGHT)) {
            int n = creeper.length();
            for (int i = 1; i < n; i++) {
                int dx = Math.abs(creeper.xs[i] - creeper.xs[i - 1]);
                int dy = Math.abs(creeper.ys[i] - creeper.ys[i - 1]);
                assertTrue((dx <= 1) && (dy <= 1) && (dx + dy > 0), "gap or repeat at pixel " + i);
            }
            // No L-shaped corners: a pixel two steps back is never a diagonal neighbor
            for (int i = 2; i < n; i++) {
                int dx = Math.abs(creeper.xs[i] - creeper.xs[i - 2]);
                int dy = Math.abs(creeper.ys[i] - creeper.ys[i - 2]);
                assertFalse((dx == 1) && (dy == 1), "corner pixel at " + i);
            }
        }
    }

    @Test
    void theMainStemHugsTheSideAndRunsAlongTheTop() {
        Creeper frame = grow(false, WIDTH, HEIGHT).get(0);
        assertEquals(Creeper.STEM, frame.kind);
        assertTrue(frame.ys[0] > HEIGHT - 10);
        int top = Integer.MAX_VALUE;
        for (int i = 0; i < frame.length(); i++) {
            top = Math.min(top, frame.ys[i]);
            // While it climbs it stays close to the side edge
            if (frame.ys[i] > 40) assertTrue(frame.xs[i] < 20, "climbs at x " + frame.xs[i]);
        }
        assertTrue(top < 20);
        int endX = frame.xs[frame.length() - 1];
        assertTrue((endX >= 200) && (endX <= 236), "ends at x " + endX);
    }

    @Test
    void theMainStemFollowsTheScreenWidth() {
        Creeper frame = grow(false, 960, HEIGHT).get(0);
        int endX = frame.xs[frame.length() - 1];
        assertTrue((endX >= 400) && (endX <= 470), "ends at x " + endX);
    }

    @Test
    void theRightSideIsTheMirrorImageOfTheLeft() {
        List<Creeper> left = grow(false, WIDTH, HEIGHT);
        List<Creeper> right = CreeperGrower.grow(new PixelRandom(606), 5353, true, WIDTH, HEIGHT);
        assertEquals(left.size(), right.size());
        for (int i = 0; i < left.size(); i++) {
            Creeper l = left.get(i);
            Creeper r = right.get(i);
            for (int p = 0; p < l.length(); p++) {
                assertEquals(WIDTH - 1 - l.xs[p], r.xs[p]);
                assertEquals(l.ys[p], r.ys[p]);
                assertEquals(-l.thickX[p], r.thickX[p]);
            }
            for (int f = 0; f < l.leafFlip.length; f++) assertEquals(!l.leafFlip[f], r.leafFlip[f]);
        }
    }

    @Test
    void vinesGrowAPixelAtATimeAndOldStemsTurnWoody() {
        Creeper frame = grow(false, WIDTH, HEIGHT).get(0);
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resizeToScreen(WIDTH, HEIGHT);

        frame.update(1.0F, 0.0F, true, 0.0F, canvas);
        assertEquals((int)Math.floor((1.0F - frame.birth) * frame.speed), frame.grownPixels());
        assertEquals(0, frame.woodyPixels());
        assertTrue(canvas.hasPending());

        frame.update(8.0F, 0.0F, true, 0.0F, canvas);
        assertTrue(frame.woodyPixels() > 0);
        assertTrue(frame.woodyPixels() < frame.grownPixels());

        frame.update(100.0F, 0.0F, true, 0.0F, canvas);
        assertEquals(frame.length(), frame.grownPixels());
        assertEquals(frame.length(), frame.woodyPixels());
        canvas.repaint(new CanvasPainter(), frame::paintStem);
        // A stem that doesn't sway and is done growing doesn't change anymore
        frame.update(101.0F, 1.0F, true, 0.0F, canvas);
        assertFalse(canvas.hasPending());

        frame.update(101.0F, 1.0F, false, 0.0F, canvas);
        assertEquals(0, frame.grownPixels());
        assertFalse(frame.isVisible());
        assertTrue(canvas.hasPending());
    }

    @Test
    void flowersOnVinesFollowTheFlowerAmount() {
        List<Creeper> creepers = grow(false, WIDTH, HEIGHT);
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resizeToScreen(WIDTH, HEIGHT);
        int none = countBlooms(creepers, 0.0F, canvas);
        int quarter = countBlooms(creepers, 0.25F / 3.0F, canvas);
        int full = countBlooms(creepers, 1.0F / 3.0F, canvas);
        int triple = countBlooms(creepers, 1.0F, canvas);
        assertEquals(0, none);
        assertTrue((quarter > 0) && (quarter < full) && (full < triple), quarter + " < " + full + " < " + triple);
    }

    private static int countBlooms(List<Creeper> creepers, float threshold, TiledCanvas canvas) {
        int count = 0;
        for (Creeper creeper : creepers) {
            // Long enough for every flower to open, whenever it got let in
            creeper.update(300.0F, 0.0F, true, threshold, canvas);
            creeper.update(400.0F, 0.0F, true, threshold, canvas);
            for (int f = 0; f < creeper.blooms.length; f++) {
                if (creeper.bloomStage(f) != OvergrowthArt.BLOOM_HIDDEN) count++;
            }
        }
        return count;
    }

    @Test
    void paintingOnlyWhatChangedGivesTheSamePictureAsPaintingEverything() {
        List<Creeper> left = grow(false, WIDTH, HEIGHT);
        List<Creeper> right = CreeperGrower.grow(new PixelRandom(606), 7171, true, WIDTH, HEIGHT);
        TiledCanvas canvas = new TiledCanvas("incremental");
        canvas.resizeToScreen(WIDTH, HEIGHT);
        CanvasPainter painter = new CanvasPainter();
        TiledCanvas.Content content = p -> {
            for (List<Creeper> side : List.of(left, right)) {
                for (Creeper creeper : side) creeper.paintStem(p);
                for (Creeper creeper : side) creeper.paintDecor(p);
            }
        };
        // Growing, blooming at a changing flower amount, swaying, and a side that goes away and grows back
        for (int frame = 0; frame < 1800; frame++) {
            float time = frame / 30.0F;
            float clock = time * 1.25F;
            float threshold = (frame < 900) ? (1.0F / 3.0F) : 1.0F;
            boolean leftOn = (frame < 1200) || (frame > 1300);
            float leftClock = (frame > 1300) ? ((frame - 1300) / 30.0F * 1.25F) : clock;
            for (Creeper creeper : left) creeper.update(leftClock, time, leftOn, threshold, canvas);
            for (Creeper creeper : right) creeper.update(clock, time, true, threshold, canvas);
            canvas.repaint(painter, content);
        }
        TiledCanvas full = new TiledCanvas("full");
        full.resizeToScreen(WIDTH, HEIGHT);
        full.invalidateAll();
        full.repaint(painter, content);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) assertEquals(full.get(x, y), canvas.get(x, y), "pixel " + x + "," + y);
        }
    }

}
