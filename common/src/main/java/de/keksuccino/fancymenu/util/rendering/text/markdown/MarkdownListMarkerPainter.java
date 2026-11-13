package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerAlignment;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerShape;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownNumberAlignment;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Measures and draws the markers in front of list items.<br>
 * A marker stands in a square area in front of the text of its item. Everything about it scales with that text.
 */
final class MarkdownListMarkerPainter {

    private MarkdownListMarkerPainter() {
    }

    /**
     * Returns how far the text of the list item the fragment starts gets indented, in pixels of text at its normal size.
     */
    static float getTextIndent(@NotNull MarkdownTextFragment fragment) {
        MarkdownStyle style = fragment.parent.style;
        float indent = (style.listIndent.get() * fragment.bulletListLevel) + style.listTextOffset.get();
        // Numbers are wider than markers
        if (fragment.listItemMarker == MarkdownTextFragment.ListItemMarker.NUMBER) indent += style.listNumberIndent.get();
        return indent;
    }

    /**
     * Tells the fragments of every list item if its list has an item that starts with "+". Has to run after every parse.<br>
     * A list with such an item reads like patch notes, which changes the markers of its "-" items.<br><br>
     *
     * A list is a run of lines that start a list item of any level, with nothing between them but blank lines.
     * Everything else ends it, and so does a line that belongs to another quote than the lines before it.
     */
    static void markListsWithPlusItems(@NotNull List<MarkdownTextFragment> fragments) {
        int listStart = -1;
        boolean listHasPlusItems = false;
        MarkdownTextFragment.QuoteContext listQuote = null;
        int lineStart = 0;
        int size = fragments.size();
        for (int index = 0; index < size; index++) {
            // A line of the text ends with the fragment that has the line break behind it
            if (!fragments.get(index).naturalLineBreakAfter && (index < (size - 1))) continue;
            MarkdownTextFragment item = null;
            boolean blank = true;
            for (int i = lineStart; i <= index; i++) {
                MarkdownTextFragment fragment = fragments.get(i);
                if (fragment.bulletListLevel > 0) {
                    if ((item == null) || fragment.bulletListItemStart) item = fragment;
                } else if (!isBlank(fragment)) {
                    blank = false;
                }
            }
            if (item != null) {
                if ((listStart >= 0) && (item.quoteContext != listQuote)) {
                    if (listHasPlusItems) markItems(fragments, listStart, lineStart);
                    listStart = -1;
                }
                if (listStart < 0) {
                    listStart = lineStart;
                    listHasPlusItems = false;
                    listQuote = item.quoteContext;
                }
                if (item.listItemMarker == MarkdownTextFragment.ListItemMarker.PLUS) listHasPlusItems = true;
            } else if ((listStart >= 0) && !blank) {
                if (listHasPlusItems) markItems(fragments, listStart, lineStart);
                listStart = -1;
            }
            lineStart = index + 1;
        }
        if ((listStart >= 0) && listHasPlusItems) markItems(fragments, listStart, size);
    }

    /**
     * Returns if the fragment is nothing but empty space of a line, which is what a blank line is made of.
     */
    private static boolean isBlank(@NotNull MarkdownTextFragment fragment) {
        return fragment.text.isBlank() && (fragment.imageSupplier == null) && !fragment.separationLine && !fragment.calloutMarker
                && (fragment.headlineType == MarkdownTextFragment.HeadlineType.NONE) && (fragment.tableContext == null) && (fragment.codeBlockContext == null);
    }

    private static void markItems(@NotNull List<MarkdownTextFragment> fragments, int start, int end) {
        for (int i = start; i < end; i++) {
            MarkdownTextFragment fragment = fragments.get(i);
            if (fragment.bulletListLevel > 0) fragment.listHasPlusItems = true;
        }
    }

    /**
     * Returns if the list item the fragment starts has the marker for removed things instead of the marker of its list level.
     */
    static boolean isRemovedItem(@NotNull MarkdownTextFragment fragment) {
        if ((fragment.listItemMarker != MarkdownTextFragment.ListItemMarker.DASH) || (fragment.bulletListLevel != 1)) return false;
        switch (fragment.parent.style.listMinusMarkerMode.get()) {
            case ALWAYS:
                return true;
            case AUTOMATIC:
                return fragment.listHasPlusItems;
            default:
                return false;
        }
    }

