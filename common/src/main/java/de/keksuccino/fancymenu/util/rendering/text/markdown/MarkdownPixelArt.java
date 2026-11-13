package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerShape;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Draws from the pixel art texture of the Markdown renderer, which holds the shapes that are made of single pixels:
 * markers, icons, and the patterns of dotted and dithered lines.<br><br>
 *
 * Every shape is one textured quad, no matter how many pixels it has. A pattern fills a whole row of the texture and repeats in it,
 * and the texture repeats at its edges, so a line of any length is one quad as well.<br>
 * The shapes are white and get drawn in the color they are asked for. One pixel of the texture covers the given pixel size on screen,
 * which only stays sharp while that size is a whole number of screen pixels.
 */
final class MarkdownPixelArt {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("fancymenu", "textures/markdown/pixel_art.png");
    private static final int TEXTURE_WIDTH = 96;
    private static final int TEXTURE_HEIGHT = 32;

    /** Markers and icons stand in cells of this size, one next to the other. */
    private static final int CELL_SIZE = 8;
    /** The size of the pixel art markers, which fill the cells of the first row of the texture. */
    static final int MARKER_SIZE = 5;
    /** The size of the pixel art callout icons. Their shapes fill the cells of the second row of the texture, and their details the cells of the third row. */
    static final int CALLOUT_ICON_SIZE = 7;

    private MarkdownPixelArt() {
    }

    /**
     * Draws a pixel art marker.
     *
     * @param shadow If the marker has a shadow like text has: one pixel to the right and below, in a darker color.
     */
    static void marker(@NotNull GuiGraphicsExtractor graphics, @NotNull MarkdownMarkerShape shape, float x, float y, float pixelSize, int color, boolean shadow) {
        int cell = getMarkerCell(shape);
        if ((cell < 0) || ((color >>> 24) == 0)) return;
        float size = MARKER_SIZE * pixelSize;
        if (shadow) RenderingUtils.blitF(graphics, RenderPipelines.GUI_TEXTURED, TEXTURE, x + pixelSize, y + pixelSize, cell * CELL_SIZE, 0, size, size, MARKER_SIZE, MARKER_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT, shadowColor(color));
        RenderingUtils.blitF(graphics, RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, cell * CELL_SIZE, 0, size, size, MARKER_SIZE, MARKER_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT, color);
    }

    /**
     * Draws a pixel art callout icon. An icon has two layers with a color of their own: its shape and the details inside of it.
     *
     * @param type The position of the icon in the second and third row of the texture, which is the ordinal of its callout type.
     */
    static void calloutIcon(@NotNull GuiGraphicsExtractor graphics, int type, float x, float y, float pixelSize, int color, int detailColor, boolean shadow) {
        float size = CALLOUT_ICON_SIZE * pixelSize;
        float u = type * CELL_SIZE;
        if (shadow) {
            RenderingUtils.blitF(graphics, RenderPipelines.GUI_TEXTURED, TEXTURE, x + pixelSize, y + pixelSize, u, CELL_SIZE, size, size, CALLOUT_ICON_SIZE, CALLOUT_ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT, shadowColor(color));
            RenderingUtils.blitF(graphics, RenderPipelines.GUI_TEXTURED, TEXTURE, x + pixelSize, y + pixelSize, u, CELL_SIZE * 2, size, size, CALLOUT_ICON_SIZE, CALLOUT_ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT, shadowColor(detailColor));
        }
        RenderingUtils.blitF(graphics, RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, CELL_SIZE, size, size, CALLOUT_ICON_SIZE, CALLOUT_ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT, color);
        RenderingUtils.blitF(graphics, RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, CELL_SIZE * 2, size, size, CALLOUT_ICON_SIZE, CALLOUT_ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT, detailColor);
    }

    /**
     * Returns if the shape is one of the pixel art markers of the texture.
     */
    static boolean isMarker(@NotNull MarkdownMarkerShape shape) {
        return getMarkerCell(shape) >= 0;
    }

    /**
     * Returns the position of the marker in the first row of the texture, or -1 if the shape is no pixel art.
     */
    private static int getMarkerCell(@NotNull MarkdownMarkerShape shape) {
        switch (shape) {
            case CURSOR:
                return 0;
            case CURSOR_SMALL:
                return 1;
            case DOT:
                return 2;
            case PLUS:
                return 3;
            case MINUS:
                return 4;
            case WAVE:
                return 5;
            case STAR:
                return 6;
            case DIAMOND:
                return 7;
            default:
                return -1;
        }
    }

    /**
     * Draws a pattern as a horizontal line that is one pixel of the texture thick.
     *
     * @param length The length of the line in pixels of the texture.
     * @param phase How many pixels of the pattern the line skips at its start.
     */
    static void pattern(@NotNull GuiGraphicsExtractor graphics, @NotNull Pattern pattern, float x, float y, float length, int phase, float pixelSize, int color) {
        if ((length <= 0.0F) || ((color >>> 24) == 0)) return;
        RenderingUtils.blitF(graphics, RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, phase, pattern.row, length * pixelSize, pixelSize, length, 1.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, color);
    }

    /**
     * Returns the color of the shadow of something in the given color, which is the color text shadows have.
     */
    static int shadowColor(int color) {
        return (color & 0xFF000000) | ((color >> 2) & 0x003F3F3F);
    }

    /**
     * The repeating patterns of the texture. Every pattern is a row of the texture with one opaque pixel at the start of each period.
     */
    enum Pattern {

        /** Every second pixel. */
        HALF(24, 2),
        /** Every fourth pixel. */
        QUARTER(26, 4),
        /** Every third pixel. */
        THIRD(28, 3);

        private final int row;
        final int period;

        Pattern(int row, int period) {
            this.row = row;
            this.period = period;
        }

    }

}
