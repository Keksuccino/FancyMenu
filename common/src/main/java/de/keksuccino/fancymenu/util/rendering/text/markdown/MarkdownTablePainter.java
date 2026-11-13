package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.TableCell;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.TableContext;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.TableRow;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownTableGrid;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * Measures and draws tables: their backgrounds, their lines, and the text of their cells.<br><br>
 *
 * The {@link MarkdownTableGrid} of the style decides how a table is built. With a full grid, the lines lie on the edges of the cells,
 * so the table is as big as its cells and the lines cover a part of their paddings. With a frame, the frame and the line below
 * the header take their own space, and the cells start behind them.
 */
final class MarkdownTablePainter {

    private MarkdownTablePainter() {
    }

    /**
     * Marks the fragments of the header cells of all tables in the given fragments, and measures them again if the style makes header text bold.
     * Has to run after every parse, because the parser builds the cells of a header like all other cells.
     */
    static void prepareHeaders(@NotNull MarkdownRenderer renderer, @NotNull List<MarkdownTextFragment> fragments) {
        boolean bold = renderer.style.tableShowHeader.get() && renderer.style.tableHeaderBold.get();
        for (MarkdownTextFragment tableFragment : fragments) {
            if (!tableFragment.isTable()) continue;
            for (TableRow row : tableFragment.tableContext.rows) {
                if (!row.isHeader) continue;
                for (TableCell cell : row.cells) {
                    for (MarkdownTextFragment fragment : cell.fragments) {
                        fragment.tableHeader = true;
                        if (bold) fragment.updateWidth();
                    }
                }
            }
        }
    }

    /**
     * Measures the columns, rows and cells of the table.
     */
    static void measure(@NotNull MarkdownRenderer renderer, @NotNull TableContext table) {
        MarkdownStyle style = renderer.style;
        MarkdownScaling scaling = renderer.scaling;
        float paddingHorizontal = scaling.sized(style.tableCellPaddingLeft.get()) + scaling.sized(style.tableCellPaddingRight.get());
        float paddingVertical = scaling.sized(style.tableCellPaddingTop.get()) + scaling.sized(style.tableCellPaddingBottom.get());
        int columnCount = table.rows.isEmpty() ? 0 : table.rows.get(0).cells.size();
        if (table.columnWidths.length != columnCount) {
            table.columnWidths = new float[columnCount];
        } else {
            Arrays.fill(table.columnWidths, 0.0F);
        }
        table.totalWidth = 0;
        table.totalRowHeight = 0;
        for (TableRow row : table.rows) {
            float textHeight = 0;
            for (int i = 0; i < row.cells.size(); i++) {
                TableCell cell = row.cells.get(i);
                float textWidth = 0;
                for (MarkdownTextFragment fragment : cell.fragments) {
                    // The paddings of inline code are part of the width of its fragments, like in a line of text
                    textWidth += fragment.getRenderWidth();
                    textHeight = Math.max(textHeight, fragment.getTextRenderHeight());
                }
                cell.textWidth = textWidth;
                if (i < columnCount) {
                    table.columnWidths[i] = Math.max(table.columnWidths[i], getVisibleTextWidth(renderer, cell) + paddingHorizontal);
                }
            }
            row.height = textHeight + paddingVertical;
            table.totalRowHeight += row.height;
        }
        for (float width : table.columnWidths) {
            table.totalWidth += width;
        }
        boolean frame = (style.tableGrid.get() == MarkdownTableGrid.FRAME);
        if (frame) table.totalWidth += getLineThickness(renderer) * 2;
        // The rows of a framed table start behind its frame, and the line below its header pushes the other rows down
        float rowOffsetY = frame ? getLineThickness(renderer) : 0.0F;
        for (int rowIndex = 0; rowIndex < table.rows.size(); rowIndex++) {
            TableRow row = table.rows.get(rowIndex);
            row.offsetY = rowOffsetY;
            rowOffsetY += row.height;
            if (rowIndex == 0) rowOffsetY += getHeaderSeparatorSpace(renderer, table);
        }
    }

