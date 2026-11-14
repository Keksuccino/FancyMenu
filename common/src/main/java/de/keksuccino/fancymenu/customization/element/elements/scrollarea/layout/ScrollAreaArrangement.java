package de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout;

import org.jetbrains.annotations.NotNull;

/**
 * What a {@link ScrollAreaArranger} works with: the area and the children it arranges go in, and the boxes of the children
 * and the size of the content come out. Everything is in the units of the content, and plain numbers, so arranging knows nothing about elements.<br><br>
 *
 * Children are arranged along the main axis, the direction the area scrolls in, and across it on the cross axis.
 * The arrangers only think in these two, and the methods here turn them into X and Y.<br>
 * One arrangement gets used for every frame of an area, so its arrays only grow when the area gets more children.
 */
public final class ScrollAreaArrangement {

    /** The size of what the area shows of its content. */
    public int viewportWidth;
    public int viewportHeight;
    /** If the main axis is the X axis: the area scrolls sideways, and its children run from left to right. */
    public boolean horizontal;
    /** The room between the edges of the content and the children. */
    public int padding;
    /** The room between two children. */
    public int spacing;
    @NotNull
    public ScrollAreaAlignment alignment = ScrollAreaAlignment.START;
    /** How many children a grid puts next to each other on the cross axis. */
    public int gridLines = 1;
    /** If a grid puts as many children next to each other as fit, instead of {@link #gridLines}. */
    public boolean gridAutoFit;
    /** The smallest size a cell of a grid that fits its lines to the area has on the cross axis. */
    public int gridMinCellSize = 1;
    public int count;
    /** The size every child has on its own. */
    public int[] naturalWidth = new int[0];
    public int[] naturalHeight = new int[0];
    /** Where every child wants to be, for arrangers that leave the place of children to them. */
    public int[] offsetX = new int[0];
    public int[] offsetY = new int[0];

    /** The box every child got. */
    public int[] x = new int[0];
    public int[] y = new int[0];
    public int[] width = new int[0];
    public int[] height = new int[0];
    /** The size the content needs for the arranged children, or -1 for an arranger that leaves the size of the content to the user. */
    public int contentWidth;
    public int contentHeight;

    /**
     * Makes room for the given number of children. What was in the arrangement before is gone afterwards.
     */
    public void prepare(int count) {
        this.count = count;
        if (this.naturalWidth.length >= count) return;
        int capacity = Math.max(count, this.naturalWidth.length * 2);
        this.naturalWidth = new int[capacity];
        this.naturalHeight = new int[capacity];
        this.offsetX = new int[capacity];
        this.offsetY = new int[capacity];
        this.x = new int[capacity];
        this.y = new int[capacity];
        this.width = new int[capacity];
        this.height = new int[capacity];
    }

    public int viewportCross() {
        return this.horizontal ? this.viewportHeight : this.viewportWidth;
    }

    /**
     * Returns the room the children have on the cross axis, between the padding on both sides.
     */
    public int availableCross() {
        return Math.max(0, this.viewportCross() - (this.padding * 2));
    }

    public int naturalMain(int child) {
        return this.horizontal ? this.naturalWidth[child] : this.naturalHeight[child];
    }

    public int naturalCross(int child) {
        return this.horizontal ? this.naturalHeight[child] : this.naturalWidth[child];
    }

    public int cross(int child) {
        return this.horizontal ? this.y[child] : this.x[child];
    }

    public void place(int child, int main, int cross, int mainSize, int crossSize) {
        if (this.horizontal) {
            this.x[child] = main;
            this.y[child] = cross;
            this.width[child] = mainSize;
            this.height[child] = crossSize;
        } else {
            this.x[child] = cross;
            this.y[child] = main;
            this.width[child] = crossSize;
            this.height[child] = mainSize;
        }
    }

    public void setCross(int child, int cross) {
        if (this.horizontal) {
            this.y[child] = cross;
        } else {
            this.x[child] = cross;
        }
    }

    /**
     * Sets the size the content needs on the main axis. On the cross axis, arranged content is always as big as the area.
     */
    public void setContentMain(int mainSize) {
        if (this.horizontal) {
            this.contentWidth = mainSize;
            this.contentHeight = this.viewportHeight;
        } else {
            this.contentWidth = this.viewportWidth;
            this.contentHeight = mainSize;
        }
    }

}
