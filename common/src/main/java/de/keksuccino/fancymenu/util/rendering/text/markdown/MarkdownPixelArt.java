package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
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

    private static final ResourceLocation TEXTURE = new ResourceLocation("fancymenu", "textures/markdown/pixel_art.png");
    private static final int TEXTURE_WIDTH = 96;
    private static final int TEXTURE_HEIGHT = 32;

    private MarkdownPixelArt() {
    }

    /**
     * Draws a pattern as a horizontal line that is one pixel of the texture thick.
     *
     * @param length The length of the line in pixels of the texture.
     * @param phase How many pixels of the pattern the line skips at its start.
     */
    static void pattern(@NotNull GuiGraphics graphics, @NotNull Pattern pattern, float x, float y, float length, int phase, float pixelSize, int color) {
        if ((length <= 0.0F) || ((color >>> 24) == 0)) return;
        blit(graphics, x, y, phase, pattern.row, length * pixelSize, pixelSize, length, 1.0F, color);
    }

    /**
     * Draws a part of the texture in the given size.<br>
     * The textured rectangles of this Minecraft version cover as many pixels of their texture as they are big, so instead of the size of the part,
     * they get the position of the part and the size of the texture in the scale the part gets drawn with.
     */
    private static void blit(@NotNull GuiGraphics graphics, float x, float y, float u, float v, float width, float height, float partWidth, float partHeight, int color) {
        float scaleX = width / partWidth;
        float scaleY = height / partHeight;
        RenderingUtils.blitF(graphics, TEXTURE, x, y, u * scaleX, v * scaleY, width, height, TEXTURE_WIDTH * scaleX, TEXTURE_HEIGHT * scaleY, color);
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
