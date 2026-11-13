package de.keksuccino.fancymenu.util.rendering.text.markdown.style;

/**
 * Which lines a table has.
 */
public enum MarkdownTableGrid {

    /** A line around the table and between all of its rows and columns. The lines lie on the edges of the cells and take no space of their own. */
    FULL,
    /** A frame around the table and a line below its header. Both take their own space, so the cells start behind them. */
    FRAME

}
