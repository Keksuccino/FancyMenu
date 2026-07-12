package de.keksuccino.fancymenu.customization.element;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

final class ElementAutoSizing {

    private static final double MIN_SCALE_FACTOR = 0.01D;
    private static final int MAX_LEGACY_CANDIDATE_PAIRS = 65_536;
    private static final int MAX_LEGACY_CACHE_ENTRIES = 128;
    private static final int LEGACY_FRAMEBUFFER_ROUNDING_MARGIN = 2;
    private static final Map<LegacyBaselineKey, Baseline> LEGACY_BASELINE_CACHE = new LinkedHashMap<>(MAX_LEGACY_CACHE_ENTRIES, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<LegacyBaselineKey, Baseline> eldest) {
            return this.size() > MAX_LEGACY_CACHE_ENTRIES;
        }
    };

    private ElementAutoSizing() {
    }

    static boolean isValidGuiScale(double guiScale) {
        return Double.isFinite(guiScale) && guiScale > 0.0D;
    }

    static boolean isValidGuiDimensions(int guiWidth, int guiHeight) {
        return guiWidth > 0 && guiHeight > 0;
    }

    static double sanitizeGuiScale(double guiScale) {
        return isValidGuiScale(guiScale) ? guiScale : 1.0D;
    }

    static int calculatePixelDimension(int guiDimension, double guiScale) {
        if (guiDimension <= 0) return 0;
        return (int)(guiDimension * sanitizeGuiScale(guiScale));
    }

    static int calculateGuiDimension(int pixelDimension, double guiScale) {
        if (pixelDimension <= 0 || !isValidGuiScale(guiScale)) return 0;
        return Math.max(1, (int)Math.ceil(pixelDimension / guiScale));
    }

    static int calculateWindowGuiDimension(int framebufferDimension, double guiScale) {
        if (framebufferDimension <= 0 || !isValidGuiScale(guiScale)) return 0;
        double scaledDimension = framebufferDimension / guiScale;
        if (!Double.isFinite(scaledDimension) || scaledDimension > Integer.MAX_VALUE) return 0;
        int truncatedDimension = (int)scaledDimension;
        return scaledDimension > truncatedDimension ? truncatedDimension + 1 : truncatedDimension;
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

    static boolean matchesLegacyBaseline(int pixelWidth, int pixelHeight, int guiWidth, int guiHeight, double guiScale) {
        if (!isValidGuiScale(guiScale)) return false;
        return pixelWidth == calculatePixelDimension(guiWidth, guiScale) && pixelHeight == calculatePixelDimension(guiHeight, guiScale);
    }

    @Nullable
    static Baseline resolveCurrentLegacyBaseline(int pixelWidth, int pixelHeight, int currentGuiWidth, int currentGuiHeight, double currentGuiScale) {
        if (!matchesLegacyBaseline(pixelWidth, pixelHeight, currentGuiWidth, currentGuiHeight, currentGuiScale)) return null;
        return new Baseline(currentGuiWidth, currentGuiHeight, currentGuiScale, LegacyResolution.RECONSTRUCTED_UNIQUE);
    }

    static Baseline reconstructLegacyLayoutBaseline(int pixelWidth, int pixelHeight, double forcedScale, int autoScalingWidth, int autoScalingHeight) {
        LegacyBaselineKey key = new LegacyBaselineKey(pixelWidth, pixelHeight, Double.doubleToLongBits(forcedScale), autoScalingWidth, autoScalingHeight);
        synchronized (LEGACY_BASELINE_CACHE) {
            Baseline cached = LEGACY_BASELINE_CACHE.get(key);
            if (cached != null) return cached;
        }
        Baseline baseline = reconstructLegacyLayoutBaseline(pixelWidth, pixelHeight, forcedScale, autoScalingWidth, autoScalingHeight, MAX_LEGACY_CANDIDATE_PAIRS);
        synchronized (LEGACY_BASELINE_CACHE) {
            LEGACY_BASELINE_CACHE.put(key, baseline);
        }
        return baseline;
    }

    /**
     * Legacy forced-and-auto-scaled layouts cannot be inverted algebraically because Window and layout scaling mix
     * ceiling, truncation, and floating-point operations. Candidate framebuffer dimensions are therefore replayed
     * through the target operation order and accepted only when they reproduce the complete serialized pixel tuple.
     * The search radius includes the maximum possible derived scale plus a small floating-point margin, while the
     * candidate cap protects layout loading from malformed or intentionally extreme metadata.
     */
    static Baseline reconstructLegacyLayoutBaseline(int pixelWidth, int pixelHeight, double forcedScale, int autoScalingWidth, int autoScalingHeight, int maxCandidatePairs) {
        Baseline fallback = createLegacyLayoutFallback(pixelWidth, pixelHeight, forcedScale, autoScalingWidth, autoScalingHeight);
        if (pixelWidth <= 0 || pixelHeight <= 0 || !isValidGuiScale(forcedScale) || autoScalingWidth <= 0 || autoScalingHeight <= 0 || maxCandidatePairs <= 0) return fallback;

        int maximumFramebufferWidth = pixelWidth < Integer.MAX_VALUE ? pixelWidth + 1 : pixelWidth;
        int maximumFramebufferHeight = pixelHeight < Integer.MAX_VALUE ? pixelHeight + 1 : pixelHeight;
        int maximumForcedGuiWidth = calculateWindowGuiDimension(maximumFramebufferWidth, forcedScale);
        int maximumForcedGuiHeight = calculateWindowGuiDimension(maximumFramebufferHeight, forcedScale);
        double maximumScaleX = calculateAutoScaledAxis(maximumForcedGuiWidth, forcedScale, autoScalingWidth);
        double maximumScaleY = calculateAutoScaledAxis(maximumForcedGuiHeight, forcedScale, autoScalingHeight);
        double maximumScale = Math.min(maximumScaleX, maximumScaleY);
        if (!isValidGuiScale(maximumScale) || maximumScale >= Integer.MAX_VALUE) return fallback;

        int minimumFramebufferWidth = Math.max(1, (int)Math.floor(pixelWidth - maximumScale) - LEGACY_FRAMEBUFFER_ROUNDING_MARGIN);
        int minimumFramebufferHeight = Math.max(1, (int)Math.floor(pixelHeight - maximumScale) - LEGACY_FRAMEBUFFER_ROUNDING_MARGIN);
        long widthCandidateCount = (long)maximumFramebufferWidth - minimumFramebufferWidth + 1L;
        long heightCandidateCount = (long)maximumFramebufferHeight - minimumFramebufferHeight + 1L;
        if (widthCandidateCount <= 0L || heightCandidateCount <= 0L || widthCandidateCount > maxCandidatePairs || heightCandidateCount > maxCandidatePairs || widthCandidateCount * heightCandidateCount > maxCandidatePairs) return fallback;

        Comparator<LegacyCandidate> comparator = legacyCandidateComparator();
        LegacyCandidate selected = null;
        long firstLogicalSizeKey = 0L;
        boolean candidateFound = false;
        boolean ambiguousLogicalSize = false;
        for (long framebufferWidthValue = minimumFramebufferWidth; framebufferWidthValue <= maximumFramebufferWidth; framebufferWidthValue++) {
            int framebufferWidth = (int)framebufferWidthValue;
            for (long framebufferHeightValue = minimumFramebufferHeight; framebufferHeightValue <= maximumFramebufferHeight; framebufferHeightValue++) {
                int framebufferHeight = (int)framebufferHeightValue;
                LegacyCandidate candidate = replayLegacyLayout(framebufferWidth, framebufferHeight, forcedScale, autoScalingWidth, autoScalingHeight, pixelWidth, pixelHeight);
                if (candidate == null) continue;
                long logicalSizeKey = ((long)candidate.guiWidth() << 32) ^ (candidate.guiHeight() & 0xFFFFFFFFL);
                if (!candidateFound) {
                    firstLogicalSizeKey = logicalSizeKey;
                    candidateFound = true;
                } else if (logicalSizeKey != firstLogicalSizeKey) {
                    ambiguousLogicalSize = true;
                }
                if (selected == null || comparator.compare(candidate, selected) < 0) selected = candidate;
            }
        }
        if (selected == null) return fallback;

        LegacyResolution resolution = ambiguousLogicalSize ? LegacyResolution.RECONSTRUCTED_AMBIGUOUS : LegacyResolution.RECONSTRUCTED_UNIQUE;
        return new Baseline(selected.guiWidth(), selected.guiHeight(), selected.guiScale(), resolution);
    }

    @Nullable
    private static LegacyCandidate replayLegacyLayout(int framebufferWidth, int framebufferHeight, double forcedScale, int autoScalingWidth, int autoScalingHeight, int expectedPixelWidth, int expectedPixelHeight) {
        int forcedGuiWidth = calculateWindowGuiDimension(framebufferWidth, forcedScale);
        int forcedGuiHeight = calculateWindowGuiDimension(framebufferHeight, forcedScale);
        double scaleX = calculateAutoScaledAxis(forcedGuiWidth, forcedScale, autoScalingWidth);
        double scaleY = calculateAutoScaledAxis(forcedGuiHeight, forcedScale, autoScalingHeight);
        double guiScale = Math.min(scaleX, scaleY);
        if (!isValidGuiScale(guiScale)) return null;
        int guiWidth = calculateWindowGuiDimension(framebufferWidth, guiScale);
        int guiHeight = calculateWindowGuiDimension(framebufferHeight, guiScale);
        if (!isValidGuiDimensions(guiWidth, guiHeight)) return null;
        int pixelWidth = calculatePixelDimension(guiWidth, guiScale);
        int pixelHeight = calculatePixelDimension(guiHeight, guiScale);
        if (pixelWidth != expectedPixelWidth || pixelHeight != expectedPixelHeight) return null;
        return new LegacyCandidate(framebufferWidth, framebufferHeight, guiWidth, guiHeight, guiScale);
    }

    private static double calculateAutoScaledAxis(int forcedGuiDimension, double forcedScale, int autoScalingDimension) {
        if (forcedGuiDimension <= 0 || !isValidGuiScale(forcedScale) || autoScalingDimension <= 0) return 0.0D;
        double guiDimension = forcedGuiDimension * forcedScale;
        double percent = (guiDimension / (double)autoScalingDimension) * 100.0D;
        return (percent / 100.0D) * forcedScale;
    }

    private static Baseline createLegacyLayoutFallback(int pixelWidth, int pixelHeight, double forcedScale, int autoScalingWidth, int autoScalingHeight) {
        double guiScale = inferLegacyLayoutGuiScale(pixelWidth, pixelHeight, forcedScale, autoScalingWidth, autoScalingHeight);
        return new Baseline(calculateGuiDimension(pixelWidth, guiScale), calculateGuiDimension(pixelHeight, guiScale), guiScale, LegacyResolution.FALLBACK);
    }

    private static Comparator<LegacyCandidate> legacyCandidateComparator() {
        return Comparator.comparingLong((LegacyCandidate candidate) -> -((long)candidate.framebufferWidth() + candidate.framebufferHeight())).thenComparing(Comparator.comparingInt(LegacyCandidate::framebufferWidth).reversed()).thenComparing(Comparator.comparingInt(LegacyCandidate::framebufferHeight).reversed());
    }

    /**
     * Legacy element data only stores the baseline pixel dimensions. This mirrors the target layout's calculation as a
     * deterministic fallback for malformed, extreme, or genuinely unreconstructable legacy baselines.
     */
    static double inferLegacyLayoutGuiScale(int pixelWidth, int pixelHeight, double forcedScale, int autoScalingWidth, int autoScalingHeight) {
        if (!isValidGuiScale(forcedScale)) return 1.0D;
        if (pixelWidth <= 0 || pixelHeight <= 0 || autoScalingWidth <= 0 || autoScalingHeight <= 0) return forcedScale;
        int forcedGuiWidth = calculateWindowGuiDimension(pixelWidth, forcedScale);
        int forcedGuiHeight = calculateWindowGuiDimension(pixelHeight, forcedScale);
        double inferredScale = Math.min(calculateAutoScaledAxis(forcedGuiWidth, forcedScale, autoScalingWidth), calculateAutoScaledAxis(forcedGuiHeight, forcedScale, autoScalingHeight));
        return isValidGuiScale(inferredScale) ? inferredScale : forcedScale;
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

    static String serializeGuiScale(double guiScale) {
        if (!isValidGuiScale(guiScale)) return "0";
        double integralScale = Math.rint(guiScale);
        if (guiScale == integralScale && integralScale < 0x1.0p63) return Long.toString((long)integralScale);
        return Double.toString(guiScale);
    }

    enum LegacyResolution {
        EXACT_METADATA,
        RECONSTRUCTED_UNIQUE,
        RECONSTRUCTED_AMBIGUOUS,
        FALLBACK
    }

    record Baseline(int guiWidth, int guiHeight, double guiScale, LegacyResolution resolution) {
    }

    private record LegacyBaselineKey(int pixelWidth, int pixelHeight, long forcedScaleBits, int autoScalingWidth, int autoScalingHeight) {
    }

    private record LegacyCandidate(int framebufferWidth, int framebufferHeight, int guiWidth, int guiHeight, double guiScale) {
    }

}
