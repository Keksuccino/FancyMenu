package de.keksuccino.fancymenu.customization.element;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementAutoSizingTest {

    @ParameterizedTest
    @ValueSource(doubles = {0.5D, 1.0D, 1.25D, 1.5D, 4.0D})
    void validGuiScalesRemainExact(double guiScale) {
        assertEquals(guiScale, ElementAutoSizing.normalizeGuiScale(guiScale));
    }

    @ParameterizedTest
    @MethodSource("invalidGuiScales")
    void invalidGuiScalesFallBackToOne(double guiScale) {
        assertEquals(1.0D, ElementAutoSizing.normalizeGuiScale(guiScale));
    }

    @ParameterizedTest
    @MethodSource("guiDimensions")
    void pixelDimensionsConvertToLogicalGuiCoordinates(int pixelDimension, double guiScale, int expected) {
        assertEquals(expected, ElementAutoSizing.calculateGuiDimension(pixelDimension, guiScale));
    }

    @ParameterizedTest
    @MethodSource("invalidGuiDimensions")
    void invalidDimensionInputsReturnZero(int pixelDimension, double guiScale) {
        assertEquals(0, ElementAutoSizing.calculateGuiDimension(pixelDimension, guiScale));
    }

    @Test
    void guiScaleChangeWithStablePixelResolutionStillResizesElement() {
        int baseWidth = ElementAutoSizing.calculateGuiDimension(1920, 2.0D);
        int baseHeight = ElementAutoSizing.calculateGuiDimension(1080, 2.0D);

        double factor = ElementAutoSizing.calculateScaleFactor(1920, 1080, baseWidth, baseHeight);

        assertEquals(2.0D, factor);
        assertEquals(400, ElementAutoSizing.calculateElementDimension(200, factor));
        assertEquals(200, ElementAutoSizing.calculateElementDimension(100, factor));
    }

    @ParameterizedTest
    @MethodSource("scaleFactors")
    void scaleFactorUsesSmallerAxisAndOnePercentMinimum(int currentWidth, int currentHeight, int baseWidth, int baseHeight, double expected) {
        assertEquals(expected, ElementAutoSizing.calculateScaleFactor(currentWidth, currentHeight, baseWidth, baseHeight));
    }

    @ParameterizedTest
    @MethodSource("invalidScaleFactorDimensions")
    void invalidScaleFactorDimensionsPreserveBaseSize(int currentWidth, int currentHeight, int baseWidth, int baseHeight) {
        assertEquals(1.0D, ElementAutoSizing.calculateScaleFactor(currentWidth, currentHeight, baseWidth, baseHeight));
    }

    @ParameterizedTest
    @MethodSource("elementDimensions")
    void elementDimensionsUseTruncationAndRemainAtLeastOne(int baseDimension, double scaleFactor, int expected) {
        assertEquals(expected, ElementAutoSizing.calculateElementDimension(baseDimension, scaleFactor));
    }

    @ParameterizedTest
    @MethodSource("vanillaGuiScales")
    void vanillaGuiScaleInferenceMatchesWindowCalculation(int width, int height, int maxScale, boolean enforceUnicode, int expected) {
        assertEquals(expected, ElementAutoSizing.inferVanillaGuiScale(width, height, maxScale, enforceUnicode));
    }

    private static Stream<Double> invalidGuiScales() {
        return Stream.of(0.0D, -1.0D, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    private static Stream<Arguments> guiDimensions() {
        return Stream.of(
                Arguments.of(1000, 1.25D, 800),
                Arguments.of(1001, 1.25D, 801),
                Arguments.of(1200, 1.5D, 800),
                Arguments.of(1201, 1.5D, 801),
                Arguments.of(1, 1.5D, 1),
                Arguments.of(1920, 2.0D, 960)
        );
    }

    private static Stream<Arguments> invalidGuiDimensions() {
        return Stream.of(
                Arguments.of(0, 1.0D),
                Arguments.of(-1, 1.0D),
                Arguments.of(1000, 0.0D),
                Arguments.of(1000, -1.0D),
                Arguments.of(1000, Double.NaN),
                Arguments.of(1000, Double.POSITIVE_INFINITY),
                Arguments.of(1000, Double.NEGATIVE_INFINITY)
        );
    }

    private static Stream<Arguments> scaleFactors() {
        return Stream.of(
                Arguments.of(1920, 1080, 960, 540, 2.0D),
                Arguments.of(1200, 600, 800, 600, 1.0D),
                Arguments.of(400, 300, 800, 600, 0.5D),
                Arguments.of(1, 1, 1000, 1000, 0.01D),
                Arguments.of(1000, 500, 1000, 1000, 0.5D)
        );
    }

    private static Stream<Arguments> invalidScaleFactorDimensions() {
        return Stream.of(
                Arguments.of(0, 100, 100, 100),
                Arguments.of(100, 0, 100, 100),
                Arguments.of(100, 100, 0, 100),
                Arguments.of(100, 100, 100, 0),
                Arguments.of(-1, 100, 100, 100),
                Arguments.of(100, -1, 100, 100),
                Arguments.of(100, 100, -1, 100),
                Arguments.of(100, 100, 100, -1)
        );
    }

    private static Stream<Arguments> elementDimensions() {
        return Stream.of(
                Arguments.of(200, 2.0D, 400),
                Arguments.of(101, 0.5D, 50),
                Arguments.of(100, 0.01D, 1),
                Arguments.of(1, 0.01D, 1),
                Arguments.of(0, 1.0D, 1),
                Arguments.of(-100, 1.0D, 1)
        );
    }

    private static Stream<Arguments> vanillaGuiScales() {
        return Stream.of(
                Arguments.of(1920, 1080, 0, false, 4),
                Arguments.of(1920, 1080, 3, false, 3),
                Arguments.of(1920, 1080, 1, false, 1),
                Arguments.of(640, 480, 0, false, 2),
                Arguments.of(960, 720, 3, false, 3),
                Arguments.of(960, 720, 3, true, 4),
                Arguments.of(319, 239, 0, false, 1),
                Arguments.of(319, 239, 0, true, 2)
        );
    }

}
