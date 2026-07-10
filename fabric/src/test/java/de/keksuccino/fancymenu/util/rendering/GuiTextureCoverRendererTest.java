package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuiTextureCoverRendererTest {

    @Test
    void coversLogicalScreenWithCompleteReportImageAspectRatio() {
        GuiTextureCoverRenderer.CoverBounds bounds = GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, 694, 2560, 1440);

        assertEquals(new GuiTextureCoverRenderer.CoverBounds(0, -12, 1277, 718), bounds);
    }

    @Test
    void fullScreenCoverDoesNotInheritCreateWorldHeaderOffset() {
        GuiTextureCoverRenderer.CoverBounds fullScreenBounds = GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, 694, 2560, 1440);
        GuiTextureCoverRenderer.CoverBounds legacyBodyBounds = GuiTextureCoverRenderer.calculateBounds(0, 24, 1277, 694, 2560, 1440);

        assertEquals(new GuiTextureCoverRenderer.CoverBounds(0, -12, 1277, 718), fullScreenBounds);
        assertEquals(new GuiTextureCoverRenderer.CoverBounds(0, 12, 1277, 718), legacyBodyBounds);
    }

    @Test
    void rejectsInvalidDimensions() {
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 0, 694, 2560, 1440));
        assertNull(GuiTextureCoverRenderer.calculateBounds(0, 0, 1277, 694, 0, 1440));
    }
}
