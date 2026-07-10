package de.keksuccino.fancymenu.customization.element;

final class ElementAutoSizing {

    private static final double MIN_SCALE_FACTOR = 0.01D;

    private ElementAutoSizing() {
    }

    static double normalizeGuiScale(double guiScale) {
        if (!Double.isFinite(guiScale) || guiScale <= 0.0D) return 1.0D;
        return guiScale;
    }

    static int calculateGuiDimension(int pixelDimension, double guiScale) {
        if (pixelDimension <= 0 || !Double.isFinite(guiScale) || guiScale <= 0.0D) return 0;
        return Math.max(1, (int)Math.ceil(pixelDimension / guiScale));
    }

    static double calculateScaleFactor(int currentWidth, int currentHeight, int baseWidth, int baseHeight) {
        if (currentWidth <= 0 || currentHeight <= 0 || baseWidth <= 0 || baseHeight <= 0) return 1.0D;
        double scaleX = currentWidth / (double)baseWidth;
        double scaleY = currentHeight / (double)baseHeight;
        return Math.max(MIN_SCALE_FACTOR, Math.min(scaleX, scaleY));
    }

    static int calculateElementDimension(int baseDimension, double scaleFactor) {
        return Math.max(1, (int)(baseDimension * scaleFactor));
    }

    /**
     * Legacy element data only stores the baseline pixel dimensions. This mirrors Vanilla's GUI-scale calculation so
     * the old values can be converted back to the logical coordinate space without changing their serialized meaning.
     */
    static int inferVanillaGuiScale(int width, int height, int maxScale, boolean enforceUnicode) {
        int scale = 1;
        while (scale != maxScale && scale < width && scale < height && width / (scale + 1) >= 320 && height / (scale + 1) >= 240) {
            scale++;
        }
        if (enforceUnicode && scale % 2 != 0) scale++;
        return scale;
    }

}
