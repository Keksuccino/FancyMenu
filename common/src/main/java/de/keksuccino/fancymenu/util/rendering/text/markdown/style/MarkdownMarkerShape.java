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
    DIAMOND,
    /** A smooth filled circle as big as the marker size of the style. */
    DISC,
    /** A smooth circle line as big as the marker size of the style. */
    RING,
    /** An icon of the UI: a plus sign. Icons are as tall as a capital letter of the text. */
    ICON_ADD,
    /** An icon of the UI: a minus sign. */
    ICON_REMOVE,
    /** An icon of the UI: a pen. */
    ICON_EDIT,
    /** An icon of the UI: a star. */
    ICON_STAR

}
