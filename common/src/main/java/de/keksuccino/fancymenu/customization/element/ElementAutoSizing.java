package de.keksuccino.fancymenu.customization.element;

final class ElementAutoSizing {

    private static final double MIN_SCALE_FACTOR = 0.01D;

    private ElementAutoSizing() {
    }

    static double normalizeGuiScale(double guiScale) {
        return isValidGuiScale(guiScale) ? guiScale : 1.0D;
    }

    static boolean isValidGuiScale(double guiScale) {
        return Double.isFinite(guiScale) && guiScale > 0.0D;
    }

    static Baseline captureBaseline(int logicalWidth, int logicalHeight, double guiScale) {
        double normalizedScale = normalizeGuiScale(guiScale);
        return new Baseline(calculatePhysicalDimension(logicalWidth, normalizedScale), calculatePhysicalDimension(logicalHeight, normalizedScale), normalizedScale);
    }

    static int calculatePhysicalDimension(int logicalDimension, double guiScale) {
        if (logicalDimension <= 0 || !isValidGuiScale(guiScale)) return 0;
        return Math.max(1, (int)(logicalDimension * guiScale));
    }

    static int calculateGuiDimension(int pixelDimension, double guiScale) {
        if (pixelDimension <= 0 || !isValidGuiScale(guiScale)) return 0;
        return Math.max(1, (int)Math.ceil(pixelDimension / guiScale));
    }

    static double calculateScaleFactor(int currentWidth, int currentHeight, int baseWidth, int baseHeight) {
        if (currentWidth <= 0 || currentHeight <= 0 || baseWidth <= 0 || baseHeight <= 0) return 1.0D;
        double scaleX = currentWidth / (double)baseWidth;
        double scaleY = currentHeight / (double)baseHeight;
        return Math.max(MIN_SCALE_FACTOR, Math.min(scaleX, scaleY));
    }

    static int calculateElementDimension(int baseDimension, double scaleFactor) {
        if (baseDimension <= 0) return 1;
        double resolvedScaleFactor = Double.isFinite(scaleFactor) && scaleFactor > 0.0D ? scaleFactor : 1.0D;
        return Math.max(1, (int)(baseDimension * resolvedScaleFactor));
    }

    static double resolveBaselineGuiScale(Baseline baseline, int currentWidth, int currentHeight, double currentGuiScale, double forcedGuiScale, int vanillaMaxScale, boolean enforceUnicode) {
        if (isValidGuiScale(baseline.guiScale())) return baseline.guiScale();
        double normalizedCurrentScale = normalizeGuiScale(currentGuiScale);
        if (calculateGuiDimension(baseline.pixelWidth(), normalizedCurrentScale) == currentWidth && calculateGuiDimension(baseline.pixelHeight(), normalizedCurrentScale) == currentHeight) return normalizedCurrentScale;
        if (isValidGuiScale(forcedGuiScale)) return forcedGuiScale;
        return inferVanillaGuiScale(baseline.pixelWidth(), baseline.pixelHeight(), vanillaMaxScale, enforceUnicode);
    }

    /** Mirrors Window#calculateScale so legacy physical baselines can recover the logical scale used when they were saved. */
    static double inferVanillaGuiScale(int width, int height, int maxScale, boolean enforceUnicode) {
        if (width <= 0 || height <= 0) return 1.0D;
        int scale = 1;
        while (scale != maxScale && scale < width && scale < height && width / (scale + 1) >= 320 && height / (scale + 1) >= 240) scale++;
        if (enforceUnicode && scale % 2 != 0) scale++;
        return scale;
    }

    static Baseline selectStackedBaseline(Baseline current, Baseline candidate) {
        return candidate.hasDimensions() ? candidate : current;
    }

    record Baseline(int pixelWidth, int pixelHeight, double guiScale) {

        boolean hasDimensions() {
            return pixelWidth != 0 || pixelHeight != 0;
        }

    }

}
