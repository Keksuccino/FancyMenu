package de.keksuccino.fancymenu.util.rendering;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;
import org.joml.Quaternionfc;

public final class RenderRotationUtil {

    private static final ThreadLocal<RotationState> ACTIVE_RENDER_ROTATION_FANCYMENU = ThreadLocal.withInitial(RotationState::new);

    private RenderRotationUtil() {
    }

    /**
     * Returns the current rotation state accumulated via {@code PoseStack#mulPose(...)} calls.
     * This reflects only additional pose rotations (not the base GUI scale).
     */
    public static RotationState getCurrentAdditionalRenderRotation_FancyMenu() {
        return ACTIVE_RENDER_ROTATION_FANCYMENU.get();
    }

    /**
     * Returns the forward 2x2 rotation/tilt matrix for GUI space.
     */
    public static Rotation2D getCurrentAdditionalRenderRotation2D() {
        RotationState rotation = ACTIVE_RENDER_ROTATION_FANCYMENU.get();
        float x = rotation.x;
        float y = rotation.y;
        float z = rotation.z;
        float w = rotation.w;

        float m00 = 1.0F - 2.0F * y * y - 2.0F * z * z;
        float m01 = 2.0F * x * y - 2.0F * z * w;
        float m10 = 2.0F * x * y + 2.0F * z * w;
        float m11 = 1.0F - 2.0F * x * x - 2.0F * z * z;

        return new Rotation2D(m00, m01, m10, m11);
    }

    /**
     * Returns the 2x2 matrix used for masking rotated/tilted GUI elements.
     * This inverts the rotation/tilt and flips the Z component to match GUI rotation direction.
     */
    public static Rotation2D getCurrentAdditionalRenderMaskRotation2D() {
        RotationState rotation = ACTIVE_RENDER_ROTATION_FANCYMENU.get();
        float x = rotation.x;
        float y = rotation.y;
        float z = -rotation.z;
        float w = rotation.w;

        float m00 = 1.0F - 2.0F * y * y - 2.0F * z * z;
        float m01 = 2.0F * x * y - 2.0F * z * w;
        float m10 = 2.0F * x * y + 2.0F * z * w;
        float m11 = 1.0F - 2.0F * x * x - 2.0F * z * z;

        float det = m00 * m11 - m01 * m10;
        if (!Float.isFinite(det) || Math.abs(det) < 1.0E-6F) {
            return Rotation2D.identity();
        }
        float invDet = 1.0F / det;
        return new Rotation2D(m11 * invDet, -m01 * invDet, -m10 * invDet, m00 * invDet);
    }

    /**
     * Returns the inverse 2x2 rotation/tilt matrix for GUI space.
     * This is suitable for mapping screen pixels back into un-rotated element space.
     */
    public static Rotation2D getCurrentAdditionalRenderInverseRotation2D() {
        Rotation2D rotation = getCurrentAdditionalRenderRotation2D();
        float det = rotation.m00() * rotation.m11() - rotation.m01() * rotation.m10();
        if (!Float.isFinite(det) || Math.abs(det) < 1.0E-6F) {
            return Rotation2D.identity();
        }
        float invDet = 1.0F / det;
        return new Rotation2D(rotation.m11() * invDet, -rotation.m01() * invDet, -rotation.m10() * invDet, rotation.m00() * invDet);
    }

    /**
     * Rotates and tilts the given GUI pose around the given center, the way elements get rendered.<br>
     * A 2D pose has no rotation around the X and Y axes, but GUI rendering is orthographic, so a tilt shows as the XY part of the
     * 3D rotation Rx(vertical tilt) * Ry(horizontal tilt), which compresses the content on the screen.<br>
     * {@link RotatedRectangleHitbox} inverts the same transformation for the mouse, so both have to stay in sync.
     */
    public static void rotateAndTilt(@NotNull Matrix3x2f pose, float centerX, float centerY, float rotationDegrees, float verticalTiltDegrees, float horizontalTiltDegrees) {
        pose.translate(centerX, centerY);
        if ((verticalTiltDegrees != 0.0F) || (horizontalTiltDegrees != 0.0F)) {
            double verticalTilt = Math.toRadians(verticalTiltDegrees);
            double horizontalTilt = Math.toRadians(horizontalTiltDegrees);
            float tilt00 = (float)Math.cos(horizontalTilt);
            float tilt10 = (float)(Math.sin(verticalTilt) * Math.sin(horizontalTilt));
            float tilt11 = (float)Math.cos(verticalTilt);
            // The pose multiplied with the rows (tilt00, 0) and (tilt10, tilt11), written out to not allocate a matrix for every tilted element in every frame
            pose.set((pose.m00() * tilt00) + (pose.m10() * tilt10), (pose.m01() * tilt00) + (pose.m11() * tilt10), pose.m10() * tilt11, pose.m11() * tilt11, pose.m20(), pose.m21());
        }
        if (rotationDegrees != 0.0F) pose.rotate((float)Math.toRadians(rotationDegrees));
        pose.translate(-centerX, -centerY);
    }

    @ApiStatus.Internal
    public static void resetActiveRenderRotation_FancyMenu() {
        ACTIVE_RENDER_ROTATION_FANCYMENU.get().setIdentity();
    }

    @ApiStatus.Internal
    public static void setActiveRenderRotation_FancyMenu(RotationState state) {
        ACTIVE_RENDER_ROTATION_FANCYMENU.get().set(state);
    }

    public record Rotation2D(float m00, float m01, float m10, float m11) {

        public static Rotation2D identity() {
            return new Rotation2D(1.0F, 0.0F, 0.0F, 1.0F);
        }
    }

    public static final class RotationState {
        public float x = 0.0F;
        public float y = 0.0F;
        public float z = 0.0F;
        public float w = 1.0F;

        public RotationState() {
        }

        public RotationState(RotationState other) {
            this.set(other);
        }

        public void set(RotationState other) {
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
            this.w = other.w;
        }

        public void setIdentity() {
            this.x = 0.0F;
            this.y = 0.0F;
            this.z = 0.0F;
            this.w = 1.0F;
        }

        public void mul(Quaternionfc quaternion) {
            float qx = quaternion.x();
            float qy = quaternion.y();
            float qz = quaternion.z();
            float qw = quaternion.w();

            float nx = this.w * qx + this.x * qw + this.y * qz - this.z * qy;
            float ny = this.w * qy - this.x * qz + this.y * qw + this.z * qx;
            float nz = this.w * qz + this.x * qy - this.y * qx + this.z * qw;
            float nw = this.w * qw - this.x * qx - this.y * qy - this.z * qz;

            this.x = nx;
            this.y = ny;
            this.z = nz;
            this.w = nw;

            float len = (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
            if (len > 0.0F && Float.isFinite(len)) {
                float invLen = 1.0F / len;
                this.x *= invLen;
                this.y *= invLen;
                this.z *= invLen;
                this.w *= invLen;
            }
        }
    }
}
