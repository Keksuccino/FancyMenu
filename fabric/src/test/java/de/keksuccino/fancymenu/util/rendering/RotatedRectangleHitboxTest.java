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

}