    /**
     * Gives the fragments in the cells of the table their positions, for where the table is right now.
     */
    static void positionCells(@NotNull MarkdownTextFragment tableFragment) {
        MarkdownRenderer renderer = tableFragment.parent;
        MarkdownStyle style = renderer.style;
        MarkdownScaling scaling = renderer.scaling;
        TableContext table = tableFragment.tableContext;
        float paddingLeft = scaling.sized(style.tableCellPaddingLeft.get());
        float paddingRight = scaling.sized(style.tableCellPaddingRight.get());
        float paddingTop = scaling.sized(style.tableCellPaddingTop.get());
        float cellsX = getCellsX(renderer, tableFragment);
        for (TableRow row : table.rows) {
            float textY = tableFragment.y + row.offsetY + paddingTop;
            float cellX = cellsX;
            for (int cellIndex = 0; (cellIndex < row.cells.size()) && (cellIndex < table.columnWidths.length); cellIndex++) {
                TableCell cell = row.cells.get(cellIndex);
                float cellWidth = table.columnWidths[cellIndex];
                float freeWidth = cellWidth - paddingLeft - paddingRight - getVisibleTextWidth(renderer, cell);
                float alignmentOffset = 0;
                if (cell.alignment == TableCell.TableCellAlignment.CENTER) {
                    alignmentOffset = freeWidth / 2;
                } else if (cell.alignment == TableCell.TableCellAlignment.RIGHT) {
                    alignmentOffset = freeWidth;
                }
                float fragmentX = scaling.snapX(cellX + paddingLeft + scaling.snapOffset(alignmentOffset));
                for (MarkdownTextFragment fragment : cell.fragments) {
                    fragment.x = fragmentX;
                    fragment.y = textY;
                    fragmentX += fragment.getRenderWidth();
                }
                cellX += cellWidth;
            }
        }
    }

    /**
     * Returns where the cells of the table start, which is behind the frame of a framed table.
     */
    private static float getCellsX(@NotNull MarkdownRenderer renderer, @NotNull MarkdownTextFragment tableFragment) {
        return (renderer.style.tableGrid.get() == MarkdownTableGrid.FRAME) ? (tableFragment.x + getLineThickness(renderer)) : tableFragment.x;
    }

    /**
     * Returns the height of the table.
     */
    static float getHeight(@NotNull MarkdownRenderer renderer, @NotNull TableContext table) {
        float lineThickness = getLineThickness(renderer);
        if (renderer.style.tableGrid.get() == MarkdownTableGrid.FRAME) {
            return table.totalRowHeight + (lineThickness * 2) + getHeaderSeparatorSpace(renderer, table);
        }
        // The lines of a full grid lie on the cells, but the table keeps the room of one line per row and one more
        return table.totalRowHeight + (lineThickness * (table.rows.size() + 1));
    }

    /**
     * Returns the width of the text of a cell that columns get fitted to and that gets aligned inside of them.
     */
    private static float getVisibleTextWidth(@NotNull MarkdownRenderer renderer, @NotNull TableCell cell) {
        if (!renderer.style.trimGlyphSpacing.get() || cell.fragments.isEmpty()) return cell.textWidth;
        MarkdownTextFragment last = cell.fragments.get(cell.fragments.size() - 1);
        return Math.max(0.0F, cell.textWidth - (renderer.getFontMetrics().glyphSpacing * last.getScale()));
    }

    private static float getLineThickness(@NotNull MarkdownRenderer renderer) {
        return renderer.scaling.sized(renderer.style.tableLineThickness.get());
    }

    /**
     * Returns the space the line below the header takes between the header and the first row, which is none if the line lies on the cells.
     */
    private static float getHeaderSeparatorSpace(@NotNull MarkdownRenderer renderer, @NotNull TableContext table) {
        if ((renderer.style.tableGrid.get() != MarkdownTableGrid.FRAME) || !isHeaderRow(renderer, table, 0)) return 0.0F;
        return renderer.scaling.sized(renderer.style.tableHeaderSeparatorThickness.get());
    }

    /**
     * Returns if the row at the given index gets shown as the header of the table.
     */
    static boolean isHeaderRow(@NotNull MarkdownRenderer renderer, @NotNull TableContext table, int rowIndex) {
        return renderer.style.tableShowHeader.get() && (rowIndex < table.rows.size()) && table.rows.get(rowIndex).isHeader;
    }

    /**
     * Returns the background color of the row at the given index.<br>
     * The header row only looks like a header while the style shows table headers. Otherwise it is a row like every other
     * one, so the colors alternate from the first row of the table and not from the first row behind the header.
     */
    @NotNull
    static MarkdownStyleOption.ColorOption getRowBackgroundColor(@NotNull MarkdownRenderer renderer, @NotNull TableContext table, int rowIndex) {
        if (isHeaderRow(renderer, table, rowIndex)) return renderer.style.tableHeaderBackgroundColor;
        int alternatingIndex = isHeaderRow(renderer, table, 0) ? (rowIndex - 1) : rowIndex;
        return (renderer.style.tableAlternateRows.get() && ((alternatingIndex % 2) == 1)) ? renderer.style.tableAlternateRowBackgroundColor : renderer.style.tableRowBackgroundColor;
    }

