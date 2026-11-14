package de.keksuccino.fancymenu.util.rendering;

import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderRotationUtilTest {

    // The element spans (10, 20) to (110, 40), so it rotates and tilts around (60, 30)

    @Test
    void hitboxesInvertTheRenderedRotationAndTilt() {
        float[][] angles = {{37.0F, 0.0F, 0.0F}, {0.0F, 60.0F, 0.0F}, {0.0F, 0.0F, -60.0F}, {0.0F, 35.0F, 25.0F}, {37.0F, -42.0F, 53.0F}, {-120.0F, 25.0F, 0.0F}, {200.0F, 0.0F, 45.0F}, {90.0F, -60.0F, 60.0F}};
        for (float[] angle : angles) {
            Matrix3x2f pose = new Matrix3x2f();
            RenderRotationUtil.rotateAndTilt(pose, 60.0F, 30.0F, angle[0], angle[1], angle[2]);
            RotatedRectangleHitbox hitbox = new RotatedRectangleHitbox();
            hitbox.setRotation(angle[0], angle[1], angle[2]);
            for (float localX : new float[]{10.0F, 35.5F, 60.0F, 110.0F}) {
                for (float localY : new float[]{20.0F, 30.0F, 40.0F}) {
                    Vector2f screen = pose.transformPosition(localX, localY, new Vector2f());
                    assertEquals(localX, hitbox.untransformX(screen.x, screen.y, 10, 20, 100, 20), 1.0E-3);
                    assertEquals(localY, hitbox.untransformY(screen.x, screen.y, 10, 20, 100, 20), 1.0E-3);
                }
            }
        }
    }

    @Test
    void tiltCompressesTheContentTowardsTheCenter() {
        // A vertical tilt by 60 degrees shows the content with half of its height
        Matrix3x2f pose = new Matrix3x2f();
        RenderRotationUtil.rotateAndTilt(pose, 60.0F, 30.0F, 0.0F, 60.0F, 0.0F);
        assertPoint(10.0F, 25.0F, pose, 10.0F, 20.0F);
        assertPoint(110.0F, 35.0F, pose, 110.0F, 40.0F);
        assertPoint(60.0F, 30.0F, pose, 60.0F, 30.0F);

        // A horizontal tilt by 60 degrees shows the content with half of its width
        pose = new Matrix3x2f();
        RenderRotationUtil.rotateAndTilt(pose, 60.0F, 30.0F, 0.0F, 0.0F, 60.0F);
        assertPoint(35.0F, 20.0F, pose, 10.0F, 20.0F);
        assertPoint(85.0F, 40.0F, pose, 110.0F, 40.0F);
        assertPoint(60.0F, 30.0F, pose, 60.0F, 30.0F);
    }

    @Test
    void rotationWithoutTiltTurnsAroundTheCenter() {
        Matrix3x2f pose = new Matrix3x2f();
        RenderRotationUtil.rotateAndTilt(pose, 60.0F, 30.0F, 90.0F, 0.0F, 0.0F);
        assertPoint(60.0F, 80.0F, pose, 110.0F, 30.0F);
        assertPoint(70.0F, -20.0F, pose, 10.0F, 20.0F);
        assertPoint(60.0F, 30.0F, pose, 60.0F, 30.0F);
    }

    @Test
    void unrotatedAndUntiltedContentKeepsThePose() {
        Matrix3x2f pose = new Matrix3x2f().translate(5.0F, 7.0F).scale(2.0F);
        RenderRotationUtil.rotateAndTilt(pose, 60.0F, 30.0F, 0.0F, 0.0F, 0.0F);
        assertPoint(25.0F, 47.0F, pose, 10.0F, 20.0F);
        assertPoint(225.0F, 87.0F, pose, 110.0F, 40.0F);
    }

    @Test
    void rotationAndTiltApplyInsideTheExistingPose() {
        Matrix3x2f pose = new Matrix3x2f().translate(5.0F, 7.0F).scale(2.0F);
        RenderRotationUtil.rotateAndTilt(pose, 60.0F, 30.0F, 0.0F, 60.0F, 0.0F);
        // Tilted to (10, 25) and (110, 35) first, then scaled and moved by the pose
        assertPoint(25.0F, 57.0F, pose, 10.0F, 20.0F);
        assertPoint(225.0F, 77.0F, pose, 110.0F, 40.0F);
    }

    private static void assertPoint(float expectedX, float expectedY, Matrix3x2f pose, float localX, float localY) {
        Vector2f screen = pose.transformPosition(localX, localY, new Vector2f());
        assertEquals(expectedX, screen.x, 1.0E-3F);
        assertEquals(expectedY, screen.y, 1.0E-3F);
    }

}
