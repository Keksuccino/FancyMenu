package de.keksuccino.fancymenu.util.rendering;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

import javax.annotation.Nonnull;

final class GuiPoseTransformUtil {

    private static final float DEFAULT_SCALE_FANCYMENU = 1.0F;

    private GuiPoseTransformUtil() {
    }

    @Nonnull
    static PoseTransform resolve(@Nonnull GuiGraphicsExtractor graphics) {
        Matrix3x2fStack pose = graphics.pose();
        float scaleX = axisLength_FancyMenu(pose.m00(), pose.m01());
        float scaleY = axisLength_FancyMenu(pose.m10(), pose.m11());
        float scale = resolveScale_FancyMenu(scaleX, scaleY);
        float translationX = finiteOrDefault_FancyMenu(pose.m20(), 0.0F);
        float translationY = finiteOrDefault_FancyMenu(pose.m21(), 0.0F);
        RenderRotationUtil.Rotation2D rotation = resolveRotation_FancyMenu(pose, scaleX, scaleY);
        return new PoseTransform(scale, translationX, translationY, rotation);
    }

    private static float resolveScale_FancyMenu(float scaleX, float scaleY) {
        if (scaleX == 0.0F && scaleY == 0.0F) {
            return DEFAULT_SCALE_FANCYMENU;
        }
        if (scaleX == 0.0F) {
            return scaleY;
        }
        if (scaleY == 0.0F) {
            return scaleX;
        }
        if (Math.abs(scaleX - scaleY) < 1.0E-4F) {
            return scaleX;
        }
        return (scaleX + scaleY) * 0.5F;
    }

    @Nonnull
    private static RenderRotationUtil.Rotation2D resolveRotation_FancyMenu(@Nonnull Matrix3x2fStack pose, float scaleX, float scaleY) {
        if (scaleX <= 0.0F || scaleY <= 0.0F) {
            return RenderRotationUtil.Rotation2D.identity();
        }

        float m00 = pose.m00() / scaleX;
        float m01 = pose.m10() / scaleY;
        float m10 = pose.m01() / scaleX;
        float m11 = pose.m11() / scaleY;
        if (!Float.isFinite(m00) || !Float.isFinite(m01) || !Float.isFinite(m10) || !Float.isFinite(m11)) {
            return RenderRotationUtil.Rotation2D.identity();
        }
        return new RenderRotationUtil.Rotation2D(m00, m01, m10, m11);
    }

    private static float axisLength_FancyMenu(float x, float y) {
        float length = (float)Math.sqrt(x * x + y * y);
        return Float.isFinite(length) && length > 0.0F ? length : 0.0F;
    }

    private static float finiteOrDefault_FancyMenu(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    record PoseTransform(float scale, float translationX, float translationY, RenderRotationUtil.Rotation2D rotation) {

        float transformX(float x) {
            return x * this.scale + this.translationX;
        }

        float transformY(float y) {
            return y * this.scale + this.translationY;
        }
    }
}
