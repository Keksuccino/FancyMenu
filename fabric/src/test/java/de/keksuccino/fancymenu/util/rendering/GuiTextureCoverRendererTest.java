package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuiTextureCoverRendererTest {

    @Test
    void preservesExactAspectRatio() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(10, 20, 320, 180), GuiTextureCoverRenderer.calculateBounds(10, 20, 320, 180, 1920, 1080));
    }

    @Test
    void coversWiderAreaByOverflowingVertically() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(0, -100, 400, 400), GuiTextureCoverRenderer.calculateBounds(0, 0, 400, 200, 1, 1));
    }

    @Test
    void coversTallerAreaByOverflowingHorizontally() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(-100, 0, 400, 400), GuiTextureCoverRenderer.calculateBounds(0, 0, 200, 400, 1, 1));
    }

    @Test
    void centersOddOverflowUsingIntegerCoordinates() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(10, -5, 101, 101), GuiTextureCoverRenderer.calculateBounds(10, 20, 101, 50, 1, 1));
    }

    @Test
    void centersRelativeToNegativeOrigin() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(-113, -7, 400, 400), GuiTextureCoverRenderer.calculateBounds(-13, -7, 200, 400, 1, 1));
    }

    @Test
    void coversReported1277By694GuiSizeWithoutRepeating() {
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(0, -12, 1277, 718), GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, 694, 1920, 1080));
    }

    @Test
    void rejectsInvalidDimensions() {
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 0, 100, 16, 9));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 100, 0, 16, 9));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, -1, 100, 16, 9));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 100, -1, 16, 9));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 100, 100, 0, 9));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 100, 100, 16, 0));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 100, 100, -1, 9));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 100, 100, 16, -1));
    }
}
