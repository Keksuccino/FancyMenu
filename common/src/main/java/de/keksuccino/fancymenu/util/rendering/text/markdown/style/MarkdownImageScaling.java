package de.keksuccino.fancymenu.util.rendering.text.markdown.style;

/**
 * How big an image gets drawn. An image that is wider than the text area always shrinks to its width.
 */
public enum MarkdownImageScaling {

    /** As big as the image is. */
    NATIVE,
    /**
     * As the biggest whole multiple of its size that fits into the text area, up to the maximum factor of the style.
     * That enlarges pixel art without blurring or bending its pixels.
     */
    WHOLE_MULTIPLE

}
