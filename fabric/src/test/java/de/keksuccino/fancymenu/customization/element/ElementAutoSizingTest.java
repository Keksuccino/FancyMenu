package de.keksuccino.fancymenu.customization.element;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementAutoSizingTest {

    @Test
    void preservesFractionalBaselineGuiScale() {
        ElementAutoSizing.Baseline baseline = ElementAutoSizing.captureBaseline(1280, 720, 1.5D);

        assertEquals(1920, baseline.pixelWidth());
        assertEquals(1080, baseline.pixelHeight());
        assertEquals(1.5D, baseline.guiScale());
        assertEquals(1280, ElementAutoSizing.calculateGuiDimension(baseline.pixelWidth(), baseline.guiScale()));
        assertEquals(720, ElementAutoSizing.calculateGuiDimension(baseline.pixelHeight(), baseline.guiScale()));
        assertEquals(1.0D, ElementAutoSizing.calculateScaleFactor(1280, 720, ElementAutoSizing.calculateGuiDimension(baseline.pixelWidth(), baseline.guiScale()), ElementAutoSizing.calculateGuiDimension(baseline.pixelHeight(), baseline.guiScale())));
    }

    @Test
    void preservesPhysicalElementSizeAcrossGuiScaleChanges() {
        ElementAutoSizing.Baseline baseline = new ElementAutoSizing.Baseline(1920, 1080, 1.5D);
        int baseLogicalWidth = ElementAutoSizing.calculateGuiDimension(baseline.pixelWidth(), baseline.guiScale());
        int baseLogicalHeight = ElementAutoSizing.calculateGuiDimension(baseline.pixelHeight(), baseline.guiScale());
        double factor = ElementAutoSizing.calculateScaleFactor(960, 540, baseLogicalWidth, baseLogicalHeight);
        int resizedElementWidth = ElementAutoSizing.calculateElementDimension(100, factor);

        assertEquals(0.75D, factor);
        assertEquals(75, resizedElementWidth);
        assertEquals(100 * baseline.guiScale(), resizedElementWidth * 2.0D);
    }

    @Test
    void usesLimitingAxisWithoutChangingAspectRatio() {
        double factor = ElementAutoSizing.calculateScaleFactor(1600, 600, 1280, 720);

        assertEquals(600.0D / 720.0D, factor);
        assertEquals(166, ElementAutoSizing.calculateElementDimension(200, factor));
        assertEquals(83, ElementAutoSizing.calculateElementDimension(100, factor));
    }

    @Test
    void handlesInvalidDimensionsAndScalesDeterministically() {
        assertEquals(1.0D, ElementAutoSizing.normalizeGuiScale(0.0D));
        assertEquals(1.0D, ElementAutoSizing.normalizeGuiScale(Double.NaN));
        assertEquals(1.0D, ElementAutoSizing.normalizeGuiScale(Double.POSITIVE_INFINITY));
        assertEquals(0, ElementAutoSizing.calculateGuiDimension(1920, 0.0D));
        assertEquals(0, ElementAutoSizing.calculateGuiDimension(0, 1.5D));
        assertEquals(1.0D, ElementAutoSizing.calculateScaleFactor(0, 720, 1280, 720));
        assertEquals(100, ElementAutoSizing.calculateElementDimension(100, Double.NaN));
    }

    @Test
    void enforcesMinimumScaleAndElementDimensions() {
        double factor = ElementAutoSizing.calculateScaleFactor(1, 1, 1000, 1000);

        assertEquals(0.01D, factor);
        assertEquals(1, ElementAutoSizing.calculateElementDimension(50, factor));
        assertEquals(1, ElementAutoSizing.calculateElementDimension(0, factor));
        assertEquals(1, ElementAutoSizing.calculateElementDimension(-10, factor));
    }

    @Test
    void infersVanillaAutomaticAndUnicodeGuiScales() {
        assertEquals(4.0D, ElementAutoSizing.inferVanillaGuiScale(1920, 1080, 0, false));
        assertEquals(3.0D, ElementAutoSizing.inferVanillaGuiScale(1366, 768, 0, false));
        assertEquals(4.0D, ElementAutoSizing.inferVanillaGuiScale(1366, 768, 0, true));
        assertEquals(2.0D, ElementAutoSizing.inferVanillaGuiScale(1920, 1080, 2, false));
    }

    @Test
    void migratesLegacyForcedScaleWithoutFlooringIt() {
        ElementAutoSizing.Baseline legacyBaseline = new ElementAutoSizing.Baseline(1800, 1000, 0.0D);

        assertEquals(1.5D, ElementAutoSizing.resolveBaselineGuiScale(legacyBaseline, 800, 450, 2.0D, 1.5D, 0, false));
        assertEquals(2.0D, ElementAutoSizing.resolveBaselineGuiScale(new ElementAutoSizing.Baseline(1600, 900, 0.0D), 800, 450, 2.0D, 1.5D, 0, false));
    }

    @Test
    void stackingSelectsAllBaselineMetadataAtomically() {
        ElementAutoSizing.Baseline current = new ElementAutoSizing.Baseline(100, 200, 1.25D);
        ElementAutoSizing.Baseline partialCandidate = new ElementAutoSizing.Baseline(300, 0, 1.5D);
        ElementAutoSizing.Baseline emptyCandidate = new ElementAutoSizing.Baseline(0, 0, 2.0D);

        assertEquals(partialCandidate, ElementAutoSizing.selectStackedBaseline(current, partialCandidate));
        assertEquals(current, ElementAutoSizing.selectStackedBaseline(current, emptyCandidate));
    }

}
