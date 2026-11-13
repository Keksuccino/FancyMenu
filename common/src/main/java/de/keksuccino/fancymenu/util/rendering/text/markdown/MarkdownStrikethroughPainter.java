package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Knows where the line through struck through text runs.
 */
final class MarkdownStrikethroughPainter {

    private MarkdownStrikethroughPainter() {
    }

    /**
     * Marks the fragments that struck through text ends with, in the given fragments and in the cells of their tables. Has to run after every parse.<br>
     * A fragment holds a word and the space behind it, and the line through the last struck through word of a text must not run through that space.
     */
    static void markEnds(@NotNull List<MarkdownTextFragment> fragments) {
        MarkdownTextFragment previous = null;
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.isTable()) {
                for (MarkdownTextFragment.TableRow row : fragment.tableContext.rows) {
                    for (MarkdownTextFragment.TableCell cell : row.cells) {
                        markEnds(cell.fragments);
                    }
                }
            }
            if ((previous != null) && !fragment.strikethrough) markEnd(previous);
            previous = fragment;
        }
        if (previous != null) markEnd(previous);
    }

    private static void markEnd(@NotNull MarkdownTextFragment fragment) {
        if (!fragment.strikethrough) return;
        fragment.strikethroughEnd = true;
        // The space behind the fragment gets drawn on its own now, which is a different text to measure
        if (fragment.text.endsWith(" ")) fragment.updateWidth();
    }

}
