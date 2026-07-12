package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownTextPositioningTest {

    private static final float EPSILON = 0.00001F;

    @ParameterizedTest
    @ValueSource(floats = {0.5F, 0.75F, 1.0F, 1.2F, 1.6F, 2.0F})
    void zeroContextOffsetKeepsPositiveAndNegativeBoundariesInvariant(float scale) {
        assertEquals(17.375F, MarkdownTextPositioning.calculateOrigin(17.375F, 0.0F, scale));
        assertEquals(-4.625F, MarkdownTextPositioning.calculateOrigin(-4.625F, 0.0F, scale));
    }

    @ParameterizedTest
    @ValueSource(floats = {0.5F, 0.75F, 1.0F, 1.2F, 1.6F, 2.0F})
    void contextualOffsetsAreAppliedExactlyOnceAtEverySupportedScale(float scale) {
        float origin = MarkdownTextPositioning.calculateOrigin(10.25F, 3.5F, scale);
        assertEquals(10.25F + 3.5F * scale, origin, EPSILON);
        assertEquals(origin / scale, MarkdownTextPositioning.calculateRenderCoordinate(origin, scale), EPSILON);
    }

    @Test
    void fractionalOffsetsProduceExactPositiveAndNegativeOrigins() {
        assertEquals(13.125F, MarkdownTextPositioning.calculateOrigin(10.5F, 3.5F, 0.75F), EPSILON);
        assertEquals(-9.375F, MarkdownTextPositioning.calculateOrigin(-6.75F, -3.5F, 0.75F), EPSILON);
    }

    @ParameterizedTest
    @ValueSource(floats = {0.0F, -0.5F, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void invalidScalesNormalizeAndRenderAsZero(float scale) {
        assertEquals(0.0F, MarkdownTextPositioning.normalizeScale(scale));
        assertEquals(7.25F, MarkdownTextPositioning.calculateOrigin(7.25F, 4.0F, scale));
        assertEquals(0.0F, MarkdownTextPositioning.calculateRenderCoordinate(7.25F, scale));
    }

    @Test
    void localTranslationAndScaleComposeWithAnOuterMatrix() {
        float scale = 0.75F;
        float originX = MarkdownTextPositioning.calculateOrigin(11.25F, 2.5F, scale);
        float originY = MarkdownTextPositioning.calculateOrigin(-3.5F, -1.25F, scale);
        float localX = 4.0F;
        float localY = 6.0F;
        Matrix3x2f transform = new Matrix3x2f().translation(23.0F, -7.0F).scale(1.5F).translate(originX, originY).scale(scale);
        Vector2f actual = transform.transformPosition(localX, localY, new Vector2f());

        assertEquals(23.0F + 1.5F * (originX + scale * localX), actual.x, EPSILON);
        assertEquals(-7.0F + 1.5F * (originY + scale * localY), actual.y, EPSILON);
    }

}
