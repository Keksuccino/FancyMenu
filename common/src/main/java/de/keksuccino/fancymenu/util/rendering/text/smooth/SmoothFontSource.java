package de.keksuccino.fancymenu.util.rendering.text.smooth;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * One font file of a {@link SmoothFont}. The file is only read and parsed when the font first needs it, or ahead of time on a
 * background thread via {@link #preloadAsync()}. All other methods must be called on the render thread.
 */
final class SmoothFontSource {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Point size the text layout gets shaped at. Positions scale linearly, because layout uses fractional, unhinted metrics. */
    static final float LAYOUT_SIZE = 64.0F;
    static final FontRenderContext RENDER_CONTEXT = new FontRenderContext(null, true, true);

    private static final int[] AWT_STYLES = {Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC};
    private static final String OBFUSCATION_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    /** AWT reports glyph codes from this value up for invisible characters (like zero-width joiners) that have no glyph. */
    static final int FIRST_INVISIBLE_GLYPH = 0xFFFE;

    // Shared by all fonts: idle threads die after a few seconds, so no thread stays alive when nothing is loading.
    private static final ExecutorService LOADER = new ThreadPoolExecutor(0, 1, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu Smooth Font Loader");
        thread.setDaemon(true);
        return thread;
    });

    final String label;
    final int index;
    final boolean emojiFallback;
    private final ByteLoader loader;
    @Nullable
    private volatile CompletableFuture<Loaded> pendingLoad;
    @Nullable
    private Loaded loaded;
    private boolean failed;

    SmoothFontSource(@NotNull String label, int index, boolean emojiFallback, @NotNull ByteLoader loader) {
        this.label = label;
        this.index = index;
        this.emojiFallback = emojiFallback;
        this.loader = loader;
    }

    /**
     * Starts reading and parsing the font file on a background thread, if that did not happen yet.
     */
    void preloadAsync() {
        if (this.loaded != null || this.failed || this.pendingLoad != null) {
            return;
        }
        this.pendingLoad = CompletableFuture.supplyAsync(this::readFont, LOADER);
    }

    /**
     * Returns the parsed font, loading it on the calling thread when needed, or {@code null} when the file failed to load.
     */
    @Nullable
    Loaded get() {
        if (this.loaded != null || this.failed) {
            return this.loaded;
        }
        try {
            CompletableFuture<Loaded> pending = this.pendingLoad;
            this.loaded = (pending != null) ? pending.join() : this.readFont();
        } catch (CompletionException | IllegalStateException ex) {
            Throwable cause = (ex instanceof CompletionException && ex.getCause() != null) ? ex.getCause() : ex;
            LOGGER.error("[FANCYMENU] Failed to load smooth font file: {}", this.label, cause);
            this.failed = true;
        }
        this.pendingLoad = null;
        return this.loaded;
    }

    boolean canDisplay(int codepoint) {
        Loaded font = this.get();
        return (font != null) && font.font.canDisplay(codepoint);
    }

    /**
     * Whether this file draws the character (on its own) as a color glyph.
     */
    boolean displaysAsColor(int codepoint) {
        Loaded font = this.get();
        if (font == null || font.colorGlyphs == null) {
            return false;
        }
        int glyph = font.layoutFont(0).createGlyphVector(RENDER_CONTEXT, Character.toChars(codepoint)).getGlyphCode(0);
        return font.isColorGlyph(glyph);
    }

    @NotNull
    private Loaded readFont() {
        try {
            byte[] bytes = this.loader.load();
            Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(bytes));
            OpenTypeTables tables = OpenTypeTables.parse(bytes);
            ColorGlyphReader colorGlyphs = (tables != null) ? ColorGlyphReader.create(tables) : null;
            boolean hasOutlines = (tables == null) || tables.has("glyf") || tables.has("CFF ") || tables.has("CFF2");
            if (colorGlyphs != null) {
                LOGGER.info("[FANCYMENU] Smooth font file '{}' has color glyphs.", this.label);
            }
            // Only color fonts keep their raw tables (and with them the file bytes), because AWT can't read color glyph data.
            return new Loaded(font, (colorGlyphs != null) ? tables : null, colorGlyphs, hasOutlines);
        } catch (IOException | FontFormatException ex) {
            throw new IllegalStateException("Unreadable font file: " + this.label, ex);
        }
    }

    @FunctionalInterface
    interface ByteLoader {
        @NotNull
        byte[] load() throws IOException;
    }

    /**
     * A parsed font file with its lazily derived AWT fonts. Derived fonts are cached, because deriving them is not free and
     * every glyph raster and text shaping call needs one.
     */
    static final class Loaded {

        final Font font;
        @Nullable
        final OpenTypeTables tables;
        @Nullable
        final ColorGlyphReader colorGlyphs;
        final boolean hasOutlines;
        private final Font[] layoutFonts = new Font[4];
        private final Int2ObjectOpenHashMap<Font> rasterFonts = new Int2ObjectOpenHashMap<>();
        @SuppressWarnings("unchecked")
        private final Int2ObjectOpenHashMap<int[]>[] obfuscationGlyphs = new Int2ObjectOpenHashMap[4];

        private Loaded(@NotNull Font font, @Nullable OpenTypeTables tables, @Nullable ColorGlyphReader colorGlyphs, boolean hasOutlines) {
            this.font = font;
            this.tables = tables;
            this.colorGlyphs = colorGlyphs;
            this.hasOutlines = hasOutlines;
        }

        /**
         * Font for shaping text runs, at {@link #LAYOUT_SIZE} with kerning enabled.
         *
         * @param styleIndex {@code 1} for bold, {@code 2} for italic, {@code 3} for both
         */
        @NotNull
        Font layoutFont(int styleIndex) {
            Font font = this.layoutFonts[styleIndex];
            if (font == null) {
                font = this.font.deriveFont(AWT_STYLES[styleIndex], LAYOUT_SIZE).deriveFont(Map.of(TextAttribute.KERNING, TextAttribute.KERNING_ON));
                this.layoutFonts[styleIndex] = font;
            }
            return font;
        }

        /**
         * Font for rasterizing glyphs at the given pixel size.
         */
        @NotNull
        Font rasterFont(int styleIndex, int pixelSize) {
            int key = (pixelSize << 2) | styleIndex;
            Font font = this.rasterFonts.get(key);
            if (font == null) {
                font = this.font.deriveFont(AWT_STYLES[styleIndex], (float) pixelSize);
                this.rasterFonts.put(key, font);
            }
            return font;
        }

        boolean isColorGlyph(int glyphId) {
            return (this.colorGlyphs != null) && this.colorGlyphs.hasColorGlyph(glyphId);
        }

        /**
         * Advance of a glyph in em units, read from the font tables. Only available for color fonts, because AWT reports
         * zero advances for glyphs without outlines.
         */
        float tableAdvance(int glyphId) {
            OpenTypeTables tables = this.tables;
            return (tables != null) ? ((float) tables.advanceWidth(glyphId) / (float) tables.unitsPerEm()) : 0.0F;
        }

        /**
         * Picks a random glyph that has the same advance as the original glyph, so obfuscated text keeps its width.
         */
        int obfuscatedGlyph(int styleIndex, int originalGlyph, float advanceEm, @NotNull Random random) {
            Int2ObjectOpenHashMap<int[]> glyphsByAdvance = this.obfuscationGlyphs[styleIndex];
            if (glyphsByAdvance == null) {
                glyphsByAdvance = this.collectObfuscationGlyphs(styleIndex);
                this.obfuscationGlyphs[styleIndex] = glyphsByAdvance;
            }
            int[] candidates = glyphsByAdvance.get(advanceKey(advanceEm));
            return (candidates != null) ? candidates[random.nextInt(candidates.length)] : originalGlyph;
        }

        @NotNull
        private Int2ObjectOpenHashMap<int[]> collectObfuscationGlyphs(int styleIndex) {
            Font font = this.layoutFont(styleIndex);
            Int2ObjectOpenHashMap<int[]> result = new Int2ObjectOpenHashMap<>();
            for (int i = 0; i < OBFUSCATION_CHARS.length(); i++) {
                GlyphVector vector = font.createGlyphVector(RENDER_CONTEXT, OBFUSCATION_CHARS.substring(i, i + 1));
                int glyph = vector.getGlyphCode(0);
                if (glyph == 0 || glyph >= FIRST_INVISIBLE_GLYPH) {
                    continue;
                }
                int key = advanceKey(vector.getGlyphMetrics(0).getAdvanceX() / LAYOUT_SIZE);
                int[] existing = result.get(key);
                int[] extended = (existing == null) ? new int[1] : Arrays.copyOf(existing, existing.length + 1);
                extended[extended.length - 1] = glyph;
                result.put(key, extended);
            }
            return result;
        }

        private static int advanceKey(float advanceEm) {
            return Math.round(advanceEm * 256.0F);
        }

    }

}
