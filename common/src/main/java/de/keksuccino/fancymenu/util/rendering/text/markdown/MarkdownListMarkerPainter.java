package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerAlignment;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerShape;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;

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
     * Draws the marker of the list item that starts in the given line.
     */
    static void render(@NotNull GuiGraphicsExtractor graphics, @NotNull MarkdownTextLine line) {
        MarkdownRenderer renderer = line.parent;
        MarkdownStyle style = renderer.style;
        MarkdownTextFragment first = line.fragments.get(0);
        MarkdownStyle.ListLevel level = style.listLevel(first.bulletListLevel);
        MarkdownMarkerShape shape = level.markerShape.get();
        if (shape == MarkdownMarkerShape.NONE) return;
        float scale = first.getScale();
        float size = style.listMarkerSize.get() * scale;
        float x = getMarkerX(first);
        float y = getMarkerY(first);
        int color = renderer.getRenderColor(level.markerColor);
        if (MarkdownPixelArt.isMarker(shape)) {
            // Pixel art has its own size and stands in the middle of the marker area
            float inset = (size - (MarkdownPixelArt.MARKER_SIZE * scale)) * 0.5F;
            MarkdownPixelArt.marker(graphics, shape, x + inset, y + inset, scale, color, style.textShadow.get());
        } else {
            RenderingUtils.fillF(graphics, x, y, x + size, y + size, color);
        }
    }

    /**
     * Returns where the area of the marker of the list item starts that the given fragment starts.
     */
    static float getMarkerX(@NotNull MarkdownTextFragment fragment) {
        MarkdownStyle style = fragment.parent.style;
        return fragment.x + ((getTextIndent(fragment) - style.listMarkerGap.get() - style.listMarkerSize.get()) * fragment.getScale());
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
