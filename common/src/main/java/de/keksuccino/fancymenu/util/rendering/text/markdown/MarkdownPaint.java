package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerShape;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * The shapes the painters of the Markdown renderer build backgrounds and frames from.
 */
final class MarkdownPaint {

    private MarkdownPaint() {
    }

    /**
     * Fills a rectangle. A corner cut bigger than 0 leaves a square of that size out of every corner, which makes the rectangle look rounded in pixel art.
     */
    static void box(@NotNull GuiGraphics graphics, float minX, float minY, float maxX, float maxY, float cornerCut, int color) {
        if (((color >>> 24) == 0) || (maxX <= minX) || (maxY <= minY)) return;
        if (cornerCut <= 0.0F) {
            RenderingUtils.fillF(graphics, minX, minY, maxX, maxY, color);
            return;
        }
        RenderingUtils.fillF(graphics, minX + cornerCut, minY, maxX - cornerCut, minY + cornerCut, color);
        RenderingUtils.fillF(graphics, minX, minY + cornerCut, maxX, maxY - cornerCut, color);
        RenderingUtils.fillF(graphics, minX + cornerCut, maxY - cornerCut, maxX - cornerCut, maxY, color);
    }

    /**
     * Draws a marker shape into a square area.
     *
     * @param size The width and height of the area. Shapes with a size of their own, like pixel art, stand in the middle of it.
     * @param pixelSize The size one pixel of pixel art gets drawn with.
     */
    static void marker(@NotNull GuiGraphics graphics, @NotNull MarkdownRenderer renderer, @NotNull MarkdownMarkerShape shape, float x, float y, float size, float pixelSize, int color) {
        if ((shape == MarkdownMarkerShape.NONE) || ((color >>> 24) == 0)) return;
        if (MarkdownPixelArt.isMarker(shape)) {
            float inset = (size - (MarkdownPixelArt.MARKER_SIZE * pixelSize)) * 0.5F;
            MarkdownPixelArt.marker(graphics, shape, x + inset, y + inset, pixelSize, color, renderer.hasTextShadow());
        } else {
            RenderingUtils.fillF(graphics, x, y, x + size, y + size, color);
        }
    }

    /**
     * Draws a frame along the inside of the edges of a rectangle, with its top and left edge in one color and its bottom and right edge in another one.
     * Two colors make the rectangle look pressed into or raised from what is behind it.
     *
     * @param cornerCut How much of every corner stays empty, so the frame follows a {@linkplain #box box} with cut corners.
     */
    static void frame(@NotNull GuiGraphics graphics, float minX, float minY, float maxX, float maxY, float thickness, float cornerCut, int topLeftColor, int bottomRightColor) {
        if ((thickness <= 0.0F) || (maxX <= minX) || (maxY <= minY)) return;
        // Without cut corners, the horizontal edges own the corners, so no pixel gets drawn twice
        float verticalInset = Math.max(cornerCut, thickness);
        if ((topLeftColor >>> 24) != 0) {
            RenderingUtils.fillF(graphics, minX + cornerCut, minY, maxX - cornerCut, minY + thickness, topLeftColor);
            RenderingUtils.fillF(graphics, minX, minY + verticalInset, minX + thickness, maxY - verticalInset, topLeftColor);
        }
        if ((bottomRightColor >>> 24) != 0) {
            RenderingUtils.fillF(graphics, minX + cornerCut, maxY - thickness, maxX - cornerCut, maxY, bottomRightColor);
            RenderingUtils.fillF(graphics, maxX - thickness, minY + verticalInset, maxX, maxY - verticalInset, bottomRightColor);
        }
    }

}
