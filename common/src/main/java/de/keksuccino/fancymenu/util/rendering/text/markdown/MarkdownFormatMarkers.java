package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * The format markers of a Markdown text that the parser reads as markers when it gets to them, in the order of the text.
 *
 * <p>{@link MarkdownStructure} adds them while it reads the text. It leaves out every marker the parser reads as text or skips:
 * escaped ones, the ones inside of code and plain text, and the ones in something like the target of a link or the marker of a list item.
 * The parser asks here if a format it is about to open gets closed again, so it only opens a format in front of a marker that really closes it.</p>
 *
 * <p>The parser has to ask in the order of the text: no question may start in front of the index an earlier question for the same marker started at.
 * Every question then only moves forward through the markers, which keeps all questions together linear to their number.</p>
 */
final class MarkdownFormatMarkers {

    private static final int[] NO_INDEXES = new int[0];

    /** The indexes of the markers in the text, for every {@link FormatMarker}. */
    final int[][] indexes = new int[FormatMarker.COUNT][];
    final int[] counts = new int[FormatMarker.COUNT];
    /** For every {@link FormatMarker}, where in its indexes the first marker is that is not in front of the last question. */
    final int[] next = new int[FormatMarker.COUNT];

    MarkdownFormatMarkers() {
        Arrays.fill(this.indexes, NO_INDEXES);
    }

    void add(@NotNull FormatMarker marker, int index) {
        int type = marker.ordinal();
        int count = this.counts[type];
        if (count == this.indexes[type].length) this.indexes[type] = Arrays.copyOf(this.indexes[type], Math.max(16, count * 2));
        this.indexes[type][count] = index;
        this.counts[type] = count + 1;
    }

    /**
     * Adds a marker that forms a pair with a marker of the same type right in front of it.
     *
     * @param pair The type of the pairs, which get added with the index of their first marker.
     */
    void addPaired(@NotNull FormatMarker marker, @NotNull FormatMarker pair, int index) {
        int count = this.counts[marker.ordinal()];
        if ((count > 0) && (this.indexes[marker.ordinal()][count - 1] == (index - 1))) this.add(pair, index - 1);
        this.add(marker, index);
    }

    /**
     * Returns if there is a marker of the given type at or after the given index.
     */
    boolean existsFrom(@NotNull FormatMarker marker, int fromIndex) {
        return this.moveTo(marker, fromIndex) < this.counts[marker.ordinal()];
    }

    /**
     * Returns if there is a marker of the given type at the given index.
     */
    boolean isAt(@NotNull FormatMarker marker, int index) {
        int next = this.moveTo(marker, index);
        return (next < this.counts[marker.ordinal()]) && (this.indexes[marker.ordinal()][next] == index);
    }

    /**
     * Returns where in the indexes of the given type the first marker at or after the given index is, which is their count if there is none.
     */
    private int moveTo(@NotNull FormatMarker marker, int fromIndex) {
        int type = marker.ordinal();
        int[] indexes = this.indexes[type];
        int count = this.counts[type];
        int next = this.next[type];
        while ((next < count) && (indexes[next] < fromIndex)) next++;
        this.next[type] = next;
        return next;
    }

    /**
     * Returns if italic text that a single asterisk opens gets closed by an asterisk at or after the given index.
     *
     * <p>Italic text ends at an asterisk that has no second one behind it, but the parser reads asterisks in pairs as markers of bold text first.
     * This goes through the rows of asterisks the way the parser will: the pairs of a row open and close bold text, and only an asterisk
     * that is left over ends the italic text. A pair that finds no later pair to close its bold text opens none, which leaves its second asterisk over.</p>
     *
     * <p>Italic text that has to end inside of the bold text it opens in has no end if the pair that closes that bold text comes first.</p>
     *
     * @param boldOpen If the italic text opens inside of bold text.
     * @param insideOuterBold If the italic text has to end inside of that bold text.
     */
    boolean hasItalicAsteriskEnd(int fromIndex, boolean boldOpen, boolean insideOuterBold) {
        int[] asterisks = this.indexes[FormatMarker.ASTERISK.ordinal()];
        int count = this.counts[FormatMarker.ASTERISK.ordinal()];
        int asterisk = this.moveTo(FormatMarker.ASTERISK, fromIndex);
        while (asterisk < count) {
            // A row is made of asterisks that follow each other without anything in between
            int rowStart = asterisk;
            asterisk++;
            while ((asterisk < count) && (asterisks[asterisk] == (asterisks[asterisk - 1] + 1))) asterisk++;
            int rowLength = asterisk - rowStart;
            if ((rowLength % 2) == 1) return true;
            for (int pair = 0; pair < (rowLength / 2); pair++) {
                if (boldOpen && insideOuterBold) return false;
                boldOpen = !boldOpen;
            }
        }
        // Bold text that is still open here was opened by a pair without a later pair, so it is none and the second asterisk of that pair is the end
        return boldOpen && !insideOuterBold;
    }

    /**
     * The types of {@link MarkdownFormatMarkers}.
     */
    enum FormatMarker {

        ASTERISK,
        /** Two asterisks in a row, which open and close bold text. */
        ASTERISK_PAIR,
        /** An underscore that opens and closes italic text: one without another one next to it, or the last one of three underscores that are markers. */
        UNDERSCORE,
        /** The first one of two underscores that can open bold text, which they can't inside of a word or in front of whitespace. */
        UNDERSCORE_PAIR_OPEN,
        /** The first one of two underscores that can close bold text, which they can't inside of a word or behind whitespace. */
        UNDERSCORE_PAIR_CLOSE,
        TILDE,
        /** Two tildes in a row, which open and close struck through text like a single one does. */
        TILDE_PAIR,
        /** The marker that closes a font. */
        FONT_SUFFIX,
        /** The marker that closes a color. */
        COLOR_SUFFIX;

        static final int COUNT = values().length;

    }

}
