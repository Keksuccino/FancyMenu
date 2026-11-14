package de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout;

import org.jetbrains.annotations.NotNull;

/**
 * Works out where the children of a scrollable area are and how big they are, and how big the content is that holds them.
 */
public interface ScrollAreaArranger {

    /**
     * Leaves every child where it wants to be, with the size it has on its own. The size of the content is up to the user.
     */
    ScrollAreaArranger FREE = arrangement -> {
        for (int i = 0; i < arrangement.count; i++) {
            arrangement.x[i] = arrangement.offsetX[i];
            arrangement.y[i] = arrangement.offsetY[i];
            arrangement.width[i] = arrangement.naturalWidth[i];
            arrangement.height[i] = arrangement.naturalHeight[i];
        }
        arrangement.contentWidth = -1;
        arrangement.contentHeight = -1;
    };

    /**
     * Puts the children in one line, one after another along the main axis.
     */
    ScrollAreaArranger STACK = arrangement -> {
        int available = arrangement.availableCross();
        boolean fill = arrangement.alignment == ScrollAreaAlignment.FILL;
        int main = arrangement.padding;
        for (int i = 0; i < arrangement.count; i++) {
            int mainSize = arrangement.naturalMain(i);
            // A child that is bigger than the area would reach out of it, where nothing can scroll it into sight
            int crossSize = (fill || ((available > 0) && (arrangement.naturalCross(i) > available))) ? available : arrangement.naturalCross(i);
            arrangement.place(i, main, arrangement.padding + arrangement.alignment.offset(available - crossSize), mainSize, crossSize);
            main += mainSize + arrangement.spacing;
        }
        if (arrangement.count > 0) main -= arrangement.spacing;
        arrangement.setContentMain(main + arrangement.padding);
    };

    /**
     * Puts the children into cells of the same size on the cross axis, line by line. Every line is as long on the main axis as its longest child.
     */
    ScrollAreaArranger GRID = arrangement -> {
        int available = arrangement.availableCross();
        int spacing = arrangement.spacing;
        int lines = arrangement.gridAutoFit ? ((available + spacing) / (Math.max(1, arrangement.gridMinCellSize) + spacing)) : arrangement.gridLines;
        lines = Math.max(1, lines);
        // What does not divide evenly goes to the first cells, one unit each, so the cells fill the area from edge to edge
        int room = Math.max(lines, available - (spacing * (lines - 1)));
        int cellSize = room / lines;
        int widerCells = room % lines;
        boolean fill = arrangement.alignment == ScrollAreaAlignment.FILL;
        int main = arrangement.padding;
        for (int rowStart = 0; rowStart < arrangement.count; rowStart += lines) {
            int rowEnd = Math.min(arrangement.count, rowStart + lines);
            int rowSize = 0;
            int cross = arrangement.padding;
            for (int i = rowStart; i < rowEnd; i++) {
                int cell = cellSize + (((i - rowStart) < widerCells) ? 1 : 0);
                int mainSize = arrangement.naturalMain(i);
                // A child that is bigger than its cell would reach into the next one
                int crossSize = fill ? cell : Math.min(cell, arrangement.naturalCross(i));
                arrangement.place(i, main, cross + arrangement.alignment.offset(cell - crossSize), mainSize, crossSize);
                rowSize = Math.max(rowSize, mainSize);
                cross += cell + spacing;
            }
            main += rowSize + spacing;
        }
        if (arrangement.count > 0) main -= spacing;
        arrangement.setContentMain(main + arrangement.padding);
    };

    /**
     * Puts the children next to each other on the cross axis, with the size they have on their own, and starts a new line
     * when the next child does not fit anymore.
     */
    ScrollAreaArranger FLOW = arrangement -> {
        int available = arrangement.availableCross();
        int spacing = arrangement.spacing;
        int main = arrangement.padding;
        int lineStart = 0;
        int lineCross = 0;
        int lineSize = 0;
        for (int i = 0; i < arrangement.count; i++) {
            int mainSize = arrangement.naturalMain(i);
            // A child that is bigger than the area has a line of its own and ends where the area ends
            int crossSize = (available > 0) ? Math.min(available, arrangement.naturalCross(i)) : arrangement.naturalCross(i);
            if ((i > lineStart) && ((lineCross + crossSize) > available)) {
                alignFlowLine(arrangement, lineStart, i, available - (lineCross - spacing));
                main += lineSize + spacing;
                lineStart = i;
                lineCross = 0;
                lineSize = 0;
            }
            arrangement.place(i, main, arrangement.padding + lineCross, mainSize, crossSize);
            lineCross += crossSize + spacing;
            lineSize = Math.max(lineSize, mainSize);
        }
        if (arrangement.count > 0) {
            alignFlowLine(arrangement, lineStart, arrangement.count, available - (lineCross - spacing));
            main += lineSize;
        }
        arrangement.setContentMain(main + arrangement.padding);
    };

    /**
     * Arranges the children of the arrangement.
     */
    void arrange(@NotNull ScrollAreaArrangement arrangement);

    private static void alignFlowLine(@NotNull ScrollAreaArrangement arrangement, int start, int end, int freeRoom) {
        int offset = arrangement.alignment.offset(freeRoom);
        if (offset == 0) return;
        for (int i = start; i < end; i++) {
            arrangement.setCross(i, arrangement.cross(i) + offset);
        }
    }

}
