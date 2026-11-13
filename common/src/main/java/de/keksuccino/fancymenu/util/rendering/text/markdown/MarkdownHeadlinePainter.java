package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownHeadlineDecoration;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * Measures and draws what the style decorates headlines with: the line below a headline or the dots behind it.
 */
final class MarkdownHeadlinePainter {

    private MarkdownHeadlinePainter() {
    }

    /**
     * Returns the space a render line of a headline keeps free below its text.
     */
    static float getLinePaddingBottom(@NotNull MarkdownTextLine line) {
        MarkdownScaling scaling = line.parent.scaling;
        MarkdownStyle.Headline headline = getHeadline(line);
        float padding = scaling.sized(headline.linePadding.get());
        if ((headline.decoration.get() == MarkdownHeadlineDecoration.RULE) && isLastLineOfHeadline(line)) {
            // The rule and the row of its shadow
            padding += scaling.sized(headline.decorationOffset.get()) + (scaling.sized(headline.decorationThickness.get()) * 2);
        }
        return padding;
    }

    static void render(@NotNull GuiGraphics graphics, @NotNull MarkdownTextLine line) {
        MarkdownRenderer renderer = line.parent;
        MarkdownStyle.Headline headline = getHeadline(line);
        MarkdownHeadlineDecoration decoration = headline.decoration.get();
        if (decoration == MarkdownHeadlineDecoration.NONE) return;
        if ((decoration != MarkdownHeadlineDecoration.UNDERLINE) && !isLastLineOfHeadline(line)) return;
        MarkdownScaling scaling = renderer.scaling;
        MarkdownTextFragment first = line.fragments.get(0);
        float left = renderer.x + scaling.padding();
        float right = renderer.x + renderer.getRealWidth() - scaling.padding() - scaling.sized(renderer.style.blockEdgeInset.get());
        float thickness = scaling.sized(headline.decorationThickness.get());
        // Half a GUI pixel is a whole screen pixel from a GUI scale of 2 on. Below that, a thinner line would vanish between two screen pixels.
        if (renderer.getBaseRenderScale() <= 1.0F) thickness = Math.max(thickness, 1.0F);
        int color = renderer.getRenderColor(headline.decorationColor);
        if (decoration == MarkdownHeadlineDecoration.LEADER) {
            if (line.alignment != MarkdownRenderer.MarkdownLineAlignment.LEFT) return;
            float start = first.x + line.getLineWidth() + scaling.sized(renderer.style.headlineLeaderGap.get());
            // The dots stand on the baseline of the headline
            float y = first.getTextY() + (renderer.getFontMetrics().baseline * first.getScale()) - thickness;
            MarkdownPixelArt.pattern(graphics, MarkdownPixelArt.Pattern.THIRD, start, y, (float) Math.floor((right - start) / thickness), 0, thickness, color);
            return;
        }
        float y = first.getTextY() + first.getTextRenderHeight() + scaling.sized(headline.decorationOffset.get());
        float fade = (decoration == MarkdownHeadlineDecoration.RULE) ? Math.min(Math.max(0.0F, right - left), scaling.sized(renderer.style.headlineRuleFadeLength.get())) : 0.0F;
        float solidEnd = right - fade;
        RenderingUtils.fillF(graphics, left, y, solidEnd, y + thickness, color);
        if (decoration != MarkdownHeadlineDecoration.RULE) return;
        if (renderer.style.headlineRuleShadowColor.isSet()) {
            RenderingUtils.fillF(graphics, left + thickness, y + thickness, solidEnd + thickness, y + (thickness * 2), renderer.getRenderColor(renderer.style.headlineRuleShadowColor));
        }
        // The rule fades out in two steps: every second pixel of it, then every fourth one
        int fadePixels = Math.round(fade / thickness);
        int halfPixels = fadePixels / 2;
        MarkdownPixelArt.pattern(graphics, MarkdownPixelArt.Pattern.HALF, solidEnd, y, halfPixels, 0, thickness, color);
        MarkdownPixelArt.pattern(graphics, MarkdownPixelArt.Pattern.QUARTER, solidEnd + (halfPixels * thickness), y, fadePixels - halfPixels, halfPixels % MarkdownPixelArt.Pattern.QUARTER.period, thickness, color);
    }

    @NotNull
    private static MarkdownStyle.Headline getHeadline(@NotNull MarkdownTextLine line) {
        return line.parent.style.headline(line.fragments.get(0).headlineType.level);
    }

    /**
     * A headline is one line of the text, so it ends in the render line that does not wrap into another one.
     */
    private static boolean isLastLineOfHeadline(@NotNull MarkdownTextLine line) {
        return !line.fragments.get(line.fragments.size() - 1).autoLineBreakAfter;
    }

}
