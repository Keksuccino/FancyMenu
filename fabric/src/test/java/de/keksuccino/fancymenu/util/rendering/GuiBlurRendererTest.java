package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurRendererTest {

    @Test
    void fractionalScaleKeepsFullscreenMaskAtFramebufferEdge() {
        double guiScale = 1.152D;
        float legacyWidth = 625.0F * (float)guiScale;

        GuiBlurRenderer.FramebufferBlurArea area = GuiBlurRenderer.calculateFramebufferBlurArea_FancyMenu(0.0F, 0.0F, 625.0F, 625.0F, 3.0F, guiScale, 720);

        assertTrue(legacyWidth < 720.0F);
        assertTrue(area.isValid_FancyMenu());
        assertEquals(0.0F, area.x());
        assertEquals(0.0F, area.y());
        assertEquals(720.0F, area.width());
        assertEquals(720.0F, area.height());
        assertEquals((float)(3.0D * guiScale), area.blurRadius());
    }

    @Test
    void framebufferRadiusConversionPreservesPhysicalRadiusAtFractionalScale() {
        double guiScale = 1.5D;
        float guiRadius = GuiBlurRenderer.convertFramebufferBlurRadiusToGui(7.0F, guiScale);

        assertEquals(7.0F, (float)(guiRadius * guiScale), 0.000001F);
    }

    @Test
    void guiScissorMarginReturnsToPhysicalBlurSupportAfterScaling() {
        double guiScale = 1.5D;
        float framebufferBlurRadius = 7.0F;

        float guiMargin = GuiBlurRenderer.calculateGuiScissorMargin_FancyMenu(framebufferBlurRadius, guiScale);

        assertEquals(framebufferBlurRadius * 4.0F, (float)(guiMargin * guiScale), 0.000001F);
    }

    @Test
    void invalidScaleFallsBackToUnitScale() {
        GuiBlurRenderer.FramebufferBlurArea area = GuiBlurRenderer.calculateFramebufferBlurArea_FancyMenu(2.0F, 3.0F, 4.0F, 5.0F, 6.0F, Double.NaN, 20);

        assertTrue(area.isValid_FancyMenu());
        assertEquals(1.0D, area.guiScale());
        assertEquals(2.0F, area.x());
        assertEquals(12.0F, area.y());
        assertEquals(4.0F, area.width());
        assertEquals(5.0F, area.height());
        assertEquals(6.0F, area.blurRadius());
        assertEquals(7.0F, GuiBlurRenderer.convertFramebufferBlurRadiusToGui(7.0F, 0.0D));
    }

    @Test
    void nonFiniteGeometryIsRejectedBeforeRendering() {
        GuiBlurRenderer.FramebufferBlurArea area = GuiBlurRenderer.calculateFramebufferBlurArea_FancyMenu(Float.POSITIVE_INFINITY, 0.0F, 10.0F, 10.0F, 2.0F, 1.5D, 100);

        assertFalse(area.isValid_FancyMenu());
    }

    @Test
    void overflowingBlurRadiusIsDisabled() {
        GuiBlurRenderer.FramebufferBlurArea area = GuiBlurRenderer.calculateFramebufferBlurArea_FancyMenu(0.0F, 0.0F, 10.0F, 10.0F, Float.MAX_VALUE, 2.0D, 100);

        assertTrue(area.isValid_FancyMenu());
        assertEquals(0.0F, area.blurRadius());
    }

}
