package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTableCellTest {

    private static final int CHAR_WIDTH = 6;

    @Test
    void cellWithTheTextThatMarksTableFragmentsIsNormalText() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("| a | [TABLE] |\n| --- | --- |\n| b | c |", 400);
        renderer.tick();
        MarkdownTextFragment table = renderer.tableFragment(0);
        MarkdownTextFragment cell = MarkdownTestRenderer.fragmentContaining(table.tableContext.rows.get(0).cells.get(1).fragments, "[TABLE]");

        assertTrue(table.isTable());
        assertFalse(cell.isTable());
        assertEquals(1, renderer.lines.size());
        assertEquals(("[TABLE]".length() * CHAR_WIDTH) + (renderer.getTableCellPadding() * 2), table.tableContext.columnWidths[1]);
        assertEquals("[TABLE]".length() * CHAR_WIDTH, cell.getRenderWidth());
        assertFalse(cell.isMouseOver(-100, -100));

        // Everything that walks into the cells of tables has to get past that cell
        cell.setHovered(true);
        renderer.resetHovered();
        assertFalse(cell.hovered);
        assertTrue(renderer.children().isEmpty());
    }

    @Test
    void textThatMarksTableFragmentsOutsideOfATableIsNormalText() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("[TABLE]", 400);
        renderer.tick();

        assertFalse(renderer.fragments.get(0).isTable());
        assertEquals("[TABLE]".length() * CHAR_WIDTH, renderer.fragments.get(0).getRenderWidth());
    }

}
