package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuiTextureCoverRendererTest {

    @Test
    void widerTextureOverflowsHorizontallyAndRemainsCentered() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(-33, 50, 1066, 600), GuiTextureCoverRenderer.calculateBounds(100, 50, 800, 600, 1920, 1080, true));
    }

    @Test
    void tallerTextureOverflowsVerticallyAndRemainsCentered() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(7, -169, 1920, 1440), GuiTextureCoverRenderer.calculateBounds(7, 11, 1920, 1080, 4, 3, true));
    }

    @Test
    void matchingAspectRatioKeepsRequestedBounds() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(13, 27, 640, 360), GuiTextureCoverRenderer.calculateBounds(13, 27, 640, 360, 16, 9, true));
    }

    @Test
    void reportedRegressionDimensionsCoverTheWholeGui() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(0, -12, 1277, 718), GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, 694, 2560, 1440, true));
    }

    @Test
    void integerRoundingStillCoversTheArea() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(-1, 0, 7, 5), GuiTextureCoverRenderer.calculateBounds(0, 0, 5, 5, 3, 2, true));
    }

    @Test
    void stretchFillsExactlyTheRequestedAreaForAnyTextureAspectRatio() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(0, 0, 1277, 694), GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, 694, 2560, 1440, false));
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(0, 0, 1277, 694), GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, 694, 100, 400, false));
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(17, -9, 320, 180), GuiTextureCoverRenderer.calculateBounds(17, -9, 320, 180, 4096, 64, false));
    }

    @Test
    void stretchRejectsNonPositiveDimensions() {
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 0, 694, 2560, 1440, false));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, -1, 2560, 1440, false));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, 694, 2560, 0, false));
    }

    @Test
    void nonPositiveDimensionsAreRejected() {
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 0, 10, 16, 9, true));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 10, 0, 16, 9, true));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, -1, 10, 16, 9, true));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 10, -1, 16, 9, true));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 10, 10, 0, 9, true));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 10, 10, 16, 0, true));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 10, 10, -1, 9, true));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 10, 10, 16, -1, true));
    }

}