    /**
     * Returns the shape of the marker of the list item the fragment starts. The character the item starts with decides,
     * and items that start with "-" have the marker of their list level unless they stand for removed things.
     */
    @NotNull
    static MarkdownMarkerShape getMarkerShape(@NotNull MarkdownTextFragment fragment) {
        MarkdownStyle style = fragment.parent.style;
        switch (fragment.listItemMarker) {
            case PLUS:
                return style.listPlusMarkerShape.get();
            case TILDE:
                return style.listTildeMarkerShape.get();
            case ASTERISK:
                return style.listAsteriskMarkerShape.get();
            default:
                return isRemovedItem(fragment) ? style.listMinusMarkerShape.get() : style.listLevel(fragment.bulletListLevel).markerShape.get();
        }
    }

    @NotNull
    static MarkdownStyleOption.ColorOption getMarkerColor(@NotNull MarkdownTextFragment fragment) {
        MarkdownStyle style = fragment.parent.style;
        switch (fragment.listItemMarker) {
            case PLUS:
                return style.listPlusMarkerColor;
            case TILDE:
                return style.listTildeMarkerColor;
            case ASTERISK:
                return style.listAsteriskMarkerColor;
            default:
                return isRemovedItem(fragment) ? style.listMinusMarkerColor : style.listLevel(fragment.bulletListLevel).markerColor;
        }
    }

    /**
     * Draws the marker of the list item that starts in the given line.
     */
    static void render(@NotNull GuiGraphics graphics, @NotNull MarkdownTextLine line) {
        MarkdownRenderer renderer = line.parent;
        MarkdownTextFragment first = line.fragments.get(0);
        float scale = first.getScale();
        if (line.listNumber != null) {
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(getNumberX(line), first.getTextY());
                graphics.pose().scale(scale, scale);
                renderer.textFont.draw(graphics, line.listNumber, 0.0F, 0.0F, renderer.getRenderColor(renderer.style.listNumberColor), renderer.textFont.hasShadow());
            } finally {
                graphics.pose().popMatrix();
            }
            return;
        }
        MarkdownPaint.marker(graphics, renderer, getMarkerShape(first), getMarkerX(first), getMarkerY(first), renderer.style.listMarkerSize.get() * scale, scale, renderer.textFont.metrics().baseline * scale, renderer.getRenderColor(getMarkerColor(first)));
    }

    /**
     * Gives the line the number of the numbered list item that starts in it, or none if no such item does.
     */
    static void prepareNumber(@NotNull MarkdownTextLine line) {
        MarkdownTextFragment first = line.fragments.isEmpty() ? null : line.fragments.get(0);
        if ((first == null) || !first.bulletListItemStart || (first.listItemMarker != MarkdownTextFragment.ListItemMarker.NUMBER) || (first.listItemNumber == null)) {
            line.listNumber = null;
            return;
        }
        // The number shows the way it got typed
        line.listNumber = Component.literal(first.listItemNumber);
        line.listNumberWidth = line.parent.textFont.width(first.listItemNumber);
    }

    /**
     * Returns where the number of the numbered list item starts that starts in the given line.
     */
    static float getNumberX(@NotNull MarkdownTextLine line) {
        MarkdownStyle style = line.parent.style;
        MarkdownTextFragment first = line.fragments.get(0);
        float scale = first.getScale();
        if (style.listNumberAlignment.get() == MarkdownNumberAlignment.RIGHT) {
            return first.getTextX() - ((style.listNumberGap.get() + line.listNumberWidth) * scale);
        }
        return first.getTextX() - ((style.listNumberIndent.get() + style.listMarkerGap.get() + style.listMarkerSize.get()) * scale);
    }

    /**
     * Returns where the area of the marker of the list item starts that the given fragment starts.<br>
     * The marker keeps its place in front of the text of the item, whatever else indents that text, like a quote the item is a part of.
     */
    static float getMarkerX(@NotNull MarkdownTextFragment fragment) {
        MarkdownStyle style = fragment.parent.style;
        return fragment.getTextX() - ((style.listMarkerGap.get() + style.listMarkerSize.get()) * fragment.getScale());
    }

    static float getMarkerY(@NotNull MarkdownTextFragment fragment) {
        MarkdownRenderer renderer = fragment.parent;
        float scale = fragment.getScale();
        float size = renderer.style.listMarkerSize.get() * scale;
        if (renderer.style.listMarkerAlignment.get() == MarkdownMarkerAlignment.LINE_CENTER) {
            return fragment.getTextY() + (fragment.getTextRenderHeight() * 0.5F) - (size * 0.5F);
        }
        MarkdownFontMetrics metrics = renderer.textFont.metrics();
        return fragment.getTextY() + ((metrics.baseline - (metrics.xHeight * 0.5F)) * scale) - (size * 0.5F);
    }

}
