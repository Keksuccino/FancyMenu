package de.keksuccino.fancymenu.util.rendering.text.markdown.style;

/**
 * What a headline gets decorated with.
 */
public enum MarkdownHeadlineDecoration {

    NONE,
    /** A line below every render line of the headline, from one edge of the text area to the other. */
    UNDERLINE,
    /**
     * A line below the last render line of the headline, from one edge of the text area to the other.
     * It can fade out to the right through dithering and takes its own space below the headline.
     */
    RULE,
    /** A row of dots that leads from the end of the headline to the right edge of the text area. Only left-aligned headlines have it. */
    LEADER

}
