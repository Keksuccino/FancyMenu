package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownQuoteBarPosition;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * Measures and draws what marks a quote: the bar next to it and the panel behind it.<br>
 * Both cover all render lines of the quote, from its first to its last fragment, no matter which of the lines are visible.
 */
final class MarkdownQuotePainter {

    private MarkdownQuotePainter() {
    }

    /**
     * Returns the space the first line of a quote keeps free above its text.
     */
    static float getPaddingTop(@NotNull MarkdownRenderer renderer) {
        return renderer.scaling.sized(renderer.style.quotePaddingTop.get());
    }

    /**
     * Returns the space the last line of a quote keeps free below its text.
     */
    static float getPaddingBottom(@NotNull MarkdownRenderer renderer) {
        return renderer.scaling.sized(renderer.style.quotePaddingBottom.get());
    }

    /**
     * Draws the panel and the bar of the quote the given line is a part of.
     */
    static void render(@NotNull GuiGraphics graphics, @NotNull MarkdownTextLine line) {
        MarkdownRenderer renderer = line.parent;
        MarkdownStyle style = renderer.style;
        MarkdownScaling scaling = renderer.scaling;
        MarkdownTextFragment.QuoteContext context = line.fragments.get(0).quoteContext;
        MarkdownStyleOption.ColorOption background = getBackgroundColor(renderer, context);
        float top = getTop(context);
        float bottom = getBottom(context);
        if (background.isSet()) {
            float left = renderer.x + scaling.padding();
            float right = renderer.x + renderer.getRealWidth() - scaling.padding();
            MarkdownPaint.box(graphics, left, top, right, bottom, scaling.sized(style.quoteCornerRadius.get()), renderer.getRenderColor(background));
        }
        if (hasBar(context)) {
            float barX = getBarX(context);
            RenderingUtils.fillF(graphics, barX, top - scaling.sized(style.quoteBarOverhangTop.get()), barX + getBarWidth(renderer), bottom + scaling.sized(style.quoteBarOverhangBottom.get()), renderer.getRenderColor(getBarColor(renderer, context)));
        }
        RenderingUtils.resetShaderColor(graphics);
    }

    /**
     * Returns the color of the bar of the quote. A callout is a quote in the color of its type.
     */
    @NotNull
    static MarkdownStyleOption.ColorOption getBarColor(@NotNull MarkdownRenderer renderer, @NotNull MarkdownTextFragment.QuoteContext context) {
        MarkdownStyle.Callout callout = MarkdownCalloutPainter.getCallout(renderer, context);
        return (callout != null) ? callout.color : renderer.style.quoteBarColor;
    }

    /**
     * Returns the color of the panel behind the quote, which can be unset. A callout whose type has no panel color has the panel of normal quotes.
     */
    @NotNull
    static MarkdownStyleOption.ColorOption getBackgroundColor(@NotNull MarkdownRenderer renderer, @NotNull MarkdownTextFragment.QuoteContext context) {
        MarkdownStyle.Callout callout = MarkdownCalloutPainter.getCallout(renderer, context);
        return ((callout != null) && callout.backgroundColor.isSet()) ? callout.backgroundColor : renderer.style.quoteBackgroundColor;
    }

    /**
     * Returns where the first render line of the quote starts.
     */
    static float getTop(@NotNull MarkdownTextFragment.QuoteContext context) {
        MarkdownTextFragment start = context.quoteFragments.get(0);
        return start.getRenderLineY();
    }

    /**
     * Returns where the last render line of the quote ends.<br>
     * The whole render line counts and not only the last fragment of the quote, because other fragments of the line can be taller.
     */
    static float getBottom(@NotNull MarkdownTextFragment.QuoteContext context) {
        MarkdownTextFragment end = context.quoteFragments.get(context.quoteFragments.size() - 1);
        float lastLineHeight = (end.parentLine != null) ? end.parentLine.getLineHeight() : end.getRenderHeight();
        return end.getRenderLineY() + lastLineHeight;
    }

    /**
     * Returns if the quote has a bar. A bar stands on the side the quote is aligned to, so centered quotes have none.
     */
    static boolean hasBar(@NotNull MarkdownTextFragment.QuoteContext context) {
        return context.quoteFragments.get(0).alignment != MarkdownRenderer.MarkdownLineAlignment.CENTERED;
    }

    static float getBarWidth(@NotNull MarkdownRenderer renderer) {
        return renderer.scaling.sized(renderer.style.quoteBarWidth.get());
    }

    /**
     * Returns the X position of the bar of the quote.<br>
     * Inside the text area, the bar stands in the space the quote indent keeps free next to the text. It must not reach into the padding
     * of the renderer unless the style asks for that, because containers like scroll areas clip their content there.
     */
    static float getBarX(@NotNull MarkdownTextFragment.QuoteContext context) {
        MarkdownRenderer renderer = context.quoteFragments.get(0).parent;
        float barWidth = getBarWidth(renderer);
        boolean inside = (renderer.style.quoteBarPosition.get() == MarkdownQuoteBarPosition.INSIDE);
        if (context.quoteFragments.get(0).alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT) {
            float textAreaRight = renderer.x + renderer.getRealWidth() - renderer.scaling.padding();
            return inside ? (textAreaRight - barWidth) : textAreaRight;
        }
        float textAreaLeft = renderer.x + renderer.scaling.padding();
        return inside ? textAreaLeft : (textAreaLeft - barWidth);
    }

}
