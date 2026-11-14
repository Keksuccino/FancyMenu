package de.keksuccino.fancymenu.util.rendering;

import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiPoseTransformUtilTest {

    private static final float[][] ANGLES = {{37.0F, 0.0F, 0.0F}, {0.0F, 60.0F, 0.0F}, {0.0F, 0.0F, -60.0F}, {0.0F, 35.0F, 25.0F}, {37.0F, -42.0F, 53.0F}, {-120.0F, 25.0F, 0.0F}, {315.0F, -60.0F, 30.0F}};

    @Test
    void scaledAndRotatedPosesKeepTheirScaleAndRotation() {
        Matrix3x2f pose = new Matrix3x2f().translate(12.0F, 34.0F).scale(2.5F).rotate((float)Math.toRadians(30.0D));
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(pose);
        float cos = (float)Math.cos(Math.toRadians(30.0D));
        float sin = (float)Math.sin(Math.toRadians(30.0D));
        assertEquals(2.5F, transform.scale(), 1.0E-5F);
        assertEquals(12.0F, transform.translationX());
        assertEquals(34.0F, transform.translationY());
        assertRotation(cos, -sin, sin, cos, transform.rotation());

        transform = GuiPoseTransformUtil.resolve(new Matrix3x2f().scale(3.0F));
        assertEquals(3.0F, transform.scale());
        assertRotation(1.0F, 0.0F, 0.0F, 1.0F, transform.rotation());
    }

    @Test
    void tiltDoesNotShrinkTheScale() {
        for (float[] angle : ANGLES) {
            Matrix3x2f pose = new Matrix3x2f().scale(1.5F);
            RenderRotationUtil.rotateAndTilt(pose, 60.0F, 30.0F, angle[0], angle[1], angle[2]);
            assertEquals(1.5F, GuiPoseTransformUtil.resolve(pose).scale(), 1.0E-4F);
        }
    }

    @Test
    void theRotationMapsScreenOffsetsBackIntoRotatedAndTiltedShapes() {
        float scale = 1.5F;
        for (float[] angle : ANGLES) {
            Matrix3x2f pose = new Matrix3x2f().translate(5.0F, 7.0F).scale(scale);
            RenderRotationUtil.rotateAndTilt(pose, 60.0F, 30.0F, angle[0], angle[1], angle[2]);
            RenderRotationUtil.Rotation2D rotation = GuiPoseTransformUtil.resolve(pose).rotation();
            Vector2f center = pose.transformPosition(60.0F, 30.0F, new Vector2f());
            for (float localX : new float[]{-50.0F, -12.5F, 0.0F, 50.0F}) {
                for (float localY : new float[]{-10.0F, 0.0F, 10.0F}) {
                    Vector2f screen = pose.transformPosition(60.0F + localX, 30.0F + localY, new Vector2f());
                    // The shape shaders work with the Y axis pointing up, and with a shape that has its sizes multiplied by the scale already
                    float offsetX = screen.x - center.x;
                    float offsetY = center.y - screen.y;
                    assertEquals(localX * scale, (rotation.m00() * offsetX) + (rotation.m01() * offsetY), 1.0E-3F);
                    assertEquals(-localY * scale, (rotation.m10() * offsetX) + (rotation.m11() * offsetY), 1.0E-3F);
                }
            }
        }
    }

    @Test
    void transformedAreasStayCenteredOnThePoseAndTakeItsScale() {
        Matrix3x2f pose = new Matrix3x2f().translate(5.0F, 7.0F).scale(2.0F);
        RenderRotationUtil.rotateAndTilt(pose, 60.0F, 30.0F, 20.0F, 30.0F, -40.0F);
        GuiPoseTransformUtil.TransformedRect area = GuiPoseTransformUtil.resolve(pose).transformRect(10.0F, 20.0F, 100.0F, 20.0F);
        Vector2f center = pose.transformPosition(60.0F, 30.0F, new Vector2f());
        assertEquals(200.0F, area.width(), 1.0E-3F);
        assertEquals(40.0F, area.height(), 1.0E-3F);
        assertEquals(center.x, area.x() + (area.width() * 0.5F), 1.0E-3F);
        assertEquals(center.y, area.y() + (area.height() * 0.5F), 1.0E-3F);
    }

    @Test
    void collapsedPosesFallBackToUnrotatedShapes() {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(new Matrix3x2f().scale(0.0F));
        assertEquals(1.0F, transform.scale());
        assertRotation(1.0F, 0.0F, 0.0F, 1.0F, transform.rotation());

        transform = GuiPoseTransformUtil.resolve(new Matrix3x2f().scale(3.0F, 0.0F));
        assertEquals(3.0F, transform.scale(), 1.0E-6F);
        assertRotation(1.0F, 0.0F, 0.0F, 1.0F, transform.rotation());
    }

    private static void assertRotation(float m00, float m01, float m10, float m11, RenderRotationUtil.Rotation2D rotation) {
        assertEquals(m00, rotation.m00(), 1.0E-5F);
        assertEquals(m01, rotation.m01(), 1.0E-5F);
        assertEquals(m10, rotation.m10(), 1.0E-5F);
        assertEquals(m11, rotation.m11(), 1.0E-5F);
    }

}
