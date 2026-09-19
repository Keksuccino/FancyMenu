package de.keksuccino.fancymenu.util.rendering;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingGuiPoseStackTest {

    private static final float EPSILON = 0.00001F;

    @BeforeEach
    @AfterEach
    void resetActiveTransforms() {
        RenderScaleUtil.resetActiveRenderScale_FancyMenu();
        RenderTranslationUtil.resetActiveRenderTranslation_FancyMenu();
        RenderRotationUtil.resetActiveRenderRotation_FancyMenu();
    }

    @Test
    void tracksTransformsWithoutAnyMixinAppliedToJoml() {
        Matrix3x2fStack pose = new TrackingGuiPoseStack(4);
        pose.translate(10, 20);
        pose.scale(2);
        pose.translate(3, 4);
        pose.rotate((float)Math.PI / 2);
        assertState(2, 16, 28, (float)Math.PI / 2);
        Matrix3x2f expected = new Matrix3x2f().translate(10, 20).scale(2).translate(3, 4).rotate((float)Math.PI / 2);
        assertTrue(expected.equals(pose, EPSILON));
    }

    @Test
    void nestedPushAndPopRestoreEachParentTransformAndReuseFrames() {
        TrackingGuiPoseStack pose = new TrackingGuiPoseStack(3);
        pose.translate(5, 7);
        pose.scale(2);
        pose.rotate(0.25F);
        for (int i = 0; i < 3; i++) {
            assertSame(pose, pose.pushMatrix());
            pose.scale(3);
            pose.translate(4, 5);
            pose.rotate(0.5F);
            pose.pushMatrix();
            pose.scale(4);
            pose.translate(1, 1);
            pose.rotate(-0.25F);
            assertState(24, 53, 61, 0.5F);
            assertSame(pose, pose.popMatrix());
            assertState(6, 29, 37, 0.75F);
            pose.popMatrix();
            assertState(2, 5, 7, 0.25F);
        }
    }

    @Test
    void clearResetsMatrixTrackingAndStackDepth() {
        TrackingGuiPoseStack pose = new TrackingGuiPoseStack(2);
        pose.scale(3);
        pose.pushMatrix();
        pose.translate(5, 6);
        pose.rotate(1);
        assertSame(pose, pose.clear());
        assertTrue(new Matrix3x2f().equals(pose, EPSILON));
        assertState(1, 0, 0, 0);
        assertThrows(IllegalStateException.class, pose::popMatrix);
        pose.pushMatrix();
        pose.scale(2);
        pose.popMatrix();
        assertState(1, 0, 0, 0);
    }

    @Test
    void overflowAndUnderflowLeaveTrackingAndMatrixUnchanged() {
        TrackingGuiPoseStack pose = new TrackingGuiPoseStack(1);
        pose.scale(2);
        pose.translate(3, 4);
        pose.rotate(0.5F);
        Matrix3x2f before = new Matrix3x2f(pose);
        assertThrows(IllegalStateException.class, pose::pushMatrix);
        assertThrows(IllegalStateException.class, pose::popMatrix);
        assertTrue(before.equals(pose, EPSILON));
        assertState(2, 6, 8, 0.5F);
        assertThrows(IllegalArgumentException.class, () -> new TrackingGuiPoseStack(0));
    }

    @Test
    void scalarVectorAndExplicitInPlaceOverloadsAreCountedOnce() {
        TrackingGuiPoseStack pose = new TrackingGuiPoseStack(2);
        pose.scale(2);
        pose.scale(new Vector2f(3, 3));
        pose.scale(4, 4, pose);
        pose.translate(new Vector2f(1, 2));
        pose.translate(3, 4, pose);
        pose.rotate(0.25F, pose);
        assertState(24, 96, 144, 0.25F);
    }

    @Test
    void writingSeparateDestinationDoesNotPublishSourceTransforms() {
        TrackingGuiPoseStack pose = new TrackingGuiPoseStack(2);
        pose.scale(2);
        pose.translate(3, 4);
        pose.rotate(0.5F);
        Matrix3x2f before = new Matrix3x2f(pose);
        Matrix3x2f destination = new Matrix3x2f();
        assertSame(destination, pose.scale(8, 9, destination));
        assertSame(destination, pose.translate(10, 11, destination));
        assertSame(destination, pose.rotate(1, destination));
        assertTrue(before.equals(pose, EPSILON));
        assertState(2, 6, 8, 0.5F);
    }

    @Test
    void preservesLegacyAbsoluteScaleForMirroringNonuniformAndZeroAxes() {
        TrackingGuiPoseStack pose = new TrackingGuiPoseStack(2);
        pose.scale(-2, 4);
        assertState(3, 0, 0, 0);
        pose.scale(0, 0);
        assertState(3, 0, 0, 0);
        pose.clear();
        pose.scale(0, 4);
        assertState(2, 0, 0, 0);
    }

    @Test
    void independentStacksRestoreTheirOwnTransformState() {
        TrackingGuiPoseStack first = new TrackingGuiPoseStack(2);
        TrackingGuiPoseStack second = new TrackingGuiPoseStack(2);
        first.scale(2);
        first.translate(4, 5);
        first.rotate(0.5F);
        second.scale(3);
        second.translate(6, 7);
        second.rotate(0.25F);
        first.pushMatrix();
        assertState(2, 8, 10, 0.5F);
        second.pushMatrix();
        assertState(3, 18, 21, 0.25F);
    }

    @Test
    void translationAndScalingPreserveExplicitElementTilt() {
        TrackingGuiPoseStack pose = new TrackingGuiPoseStack(2);
        RenderRotationUtil.RotationState tilt = new RenderRotationUtil.RotationState();
        tilt.mul(new org.joml.Quaternionf().rotationX(0.5F));
        RenderRotationUtil.setActiveRenderRotation_FancyMenu(tilt);
        pose.translate(2, 3);
        pose.scale(2);
        var active = RenderRotationUtil.getCurrentAdditionalRenderRotation_FancyMenu();
        assertEquals(tilt.x, active.x, EPSILON);
        assertEquals(tilt.w, active.w, EPSILON);
    }

    @Test
    void clonedStackDoesNotShareTrackingFrames() throws CloneNotSupportedException {
        TrackingGuiPoseStack original = new TrackingGuiPoseStack(3);
        original.translate(4, 5);
        original.pushMatrix();
        original.scale(2);
        original.rotate(0.5F);
        TrackingGuiPoseStack copy = (TrackingGuiPoseStack)original.clone();
        copy.scale(3);
        copy.translate(10, 20);
        copy.rotate(1);
        original.pushMatrix();
        assertState(2, 4, 5, 0.5F);
        copy.popMatrix();
        assertState(1, 4, 5, 0);
        original.popMatrix();
        assertState(2, 4, 5, 0.5F);
    }

    private static void assertState(float scale, float x, float y, float angle) {
        assertEquals(scale, RenderScaleUtil.getCurrentAdditionalRenderScale(), EPSILON);
        assertEquals(x, RenderTranslationUtil.getCurrentAdditionalRenderTranslationX(), EPSILON);
        assertEquals(y, RenderTranslationUtil.getCurrentAdditionalRenderTranslationY(), EPSILON);
        assertEquals(0, RenderTranslationUtil.getCurrentAdditionalRenderTranslationZ(), EPSILON);
        var rotation = RenderRotationUtil.getCurrentAdditionalRenderRotation_FancyMenu();
        assertEquals(0, rotation.x, EPSILON);
        assertEquals(0, rotation.y, EPSILON);
        assertEquals(Math.sin(angle * 0.5), rotation.z, EPSILON);
        assertEquals(Math.cos(angle * 0.5), rotation.w, EPSILON);
    }

}
