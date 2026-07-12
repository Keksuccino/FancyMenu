package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurGeometryTest {

    private static final float EPSILON = 1.0E-5F;

    @Test
    void scalesFractionalGeometryInDoubleBeforeNarrowing() {
        GuiBlurGeometry.ScaledArea scaled = GuiBlurGeometry.scaleArea(90.0F, 0.0F, 90.0F, 90.0F, 4.0F, 117, 1.3D);

        assertNotNull(scaled);
        assertAll(() -> assertEquals(117.0D, scaled.x()), () -> assertEquals(0.0D, scaled.y()), () -> assertEquals(117.0D, scaled.width()), () -> assertEquals(117.0D, scaled.height()), () -> assertNotEquals((double)(90.0F * (float)1.3D), scaled.width()), () -> assertEquals(1.3D, scaled.guiScale()));
    }

    @Test
    void preciseScaledGeometrySurvivesIntoScissorCoverage() {
        double guiScale = 1.3D;
        GuiBlurGeometry.ScaledArea scaled = GuiBlurGeometry.scaleArea(7.0F, 10.0F, 91.0F, 73.0F, 4.0F, 200, guiScale);

        assertNotNull(scaled);
        assertNotEquals((double)(float)scaled.width(), scaled.width());
        GuiBlurGeometry.ScissorBounds bounds = GuiBlurGeometry.convertPixelBoundsToGui(scaled.x(), scaled.y(), scaled.x() + scaled.width(), scaled.y() + scaled.height(), 200, 200, guiScale);
        assertNotNull(bounds);
        assertPhysicalCoverage(bounds, guiScale, 200, scaled.x(), scaled.y(), scaled.x() + scaled.width(), scaled.y() + scaled.height());
    }

    @Test
    void fractionalFullscreenExtentCoversNonDivisibleFramebuffer() {
        assertAll(() -> assertFullscreenCoverage(1919, 1079, 1.3D), () -> assertFullscreenCoverage(2560, 1440, 2.75D));
    }

    @Test
    void invalidScalesFallBackToOne() {
        assertAll(() -> assertEquals(1.3D, GuiBlurGeometry.sanitizeGuiScale(1.3D)), () -> assertEquals(1.0D, GuiBlurGeometry.sanitizeGuiScale(0.0D)), () -> assertEquals(1.0D, GuiBlurGeometry.sanitizeGuiScale(-1.0D)), () -> assertEquals(1.0D, GuiBlurGeometry.sanitizeGuiScale(Double.NaN)), () -> assertEquals(1.0D, GuiBlurGeometry.sanitizeGuiScale(Double.POSITIVE_INFINITY)), () -> assertEquals(1.0D, GuiBlurGeometry.sanitizeGuiScale(Double.NEGATIVE_INFINITY)));
    }

    @Test
    void framebufferRadiusConversionRoundTripsAndRejectsInvalidValues() {
        for (double guiScale : new double[]{1.5D, 2.75D, 4.0D / 3.0D, 3.0D}) {
            float guiRadius = GuiBlurGeometry.convertFramebufferBlurRadiusToGui(7.0F, guiScale);
            GuiBlurGeometry.ScaledArea scaled = GuiBlurGeometry.scaleArea(0.0F, 0.0F, 10.0F, 10.0F, guiRadius, 100, guiScale);
            assertNotNull(scaled, "scale " + guiScale);
            assertEquals(7.0F, scaled.blurRadius(), EPSILON, "scale " + guiScale);
        }

        assertAll(() -> assertEquals(0.0F, GuiBlurGeometry.convertFramebufferBlurRadiusToGui(0.0F, 1.5D)), () -> assertEquals(0.0F, GuiBlurGeometry.convertFramebufferBlurRadiusToGui(-1.0F, 1.5D)), () -> assertEquals(0.0F, GuiBlurGeometry.convertFramebufferBlurRadiusToGui(Float.NaN, 1.5D)), () -> assertEquals(0.0F, GuiBlurGeometry.convertFramebufferBlurRadiusToGui(Float.POSITIVE_INFINITY, 1.5D)), () -> assertEquals(7.0F, GuiBlurGeometry.convertFramebufferBlurRadiusToGui(7.0F, Double.NaN), EPSILON), () -> assertEquals(0.0F, GuiBlurGeometry.convertFramebufferBlurRadiusToGui(Float.MAX_VALUE, Double.MIN_VALUE)));
    }

    @Test
    void invalidOrUnrepresentableGeometryIsRejected() {
        assertAll(() -> assertNull(GuiBlurGeometry.scaleArea(Float.NaN, 0.0F, 1.0F, 1.0F, 1.0F, 100, 1.0D)), () -> assertNull(GuiBlurGeometry.scaleArea(0.0F, Float.POSITIVE_INFINITY, 1.0F, 1.0F, 1.0F, 100, 1.0D)), () -> assertNull(GuiBlurGeometry.scaleArea(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 100, 1.0D)), () -> assertNull(GuiBlurGeometry.scaleArea(0.0F, 0.0F, 1.0F, -1.0F, 1.0F, 100, 1.0D)), () -> assertNull(GuiBlurGeometry.scaleArea(0.0F, 0.0F, Float.MAX_VALUE, 1.0F, 1.0F, 100, 2.0D)), () -> assertNull(GuiBlurGeometry.scaleArea(0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 100, Double.MIN_VALUE)), () -> assertNotNull(GuiBlurGeometry.scaleArea(-10.0F, 5.0F, 20.0F, 30.0F, Float.NaN, 100, 1.25D)));
    }

    @Test
    void cornerScalingUsesDoublePrecisionAndRejectsInvalidValues() {
        assertAll(() -> assertEquals(117.0F, GuiBlurGeometry.scaleCornerRadius(90.0F, 1.3D)), () -> assertNotEquals(90.0F * (float)1.3D, GuiBlurGeometry.scaleCornerRadius(90.0F, 1.3D)), () -> assertEquals(0.0F, GuiBlurGeometry.scaleCornerRadius(0.0F, 1.3D)), () -> assertEquals(0.0F, GuiBlurGeometry.scaleCornerRadius(-1.0F, 1.3D)), () -> assertEquals(0.0F, GuiBlurGeometry.scaleCornerRadius(Float.NaN, 1.3D)), () -> assertEquals(0.0F, GuiBlurGeometry.scaleCornerRadius(Float.POSITIVE_INFINITY, 1.3D)));
    }

    @Test
    void scissorBoundsRoundOutwardAndSurviveGuiGraphicsPhysicalTruncation() {
        double guiScale = 1.5D;
        GuiBlurGeometry.ScissorBounds bounds = GuiBlurGeometry.convertPixelBoundsToGui(10.1D, 20.1D, 100.1D, 90.1D, 200, 120, guiScale);

        assertNotNull(bounds);
        assertAll(() -> assertEquals(6, bounds.minX()), () -> assertEquals(19, bounds.minY()), () -> assertEquals(68, bounds.maxX()), () -> assertEquals(67, bounds.maxY()), () -> assertPhysicalCoverage(bounds, guiScale, 120, 10.1D, 20.1D, 100.1D, 90.1D));
    }

    @Test
    void fullscreenScissorCoversNonDivisibleFramebufferAfterPhysicalRoundTrip() {
        int framebufferWidth = 1919;
        int framebufferHeight = 1079;
        double guiScale = 1.3D;
        GuiBlurGeometry.ScissorBounds bounds = GuiBlurGeometry.convertPixelBoundsToGui(0.0D, 0.0D, framebufferWidth, framebufferHeight, framebufferWidth, framebufferHeight, guiScale);

        assertNotNull(bounds);
        assertAll(() -> assertEquals(0, bounds.minX()), () -> assertEquals(0, bounds.minY()), () -> assertPhysicalCoverage(bounds, guiScale, framebufferHeight, 0.0D, 0.0D, framebufferWidth, framebufferHeight));
    }

    @Test
    void scissorBoundsClampToFramebufferAndPreserveEmptyIntersections() {
        GuiBlurGeometry.ScissorBounds clamped = GuiBlurGeometry.convertPixelBoundsToGui(-10.0D, -20.0D, 210.0D, 130.0D, 200, 120, 1.5D);
        GuiBlurGeometry.ScissorBounds outside = GuiBlurGeometry.convertPixelBoundsToGui(250.0D, 20.0D, 260.0D, 40.0D, 200, 120, 1.5D);

        assertNotNull(clamped);
        assertNotNull(outside);
        assertAll(() -> assertEquals(new GuiBlurGeometry.ScissorBounds(0, 0, 134, 80), clamped), () -> assertFalse(clamped.isEmpty()), () -> assertTrue(outside.isEmpty()), () -> assertNull(GuiBlurGeometry.convertPixelBoundsToGui(Double.NaN, 0.0D, 1.0D, 1.0D, 200, 120, 1.5D)), () -> assertNull(GuiBlurGeometry.convertPixelBoundsToGui(2.0D, 0.0D, 1.0D, 1.0D, 200, 120, 1.5D)));
    }

    private static void assertPhysicalCoverage(GuiBlurGeometry.ScissorBounds bounds, double guiScale, int targetHeight, double requestedMinX, double requestedMinY, double requestedMaxX, double requestedMaxY) {
        int physicalX = (int)((double)bounds.minX() * guiScale);
        int physicalY = (int)((double)targetHeight - (double)bounds.maxY() * guiScale);
        int physicalWidth = (int)((double)(bounds.maxX() - bounds.minX()) * guiScale);
        int physicalHeight = (int)((double)(bounds.maxY() - bounds.minY()) * guiScale);
        assertAll(() -> assertTrue(physicalX <= requestedMinX), () -> assertTrue(physicalY <= requestedMinY), () -> assertTrue(physicalX + physicalWidth >= requestedMaxX), () -> assertTrue(physicalY + physicalHeight >= requestedMaxY));
    }

    private static void assertFullscreenCoverage(int framebufferWidth, int framebufferHeight, double guiScale) {
        int guiWidth = (int)Math.ceil((double)framebufferWidth / guiScale);
        int guiHeight = (int)Math.ceil((double)framebufferHeight / guiScale);
        GuiBlurGeometry.ScaledArea scaled = GuiBlurGeometry.scaleArea(0.0F, 0.0F, guiWidth, guiHeight, 7.0F, framebufferHeight, guiScale);
        assertNotNull(scaled);
        assertAll(() -> assertTrue(scaled.width() >= framebufferWidth), () -> assertTrue(scaled.height() >= framebufferHeight), () -> assertTrue(scaled.x() <= 0.0F), () -> assertTrue(scaled.y() <= 0.0F));
    }

}
