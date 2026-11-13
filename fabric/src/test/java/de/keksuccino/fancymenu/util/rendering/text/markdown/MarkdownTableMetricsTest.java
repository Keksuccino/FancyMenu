package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTableMetricsTest {

    private static final int CHAR_WIDTH = 6;
    private static final int LINE_HEIGHT = 9;
    private static final String TABLE = "| Name | Value |\n| --- | ---: |\n| a | longer cell |\n| bb | c |";

    @Test
    void tableSizesComeFromItsCells() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TABLE, 400);
        renderer.setTableCellPadding(5);
        renderer.setTableLineThickness(2);
        renderer.setTableMargin(3);
        renderer.tick();
        MarkdownTextFragment table = renderer.tableFragment(0);
        MarkdownTextFragment.TableContext context = table.tableContext;

        float firstColumn = ("Name".length() * CHAR_WIDTH) + 10;
        float secondColumn = ("longer cell".length() * CHAR_WIDTH) + 10;
        float rowHeight = LINE_HEIGHT + 10;
        assertArrayEquals(new float[]{firstColumn, secondColumn}, context.columnWidths);
        assertEquals(firstColumn + secondColumn, context.totalWidth);
        assertEquals(3, context.rows.size());
        for (MarkdownTextFragment.TableRow row : context.rows) {
            assertEquals(rowHeight, row.height);
            assertEquals(rowHeight, context.getRowHeight(row));
        }
        assertEquals(rowHeight * 3, context.totalRowHeight);
        assertEquals("longer cell".length() * CHAR_WIDTH, context.rows.get(1).cells.get(1).textWidth);
        assertEquals(firstColumn + secondColumn, table.getRenderWidth());
        assertEquals((rowHeight * 3) + (2 * 4), table.getRenderHeight());
        assertEquals(table.getRenderWidth(), table.parentLine.getLineWidth());
        assertEquals(table.getRenderHeight(), table.parentLine.getLineHeight());
        // The margin is space around the line of the table and no part of it
        assertEquals(2 + 3, table.parentLine.offsetY);
        assertEquals(2 + 3 + table.getRenderHeight() + 3 + 2 + 2, renderer.getRealHeight());
    }

    @Test
    void tableGetsMeasuredOncePerLayout() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TABLE, 400);
        renderer.tick();
        MarkdownTextFragment table = renderer.tableFragment(0);
        MarkdownTextFragment.TableContext context = table.tableContext;
        float[] columnWidths = context.columnWidths;
        float width = table.getRenderWidth();
        float height = table.getRenderHeight();

        // The next measurement would show this, so the sizes staying the same proves that nothing measured the table again
        MarkdownTextFragment cellFragment = MarkdownTestRenderer.fragmentContaining(context.rows.get(1).cells.get(1).fragments, "longer");
        cellFragment.unscaledTextWidth += 100;
        for (int i = 0; i < 5; i++) {
            renderer.tick();
            renderer.getRealWidth();
            table.isMouseOver(table.x + 1, table.y + renderer.getTableMargin() + 1);
            table.getRenderWidth();
            table.getRenderHeight();
        }

        assertSame(columnWidths, context.columnWidths);
        assertEquals(width, table.getRenderWidth());
        assertEquals(height, table.getRenderHeight());
        assertEquals(1, renderer.layouts);

        renderer.setTableCellPadding(renderer.getTableCellPadding() + 1);
        renderer.tick();

        assertEquals(2, renderer.layouts);
        assertEquals(width + 100 + 4, table.getRenderWidth());
        assertEquals(height + 6, table.getRenderHeight());
    }

    @Test
    void mouseIsOverTheTableButNotOverTheSpaceAroundIt() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TABLE, 400);
        renderer.setTableMargin(10);
        renderer.setX(20).setY(40);
        renderer.tick();
        MarkdownTextFragment table = renderer.tableFragment(0);
        float tableHeight = table.getRenderHeight();

        assertEquals(40 + 2 + 10, table.y);
        assertTrue(table.isMouseOver(table.x, table.y));
        assertTrue(table.isMouseOver(table.x + table.getRenderWidth() - 0.5F, table.y + tableHeight - 0.5F));
        assertFalse(table.isMouseOver(table.x, table.y - 1));
        assertFalse(table.isMouseOver(table.x + table.getRenderWidth(), table.y));
        assertFalse(table.isMouseOver(table.x, table.y + tableHeight));
    }

    @Test
    void textScaleChangesTheTableSizes() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TABLE, 400);
        renderer.tick();
        MarkdownTextFragment table = renderer.tableFragment(0);
        float width = table.getRenderWidth();
        float padding = renderer.getTableCellPadding();

        renderer.setTextBaseScale(2.0F);
        renderer.tick();

        assertSame(table, renderer.tableFragment(0));
        assertEquals(((width - (padding * 4)) * 2) + (padding * 4), table.getRenderWidth());
    }

}
