package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.RenderScaleUtil;
import de.keksuccino.fancymenu.util.rendering.RenderTranslationUtil;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import org.jetbrains.annotations.NotNull;

/**
 * Turns the sizes and positions of a {@link MarkdownRenderer} into what gets drawn: it scales the sizes of the style with the text,
 * and it rounds text scales and positions to whole screen pixels where the style asks for that.<br><br>
 *
 * Rounding needs to know how many screen pixels a GUI pixel covers and where the render pose puts the origin, which both are only
 * known while rendering. The renderer hands them over at the start of every frame, and lays out its lines again when the scale changed.
 */
final class MarkdownScaling {

    private final MarkdownRenderer renderer;
    /** Screen pixels per GUI pixel, with the scale of the render pose. Only up to date while the style rounds text scales or positions. */
    private float renderScale = 1.0F;
    /** Where the render pose moves the origin to, in screen pixels. Only up to date while the style rounds positions. */
    private float renderTranslationX = 0.0F;
    private float renderTranslationY = 0.0F;

    MarkdownScaling(@NotNull MarkdownRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Reads the scale things get rendered with right now.
     *
     * @return If the scale changed in a way that changes the size of text, which makes the lines outdated.
     */
    boolean updateRenderScale() {
        MarkdownStyle style = this.renderer.style;
        if (!style.snapTextScale.get() && !style.snapPositions.get()) return false;
        float scale = this.renderer.getRenderScale();
        if (!Float.isFinite(scale) || (scale <= 0.0F) || (scale == this.renderScale)) return false;
        this.renderScale = scale;
        return style.snapTextScale.get();
    }

    /**
     * Reads where the render pose puts the origin right now. Has to run before lines get positioned for rendering.
     */
    void updateRenderTranslation() {
        if (!this.renderer.style.snapPositions.get()) return;
        this.renderTranslationX = RenderTranslationUtil.getCurrentRenderTranslationX();
        this.renderTranslationY = RenderTranslationUtil.getCurrentRenderTranslationY();
    }

    /**
     * Returns the scale of text whose size is the given factor of the size of normal text.
     */
    float textScale(float factor) {
        float scale = factor * this.renderer.textBaseScale;
        if (!Float.isFinite(scale) || (scale <= 0.0F)) return 0.0F;
        if (!this.renderer.style.snapTextScale.get()) return scale;
        // One pixel of the font covers at least one screen pixel, and never a fraction of one
        return Math.max(1, Math.round(scale * this.renderScale)) / this.renderScale;
    }

    /**
     * Returns the scale of normal text, which is the size of one pixel of the style.
     */
    float bodyScale() {
        return this.textScale(1.0F);
    }

    /**
     * Scales a size of the style that grows with the text in every look, like an indent.
     */
    float scaled(float size) {
        return size * this.bodyScale();
    }

    /**
     * Scales a size of the style that only grows with the text if the style says so, like the line spacing.
     */
    float sized(float size) {
        return this.renderer.style.scaleSizesWithText.get() ? (size * this.bodyScale()) : size;
    }

    float padding() {
        return this.sized(this.renderer.style.padding.get());
    }

    float lineSpacing() {
        return this.sized(this.renderer.style.lineSpacing.get());
    }

    /**
     * Returns the distance rounded down to whole pixels of the style, if the style rounds positions.<br>
     * Things that get centered or aligned to the right end up a fraction of a pixel away from everything around them otherwise,
     * which keeps them off the grid all other pixels of the text share.
     */
    float snapOffset(float offset) {
        float pixel = this.bodyScale();
        if (!this.renderer.style.snapPositions.get() || (pixel <= 0.0F)) return offset;
        return (float) Math.floor(offset / pixel) * pixel;
    }

    /**
     * Returns the X position rounded down to a whole screen pixel, if the style rounds positions.
     */
    float snapX(float x) {
        if (!this.renderer.style.snapPositions.get()) return x;
        return RenderScaleUtil.snapGuiCoordinateToPixel(x, this.renderScale, this.renderTranslationX);
    }

    /**
     * Returns the Y position rounded down to a whole screen pixel, if the style rounds positions.
     */
    float snapY(float y) {
        if (!this.renderer.style.snapPositions.get()) return y;
        return RenderScaleUtil.snapGuiCoordinateToPixel(y, this.renderScale, this.renderTranslationY);
    }

}
