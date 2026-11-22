package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParser;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer.MarkdownLineAlignment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The formatting shortcuts of a {@link TextEditorWindowBody} that edits Markdown text: bold, italic and underlined text, and the alignment of lines.
 *
 * <p>Everything here works on the text of the editor, with its lines separated by line feeds, and on its selection as offsets into that text.
 * Every shortcut toggles its format: it adds the markers of the format, or takes them away again if the selected text has the format already.</p>
 */
public final class TextEditorMarkdownFormatting {

    private static final char NEWLINE_CHAR = '\n';
    private static final String NEWLINE = "\n";
    /**
     * What a Markdown line can start with that is no part of its text: the prefixes of quotes, and the marker of a list item or a headline.<br>
     * Markers of formats in front of such a prefix would break it, so formatting a selection that starts in front of it starts behind it.
     */
    private static final Pattern LINE_PREFIX = Pattern.compile("(?:> )*(?: *(?:[-+*~]|\\d{1,9}[.)]) |#{1,3} )?");
    /** The marker lines of aligned blocks, multi-line plain text and code blocks, which stop being marker lines if a format marker stands in front of them. */
    private static final Pattern BLOCK_MARKER_LINE = Pattern.compile("(?:\\^\\^\\^|\\|\\|\\||;;;)\\s*|```.*");
    private static final String CENTERED_BLOCK_MARKER = "^^^";
    private static final String RIGHT_BLOCK_MARKER = "|||";

    private TextEditorMarkdownFormatting() {
    }

    /**
     * Applies the shortcut to the text.
     *
     * @param selectionStart The offset of the start of the selection, which is the offset of the cursor if nothing is selected.
     * @param selectionEnd The offset of the end of the selection, which is the offset of the cursor if nothing is selected.
     * @return The changed text and what is selected in it afterwards, or NULL if the shortcut changes nothing.
     */
    @Nullable
    public static Edit apply(@NotNull Shortcut shortcut, @NotNull String text, int selectionStart, int selectionEnd) {
        int start = Math.max(0, Math.min(selectionStart, selectionEnd));
        int end = Math.min(text.length(), Math.max(selectionStart, selectionEnd));
        return switch (shortcut) {
            case BOLD -> toggleInlineFormat(InlineFormat.BOLD, text, start, end);
            case ITALIC -> toggleInlineFormat(InlineFormat.ITALIC, text, start, end);
            case UNDERLINE -> toggleInlineFormat(InlineFormat.UNDERLINE, text, start, end);
            case ALIGN_LEFT -> align(text, start, end, MarkdownLineAlignment.LEFT);
            case ALIGN_CENTER -> align(text, start, end, MarkdownLineAlignment.CENTERED);
            case ALIGN_RIGHT -> align(text, start, end, MarkdownLineAlignment.RIGHT);
        };
    }

    /**
     * Toggles a format of text inside of lines.
     *
     * <p>Without a selection, this adds the markers of the format at the cursor, which stands between them afterwards, so what gets typed next has the format.
     * If the cursor stands between such markers already, they get taken away again.</p>
     *
     * <p>A selection that starts or ends with whitespace gets formatted without it. If the selection is the text between the markers of the format,
     * or starts and ends with them, they get taken away. Every other selection gets the markers of the format around it. The markers of the format inside of it
     * get taken away then, so text that is only partly formatted becomes formatted as a whole. That only works if the selection holds whole formatted texts,
     * which it does if it holds an even number of markers. Otherwise, it starts or ends inside of formatted text, and nothing happens.</p>
     */
    @Nullable
    private static Edit toggleInlineFormat(@NotNull InlineFormat format, @NotNull String text, int start, int end) {
        String marker = format.marker;
        int markerLength = marker.length();
        char character = format.character;

        if (start == end) {
            if (format.isMarkerRow(countRowBefore(text, start, character, 0)) && format.isMarkerRow(countRowFrom(text, start, character, text.length()))) {
                return new Edit(text.substring(0, start - markerLength) + text.substring(start + markerLength), start - markerLength, start - markerLength);
            }
            return new Edit(text.substring(0, start) + marker + marker + text.substring(start), start + markerLength, start + markerLength);
        }

        int[] range = getInlineFormatRange(text, start, end, character);
        if (range == null) return null;
        int rangeStart = range[0];
        int rangeEnd = range[1];

        List<int[]> markerRows = findMarkerRows(format, text, rangeStart, rangeEnd);
        // The selection starts and ends with the markers of the format, and nothing in between is another one of them
        if ((markerRows.size() == 2) && (markerRows.get(0)[0] == rangeStart) && (markerRows.get(1)[1] == rangeEnd)) {
            return new Edit(text.substring(0, rangeStart) + text.substring(rangeStart + markerLength, rangeEnd - markerLength) + text.substring(rangeEnd), rangeStart, rangeEnd - (2 * markerLength));
        }
        // The selection is the text between the markers of the format
        if (markerRows.isEmpty() && format.isMarkerRow(countRowBefore(text, rangeStart, character, 0)) && format.isMarkerRow(countRowFrom(text, rangeEnd, character, text.length()))) {
            return new Edit(text.substring(0, rangeStart - markerLength) + text.substring(rangeStart, rangeEnd) + text.substring(rangeEnd + markerLength), rangeStart - markerLength, rangeEnd - markerLength);
        }
        if ((markerRows.size() % 2) != 0) return null;

        StringBuilder formatted = new StringBuilder(text.length() + (2 * markerLength)).append(text, 0, rangeStart).append(marker);
        int copiedUntil = rangeStart;
        for (int[] row : markerRows) {
            formatted.append(text, copiedUntil, row[0]).append(text, row[0] + markerLength, row[1]);
            copiedUntil = row[1];
        }
        formatted.append(text, copiedUntil, rangeEnd);
        int formattedEnd = formatted.length();
        formatted.append(marker).append(text, rangeEnd, text.length());
        return new Edit(formatted.toString(), rangeStart + markerLength, formattedEnd);
    }

