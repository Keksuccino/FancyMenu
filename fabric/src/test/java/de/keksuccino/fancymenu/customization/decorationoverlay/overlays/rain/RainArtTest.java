package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RainArtTest {

    @Test
    void aNearStreakHasItsHeadBottomRightAndFollowsTheSlope() {
        int slopeIndex = RainArt.slopeIndex(0.25F);
        PixelSprite streak = RainArt.buildStreak(12, slopeIndex, true);
        int offset = RainArt.tailOffset(12, slopeIndex);
        assertEquals(3, offset);
        assertEquals(offset + 1, streak.width());
        assertEquals(12, streak.height());
        assertEquals('h', streak.codeAt(offset, 11));
        for (int i = 0; i < 12; i++) {
            int column = offset - Math.round(i * 0.25F);
            int row = 11 - i;
            char expected = (i == 0) ? 'h' : ((i < 4) ? 'a' : ((i < 8) ? 'b' : (((i & 1) == 1) ? '.' : 'c')));
            assertEquals(expected, streak.codeAt(column, row), "pixel " + i);
        }
    }

    @Test
    void everyRowOfAStreakHasExactlyOnePixelOrNoneInTheDitheredTail() {
        PixelSprite streak = RainArt.buildStreak(13, RainArt.slopeIndex(0.45F), true);
        for (int row = 0; row < streak.height(); row++) {
            int pixels = 0;
            for (int x = 0; x < streak.width(); x++) if (streak.codeAt(x, row) != '.') pixels++;
            int i = streak.height() - 1 - row;
            assertEquals(((i >= 8) && ((i & 1) == 1)) ? 0 : 1, pixels, "row " + row);
        }
    }

    @Test
    void farStreaksHaveTwoOpacitySteps() {
        int slopeIndex = RainArt.slopeIndex(0.3F);
        float slope = slopeIndex / (float)RainArt.SLOPE_STEPS;
        PixelSprite streak = RainArt.buildStreak(5, slopeIndex, false);
        int offset = RainArt.tailOffset(5, slopeIndex);
        for (int i = 0; i < 5; i++) assertEquals((i < 2) ? 'a' : 'b', streak.codeAt(offset - Math.round(i * slope), 4 - i), "pixel " + i);
    }

    @Test
    void slopesSnapToTheirStepAndStayInRange() {
        assertEquals(16, RainArt.slopeIndex(0.25F));
        assertEquals(0, RainArt.slopeIndex(-1.0F));
        assertEquals(RainArt.SLOPE_STEPS - 1, RainArt.slopeIndex(3.0F));
    }

    @Test
    void streakSpritesGetBuiltOnceAndKept() {
        assertSame(RainArt.nearStreak(10, 20), RainArt.nearStreak(10, 20));
        assertSame(RainArt.farStreak(4, 20), RainArt.farStreak(4, 20));
    }

}
