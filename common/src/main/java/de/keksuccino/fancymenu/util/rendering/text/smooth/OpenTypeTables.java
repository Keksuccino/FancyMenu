package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal read-only view of an OpenType/TrueType file: the table directory plus the few header values the smooth font
 * system needs that AWT does not expose (units per em, advance widths of bitmap-only glyphs, vertical metrics).
 * Font collections ({@code ttcf}) are read from their first face.
 */
final class OpenTypeTables {

    private static final int TAG_COLLECTION = tag("ttcf");

    private final ByteBuffer data;
    private final Map<Integer, int[]> tables = new HashMap<>();
    private final int unitsPerEm;
    private final int ascender;
    private final int descender;
    private final int numberOfHMetrics;
    private final int hmtxOffset;

    private OpenTypeTables(@NotNull ByteBuffer data) {
        this.data = data.duplicate().order(ByteOrder.BIG_ENDIAN);
        int directory = 0;
        if (this.data.getInt(0) == TAG_COLLECTION) {
            directory = this.data.getInt(12);
        }
        int numTables = this.data.getShort(directory + 4) & 0xFFFF;
        for (int i = 0; i < numTables; i++) {
            int record = directory + 12 + (i * 16);
            int tag = this.data.getInt(record);
            int offset = this.data.getInt(record + 8);
            int length = this.data.getInt(record + 12);
            if (offset >= 0 && length >= 0 && (long) offset + length <= this.data.capacity()) {
                this.tables.put(tag, new int[]{offset, length});
            }
        }
        int head = this.offsetOf("head");
        this.unitsPerEm = (head >= 0) ? Math.max(1, this.data.getShort(head + 18) & 0xFFFF) : 1000;
        int hhea = this.offsetOf("hhea");
        if (hhea >= 0) {
            this.ascender = this.data.getShort(hhea + 4);
            this.descender = this.data.getShort(hhea + 6);
            this.numberOfHMetrics = this.data.getShort(hhea + 34) & 0xFFFF;
        } else {
            this.ascender = this.unitsPerEm;
            this.descender = 0;
            this.numberOfHMetrics = 0;
        }
        this.hmtxOffset = this.offsetOf("hmtx");
    }

    /**
     * Parses the table directory of the given font file, or returns {@code null} when the data is no readable sfnt font.
     */
    @Nullable
    static OpenTypeTables parse(@NotNull byte[] fontData) {
        if (fontData.length < 12) {
            return null;
        }
        try {
            return new OpenTypeTables(ByteBuffer.wrap(fontData));
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    static int tag(@NotNull String tag) {
        return (tag.charAt(0) << 24) | (tag.charAt(1) << 16) | (tag.charAt(2) << 8) | tag.charAt(3);
    }

    boolean has(@NotNull String tag) {
        return this.tables.containsKey(tag(tag));
    }

    /**
     * Absolute offset of a table in the font file, or {@code -1} when the font has no such table.
     */
    int offsetOf(@NotNull String tag) {
        int[] table = this.tables.get(tag(tag));
        return (table != null) ? table[0] : -1;
    }

    int lengthOf(@NotNull String tag) {
        int[] table = this.tables.get(tag(tag));
        return (table != null) ? table[1] : 0;
    }

    /**
     * Big-endian view over the whole font file. Offsets returned by {@link #offsetOf(String)} index into this buffer.
     */
    @NotNull
    ByteBuffer data() {
        return this.data;
    }

    int unitsPerEm() {
        return this.unitsPerEm;
    }

    int ascender() {
        return this.ascender;
    }

    int descender() {
        return this.descender;
    }

    /**
     * Advance width of a glyph in font units, read from {@code hmtx}.
     */
    int advanceWidth(int glyphId) {
        if (this.hmtxOffset < 0 || this.numberOfHMetrics <= 0 || glyphId < 0) {
            return this.unitsPerEm;
        }
        int metric = Math.min(glyphId, this.numberOfHMetrics - 1);
        return this.data.getShort(this.hmtxOffset + (metric * 4)) & 0xFFFF;
    }

    int numGlyphs() {
        int maxp = this.offsetOf("maxp");
        return (maxp >= 0) ? (this.data.getShort(maxp + 4) & 0xFFFF) : 0;
    }

}
