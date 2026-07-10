package de.keksuccino.fancymenu.util.rendering;

final class GuiBlurScissorMath {

    private static final double BLUR_RADIUS_PADDING_MULTIPLIER_FANCYMENU = 4.0D;
    private static final double ANTI_ALIASING_PADDING_PIXELS_FANCYMENU = 2.0D;

    private GuiBlurScissorMath() {
    }

    static LogicalBounds resolve(float x, float y, float width, float height, float blurRadius, double guiScale, int targetHeight, RenderRotationUtil.Rotation2D maskRotation) {
        double normalizedScale = GuiBlurScaleMath.normalizeGuiScale(guiScale);
        double scaledX = x * normalizedScale;
        double scaledY = targetHeight - y * normalizedScale - height * normalizedScale;
        double scaledWidth = width * normalizedScale;
        double scaledHeight = height * normalizedScale;
        double padding = Math.max(0.0F, blurRadius) * BLUR_RADIUS_PADDING_MULTIPLIER_FANCYMENU + ANTI_ALIASING_PADDING_PIXELS_FANCYMENU;
        double minXInPixels = scaledX - padding;
        double maxXInPixels = scaledX + scaledWidth + padding;
        double minYInPixels = scaledY - padding;
        double maxYInPixels = scaledY + scaledHeight + padding;

        RenderRotationUtil.Rotation2D forwardRotation = invertRotation(maskRotation);
        double halfWidth = scaledWidth * 0.5D;
        double halfHeight = scaledHeight * 0.5D;
        double extentX = Math.abs(forwardRotation.m00()) * halfWidth + Math.abs(forwardRotation.m01()) * halfHeight;
        double extentY = Math.abs(forwardRotation.m10()) * halfWidth + Math.abs(forwardRotation.m11()) * halfHeight;
        if (Double.isFinite(extentX) && Double.isFinite(extentY)) {
            double centerX = scaledX + halfWidth;
            double centerY = scaledY + halfHeight;
            minXInPixels = Math.min(minXInPixels, centerX - extentX - padding);
            maxXInPixels = Math.max(maxXInPixels, centerX + extentX + padding);
            minYInPixels = Math.min(minYInPixels, centerY - extentY - padding);
            maxYInPixels = Math.max(maxYInPixels, centerY + extentY + padding);
        }

        if (!Double.isFinite(minXInPixels) || !Double.isFinite(maxXInPixels) || !Double.isFinite(minYInPixels) || !Double.isFinite(maxYInPixels)) {
            return LogicalBounds.fromGuiBounds(x, y, x + width, y + height);
        }
        return LogicalBounds.fromGuiBounds(minXInPixels / normalizedScale, (targetHeight - maxYInPixels) / normalizedScale, maxXInPixels / normalizedScale, (targetHeight - minYInPixels) / normalizedScale);
    }

    private static RenderRotationUtil.Rotation2D invertRotation(RenderRotationUtil.Rotation2D rotation) {
        float determinant = rotation.m00() * rotation.m11() - rotation.m01() * rotation.m10();
        if (!Float.isFinite(determinant) || Math.abs(determinant) < 1.0E-6F) return RenderRotationUtil.Rotation2D.identity();
        float inverseDeterminant = 1.0F / determinant;
        return new RenderRotationUtil.Rotation2D(rotation.m11() * inverseDeterminant, -rotation.m01() * inverseDeterminant, -rotation.m10() * inverseDeterminant, rotation.m00() * inverseDeterminant);
    }

    record LogicalBounds(int minX, int minY, int maxX, int maxY) {

        private static LogicalBounds fromGuiBounds(double minX, double minY, double maxX, double maxY) {
            return new LogicalBounds((int)Math.floor(minX), (int)Math.floor(minY), (int)Math.ceil(maxX), (int)Math.ceil(maxY));
        }
    }
}