    /**
     * Returns the part of the selection that gets formatted, as start and end offsets, or NULL if nothing of it can get formatted.
     *
     * <p>That part has no whitespace at its start and end. It starts behind the prefix of a line, and it leaves out marker lines of blocks at its start and end.
     * Markers of the format at its start and end belong to it as a whole, so it never ends in the middle of them.</p>
     */
    @Nullable
    private static int[] getInlineFormatRange(@NotNull String text, int start, int end, char markerCharacter) {
        int rangeStart = start;
        int rangeEnd = end;
        boolean changed = true;
        while (changed && (rangeStart < rangeEnd)) {
            int oldStart = rangeStart;
            int oldEnd = rangeEnd;
            while ((rangeStart < rangeEnd) && Character.isWhitespace(text.charAt(rangeStart))) rangeStart++;
            while ((rangeEnd > rangeStart) && Character.isWhitespace(text.charAt(rangeEnd - 1))) rangeEnd--;
            if (rangeStart == rangeEnd) return null;
            int startLine = getLineStart(text, rangeStart);
            int startLineEnd = getLineEnd(text, rangeStart);
            if (BLOCK_MARKER_LINE.matcher(text).region(startLine, startLineEnd).matches()) {
                rangeStart = Math.min(rangeEnd, startLineEnd + 1);
            } else {
                rangeStart = Math.max(rangeStart, startLine + getLinePrefixLength(text, startLine, startLineEnd));
            }
            if (rangeStart >= rangeEnd) return null;
            int endLine = getLineStart(text, rangeEnd - 1);
            int endLineEnd = getLineEnd(text, rangeEnd - 1);
            if (endLine > rangeStart) {
                // A selection that ends inside of the prefix or the marker line of a block ends at the end of the line in front of it
                boolean markerLine = BLOCK_MARKER_LINE.matcher(text).region(endLine, endLineEnd).matches();
                if (markerLine || (rangeEnd <= (endLine + getLinePrefixLength(text, endLine, endLineEnd)))) rangeEnd = endLine - 1;
            }
            changed = (rangeStart != oldStart) || (rangeEnd != oldEnd);
        }
        if (rangeStart >= rangeEnd) return null;
        while ((rangeStart > 0) && (text.charAt(rangeStart) == markerCharacter) && (text.charAt(rangeStart - 1) == markerCharacter)) rangeStart--;
        while ((rangeEnd < text.length()) && (text.charAt(rangeEnd - 1) == markerCharacter) && (text.charAt(rangeEnd) == markerCharacter)) rangeEnd++;
        return new int[] {rangeStart, rangeEnd};
    }

    /**
     * Returns the rows of the marker character between the two offsets that are markers of the format, as their start and end offsets.<br>
     * Characters behind a backslash are no part of a row, because they are escaped.
     */
    @NotNull
    private static List<int[]> findMarkerRows(@NotNull InlineFormat format, @NotNull String text, int start, int end) {
        List<int[]> rows = new ArrayList<>();
        int index = start;
        while (index < end) {
            char c = text.charAt(index);
            if ((c == '\\') && ((index + 1) < end)) {
                index += 2;
                continue;
            }
            if (c != format.character) {
                index++;
                continue;
            }
            int rowEnd = index + 1;
            while ((rowEnd < end) && (text.charAt(rowEnd) == format.character)) rowEnd++;
            if (format.isMarkerRow(rowEnd - index)) rows.add(new int[] {index, rowEnd});
            index = rowEnd;
        }
        return rows;
    }

