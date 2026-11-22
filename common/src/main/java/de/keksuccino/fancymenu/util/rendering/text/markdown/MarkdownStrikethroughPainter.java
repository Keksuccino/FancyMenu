package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Knows where the line through struck through text runs, and draws that line where the style keeps it on the pixel grid of the font.<br>
 * The font draws the line as a part of the text otherwise, between two rows of its pixels.
 */
final class MarkdownStrikethroughPainter {

    private MarkdownStrikethroughPainter() {
    }

    /**
     * Returns if this painter draws the line through the text of the fragment, which is no part of the text then.
     */
    static boolean drawsLine(@NotNull MarkdownTextFragment fragment) {
        MarkdownRenderer renderer = fragment.parent;
        return fragment.strikethrough && renderer.style.snapStrikethrough.get() && renderer.textFont.metrics().pixelFont;
    }

    /**
     * Returns the width of the line through the text of the fragment, which ends where the struck through text ends.
     */
    static float getLineWidth(@NotNull MarkdownTextFragment fragment) {
        return fragment.getUnscaledStyledTextWidth() * fragment.getScale();
    }

    /**
     * Draws the line through the text of the fragment or the shadow of that line, if this painter draws the line.
     *
     * @param shadow Draws the shadow of the line. The shadow has to get drawn before the text and the line after it, because text lies between the two.
     */
    static void render(@NotNull GuiGraphicsExtractor graphics, @NotNull MarkdownTextFragment fragment, boolean shadow) {
        if (!drawsLine(fragment)) return;
        MarkdownRenderer renderer = fragment.parent;
        if (shadow && !renderer.textFont.hasShadow()) return;
        MarkdownFontMetrics metrics = renderer.textFont.metrics();
        float scale = fragment.getScale();
        float offset = shadow ? scale : 0.0F;
        float x = fragment.getTextX() + offset;
        // The line runs through the middle of lowercase letters
        float y = fragment.getTextY() + ((metrics.baseline - ((metrics.xHeight + metrics.underlineThickness) / 2.0F)) * scale) + offset;
        int color = getColor(fragment);
        RenderingUtils.fillF(graphics, x, y, x + getLineWidth(fragment), y + (metrics.underlineThickness * scale), shadow ? MarkdownPixelArt.shadowColor(color) : color);
    }

    /**
     * Returns the color the text of the fragment gets drawn in.
     */
    private static int getColor(@NotNull MarkdownTextFragment fragment) {
        MarkdownRenderer renderer = fragment.parent;
        int color = renderer.getRenderColor(renderer.style.textColor);
        TextColor ownColor = fragment.getRenderComponent().getStyle().getColor();
        return (ownColor != null) ? ((color & 0xFF000000) | ownColor.getValue()) : color;
    }

}
