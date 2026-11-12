package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.TableCell;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.TableRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.tableFragment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertFalse(table.mouseClicked(1.0D, table.parent.tableMargin + 1.0D, 0));
    }

}
