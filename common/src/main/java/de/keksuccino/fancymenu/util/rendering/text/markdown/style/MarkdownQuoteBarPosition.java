package de.keksuccino.fancymenu.util.rendering.text.markdown.style;

/**
 * Where the bar next to a quote stands.
 */
public enum MarkdownQuoteBarPosition {

    /** At the edge of the text area, in the space the quote indent keeps free in front of the text. */
    INSIDE,
    /**
     * In the padding of the renderer, right next to the text area. Containers like scroll areas clip the outer 2 pixels
     * of what they show, so the bar is only visible in there if the padding is wider than the bar and those pixels together.
     */
    BORDER

}
