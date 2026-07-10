package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurScaleMathTest {

    @Test
    void convertsFramebufferRadiusWithFractionalScale() {
        double guiScale = 800.0D / 309.0D;
        float guiRadius = GuiBlurScaleMath.convertFramebufferRadiusToGui(7.0F, guiScale);

        assertEquals(2.70375F, guiRadius);
        assertEquals(7.0F, GuiBlurScaleMath.scalePositiveRadius(guiRadius, guiScale), 1.0E-6F);
    }

    @Test
    void invalidScalesFallBackToOne() {
        assertEquals(1.0D, GuiBlurScaleMath.normalizeGuiScale(0.0D));
        assertEquals(1.0D, GuiBlurScaleMath.normalizeGuiScale(-2.0D));
        assertEquals(1.0D, GuiBlurScaleMath.normalizeGuiScale(Double.NaN));
        assertEquals(1.0D, GuiBlurScaleMath.normalizeGuiScale(Double.POSITIVE_INFINITY));
        assertEquals(7.0F, GuiBlurScaleMath.convertFramebufferRadiusToGui(7.0F, Double.NaN));
    }

    @Test
    void fractionalFullscreenAreaCoversEveryFramebufferPixel() {
        int targetWidth = 800;
        int targetHeight = 450;
        double guiScale = 800.0D / 309.0D;
        int guiWidth = (int)Math.ceil(targetWidth / guiScale);
        int guiHeight = (int)Math.ceil(targetHeight / guiScale);

        GuiBlurScaleMath.FramebufferArea area = GuiBlurScaleMath.scaleArea(0.0F, 0.0F, guiWidth, guiHeight, guiScale, targetHeight);

        assertTrue(area.isValid());
        assertEquals(800.0F, area.width());
        assertTrue(area.x() <= 0.0F);
        assertTrue(area.y() <= 0.0F);
        assertTrue(area.x() + area.width() >= targetWidth);
        assertTrue(area.y() + area.height() >= targetHeight);
    }

    @Test
    void preservesFractionalSubAreaGeometry() {
        GuiBlurScaleMath.FramebufferArea area = GuiBlurScaleMath.scaleArea(2.25F, 3.5F, 11.75F, 7.25F, 1.5D, 100);

        assertTrue(area.isValid());
        assertEquals(3.375F, area.x());
        assertEquals(83.875F, area.y());
        assertEquals(17.625F, area.width());
        assertEquals(10.875F, area.height());
    }

    @Test
    void rejectsNonPositiveAndOverflowedGeometryAndRadius() {
        assertFalse(GuiBlurScaleMath.scaleArea(0.0F, 0.0F, 0.0F, 10.0F, 1.5D, 100).isValid());
        assertFalse(GuiBlurScaleMath.scaleArea(0.0F, 0.0F, Float.MAX_VALUE, 10.0F, 2.0D, 100).isValid());
        assertEquals(0.0F, GuiBlurScaleMath.scalePositiveRadius(-1.0F, 1.5D));
        assertEquals(0.0F, GuiBlurScaleMath.scalePositiveRadius(Float.MAX_VALUE, 2.0D));
    }
}
