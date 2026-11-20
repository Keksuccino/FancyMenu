package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cobwebs;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobwebsTest {

    @Test
    void websAreFixedToTheTopAndTheSideEdge() {
        for (CobwebShape shape : new CobwebShape[]{CobwebShape.OPEN, CobwebShape.CLOSED}) {
            int alongTop = 0;
            int alongSide = 0;
            for (int i = 0; i < shape.pixelCount(); i++) {
                if (shape.ys[i] == 0) alongTop = Math.max(alongTop, shape.xs[i]);
                if (shape.xs[i] == 0) alongSide = Math.max(alongSide, shape.ys[i]);
            }
            assertTrue(alongTop >= 90, "thread along the top edge ends at " + alongTop);
            assertTrue(alongSide >= 70, "thread along the side edge ends at " + alongSide);
        }
    }

    @Test
    void everyWebPixelExistsOnce() {
        for (CobwebShape shape : new CobwebShape[]{CobwebShape.OPEN, CobwebShape.CLOSED}) {
            Set<Integer> seen = new HashSet<>();
            for (int i = 0; i < shape.pixelCount(); i++) assertTrue(seen.add(shape.ys[i] * 1000 + shape.xs[i]));
        }
    }

    @Test
    void aClosedFrontHasARimAndNothingSticksOut() {
        assertTrue(hasCode(CobwebShape.CLOSED, 'b'));
        assertFalse(hasCode(CobwebShape.OPEN, 'b'));
        // Open, the radial threads run on past the outermost ring. Closed, they end at the rim.
        assertTrue(farthest(CobwebShape.OPEN) > farthest(CobwebShape.CLOSED));
        assertTrue(farthest(CobwebShape.CLOSED) <= 109.0);
    }

    private static boolean hasCode(CobwebShape shape, char code) {
        for (int y = 0; y < shape.sprite.height(); y++) {
            for (int x = 0; x < shape.sprite.width(); x++) {
                if (shape.sprite.codeAt(x, y) == code) return true;
            }
        }
        return false;
    }

    private static double farthest(CobwebShape shape) {
        double farthest = 0.0;
        for (int i = 0; i < shape.pixelCount(); i++) farthest = Math.max(farthest, Math.hypot(shape.xs[i], shape.ys[i]));
        return farthest;
    }

    @Test
    void aSpiderDropsHangsAndClimbsBack() {
        PixelRandom random = new PixelRandom(3);
        CobwebSpider spider = new CobwebSpider(0, 24, 20, 1.0F);
        spider.advance(0.5F, false, random);
        assertEquals(CobwebSpider.WAITING, spider.state);
        spider.advance(0.6F, false, random);
        assertEquals(CobwebSpider.DROPPING, spider.state);
        assertTrue(spider.isMoving());

        int frames = 0;
        while ((spider.state == CobwebSpider.DROPPING) && (frames++ < 1000)) spider.advance(0.05F, false, random);
        assertEquals(CobwebSpider.HANGING, spider.state);
        assertFalse(spider.isMoving());
        assertTrue((spider.drop >= 34.0F) && (spider.drop <= 78.0F), "hangs at " + spider.drop);

        frames = 0;
        while ((spider.state != CobwebSpider.WAITING) && (frames++ < 1000)) spider.advance(0.05F, false, random);
        assertEquals(CobwebSpider.WAITING, spider.state);
        assertEquals(0.0F, spider.drop);
        assertTrue(spider.timer >= 5.0F);
    }

    @Test
    void aScaredSpiderScurriesUpQuickly() {
        PixelRandom random = new PixelRandom(3);
        CobwebSpider spider = new CobwebSpider(0, 24, 20, 0.01F);
        spider.advance(0.02F, false, random);
        for (int i = 0; i < 40; i++) spider.advance(0.05F, false, random);
        float drop = spider.drop;
        assertTrue(drop > 30.0F);

        spider.advance(0.1F, true, random);
        assertEquals(CobwebSpider.SCURRYING, spider.state);
        assertEquals(drop - 7.5F, spider.drop, 1.0E-3F);
        // It keeps going once it got scared, much faster than it climbs
        int frames = 0;
        while ((spider.drop > 0.0F) && (frames++ < 100)) spider.advance(0.05F, false, random);
        assertTrue(frames * 0.05F < 1.2F, "took " + (frames * 0.05F) + " seconds");
        assertEquals(CobwebSpider.WAITING, spider.state);
    }

}
