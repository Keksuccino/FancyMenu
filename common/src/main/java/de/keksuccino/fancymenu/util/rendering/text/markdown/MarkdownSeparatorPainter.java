package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownSeparatorStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * Measures and draws separators. Everything about a separator scales with normal text.
 */
final class MarkdownSeparatorPainter {

    private MarkdownSeparatorPainter() {
    }

    static float getHeight(@NotNull MarkdownTextFragment fragment) {
        MarkdownStyle style = fragment.parent.style;
        float size = (style.separatorStyle.get() == MarkdownSeparatorStyle.ORNAMENT) ? Math.max(style.separatorOrnamentSize.get(), style.separatorThickness.get()) : style.separatorThickness.get();
        return size * fragment.getScale();
    }

    static void render(@NotNull GuiGraphics graphics, @NotNull MarkdownTextFragment fragment) {
        MarkdownRenderer renderer = fragment.parent;
        MarkdownStyle style = renderer.style;
        MarkdownScaling scaling = renderer.scaling;
        float pixel = fragment.getScale();
        float left = renderer.x + scaling.padding();
        float right = renderer.x + renderer.getRealWidth() - scaling.padding();
        float thickness = style.separatorThickness.get() * pixel;
        int color = renderer.getRenderColor(style.separatorColor);
        if (style.separatorStyle.get() == MarkdownSeparatorStyle.LINE) {
            RenderingUtils.fillF(graphics, left, fragment.y, right, fragment.y + thickness, color);
            RenderingUtils.resetShaderColor(graphics);
            return;
        }
        float height = getHeight(fragment);
        float ornamentSize = style.separatorOrnamentSize.get() * pixel;
        float gap = style.separatorOrnamentGap.get() * pixel;
        // The ornament stands in the middle of the text area, on the grid its pixels share with the text
        float center = left + scaling.snapOffset((right - left) / 2);
        float ornamentLeft = center - ((float) Math.floor(style.separatorOrnamentSize.get() / 2) * pixel);
        float ornamentRight = ornamentLeft + ornamentSize;
        MarkdownPaint.marker(graphics, renderer, style.separatorOrnamentShape.get(), ornamentLeft, fragment.y + ((height - ornamentSize) / 2), ornamentSize, pixel, renderer.getRenderColor(style.separatorOrnamentColor));

        // Both lines are equally long and stay one pixel away from the edges of the text area
        float lineY = fragment.y + ((height - thickness) / 2);
        int reach = (int) Math.floor(Math.min(ornamentLeft - gap - left, right - ornamentRight - gap) / thickness) - 1;
        if (style.separatorReach.get() > 0.0F) reach = Math.min(reach, (int) Math.floor(style.separatorReach.get() * pixel / thickness));
        if (reach <= 0) return;
        // A line fades out in two steps towards its outer end: every second pixel of it, then every fourth one.
        // Which pixels are left depends on how far they are from the ornament, so both lines mirror each other.
        int fade = Math.min(reach, Math.round(style.separatorFadeLength.get() * pixel / thickness));
        int quarter = fade / 2;
        int half = fade - quarter;
        int solid = reach - fade;
        float rightStart = ornamentRight + gap;
        RenderingUtils.fillF(graphics, rightStart, lineY, rightStart + (solid * thickness), lineY + thickness, color);
        MarkdownPixelArt.pattern(graphics, MarkdownPixelArt.Pattern.HALF, rightStart + (solid * thickness), lineY, half, solid % MarkdownPixelArt.Pattern.HALF.period, thickness, color);
        MarkdownPixelArt.pattern(graphics, MarkdownPixelArt.Pattern.QUARTER, rightStart + ((solid + half) * thickness), lineY, quarter, (solid + half) % MarkdownPixelArt.Pattern.QUARTER.period, thickness, color);
        float leftEnd = ornamentLeft - gap;
        RenderingUtils.fillF(graphics, leftEnd - (solid * thickness), lineY, leftEnd, lineY + thickness, color);
        MarkdownPixelArt.pattern(graphics, MarkdownPixelArt.Pattern.HALF, leftEnd - ((solid + half) * thickness), lineY, half, mirroredPhase(solid + half, MarkdownPixelArt.Pattern.HALF), thickness, color);
        MarkdownPixelArt.pattern(graphics, MarkdownPixelArt.Pattern.QUARTER, leftEnd - (reach * thickness), lineY, quarter, mirroredPhase(reach, MarkdownPixelArt.Pattern.QUARTER), thickness, color);
        RenderingUtils.resetShaderColor(graphics);
    }

    /**
     * Returns the phase a pattern has to start with to end up the same as one that runs the other way.
     *
     * @param end How many pixels the far end of the pattern is away from where the mirrored pattern starts counting.
     */
    static int mirroredPhase(int end, @NotNull MarkdownPixelArt.Pattern pattern) {
        return (pattern.period - ((end - 1) % pattern.period)) % pattern.period;
    }

}
