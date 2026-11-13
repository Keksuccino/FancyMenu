package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.SmoothCircleRenderer;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerShape;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The shapes the painters of the Markdown renderer build backgrounds, frames and markers from.<br><br>
 *
 * Corners come in two kinds, which the style picks: pixel art corners leave a square out of every corner, which makes a rectangle
 * look rounded with a few big pixels, and smooth corners are real arcs, like the ones of the widgets of the UI.
 */
final class MarkdownPaint {

    private MarkdownPaint() {
    }

    /**
     * Returns the corner radius the given radius of the style gets drawn with.
     */
    static float cornerRadius(@NotNull MarkdownRenderer renderer, float radius) {
        float scaled = renderer.scaling.sized(radius);
        // Smooth corners belong to the UI theme, which can have no rounded corners at all
        return renderer.style.smoothCorners.get() ? Math.min(scaled, UIBase.getWidgetCornerRoundingRadius()) : scaled;
    }

    /**
     * Fills a rectangle whose corners all have the given radius.
     */
    static void box(@NotNull GuiGraphics graphics, @NotNull MarkdownRenderer renderer, float minX, float minY, float maxX, float maxY, float radius, int color) {
        box(graphics, renderer, minX, minY, maxX, maxY, radius, radius, color);
    }

    /**
     * Fills a rectangle whose upper and lower corners have a radius of their own.
     */
    static void box(@NotNull GuiGraphics graphics, @NotNull MarkdownRenderer renderer, float minX, float minY, float maxX, float maxY, float topRadius, float bottomRadius, int color) {
        if (((color >>> 24) == 0) || (maxX <= minX) || (maxY <= minY)) return;
        if ((topRadius <= 0.0F) && (bottomRadius <= 0.0F)) {
            RenderingUtils.fillF(graphics, minX, minY, maxX, maxY, color);
            return;
        }
        if (renderer.style.smoothCorners.get()) {
            UIBase.renderRoundedRect(graphics, minX, minY, maxX - minX, maxY - minY, topRadius, topRadius, bottomRadius, bottomRadius, color);
            return;
        }
        if (topRadius > 0.0F) RenderingUtils.fillF(graphics, minX + topRadius, minY, maxX - topRadius, minY + topRadius, color);
        RenderingUtils.fillF(graphics, minX, minY + topRadius, maxX, maxY - bottomRadius, color);
        if (bottomRadius > 0.0F) RenderingUtils.fillF(graphics, minX + bottomRadius, maxY - bottomRadius, maxX - bottomRadius, maxY, color);
    }

    /**
     * Draws a frame along the inside of the edges of a rectangle.<br>
     * A pixel art frame can have its top and left edge in one color and its bottom and right edge in another one,
     * which makes the rectangle look pressed into or raised from what is behind it. A smooth frame has the first color that is visible.
     *
     * @param radius The corner radius of the rectangle the frame goes around.
     */
    static void frame(@NotNull GuiGraphics graphics, @NotNull MarkdownRenderer renderer, float minX, float minY, float maxX, float maxY, float thickness, float radius, int topLeftColor, int bottomRightColor) {
        if ((thickness <= 0.0F) || (maxX <= minX) || (maxY <= minY)) return;
        if (renderer.style.smoothCorners.get()) {
            int color = ((topLeftColor >>> 24) != 0) ? topLeftColor : bottomRightColor;
            float innerRadius = Math.max(0.0F, radius - thickness);
            if ((color >>> 24) != 0) UIBase.renderRoundedBorder(graphics, minX, minY, maxX, maxY, thickness, innerRadius, innerRadius, innerRadius, innerRadius, color);
            return;
        }
        // Without cut corners, the horizontal edges own the corners, so no pixel gets drawn twice
        float verticalInset = Math.max(radius, thickness);
        if ((topLeftColor >>> 24) != 0) {
            RenderingUtils.fillF(graphics, minX + radius, minY, maxX - radius, minY + thickness, topLeftColor);
            RenderingUtils.fillF(graphics, minX, minY + verticalInset, minX + thickness, maxY - verticalInset, topLeftColor);
        }
        if ((bottomRightColor >>> 24) != 0) {
            RenderingUtils.fillF(graphics, minX + radius, maxY - thickness, maxX - radius, maxY, bottomRightColor);
            RenderingUtils.fillF(graphics, maxX - thickness, minY + verticalInset, maxX, maxY - verticalInset, bottomRightColor);
        }
    }

    /**
     * Draws a marker shape into a square area.
     *
     * @param size The width and height of the area, which squares and circles fill. Shapes with a size of their own stand in the middle of it.
     * @param pixelSize The size one pixel of pixel art gets drawn with, and the thickness of the line of a ring.
     * @param iconSize The size icons get drawn with.
     */
    static void marker(@NotNull GuiGraphics graphics, @NotNull MarkdownRenderer renderer, @NotNull MarkdownMarkerShape shape, float x, float y, float size, float pixelSize, float iconSize, int color) {
        if ((shape == MarkdownMarkerShape.NONE) || ((color >>> 24) == 0)) return;
        if (MarkdownPixelArt.isMarker(shape)) {
            float inset = (size - (MarkdownPixelArt.MARKER_SIZE * pixelSize)) * 0.5F;
            MarkdownPixelArt.marker(graphics, shape, x + inset, y + inset, pixelSize, color, renderer.hasTextShadow());
            return;
        }
        MaterialIcon icon = getIcon(shape);
        if (icon != null) {
            float inset = (size - iconSize) * 0.5F;
            icon.render(graphics, x + inset, y + inset, iconSize, iconSize, color);
        } else if (shape == MarkdownMarkerShape.DISC) {
            SmoothCircleRenderer.renderSmoothCircleScaled(graphics, x, y, size, size, 2.0F, color, 1.0F);
        } else if (shape == MarkdownMarkerShape.RING) {
            SmoothCircleRenderer.renderSmoothCircleBorderScaled(graphics, x, y, size, size, pixelSize, 2.0F, color, 1.0F);
        } else {
            RenderingUtils.fillF(graphics, x, y, x + size, y + size, color);
        }
    }

    @Nullable
    private static MaterialIcon getIcon(@NotNull MarkdownMarkerShape shape) {
        switch (shape) {
            case ICON_ADD:
                return MaterialIcons.ADD;
            case ICON_REMOVE:
                return MaterialIcons.REMOVE;
            case ICON_EDIT:
                return MaterialIcons.EDIT;
            case ICON_STAR:
                return MaterialIcons.STAR;
            default:
                return null;
        }
    }

}
