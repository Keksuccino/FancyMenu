package de.keksuccino.fancymenu.util.rendering.text.smooth;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
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

    // Thresholds for switching LODs (in pixel size)
    // <= 18px: Use Small LOD (2x gen)
    // <= 48px: Use Medium LOD (4x gen)
    // > 48px:  Use Large LOD (8x gen)
    private static final float LOD_SMALL_LIMIT = 18.0F;
    private static final float LOD_MEDIUM_LIMIT = 48.0F;

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
    private final int[] defaultOrder;
    @Nullable
    private final Map<String, int[]> languageOrders;
    private final Int2IntOpenHashMap codepointToSourceIndex = new Int2IntOpenHashMap();
    private String cachedLanguageCode = "";
    private final int fallbackSourceIndex;

    SmoothFont(@Nonnull String debugName, @Nonnull Font baseFont, float baseSize, float sdfRange) {
        this(debugName, List.of(baseFont), baseSize, sdfRange, null);
    }

    SmoothFont(@Nonnull String debugName, @Nonnull List<Font> baseFonts, float baseSize, float sdfRange, @Nullable Map<String, int[]> languageOrders) {
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
        this.lineHeight = metrics.getHeight();
        this.underlineOffset = metrics.getUnderlineOffset();
        this.underlineThickness = metrics.getUnderlineThickness();
        this.strikethroughOffset = metrics.getStrikethroughOffset();
        this.strikethroughThickness = metrics.getStrikethroughThickness();

        this.lodGenerationSizes = new float[] {this.baseSize * 2.0F, this.baseSize * 4.0F, this.baseSize * 8.0F};
        this.sources = new FontSource[baseFonts.size()];
        for (int i = 0; i < baseFonts.size(); i++) {
            Font font = Objects.requireNonNull(baseFonts.get(i));
            String sourceDebugName = this.debugName + "_f" + i;
            this.sources[i] = new FontSource(this, font, this.baseSize, sourceDebugName);
        }

        this.defaultOrder = new int[this.sources.length];
        for (int i = 0; i < this.sources.length; i++) {
            this.defaultOrder[i] = i;
        }
        this.languageOrders = (languageOrders == null || languageOrders.isEmpty()) ? null : languageOrders;
        this.codepointToSourceIndex.defaultReturnValue(-1);
        this.fallbackSourceIndex = resolveFallbackSourceIndex();
    }

    public int getLodLevel(float size) {
        if (size <= LOD_SMALL_LIMIT) return 0;
        if (size <= LOD_MEDIUM_LIMIT) return 1;
        return 2;
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

    private static class FontSource implements AutoCloseable {
        final Font rawFont;
        final LodLevel[] lodLevels;

        FontSource(SmoothFont parent, Font rawFont, float baseSize, String debugName) {
            this.rawFont = rawFont;
            this.lodLevels = new LodLevel[3];

            // LOD 0: Small (2x scale, starts with 512px atlas)
            this.lodLevels[0] = new LodLevel(parent, rawFont, baseSize * 2.0F, 512, debugName, "_small");

            // LOD 1: Medium (4x scale, starts with 1024px atlas)
            this.lodLevels[1] = new LodLevel(parent, rawFont, baseSize * 4.0F, 1024, debugName, "_medium");

            // LOD 2: Large (8x scale, starts with 2048px atlas)
            this.lodLevels[2] = new LodLevel(parent, rawFont, baseSize * 8.0F, 2048, debugName, "_large");
        }

        boolean canDisplay(int codepoint) {
            return rawFont.canDisplay(codepoint);
        }

        SmoothFontGlyph getGlyph(int lodIndex, int codepoint, boolean bold, boolean italic) {
            return lodLevels[lodIndex].getAtlas(bold, italic).getGlyph(codepoint);
        }

        SmoothFontAtlas getAtlas(int lodIndex, boolean bold, boolean italic) {
            return lodLevels[lodIndex].getAtlas(bold, italic);
        }

        @Override
        public void close() {
            for (LodLevel lod : lodLevels) {
                lod.close();
            }
        }
    }

    private static class LodLevel implements AutoCloseable {
        final float generationSize;
        final SmoothFontAtlas plainAtlas;
        final SmoothFontAtlas boldAtlas;
        final SmoothFontAtlas italicAtlas;
        final SmoothFontAtlas boldItalicAtlas;

        LodLevel(SmoothFont parent, Font rawFont, float genSize, int initialAtlasSize, String debugName, String suffix) {
            this.generationSize = genSize;

            Font plain = rawFont.deriveFont(Font.PLAIN, genSize);
            Font bold = rawFont.deriveFont(Font.BOLD, genSize);
            Font italic = rawFont.deriveFont(Font.ITALIC, genSize);
            Font boldItalic = rawFont.deriveFont(Font.BOLD | Font.ITALIC, genSize);

            this.plainAtlas = new SmoothFontAtlas(parent, plain, parent.getFontRenderContext(), parent.getSdfRange(), debugName + suffix + "_plain", initialAtlasSize);
            this.boldAtlas = new SmoothFontAtlas(parent, bold, parent.getFontRenderContext(), parent.getSdfRange(), debugName + suffix + "_bold", initialAtlasSize);
            this.italicAtlas = new SmoothFontAtlas(parent, italic, parent.getFontRenderContext(), parent.getSdfRange(), debugName + suffix + "_italic", initialAtlasSize);
            this.boldItalicAtlas = new SmoothFontAtlas(parent, boldItalic, parent.getFontRenderContext(), parent.getSdfRange(), debugName + suffix + "_bold_italic", initialAtlasSize);
        }

        SmoothFontAtlas getAtlas(boolean bold, boolean italic) {
            if (bold && italic) return boldItalicAtlas;
            if (bold) return boldAtlas;
            if (italic) return italicAtlas;
            return plainAtlas;
        }

        @Override
        public void close() {
            plainAtlas.close();
            boldAtlas.close();
            italicAtlas.close();
            boldItalicAtlas.close();
        }
    }
}
