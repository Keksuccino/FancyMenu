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
        RenderRotationUtil.Rotation2D rotation = RenderRotationUtil.getCurrentAdditionalRenderMaskRotation2D();
        return new PoseTransform(scale, translationX, translationY, pose.m00(), pose.m01(), pose.m10(), pose.m11(), rotation);
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

    private static float axisLength_FancyMenu(float x, float y) {
        float length = (float)Math.sqrt(x * x + y * y);
        return Float.isFinite(length) && length > 0.0F ? length : 0.0F;
    }

    private static float finiteOrDefault_FancyMenu(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    record PoseTransform(float scale, float translationX, float translationY, float m00, float m01, float m10, float m11, RenderRotationUtil.Rotation2D rotation) {

        float transformX(float x) {
            return x * this.scale + this.translationX;
        }

        float transformY(float y) {
            return y * this.scale + this.translationY;
        }

        TransformedArea transformArea(float x, float y, float width, float height) {
            float transformedWidth = width * this.scale;
            float transformedHeight = height * this.scale;
            float centerX = this.transformPositionX(x + (width * 0.5F), y + (height * 0.5F));
            float centerY = this.transformPositionY(x + (width * 0.5F), y + (height * 0.5F));
            return new TransformedArea(
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

    record TransformedArea(float x, float y, float width, float height) {
    }
}
