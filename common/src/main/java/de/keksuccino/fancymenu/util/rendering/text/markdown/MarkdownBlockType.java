package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.jetbrains.annotations.NotNull;

/**
 * The kinds of blocks a text is made of. Every render line is a part of one block, and the style gives every kind its own space above and below.
 */
enum MarkdownBlockType {

    PARAGRAPH,
    HEADLINE,
    LIST_ITEM,
    QUOTE,
    CODE_BLOCK,
    TABLE,
    IMAGE,
    SEPARATOR,
    /** A line without content, which separates the blocks around it. */
    BLANK;

    /**
     * Returns the kind of block the given line is a part of. The first fragment of a line decides, because the things that make a block start a line.
     */
    @NotNull
    static MarkdownBlockType of(@NotNull MarkdownTextLine line) {
        if (line.fragments.isEmpty()) return BLANK;
        MarkdownTextFragment first = line.fragments.get(0);
        if (first.isTable()) return TABLE;
        if (first.imageSupplier != null) return IMAGE;
        if (first.separationLine) return SEPARATOR;
        if ((first.codeBlockContext != null) && !first.codeBlockContext.singleLine) return CODE_BLOCK;
        if (first.quoteContext != null) return QUOTE;
        if (first.headlineType != MarkdownTextFragment.HeadlineType.NONE) return HEADLINE;
        if (first.bulletListLevel > 0) return LIST_ITEM;
        for (MarkdownTextFragment fragment : line.fragments) {
            if (!isBlank(fragment.text)) return PARAGRAPH;
        }
        return BLANK;
    }

    private static boolean isBlank(@NotNull String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > ' ') return false;
        }
        return true;
    }

}
