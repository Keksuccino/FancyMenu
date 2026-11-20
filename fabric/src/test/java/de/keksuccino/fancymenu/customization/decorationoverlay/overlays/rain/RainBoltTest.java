package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RainBoltTest {

    @Test
    void theMainChannelRunsFromTheTopToTheMiddleOfTheScreenWithoutGaps() {
        for (int seed = 0; seed < 20; seed++) {
            RainBolt bolt = new RainBolt();
            bolt.strike(new PixelRandom(seed), 480, 270, 1);
            assertEquals(0, bolt.mainY[0]);
            int last = bolt.mainCount - 1;
            assertTrue((bolt.mainY[last] >= 120) && (bolt.mainY[last] <= 172 + 15), "ends at row " + bolt.mainY[last]);
            for (int i = 1; i < bolt.mainCount; i++) {
                assertTrue(Math.abs(bolt.mainX[i] - bolt.mainX[i - 1]) <= 1, "seed " + seed + " point " + i);
                assertTrue((bolt.mainY[i] - bolt.mainY[i - 1] >= 0) && (bolt.mainY[i] - bolt.mainY[i - 1] <= 1), "seed " + seed + " point " + i);
            }
        }
    }

    @Test
    void aStrikeRunsThroughItsStagesAndEnds() {
        RainBolt bolt = new RainBolt();
        assertEquals(RainBolt.STAGE_NONE, bolt.stage());
        bolt.strike(new PixelRandom(3), 480, 270, 1);
        assertEquals(RainBolt.STAGE_STRIKE, bolt.stage());
        bolt.advance(0.09F);
        assertEquals(RainBolt.STAGE_GAP, bolt.stage());
        bolt.advance(0.05F);
        assertEquals(RainBolt.STAGE_RESTRIKE, bolt.stage());
        bolt.advance(0.1F);
        assertEquals(RainBolt.STAGE_AFTERGLOW, bolt.stage());
        bolt.advance(0.1F);
        assertEquals(RainBolt.STAGE_AFTERGLOW_FAINT, bolt.stage());
        bolt.advance(0.2F);
        assertFalse(bolt.isActive());
    }

    @Test
    void theCanvasShowsTheBoltOnlyWhileItIsVisibleAndIsClearAfterwards() {
        PixelCanvas canvas = new PixelCanvas("test");
        canvas.resize(480, 270);
        RainBolt bolt = new RainBolt();
        bolt.strike(new PixelRandom(5), 480, 270, 1);

        assertTrue(bolt.drawInto(canvas));
        assertEquals(0xFFFFFFFF, canvas.get(bolt.mainX[5], bolt.mainY[5]));
        bolt.advance(0.09F);
        assertFalse(bolt.drawInto(canvas));
        assertEquals(0, countVisible(canvas));
        bolt.advance(0.05F);
        assertTrue(bolt.drawInto(canvas));
        assertTrue(countVisible(canvas) > 0);
        bolt.advance(0.5F);
        assertFalse(bolt.drawInto(canvas));
        assertEquals(0, countVisible(canvas));
    }

    @Test
    void biggerSizesDrawEveryPointAsABlock() {
        PixelCanvas canvas = new PixelCanvas("test");
        canvas.resize(480, 270);
        RainBolt bolt = new RainBolt();
        bolt.strike(new PixelRandom(5), 240, 135, 2);
        bolt.drawInto(canvas);
        int x = bolt.mainX[3] * 2;
        int y = bolt.mainY[3] * 2;
        assertEquals(0xFFFFFFFF, canvas.get(x, y));
        assertEquals(0xFFFFFFFF, canvas.get(x + 1, y + 1));
    }

    private static int countVisible(PixelCanvas canvas) {
        int count = 0;
        for (int pixel : canvas.pixels()) if ((pixel >>> 24) != 0) count++;
        return count;
    }

}
