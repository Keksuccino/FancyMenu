package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.jetbrains.annotations.NotNull;
import java.awt.Font;
import java.awt.font.GlyphVector;
import java.util.Arrays;

/**
 * Turns {@link SmoothStyledText} into a {@link SmoothTextLayout}.
 * <p>
 * The text gets split into clusters, every cluster gets a font source (with emoji clusters preferring color emoji fonts),
 * and consecutive clusters with the same source and style get shaped together by AWT, which applies kerning and the
 * glyph substitutions of the font (that is how emoji sequences like flags and families become single glyphs). Color fonts
 * report no advances to AWT, so their glyphs get advanced by the widths from the font's own {@code hmtx} table instead.
 * </p>
 */
final class SmoothTextShaper {

    private final SmoothFont font;
    private final SmoothStyledText text;
    private final int[] codepoints;
    private final SmoothTextLayout.Builder layout;
    private final int[] clusterEnds;
    private final int[] clusterSources;
    private final boolean[] clusterEmoji;
    private final int[] clusterStartOf;
    private char[] chars = new char[32];
    private int[] charCodepoints = new int[32];
    private int line;
    private float pen;

    private SmoothTextShaper(@NotNull SmoothFont font, @NotNull SmoothStyledText text) {
        this.font = font;
        this.text = text;
        this.codepoints = text.codepointBuffer();
        int length = text.length();
        this.layout = new SmoothTextLayout.Builder(length);
        this.clusterEnds = new int[length];
        this.clusterSources = new int[length];
        this.clusterEmoji = new boolean[length];
        this.clusterStartOf = new int[length];
    }

    @NotNull
    static SmoothTextLayout shape(@NotNull SmoothFont font, @NotNull SmoothStyledText text) {
        if (text.length() == 0) {
            return SmoothTextLayout.EMPTY;
        }
        return new SmoothTextShaper(font, text).shapeAll();
    }

    @NotNull
    private SmoothTextLayout shapeAll() {
        int length = this.text.length();
        boolean multiline = this.text.multiline();
        float maxWidth = 0.0F;
        int lineStart = 0;
        for (int i = 0; i <= length; i++) {
            if (i < length && !(multiline && this.codepoints[i] == '\n')) {
                continue;
            }
            maxWidth = Math.max(maxWidth, this.shapeLine(lineStart, i));
            if (i < length) {
                this.line++;
                lineStart = i + 1;
            }
        }
        return this.layout.build(maxWidth, this.line + 1);
    }

    private float shapeLine(int start, int end) {
        this.pen = 0.0F;
        for (int c = start; c < end; ) {
            int clusterEnd = UnicodeClusters.clusterEnd(this.codepoints, c, end);
            boolean emoji = UnicodeClusters.isEmojiCluster(this.codepoints, c, clusterEnd);
            this.clusterEnds[c] = clusterEnd;
            this.clusterEmoji[c] = emoji;
            this.clusterSources[c] = this.font.resolveSource(this.displayedCodepoint(c), emoji);
            for (int i = c; i < clusterEnd; i++) {
                this.clusterStartOf[i] = c;
            }
            c = clusterEnd;
        }
        for (int c = start; c < end; ) {
            int resolved = this.clusterSources[c];
            int styleIndex = styleIndex(this.text.styleAt(c));
            if (this.clusterEmoji[c] && this.isColorSource(resolved)) {
                // Emoji get shaped one cluster at a time, so a sequence the color font can't draw can fall back on its own.
                int clusterEnd = this.clusterEnds[c];
                if (!this.shapeRun(c, clusterEnd, resolved, 0, true)) {
                    this.shapeRun(c, clusterEnd, this.font.resolveTextSource(this.displayedCodepoint(c)), styleIndex, false);
                }
                c = clusterEnd;
                continue;
            }
            int runEnd = this.clusterEnds[c];
            while (runEnd < end && this.clusterSources[runEnd] == resolved && styleIndex(this.text.styleAt(runEnd)) == styleIndex && !(this.clusterEmoji[runEnd] && this.isColorSource(resolved))) {
                runEnd = this.clusterEnds[runEnd];
            }
            this.shapeRun(c, runEnd, resolved, styleIndex, false);
            c = runEnd;
        }
        this.layout.setCaret(end, this.pen);
        this.addDecorations(start, end);
        return this.pen;
    }