    /**
     * Aligns the selected lines, or the line of the cursor if nothing is selected.
     *
     * <p>The lines get the alignment by putting them into an aligned block, which is a marker line in front of them and one behind them. Blocks can't be nested,
     * so aligning lines inside of a block splits it, and aligning lines next to a block of the same alignment joins them. Centering or right-aligning lines that have
     * that alignment already aligns them left again, like the shortcuts of most text editors do. Lines inside of code blocks and plain text keep their text as it is.</p>
     */
    @Nullable
    private static Edit align(@NotNull String text, int start, int end, @NotNull MarkdownLineAlignment alignment) {
        String[] lines = text.split(NEWLINE, -1);
        MarkdownLineAlignment[] alignments = MarkdownParser.getLineAlignments(text);
        int startLine = getLineIndex(text, start);
        int endLine = getLineIndex(text, end);
        // A selection that ends at the start of a line leaves that line out
        int lastSelectedLine = ((end > start) && (endLine > startLine) && (end == getLineStart(text, end))) ? (endLine - 1) : endLine;

        // The first and last selected line that is no marker line of a block
        int first = -1;
        int last = -1;
        for (int i = startLine; i <= lastSelectedLine; i++) {
            if (alignments[i] == null) continue;
            if (first == -1) first = i;
            last = i;
        }
        if (first == -1) return null;

        if (alignment != MarkdownLineAlignment.LEFT) {
            boolean allAligned = true;
            for (int i = first; i <= last; i++) {
                if ((alignments[i] != null) && (alignments[i] != alignment)) allAligned = false;
            }
            if (allAligned) alignment = MarkdownLineAlignment.LEFT;
        }

        // The lines whose blocks get built again: the blocks the first and last line are in, and the blocks right in front of and behind them, which might get joined
        int rebuildStart = first;
        if (alignments[first] != MarkdownLineAlignment.LEFT) {
            rebuildStart = findMarkerLineAbove(alignments, first);
        } else if ((first > 0) && (alignments[first - 1] == null)) {
            rebuildStart = findMarkerLineAbove(alignments, first - 1);
        }
        int rebuildEnd = last;
        if (alignments[last] != MarkdownLineAlignment.LEFT) {
            rebuildEnd = findMarkerLineBelow(alignments, last);
        } else if (((last + 1) < lines.length) && (alignments[last + 1] == null)) {
            rebuildEnd = findMarkerLineBelow(alignments, last + 1);
        }

        List<String> newLines = new ArrayList<>(lines.length + 4);
        // The index every line that is no marker line has afterwards. Only marker lines get added or taken away, so all other lines stay.
        int[] newLineIndexes = new int[lines.length];
        Arrays.fill(newLineIndexes, -1);
        for (int i = 0; i < rebuildStart; i++) {
            newLineIndexes[i] = newLines.size();
            newLines.add(lines[i]);
        }
        MarkdownLineAlignment openBlock = MarkdownLineAlignment.LEFT;
        for (int i = rebuildStart; i <= rebuildEnd; i++) {
            if (alignments[i] == null) continue;
            MarkdownLineAlignment lineAlignment = ((i >= first) && (i <= last)) ? alignment : alignments[i];
            if (lineAlignment != openBlock) {
                if (openBlock != MarkdownLineAlignment.LEFT) newLines.add(getBlockMarker(openBlock));
                if (lineAlignment != MarkdownLineAlignment.LEFT) newLines.add(getBlockMarker(lineAlignment));
                openBlock = lineAlignment;
            }
            newLineIndexes[i] = newLines.size();
            newLines.add(lines[i]);
        }
        if (openBlock != MarkdownLineAlignment.LEFT) newLines.add(getBlockMarker(openBlock));
        for (int i = rebuildEnd + 1; i < lines.length; i++) {
            newLineIndexes[i] = newLines.size();
            newLines.add(lines[i]);
        }

        String newText = String.join(NEWLINE, newLines);
        if (newText.equals(text)) return null;

        // The selection keeps its place in the lines that stay. Where it starts or ends in a marker line, it starts or ends with the aligned lines instead.
        int[] newLineStarts = getLineStarts(newLines);
        int newStart = (alignments[startLine] != null) ? (newLineStarts[newLineIndexes[startLine]] + (start - getLineStart(text, start))) : newLineStarts[newLineIndexes[first]];
        int newEnd = (alignments[endLine] != null) ? (newLineStarts[newLineIndexes[endLine]] + (end - getLineStart(text, end))) : (newLineStarts[newLineIndexes[last]] + lines[last].length());
        return new Edit(newText, newStart, Math.max(newStart, newEnd));
    }

    @NotNull
    private static String getBlockMarker(@NotNull MarkdownLineAlignment alignment) {
        return (alignment == MarkdownLineAlignment.CENTERED) ? CENTERED_BLOCK_MARKER : RIGHT_BLOCK_MARKER;
    }

