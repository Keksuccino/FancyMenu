package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfettiArtTest {

    @Test
    void paperNarrowsToAnEdgeAndShowsItsBack() {
        PixelSprite face = ConfettiArt.paper(4, 3, 4, 0, false, false);
        assertEquals(4, face.width());
        assertEquals(3, face.height());
        assertEquals("ffff", row(face, 1));
        PixelSprite edge = ConfettiArt.paper(4, 3, 1, 0, true, false);
        assertEquals(1, edge.width());
        assertEquals("b", row(edge, 2));
    }

    @Test
    void aHalfTurnedPieceShearsItsTopHalf() {
        PixelSprite right = ConfettiArt.paper(4, 4, 2, 1, false, false);
        assertEquals(3, right.width());
        assertEquals(".ff", row(right, 0));
        assertEquals(".ff", row(right, 1));
        assertEquals("ff.", row(right, 2));
        PixelSprite left = ConfettiArt.paper(4, 4, 2, -1, false, false);
        assertEquals("ff.", row(left, 0));
        assertEquals(".ff", row(left, 3));
    }

    @Test
    void foilGlintsInItsTopLeftPixel() {
        PixelSprite foil = ConfettiArt.paper(3, 3, 3, 0, false, true);
        assertEquals("gff", row(foil, 0));
        assertEquals("fff", row(foil, 1));
    }

    @Test
    void streamersAreTwoPixelsWideWithTheBackOnTheRight() {
        for (int length = ConfettiArt.STREAMER_MIN_LENGTH; length <= ConfettiArt.STREAMER_MAX_LENGTH; length++) {
            for (int step = 0; step < ConfettiArt.STREAMER_PHASES; step++) {
                PixelSprite streamer = ConfettiArt.streamer(length, step);
                assertEquals(length, streamer.height());
                for (int y = 0; y < length; y++) {
                    String row = row(streamer, y);
                    int first = indexOfPixel(row);
                    assertEquals(2, row.replace(".", "").length(), "row " + y);
                    assertEquals('b', row.charAt(first + 1));
                }
                assertTrue(ConfettiArt.streamerOffset(length, step) >= -2 && ConfettiArt.streamerOffset(length, step) <= 2);
            }
        }
    }

    @Test
    void phaseStepsWrapAroundTheTwist() {
        assertEquals(0, ConfettiArt.streamerPhaseStep(0.0F));
        assertEquals(0, ConfettiArt.streamerPhaseStep(PixelMath.TAU));
        assertEquals(ConfettiArt.STREAMER_PHASES / 4, ConfettiArt.streamerPhaseStep(PixelMath.TAU / 4.0F + 2.0F * PixelMath.TAU));
        assertTrue(ConfettiArt.streamerPhaseStep(-1.0F) >= 0);
    }

    @Test
    void litterLiesFlatAndBlinksInDitherSteps() {
        assertEquals("fffb", row(ConfettiArt.litter(4, 0), 0));
        assertEquals("ffb", row(ConfettiArt.litter(3, 0), 0));
        assertEquals("ff", row(ConfettiArt.litter(2, 0), 0));
        assertEquals("ffbbff", row(ConfettiArt.litter(6, 0), 0));
        assertEquals("f.b.f.", row(ConfettiArt.litter(6, 1), 0));
        assertEquals(".f.b.f", row(ConfettiArt.litter(6, 2), 0));
        assertEquals(6, ConfettiArt.litterWidth(ConfettiArt.TYPE_STREAMER, true));
        assertEquals(2, ConfettiArt.litterWidth(ConfettiArt.TYPE_DOT, true));
        assertEquals(3, ConfettiArt.litterWidth(ConfettiArt.TYPE_FOIL, false));
    }

    @Test
    void theCustomColorGetsADarkerBackSide() {
        ConfettiArt art = new ConfettiArt();
        art.setCustomColor(0xFFFF0000);
        assertEquals(0xFFFF0000, art.palettes[ConfettiArt.CUSTOM_PAIR].color('f'));
        int back = art.palettes[ConfettiArt.CUSTOM_PAIR].color('b');
        assertEquals(0xFF, back >>> 24);
        assertTrue(PixelMath.red(back) < 0xFF);
    }

    private static String row(PixelSprite sprite, int y) {
        StringBuilder builder = new StringBuilder();
        for (int x = 0; x < sprite.width(); x++) builder.append(sprite.codeAt(x, y));
        return builder.toString();
    }

    private static int indexOfPixel(String row) {
        for (int i = 0; i < row.length(); i++) if (row.charAt(i) != '.') return i;
        return -1;
    }

}
