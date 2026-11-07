package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * Shaped, size-independent text: glyph positions and decoration spans in em units (multiply by the text size to get GUI units).
 * Layouts are immutable and cached per font, so measuring and rendering the same text only shapes it once.
 */
final class SmoothTextLayout {

    static final byte GLYPH_BOLD = 1;
    static final byte GLYPH_ITALIC = 2;
    static final byte GLYPH_COLOR = 4;
    static final byte GLYPH_OBFUSCATED = 8;
    static final byte DECORATION_UNDERLINE = 1;
    static final byte DECORATION_STRIKETHROUGH = 2;

    static final SmoothTextLayout EMPTY = new Builder(0).build(0.0F, 0);
    /** A single line without content, for non-empty legacy strings that only consist of formatting codes. */
    static final SmoothTextLayout EMPTY_LINE = new Builder(0).build(0.0F, 1);

    /** Width of the widest line, in em. */
    final float width;
    final int lineCount;

    final int glyphCount;
    final int[] glyphIds;
    final byte[] glyphSources;
    final byte[] glyphFlags;
    final float[] glyphX;
    final int[] glyphLines;
    final int[] glyphStyles;
    final float[] glyphAdvances;

    final int decorationCount;
    final float[] decorationStarts;
    final float[] decorationEnds;
    final int[] decorationLines;
    final int[] decorationStyles;
    final byte[] decorationKinds;

    /**
     * Caret position (in em) before each codepoint of the input text, plus one entry for the end of the text. Positions inside a
     * cluster are {@code NaN}, because the text can't get split there. Carets restart at {@code 0} on every new line.
     */
    final float[] carets;

    private SmoothTextLayout(@NotNull Builder builder, float width, int lineCount) {
        this.width = width;
        this.lineCount = lineCount;
        this.glyphCount = builder.glyphCount;
        this.glyphIds = Arrays.copyOf(builder.glyphIds, builder.glyphCount);
        this.glyphSources = Arrays.copyOf(builder.glyphSources, builder.glyphCount);
        this.glyphFlags = Arrays.copyOf(builder.glyphFlags, builder.glyphCount);
        this.glyphX = Arrays.copyOf(builder.glyphX, builder.glyphCount);
        this.glyphLines = Arrays.copyOf(builder.glyphLines, builder.glyphCount);
        this.glyphStyles = Arrays.copyOf(builder.glyphStyles, builder.glyphCount);
        this.glyphAdvances = Arrays.copyOf(builder.glyphAdvances, builder.glyphCount);
        this.decorationCount = builder.decorationCount;
        this.decorationStarts = Arrays.copyOf(builder.decorationStarts, builder.decorationCount);
        this.decorationEnds = Arrays.copyOf(builder.decorationEnds, builder.decorationCount);
        this.decorationLines = Arrays.copyOf(builder.decorationLines, builder.decorationCount);
        this.decorationStyles = Arrays.copyOf(builder.decorationStyles, builder.decorationCount);
        this.decorationKinds = Arrays.copyOf(builder.decorationKinds, builder.decorationCount);
        this.carets = builder.carets;
    }

    /**
     * Collects glyphs and decorations while shaping, growing its arrays as needed.
     */
    static final class Builder {

        private int glyphCount;
        private int[] glyphIds = new int[16];
        private byte[] glyphSources = new byte[16];
        private byte[] glyphFlags = new byte[16];
        private float[] glyphX = new float[16];
        private int[] glyphLines = new int[16];
        private int[] glyphStyles = new int[16];
        private float[] glyphAdvances = new float[16];

        private int decorationCount;
        private float[] decorationStarts = new float[4];
        private float[] decorationEnds = new float[4];
        private int[] decorationLines = new int[4];
        private int[] decorationStyles = new int[4];
        private byte[] decorationKinds = new byte[4];

        private final float[] carets;

        Builder(int codepointCount) {
            this.carets = new float[codepointCount + 1];
            Arrays.fill(this.carets, Float.NaN);
        }

        void addGlyph(int glyphId, int source, byte flags, float x, int line, int style, float advance) {
            if (this.glyphCount == this.glyphIds.length) {
                int capacity = this.glyphCount * 2;
                this.glyphIds = Arrays.copyOf(this.glyphIds, capacity);
                this.glyphSources = Arrays.copyOf(this.glyphSources, capacity);
                this.glyphFlags = Arrays.copyOf(this.glyphFlags, capacity);
                this.glyphX = Arrays.copyOf(this.glyphX, capacity);
                this.glyphLines = Arrays.copyOf(this.glyphLines, capacity);
                this.glyphStyles = Arrays.copyOf(this.glyphStyles, capacity);
                this.glyphAdvances = Arrays.copyOf(this.glyphAdvances, capacity);
            }
            int i = this.glyphCount++;
            this.glyphIds[i] = glyphId;
            this.glyphSources[i] = (byte) source;
            this.glyphFlags[i] = flags;
            this.glyphX[i] = x;
            this.glyphLines[i] = line;
            this.glyphStyles[i] = style;
            this.glyphAdvances[i] = advance;
        }

        void addDecoration(byte kind, float start, float end, int line, int style) {
            if (end <= start) {
                return;
            }
            if (this.decorationCount == this.decorationStarts.length) {
                int capacity = this.decorationCount * 2;
                this.decorationStarts = Arrays.copyOf(this.decorationStarts, capacity);
                this.decorationEnds = Arrays.copyOf(this.decorationEnds, capacity);
                this.decorationLines = Arrays.copyOf(this.decorationLines, capacity);
                this.decorationStyles = Arrays.copyOf(this.decorationStyles, capacity);
                this.decorationKinds = Arrays.copyOf(this.decorationKinds, capacity);
            }
            int i = this.decorationCount++;
            this.decorationKinds[i] = kind;
            this.decorationStarts[i] = start;
            this.decorationEnds[i] = end;
            this.decorationLines[i] = line;
            this.decorationStyles[i] = style;
        }

        void setCaret(int codepointIndex, float x) {
            this.carets[codepointIndex] = x;
        }

        float caret(int codepointIndex) {
            return this.carets[codepointIndex];
        }

        @NotNull
        SmoothTextLayout build(float width, int lineCount) {
            return new SmoothTextLayout(this, width, lineCount);
        }

    }

}
