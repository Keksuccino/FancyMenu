package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TiledCanvasTest {

    private static final int RED = 0xFFFF0000;
    private static final int BLUE = 0xFF0000FF;

    @Test
    void tilesOnlyExistWhereSomethingGotPainted() {
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resize(0, 0, 200, 100, 64, 64);
        assertEquals(0, canvas.tileCount());

        // Erasing where nothing got painted creates nothing
        canvas.set(5, 5, 0);
        assertEquals(0, canvas.tileCount());

        canvas.set(70, 10, RED);
        assertEquals(1, canvas.tileCount());
        assertEquals(RED, canvas.get(70, 10));
        assertEquals(0, canvas.get(10, 10));
        assertEquals(0, canvas.get(500, 10));
    }

    @Test
    void theOriginMovesTheCanvasOnTheScreen() {
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resize(0, 90, 100, 10, 100, 10);
        canvas.set(5, 95, RED);
        canvas.set(5, 5, RED);

        assertEquals(RED, canvas.get(5, 95));
        assertEquals(0, canvas.get(5, 5));
        assertEquals(1, canvas.tileCount());
    }

    @Test
    void repaintClearsInvalidatedAreasAndPaintsThemAgainTileByTile() {
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resize(0, 0, 200, 100, 64, 64);
        CanvasPainter painter = new CanvasPainter();
        canvas.set(10, 10, RED);
        canvas.set(100, 10, RED);

        canvas.invalidate(0, 0, 80, 20);
        assertTrue(canvas.hasPending());
        List<int[]> clips = new ArrayList<>();
        canvas.repaint(painter, p -> {
            clips.add(new int[]{p.clipX0(), p.clipY0(), p.clipX1(), p.clipY1()});
            p.pixel(70, 5, BLUE);
        });

        assertEquals(2, clips.size());
        assertArrayEquals(new int[]{0, 0, 64, 20}, clips.get(0));
        assertArrayEquals(new int[]{64, 0, 80, 20}, clips.get(1));
        assertEquals(0, canvas.get(10, 10));
        assertEquals(RED, canvas.get(100, 10));
        assertEquals(BLUE, canvas.get(70, 5));
        assertFalse(canvas.hasPending());
    }

    @Test
    void invalidatedAreasOfOneTileGrowIntoOneRepaint() {
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resize(0, 0, 64, 64, 64, 64);
        canvas.invalidate(2, 2, 4, 4);
        canvas.invalidate(10, 20, 12, 22);
        List<int[]> clips = new ArrayList<>();
        canvas.repaint(new CanvasPainter(), p -> clips.add(new int[]{p.clipX0(), p.clipY0(), p.clipX1(), p.clipY1()}));

        assertEquals(1, clips.size());
        assertArrayEquals(new int[]{2, 2, 12, 22}, clips.getFirst());
    }

    @Test
    void thePainterStaysInsideTheRepaintedArea() {
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resize(0, 0, 64, 64, 64, 64);
        canvas.invalidate(10, 10, 20, 20);
        canvas.repaint(new CanvasPainter(), p -> p.rect(0, 0, 64, 64, RED));

        assertEquals(RED, canvas.get(10, 10));
        assertEquals(RED, canvas.get(19, 19));
        assertEquals(0, canvas.get(9, 10));
        assertEquals(0, canvas.get(20, 19));
    }

    @Test
    void translucentPixelsBlendOverWhatIsThere() {
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resize(0, 0, 8, 8, 8, 8);
        canvas.set(1, 1, RED);
        canvas.blend(1, 1, PixelMath.withAlpha(0x0000FF, 128));

        assertEquals(PixelMath.blendOver(RED, PixelMath.withAlpha(0x0000FF, 128)), canvas.get(1, 1));
    }

    @Test
    void aNewSizeDropsAllTiles() {
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resizeToScreen(100, 100);
        canvas.set(1, 1, RED);
        assertFalse(canvas.resizeToScreen(100, 100));
        assertEquals(1, canvas.tileCount());

        assertTrue(canvas.resizeToScreen(120, 100));
        assertEquals(0, canvas.tileCount());
        assertEquals(0, canvas.get(1, 1));
    }

}
