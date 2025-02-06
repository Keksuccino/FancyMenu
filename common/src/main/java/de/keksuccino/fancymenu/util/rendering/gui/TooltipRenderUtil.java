package de.keksuccino.fancymenu.util.rendering.gui;

public class TooltipRenderUtil {

    public static final int MOUSE_OFFSET = 12;
    private static final int PADDING = 3;
    public static final int PADDING_LEFT = 3;
    public static final int PADDING_RIGHT = 3;
    public static final int PADDING_TOP = 3;
    public static final int PADDING_BOTTOM = 3;
    private static final int BACKGROUND_COLOR = -267386864;
    private static final int BORDER_COLOR_TOP = 1347420415;
    private static final int BORDER_COLOR_BOTTOM = 1344798847;

    public static void renderTooltipBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int z) {
        int i = x - PADDING;
        int j = y - PADDING;
        int k = width + PADDING_LEFT + PADDING_RIGHT;
        int l = height + PADDING_TOP + PADDING_BOTTOM;

        renderHorizontalLine(guiGraphics, i, j - 1, k, z, BACKGROUND_COLOR);
        renderHorizontalLine(guiGraphics, i, j + l, k, z, BACKGROUND_COLOR);
        renderRectangle(guiGraphics, i, j, k, l, z, BACKGROUND_COLOR);
        renderVerticalLine(guiGraphics, i - 1, j, l, z, BACKGROUND_COLOR);
        renderVerticalLine(guiGraphics, i + k, j, l, z, BACKGROUND_COLOR);
        renderFrameGradient(guiGraphics, i, j + 1, k, l, z, BORDER_COLOR_TOP, BORDER_COLOR_BOTTOM);
    }

    private static void renderFrameGradient(GuiGraphics guiGraphics, int x, int y, int width, int height, int z, int topColor, int bottomColor) {
        renderVerticalLineGradient(guiGraphics, x, y, height - 2, z, topColor, bottomColor);
        renderVerticalLineGradient(guiGraphics, x + width - 1, y, height - 2, z, topColor, bottomColor);
        renderHorizontalLine(guiGraphics, x, y - 1, width, z, topColor);
        renderHorizontalLine(guiGraphics, x, y - 1 + height - 1, width, z, bottomColor);
    }

    private static void renderVerticalLine(GuiGraphics guiGraphics, int x, int y, int length, int z, int color) {
        guiGraphics.fill(x, y, x + 1, y + length, z, color);
    }

    private static void renderVerticalLineGradient(GuiGraphics guiGraphics, int x, int y, int length, int z, int topColor, int bottomColor) {
        guiGraphics.fillGradient(x, y, x + 1, y + length, z, topColor, bottomColor);
    }

    private static void renderHorizontalLine(GuiGraphics guiGraphics, int x, int y, int length, int z, int color) {
        guiGraphics.fill(x, y, x + length, y + 1, z, color);
    }

    private static void renderRectangle(GuiGraphics guiGraphics, int x, int y, int width, int height, int z, int color) {
        guiGraphics.fill(x, y, x + width, y + height, z, color);
    }

}
