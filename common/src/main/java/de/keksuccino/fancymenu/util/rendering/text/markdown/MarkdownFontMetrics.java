package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothFont;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import org.jetbrains.annotations.NotNull;

/**
 * Where the letters of the font of a {@link MarkdownRenderer} sit inside a line, in pixels of text at its normal size.
 * Decorations that follow the text, like the dots behind a headline, get their place from these instead of assuming one font.
 */
final class MarkdownFontMetrics {

    /**
     * The font of Minecraft draws its letters on a grid of 9 rows: capital letters fill the upper 7 rows, the 8th row holds descenders,
     * and the 9th one stays empty. Resource packs draw other letters onto the same grid, and the font has no way to ask it for these numbers.
     */
    private static final MarkdownFontMetrics MINECRAFT = new MarkdownFontMetrics(7.0F);

    /** The distance from the top of a line to the baseline its letters stand on. */
    final float baseline;

    private MarkdownFontMetrics(float baseline) {
        this.baseline = baseline;
    }

    /**
     * Returns the metrics of the font the renderer draws its text with right now.
     */
    @NotNull
    static MarkdownFontMetrics of(@NotNull MarkdownRenderer renderer) {
        // The UI draws with the Minecraft font if the user prefers it or the smooth font failed to load
        if (!renderer.uiFontRenderingEnabled || UIBase.shouldUseMinecraftFontForUIRendering()) return MINECRAFT;
        SmoothFont font = UIBase.getUIFont();
        float size = UIBase.getUITextSizeNormal();
        return new MarkdownFontMetrics(font.getAscent(size) + font.getYOffset(size));
    }

}
