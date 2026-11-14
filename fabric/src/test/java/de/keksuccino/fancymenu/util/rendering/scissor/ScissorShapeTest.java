package de.keksuccino.fancymenu.util.rendering.scissor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScissorShapeTest {

    @Test
    void axisAlignedTransformsNeedNoShape() {
        assertNull(ScissorShape.ofTransformedRectangle(1, 0, 0, 1, 0, 0, 0, 0, 10, 10));
        assertNull(ScissorShape.ofTransformedRectangle(2.5F, 0, 0, 0.5F, 30, -4, 0, 0, 10, 10));
        assertNull(ScissorShape.ofTransformedRectangle(-1, 0, 0, 1, 0, 0, 0, 0, 10, 10));
    }

    @Test
    void rotatedRectangleCoversTheRotatedArea() {
        // 90 degrees around (50, 50): x' = -y + 100, y' = x
        ScissorShape shape = ScissorShape.ofTransformedRectangle(0, 1, -1, 0, 100, 0, 20, 40, 80, 60);
        assertNotNull(shape);
        assertEquals(4, shape.getCornerCount());
        // The 60x20 rectangle becomes a 20x60 rectangle with the same center
        assertTrue(shape.contains(50, 50));
        assertTrue(shape.contains(41, 21));
        assertTrue(shape.contains(59, 79));
        assertFalse(shape.contains(30, 50));
        assertFalse(shape.contains(70, 50));
        assertFalse(shape.contains(50, 15));
        assertFalse(shape.contains(50, 85));
    }

    @Test
    void containsMatchesTheRotatedLocalRectangleForAnyAngle() {
        for (float degrees : new float[]{7, 33, 45, 128, 211, -64}) {
            double radians = Math.toRadians(degrees);
            float cos = (float)Math.cos(radians);
            float sin = (float)Math.sin(radians);
            // Rotate around (100, 80), like elements rotate around their center
            float m20 = 100 - cos * 100 + sin * 80;
            float m21 = 80 - sin * 100 - cos * 80;
            ScissorShape shape = ScissorShape.ofTransformedRectangle(cos, sin, -sin, cos, m20, m21, 60, 60, 140, 100);
            assertNotNull(shape);
            for (double localX = 50; localX <= 150; localX += 2.5) {
                for (double localY = 50; localY <= 110; localY += 2.5) {
                    double screenX = cos * localX - sin * localY + m20;
                    double screenY = sin * localX + cos * localY + m21;
                    boolean clearlyInside = (localX > 60.01) && (localX < 139.99) && (localY > 60.01) && (localY < 99.99);
                    boolean clearlyOutside = (localX < 59.9) || (localX > 140.1) || (localY < 59.9) || (localY > 100.1);
                    if (clearlyInside) assertTrue(shape.contains(screenX, screenY), "inside at " + degrees + "°: " + localX + ", " + localY);
                    if (clearlyOutside) assertFalse(shape.contains(screenX, screenY), "outside at " + degrees + "°: " + localX + ", " + localY);
                }
            }
        }
    }

    @Test
    void windingDoesNotChangeTheInside() {
        ScissorShape clockwise = ScissorShape.ofConvexPolygon(new float[]{0, 10, 10, 0}, new float[]{0, 0, 10, 10});
        ScissorShape counterClockwise = ScissorShape.ofConvexPolygon(new float[]{0, 0, 10, 10}, new float[]{0, 10, 10, 0});
        for (double x = -2; x <= 12; x += 0.5) {
            for (double y = -2; y <= 12; y += 0.5) {
                assertEquals(clockwise.contains(x, y), counterClockwise.contains(x, y));
            }
        }
        assertTrue(clockwise.contains(5, 5));
        assertFalse(clockwise.contains(11, 5));
    }

    @Test
    void intersectionShowsOnlyWhatBothShapesShow() {
        ScissorShape square = ScissorShape.ofRectangle(0, 0, 10, 10);
        ScissorShape diamond = ScissorShape.ofConvexPolygon(new float[]{10, 15, 10, 5}, new float[]{0, 5, 10, 5});
        ScissorShape both = ScissorShape.intersect(square, diamond);
        assertNotNull(both);
        for (double x = -1; x <= 16; x += 0.25) {
            for (double y = -1; y <= 11; y += 0.25) {
                boolean inBoth = square.contains(x, y) && diamond.contains(x, y);
                boolean nearEdge = !square.contains(x - 0.01, y) || !square.contains(x + 0.01, y) || !diamond.contains(x - 0.01, y) || !diamond.contains(x + 0.01, y)
                        || !square.contains(x, y - 0.01) || !square.contains(x, y + 0.01) || !diamond.contains(x, y - 0.01) || !diamond.contains(x, y + 0.01);
                if (!nearEdge) assertEquals(inBoth, both.contains(x, y), x + ", " + y);
            }
        }
    }

    @Test
    void intersectionTreatsNullAsUnlimitedAndDisjointShapesAsEmpty() {
        ScissorShape square = ScissorShape.ofRectangle(0, 0, 10, 10);
        assertSame(square, ScissorShape.intersect(square, null));
        assertSame(square, ScissorShape.intersect(null, square));
        assertNull(ScissorShape.intersect(null, null));
        ScissorShape disjoint = ScissorShape.intersect(square, ScissorShape.ofRectangle(20, 20, 30, 30));
        assertNotNull(disjoint);
        assertTrue(disjoint.isEmpty());
        assertFalse(disjoint.contains(5, 5));
        assertFalse(disjoint.contains(25, 25));
    }

    @Test
    void degenerateInputMakesEmptyShapes() {
        assertTrue(ScissorShape.ofRectangle(5, 5, 5, 10).isEmpty());
        assertTrue(ScissorShape.ofConvexPolygon(new float[]{0, 1}, new float[]{0, 1}).isEmpty());
        assertFalse(ScissorShape.ofRectangle(5, 5, 6, 6).isEmpty());
    }

}
