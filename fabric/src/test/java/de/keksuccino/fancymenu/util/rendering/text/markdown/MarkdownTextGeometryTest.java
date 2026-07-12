package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTextGeometryTest {

    @Test
    void preservesFractionalFragmentOrigin() {
        assertEquals(13.75F, MarkdownTextGeometry.screenOrigin(13.75F, 0.0F, 1.6F));
        assertEquals(8.59375F, MarkdownTextGeometry.localCoordinate(13.75F, 1.6F));
    }

    @Test
    void scaledHeadingAppliesLocalOffsetExactlyOnce() {
        float scale = MarkdownTextGeometry.sanitizeScale(2.0F * 0.75F);

        assertEquals(1.5F, scale);
        assertEquals(24.25F, MarkdownTextGeometry.screenOrigin(12.25F, 8.0F, scale));
    }

    @Test
    void combinesQuoteBulletAndCodeOffsetsInLocalTextSpace() {
        assertEquals(39.0F, MarkdownTextGeometry.horizontalOffset(true, 8.0F, true, 2, 8.0F, 5.0F, true, false));
        assertEquals(1.0F, MarkdownTextGeometry.horizontalOffset(false, 8.0F, false, 0, 8.0F, 5.0F, false, true));
        assertEquals(13.0F, MarkdownTextGeometry.verticalOffset(true, true, 3.0F));
    }

    @Test
    void rejectsZeroNegativeAndNonFiniteScales() {
        assertEquals(0.0F, MarkdownTextGeometry.sanitizeScale(0.0F));
        assertEquals(0.0F, MarkdownTextGeometry.sanitizeScale(-1.0F));
        assertEquals(0.0F, MarkdownTextGeometry.sanitizeScale(Float.NaN));
        assertEquals(0.0F, MarkdownTextGeometry.sanitizeScale(Float.POSITIVE_INFINITY));
        assertEquals(0.0F, MarkdownTextGeometry.sanitizeScale(Float.NEGATIVE_INFINITY));
        assertEquals(0.0F, MarkdownTextGeometry.localCoordinate(10.0F, 0.0F));
    }

    @Test
    void renderOriginAndHitTestBoundsUseSameCoordinates() {
        float scale = 1.6F;
        float x = MarkdownTextGeometry.screenOrigin(5.75F, 39.0F, scale);
        float y = MarkdownTextGeometry.screenOrigin(9.25F, 13.0F, scale);
        float width = 10.0F * scale;
        float height = 9.0F * scale;

        assertTrue(MarkdownTextGeometry.contains(x, y, x, y, width, height));
        assertTrue(MarkdownTextGeometry.contains(x + width - 0.001F, y + height - 0.001F, x, y, width, height));
        assertFalse(MarkdownTextGeometry.contains(x - 0.001F, y, x, y, width, height));
        assertFalse(MarkdownTextGeometry.contains(x + width, y, x, y, width, height));
        assertFalse(MarkdownTextGeometry.contains(x, y + height, x, y, width, height));
    }

}
