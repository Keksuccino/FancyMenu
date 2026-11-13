package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothFont;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Where the letters of the font of a {@link MarkdownRenderer} sit inside a line, in pixels of text at its normal size.
 * Decorations that follow the text, like the dots behind a headline, get their place from these instead of assuming one font.
 */
final class MarkdownFontMetrics {

    /** The distance from the top of a line to the baseline its letters stand on. */
    final float baseline;
    /** The height of lowercase letters without an ascender, like "x". */
    final float xHeight;
    /** The distance from the baseline down to an underline. */
    final float underlineOffset;
    final float underlineThickness;
    /** The width of the empty space the font puts behind the last letter of a text. */
    final float glyphSpacing;
    /** The width of the letter "m" in the font of code, which is what the cells of fixed-width text are as wide as by default. */
    final float cellWidth;
    /** If the font is made of pixels, which only stay sharp at whole positions. */
    final boolean pixelFont;

    private MarkdownFontMetrics(float baseline, float xHeight, float underlineOffset, float underlineThickness, float glyphSpacing, float cellWidth, boolean pixelFont) {
        this.baseline = baseline;
        this.xHeight = xHeight;
        this.underlineOffset = underlineOffset;
        this.underlineThickness = underlineThickness;
        this.glyphSpacing = glyphSpacing;
        this.cellWidth = cellWidth;
        this.pixelFont = pixelFont;
    }

    /**
     * Returns the metrics of the font the renderer draws its text with right now.
     */
    @NotNull
    static MarkdownFontMetrics of(@NotNull MarkdownRenderer renderer) {
        MarkdownTextFont textFont = renderer.textFont;
        float cellWidth = textFont.width(textFont.component(textFont.withCodeFont(Style.EMPTY), "m", false));
        if (textFont.isMinecraftFont()) {
            // The font of Minecraft draws its letters on a grid of 9 rows: capital letters fill the upper 7 rows, the 8th row holds descenders,
            // and underlines fill the 9th one. Lowercase letters like "x" are 5 rows tall, and every letter is followed by one empty column.
            // Resource packs draw other letters onto the same grid, and the font has no way to ask it for these numbers.
            return new MarkdownFontMetrics(7.0F, 5.0F, 1.0F, 1.0F, 1.0F, cellWidth, true);
        }
        SmoothFont font = UIBase.getUIFont();
        float size = UIBase.getUITextSizeNormal();
        return new MarkdownFontMetrics(font.getAscent(size) + font.getYOffset(size), font.getXHeight(size), font.getUnderlineOffset(size), font.getUnderlineThickness(size), 0.0F, cellWidth, false);
    }

}
