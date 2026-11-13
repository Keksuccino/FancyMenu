package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownUnderlineMode;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Knows how the style underlines links and text events, and draws the underlines that have a color of their own.<br>
 * An underline in the color of its text is part of the text itself, because fonts draw those. The font can't draw
 * an underline in another color, so this draws it as a line below the text of every fragment of the link.
 */
final class MarkdownLinkPainter {

    private MarkdownLinkPainter() {
    }

    /**
     * Returns if the link-like fragment is underlined right now.
     */
    static boolean isUnderlined(@NotNull MarkdownTextFragment fragment, boolean hovered) {
        MarkdownUnderlineMode mode = fragment.parent.style.linkUnderline.get();
        return (mode == MarkdownUnderlineMode.ALWAYS) || ((mode == MarkdownUnderlineMode.ON_HOVER) && hovered);
    }

    /**
     * Returns the color of the underline this painter draws below the link-like fragment right now, or NULL if it draws none,
     * which is the case while the fragment is not underlined or its underline is part of its text.
     */
    @Nullable
    static MarkdownStyleOption.ColorOption getUnderlineColor(@NotNull MarkdownTextFragment fragment) {
        MarkdownStyle style = fragment.parent.style;
        boolean hovered = fragment.isLinkHovered();
        if (!style.linkUnderlineColor.isSet() || !isUnderlined(fragment, hovered)) return null;
        return (hovered && style.linkHoverUnderlineColor.isSet()) ? style.linkHoverUnderlineColor : style.linkUnderlineColor;
    }

    /**
     * Returns the width of the underline below the link-like fragment, which ends where the text of the link ends.
     */
    static float getUnderlineWidth(@NotNull MarkdownTextFragment fragment) {
        MarkdownRenderer renderer = fragment.parent;
        float width = fragment.unscaledTextWidth;
        // The space behind the last word of a link is drawn behind the link, and the one at the end of a line is not drawn at all
        if (fragment.getTrailingSpaceMode() != MarkdownTextFragment.TRAILING_SPACE_KEEP) width -= fragment.unscaledTrailingSpaceWidth;
        if (renderer.style.trimGlyphSpacing.get()) width -= renderer.getFontMetrics().glyphSpacing;
        return Math.max(0.0F, width) * fragment.getScale();
    }

    static void renderUnderline(@NotNull GuiGraphics graphics, @NotNull MarkdownTextFragment fragment) {
        MarkdownStyleOption.ColorOption colorOption = getUnderlineColor(fragment);
        if (colorOption == null) return;
        MarkdownRenderer renderer = fragment.parent;
        MarkdownFontMetrics metrics = renderer.getFontMetrics();
        float scale = fragment.getScale();
        float x = fragment.getTextX();
        float y = fragment.getTextY() + ((metrics.baseline + metrics.underlineOffset) * scale);
        float width = getUnderlineWidth(fragment);
        float thickness = metrics.underlineThickness * scale;
        int color = renderer.getRenderColor(colorOption);
        if (renderer.hasTextShadow()) {
            RenderingUtils.fillF(graphics, x + scale, y + scale, x + scale + width, y + scale + thickness, MarkdownPixelArt.shadowColor(color));
        }
        RenderingUtils.fillF(graphics, x, y, x + width, y + thickness, color);
    }

}
