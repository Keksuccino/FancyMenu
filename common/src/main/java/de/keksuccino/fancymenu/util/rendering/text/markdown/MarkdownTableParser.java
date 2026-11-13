package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownSyntax.*;

/**
 * Finds the tables of a Markdown text and parses their rows and cells.
 */
final class MarkdownTableParser {

    /**
     * A table of the text.
     *
     * @param start The index of the first character of the first line of the table.
     * @param end The index behind the last line of the table, which is the line break that ends that line or the end of the text.
     */
    record Table(int start, int end, @NotNull MarkdownTextFragment.TableContext tableContext) {
    }

    private MarkdownTableParser() {
    }

    /**
     * Finds all tables of the text and parses their cells. The tables come in the order of their lines.
     */
    @NotNull
    static List<Table> findTables(@NotNull MarkdownRenderer renderer, @NotNull String markdownText) {
        // Tables need pipes
        if (markdownText.indexOf(VERTICAL_BAR_CHAR) == -1) return Collections.emptyList();

        List<Table> tables = new ArrayList<>();
        String[] lines = markdownText.split(NEWLINE);
        // Index of the current line in the text
        int lineStart = 0;

        for (int i = 0; i < lines.length; i++) {
            String currentLine = lines[i];
            String nextLine = (i + 1 < lines.length) ? lines[i + 1] : null;

            // Check if this is the start of a table
            if (isTableRow(currentLine) && nextLine != null && isTableSeparatorLine(nextLine)) {
                int tableStart = lineStart;
                // Parse the table
                MarkdownTextFragment.TableContext tableContext = new MarkdownTextFragment.TableContext();
                List<MarkdownTextFragment.TableCell.TableCellAlignment> alignments = parseTableAlignments(nextLine);

                // Parse header row
                MarkdownTextFragment.TableRow headerRow = new MarkdownTextFragment.TableRow(renderer);
                headerRow.isHeader = true;
                tableContext.hasHeader = true;
                addTableCells(renderer, tableContext, headerRow, currentLine, alignments);
                tableContext.rows.add(headerRow);

                // Skip the separator line
                lineStart += currentLine.length() + nextLine.length() + 2;
                i += 2;

                // Parse data rows
                while (i < lines.length && isTableRow(lines[i])) {
                    MarkdownTextFragment.TableRow dataRow = new MarkdownTextFragment.TableRow(renderer);
                    addTableCells(renderer, tableContext, dataRow, lines[i], alignments);
                    tableContext.rows.add(dataRow);
                    lineStart += lines[i].length() + 1;
                    i++;
                }

                // Every line of the table was counted with a line break behind it, and the one behind its last line is no part of the table
                tables.add(new Table(tableStart, lineStart - 1, tableContext));
                i--; // Adjust because the loop will increment
            } else {
                lineStart += currentLine.length() + 1;
            }
        }

        return tables;
    }

    /**
     * Parses the cells of the given line of a table and adds them to the given row of it.
     *
     * @param alignments The alignments of the columns of the table. Cells in columns behind the last one keep their default alignment.
     */
    static void addTableCells(@NotNull MarkdownRenderer renderer, @NotNull MarkdownTextFragment.TableContext tableContext, @NotNull MarkdownTextFragment.TableRow row, @NotNull String line, @NotNull List<MarkdownTextFragment.TableCell.TableCellAlignment> alignments) {
        List<String> cells = parseTableRow(line);
        for (int j = 0; j < cells.size(); j++) {
            MarkdownTextFragment.TableCell cell = new MarkdownTextFragment.TableCell();
            if (j < alignments.size()) {
                cell.alignment = alignments.get(j);
            }
            // Parse the cell content as markdown
            List<MarkdownTextFragment> cellFragments = parseTableCell(renderer, cells.get(j));
            for (MarkdownTextFragment frag : cellFragments) {
                frag.tableContext = tableContext;
            }
            cell.fragments.addAll(cellFragments);
            row.cells.add(cell);
        }
    }

    /**
     * Parses the content of a table cell.<br>
     * A cell is a single line, so it can't hold a table itself.
     */
    @NotNull
    static List<MarkdownTextFragment> parseTableCell(@NotNull MarkdownRenderer renderer, @NotNull String cellText) {
        return MarkdownParser.parseFragments(renderer, cellText, true, Collections.emptyList());
    }

    static boolean isTableRow(@NotNull String line) {
        if (line.indexOf(VERTICAL_BAR_CHAR) == -1) return false;
        String structuralLine = splitLeadingFormattingTokens(line.trim()).content.trim();
        return containsUnescapedPipe(structuralLine) && !isTableSeparatorLine(line);
    }

