package de.keksuccino.fancymenu.util.rendering;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

public final class SmoothLineRenderer {

    private static final float AXIS_EPSILON_FANCYMENU = 1.0E-4F;

    private SmoothLineRenderer() {
    }

    public static void renderSmoothHorizontalLine(@NotNull GuiGraphics graphics, float x, float y, float width, float thickness, int color, float partial) {
        SmoothRectangleRenderer.renderSmoothRect(graphics, x, y, width, thickness, 0.0F, color, partial);
    }

    public static void renderSmoothHorizontalLineScaled(@NotNull GuiGraphics graphics, float x, float y, float width, float thickness, int color, float partial) {
        SmoothRectangleRenderer.renderSmoothRectScaled(graphics, x, y, width, thickness, 0.0F, color, partial);
    }

    public static void renderSmoothVerticalLine(@NotNull GuiGraphics graphics, float x, float y, float height, float thickness, int color, float partial) {
        SmoothRectangleRenderer.renderSmoothRect(graphics, x, y, thickness, height, 0.0F, color, partial);
    }

    public static void renderSmoothVerticalLineScaled(@NotNull GuiGraphics graphics, float x, float y, float height, float thickness, int color, float partial) {
        SmoothRectangleRenderer.renderSmoothRectScaled(graphics, x, y, thickness, height, 0.0F, color, partial);
    }

    /**
     * Renders a smooth line between two axis-aligned points.
     * If the line is not horizontal/vertical, nothing is rendered.
     */
    public static void renderSmoothLine(@NotNull GuiGraphics graphics, float x1, float y1, float x2, float y2, float thickness, int color, float partial) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        if (Math.abs(dx) <= AXIS_EPSILON_FANCYMENU) {
            float startY = Math.min(y1, y2);
            renderSmoothVerticalLine(graphics, x1, startY, Math.abs(dy), thickness, color, partial);
        } else if (Math.abs(dy) <= AXIS_EPSILON_FANCYMENU) {
            float startX = Math.min(x1, x2);
            renderSmoothHorizontalLine(graphics, startX, y1, Math.abs(dx), thickness, color, partial);
        }
    }

    /**
     * Renders a smooth line between two axis-aligned points with additional render scale.
     * If the line is not horizontal/vertical, nothing is rendered.
     */
    public static void renderSmoothLineScaled(@NotNull GuiGraphics graphics, float x1, float y1, float x2, float y2, float thickness, int color, float partial) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        if (Math.abs(dx) <= AXIS_EPSILON_FANCYMENU) {
            float startY = Math.min(y1, y2);
            renderSmoothVerticalLineScaled(graphics, x1, startY, Math.abs(dy), thickness, color, partial);
        } else if (Math.abs(dy) <= AXIS_EPSILON_FANCYMENU) {
            float startX = Math.min(x1, x2);
            renderSmoothHorizontalLineScaled(graphics, startX, y1, Math.abs(dx), thickness, color, partial);
        }
    }
    
}
