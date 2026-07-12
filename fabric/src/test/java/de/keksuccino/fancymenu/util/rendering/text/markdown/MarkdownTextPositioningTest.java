package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTextPositioningTest {

    private static final float EPSILON = 1.0E-5F;

    @ParameterizedTest
    @MethodSource("fractionalOrigins")
    void fractionalOriginsRemainExactAtSubUnitScale(float origin, float scale) {
        assertEquals(origin, MarkdownTextPositioning.resolveTextOrigin(origin, 0.0F, scale));
    }

    @ParameterizedTest
    @MethodSource("offsetOrigins")
    void localOffsetsAreScaledFromTheExactFragmentOrigin(float origin, float offset, float scale, float expected) {
        assertEquals(expected, MarkdownTextPositioning.resolveTextOrigin(origin, offset, scale), EPSILON);
    }

    @ParameterizedTest
    @MethodSource("legacyDriftCases")
    void legacyAbsoluteCoordinateDivisionDriftsFromTheExactOrigin(float origin, float offset, float scale) {
        float exactOrigin = MarkdownTextPositioning.resolveTextOrigin(origin, offset, scale);
        float legacyOrigin = legacyTextOrigin(origin, offset, scale);

        assertNotEquals(exactOrigin, legacyOrigin, EPSILON);
        assertTrue(Math.abs(exactOrigin - legacyOrigin) < scale);
    }

    @ParameterizedTest
    @MethodSource("localCoordinateCases")
    void exactAbsoluteOriginsConvertBackToLocalCoordinates(float origin, float offset, float scale) {
        float absoluteOrigin = MarkdownTextPositioning.resolveTextOrigin(origin, offset, scale);

        assertEquals((origin / scale) + offset, MarkdownTextPositioning.toLocalCoordinate(absoluteOrigin, scale), EPSILON);
    }

    @ParameterizedTest
    @MethodSource("invalidScales")
    void invalidScalesDisableDrawingWithoutCorruptingAbsoluteOrigins(float scale) {
        assertEquals(0.0F, MarkdownTextPositioning.sanitizeScale(scale));
        assertEquals(37.25F, MarkdownTextPositioning.resolveTextOrigin(37.25F, 12.0F, scale));
        assertEquals(0.0F, MarkdownTextPositioning.toLocalCoordinate(37.25F, scale));
    }

    @ParameterizedTest
    @MethodSource("validScales")
    void finitePositiveScalesRemainUnchanged(float scale) {
        assertEquals(scale, MarkdownTextPositioning.sanitizeScale(scale));
    }

    @Test
    void overflowingScaleProductsAreRejected() {
        assertEquals(0.0F, MarkdownTextPositioning.sanitizeScale(Float.MAX_VALUE * 2.0F));
    }

    private static float legacyTextOrigin(float origin, float offset, float scale) {
        return ((int)((origin / scale) + offset)) * scale;
    }

    private static Stream<Arguments> fractionalOrigins() {
        return Stream.of(Arguments.of(101.25F, 0.67F), Arguments.of(101.75F, 0.5F), Arguments.of(-43.375F, 0.87F), Arguments.of(-0.125F, 0.67F));
    }

    private static Stream<Arguments> offsetOrigins() {
        return Stream.of(Arguments.of(101.25F, 8.0F, 0.67F, 106.61F), Arguments.of(-43.375F, 21.0F, 0.5F, -32.875F), Arguments.of(7.125F, 10.0F, 1.6F, 23.125F), Arguments.of(-2.75F, 1.0F, 2.0F, -0.75F));
    }

    private static Stream<Arguments> legacyDriftCases() {
        return Stream.of(Arguments.of(101.25F, 0.0F, 0.67F), Arguments.of(101.75F, 13.0F, 0.67F), Arguments.of(-43.375F, 0.0F, 0.87F), Arguments.of(-43.125F, 11.0F, 0.5F));
    }

    private static Stream<Arguments> localCoordinateCases() {
        return Stream.of(Arguments.of(101.25F, 0.0F, 0.67F), Arguments.of(101.25F, 13.0F, 0.67F), Arguments.of(-43.375F, 10.0F, 0.87F), Arguments.of(-4.5F, 21.0F, 1.6F));
    }

    private static Stream<Float> invalidScales() {
        return Stream.of(0.0F, -0.5F, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    private static Stream<Float> validScales() {
        return Stream.of(Float.MIN_VALUE, 0.5F, 0.67F, 0.87F, 1.0F, 1.6F, Float.MAX_VALUE);
    }

}
