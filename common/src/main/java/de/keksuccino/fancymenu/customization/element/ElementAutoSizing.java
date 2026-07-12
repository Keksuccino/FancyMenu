package de.keksuccino.fancymenu.customization.element;

final class ElementAutoSizing {

    private static final double MIN_SCALE_FACTOR = 0.01D;

    private ElementAutoSizing() {
    }

    static int normalizeGuiScale(double guiScale) {
        if (!Double.isFinite(guiScale) || guiScale <= 0.0D) return 1;
        return Math.max(1, (int)Math.floor(guiScale));
    }

    static int calculateGuiDimension(int pixelDimension, int guiScale) {
        if (pixelDimension <= 0 || guiScale <= 0) return 0;
        return Math.max(1, (int)Math.ceil(pixelDimension / (double)guiScale));
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

    static int resolveBaseGuiScale(int savedGuiScale, int basePixelWidth, int basePixelHeight, int currentGuiWidth, int currentGuiHeight, int currentGuiScale, double forcedGuiScale, int vanillaMaxScale, boolean enforceUnicode) {
        if (savedGuiScale > 0) return normalizeGuiScale(savedGuiScale);
        int normalizedCurrentScale = normalizeGuiScale(currentGuiScale);
        if (basePixelWidth == currentGuiWidth * normalizedCurrentScale && basePixelHeight == currentGuiHeight * normalizedCurrentScale) return normalizedCurrentScale;
        if (Double.isFinite(forcedGuiScale) && forcedGuiScale > 0.0D) return normalizeGuiScale(forcedGuiScale);
        return inferVanillaGuiScale(basePixelWidth, basePixelHeight, vanillaMaxScale, enforceUnicode);
    }

    /**
     * Legacy element data only stores the baseline pixel dimensions. This mirrors Vanilla's GUI-scale calculation so
     * the old values can be converted back to logical coordinates without changing their serialized meaning.
     */
    static int inferVanillaGuiScale(int width, int height, int maxScale, boolean enforceUnicode) {
        int scale = 1;
        while (scale != maxScale && scale < width && scale < height && width / (scale + 1) >= 320 && height / (scale + 1) >= 240) scale++;
        if (enforceUnicode && scale % 2 != 0) scale++;
        return scale;
    }

    static Baseline selectStackedBaseline(Baseline current, Baseline override) {
        return override.screenWidth() != 0 || override.screenHeight() != 0 ? override : current;
    }

    record Baseline(int screenWidth, int screenHeight, int guiScale) {
    }

}
