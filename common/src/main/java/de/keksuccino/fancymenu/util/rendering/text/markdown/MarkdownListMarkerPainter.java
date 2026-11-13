package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerAlignment;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerShape;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        return (style.listIndent.get() * fragment.bulletListLevel) + style.listTextOffset.get();
    }

    /**
     * Returns if one of the fragments starts a list item with "+". Text with such items reads like patch notes, which changes the markers of its "-" items.
     */
    static boolean hasPlusItems(@NotNull List<MarkdownTextFragment> fragments) {
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.bulletListItemStart && (fragment.listItemMarker == MarkdownTextFragment.ListItemMarker.PLUS)) return true;
        }
        return false;
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
                return fragment.parent.textHasPlusListItems;
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
    static void render(@NotNull GuiGraphicsExtractor graphics, @NotNull MarkdownTextLine line) {
        MarkdownRenderer renderer = line.parent;
        MarkdownTextFragment first = line.fragments.get(0);
        float scale = first.getScale();
        MarkdownPaint.marker(graphics, renderer, getMarkerShape(first), getMarkerX(first), getMarkerY(first), renderer.style.listMarkerSize.get() * scale, scale, renderer.getRenderColor(getMarkerColor(first)));
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
        MarkdownFontMetrics metrics = renderer.getFontMetrics();
        return fragment.getTextY() + ((metrics.baseline - (metrics.xHeight * 0.5F)) * scale) - (size * 0.5F);
    }

}