    /**
     * Returns the index of the marker line above the given line that is closest to it. Lines inside of a block and the marker line that closes it always have one.
     */
    private static int findMarkerLineAbove(@NotNull MarkdownLineAlignment[] alignments, int line) {
        for (int i = line - 1; i >= 0; i--) {
            if (alignments[i] == null) return i;
        }
        return 0;
    }

    /**
     * Returns the index of the marker line below the given line that is closest to it. Lines inside of a block and the marker line that opens it always have one.
     */
    private static int findMarkerLineBelow(@NotNull MarkdownLineAlignment[] alignments, int line) {
        for (int i = line + 1; i < alignments.length; i++) {
            if (alignments[i] == null) return i;
        }
        return alignments.length - 1;
    }

    /**
     * Returns how many of the given character stand in a row right in front of the offset, without going in front of the limit.
     */
    private static int countRowBefore(@NotNull String text, int offset, char character, int limit) {
        int index = offset;
        while ((index > limit) && (text.charAt(index - 1) == character)) index--;
        return offset - index;
    }

    /**
     * Returns how many of the given character stand in a row from the offset on, without reaching the limit.
     */
    private static int countRowFrom(@NotNull String text, int offset, char character, int limit) {
        int index = offset;
        while ((index < limit) && (text.charAt(index) == character)) index++;
        return index - offset;
    }

    private static int getLinePrefixLength(@NotNull String text, int lineStart, int lineEnd) {
        Matcher matcher = LINE_PREFIX.matcher(text).region(lineStart, lineEnd);
        return matcher.lookingAt() ? (matcher.end() - lineStart) : 0;
    }

    private static int getLineStart(@NotNull String text, int offset) {
        return text.lastIndexOf(NEWLINE_CHAR, offset - 1) + 1;
    }

    private static int getLineEnd(@NotNull String text, int offset) {
        int lineEnd = text.indexOf(NEWLINE_CHAR, offset);
        return (lineEnd != -1) ? lineEnd : text.length();
    }

    private static int getLineIndex(@NotNull String text, int offset) {
        int line = 0;
        for (int i = 0; i < offset; i++) {
            if (text.charAt(i) == NEWLINE_CHAR) line++;
        }
        return line;
    }

    @NotNull
    private static int[] getLineStarts(@NotNull List<String> lines) {
        int[] starts = new int[lines.size()];
        int offset = 0;
        for (int i = 0; i < lines.size(); i++) {
            starts[i] = offset;
            offset += lines.get(i).length() + 1;
        }
        return starts;
    }

    /**
     * The changed text of a {@link TextEditorWindowBody} and what is selected in it afterwards.
     *
     * @param selectionStart The offset of the start of the selection, which is the offset of the cursor if nothing is selected.
     * @param selectionEnd The offset of the end of the selection and of the cursor.
     */
    public record Edit(@NotNull String text, int selectionStart, int selectionEnd) {
    }

    public enum Shortcut {

        BOLD("b"),
        ITALIC("i"),
        UNDERLINE("u"),
        ALIGN_LEFT("l"),
        ALIGN_CENTER("e"),
        ALIGN_RIGHT("r");

        @NotNull
        private final String keyName;

        /**
         * @param keyName The name of the key that triggers the shortcut together with the GUI shortcut modifier, the way the key names of the input utilities are.
         */
        Shortcut(@NotNull String keyName) {
            this.keyName = keyName;
        }

        /**
         * Returns the name of the shortcut that its entries in menus and their localizations get named after.
         */
        @NotNull
        public String getName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        @Nullable
        public static Shortcut byKeyName(@Nullable String keyName) {
            for (Shortcut shortcut : values()) {
                if (shortcut.keyName.equals(keyName)) return shortcut;
            }
            return null;
        }

    }

    /**
     * The formats that start and end inside of a line, and how their markers look in a row of their marker character.
     */
    private enum InlineFormat {

        /** Two asterisks. A row of three is the marker of bold and italic text at once. */
        BOLD("**", '*') {
            @Override
            boolean isMarkerRow(int rowLength) {
                return rowLength >= 2;
            }
        },
        /** One asterisk. A row of three is the marker of bold and italic text at once. */
        ITALIC("*", '*') {
            @Override
            boolean isMarkerRow(int rowLength) {
                return (rowLength % 2) == 1;
            }
        },
        /** Two plus signs. Every other row of plus signs is text. */
        UNDERLINE("++", '+') {
            @Override
            boolean isMarkerRow(int rowLength) {
                return rowLength == 2;
            }
        };

        @NotNull
        final String marker;
        final char character;

        InlineFormat(@NotNull String marker, char character) {
            this.marker = marker;
            this.character = character;
        }

        /**
         * Returns if a row of the marker character that is this long holds a marker of this format.
         */
        abstract boolean isMarkerRow(int rowLength);

    }

}
