package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * Measures and draws what is behind code: the background of inline code and the panel of a code block.
 */
final class MarkdownCodePainter {

    private MarkdownCodePainter() {
    }

    /**
     * Returns the space a line of a code block keeps free in front of and behind its text.
     */
    static float getBlockPaddingHorizontal(@NotNull MarkdownRenderer renderer) {
        return renderer.scaling.scaled(renderer.style.codeBlockPaddingHorizontal.get());
    }

    /**
     * Returns the space the first line of a code block keeps free above its text, and the last one below it.
     */
    static float getBlockPaddingVertical(@NotNull MarkdownRenderer renderer) {
        return renderer.scaling.scaled(renderer.style.codeBlockPaddingVertical.get());
    }

    /**
     * Draws the panel of the code block the given line is a part of. The panel covers all lines of the block, no matter which of them are visible.
     */
    static void renderBlock(@NotNull GuiGraphics graphics, @NotNull MarkdownTextLine line) {
        MarkdownRenderer renderer = line.parent;
        MarkdownStyle style = renderer.style;
        MarkdownScaling scaling = renderer.scaling;
        MarkdownTextFragment.CodeBlockContext context = line.fragments.get(0).codeBlockContext;
        MarkdownTextFragment start = context.getBlockStart();
        MarkdownTextFragment end = context.getBlockEnd();
        if ((start == null) || (end == null) || (start.parentLine == null) || (end.parentLine == null)) return;
        float inset = scaling.sized(style.blockEdgeInset.get());
        float pixel = scaling.sized(1.0F);
        float radius = MarkdownPaint.cornerRadius(renderer, style.codeCornerRadius.get());
        float minX = renderer.x + scaling.padding();
        float maxX = renderer.x + renderer.getRealWidth() - scaling.padding() - inset;
        float minY = start.parentLine.getRenderY();
        float maxY = end.parentLine.getRenderY() + end.parentLine.getLineHeight() - inset;
        MarkdownPaint.box(graphics, renderer, minX, minY, maxX, maxY, radius, renderer.getRenderColor(style.codeBlockBackgroundColor));
        if (style.codeBlockInnerShadowColor.isSet()) {
            // The shadow the upper edge of the frame throws onto the panel
            RenderingUtils.fillF(graphics, minX + pixel, minY + pixel, maxX - pixel, minY + (pixel * 2), renderer.getRenderColor(style.codeBlockInnerShadowColor));
        }
        MarkdownPaint.frame(graphics, renderer, minX, minY, maxX, maxY, pixel, radius, renderer.getRenderColor(style.codeBlockFrameTopColor), renderer.getRenderColor(style.codeBlockFrameBottomColor));
        RenderingUtils.resetShaderColor(graphics);
    }

    /**
     * Draws the background of inline code, from its first to its last fragment in a line or in a table cell.
     */
    static void renderInline(@NotNull GuiGraphics graphics, @NotNull MarkdownTextFragment start, @NotNull MarkdownTextFragment end) {
        MarkdownRenderer renderer = start.parent;
        MarkdownPaint.box(graphics, renderer, start.x, getInlineTop(start), getInlineRight(end), getInlineBottom(start), MarkdownPaint.cornerRadius(renderer, renderer.style.codeCornerRadius.get()), renderer.getRenderColor(renderer.style.inlineCodeBackgroundColor));
        RenderingUtils.resetShaderColor(graphics);
    }

    /**
     * Returns where the background of the inline code of the fragment starts.<br>
     * The background belongs to the text, so it follows everything that moves the text inside its line.
     */
    static float getInlineTop(@NotNull MarkdownTextFragment fragment) {
        return fragment.getTextY() - fragment.parent.scaling.sized(fragment.parent.style.inlineCodePaddingTop.get());
    }

    static float getInlineBottom(@NotNull MarkdownTextFragment fragment) {
        return fragment.getTextY() + fragment.getTextRenderHeight() + fragment.parent.scaling.sized(fragment.parent.style.inlineCodePaddingBottom.get());
    }

    /**
     * Returns where the background of inline code ends that the given fragment is the last one of in its line.
     * The background ends behind the text and its padding. A space behind the text is not part of it.
     */
    static float getInlineRight(@NotNull MarkdownTextFragment end) {
        MarkdownRenderer renderer = end.parent;
        float right = end.x + end.getRenderWidth();
        if (end.text.endsWith(" ")) right -= renderer.getUnscaledSpaceWidth() * end.getScale();
        if (renderer.style.trimGlyphSpacing.get()) right -= renderer.textFont.metrics().glyphSpacing * end.getScale();
        return right;
    }

}
