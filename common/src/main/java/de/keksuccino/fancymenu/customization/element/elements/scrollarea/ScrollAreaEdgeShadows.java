package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * Soft shadows at the edges of a scrollable area that there is more content behind. They show which way the area can still be scrolled,
 * and fade in over the first pixels the content moves away from an edge, so they never pop up.
 */
public final class ScrollAreaEdgeShadows {

    /** How far a shadow reaches into the area, and how far the content has to be from an edge for its shadow to show in full. */
    private static final int SIZE = 14;

    private ScrollAreaEdgeShadows() {
    }

    /**
     * Draws the shadows over what the area shows of its content.
     *
     * @param color The color of the shadows where they are the darkest, at the edge.
     * @param opacity The opacity of the area, between 0 and 1.
     */
    public static void render(@NotNull GuiGraphics graphics, @NotNull ScrollAreaGeometry geometry, @NotNull ScrollAreaScroller scroller, int color, float opacity) {
        int left = geometry.getViewportX();
        int top = geometry.getViewportY();
        int right = left + geometry.getViewportWidth();
        int bottom = top + geometry.getViewportHeight();
        int clear = color & 0x00FFFFFF;
        ScrollAreaScroller.Axis y = scroller.y();
        if (y.canScroll()) {
            int height = Math.min(SIZE, geometry.getViewportHeight() / 2);
            int atStart = shade(color, y.getPosition(), opacity);
            int atEnd = shade(color, y.getMax() - y.getPosition(), opacity);
            if ((atStart >>> 24) > 0) graphics.fillGradient(left, top, right, top + height, atStart, clear);
            if ((atEnd >>> 24) > 0) graphics.fillGradient(left, bottom - height, right, bottom, clear, atEnd);
        }
        ScrollAreaScroller.Axis x = scroller.x();
        if (x.canScroll()) {
            int width = Math.min(SIZE, geometry.getViewportWidth() / 2);
            int atStart = shade(color, x.getPosition(), opacity);
            int atEnd = shade(color, x.getMax() - x.getPosition(), opacity);
            if ((atStart >>> 24) > 0) RenderingUtils.fillGradientHorizontal(graphics, left, top, left + width, bottom, atStart, clear);
            if ((atEnd >>> 24) > 0) RenderingUtils.fillGradientHorizontal(graphics, right - width, top, right, bottom, clear, atEnd);
        }
    }

    /**
     * Returns the color of a shadow for content that is the given distance away from the edge of the shadow.
     */
    private static int shade(int color, double distance, float opacity) {
        float strength = (float) Math.min(1.0D, Math.max(0.0D, distance / SIZE));
        return RenderingUtils.multiplyAlphaInColor(color, strength * opacity);
    }

}
