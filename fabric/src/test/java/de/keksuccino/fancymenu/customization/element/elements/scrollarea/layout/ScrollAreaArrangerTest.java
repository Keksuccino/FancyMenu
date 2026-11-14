package de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrollAreaArrangerTest {

    /** An arrangement of an area that shows 200x100 of its content, with children of the given sizes (width and height in turns). */
    private static ScrollAreaArrangement arrangement(int... sizes) {
        ScrollAreaArrangement arrangement = new ScrollAreaArrangement();
        arrangement.viewportWidth = 200;
        arrangement.viewportHeight = 100;
        arrangement.prepare(sizes.length / 2);
        for (int i = 0; i < arrangement.count; i++) {
            arrangement.naturalWidth[i] = sizes[i * 2];
            arrangement.naturalHeight[i] = sizes[(i * 2) + 1];
        }
        return arrangement;
    }

    private static int[] box(ScrollAreaArrangement arrangement, int child) {
        return new int[] { arrangement.x[child], arrangement.y[child], arrangement.width[child], arrangement.height[child] };
    }

    @Test
    void stackedChildrenFollowEachOtherFromTopToBottom() {
        ScrollAreaArrangement arrangement = arrangement(50, 20, 80, 30, 40, 10);
        arrangement.padding = 5;
        arrangement.spacing = 3;
        ScrollAreaArranger.STACK.arrange(arrangement);
        assertArrayEquals(new int[] { 5, 5, 50, 20 }, box(arrangement, 0));
        assertArrayEquals(new int[] { 5, 28, 80, 30 }, box(arrangement, 1));
        assertArrayEquals(new int[] { 5, 61, 40, 10 }, box(arrangement, 2));
        assertEquals(200, arrangement.contentWidth);
        // Padding, the three children with two spaces between them, padding
        assertEquals(5 + 20 + 3 + 30 + 3 + 10 + 5, arrangement.contentHeight);
    }

    @Test
    void stackedChildrenAlignAcrossTheStack() {
        ScrollAreaArrangement arrangement = arrangement(50, 20);
        arrangement.padding = 10;
        arrangement.alignment = ScrollAreaAlignment.CENTER;
        ScrollAreaArranger.STACK.arrange(arrangement);
        // 180 units of room, 130 of them free
        assertEquals(10 + 65, arrangement.x[0]);

        arrangement.alignment = ScrollAreaAlignment.END;
        ScrollAreaArranger.STACK.arrange(arrangement);
        assertEquals(10 + 130, arrangement.x[0]);

        arrangement.alignment = ScrollAreaAlignment.FILL;
        ScrollAreaArranger.STACK.arrange(arrangement);
        assertArrayEquals(new int[] { 10, 10, 180, 20 }, box(arrangement, 0));
    }

    @Test
    void sidewaysStacksRunFromLeftToRight() {
        ScrollAreaArrangement arrangement = arrangement(50, 20, 80, 30);
        arrangement.horizontal = true;
        arrangement.padding = 4;
        arrangement.spacing = 6;
        arrangement.alignment = ScrollAreaAlignment.END;
        ScrollAreaArranger.STACK.arrange(arrangement);
        // 92 units of room across, so the children sit on the bottom
        assertArrayEquals(new int[] { 4, 4 + 72, 50, 20 }, box(arrangement, 0));
        assertArrayEquals(new int[] { 60, 4 + 62, 80, 30 }, box(arrangement, 1));
        assertEquals(4 + 50 + 6 + 80 + 4, arrangement.contentWidth);
        assertEquals(100, arrangement.contentHeight);
    }

    @Test
    void anEmptyStackOnlyHasItsPadding() {
        ScrollAreaArrangement arrangement = arrangement();
        arrangement.padding = 7;
        arrangement.spacing = 3;
        ScrollAreaArranger.STACK.arrange(arrangement);
        assertEquals(14, arrangement.contentHeight);
    }

    @Test
    void gridCellsShareTheAreaEvenly() {
        ScrollAreaArrangement arrangement = arrangement(30, 20, 30, 40, 30, 10, 30, 25);
        arrangement.padding = 10;
        arrangement.spacing = 5;
        arrangement.gridLines = 3;
        ScrollAreaArranger.GRID.arrange(arrangement);
        // 180 units of room minus two spaces are 170, so the cells are 57, 57 and 56 wide
        assertArrayEquals(new int[] { 10, 10, 30, 20 }, box(arrangement, 0));
        assertArrayEquals(new int[] { 10 + 57 + 5, 10, 30, 40 }, box(arrangement, 1));
        assertArrayEquals(new int[] { 10 + 57 + 5 + 57 + 5, 10, 30, 10 }, box(arrangement, 2));
        // The second row starts below the highest child of the first one
        assertArrayEquals(new int[] { 10, 10 + 40 + 5, 30, 25 }, box(arrangement, 3));
        assertEquals(10 + 40 + 5 + 25 + 10, arrangement.contentHeight);
    }

    @Test
    void gridChildrenFillOrAlignInTheirCells() {
        ScrollAreaArrangement arrangement = arrangement(30, 20, 30, 20);
        arrangement.gridLines = 2;
        arrangement.alignment = ScrollAreaAlignment.FILL;
        ScrollAreaArranger.GRID.arrange(arrangement);
        assertArrayEquals(new int[] { 0, 0, 100, 20 }, box(arrangement, 0));
        assertArrayEquals(new int[] { 100, 0, 100, 20 }, box(arrangement, 1));

        arrangement.alignment = ScrollAreaAlignment.CENTER;
        ScrollAreaArranger.GRID.arrange(arrangement);
        assertEquals(35, arrangement.x[0]);
        assertEquals(135, arrangement.x[1]);
    }

    @Test
    void gridChildrenNeverReachIntoTheNextCell() {
        ScrollAreaArrangement arrangement = arrangement(150, 20, 30, 20);
        arrangement.gridLines = 2;
        ScrollAreaArranger.GRID.arrange(arrangement);
        assertEquals(100, arrangement.width[0]);
        assertEquals(100, arrangement.x[1]);
    }

    @Test
    void gridsThatFitTheirLinesUseAsManyAsHaveRoom() {
        ScrollAreaArrangement arrangement = arrangement(10, 10, 10, 10, 10, 10, 10, 10, 10, 10);
        arrangement.spacing = 10;
        arrangement.gridAutoFit = true;
        arrangement.gridMinCellSize = 60;
        ScrollAreaArranger.GRID.arrange(arrangement);
        // Three cells of 60 with two spaces of 10 need exactly the 200 the area has
        assertEquals(0, arrangement.y[2]);
        assertEquals(20, arrangement.y[3]);
        assertEquals(140, arrangement.x[2]);

        arrangement.gridMinCellSize = 61;
        ScrollAreaArranger.GRID.arrange(arrangement);
        assertEquals(20, arrangement.y[2]);

        // An area that is smaller than one cell still has one line
        arrangement.gridMinCellSize = 5000;
        ScrollAreaArranger.GRID.arrange(arrangement);
        assertEquals(0, arrangement.x[1]);
        assertEquals(20, arrangement.y[1]);
    }

    @Test
    void sidewaysGridsFillLinesFromTopToBottom() {
        ScrollAreaArrangement arrangement = arrangement(30, 20, 40, 20, 50, 20);
        arrangement.horizontal = true;
        arrangement.gridLines = 2;
        ScrollAreaArranger.GRID.arrange(arrangement);
        assertArrayEquals(new int[] { 0, 0, 30, 20 }, box(arrangement, 0));
        assertArrayEquals(new int[] { 0, 50, 40, 20 }, box(arrangement, 1));
        // The second line starts behind the widest child of the first one
        assertArrayEquals(new int[] { 40, 0, 50, 20 }, box(arrangement, 2));
        assertEquals(90, arrangement.contentWidth);
        assertEquals(100, arrangement.contentHeight);
    }

    @Test
    void flowingChildrenWrapWhenTheLineIsFull() {
        ScrollAreaArrangement arrangement = arrangement(90, 20, 90, 30, 90, 10, 50, 15);
        arrangement.padding = 5;
        arrangement.spacing = 10;
        ScrollAreaArranger.FLOW.arrange(arrangement);
        // 190 units of room: two children of 90 and their space fit exactly
        assertArrayEquals(new int[] { 5, 5, 90, 20 }, box(arrangement, 0));
        assertArrayEquals(new int[] { 105, 5, 90, 30 }, box(arrangement, 1));
        assertArrayEquals(new int[] { 5, 5 + 30 + 10, 90, 10 }, box(arrangement, 2));
        assertArrayEquals(new int[] { 105, 5 + 30 + 10, 50, 15 }, box(arrangement, 3));
        assertEquals(5 + 30 + 10 + 15 + 5, arrangement.contentHeight);
    }

    @Test
    void flowingLinesAlignAsAWhole() {
        ScrollAreaArrangement arrangement = arrangement(50, 20, 50, 20, 150, 20);
        arrangement.spacing = 10;
        arrangement.alignment = ScrollAreaAlignment.CENTER;
        ScrollAreaArranger.FLOW.arrange(arrangement);
        // The first line is 110 wide, the second one 150
        assertEquals(45, arrangement.x[0]);
        assertEquals(105, arrangement.x[1]);
        assertEquals(25, arrangement.x[2]);

        arrangement.alignment = ScrollAreaAlignment.END;
        ScrollAreaArranger.FLOW.arrange(arrangement);
        assertEquals(90, arrangement.x[0]);
        assertEquals(50, arrangement.x[2]);
    }

    @Test
    void flowingChildrenThatAreBiggerThanTheAreaEndWithIt() {
        ScrollAreaArrangement arrangement = arrangement(30, 20, 500, 20, 30, 20);
        ScrollAreaArranger.FLOW.arrange(arrangement);
        assertArrayEquals(new int[] { 0, 20, 200, 20 }, box(arrangement, 1));
        assertEquals(40, arrangement.y[2]);
    }

    @Test
    void freeChildrenStayWhereTheyWantToBe() {
        ScrollAreaArrangement arrangement = arrangement(30, 20, 40, 50);
        arrangement.offsetX[0] = 12;
        arrangement.offsetY[0] = 34;
        arrangement.offsetX[1] = -5;
        arrangement.offsetY[1] = 600;
        ScrollAreaArranger.FREE.arrange(arrangement);
        assertArrayEquals(new int[] { 12, 34, 30, 20 }, box(arrangement, 0));
        assertArrayEquals(new int[] { -5, 600, 40, 50 }, box(arrangement, 1));
        // The size of the content stays up to the user
        assertEquals(-1, arrangement.contentWidth);
        assertEquals(-1, arrangement.contentHeight);
    }

    @Test
    void arrangementsKeepWorkingWhenAnAreaGetsMoreChildren() {
        ScrollAreaArrangement arrangement = arrangement(10, 10);
        ScrollAreaArranger.STACK.arrange(arrangement);
        arrangement.prepare(40);
        for (int i = 0; i < 40; i++) {
            arrangement.naturalWidth[i] = 10;
            arrangement.naturalHeight[i] = 10;
        }
        ScrollAreaArranger.STACK.arrange(arrangement);
        assertEquals(390, arrangement.y[39]);
        assertEquals(400, arrangement.contentHeight);
    }

    @Test
    void alignmentsOnlyMoveThingsThatHaveRoom() {
        assertEquals(0, ScrollAreaAlignment.START.offset(40));
        assertEquals(20, ScrollAreaAlignment.CENTER.offset(40));
        assertEquals(40, ScrollAreaAlignment.END.offset(40));
        assertEquals(0, ScrollAreaAlignment.FILL.offset(40));
        assertEquals(0, ScrollAreaAlignment.END.offset(-10));
    }

}