    static boolean isTableSeparatorLine(@NotNull String line) {
        if (line.indexOf(VERTICAL_BAR_CHAR) == -1) return false;
        String structuralLine = splitLeadingFormattingTokens(line.trim()).content.trim();
        if (!containsUnescapedPipe(structuralLine)) {
            return false;
        }
        return hasOnlyTableSeparatorCells(structuralLine);
    }

    /**
     * Returns if the line holds nothing but table separator cells, which are rows of dashes with an optional colon at each end.
     * The cells are split by pipes, with optional whitespace around the pipes and optional pipes at both ends of the line.
     *
     * <p>This reads the line in a loop on purpose. A regular expression goes one call deeper for every cell, which overflows
     * the stack for lines with thousands of cells.</p>
     */
    static boolean hasOnlyTableSeparatorCells(@NotNull String line) {
        int length = line.length();
        int index = skipWhitespace(line, 0);
        if ((index < length) && (line.charAt(index) == VERTICAL_BAR_CHAR)) index = skipWhitespace(line, index + 1);
        while (true) {
            if ((index < length) && (line.charAt(index) == COLON_CHAR)) index++;
            int dashesStart = index;
            while ((index < length) && (line.charAt(index) == MINUS_CHAR)) index++;
            if (index == dashesStart) return false;
            if ((index < length) && (line.charAt(index) == COLON_CHAR)) index++;
            index = skipWhitespace(line, index);
            if (index == length) return true;
            if (line.charAt(index) != VERTICAL_BAR_CHAR) return false;
            index = skipWhitespace(line, index + 1);
            if (index == length) return true;
        }
    }

    /**
     * Returns the index of the first character at or after the given index that is no space, tab, line break, vertical tab, form feed or carriage return.
     */
    static int skipWhitespace(@NotNull String text, int index) {
        while (index < text.length()) {
            char c = text.charAt(index);
            if ((c != SPACE_CHAR) && ((c < '\t') || (c > '\r'))) break;
            index++;
        }
        return index;
    }

    @NotNull
    static List<String> parseTableRow(@NotNull String line) {
        List<String> cells = new ArrayList<>();
        LeadingFormattingTokens leadingFormatting = splitLeadingFormattingTokens(line.trim());
        String trimmed = leadingFormatting.content.trim();
        if (trimmed.startsWith(VERTICAL_BAR)) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith(VERTICAL_BAR)) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        List<String> parts = splitByUnescapedPipes(trimmed);
        parts.forEach(part -> cells.add(part.trim()));
        if (!leadingFormatting.tokens.isEmpty() && !cells.isEmpty()) cells.set(0, leadingFormatting.tokens + cells.get(0));
        return cells;
    }

    @NotNull
    static List<MarkdownTextFragment.TableCell.TableCellAlignment> parseTableAlignments(@NotNull String separatorLine) {
        List<MarkdownTextFragment.TableCell.TableCellAlignment> alignments = new ArrayList<>();
        String trimmed = splitLeadingFormattingTokens(separatorLine.trim()).content.trim();
        if (trimmed.startsWith(VERTICAL_BAR)) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith(VERTICAL_BAR)) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        List<String> parts = splitByUnescapedPipes(trimmed);
        for (String part : parts) {
            String cell = part.trim();
            if (cell.startsWith(COLON) && cell.endsWith(COLON)) {
                alignments.add(MarkdownTextFragment.TableCell.TableCellAlignment.CENTER);
            } else if (cell.endsWith(COLON)) {
                alignments.add(MarkdownTextFragment.TableCell.TableCellAlignment.RIGHT);
            } else {
                alignments.add(MarkdownTextFragment.TableCell.TableCellAlignment.LEFT);
            }
        }
        return alignments;
    }

    @NotNull
    private static LeadingFormattingTokens splitLeadingFormattingTokens(@NotNull String line) {
        int index = 0;
        while (((index + 1) < line.length()) && isFormattingCode(line, index)) index += 2;
        return new LeadingFormattingTokens(line.substring(0, index), line.substring(index));
    }

    private record LeadingFormattingTokens(@NotNull String tokens, @NotNull String content) {
    }

    static boolean containsUnescapedPipe(@NotNull String line) {
        int length = line.length();
        for (int i = 0; i < length; i++) {
            if ((line.charAt(i) == VERTICAL_BAR_CHAR) && !isCharacterEscaped(line, i)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    static List<String> splitByUnescapedPipes(@NotNull String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int length = line.length();
        for (int i = 0; i < length; i++) {
            char c = line.charAt(i);
            if ((c == VERTICAL_BAR_CHAR) && !isCharacterEscaped(line, i)) {
                parts.add(current.toString());
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        parts.add(current.toString());
        return parts;
    }

}
