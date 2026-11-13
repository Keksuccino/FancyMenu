package de.keksuccino.fancymenu.util.rendering.text.markdown.style;

/**
 * What a blank line of the text becomes.
 */
public enum MarkdownBlankLineMode {

    /** An empty line that is as tall as a line of text. */
    EMPTY_LINE,
    /**
     * Nothing with a height of its own. The blank line only ends the block in front of it, like a paragraph,
     * so the space between that block and the next one is all there is.
     */
    PARAGRAPH_GAP

}