    /**
     * Draws the table of the given table fragment.
     */
    static void render(@NotNull GuiGraphics graphics, @NotNull MarkdownTextFragment tableFragment, int mouseX, int mouseY, float partial) {
        MarkdownRenderer renderer = tableFragment.parent;
        MarkdownStyle style = renderer.style;
        TableContext table = tableFragment.tableContext;
        boolean frame = (style.tableGrid.get() == MarkdownTableGrid.FRAME);
        float lineThickness = getLineThickness(renderer);
        float cornerCut = renderer.scaling.sized(style.tableCornerRadius.get());
        float tableX = tableFragment.x;
        float tableY = tableFragment.y;
        float tableWidth = table.totalWidth;
        float tableHeight = getHeight(renderer, table);
        float cellsX = getCellsX(renderer, tableFragment);
        float cellsWidth = frame ? (tableWidth - (lineThickness * 2)) : tableWidth;
        float headerSeparatorSpace = getHeaderSeparatorSpace(renderer, table);

        // Fragments draw things that reach to fragments after them, like the background of inline code, so all of them get their position before the first one renders
        positionCells(tableFragment);

        MarkdownPaint.box(graphics, tableX, tableY, tableX + tableWidth, tableY + tableHeight, cornerCut, renderer.getRenderColor(style.tableBackgroundColor));

        float rowsEndY = tableY;
        for (int rowIndex = 0; rowIndex < table.rows.size(); rowIndex++) {
            TableRow row = table.rows.get(rowIndex);
            float rowY = tableY + row.offsetY;
            rowsEndY = rowY + row.height;
            int background = renderer.getRenderColor(getRowBackgroundColor(renderer, table, rowIndex));
            if ((background >>> 24) != 0) RenderingUtils.fillF(graphics, cellsX, rowY, cellsX + cellsWidth, rowsEndY, background);
            if ((rowIndex == 0) && (headerSeparatorSpace > 0.0F)) {
                RenderingUtils.fillF(graphics, cellsX, rowsEndY, cellsX + cellsWidth, rowsEndY + headerSeparatorSpace, renderer.getRenderColor(style.tableHeaderSeparatorColor));
            }
            for (TableCell cell : row.cells) {
                renderCell(graphics, cell, mouseX, mouseY, partial);
            }
        }

        int lineColor = renderer.getRenderColor(style.tableLineColor);
        if (frame) {
            MarkdownPaint.frame(graphics, tableX, tableY, tableX + tableWidth, tableY + tableHeight, lineThickness, cornerCut, lineColor, lineColor);
        } else {
            renderGrid(graphics, renderer, table, tableX, tableY, rowsEndY, lineThickness, lineColor);
        }
        RenderingUtils.resetShaderColor(graphics);
    }

    private static void renderCell(@NotNull GuiGraphics graphics, @NotNull TableCell cell, int mouseX, int mouseY, float partial) {
        MarkdownTextFragment codeStart = null;
        for (int i = 0; i < cell.fragments.size(); i++) {
            MarkdownTextFragment fragment = cell.fragments.get(i);
            if ((fragment.codeBlockContext == null) || !fragment.codeBlockContext.singleLine) continue;
            if ((codeStart == null) || (codeStart.codeBlockContext != fragment.codeBlockContext)) codeStart = fragment;
            boolean lastOfCode = (i == (cell.fragments.size() - 1)) || (cell.fragments.get(i + 1).codeBlockContext != fragment.codeBlockContext);
            if (lastOfCode) MarkdownCodePainter.renderInline(graphics, codeStart, fragment);
        }
        for (MarkdownTextFragment fragment : cell.fragments) {
            fragment.render(graphics, mouseX, mouseY, partial);
        }
    }

    /**
     * Draws the lines of a full grid onto the cells of the table.
     */
    private static void renderGrid(@NotNull GuiGraphics graphics, @NotNull MarkdownRenderer renderer, @NotNull TableContext table, float tableX, float tableY, float rowsEndY, float lineThickness, int lineColor) {
        float y = tableY;
        for (int i = 0; i <= table.rows.size(); i++) {
            RenderingUtils.fillF(graphics, tableX, y, tableX + table.totalWidth, y + lineThickness, lineColor);
            if (i < table.rows.size()) y += table.rows.get(i).height;
        }
        if (table.hasHeader && isHeaderRow(renderer, table, 0)) {
            float headerY = tableY + table.rows.get(0).height;
            RenderingUtils.fillF(graphics, tableX, headerY, tableX + table.totalWidth, headerY + renderer.scaling.sized(renderer.style.tableHeaderSeparatorThickness.get()), renderer.getRenderColor(renderer.style.tableHeaderSeparatorColor));
        }
        float x = tableX;
        for (int i = 0; i <= table.columnWidths.length; i++) {
            RenderingUtils.fillF(graphics, x, tableY, x + lineThickness, rowsEndY + lineThickness, lineColor);
            if (i < table.columnWidths.length) x += table.columnWidths[i];
        }
    }

}
