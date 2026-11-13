package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
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
