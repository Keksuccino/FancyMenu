package de.keksuccino.fancymenu.util.rendering;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fc;

import javax.annotation.Nonnull;

final class GuiPoseTransformUtil {

    private static final float DEFAULT_SCALE_FANCYMENU = 1.0F;
    private static final float DEGENERATE_DETERMINANT_FANCYMENU = 1.0E-6F;

    private GuiPoseTransformUtil() {
    }

    @Nonnull
    static PoseTransform resolve(@Nonnull GuiGraphics graphics) {
        return resolve(graphics.pose());
    }

    /**
     * Splits the pose into what the shape shaders work with: one scale for all sizes of a shape, and the matrix that maps a screen offset
     * from the center of the shape back into the shape.<br>
     * Do not take the scale from the length of the pose axes: a tilted element compresses its content in one direction,
     * which must not shrink the sizes, because the matrix compresses the shape already.
     */
    @Nonnull
    static PoseTransform resolve(@Nonnull Matrix3x2fc pose) {
        float scale = resolveScale_FancyMenu(pose);
        float translationX = finiteOrDefault_FancyMenu(pose.m20(), 0.0F);
        float translationY = finiteOrDefault_FancyMenu(pose.m21(), 0.0F);
        return new PoseTransform(scale, translationX, translationY, pose.m00(), pose.m01(), pose.m10(), pose.m11(), resolveRotation_FancyMenu(pose, scale));
    }

    /**
     * Returns the largest stretch of the pose, which is the largest singular value of its 2x2 part.
     * For a uniformly scaled pose, this is its scale, no matter how it is rotated and tilted.
     */
    private static float resolveScale_FancyMenu(@Nonnull Matrix3x2fc pose) {
        double e = ((double)pose.m00() + pose.m11()) * 0.5D;
        double f = ((double)pose.m00() - pose.m11()) * 0.5D;
        double g = ((double)pose.m01() + pose.m10()) * 0.5D;
        double h = ((double)pose.m01() - pose.m10()) * 0.5D;
        float scale = (float)(Math.sqrt((e * e) + (h * h)) + Math.sqrt((f * f) + (g * g)));
        return (Float.isFinite(scale) && (scale > 0.0F)) ? scale : DEFAULT_SCALE_FANCYMENU;
    }

    /**
     * Returns the inverse of the 2x2 part of the pose without its scale, in the space of the shape shaders, where the Y axis points up.
     * For a pose that is only scaled and rotated, this is its rotation.
     */
    @Nonnull
    private static RenderRotationUtil.Rotation2D resolveRotation_FancyMenu(@Nonnull Matrix3x2fc pose, float scale) {
        float determinant = (pose.m00() * pose.m11()) - (pose.m10() * pose.m01());
        float factor = scale / determinant;
        if (!Float.isFinite(factor) || (Math.abs(determinant) <= (scale * scale * DEGENERATE_DETERMINANT_FANCYMENU))) {
            return RenderRotationUtil.Rotation2D.identity();
        }
        return new RenderRotationUtil.Rotation2D(pose.m11() * factor, pose.m10() * factor, pose.m01() * factor, pose.m00() * factor);
    }

    private static float finiteOrDefault_FancyMenu(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    /**
     * @param rotation The matrix that maps a screen offset from the center of a shape back into the shape, as the shape shaders take it.
     */
    record PoseTransform(float scale, float translationX, float translationY, float m00, float m01, float m10, float m11, RenderRotationUtil.Rotation2D rotation) {

        TransformedRect transformRect(float x, float y, float width, float height) {
            float transformedWidth = width * this.scale;
            float transformedHeight = height * this.scale;
            float centerX = this.transformPositionX(x + (width * 0.5F), y + (height * 0.5F));
            float centerY = this.transformPositionY(x + (width * 0.5F), y + (height * 0.5F));
            return new TransformedRect(
                    centerX - (transformedWidth * 0.5F),
                    centerY - (transformedHeight * 0.5F),
                    transformedWidth,
                    transformedHeight
            );
        }

        private float transformPositionX(float x, float y) {
            return (x * this.m00) + (y * this.m10) + this.translationX;
        }

        private float transformPositionY(float x, float y) {
            return (x * this.m01) + (y * this.m11) + this.translationY;
        }
    }

    record TransformedRect(float x, float y, float width, float height) {
    }
}