    /**
     * Shapes the codepoints {@code [start, end)} with one font source and appends the glyphs at the current pen position.
     *
     * @param requireDrawable when {@code true}, nothing gets added and {@code false} is returned if the source has no visible
     *                        glyph for some part of the run (a color font without outlines that lacks a color glyph)
     */
    private boolean shapeRun(int start, int end, int resolvedSource, int styleIndex, boolean requireDrawable) {
        int sourceIndex = resolvedSource & SmoothFont.SOURCE_INDEX_MASK;
        boolean missing = (resolvedSource & SmoothFont.SOURCE_MISSING) != 0;
        SmoothFontSource.Loaded source = this.font.source(sourceIndex).get();
        if (source == null) {
            return false;
        }
        int charCount = this.fillChars(start, end, missing);
        Font layoutFont = source.layoutFont(styleIndex);
        GlyphVector vector = layoutFont.layoutGlyphVector(SmoothFontSource.RENDER_CONTEXT, this.chars, 0, charCount, Font.LAYOUT_LEFT_TO_RIGHT);
        int glyphCount = vector.getNumGlyphs();
        int[] glyphCodes = vector.getGlyphCodes(0, glyphCount, null);
        int[] glyphChars = vector.getGlyphCharIndices(0, glyphCount, null);
        boolean colorFont = source.colorGlyphs != null;
        if (requireDrawable && colorFont && !source.hasOutlines) {
            for (int glyph : glyphCodes) {
                if (glyph < SmoothFontSource.FIRST_INVISIBLE_GLYPH && !source.isColorGlyph(glyph)) {
                    return false;
                }
            }
        }
        float[] positions = colorFont ? null : vector.getGlyphPositions(0, glyphCount + 1, null);
        float colorScale = this.font.colorGlyphScale();
        float runPen = 0.0F;
        for (int g = 0; g < glyphCount; g++) {
            int glyph = glyphCodes[g];
            int codepointIndex = this.charCodepoints[glyphChars[g]];
            boolean invisible = glyph >= SmoothFontSource.FIRST_INVISIBLE_GLYPH;
            boolean colorGlyph = !invisible && colorFont && source.isColorGlyph(glyph);
            float x;
            float advance;
            if (positions != null) {
                x = positions[g * 2] / SmoothFontSource.LAYOUT_SIZE;
                advance = (positions[(g + 1) * 2] - positions[g * 2]) / SmoothFontSource.LAYOUT_SIZE;
            } else {
                x = runPen;
                advance = invisible ? 0.0F : (source.tableAdvance(glyph) * (colorGlyph ? colorScale : 1.0F));
                runPen += advance;
            }
            int clusterStart = this.clusterStartOf[codepointIndex];
            float caret = this.layout.caret(clusterStart);
            if (Float.isNaN(caret) || (this.pen + x) < caret) {
                this.layout.setCaret(clusterStart, this.pen + x);
            }
            if (invisible) {
                continue;
            }
            int style = this.text.styleAt(codepointIndex);
            byte flags = 0;
            if (colorGlyph) {
                flags |= SmoothTextLayout.GLYPH_COLOR;
            } else {
                if ((styleIndex & 1) != 0) flags |= SmoothTextLayout.GLYPH_BOLD;
                if ((styleIndex & 2) != 0) flags |= SmoothTextLayout.GLYPH_ITALIC;
                if ((style & SmoothStyledText.OBFUSCATED) != 0 && this.codepoints[codepointIndex] != ' ') flags |= SmoothTextLayout.GLYPH_OBFUSCATED;
            }
            this.layout.addGlyph(glyph, sourceIndex, flags, this.pen + x, this.line, style, advance);
        }
        float runAdvance = (positions != null) ? (positions[glyphCount * 2] / SmoothFontSource.LAYOUT_SIZE) : runPen;
        // Clusters that produced no glyph of their own (merged into a ligature, or invisible) get the caret of the next cluster.
        float nextCaret = this.pen + runAdvance;
        int c = end;
        while (c > start) {
            c = this.clusterStartOf[c - 1];
            float caret = this.layout.caret(c);
            if (Float.isNaN(caret)) {
                this.layout.setCaret(c, nextCaret);
            } else {
                nextCaret = caret;
            }
        }
        this.pen += runAdvance;
        return true;
    }

