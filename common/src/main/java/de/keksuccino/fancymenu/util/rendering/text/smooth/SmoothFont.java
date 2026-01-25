package de.keksuccino.fancymenu.util.rendering.text.smooth;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger LOGGER = LogManager.getLogger();

    private static final float LOD_TINY_LIMIT = 20.0F;
    private static final float LOD_SMALL_LIMIT = 40.0F;
    private static final float LOD_MEDIUM_LIMIT = 72.0F;

    private final String debugName;
    private final float baseSize;
    private final float sdfRange;
    private final FontRenderContext fontRenderContext;
    private final float[] lodGenerationSizes;

    // Metrics
    private final float ascent;
    private final float descent;
    private final float lineHeight;
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

    SmoothFont(@Nonnull String debugName, @Nonnull Font baseFont, float baseSize, float sdfRange) {
        this(debugName, List.of(baseFont), baseSize, sdfRange, 1.0F, null, null);
    }

    SmoothFont(@Nonnull String debugName, @Nonnull List<Font> baseFonts, float baseSize, float sdfRange, @Nullable Map<String, int[]> languageOrders, @Nullable List<String> sourceLabels) {
        this(debugName, baseFonts, baseSize, sdfRange, 1.0F, languageOrders, sourceLabels);
    }

    SmoothFont(@Nonnull String debugName, @Nonnull List<Font> baseFonts, float baseSize, float sdfRange, float lineHeightMultiplier, @Nullable Map<String, int[]> languageOrders, @Nullable List<String> sourceLabels) {
        this.debugName = Objects.requireNonNull(debugName);
        Objects.requireNonNull(baseFonts);
        if (baseFonts.isEmpty()) {
            throw new IllegalArgumentException("SmoothFont requires at least one base font");
        }
        this.baseSize = Math.max(1.0F, baseSize);
        this.sdfRange = Math.max(1.0F, sdfRange);
        this.fontRenderContext = new FontRenderContext(null, true, true);

        // Calculate metrics using the primary font at base size
        Font logicalFont = baseFonts.get(0).deriveFont(Font.PLAIN, this.baseSize);
        LineMetrics metrics = logicalFont.getLineMetrics("Hg", this.fontRenderContext);
        this.ascent = metrics.getAscent();
        this.descent = metrics.getDescent();
        float resolvedLineHeightMultiplier = (lineHeightMultiplier > 0.0F) ? lineHeightMultiplier : 1.0F;
        this.lineHeight = metrics.getHeight() * resolvedLineHeightMultiplier;
        this.underlineOffset = metrics.getUnderlineOffset();
        this.underlineThickness = metrics.getUnderlineThickness();
        this.strikethroughOffset = metrics.getStrikethroughOffset();
        this.strikethroughThickness = metrics.getStrikethroughThickness();

        this.lodGenerationSizes = new float[] {this.baseSize * 1.0F, this.baseSize * 2.0F, this.baseSize * 4.0F, this.baseSize * 6.0F, this.baseSize * 8.0F};
        this.sources = new FontSource[baseFonts.size()];
        this.sourceLabels = normalizeLabels(sourceLabels, baseFonts.size());
        for (int i = 0; i < baseFonts.size(); i++) {
            Font font = Objects.requireNonNull(baseFonts.get(i));
            String sourceDebugName = this.debugName + "_f" + i;
            this.sources[i] = new FontSource(this, font, this.baseSize, sourceDebugName, this.sourceLabels[i], i);
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
     * Selects the appropriate LOD level based on effective screen size.
     *
     * @param size logical text size
     * @param renderScale current render scale (GUI scale * any additional pose scaling)
     * @return LOD index (0 = Tiny, 1 = Small, 2 = Medium, 3 = Large)
     */
    public int getLodLevel(float size, float renderScale) {
        int lod = this._getLodLevel(size, renderScale);
        float renderSize = size * renderScale;
        LOGGER.info("############### LOD LEVEL: " + lod + " | SIZE: " + size + " | RENDER_SCALE: " + renderScale + " | RENDER_SIZE: " + renderSize + " | ACTUAL WINDOW SCALE: " + Minecraft.getInstance().getWindow().getGuiScale() + " | FANCYMENU UI SCALE: " + UIBase.getUIScale() + " | FANCYMENU UI RENDER SCALE: " + UIBase.getFixedUIScale());
        return lod;
    }

    private int _getLodLevel(float size, float renderScale) {
        float renderSize = size * renderScale;
        if (renderSize <= LOD_TINY_LIMIT) return 0;
        if (renderSize <= LOD_SMALL_LIMIT) return 1;
        if (renderSize <= LOD_MEDIUM_LIMIT) return 2;
        return 3;
    }

    public float getBaseSize() {
        return baseSize;
    }

    // Calculates the rendering scale factor for a specific LOD level.
    // This bridges the gap between the requested size and the huge internal texture.
    float getScaleForLod(int lodIndex, float size) {
        float genSize = lodGenerationSizes[lodIndex];
        return size / genSize;
    }

    public float getLineHeight(float size) {
        return lineHeight * (size / baseSize);
    }

    public float getAscent(float size) {
        return ascent * (size / baseSize);
    }

    public float getDescent(float size) {
        return descent * (size / baseSize);
    }

    public float getUnderlineOffset(float size) {
        return underlineOffset * (size / baseSize);
    }

    public float getUnderlineThickness(float size) {
        return Math.max(1.0F, underlineThickness * (size / baseSize));
    }

    public float getStrikethroughOffset(float size) {
        return strikethroughOffset * (size / baseSize);
    }

    public float getStrikethroughThickness(float size) {
        return Math.max(1.0F, strikethroughThickness * (size / baseSize));
    }

    SmoothFontGlyph getGlyph(int lodIndex, int codepoint, boolean bold, boolean italic) {
        int sourceIndex = resolveSourceIndex(codepoint);
        return sources[sourceIndex].getGlyph(lodIndex, codepoint, bold, italic);
    }

    // Internal method to expose Atlas for texture binding
    SmoothFontAtlas getAtlas(int lodIndex, boolean bold, boolean italic) {
        return sources[0].getAtlas(lodIndex, bold, italic);
    }

    float getSdfRange() {
        return sdfRange;
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
        final float baseSize;
        final String debugName;
        final String sourceLabel;
        final int sourceIndex;
        final LodLevel[] lodLevels;

        FontSource(SmoothFont parent, Font rawFont, float baseSize, String debugName, String sourceLabel, int sourceIndex) {
            this.parent = parent;
            this.rawFont = rawFont;
            this.baseSize = baseSize;
            this.debugName = debugName;
            this.sourceLabel = sourceLabel;
            this.sourceIndex = sourceIndex;
            this.lodLevels = new LodLevel[5];
        }

        boolean canDisplay(int codepoint) {
            return rawFont.canDisplay(codepoint);
        }

        SmoothFontGlyph getGlyph(int lodIndex, int codepoint, boolean bold, boolean italic) {
            return getLodLevel(lodIndex).getAtlas(bold, italic).getGlyph(codepoint);
        }

        SmoothFontAtlas getAtlas(int lodIndex, boolean bold, boolean italic) {
            return getLodLevel(lodIndex).getAtlas(bold, italic);
        }

        @Override
        public void close() {
            for (LodLevel lod : lodLevels) {
                if (lod != null) {
                    lod.close();
                }
            }
        }

        private LodLevel getLodLevel(int lodIndex) {
            LodLevel lod = lodLevels[lodIndex];
            if (lod != null) {
                return lod;
            }
            synchronized (this) {
                lod = lodLevels[lodIndex];
                if (lod == null) {
                    lod = createLodLevel(lodIndex);
                    lodLevels[lodIndex] = lod;
                }
                return lod;
            }
        }

        private LodLevel createLodLevel(int lodIndex) {
            return switch (lodIndex) {
                // LOD 0: Tiny (2x scale, starts with 512px atlas)
                case 0 -> new LodLevel(parent, rawFont, baseSize * 2.0F, 512, debugName, "_tiny", "tiny", sourceLabel, sourceIndex);
                // LOD 1: Small (4x scale, starts with 1024px atlas)
                case 1 -> new LodLevel(parent, rawFont, baseSize * 4.0F, 1024, debugName, "_small", "small", sourceLabel, sourceIndex);
                // LOD 2: Medium (6x scale, starts with 1024px atlas)
                case 2 -> new LodLevel(parent, rawFont, baseSize * 6.0F, 1024, debugName, "_medium", "medium", sourceLabel, sourceIndex);
                // LOD 3: Large (8x scale, starts with 1024px atlas)
                case 3 -> new LodLevel(parent, rawFont, baseSize * 8.0F, 1024, debugName, "_large", "large", sourceLabel, sourceIndex);
                default -> throw new IllegalArgumentException("Invalid LOD index: " + lodIndex);
            };
        }
    }

    private static class LodLevel implements AutoCloseable {
        final float generationSize;
        private final SmoothFont parent;
        private final Font rawFont;
        private final float lodSdfRange;
        private final int initialAtlasSize;
        private final String debugName;
        private final String suffix;
        private final String lodLabel;
        private final String sourceLabel;
        private final int sourceIndex;
        private SmoothFontAtlas plainAtlas;
        private SmoothFontAtlas boldAtlas;
        private SmoothFontAtlas italicAtlas;
        private SmoothFontAtlas boldItalicAtlas;

        LodLevel(SmoothFont parent, Font rawFont, float genSize, int initialAtlasSize, String debugName, String suffix, String lodLabel, String sourceLabel, int sourceIndex) {
            this.generationSize = genSize;
            this.parent = parent;
            this.rawFont = rawFont;
            this.initialAtlasSize = Math.max(1, initialAtlasSize);
            this.debugName = debugName;
            this.suffix = suffix;
            this.lodLabel = lodLabel;
            this.sourceLabel = sourceLabel;
            this.sourceIndex = sourceIndex;

            // Scale SDF range based on LOD size.
            // This ensures we have a consistent "relative" softness across all LODs.
            float scale = genSize / parent.getBaseSize();
            this.lodSdfRange = Math.max(1.0F, parent.getSdfRange() * scale);
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
            Font derived = rawFont.deriveFont(style, generationSize);
            return new SmoothFontAtlas(parent, derived, parent.getFontRenderContext(), lodSdfRange, debugName + suffix + styleSuffix, initialAtlasSize, sourceLabel, sourceIndex, lodLabel, styleLabel);
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
