package de.keksuccino.fancymenu.util.rendering;

import javax.annotation.Nullable;

final class GuiBlurGeometry {

    private GuiBlurGeometry() {
    }

    static double sanitizeGuiScale(double guiScale) {
        return Double.isFinite(guiScale) && guiScale > 0.0D ? guiScale : 1.0D;
    }

    static float convertFramebufferBlurRadiusToGui(float framebufferBlurRadius, double guiScale) {
        if (!Float.isFinite(framebufferBlurRadius) || framebufferBlurRadius <= 0.0F) return 0.0F;
        return positiveFiniteFloat((double)framebufferBlurRadius / sanitizeGuiScale(guiScale));
    }

    @Nullable
    static ScaledArea scaleArea(float x, float y, float width, float height, float blurRadius, int targetHeight, double guiScale) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(width) || !Float.isFinite(height) || width <= 0.0F || height <= 0.0F || targetHeight <= 0) return null;
        double safeScale = sanitizeGuiScale(guiScale);
        double scaledX = (double)x * safeScale;
        double scaledY = (double)targetHeight - (double)y * safeScale - (double)height * safeScale;
        double scaledWidth = (double)width * safeScale;
        double scaledHeight = (double)height * safeScale;
        if (!isFiniteFloat(scaledX) || !isFiniteFloat(scaledY) || !isFinitePositiveFloat(scaledWidth) || !isFinitePositiveFloat(scaledHeight)) return null;
        return new ScaledArea(scaledX, scaledY, scaledWidth, scaledHeight, positiveFiniteFloat((double)blurRadius * safeScale), safeScale);
    }

    static float scaleCornerRadius(float radius, double guiScale) {
        if (!Float.isFinite(radius) || radius <= 0.0F) return 0.0F;
        return positiveFiniteFloat((double)radius * sanitizeGuiScale(guiScale));
    }

    /**
     * Converts physical bounds to outward-rounded logical bounds instead of using a direct framebuffer scissor. Minecraft 1.20.1's GuiGraphics
     * multiplies and truncates these integers back to framebuffer pixels, while its logical scissor stack preserves intersections with parent clips.
     */
    @Nullable
    static ScissorBounds convertPixelBoundsToGui(double minX, double minY, double maxX, double maxY, int targetWidth, int targetHeight, double guiScale) {
        if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxX) || !Double.isFinite(maxY) || maxX < minX || maxY < minY || targetWidth <= 0 || targetHeight <= 0) return null;
        double safeScale = sanitizeGuiScale(guiScale);
        double clampedMinX = clamp(minX, 0.0D, targetWidth);
        double clampedMaxX = clamp(maxX, 0.0D, targetWidth);
        double clampedMinY = clamp(minY, 0.0D, targetHeight);
        double clampedMaxY = clamp(maxY, 0.0D, targetHeight);
        if (clampedMaxX <= clampedMinX || clampedMaxY <= clampedMinY) return new ScissorBounds(0, 0, 0, 0);

        long logicalLeft = floorToNonNegativeInt(clampedMinX / safeScale);
        long logicalBottom = ceilToNonNegativeInt(((double)targetHeight - clampedMinY) / safeScale);
        if (logicalLeft < 0L || logicalBottom < 0L) return null;

        int physicalLeft = (int)((double)logicalLeft * safeScale);
        int physicalBottom = (int)((double)targetHeight - (double)logicalBottom * safeScale);
        int requiredWidth = ceilToInt(clampedMaxX) - physicalLeft;
        int requiredHeight = ceilToInt(clampedMaxY) - physicalBottom;
        long logicalWidth = logicalSpanForPhysicalCoverage(requiredWidth, safeScale);
        long logicalHeight = logicalSpanForPhysicalCoverage(requiredHeight, safeScale);
        if (logicalWidth < 0L || logicalHeight < 0L || logicalLeft + logicalWidth > Integer.MAX_VALUE || logicalBottom - logicalHeight < Integer.MIN_VALUE) return null;
        return new ScissorBounds((int)logicalLeft, (int)(logicalBottom - logicalHeight), (int)(logicalLeft + logicalWidth), (int)logicalBottom);
    }

    private static float positiveFiniteFloat(double value) {
        return isFinitePositiveFloat(value) ? (float)value : 0.0F;
    }

    private static boolean isFinitePositiveFloat(double value) {
        return value > 0.0D && isFiniteFloat(value) && (float)value > 0.0F;
    }

    private static boolean isFiniteFloat(double value) {
        return Double.isFinite(value) && Math.abs(value) <= Float.MAX_VALUE;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private static long logicalSpanForPhysicalCoverage(int requiredPixels, double guiScale) {
        if (requiredPixels <= 0) return 0L;
        double rawSpan = Math.ceil((double)requiredPixels / guiScale);
        if (!Double.isFinite(rawSpan) || rawSpan > Integer.MAX_VALUE) return -1L;
        long span = (long)rawSpan;
        if ((int)((double)span * guiScale) < requiredPixels) {
            rawSpan = Math.ceil(((double)requiredPixels + 1.0D) / guiScale);
            if (!Double.isFinite(rawSpan) || rawSpan > Integer.MAX_VALUE) return -1L;
            span = (long)rawSpan;
            if ((int)((double)span * guiScale) < requiredPixels) return -1L;
        }
        return span;
    }

    private static long floorToNonNegativeInt(double value) {
        double floored = Math.floor(value);
        return Double.isFinite(floored) && floored >= 0.0D && floored <= Integer.MAX_VALUE ? (long)floored : -1L;
    }

    private static long ceilToNonNegativeInt(double value) {
        double ceiled = Math.ceil(value);
        return Double.isFinite(ceiled) && ceiled >= 0.0D && ceiled <= Integer.MAX_VALUE ? (long)ceiled : -1L;
    }

    private static int ceilToInt(double value) {
        return (int)Math.ceil(value);
    }

    record ScaledArea(double x, double y, double width, double height, float blurRadius, double guiScale) {
    }

    record ScissorBounds(int minX, int minY, int maxX, int maxY) {

        boolean isEmpty() {
            return maxX <= minX || maxY <= minY;
        }

    }

}
