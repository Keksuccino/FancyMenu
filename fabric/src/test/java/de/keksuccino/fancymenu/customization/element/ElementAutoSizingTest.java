package de.keksuccino.fancymenu.customization.element;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ElementAutoSizingTest {

    @ParameterizedTest
    @CsvSource({"NaN, 1", "Infinity, 1", "-Infinity, 1", "0.0, 1", "-2.0, 1", "0.5, 1", "1.0, 1", "1.9, 1", "2.0, 2", "3.75, 3"})
    void guiScaleNormalizationUsesFinitePositiveFlooredIntegers(double scale, int expected) {
        assertEquals(expected, ElementAutoSizing.normalizeGuiScale(scale));
    }

    @ParameterizedTest
    @CsvSource({"1920, 2, 960", "1921, 2, 961", "1, 3, 1", "0, 2, 0", "1920, 0, 0", "-1, 2, 0"})
    void pixelBaselinesConvertToLogicalGuiDimensions(int pixels, int guiScale, int expected) {
        assertEquals(expected, ElementAutoSizing.calculateGuiDimension(pixels, guiScale));
    }

    @ParameterizedTest
    @CsvSource({"1920, 1080, 1920, 1080, 1.0", "3840, 2160, 1920, 1080, 2.0", "960, 540, 1920, 1080, 0.5", "1920, 540, 1920, 1080, 0.5", "1, 1, 100, 100, 0.01", "0, 1080, 1920, 1080, 1.0"})
    void scaleFactorUsesSmallestLogicalCoordinateRatio(int currentWidth, int currentHeight, int baseWidth, int baseHeight, double expected) {
        assertEquals(expected, ElementAutoSizing.calculateScaleFactor(currentWidth, currentHeight, baseWidth, baseHeight));
    }

    @ParameterizedTest
    @CsvSource({"101, 0.5, 50", "100, 0.75, 75", "1, 0.01, 1", "0, 2.0, 1", "-10, 2.0, 1"})
    void elementDimensionsTruncateAndNeverDropBelowOne(int baseDimension, double scaleFactor, int expected) {
        assertEquals(expected, ElementAutoSizing.calculateElementDimension(baseDimension, scaleFactor));
    }

    @Test
    void guiScaleChangeResizesFromLogicalBaselineCoordinates() {
        int baseWidth = ElementAutoSizing.calculateGuiDimension(1920, 2);
        int baseHeight = ElementAutoSizing.calculateGuiDimension(1080, 2);
        double scaleFactor = ElementAutoSizing.calculateScaleFactor(640, 360, baseWidth, baseHeight);

        assertEquals(2.0D / 3.0D, scaleFactor);
        assertEquals(66, ElementAutoSizing.calculateElementDimension(100, scaleFactor));
        assertEquals(13, ElementAutoSizing.calculateElementDimension(20, scaleFactor));
    }

    @Test
    void vanillaScaleInferenceMatchesMaximumAndAutomaticModes() {
        assertEquals(3, ElementAutoSizing.inferVanillaGuiScale(1920, 1080, 3, false));
        assertEquals(4, ElementAutoSizing.inferVanillaGuiScale(1920, 1080, 0, false));
        assertEquals(4, ElementAutoSizing.inferVanillaGuiScale(1920, 1080, 3, true));
        assertEquals(1, ElementAutoSizing.inferVanillaGuiScale(320, 240, 0, false));
    }

    @Test
    void savedScaleHasHighestResolutionPriority() {
        assertEquals(3, ElementAutoSizing.resolveBaseGuiScale(3, 1920, 1080, 960, 540, 2, 4.0D, 1, false));
    }

    @Test
    void exactCurrentBaselineMatchPrecedesForcedAndVanillaScale() {
        assertEquals(2, ElementAutoSizing.resolveBaseGuiScale(0, 1920, 1080, 960, 540, 2, 4.0D, 1, false));
    }

    @Test
    void forcedScalePrecedesVanillaInferenceForLegacyMismatch() {
        assertEquals(3, ElementAutoSizing.resolveBaseGuiScale(0, 1920, 1080, 1000, 600, 2, 3.75D, 1, false));
    }

    @Test
    void vanillaInferenceIsFallbackForLegacyMismatchWithoutForcedScale() {
        assertEquals(4, ElementAutoSizing.resolveBaseGuiScale(0, 1920, 1080, 1000, 600, 2, 0.0D, 0, false));
    }

    @Test
    void stackedBaselineOverrideCopiesAllFieldsIncludingLegacyZeroScale() {
        ElementAutoSizing.Baseline override = new ElementAutoSizing.Baseline(1920, 1080, 0);
        assertSame(override, ElementAutoSizing.selectStackedBaseline(new ElementAutoSizing.Baseline(1280, 720, 2), override));
    }

    @Test
    void emptyStackedBaselineKeepsAllCurrentFieldsAtomically() {
        ElementAutoSizing.Baseline current = new ElementAutoSizing.Baseline(1280, 720, 2);
        assertSame(current, ElementAutoSizing.selectStackedBaseline(current, new ElementAutoSizing.Baseline(0, 0, 0)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1080})
    void partialStackedBaselineOverridesAsOneAtomicRecord(int overrideHeight) {
        ElementAutoSizing.Baseline override = new ElementAutoSizing.Baseline(1920, overrideHeight, 0);
        assertSame(override, ElementAutoSizing.selectStackedBaseline(new ElementAutoSizing.Baseline(1280, 720, 2), override));
    }

}
