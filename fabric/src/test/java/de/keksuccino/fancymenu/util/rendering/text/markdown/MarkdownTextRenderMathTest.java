package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MarkdownTextRenderMathTest {

    @Test
    void fractionalFragmentOriginSurvivesLocalScaling() {
        float scale = 0.75F;
        float textOrigin = MarkdownTextRenderMath.resolveOrigin(12.5F, 0.0F, scale);
        float truncatedAbsoluteCoordinate = (int)(12.5F / scale) * scale;

        assertEquals(12.5F, textOrigin);
        assertNotEquals(textOrigin, truncatedAbsoluteCoordinate);
        assertEquals(textOrigin, MarkdownTextRenderMath.toLocalCoordinate(textOrigin, scale) * scale);
    }

    @Test
    void unscaledQuoteBulletAndCodeOffsetsAreAppliedBeforeScaling() {
        float scale = 0.75F;
        float combinedOffset = 8.0F + 21.0F + 10.0F;
        float textOrigin = MarkdownTextRenderMath.resolveOrigin(12.5F, combinedOffset, scale);

        assertEquals(41.75F, textOrigin);
        assertEquals(textOrigin, MarkdownTextRenderMath.toLocalCoordinate(textOrigin, scale) * scale);
    }

    @Test
    void positiveFiniteScaleRemainsUnchanged() {
        assertEquals(0.75F, MarkdownTextRenderMath.sanitizeScale(0.75F));
        assertEquals(Float.MIN_VALUE, MarkdownTextRenderMath.sanitizeScale(Float.MIN_VALUE));
    }

    @ParameterizedTest
    @ValueSource(floats = {0.0F, -1.0F, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void invalidScaleIsDisabled(float scale) {
        assertEquals(0.0F, MarkdownTextRenderMath.sanitizeScale(scale));
        assertEquals(0.0F, MarkdownTextRenderMath.toLocalCoordinate(12.5F, MarkdownTextRenderMath.sanitizeScale(scale)));
    }

}
