package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.embers;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.TiledCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AshDriftTest {

    private static AshDrift emptyDrift(boolean floor, int width) {
        AshDrift drift = new AshDrift(new PixelRandom(1), floor, 100, 50, width);
        Arrays.fill(drift.heights, 0.0F);
        return drift;
    }

    @Test
    void aDepositSpreadsOverThreeColumns() {
        AshDrift drift = emptyDrift(false, 40);
        drift.deposit(10, 1.0F);
        assertEquals(0.5F, drift.heights[10], 1.0E-5F);
        assertEquals(0.25F, drift.heights[9], 1.0E-5F);
        assertEquals(0.25F, drift.heights[11], 1.0E-5F);
        assertEquals(0.0F, drift.heights[8]);
    }

    @Test
    void aBigDepositSlumpsToTheSides() {
        AshDrift drift = emptyDrift(true, 200);
        drift.deposit(100, 6.0F);
        // Without slumping the middle would be 3 high and nothing would reach two columns away
        assertTrue(drift.heights[100] < 3.0F);
        assertTrue((drift.heights[98] > 0.0F) && (drift.heights[102] > 0.0F));
        float total = 0.0F;
        for (float h : drift.heights) total += h;
        assertEquals(6.0F, total, 1.0E-4F);
    }

    @Test
    void driftsOnElementsHaveRoundedEndsAndHangOverByOnePixel() {
        AshDrift drift = emptyDrift(false, 40);
        assertEquals(42, drift.columns);
        assertEquals(99, drift.x0);
        for (int i = 0; i < 40; i++) {
            drift.deposit(0, 0.5F);
            drift.deposit(41, 0.5F);
        }
        assertTrue(drift.heights[0] <= 0.8F + 1.0E-5F);
        assertTrue(drift.heights[41] <= 0.8F + 1.0E-5F);
        assertTrue(drift.heights[1] <= 1.6F + 1.0E-5F);
    }

    @Test
    void freshAshSinksBackToTheSettledShape() {
        AshDrift drift = new AshDrift(new PixelRandom(1), false, 100, 50, 40);
        float[] start = drift.heights.clone();
        drift.deposit(20, 2.0F);
        drift.sink(10_000.0F);
        for (int i = 0; i < drift.columns; i++) assertEquals(start[i] * 0.9F, drift.heights[i], 1.0E-4F);
    }

    @Test
    void onlyChangedColumnsGetPaintedAgain() {
        AshDrift drift = new AshDrift(new PixelRandom(1), false, 100, 50, 40);
        TiledCanvas canvas = new TiledCanvas("test");
        canvas.resizeToScreen(300, 100);
        CanvasPainter painter = new CanvasPainter();

        drift.sync(true, canvas);
        assertTrue(drift.isVisible());
        assertTrue(canvas.hasPending());
        canvas.repaint(painter, drift::paint);
        // The pale top edge of a column and the dark overhang under the left end
        int column = 20;
        int h = drift.height(column);
        assertEquals(AshDrift.ASH[0], canvas.get(drift.x0 + column, 50 - h));
        assertEquals(AshDrift.ASH[3], canvas.get(99, 50));

        drift.sync(true, canvas);
        assertFalse(canvas.hasPending());

        drift.deposit(column, 2.0F);
        drift.sync(true, canvas);
        assertTrue(canvas.hasPending());
        canvas.repaint(painter, drift::paint);
        assertEquals(AshDrift.ASH[0], canvas.get(drift.x0 + column, 50 - drift.height(column)));

        drift.sync(false, canvas);
        canvas.repaint(painter, drift::paint);
        assertEquals(0, canvas.get(drift.x0 + column, 49));
        assertEquals(0, canvas.get(99, 50));
    }

}
