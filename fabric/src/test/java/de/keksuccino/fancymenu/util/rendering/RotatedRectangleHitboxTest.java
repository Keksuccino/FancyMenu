package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotatedRectangleHitboxTest {

    @Test
    void untransformedBoundsUseWidgetEdgesAndRejectEmptyRectangles() {
        RotatedRectangleHitbox hitbox = new RotatedRectangleHitbox();
        assertTrue(hitbox.contains(10, 20, 10, 20, 100, 30));
        assertTrue(hitbox.contains(109.999, 49.999, 10, 20, 100, 30));
        assertFalse(hitbox.contains(110, 30, 10, 20, 100, 30));
        assertFalse(hitbox.contains(50, 50, 10, 20, 100, 30));
        assertFalse(hitbox.contains(9.999, 20, 10, 20, 100, 30));
        assertFalse(hitbox.contains(10, 20, 10, 20, 0, 30));
        assertFalse(hitbox.contains(10, 20, 10, 20, 100, -1));
    }

    @Test
    void rotationUsesTheRectangleCenterAndTracksMovedOrResizedBounds() {
        RotatedRectangleHitbox hitbox = new RotatedRectangleHitbox();
        hitbox.setRotation(90.0F, 0.0F, 0.0F);
        assertTrue(hitbox.contains(60, 70, 10, 20, 100, 20));
        assertFalse(hitbox.contains(15, 30, 10, 20, 100, 20));
        assertFalse(hitbox.contains(60, 70, 110, 20, 100, 20));
        assertTrue(hitbox.contains(160, 70, 110, 20, 100, 20));
        assertFalse(hitbox.contains(160, 70, 110, 20, 20, 20));
    }

    @Test
    void combinedTiltsAndRotationMatchForwardRenderingOrder() {
        RotatedRectangleHitbox hitbox = new RotatedRectangleHitbox();
        float rotation = 37.0F;
        float verticalTilt = -42.0F;
        float horizontalTilt = 53.0F;
        hitbox.setRotation(rotation, verticalTilt, horizontalTilt);

        // Independently project local points by applying Z, then Y, then X to the vector.
        // Column-vector rendering composes these operations as Rx * Ry * Rz.
        for (double localX : new double[]{-51, -49, 0, 49, 51}) {
            for (double localY : new double[]{-11, -9, 0, 9, 11}) {
                double zRotationX = localX * Math.cos(Math.toRadians(rotation)) - localY * Math.sin(Math.toRadians(rotation));
                double zRotationY = localX * Math.sin(Math.toRadians(rotation)) + localY * Math.cos(Math.toRadians(rotation));
                double yRotationX = zRotationX * Math.cos(Math.toRadians(horizontalTilt));
                double yRotationZ = -zRotationX * Math.sin(Math.toRadians(horizontalTilt));
                double xRotationY = zRotationY * Math.cos(Math.toRadians(verticalTilt)) - yRotationZ * Math.sin(Math.toRadians(verticalTilt));
                boolean expected = (Math.abs(localX) < 50) && (Math.abs(localY) < 10);
                assertEquals(expected, hitbox.contains(60 + yRotationX, 30 + xRotationY, 10, 20, 100, 20));
                assertEquals(60 + localX, hitbox.untransformX(60 + yRotationX, 30 + xRotationY, 10, 20, 100, 20), 1.0E-9);
            }
        }
    }

    @Test
    void changedAnglesAndResetDoNotReuseAnObsoleteInverse() {
        RotatedRectangleHitbox hitbox = new RotatedRectangleHitbox();
        hitbox.setRotation(90.0F, 0.0F, 0.0F);
        assertTrue(hitbox.contains(50, 40, 0, 0, 100, 20));
        hitbox.setRotation(0.0F, 0.0F, 60.0F);
        assertFalse(hitbox.contains(10, 10, 0, 0, 100, 20));
        hitbox.setRotation(0.0F, 0.0F, 60.0F);
        assertTrue(hitbox.contains(50, 10, 0, 0, 100, 20));
        assertEquals(60.0F, hitbox.getHorizontalTiltDegrees());
        hitbox.setRotation(0.0F, 0.0F, 0.0F);
        assertFalse(hitbox.isTransformed());
        assertTrue(hitbox.contains(10, 10, 0, 0, 100, 20));
        assertFalse(hitbox.contains(50, 40, 0, 0, 100, 20));
    }

    @Test
    void singularAndInvalidTransformsRetainTheUntransformedFallback() {
        RotatedRectangleHitbox hitbox = new RotatedRectangleHitbox();
        hitbox.setRotation(45.0F, 90.0F, 0.0F);
        assertFalse(hitbox.isTransformed());
        assertTrue(hitbox.contains(10, 10, 0, 0, 100, 20));
        hitbox.setRotation(Float.NaN, 0.0F, 0.0F);
        assertFalse(hitbox.isTransformed());
        assertFalse(hitbox.contains(Double.NaN, 10, 0, 0, 100, 20));
        hitbox.setRotation(45.0F, 20.0F, 30.0F);
        assertTrue(hitbox.isTransformed());
        assertEquals(45.0F, hitbox.getRotationDegrees());
        assertEquals(20.0F, hitbox.getVerticalTiltDegrees());
    }

    @Test
    void pointerCoordinatesPassThroughWithoutRotation() {
        RotatedRectangleHitbox hitbox = new RotatedRectangleHitbox();
        for (double mouseX : new double[]{-100.5, 10, 60.25, 110, 250.75}) {
            assertEquals(mouseX, hitbox.untransformX(mouseX, 70, 10, 20, 100, 20));
        }
    }

    @Test
    void pointerCoordinatesFollowAnyRotationWithoutLosingSubpixelPrecision() {
        RotatedRectangleHitbox hitbox = new RotatedRectangleHitbox();
        for (float angle : new float[]{90, -90, 180, 270, 37, -143, 360}) {
            hitbox.setRotation(angle, 0.0F, 0.0F);
            double radians = Math.toRadians(angle);
            for (double localX : new double[]{-46, -23.25, 0, 19.75, 46}) {
                for (double localY : new double[]{-7.5, 0, 8.25}) {
                    double mouseX = 60 + localX * Math.cos(radians) - localY * Math.sin(radians);
                    double mouseY = 30 + localX * Math.sin(radians) + localY * Math.cos(radians);
                    assertEquals(60 + localX, hitbox.untransformX(mouseX, mouseY, 10, 20, 100, 20), 1.0E-9);
                }
            }
        }
    }

    @Test
    void capturedDragsKeepCoordinatesOutsideTheHitboxForVanillaClamping() {
        RotatedRectangleHitbox hitbox = new RotatedRectangleHitbox();
        hitbox.setRotation(90.0F, 0.0F, 0.0F);
        assertFalse(hitbox.contains(60, -100, 10, 20, 100, 20));
        assertEquals(-70, hitbox.untransformX(60, -100, 10, 20, 100, 20), 1.0E-9);
        assertFalse(hitbox.contains(60, 160, 10, 20, 100, 20));
        assertEquals(190, hitbox.untransformX(60, 160, 10, 20, 100, 20), 1.0E-9);
    }

    @Test
    void pointerConversionUsesCurrentBoundsWhenTheWidgetMovesOrResizes() {
        RotatedRectangleHitbox hitbox = new RotatedRectangleHitbox();
        hitbox.setRotation(90.0F, 0.0F, 0.0F);
        assertEquals(100, hitbox.untransformX(60, 70, 10, 20, 100, 20), 1.0E-9);
        assertEquals(200, hitbox.untransformX(160, 70, 110, 20, 100, 20), 1.0E-9);
        assertEquals(150, hitbox.untransformX(130, 70, 110, 20, 40, 60), 1.0E-9);
        assertEquals(95.5, hitbox.untransformX(60.5, 70.5, 10, 25, 101, 21), 1.0E-9);
    }

    @Test
    void resetAndInvalidTransformsDoNotReusePointerConversionFromAPreviousRotation() {
        RotatedRectangleHitbox hitbox = new RotatedRectangleHitbox();
        for (float[] angles : new float[][]{{0, 0, 0}, {45, 90, 0}, {45, 0, 90}, {Float.NaN, 0, 0}, {Float.POSITIVE_INFINITY, 0, 0}}) {
            hitbox.setRotation(90.0F, 0.0F, 0.0F);
            assertEquals(100, hitbox.untransformX(60, 70, 10, 20, 100, 20), 1.0E-9);
            hitbox.setRotation(angles[0], angles[1], angles[2]);
            assertFalse(hitbox.isTransformed());
            assertEquals(60, hitbox.untransformX(60, 70, 10, 20, 100, 20));
        }
    }

}
