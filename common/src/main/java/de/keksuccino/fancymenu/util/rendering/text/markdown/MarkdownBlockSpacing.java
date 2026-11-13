package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownBlankLineMode;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownBlockGapMode;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import org.jetbrains.annotations.NotNull;

/**
 * Knows how much space the style of a {@link MarkdownRenderer} puts between the render lines of its text.<br>
 * Lines of the same block, like the lines of a paragraph, have the line spacing between them. Between two blocks, the space below
 * the first one and the space above the second one come together the way the {@link MarkdownBlockGapMode} of the style says.
 */
final class MarkdownBlockSpacing {

    private MarkdownBlockSpacing() {
    }

    /**
     * Returns the space above the first line of the text.
     */
    static float aboveFirst(@NotNull MarkdownRenderer renderer, @NotNull MarkdownTextLine line) {
        return (renderer.style.blockGapMode.get() == MarkdownBlockGapMode.ADD) ? above(renderer, line) : 0.0F;
    }

    /**
     * Returns the space below the last line of the text.
     */
    static float belowLast(@NotNull MarkdownRenderer renderer, @NotNull MarkdownTextLine line) {
        return (renderer.style.blockGapMode.get() == MarkdownBlockGapMode.ADD) ? (below(renderer, line) + renderer.scaling.lineSpacing()) : 0.0F;
    }

    /**
     * Returns the space between two lines that follow each other.
     *
     * @param blankLineBetween If a blank line that takes no space itself sits between the two lines. It still separates their blocks.
     */
    static float between(@NotNull MarkdownRenderer renderer, @NotNull MarkdownTextLine previous, @NotNull MarkdownTextLine line, boolean blankLineBetween) {
        MarkdownStyle style = renderer.style;
        boolean add = (style.blockGapMode.get() == MarkdownBlockGapMode.ADD);
        float lineSpacing = renderer.scaling.lineSpacing();
        float itemSpacing = renderer.scaling.scaled(style.listItemSpacing.get());
        if (!blankLineBetween && isSameBlock(previous, line)) {
            // A quote goes on until a blank line ends it, so its lines can be list items, which keep their spacing in there
            if (line.bulletListItemStartLine) return add ? (lineSpacing + itemSpacing) : Math.max(lineSpacing, itemSpacing);
            return lineSpacing;
        }
        // The items of a list have the item spacing between them, and the spaces around the list are for what comes before and after it
        if (!blankLineBetween && (previous.blockType == MarkdownBlockType.LIST_ITEM) && (line.blockType == MarkdownBlockType.LIST_ITEM)) {
            return add ? (lineSpacing + itemSpacing) : itemSpacing;
        }
        float below = below(renderer, previous);
        float above = above(renderer, line);
        return add ? (lineSpacing + below + above) : Math.max(below, above);
    }

    /**
     * Returns if a blank line takes no space, which depends on how many blank lines stand in front of it.
     *
     * @param positionInRow 1 for the first blank line of a row of blank lines, 2 for the second one, and so on.
     */
    static boolean isCollapsedBlankLine(@NotNull MarkdownRenderer renderer, int positionInRow) {
        if (positionInRow == 1) return renderer.style.blankLineMode.get() == MarkdownBlankLineMode.PARAGRAPH_GAP;
        return renderer.style.collapseBlankLines.get();
    }

    /**
     * Returns if both lines are part of the same block. The second line has to follow the first one directly.
     */
    static boolean isSameBlock(@NotNull MarkdownTextLine previous, @NotNull MarkdownTextLine line) {
        if ((previous.blockType != line.blockType) || previous.fragments.isEmpty() || line.fragments.isEmpty()) return false;
        MarkdownTextFragment previousLast = previous.fragments.get(previous.fragments.size() - 1);
        MarkdownTextFragment first = line.fragments.get(0);
        switch (line.blockType) {
            case PARAGRAPH:
                return previous.alignment == line.alignment;
            case CODE_BLOCK:
                return previousLast.codeBlockContext == first.codeBlockContext;
            case QUOTE:
                return previousLast.quoteContext == first.quoteContext;
            case HEADLINE:
            case LIST_ITEM:
                // Every headline and every list item is one line of the text, so only the lines it wraps into belong to it
                return previousLast.autoLineBreakAfter;
            default:
                return false;
        }
    }

    private static float above(@NotNull MarkdownRenderer renderer, @NotNull MarkdownTextLine line) {
        MarkdownStyle style = renderer.style;
        MarkdownScaling scaling = renderer.scaling;
        switch (line.blockType) {
            case PARAGRAPH:
                return scaling.sized(style.paragraphSpaceAbove.get());
            case HEADLINE:
                return scaling.sized(style.headline(line.fragments.get(0).headlineType.level).spaceAbove.get());
            case LIST_ITEM:
                return scaling.scaled(style.listSpaceAbove.get());
            case QUOTE:
                return scaling.sized(style.quoteSpaceAbove.get());
            case CODE_BLOCK:
                return scaling.sized(style.codeBlockSpaceAbove.get());
            case TABLE:
                return scaling.sized(style.tableSpaceAbove.get());
            case IMAGE:
                return scaling.sized(style.imageSpaceAbove.get());
            case SEPARATOR:
                return scaling.sized(style.separatorSpaceAbove.get());
            default:
                return 0.0F;
        }
    }

    private static float below(@NotNull MarkdownRenderer renderer, @NotNull MarkdownTextLine line) {
        MarkdownStyle style = renderer.style;
        MarkdownScaling scaling = renderer.scaling;
        switch (line.blockType) {
            case PARAGRAPH:
                return scaling.sized(style.paragraphSpaceBelow.get());
            case HEADLINE:
                return scaling.sized(style.headline(line.fragments.get(0).headlineType.level).spaceBelow.get());
            case LIST_ITEM:
                return scaling.scaled(style.listSpaceBelow.get());
            case QUOTE:
                return scaling.sized(style.quoteSpaceBelow.get());
            case CODE_BLOCK:
                return scaling.sized(style.codeBlockSpaceBelow.get());
            case TABLE:
                return scaling.sized(style.tableSpaceBelow.get());
            case IMAGE:
                return scaling.sized(style.imageSpaceBelow.get());
            case SEPARATOR:
                return scaling.sized(style.separatorSpaceBelow.get());
            default:
                return 0.0F;
        }
    }

}
