package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.HeadlineType;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.TableCell;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.TableRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.TEST_FONT;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.tableFragment;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownParserTableTest {

    /** The text the parser gives the fragment that stands for a table. */
    private static final String TABLE_FRAGMENT_TEXT = "[TABLE]";

    @Test
    void cellWithTheTextOfTheTableFragmentIsNoTable() {
        List<MarkdownTextFragment> fragments = parse("| " + TABLE_FRAGMENT_TEXT + " | b |\n| --- | --- |\n| c | " + TABLE_FRAGMENT_TEXT + " |");
        MarkdownTextFragment table = tableFragment(fragments);

        assertEquals(1, fragments.size());
        assertEquals(TABLE_FRAGMENT_TEXT, table.text);
        assertTrue(table.isTable());
        assertEquals(TABLE_FRAGMENT_TEXT, joinText(table.tableContext.rows.get(0).cells.get(0).fragments));
        assertEquals(TABLE_FRAGMENT_TEXT, joinText(table.tableContext.rows.get(1).cells.get(1).fragments));
        for (TableRow row : table.tableContext.rows) {
            for (TableCell cell : row.cells) {
                for (MarkdownTextFragment fragment : cell.fragments) {
                    assertSame(table.tableContext, fragment.tableContext);
                    assertFalse(fragment.isTable());
                }
            }
        }
    }

    @Test
    void clickOnATableDoesNotRunInCirclesThroughACellWithTheTextOfTheTableFragment() {
        MarkdownTextFragment table = tableFragment(parse("| " + TABLE_FRAGMENT_TEXT + " | b |\n| --- | --- |"));

        // The click is inside the table and on the cell fragment, which both sit at the origin
        assertFalse(table.mouseClicked(1.0D, table.parent.getTableMargin() + 1.0D, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[TABLE_0]", "[TABLE_1]", "[TABLE_x]", "[TABLE_", "[TABLE_0] and more", "[TABLE]"})
    void linesThatLookLikeTableMarkersStayText(String line) {
        List<MarkdownTextFragment> withoutTable = parse("above\n" + line + "\nbelow");
        List<MarkdownTextFragment> withTable = parse("| a | b |\n| --- | --- |\n" + line + "\nbelow");
        List<MarkdownTextFragment> withoutMarkdown = MarkdownParser.parse(new MarkdownRenderer(TEST_FONT), "above\n" + line + "\nbelow", false);

        assertEquals("above" + line + "below", joinText(withoutTable));
        assertTrue(withoutTable.stream().noneMatch(MarkdownTextFragment::isTable));
        assertEquals(TABLE_FRAGMENT_TEXT + line + "below", joinText(withTable));
        assertEquals(1, withTable.stream().filter(MarkdownTextFragment::isTable).count());
        assertTrue(withTable.get(0).isTable());
        assertEquals("above" + line + "below", joinText(withoutMarkdown));
    }

    @Test
    void cellThatLooksLikeATableMarkerKeepsItsText() {
        List<MarkdownTextFragment> fragments = parse("| a | b |\n| --- | --- |\n\n| [TABLE_0] | d |\n| --- | --- |");
        List<MarkdownTextFragment> tables = fragments.stream().filter(MarkdownTextFragment::isTable).toList();

        assertEquals(2, tables.size());
        List<TableCell> cells = tables.get(1).tableContext.rows.get(0).cells;
        assertEquals(2, cells.size());
        assertEquals("[TABLE_0]", joinText(cells.get(0).fragments));
        assertTrue(cells.get(0).fragments.stream().noneMatch(MarkdownTextFragment::isTable));
        assertEquals("d", joinText(cells.get(1).fragments));
    }

    @Test
    void tableDoesNotCloseAnUnderscoreInFrontOfIt() {
        List<MarkdownTextFragment> fragments = parse("snake_case\n| a | b |\n| --- | --- |\nafter");

        assertEquals(List.of("snake_case", TABLE_FRAGMENT_TEXT, "after"), texts(fragments));
        for (MarkdownTextFragment fragment : fragments) assertFalse(fragment.italic);
    }

    @Test
    void tableDirectlyBelowAHeadlineKeepsTheHeadlineInItsOwnLine() {
        List<MarkdownTextFragment> direct = parse("# Title\n| a | b |\n| --- | --- |\ntext");
        List<MarkdownTextFragment> withEmptyLine = parse("# Title\n\n| a | b |\n| --- | --- |\ntext");

        for (List<MarkdownTextFragment> fragments : List.of(direct, withEmptyLine)) {
            assertEquals(List.of("Title", TABLE_FRAGMENT_TEXT, "text"), texts(fragments));
            assertEquals(HeadlineType.BIGGEST, fragments.get(0).headlineType);
            assertTrue(fragments.get(0).naturalLineBreakAfter);
            assertTrue(fragments.get(1).isTable());
            assertEquals(HeadlineType.NONE, fragments.get(2).headlineType);
        }
    }

    @Test
    void tablesKeepTheirPlaceBetweenTheLinesAroundThem() {
        List<MarkdownTextFragment> fragments = parse("first\n| a | b |\n| --- | --- |\n| c | d |\nsecond\n\n| e |\n| --- |\n| f |\n| g |");
        List<MarkdownTextFragment> tables = fragments.stream().filter(MarkdownTextFragment::isTable).toList();

        assertEquals(List.of("first", TABLE_FRAGMENT_TEXT, "second", "", TABLE_FRAGMENT_TEXT), texts(fragments));
        assertEquals(2, tables.get(0).tableContext.rows.size());
        assertEquals("c", joinText(tables.get(0).tableContext.rows.get(1).cells.get(0).fragments));
        assertEquals(3, tables.get(1).tableContext.rows.size());
        assertEquals("g", joinText(tables.get(1).tableContext.rows.get(2).cells.get(0).fragments));
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", ";;;"})
    void tableSyntaxInsideACodeOrPlainTextBlockStaysText(String blockMarker) {
        List<MarkdownTextFragment> fragments = parse("before\n" + blockMarker + "\n| a | **b** |\n| --- | --- |\n| c | d |\nlast line\n" + blockMarker + "\nafter");
        List<MarkdownTextFragment> block = fragments.subList(1, fragments.size() - 1);

        assertTrue(fragments.stream().noneMatch(MarkdownTextFragment::isTable));
        assertEquals("before", fragments.get(0).text);
        assertEquals("after", fragments.get(fragments.size() - 1).text);
        assertEquals("| a | **b** || --- | --- || c | d |last line", joinText(block));
        // One line of the block for every line of the table, and one for the line behind it
        assertEquals(List.of("|", "|", "|", "line"), block.stream().filter(fragment -> fragment.naturalLineBreakAfter).map(fragment -> fragment.text).toList());
        for (MarkdownTextFragment fragment : block) {
            assertTrue(fragment.plainText || ((fragment.codeBlockContext != null) && !fragment.codeBlockContext.singleLine));
            assertFalse(fragment.bold);
            assertNull(fragment.tableContext);
        }
        assertNull(fragments.get(fragments.size() - 1).codeBlockContext);
        assertFalse(fragments.get(fragments.size() - 1).plainText);
    }

    @Test
    void tableSyntaxInsidePlainTextThatSpansLinesStaysText() {
        List<MarkdownTextFragment> fragments = parse("start ;;\n| x | y |\n| --- | --- |\n;; end");

        assertTrue(fragments.stream().noneMatch(MarkdownTextFragment::isTable));
        assertEquals("start | x | y || --- | --- | end", joinText(fragments));
        assertFalse(fragmentContaining(fragments, "start").plainText);
        assertTrue(fragmentContaining(fragments, "x").plainText);
        assertTrue(fragmentContaining(fragments, "---").plainText);
        assertFalse(fragmentContaining(fragments, "end").plainText);
    }

    @Test
    void tablesAroundACodeBlockWithTableSyntaxStayTables() {
        List<MarkdownTextFragment> fragments = parse("| first |\n| --- |\n```\n| x |\n| --- |\n```\n| second |\n| --- |\n```\n| y |\n| --- |\n```");
        List<MarkdownTextFragment> tables = fragments.stream().filter(MarkdownTextFragment::isTable).toList();

        assertEquals(2, tables.size());
        assertEquals("first", joinText(tables.get(0).tableContext.rows.get(0).cells.get(0).fragments));
        assertEquals("second", joinText(tables.get(1).tableContext.rows.get(0).cells.get(0).fragments));
        assertSame(tables.get(0), fragments.get(0));
        assertEquals(TABLE_FRAGMENT_TEXT + "| x || --- |" + TABLE_FRAGMENT_TEXT + "| y || --- |", joinText(fragments));
        assertNotNull(fragmentContaining(fragments, "x").codeBlockContext);
        assertNotNull(fragmentContaining(fragments, "y").codeBlockContext);
        assertNotSame(fragmentContaining(fragments, "x").codeBlockContext, fragmentContaining(fragments, "y").codeBlockContext);
    }

    @Test
    void tableBehindACodeBlockMarkerWithoutClosingLineStaysATable() {
        List<MarkdownTextFragment> fragments = parse("```\n| a | b |\n| --- | --- |");

        assertEquals(1, fragments.stream().filter(MarkdownTextFragment::isTable).count());
        assertTrue(fragments.get(fragments.size() - 1).isTable());
    }

    @Test
    void tableSyntaxThatEndsTheTextInsideACodeBlockAddsNoLine() {
        // The line "```x" closes no code block, so the block runs to the end of the text
        List<MarkdownTextFragment> fragments = parse("```\ncode\n```x\n| a | b |\n| --- | --- |");

        assertTrue(fragments.stream().noneMatch(MarkdownTextFragment::isTable));
        // One line for every line of the text behind the marker that opens the block
        assertEquals(List.of("code", "```x", "|", "|"), fragments.stream().filter(fragment -> fragment.naturalLineBreakAfter).map(fragment -> fragment.text).toList());
        assertEquals("|", fragments.get(fragments.size() - 1).text);
    }

    @Test
    void closingMarkerOfAPlainTextBlockIsFoundBehindTableSyntaxInIt() {
        // The table lines are no part of the text the block markers get searched in, the lines around them are
        List<MarkdownTextFragment> fragments = parse(";;;\n| a |\n| --- |\n;;;\n**bold**");

        assertTrue(fragments.stream().noneMatch(MarkdownTextFragment::isTable));
        assertTrue(fragmentContaining(fragments, "a").plainText);
        assertFalse(fragmentContaining(fragments, "bold").plainText);
        assertTrue(fragmentContaining(fragments, "bold").bold);
    }

    @Test
    void tableTakesOverTheAlignmentOfItsBlock() {
        List<MarkdownTextFragment> fragments = parse("^^^\n| a | b |\n| --- | --- |\n^^^\n| c | d |\n| --- | --- |");
        List<MarkdownTextFragment> tables = fragments.stream().filter(MarkdownTextFragment::isTable).toList();

        assertEquals(2, fragments.size());
        assertEquals(MarkdownRenderer.MarkdownLineAlignment.CENTERED, tables.get(0).alignment);
        assertEquals(MarkdownRenderer.MarkdownLineAlignment.LEFT, tables.get(1).alignment);
    }

}
