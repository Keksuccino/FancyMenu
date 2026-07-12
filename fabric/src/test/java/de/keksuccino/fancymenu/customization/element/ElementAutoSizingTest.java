package de.keksuccino.fancymenu.customization.element;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementAutoSizingTest {

    @Test
    void preservesFinitePositiveGuiScalesIncludingFractionalAndSubUnitValues() {
        assertEquals(2.5D, ElementAutoSizing.sanitizeGuiScale(2.5D));
        assertEquals(0.75D, ElementAutoSizing.sanitizeGuiScale(0.75D));
        assertEquals(1.0D, ElementAutoSizing.sanitizeGuiScale(0.0D));
        assertEquals(1.0D, ElementAutoSizing.sanitizeGuiScale(-2.0D));
        assertEquals(1.0D, ElementAutoSizing.sanitizeGuiScale(Double.NaN));
        assertEquals(1.0D, ElementAutoSizing.sanitizeGuiScale(Double.POSITIVE_INFINITY));
        assertEquals(1.0D, ElementAutoSizing.sanitizeGuiScale(Double.NEGATIVE_INFINITY));
    }

    @Test
    void validatesOnlyCompleteLogicalBaselineMetadata() {
        assertTrue(ElementAutoSizing.isValidGuiDimensions(5363, 2676));
        assertFalse(ElementAutoSizing.isValidGuiDimensions(5363, 0));
        assertFalse(ElementAutoSizing.isValidGuiDimensions(0, 2676));
        assertFalse(ElementAutoSizing.isValidGuiDimensions(-1, 2676));
    }

    @Test
    void mirrorsTargetWindowDimensionMath() {
        assertEquals(769, ElementAutoSizing.calculateWindowGuiDimension(1921, 2.5D));
        assertEquals(711, ElementAutoSizing.calculateWindowGuiDimension(533, 0.75D));
        assertEquals(0, ElementAutoSizing.calculateWindowGuiDimension(1921, Double.NaN));
        assertEquals(0, ElementAutoSizing.calculateWindowGuiDimension(Integer.MAX_VALUE, Double.MIN_VALUE));
    }

    @Test
    void calculatesElementSizeFromLogicalGuiCoordinatesAcrossScaleChanges() {
        double factor = ElementAutoSizing.calculateScaleFactor(640, 360, 960, 540);

        assertEquals(2.0D / 3.0D, factor, 0.0000001D);
        assertEquals(133, ElementAutoSizing.calculateElementDimension(200, factor));
        assertEquals(13, ElementAutoSizing.calculateElementDimension(20, factor));
    }

    @Test
    void usesSmallerAxisAndKeepsScaleAndDimensionsAboveTheirMinimums() {
        assertEquals(0.5D, ElementAutoSizing.calculateScaleFactor(500, 800, 1000, 1000));
        assertEquals(0.01D, ElementAutoSizing.calculateScaleFactor(1, 1, 1000, 2000));
        assertEquals(1.0D, ElementAutoSizing.calculateScaleFactor(0, 800, 1000, 1000));
        assertEquals(1, ElementAutoSizing.calculateElementDimension(0, 0.5D));
        assertEquals(1, ElementAutoSizing.calculateElementDimension(20, 0.01D));
    }

    @Test
    void matchesLegacyBaselineUsingTheExactFractionalCaptureOperation() {
        assertTrue(ElementAutoSizing.matchesLegacyBaseline(1922, 1080, 769, 432, 2.5D));
        assertTrue(ElementAutoSizing.matchesLegacyBaseline(3733, 1862, 5363, 2676, 0.6961883408071748D));
        assertFalse(ElementAutoSizing.matchesLegacyBaseline(1920, 1080, 769, 432, 2.5D));
        assertFalse(ElementAutoSizing.matchesLegacyBaseline(1922, 1080, 769, 432, Double.NaN));
    }

    @Test
    void exactCurrentScreenMatchWinsForScaleOnlyLegacyMetadata() {
        ElementAutoSizing.Baseline baseline = ElementAutoSizing.resolveCurrentLegacyBaseline(3733, 1862, 5363, 2676, 0.6961883408071748D);

        assertEquals(5363, baseline.guiWidth());
        assertEquals(2676, baseline.guiHeight());
        assertEquals(0.6961883408071748D, baseline.guiScale());
        assertEquals(ElementAutoSizing.LegacyResolution.RECONSTRUCTED_UNIQUE, baseline.resolution());
    }

    @Test
    void reconstructsRealisticIntegerForcedScaleLegacyBaseline() {
        ElementAutoSizing.Baseline baseline = ElementAutoSizing.reconstructLegacyLayoutBaseline(1813, 1598, 4.0D, 2390, 1049);

        assertEquals(598, baseline.guiWidth());
        assertEquals(527, baseline.guiHeight());
        assertEquals(3.0326359832635985D, baseline.guiScale(), 0.000000000000001D);
        assertEquals(ElementAutoSizing.LegacyResolution.RECONSTRUCTED_UNIQUE, baseline.resolution());
    }

    @Test
    void reconstructsFractionalForcedScaleLegacyBaseline() {
        ElementAutoSizing.Baseline baseline = ElementAutoSizing.reconstructLegacyLayoutBaseline(3174, 1461, 3.5D, 1092, 1178);

        assertEquals(732, baseline.guiWidth());
        assertEquals(337, baseline.guiHeight());
        assertEquals(4.336375212224109D, baseline.guiScale(), 0.000000000000001D);
        assertEquals(ElementAutoSizing.LegacyResolution.RECONSTRUCTED_UNIQUE, baseline.resolution());
    }

    @Test
    void replaysTargetPercentOperationOrderForSubUnitDerivedScale() {
        ElementAutoSizing.Baseline baseline = ElementAutoSizing.reconstructLegacyLayoutBaseline(3733, 1863, 0.75D, 941, 2007);

        assertEquals(5363, baseline.guiWidth());
        assertEquals(2676, baseline.guiHeight());
        assertEquals(0.696188340807175D, baseline.guiScale(), 0.000000000000001D);
        assertTrue(baseline.resolution() != ElementAutoSizing.LegacyResolution.FALLBACK);
    }

    @Test
    void selectsProxyLogicalSizeDeterministicallyWhenLegacyTupleIsAmbiguous() {
        ElementAutoSizing.Baseline baseline = ElementAutoSizing.reconstructLegacyLayoutBaseline(327, 246, 1.0D, 320, 240);

        assertEquals(320, baseline.guiWidth());
        assertEquals(241, baseline.guiHeight());
        assertEquals(ElementAutoSizing.LegacyResolution.RECONSTRUCTED_AMBIGUOUS, baseline.resolution());
    }

    @Test
    void fallsBackWhenCandidateSafetyCapIsExceededOrDisabled() {
        ElementAutoSizing.Baseline capped = ElementAutoSizing.reconstructLegacyLayoutBaseline(1813, 1598, 4.0D, 2390, 1049, 1);
        ElementAutoSizing.Baseline disabled = ElementAutoSizing.reconstructLegacyLayoutBaseline(1813, 1598, 4.0D, 2390, 1049, 0);
        ElementAutoSizing.Baseline invalid = ElementAutoSizing.reconstructLegacyLayoutBaseline(0, 1598, 4.0D, 2390, 1049);

        assertEquals(ElementAutoSizing.LegacyResolution.FALLBACK, capped.resolution());
        assertEquals(ElementAutoSizing.LegacyResolution.FALLBACK, disabled.resolution());
        assertEquals(ElementAutoSizing.LegacyResolution.FALLBACK, invalid.resolution());
    }

    @Test
    void candidateCapBoundaryIncludesTheCompleteBoundedSearchArea() {
        ElementAutoSizing.Baseline belowBoundary = ElementAutoSizing.reconstructLegacyLayoutBaseline(1813, 1598, 4.0D, 2390, 1049, 63);
        ElementAutoSizing.Baseline atBoundary = ElementAutoSizing.reconstructLegacyLayoutBaseline(1813, 1598, 4.0D, 2390, 1049, 64);

        assertEquals(ElementAutoSizing.LegacyResolution.FALLBACK, belowBoundary.resolution());
        assertEquals(ElementAutoSizing.LegacyResolution.RECONSTRUCTED_UNIQUE, atBoundary.resolution());
        assertEquals(598, atBoundary.guiWidth());
        assertEquals(527, atBoundary.guiHeight());
    }

    @Test
    void mirrorsVanillaGuiScaleSelectionIncludingAutoAndUnicodeRules() {
        assertEquals(3, ElementAutoSizing.inferVanillaGuiScale(1920, 1080, 3, false));
        assertEquals(4, ElementAutoSizing.inferVanillaGuiScale(1920, 1080, 0, false));
        assertEquals(4, ElementAutoSizing.inferVanillaGuiScale(960, 720, 0, true));
        assertEquals(1, ElementAutoSizing.inferVanillaGuiScale(639, 479, 0, false));
        assertEquals(2, ElementAutoSizing.inferVanillaGuiScale(640, 480, 0, false));
    }

    @Test
    void serializesIntegralScalesCompatiblyAndRejectsInvalidMetadata() {
        assertEquals("4", ElementAutoSizing.serializeGuiScale(4.0D));
        assertEquals("2.5", ElementAutoSizing.serializeGuiScale(2.5D));
        assertEquals(Double.toString(0x1.0p63), ElementAutoSizing.serializeGuiScale(0x1.0p63));
        assertEquals("0", ElementAutoSizing.serializeGuiScale(0.0D));
        assertEquals("0", ElementAutoSizing.serializeGuiScale(Double.NaN));
        assertEquals("0", ElementAutoSizing.serializeGuiScale(Double.POSITIVE_INFINITY));
    }

}
