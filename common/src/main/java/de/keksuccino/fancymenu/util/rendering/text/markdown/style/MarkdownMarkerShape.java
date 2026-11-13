package de.keksuccino.fancymenu.util.rendering.text.markdown.style;

/**
 * The shapes of the markers in front of list items, and of the ornament of a separator.
 */
public enum MarkdownMarkerShape {

    NONE,
    /** A filled square as big as the marker size of the style. */
    SQUARE,
    /** Pixel art: a triangle that points at the item, like the cursor of a game menu. */
    CURSOR,
    /** Pixel art: a smaller triangle. */
    CURSOR_SMALL,
    /** Pixel art: a dot of 2 by 2 pixels. */
    DOT,
    /** Pixel art: a plus sign. */
    PLUS,
    /** Pixel art: a minus sign. */
    MINUS,
    /** Pixel art: a wave, like a tilde. */
    WAVE,
    /** Pixel art: a star. */
    STAR,
    /** Pixel art: a diamond. */
    DIAMOND

}
