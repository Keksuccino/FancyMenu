package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownTableGrid;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTableStyleTest {

    private static final int CHAR_WIDTH = 6;
    private static final int LINE_HEIGHT = 9;
    private static final String TABLE = "| Item | Old | New |\n|:-----|----:|:---:|\n| Copper Pick | 12 | 14 |\n| Oil Flask | 30 | 24 |\n| Glow Berry | 5 | 8 |";

    @Test
    void pixelLookBuildsCompactTablesInsideAFrame() {
        MarkdownTestRenderer renderer = pixelRenderer(TABLE);
        renderer.tick();
        MarkdownTextFragment table = renderer.tableFragment(0);
        MarkdownTextFragment.TableContext context = table.tableContext;

        // Columns are as wide as the letters of their widest cell with 4 pixels of padding on both sides, and rows are 13 pixels tall
        float first = ("Copper Pick".length() * CHAR_WIDTH) - 1 + 8;
        float second = ("Old".length() * CHAR_WIDTH) - 1 + 8;
        assertArrayEquals(new float[]{first, second, second}, context.columnWidths);
        for (MarkdownTextFragment.TableRow row : context.rows) {
            assertEquals(13, row.height);
        }
        // The frame is 1 pixel thick, and so is the line below the header
        assertEquals(first + second + second + 2, table.getRenderWidth());
        assertEquals(1 + 13 + 1 + (3 * 13) + 1, table.getRenderHeight());
        assertEquals(1, context.rows.get(0).offsetY);
        assertEquals(1 + 13 + 1, context.rows.get(1).offsetY);
        assertEquals(1 + 13 + 1 + 13, context.rows.get(2).offsetY);
    }

    @Test
    void pixelLookAlignsTheLettersOfCellsInsideTheirPaddings() {
        MarkdownTestRenderer renderer = pixelRenderer(TABLE);
        renderer.setX(30).setY(50);
        renderer.tick();
        MarkdownTextFragment table = renderer.tableFragment(0);
        MarkdownTextFragment.TableContext context = table.tableContext;

        MarkdownTablePainter.positionCells(table);

        MarkdownTextFragment left = context.rows.get(2).cells.get(0).fragments.get(0);
        MarkdownTextFragment right = context.rows.get(3).cells.get(1).fragments.get(0);
        MarkdownTextFragment centered = context.rows.get(3).cells.get(2).fragments.get(0);
        float secondColumn = table.x + 1 + context.columnWidths[0];
        float thirdColumn = secondColumn + context.columnWidths[1];
        assertEquals(table.x + 1 + 4, left.getTextX());
        assertEquals(table.y + context.rows.get(2).offsetY + 3, left.getTextY());
        // The letters of a right-aligned cell end 4 pixels in front of the end of the column. The empty column behind the last letter is no part of them.
        assertEquals(thirdColumn - 4, right.getTextX() + right.getTextWidth() - 1);
        float lettersWidth = centered.getTextWidth() - 1;
        assertEquals(thirdColumn + ((context.columnWidths[2] - lettersWidth) / 2), centered.getTextX());
    }

    @Test
    void headerOfThePixelLookIsBoldAndHasItsOwnColor() {
        MarkdownTestRenderer renderer = pixelRenderer(TABLE);
        renderer.tick();
        MarkdownTextFragment.TableContext context = renderer.tableFragment(0).tableContext;
        MarkdownTextFragment header = context.rows.get(0).cells.get(0).fragments.get(0);
        MarkdownTextFragment cell = context.rows.get(1).cells.get(0).fragments.get(0);

        assertTrue(header.tableHeader);
        assertFalse(cell.tableHeader);
        Style headerStyle = header.getRenderComponent().getStyle();
        assertTrue(headerStyle.isBold());
        assertEquals(0xFFC94D, headerStyle.getColor().getValue());
        assertFalse(cell.getRenderComponent().getStyle().isBold());
        assertNull(cell.getRenderComponent().getStyle().getColor());
        assertSame(renderer.getStyle().tableHeaderBackgroundColor, MarkdownTablePainter.getRowBackgroundColor(renderer, context, 0));
    }

    @Test
    void tableWithoutShownHeaderHasNoHeaderLookAndNoLineBelowItsFirstRow() {
        MarkdownTestRenderer renderer = pixelRenderer(TABLE);
        renderer.getStyle().tableShowHeader.set(false);
        renderer.tick();
        MarkdownTextFragment table = renderer.tableFragment(0);
        MarkdownTextFragment header = table.tableContext.rows.get(0).cells.get(0).fragments.get(0);

        assertFalse(header.getRenderComponent().getStyle().isBold());
        assertNull(header.getRenderComponent().getStyle().getColor());
        assertEquals(1 + (4 * 13) + 1, table.getRenderHeight());
        assertEquals(1 + 13, table.tableContext.rows.get(1).offsetY);
        assertSame(renderer.getStyle().tableRowBackgroundColor, MarkdownTablePainter.getRowBackgroundColor(renderer, table.tableContext, 0));
        assertSame(renderer.getStyle().tableAlternateRowBackgroundColor, MarkdownTablePainter.getRowBackgroundColor(renderer, table.tableContext, 1));
    }

    @Test
    void fullGridPutsItsLinesOntoTheCells() {
        MarkdownTestRenderer renderer = pixelRenderer(TABLE);
        renderer.getStyle().tableGrid.set(MarkdownTableGrid.FULL);
        renderer.tick();
        MarkdownTextFragment table = renderer.tableFragment(0);
        MarkdownTextFragment.TableContext context = table.tableContext;

        float columns = context.columnWidths[0] + context.columnWidths[1] + context.columnWidths[2];
        assertEquals(columns, table.getRenderWidth());
        assertEquals(0, context.rows.get(0).offsetY);
        assertEquals(13, context.rows.get(1).offsetY);
        // The table keeps the room of one line per row and one more
        assertEquals((4 * 13) + 5, table.getRenderHeight());
    }

    @Test
    void classicLookKeepsItsWideCells() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TABLE, 400);
        renderer.setX(30).setY(50);
        renderer.tick();
        MarkdownTextFragment table = renderer.tableFragment(0);
        MarkdownTextFragment.TableContext context = table.tableContext;

        MarkdownTablePainter.positionCells(table);

        assertEquals(("Copper Pick".length() * CHAR_WIDTH) + 16, context.columnWidths[0]);
        assertEquals(LINE_HEIGHT + 16, context.rows.get(0).height);
        MarkdownTextFragment header = context.rows.get(0).cells.get(0).fragments.get(0);
        assertEquals(table.x + 8, header.getTextX());
        assertEquals(table.y + 8, header.getTextY());
        assertFalse(header.getRenderComponent().getStyle().isBold());
        MarkdownTextFragment right = context.rows.get(3).cells.get(1).fragments.get(0);
        assertEquals(table.x + context.columnWidths[0] + context.columnWidths[1] - 8, right.getTextX() + right.getTextWidth());
    }

    @Test
    void settersOfTheRendererKeepTheOptionsTheyCoverInSync() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer();
        DrawableColor color = DrawableColor.of(1, 2, 3);

        renderer.setTableCellPadding(5);
        renderer.setTableLineThickness(3);
        renderer.setTableLineColor(color);

        assertEquals(5, renderer.getStyle().tableCellPaddingLeft.get());
        assertEquals(5, renderer.getStyle().tableCellPaddingRight.get());
        assertEquals(5, renderer.getStyle().tableCellPaddingTop.get());
        assertEquals(5, renderer.getStyle().tableCellPaddingBottom.get());
        assertEquals(3, renderer.getStyle().tableLineThickness.get());
        assertEquals(6, renderer.getStyle().tableHeaderSeparatorThickness.get());
        assertSame(color, renderer.getStyle().tableLineColor.get());
        assertSame(color, renderer.getStyle().tableHeaderSeparatorColor.get());
        assertEquals(5, renderer.getTableCellPadding());
    }

    private static MarkdownTestRenderer pixelRenderer(String text) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        return renderer;
    }

}