    private int fillChars(int start, int end, boolean missing) {
        int charCount = 0;
        for (int i = start; i < end; i++) {
            if (charCount + 2 > this.chars.length) {
                this.chars = Arrays.copyOf(this.chars, this.chars.length * 2);
                this.charCodepoints = Arrays.copyOf(this.charCodepoints, this.chars.length);
            }
            int codepoint = missing ? SmoothFont.MISSING_GLYPH_CODEPOINT : this.displayedCodepoint(i);
            int written = Character.toChars(codepoint, this.chars, charCount);
            for (int k = 0; k < written; k++) {
                this.charCodepoints[charCount + k] = i;
            }
            charCount += written;
            if (missing) {
                // A cluster without any font that can draw it becomes a single replacement glyph.
                i = this.clusterEnds[this.clusterStartOf[i]] - 1;
            }
        }
        return charCount;
    }

    // Line breaks that don't start a new line (single-line mode) are drawn as spaces.
    private int displayedCodepoint(int index) {
        int codepoint = this.codepoints[index];
        return (codepoint == '\n') ? ' ' : codepoint;
    }

    private boolean isColorSource(int resolvedSource) {
        SmoothFontSource.Loaded source = this.font.source(resolvedSource & SmoothFont.SOURCE_INDEX_MASK).get();
        return (source != null) && (source.colorGlyphs != null);
    }

    private void addDecorations(int start, int end) {
        this.addDecorations(start, end, SmoothStyledText.UNDERLINE, SmoothTextLayout.DECORATION_UNDERLINE);
        this.addDecorations(start, end, SmoothStyledText.STRIKETHROUGH, SmoothTextLayout.DECORATION_STRIKETHROUGH);
    }

    private void addDecorations(int start, int end, int styleFlag, byte kind) {
        int colorMask = SmoothStyledText.HAS_COLOR | SmoothStyledText.COLOR_MASK;
        float segmentStart = Float.NaN;
        int segmentStyle = 0;
        for (int c = start; c < end; c = this.clusterEnds[c]) {
            int style = this.text.styleAt(c);
            boolean decorated = (style & styleFlag) != 0;
            boolean sameColor = (style & colorMask) == (segmentStyle & colorMask);
            if (!Float.isNaN(segmentStart) && (!decorated || !sameColor)) {
                this.layout.addDecoration(kind, segmentStart, this.layout.caret(c), this.line, segmentStyle);
                segmentStart = Float.NaN;
            }
            if (decorated && Float.isNaN(segmentStart)) {
                segmentStart = this.layout.caret(c);
                segmentStyle = style;
            }
        }
        if (!Float.isNaN(segmentStart)) {
            this.layout.addDecoration(kind, segmentStart, this.layout.caret(end), this.line, segmentStyle);
        }
    }

    static int styleIndex(int style) {
        return (((style & SmoothStyledText.BOLD) != 0) ? 1 : 0) | (((style & SmoothStyledText.ITALIC) != 0) ? 2 : 0);
    }

}
