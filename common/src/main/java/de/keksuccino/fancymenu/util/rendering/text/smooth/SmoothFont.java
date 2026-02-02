package de.keksuccino.fancymenu.util.rendering.text.smooth;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SmoothFont implements AutoCloseable {

    // Mirrored from MaterialIcons sizing heuristics to keep glyph quality consistent.
    private static final int BASELINE_GLYPH_DP = 24;
    private static final float BASELINE_QUALITY_SCALE = 2.0F;
    private static final float GLYPH_COVERAGE_COMPENSATION = 1.2F;
    private static final int DEFAULT_FALLBACK_PIXEL_SIZE = 96;
    private static final float MAX_MINIFICATION_RATIO_DEFAULT = 2.75F;
    private static final float MAX_MINIFICATION_RATIO_HIGH_SCALE = 2.4F;
    private static final float SDF_RANGE_MIN = 2.0F;
    private static final float SDF_RANGE_MAX = 4.0F;
    private static final int SMALL_ATLAS_SIZE = 512;
    private static final int DEFAULT_ATLAS_SIZE = 1024;

    private final String debugName;
    private final float baseSize;
    private final FontRenderContext fontRenderContext;

    // Metrics
    private final float ascent;
    private final float descent;
    private final float lineHeight;
    private final float lineHeightOverride;
    private final float lineHeightOffset;
    private final float yOffset;
    private final float metricsScale;
    private final float underlineOffset;
    private final float underlineThickness;
    private final float strikethroughOffset;
    private final float strikethroughThickness;

    private final FontSource[] sources;
    private final String[] sourceLabels;
    private final int[] defaultOrder;
    @Nullable
    private final Map<String, int[]> languageOrders;
    private final Int2IntOpenHashMap codepointToSourceIndex = new Int2IntOpenHashMap();
    private String cachedLanguageCode = "";
    private final int fallbackSourceIndex;

    SmoothFont(@Nonnull String debugName, @Nonnull Font baseFont, float baseSize) {
        this(debugName, List.of(baseFont), baseSize, -1.0F, 0.0F, 0.0F, null, null);
    }

    SmoothFont(@Nonnull String debugName, @Nonnull List<Font> baseFonts, float baseSize, @Nullable Map<String, int[]> languageOrders, @Nullable List<String> sourceLabels) {
        this(debugName, baseFonts, baseSize, -1.0F, 0.0F, 0.0F, languageOrders, sourceLabels);
    }

    SmoothFont(@Nonnull String debugName, @Nonnull List<Font> baseFonts, float baseSize, float lineHeightOverride, float lineHeightOffset, float yOffset, @Nullable Map<String, int[]> languageOrders, @Nullable List<String> sourceLabels) {
        this.debugName = Objects.requireNonNull(debugName);
        Objects.requireNonNull(baseFonts);
        if (baseFonts.isEmpty()) {
            throw new IllegalArgumentException("SmoothFont requires at least one base font");
        }
        this.baseSize = Math.max(1.0F, baseSize);
        this.fontRenderContext = new FontRenderContext(null, true, true);

        // Calculate metrics using the primary font at base size
        Font logicalFont = baseFonts.get(0).deriveFont(Font.PLAIN, this.baseSize);
        LineMetrics metrics = logicalFont.getLineMetrics("Hg", this.fontRenderContext);
        this.ascent = metrics.getAscent();
        this.descent = metrics.getDescent();
        float minHeight = this.ascent + this.descent;
        this.lineHeight = Math.max(1.0F, minHeight);
        this.lineHeightOverride = lineHeightOverride > 0.0F ? lineHeightOverride : -1.0F;
        this.lineHeightOffset = lineHeightOffset;
        this.yOffset = yOffset;
        this.metricsScale = (this.lineHeightOverride > 0.0F)
                ? (this.lineHeightOverride / this.lineHeight)
                : 1.0F;
        this.underlineOffset = metrics.getUnderlineOffset();
        this.underlineThickness = metrics.getUnderlineThickness();
        this.strikethroughOffset = metrics.getStrikethroughOffset();
        this.strikethroughThickness = metrics.getStrikethroughThickness();

        this.sources = new FontSource[baseFonts.size()];
        this.sourceLabels = normalizeLabels(sourceLabels, baseFonts.size());
        for (int i = 0; i < baseFonts.size(); i++) {
            Font font = Objects.requireNonNull(baseFonts.get(i));
            String sourceDebugName = this.debugName + "_f" + i;
            this.sources[i] = new FontSource(this, font, sourceDebugName, this.sourceLabels[i], i);
        }

        this.defaultOrder = new int[this.sources.length];
        for (int i = 0; i < this.sources.length; i++) {
            this.defaultOrder[i] = i;
        }
        this.languageOrders = (languageOrders == null || languageOrders.isEmpty()) ? null : languageOrders;
        this.codepointToSourceIndex.defaultReturnValue(-1);
        this.fallbackSourceIndex = resolveFallbackSourceIndex();
    }

    /**
     * Resolves the glyph generation size (in pixels) for a specific render size and scale.
     *
     * @param size logical text size
     * @param renderScale scale the text will be rendered at (GUI scale * additional pose scaling)
     * @return generation size in pixels
     */
    public int getGenerationSize(float size, float renderScale) {
        return resolveGenerationSize(size, renderScale);
    }

    public float getBaseSize() {
        return baseSize;
    }

    // Calculates the rendering scale factor between the requested size and the generated glyph size.
    float getScaleForGenerationSize(int generationSize, float size) {
        int resolvedSize = normalizeSize(generationSize);
        if (resolvedSize <= 0) {
            return 0.0F;
        }
        return size / (float) resolvedSize;
    }

    public float getLineHeight(float size) {
        float resolved = (lineHeight * metricsScale) + lineHeightOffset;
        return Math.max(1.0F, resolved) * (size / baseSize);
    }

    public float getAscent(float size) {
        return ascent * metricsScale * (size / baseSize);
    }

    public float getDescent(float size) {
        return descent * metricsScale * (size / baseSize);
    }

    public float getUnderlineOffset(float size) {
        return underlineOffset * metricsScale * (size / baseSize);
    }

    public float getUnderlineThickness(float size) {
        return Math.max(1.0F, underlineThickness * metricsScale * (size / baseSize));
    }

    public float getStrikethroughOffset(float size) {
        return strikethroughOffset * metricsScale * (size / baseSize);
    }

    public float getStrikethroughThickness(float size) {
        return Math.max(1.0F, strikethroughThickness * metricsScale * (size / baseSize));
    }

    public float getYOffset(float size) {
        return yOffset * (size / baseSize);
    }

    private static int resolveGenerationSize(float size, float renderScale) {
        float logicalSize = size;
        if (!Float.isFinite(logicalSize) || logicalSize <= 0.0F) {
            return normalizeSize(DEFAULT_FALLBACK_PIXEL_SIZE);
        }
        float scale = renderScale;
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            scale = 1.0F;
        }

        float renderPixelSize = logicalSize * scale;
        if (!Float.isFinite(renderPixelSize) || renderPixelSize <= 0.0F) {
            return normalizeSize(DEFAULT_FALLBACK_PIXEL_SIZE);
        }

        float densityBucket = resolveDensityBucket(scale);
        float qualityScale = resolveQualityScale(renderPixelSize);
        float minTextureSize = BASELINE_GLYPH_DP * densityBucket * qualityScale;
        float oversample = resolveOversampleFactor(renderPixelSize);
        float desiredSize = renderPixelSize * oversample * GLYPH_COVERAGE_COMPENSATION;

        float targetSize = Math.max(minTextureSize, desiredSize);
        float maxMinification = resolveMaxMinificationRatio(scale);
        float maxAllowedSize = renderPixelSize * maxMinification;
        if (Float.isFinite(maxAllowedSize) && maxAllowedSize > 0.0F) {
            targetSize = Math.min(targetSize, maxAllowedSize);
        }
        int resolvedSize = quantizeTextureSize(targetSize);
        return normalizeSize(resolvedSize);
    }

    private static float resolveDensityBucket(float renderScale) {
        if (renderScale <= 1.0F) {
            return 1.0F;
        }
        float bucket = (float) Math.ceil(renderScale * 2.0F) / 2.0F;
        return Math.min(4.0F, Math.max(1.0F, bucket));
    }

    private static float resolveOversampleFactor(float renderPixelSize) {
        if (renderPixelSize <= 12.0F) {
            return 2.2F;
        }
        if (renderPixelSize <= 16.0F) {
            return 2.0F;
        }
        if (renderPixelSize <= 20.0F) {
            return 1.8F;
        }
        if (renderPixelSize <= 24.0F) {
            return 1.65F;
        }
        if (renderPixelSize <= 32.0F) {
            return 1.45F;
        }
        if (renderPixelSize <= 48.0F) {
            return 1.3F;
        }
        if (renderPixelSize <= 64.0F) {
            return 1.2F;
        }
        if (renderPixelSize <= 96.0F) {
            return 1.12F;
        }
        return 1.08F;
    }

    private static float resolveQualityScale(float renderPixelSize) {
        if (renderPixelSize <= 18.0F) {
            return 1.0F;
        }
        if (renderPixelSize <= 24.0F) {
            return 1.2F;
        }
        if (renderPixelSize <= 32.0F) {
            return 1.5F;
        }
        if (renderPixelSize <= 48.0F) {
            return 1.75F;
        }
        return BASELINE_QUALITY_SCALE;
    }

    private static float resolveMaxMinificationRatio(float renderScale) {
        if (renderScale >= 3.0F) {
            return MAX_MINIFICATION_RATIO_HIGH_SCALE;
        }
        return MAX_MINIFICATION_RATIO_DEFAULT;
    }

    private static int quantizeTextureSize(float desiredSize) {
        int size = Math.max(1, (int) Math.ceil(desiredSize));
        int step = resolveQuantizationStep(size);
        return roundUpToStep(size, step);
    }

    private static int resolveQuantizationStep(int size) {
        if (size <= 24) {
            return 1;
        }
        if (size <= 64) {
            return 2;
        }
        if (size <= 160) {
            return 4;
        }
        if (size <= 320) {
            return 8;
        }
        return 16;
    }

    private static int roundUpToStep(int value, int step) {
        return ((value + step - 1) / step) * step;
    }

    private static int normalizeSize(int sizePx) {
        if (sizePx <= 0) {
            return DEFAULT_FALLBACK_PIXEL_SIZE;
        }
        return sizePx;
    }

    private static float resolveSdfRange(int sizePx) {
        int normalizedSize = normalizeSize(sizePx);
        if (normalizedSize <= 24) {
            return 3.0F;
        }
        if (normalizedSize <= 32) {
            return 2.8F;
        }
        if (normalizedSize <= 48) {
            return 2.7F;
        }
        if (normalizedSize <= 64) {
            return 2.5F;
        }
        if (normalizedSize <= 96) {
            return 2.4F;
        }
        if (normalizedSize <= 128) {
            return 2.2F;
        }
        return SDF_RANGE_MIN;
    }

    private static int resolveBlurPadding(float sdfRange, int generationSize) {
        float sigma = sdfRange / 4.0F;
        int radius = (int) Math.ceil(sigma * 3.0F);
        int padding = Math.max(1, Math.min(6, radius));
        if (sdfRange >= 2.8F && padding > 1) {
            padding -= 1;
        }
        float scale = resolvePaddingScale(generationSize);
        int reduced = Math.round(padding * scale);
        return Math.max(0, reduced);
    }

    private static float resolvePaddingScale(int sizePx) {
        int normalizedSize = normalizeSize(sizePx);
        if (normalizedSize <= 32) {
            return 0.2F;
        }
        return 0.3F;
    }

    private static int resolveInitialAtlasSize(int generationSize) {
        int normalizedSize = normalizeSize(generationSize);
        if (normalizedSize <= 32) {
            return SMALL_ATLAS_SIZE;
        }
        return DEFAULT_ATLAS_SIZE;
    }

    SmoothFontGlyph getGlyph(int generationSize, int codepoint, boolean bold, boolean italic) {
        int sourceIndex = resolveSourceIndex(codepoint);
        return sources[sourceIndex].getGlyph(generationSize, codepoint, bold, italic);
    }

    // Internal method to expose Atlas for texture binding
    SmoothFontAtlas getAtlas(int generationSize, boolean bold, boolean italic) {
        return sources[0].getAtlas(generationSize, bold, italic);
    }

    FontRenderContext getFontRenderContext() {
        return fontRenderContext;
    }

    String getDebugName() {
        return debugName;
    }

    @Override
    public void close() {
        for (FontSource source : sources) {
            source.close();
        }
    }

    private int resolveFallbackSourceIndex() {
        for (int i = 0; i < sources.length; i++) {
            if (sources[i].canDisplay('?')) {
                return i;
            }
        }
        return 0;
    }

    private int resolveSourceIndex(int codepoint) {
        if (sources.length == 1) {
            return 0;
        }
        String languageCode = resolveLanguageCode();
        if (!languageCode.equals(cachedLanguageCode)) {
            cachedLanguageCode = languageCode;
            codepointToSourceIndex.clear();
        }
        int cached = codepointToSourceIndex.get(codepoint);
        if (cached != -1) {
            return cached;
        }

        int[] order = resolveOrderForLanguage(languageCode);
        for (int index : order) {
            if (sources[index].canDisplay(codepoint)) {
                codepointToSourceIndex.put(codepoint, index);
                return index;
            }
        }
        codepointToSourceIndex.put(codepoint, fallbackSourceIndex);
        return fallbackSourceIndex;
    }

    private int[] resolveOrderForLanguage(String languageCode) {
        if (languageOrders == null || languageCode.isEmpty()) {
            return defaultOrder;
        }
        int[] order = languageOrders.get(languageCode);
        return order == null ? defaultOrder : order;
    }

    private static String resolveLanguageCode() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null || minecraft.options.languageCode == null) {
            return "";
        }
        return minecraft.options.languageCode.toLowerCase(Locale.ROOT);
    }

    private static String[] normalizeLabels(@Nullable List<String> labels, int size) {
        String[] resolved = new String[size];
        for (int i = 0; i < size; i++) {
            String label = null;
            if (labels != null && i < labels.size()) {
                label = labels.get(i);
            }
            if (label == null || label.isBlank()) {
                label = "unknown";
            }
            resolved[i] = label;
        }
        return resolved;
    }

    private static class FontSource implements AutoCloseable {
        final SmoothFont parent;
        final Font rawFont;
        final String debugName;
        final String sourceLabel;
        final int sourceIndex;
        final Int2ObjectOpenHashMap<SizeLevel> sizeLevels;

        FontSource(SmoothFont parent, Font rawFont, String debugName, String sourceLabel, int sourceIndex) {
            this.parent = parent;
            this.rawFont = rawFont;
            this.debugName = debugName;
            this.sourceLabel = sourceLabel;
            this.sourceIndex = sourceIndex;
            this.sizeLevels = new Int2ObjectOpenHashMap<>();
        }

        boolean canDisplay(int codepoint) {
            return rawFont.canDisplay(codepoint);
        }

        SmoothFontGlyph getGlyph(int generationSize, int codepoint, boolean bold, boolean italic) {
            return getSizeLevel(generationSize).getAtlas(bold, italic).getGlyph(codepoint);
        }

        SmoothFontAtlas getAtlas(int generationSize, boolean bold, boolean italic) {
            return getSizeLevel(generationSize).getAtlas(bold, italic);
        }

        @Override
        public void close() {
            for (SizeLevel level : sizeLevels.values()) {
                level.close();
            }
            sizeLevels.clear();
        }

        private SizeLevel getSizeLevel(int generationSize) {
            int resolvedSize = normalizeSize(generationSize);
            SizeLevel level = sizeLevels.get(resolvedSize);
            if (level != null) {
                return level;
            }
            synchronized (this) {
                level = sizeLevels.get(resolvedSize);
                if (level == null) {
                    level = createSizeLevel(resolvedSize);
                    sizeLevels.put(resolvedSize, level);
                }
                return level;
            }
        }

        private SizeLevel createSizeLevel(int generationSize) {
            float sdfRange = resolveSdfRange(generationSize);
            int padding = resolveBlurPadding(sdfRange, generationSize);
            int initialAtlasSize = resolveInitialAtlasSize(generationSize);
            String sizeLabel = generationSize + "px";
            return new SizeLevel(parent, rawFont, generationSize, sdfRange, padding, initialAtlasSize, debugName, sizeLabel, sourceLabel, sourceIndex);
        }
    }

    private static class SizeLevel implements AutoCloseable {
        final int generationSize;
        private final SmoothFont parent;
        private final Font rawFont;
        private final float sdfRange;
        private final int padding;
        private final int initialAtlasSize;
        private final String debugName;
        private final String sizeLabel;
        private final String sourceLabel;
        private final int sourceIndex;
        private SmoothFontAtlas plainAtlas;
        private SmoothFontAtlas boldAtlas;
        private SmoothFontAtlas italicAtlas;
        private SmoothFontAtlas boldItalicAtlas;

        SizeLevel(SmoothFont parent, Font rawFont, int genSize, float sdfRange, int padding, int initialAtlasSize, String debugName, String sizeLabel, String sourceLabel, int sourceIndex) {
            this.generationSize = genSize;
            this.parent = parent;
            this.rawFont = rawFont;
            this.initialAtlasSize = Math.max(1, initialAtlasSize);
            this.debugName = debugName;
            this.sizeLabel = sizeLabel;
            this.sourceLabel = sourceLabel;
            this.sourceIndex = sourceIndex;
            this.sdfRange = Math.max(0.5F, Math.min(SDF_RANGE_MAX, sdfRange));
            this.padding = Math.max(1, padding);
        }

        SmoothFontAtlas getAtlas(boolean bold, boolean italic) {
            if (bold && italic) {
                if (boldItalicAtlas != null) {
                    return boldItalicAtlas;
                }
                synchronized (this) {
                    if (boldItalicAtlas == null) {
                        boldItalicAtlas = createAtlas(Font.BOLD | Font.ITALIC, "_bold_italic", "bold_italic");
                    }
                    return boldItalicAtlas;
                }
            }
            if (bold) {
                if (boldAtlas != null) {
                    return boldAtlas;
                }
                synchronized (this) {
                    if (boldAtlas == null) {
                        boldAtlas = createAtlas(Font.BOLD, "_bold", "bold");
                    }
                    return boldAtlas;
                }
            }
            if (italic) {
                if (italicAtlas != null) {
                    return italicAtlas;
                }
                synchronized (this) {
                    if (italicAtlas == null) {
                        italicAtlas = createAtlas(Font.ITALIC, "_italic", "italic");
                    }
                    return italicAtlas;
                }
            }
            if (plainAtlas != null) {
                return plainAtlas;
            }
            synchronized (this) {
                if (plainAtlas == null) {
                    plainAtlas = createAtlas(Font.PLAIN, "_plain", "plain");
                }
                return plainAtlas;
            }
        }

        private SmoothFontAtlas createAtlas(int style, String styleSuffix, String styleLabel) {
            Font derived = rawFont.deriveFont(style, (float) generationSize);
            String atlasName = debugName + "_" + generationSize + styleSuffix;
            return new SmoothFontAtlas(parent, derived, parent.getFontRenderContext(), sdfRange, padding, atlasName, initialAtlasSize, sourceLabel, sourceIndex, sizeLabel, styleLabel);
        }

        @Override
        public void close() {
            if (plainAtlas != null) {
                plainAtlas.close();
            }
            if (boldAtlas != null) {
                boldAtlas.close();
            }
            if (italicAtlas != null) {
                italicAtlas.close();
            }
            if (boldItalicAtlas != null) {
                boldItalicAtlas.close();
            }
        }
    }
}
