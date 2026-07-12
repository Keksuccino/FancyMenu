package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurRendererTest {

    private static final float EPSILON = 1.0E-6F;

    @Test
    void convertsPhysicalBlurRadiusWithPreciseFractionalScale() {
        float guiRadius = GuiBlurRenderer.convertFramebufferBlurRadiusToGui(7.0F, 1.5D);

        assertEquals(7.0F / 1.5F, guiRadius, EPSILON);
        assertEquals(7.0F, GuiBlurRenderer.scaleBlurRadiusToFramebuffer(guiRadius, 1.5D), EPSILON);
    }

    @Test
    void fallsBackToUnitScaleForInvalidScaleValues() {
        assertEquals(7.0F, GuiBlurRenderer.convertFramebufferBlurRadiusToGui(7.0F, 0.0D));
        assertEquals(7.0F, GuiBlurRenderer.convertFramebufferBlurRadiusToGui(7.0F, -1.0D));
        assertEquals(7.0F, GuiBlurRenderer.convertFramebufferBlurRadiusToGui(7.0F, Double.NaN));
        assertEquals(7.0F, GuiBlurRenderer.convertFramebufferBlurRadiusToGui(7.0F, Double.POSITIVE_INFINITY));
    }

    @Test
    void validatesScaledGeometryDimensionsAndCoordinates() {
        assertTrue(GuiBlurRenderer.isValidScaledGeometry(-12.5F, 4.25F, 100.0F, 50.0F));
        assertFalse(GuiBlurRenderer.isValidScaledGeometry(0.0F, 0.0F, 0.0F, 50.0F));
        assertFalse(GuiBlurRenderer.isValidScaledGeometry(0.0F, 0.0F, 100.0F, -1.0F));
        assertFalse(GuiBlurRenderer.isValidScaledGeometry(Float.NaN, 0.0F, 100.0F, 50.0F));
        assertFalse(GuiBlurRenderer.isValidScaledGeometry(0.0F, Float.POSITIVE_INFINITY, 100.0F, 50.0F));
    }

    @Test
    void roundsFractionalScissorOutward() {
        GuiBlurRenderer.PostPassScissor scissor = GuiBlurRenderer.toPostPassScissor(new GuiBlurRenderer.ScissorBounds(0.25F, 1.25F, 10.5F, 20.5F), 1.5D, 100, 80);

        assertEquals(new GuiBlurRenderer.PostPassScissor(0, 49, 16, 30), scissor);
    }

    @Test
    void fullscreenScissorCoversEveryFramebufferPixelAtFractionalScale() {
        int framebufferWidth = 1921;
        int framebufferHeight = 1081;
        double guiScale = 1.5D;
        float guiWidth = (float)Math.ceil(framebufferWidth / guiScale);
        float guiHeight = (float)Math.ceil(framebufferHeight / guiScale);

        GuiBlurRenderer.PostPassScissor scissor = GuiBlurRenderer.toPostPassScissor(new GuiBlurRenderer.ScissorBounds(0.0F, 0.0F, guiWidth, guiHeight), guiScale, framebufferWidth, framebufferHeight);

        assertEquals(new GuiBlurRenderer.PostPassScissor(0, 0, framebufferWidth, framebufferHeight), scissor);
    }

    @Test
    void rejectsInvalidAndOverflowingScissorGeometry() {
        assertTrue(GuiBlurRenderer.toPostPassScissor(new GuiBlurRenderer.ScissorBounds(0.0F, 0.0F, 10.0F, 10.0F), 1.5D, 0, 100).isEmpty());
        assertTrue(GuiBlurRenderer.toPostPassScissor(new GuiBlurRenderer.ScissorBounds(0.0F, 0.0F, Float.NaN, 10.0F), 1.5D, 100, 100).isEmpty());
        assertTrue(GuiBlurRenderer.toPostPassScissor(new GuiBlurRenderer.ScissorBounds(0.0F, 0.0F, Float.MAX_VALUE, 10.0F), Double.MAX_VALUE, 100, 100).isEmpty());
    }

    @Test
    void rejectsInvalidAndOverflowingPhysicalBlurRadii() {
        assertEquals(0.0F, GuiBlurRenderer.scaleBlurRadiusToFramebuffer(Float.NaN, 1.5D));
        assertEquals(0.0F, GuiBlurRenderer.scaleBlurRadiusToFramebuffer(-1.0F, 1.5D));
        assertEquals(0.0F, GuiBlurRenderer.scaleBlurRadiusToFramebuffer(Float.MAX_VALUE, 2.0D));
        assertEquals(12.5F, GuiBlurRenderer.scaleBlurRadiusToFramebuffer(5.0F, 2.5D));
    }

    @Test
    void scalesCornerRadiiWithFractionalScale() {
        GuiBlurRenderer.CornerRadii radii = new GuiBlurRenderer.CornerRadii(1.0F, 2.0F, 3.0F, 4.0F).scaled(1.25D);

        assertEquals(new GuiBlurRenderer.CornerRadii(1.25F, 2.5F, 3.75F, 5.0F), radii);
    }

}
