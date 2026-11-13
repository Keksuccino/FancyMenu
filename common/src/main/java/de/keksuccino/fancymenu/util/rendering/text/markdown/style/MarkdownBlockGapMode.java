package de.keksuccino.fancymenu.util.rendering.text.markdown.style;

/**
 * How the space between two blocks of the text, like a headline and a paragraph, comes together.
 */
public enum MarkdownBlockGapMode {

    /**
     * The line spacing, the space below the first block and the space above the second block add up.
     * The first block of the text has its space above it, and the last block has its space and the line spacing below it.
     */
    ADD,
    /**
     * The bigger one of the space below the first block and the space above the second block wins, and the line spacing plays no part.
     * The first block of the text has no space above it, and the last block has none below it.
     */
    LARGEST

}
