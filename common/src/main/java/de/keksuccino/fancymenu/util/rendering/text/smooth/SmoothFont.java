package de.keksuccino.fancymenu.util.rendering.text.smooth;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Font;
import java.awt.Shape;
import java.awt.font.LineMetrics;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A family of font files that get used together: the first file is the primary font (it defines the metrics), the others are
 * fallbacks for characters the primary font can't draw. Fallback files only get loaded when some text needs them.
 * <p>
 * All metrics are defined at the {@linkplain #getBaseSize() base size} and scale linearly with the requested text size.
 * Instances must only be used on the render thread.
 * </p>
 */
public final class SmoothFont implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    static final int SOURCE_INDEX_MASK = 0xFF;
    static final int SOURCE_MISSING = 1 << 8;
    static final int MISSING_GLYPH_CODEPOINT = '?';

    private static final int LAYOUT_CACHE_LIMIT = 1024;
    /** Precision (in layout units) of the curve flattening that measures the ink bounds of icon glyphs. */
    private static final double ICON_BOUNDS_FLATNESS = 0.01;
    private static final IconGlyph MISSING_ICON = new IconGlyph(0, 0, null, 0.0F, 0.0F, 0.0F, 0.0F);

    private final String debugName;
    private final float baseSize;

    // Metrics at base size
    private final float ascent;
    private final float descent;
    private final float lineHeight;
    private final float lineHeightOffset;
    private final float yOffset;
    private final float metricsScale;
    private final float underlineOffset;
    private final float underlineThickness;
    private final float strikethroughOffset;
    private final float strikethroughThickness;
    /** Cap height of the primary font in em, used to center color glyphs vertically. */
    private final float capHeightEm;
    /** Height of the lowercase letters of the primary font that have no ascender, in em. */
    private final float xHeightEm;
    private final float colorGlyphScale;

    private final SmoothFontSource[] sources;
    private final int[] defaultOrder;
    @Nullable
    private final Map<String, int[]> languageOrders;
    private final Supplier<String> languageCode;
    private String activeLanguage;
    private int[] textOrder;
    private int[] emojiOrder;
    private final Int2IntOpenHashMap textSourceCache = new Int2IntOpenHashMap();
    private final Int2IntOpenHashMap emojiSourceCache = new Int2IntOpenHashMap();
    private final int fallbackSourceIndex;

    private final SmoothGlyphAtlas atlas;
    private final Int2ObjectOpenHashMap<IconGlyph> iconGlyphs = new Int2ObjectOpenHashMap<>();
    private boolean closed;
    private final SmoothStyledText legacyScratch = new SmoothStyledText(64);
    private final LinkedHashMap<SmoothStyledText, SmoothTextLayout> styledLayouts = createLayoutCache();
    private final LinkedHashMap<String, SmoothTextLayout> singleLineLegacyLayouts = createLayoutCache();
    private final LinkedHashMap<String, SmoothTextLayout> multilineLegacyLayouts = createLayoutCache();

    /**
     * @param sources font files; the first one must be loadable, because it defines the metrics
     * @param languageOrders per language code, the order in which the files get searched for a character
     * @param colorGlyphScale size of color (emoji) glyphs relative to the text size
     * @param languageCode supplies the active language code, to pick the matching entry of {@code languageOrders}
     * @param atlasPageSize edge length of the glyph atlas pages in pixels
     * @throws IllegalStateException when the primary font file can't be loaded
     */
    SmoothFont(@NotNull String debugName, @NotNull List<SmoothFontSource> sources, float baseSize, float lineHeightOverride, float lineHeightOffset, float yOffset, @Nullable Map<String, int[]> languageOrders, float colorGlyphScale, @NotNull Supplier<String> languageCode, int atlasPageSize) {
        this.debugName = Objects.requireNonNull(debugName);
        if (sources.isEmpty() || sources.size() > SOURCE_INDEX_MASK) {
            throw new IllegalArgumentException("SmoothFont needs between 1 and " + SOURCE_INDEX_MASK + " font files");
        }
        this.sources = sources.toArray(new SmoothFontSource[0]);
        SmoothFontSource.Loaded primary = this.sources[0].get();
        if (primary == null) {
            throw new IllegalStateException("The primary font file of '" + debugName + "' could not be loaded");
        }
        this.baseSize = Math.max(1.0F, baseSize);

        Font logicalFont = primary.font.deriveFont(Font.PLAIN, this.baseSize);
        LineMetrics metrics = logicalFont.getLineMetrics("Hg", SmoothFontSource.RENDER_CONTEXT);
        this.ascent = metrics.getAscent();
        this.descent = metrics.getDescent();
        this.lineHeight = Math.max(1.0F, this.ascent + this.descent);
        float resolvedOverride = (lineHeightOverride > 0.0F) ? lineHeightOverride : -1.0F;
        this.lineHeightOffset = lineHeightOffset;
        this.yOffset = yOffset;
        this.metricsScale = (resolvedOverride > 0.0F) ? (resolvedOverride / this.lineHeight) : 1.0F;
        this.underlineOffset = metrics.getUnderlineOffset();
        this.underlineThickness = metrics.getUnderlineThickness();
        this.strikethroughOffset = metrics.getStrikethroughOffset();
        this.strikethroughThickness = metrics.getStrikethroughThickness();
        Rectangle2D capBounds = primary.layoutFont(0).createGlyphVector(SmoothFontSource.RENDER_CONTEXT, "H").getVisualBounds();
        this.capHeightEm = (float) capBounds.getHeight() / SmoothFontSource.LAYOUT_SIZE;
        Rectangle2D xBounds = primary.layoutFont(0).createGlyphVector(SmoothFontSource.RENDER_CONTEXT, "x").getVisualBounds();
        this.xHeightEm = (float) xBounds.getHeight() / SmoothFontSource.LAYOUT_SIZE;
        this.colorGlyphScale = (colorGlyphScale > 0.0F) ? colorGlyphScale : 1.0F;

        this.defaultOrder = new int[this.sources.length];
        for (int i = 0; i < this.sources.length; i++) {
            this.defaultOrder[i] = i;
        }
        this.languageOrders = (languageOrders == null || languageOrders.isEmpty()) ? null : languageOrders;
        this.languageCode = languageCode;
        this.textSourceCache.defaultReturnValue(-1);
        this.emojiSourceCache.defaultReturnValue(-1);
        this.refreshLanguage();
        this.fallbackSourceIndex = this.findFallbackSource();
        this.atlas = new SmoothGlyphAtlas(this.debugName, atlasPageSize);
    }

    public float getBaseSize() {
        return this.baseSize;
    }

    public float getLineHeight(float size) {
        float resolved = (this.lineHeight * this.metricsScale) + this.lineHeightOffset;
        return Math.max(1.0F, resolved) * (size / this.baseSize);
    }

    public float getAscent(float size) {
        return this.ascent * this.metricsScale * (size / this.baseSize);
    }

    public float getDescent(float size) {
        return this.descent * this.metricsScale * (size / this.baseSize);
    }

    public float getUnderlineOffset(float size) {
        return this.underlineOffset * this.metricsScale * (size / this.baseSize);
    }

    public float getUnderlineThickness(float size) {
        return Math.max(1.0F, this.underlineThickness * this.metricsScale * (size / this.baseSize));
    }

    public float getStrikethroughOffset(float size) {
        return this.strikethroughOffset * this.metricsScale * (size / this.baseSize);
    }

    public float getStrikethroughThickness(float size) {
        return Math.max(1.0F, this.strikethroughThickness * this.metricsScale * (size / this.baseSize));
    }

    public float getYOffset(float size) {
        return this.yOffset * (size / this.baseSize);
    }

    float capHeightEm() {
        return this.capHeightEm;
    }

    /**
     * Returns the height of lowercase letters without an ascender, like "x". Things that belong to a line of text,
     * like the marker of a list item, look centered when they sit in the middle of that height above the baseline.
     */
    public float getXHeight(float size) {
        return this.xHeightEm * size;
    }

    float colorGlyphScale() {
        return this.colorGlyphScale;
    }

    @NotNull
    SmoothFontSource source(int index) {
        return this.sources[index];
    }

    @NotNull
    SmoothGlyphAtlas atlas() {
        return this.atlas;
    }

    String getDebugName() {
        return this.debugName;
    }

    /**
     * Returns the layout of a legacy formatted string, shaping it on the first request.
     */
    @NotNull
    SmoothTextLayout layoutLegacy(@NotNull String text, boolean multiline) {
        LinkedHashMap<String, SmoothTextLayout> cache = multiline ? this.multilineLegacyLayouts : this.singleLineLegacyLayouts;
        SmoothTextLayout layout = cache.get(text);
        if (layout == null) {
            this.refreshLanguage();
            layout = SmoothTextShaper.shape(this, this.legacyScratch.reset(multiline).appendLegacy(text));
            // A non-empty string that only holds formatting codes still takes up one empty line, so code that stacks lines
            // by their measured heights keeps those lines.
            if (layout.lineCount == 0 && !text.isEmpty()) {
                layout = SmoothTextLayout.EMPTY_LINE;
            }
            cache.put(text, layout);
        }
        return layout;
    }

    /**
     * Returns the layout of a styled text, shaping it on the first request. The given text may be a reused scratch instance.
     */
    @NotNull
    SmoothTextLayout layout(@NotNull SmoothStyledText text) {
        SmoothTextLayout layout = this.styledLayouts.get(text);
        if (layout == null) {
            this.refreshLanguage();
            layout = SmoothTextShaper.shape(this, text);
            this.styledLayouts.put(text.copy(), layout);
        }
        return layout;
    }

    /**
     * Returns the glyph that draws a character as an icon, or {@code null} when no file of this font has a glyph for it.
     * Results get cached, so this is cheap after the first request for a character.
     */
    @Nullable
    IconGlyph iconGlyph(int codepoint) {
        IconGlyph glyph = this.iconGlyphs.get(codepoint);
        if (glyph == null) {
            glyph = this.loadIconGlyph(codepoint);
            this.iconGlyphs.put(codepoint, glyph);
        }
        return (glyph == MISSING_ICON) ? null : glyph;
    }

    @NotNull
    private IconGlyph loadIconGlyph(int codepoint) {
        int resolved = this.resolveSource(codepoint, false);
        SmoothFontSource.Loaded source = this.sources[resolved & SOURCE_INDEX_MASK].get();
        if ((resolved & SOURCE_MISSING) != 0 || source == null) {
            LOGGER.error("[FANCYMENU] Smooth font '{}' has no glyph for icon U+{}.", this.debugName, Integer.toHexString(codepoint).toUpperCase(Locale.ROOT));
            return MISSING_ICON;
        }
        int glyphId = source.layoutFont(0).createGlyphVector(SmoothFontSource.RENDER_CONTEXT, Character.toChars(codepoint)).getGlyphCode(0);
        Shape outline = source.layoutFont(0).createGlyphVector(SmoothFontSource.RENDER_CONTEXT, new int[]{glyphId}).getGlyphOutline(0);
        float[] bounds = inkBounds(outline);
        if (bounds == null) {
            return MISSING_ICON;
        }
        float layoutSize = SmoothFontSource.LAYOUT_SIZE;
        return new IconGlyph(resolved & SOURCE_INDEX_MASK, glyphId, outline, bounds[0] / layoutSize, bounds[1] / layoutSize, bounds[2] / layoutSize, bounds[3] / layoutSize);
    }

    /**
     * Measures the tight bounds of an outline ({@code minX, minY, maxX, maxY}), or returns {@code null} for an empty outline.
     * {@link Shape#getBounds2D()} isn't used, because older Java versions include the control points of curves in it.
     */
    @Nullable
    static float[] inkBounds(@NotNull Shape outline) {
        double[] coords = new double[6];
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (PathIterator iterator = outline.getPathIterator(null, ICON_BOUNDS_FLATNESS); !iterator.isDone(); iterator.next()) {
            if (iterator.currentSegment(coords) == PathIterator.SEG_CLOSE) {
                continue;
            }
            minX = Math.min(minX, coords[0]);
            minY = Math.min(minY, coords[1]);
            maxX = Math.max(maxX, coords[0]);
            maxY = Math.max(maxY, coords[1]);
        }
        if (!(maxX > minX) || !(maxY > minY)) {
            return null;
        }
        return new float[]{(float) minX, (float) minY, (float) maxX, (float) maxY};
    }

    void clearLayouts() {
        this.styledLayouts.clear();
        this.singleLineLegacyLayouts.clear();
        this.multilineLegacyLayouts.clear();
    }

    /**
     * Finds the font file that draws a character, loading fallback files on demand.
     *
     * @param emoji whether the character starts an emoji cluster; emoji prefer the emoji fallback files, everything else avoids them
     * @return the source index, combined with {@link #SOURCE_MISSING} when no file can draw the character
     */
    int resolveSource(int codepoint, boolean emoji) {
        Int2IntOpenHashMap cache = emoji ? this.emojiSourceCache : this.textSourceCache;
        int cached = cache.get(codepoint);
        if (cached != -1) {
            return cached;
        }
        int resolved = this.searchSource(codepoint, emoji ? this.emojiOrder : this.textOrder, !emoji);
        cache.put(codepoint, resolved);
        return resolved;
    }

    /**
     * Like {@link #resolveSource(int, boolean)} for text presentation, used when a color font can't draw an emoji sequence.
     */
    int resolveTextSource(int codepoint) {
        return this.resolveSource(codepoint, false);
    }

    /**
     * @param avoidColor whether a file that draws the character as a color glyph should only be used when no other file can draw
     *                   it, so characters in text presentation (like U+2764 with U+FE0E) stay monochrome when possible
     */
    private int searchSource(int codepoint, @NotNull int[] order, boolean avoidColor) {
        if (this.sources.length == 1) {
            return this.sources[0].canDisplay(codepoint) ? 0 : (this.fallbackSourceIndex | SOURCE_MISSING);
        }
        int firstColorSource = -1;
        for (int index : order) {
            SmoothFontSource source = this.sources[index];
            if (!source.canDisplay(codepoint)) {
                continue;
            }
            if (avoidColor && source.displaysAsColor(codepoint)) {
                if (firstColorSource < 0) {
                    firstColorSource = index;
                }
                continue;
            }
            return index;
        }
        return (firstColorSource >= 0) ? firstColorSource : (this.fallbackSourceIndex | SOURCE_MISSING);
    }

    private int findFallbackSource() {
        for (int index : this.textOrder) {
            if (this.sources[index].canDisplay(MISSING_GLYPH_CODEPOINT)) {
                return index;
            }
        }
        return 0;
    }

    /**
     * Picks the search orders of the active language. A language change resets everything that depends on them.
     */
    private void refreshLanguage() {
        String language = this.languageCode.get();
        if (language.equals(this.activeLanguage)) {
            return;
        }
        this.activeLanguage = language;
        int[] order = (this.languageOrders != null) ? this.languageOrders.getOrDefault(language, this.defaultOrder) : this.defaultOrder;
        this.textOrder = this.partitionOrder(order, false);
        this.emojiOrder = this.partitionOrder(order, true);
        this.textSourceCache.clear();
        this.emojiSourceCache.clear();
        this.clearLayouts();
        // The language's preferred fallback (for example the Japanese font for ja_jp) is needed right away, so it loads ahead of time.
        if (this.languageOrders != null && this.languageOrders.containsKey(language) && order.length > 1) {
            SmoothFontSource preferred = this.sources[order[1]];
            if (!preferred.emojiFallback) {
                preferred.preloadAsync();
            }
        }
    }

    @NotNull
    private int[] partitionOrder(@NotNull int[] order, boolean emojiFirst) {
        int[] result = new int[order.length];
        int count = 0;
        for (int pass = 0; pass < 2; pass++) {
            boolean wantEmoji = (pass == 0) == emojiFirst;
            for (int index : order) {
                if (this.sources[index].emojiFallback == wantEmoji) {
                    result[count++] = index;
                }
            }
        }
        return result;
    }

    /**
     * Whether this font got closed, which happens to all cached fonts on resource reloads. Closed fonts can't draw anymore,
     * so holders of a font should build it again.
     */
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
        this.atlas.close();
        this.clearLayouts();
        this.iconGlyphs.clear();
    }

    /**
     * A glyph drawn as an icon: its outline at {@link SmoothFontSource#LAYOUT_SIZE} and the tight bounds of its ink in em
     * (y grows down, relative to the pen position on the baseline). Icons get scaled to fit their ink box into the target area.
     */
    record IconGlyph(int source, int glyphId, Shape outline, float minX, float minY, float maxX, float maxY) {

        float inkWidth() {
            return this.maxX - this.minX;
        }

        float inkHeight() {
            return this.maxY - this.minY;
        }

    }

    @NotNull
    private static <K> LinkedHashMap<K, SmoothTextLayout> createLayoutCache() {
        return new LinkedHashMap<>(64, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, SmoothTextLayout> eldest) {
                return this.size() > LAYOUT_CACHE_LIMIT;
            }
        };
    }

}
