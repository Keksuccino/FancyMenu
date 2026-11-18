package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuiTextureCoverRendererTest {

    @Test
    void coversLogicalScreenWithCompleteReportImageAspectRatio() {
        GuiTextureCoverRenderer.CoverBounds bounds = GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, 694, 2560, 1440, true);

        assertEquals(new GuiTextureCoverRenderer.CoverBounds(0, -12, 1277, 718), bounds);
    }

    @Test
    void fullScreenCoverDoesNotInheritCreateWorldHeaderOffset() {
        GuiTextureCoverRenderer.CoverBounds fullScreenBounds = GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, 694, 2560, 1440, true);
        GuiTextureCoverRenderer.CoverBounds legacyBodyBounds = GuiTextureCoverRenderer.calculateBounds(0, 24, 1277, 694, 2560, 1440, true);

        assertEquals(new GuiTextureCoverRenderer.CoverBounds(0, -12, 1277, 718), fullScreenBounds);
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(0, 12, 1277, 718), legacyBodyBounds);
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
    void rejectsInvalidDimensions() {
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 0, 694, 2560, 1440, true));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, 694, 0, 1440, true));
    }

}
