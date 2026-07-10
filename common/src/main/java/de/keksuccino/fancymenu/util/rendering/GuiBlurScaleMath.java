package de.keksuccino.fancymenu.util.rendering;

final class GuiBlurScaleMath {

    private static final double DEFAULT_GUI_SCALE_FANCYMENU = 1.0D;

    private GuiBlurScaleMath() {
    }

    static double normalizeGuiScale(double guiScale) {
        return Double.isFinite(guiScale) && guiScale > 0.0D ? guiScale : DEFAULT_GUI_SCALE_FANCYMENU;
    }

    static float convertFramebufferRadiusToGui(float framebufferRadius, double guiScale) {
        return (float)(framebufferRadius / normalizeGuiScale(guiScale));
    }

    static float scalePositiveRadius(float guiRadius, double guiScale) {
        double scaledRadius = guiRadius * normalizeGuiScale(guiScale);
        return Double.isFinite(scaledRadius) && scaledRadius > 0.0D && scaledRadius <= Float.MAX_VALUE ? (float)scaledRadius : 0.0F;
    }

    static FramebufferArea scaleArea(float x, float y, float width, float height, double guiScale, int targetHeight) {
        double normalizedScale = normalizeGuiScale(guiScale);
        return new FramebufferArea((float)(x * normalizedScale), (float)(targetHeight - y * normalizedScale - height * normalizedScale), (float)(width * normalizedScale), (float)(height * normalizedScale));
    }

    record FramebufferArea(float x, float y, float width, float height) {

        boolean isValid() {
            return Float.isFinite(this.x) && Float.isFinite(this.y) && Float.isFinite(this.width) && Float.isFinite(this.height) && this.width > 0.0F && this.height > 0.0F;
        }

    }
}
