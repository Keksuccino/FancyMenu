package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Arranges the fragments of a {@link MarkdownRenderer} in render lines, and holds everything that only changes together with that arrangement:
 * the size and position of every line, the real size of the whole text, and the lookup of the lines inside the visible area.<br><br>
 *
 * Nothing in here runs per frame except {@link #updateVisibleLines()} and {@link #haveImagesChanged()}, which don't walk the text.
 */
final class MarkdownLayout {

    private final MarkdownRenderer renderer;
    /** The textures the image fragments got measured with, to notice images that finished loading or got replaced. */
    private final List<MeasuredImage> measuredImages = new ArrayList<>();
    /** Lines only can get looked up with a binary search while every line starts and ends at or below the line before it. */
    private boolean linesSortedByPosition = true;
    private float spaceWidth = Float.NaN;
    /** Index of the first line that can be inside the visible range. Set by {@link #updateVisibleLines()}. */
    int firstVisibleLine = 0;
    /** Index after the last line that can be inside the visible range. Set by {@link #updateVisibleLines()}. */
    int visibleLinesEnd = 0;

    MarkdownLayout(@NotNull MarkdownRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Splits the fragments of the renderer into render lines, then measures and positions the lines.
     */
    void rebuildLines() {

        MarkdownRenderer renderer = this.renderer;
        List<MarkdownTextLine> lines = renderer.lines;
        lines.clear();
        this.spaceWidth = renderer.getUnscaledTextWidth(" ");

        boolean queueNewLine = true;
        float currentLineWidth = renderer.border;
        MarkdownTextFragment lastFragment = null;
        MarkdownTextLine line = new MarkdownTextLine(renderer);

        for (MarkdownTextFragment f : renderer.fragments) {

            boolean isStartOfLine = queueNewLine;
            queueNewLine = false;

            f.autoLineBreakAfter = false;

            // Tables should always be on their own line
            if (f.isTable()) {
                f.tableContext.updateMetrics(renderer);
                // If there's content on the current line, finish it
                if (!isStartOfLine && !line.fragments.isEmpty()) {
                    line.prepareLine();
                    lines.add(line);
                    line = new MarkdownTextLine(renderer);
                }
                // Add table as its own line
                line.fragments.add(f);
                line.prepareLine();
                lines.add(line);
                // Reset for next line
                line = new MarkdownTextLine(renderer);
                currentLineWidth = renderer.border;
                queueNewLine = true;
                continue;
            }

            //Handle Auto Line Break
            if (!isStartOfLine && renderer.isAutoLineBreakingEnabled()) {
                f.startOfRenderLine = false;
                if (lastFragment.endOfWord && ((lastFragment.codeBlockContext == null) || lastFragment.codeBlockContext.singleLine) && ((currentLineWidth + f.getRenderWidth() + renderer.border) > renderer.optimalWidth)) {
                    currentLineWidth = renderer.border;
                    isStartOfLine = true;
                    line.prepareLine();
                    lines.add(line);
                    line = new MarkdownTextLine(renderer);
                }
            }

            f.startOfRenderLine = isStartOfLine;

            line.fragments.add(f);

            currentLineWidth += f.getRenderWidth();

            //Handle Natural Line Break
            if (f.naturalLineBreakAfter) {
                currentLineWidth = renderer.border;
                queueNewLine = true;
                line.prepareLine();
                lines.add(line);
                line = new MarkdownTextLine(renderer);
            }

            lastFragment = f;

        }

        if (lines.isEmpty() || !line.fragments.isEmpty()) {
            line.prepareLine();
            lines.add(line);
        }

        this.updateMetrics();

    }

    /**
     * Measures the real size of the text and positions all lines and their fragments.<br>
     * Lines keep the size they got measured with when they got prepared. Only lines with images get measured again, because an image is never
     * wider than the text, which makes its size depend on the lines around it.
     */
    void updateMetrics() {

        MarkdownRenderer renderer = this.renderer;
        List<MarkdownTextLine> lines = renderer.lines;
        boolean containsImages = false;

        // The real width grows line by line here, and an image line gets measured against the width of the lines before it
        renderer.realWidth = 0;
        renderer.realHeight = 0;
        for (MarkdownTextLine line : lines) {
            if (line.containsImages) {
                containsImages = true;
                line.updateSize();
            }
            if (line.lineWidth > renderer.realWidth) {
                renderer.realWidth = line.lineWidth;
            }
            renderer.realHeight += line.lineHeight + renderer.lineSpacing;
        }
        renderer.realWidth += renderer.border + renderer.border;
        renderer.realHeight += renderer.border + renderer.border;

        this.measuredImages.clear();
        if (containsImages) {
            // Images render against the final real width, so their lines get positioned with that size
            for (MarkdownTextLine line : lines) {
                if (!line.containsImages) continue;
                line.updateSize();
                for (MarkdownTextFragment f : line.fragments) {
                    if (f.imageSupplier != null) this.measuredImages.add(new MeasuredImage(f));
                }
            }
        }

        float realInnerWidth = renderer.getRealWidth() - renderer.border - renderer.border;
        float lineOffsetY = renderer.border;
        for (MarkdownTextLine line : lines) {
            float lineAlignmentOffsetX = 0;
            if (line.isAlignmentAllowed(line.alignment)) {
                if (line.alignment == MarkdownRenderer.MarkdownLineAlignment.CENTERED) {
                    lineAlignmentOffsetX = Math.max(0, realInnerWidth - line.lineWidth);
                    if (lineAlignmentOffsetX > 0) lineAlignmentOffsetX = lineAlignmentOffsetX / 2f;
                }
                if (line.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT) {
                    lineAlignmentOffsetX = Math.max(0, realInnerWidth - line.lineWidth);
                }
            }
            line.layoutOffsetX = renderer.border + lineAlignmentOffsetX;
            line.offsetX = line.layoutOffsetX;
            line.offsetY = lineOffsetY;
            // Lines outside the visible range don't render, so this is where their fragments get a position
            line.onRender(null, 0, 0, 0, false);
            lineOffsetY += line.lineHeight + renderer.lineSpacing;
        }

        // Line heights are never negative, so only a negative line spacing can move a line above the one before it
        this.linesSortedByPosition = (renderer.lineSpacing >= 0.0F);

    }

    /**
     * Checks if an image of the text got a different texture or texture size than the one the lines got measured with.
     * {@link #updateMetrics()} has to run when that happens.
     */
    boolean haveImagesChanged() {
        for (int i = 0; i < this.measuredImages.size(); i++) {
            if (this.measuredImages.get(i).hasChanged()) return true;
        }
        return false;
    }

    /**
     * Looks up the lines that can be inside the visible vertical range of the renderer, and sets {@link #firstVisibleLine} and
     * {@link #visibleLinesEnd} to them. Every line in that range still has to pass {@link #isLineVisible(MarkdownTextLine)}.<br><br>
     *
     * Lines are sorted by their position, so the range gets found with a binary search instead of walking all lines. Rounding can make two
     * lines that are almost at the same position disagree about which one of them is outside the range, so the range gets widened by one line on both ends.
     */
    void updateVisibleLines() {
        List<MarkdownTextLine> lines = this.renderer.lines;
        int lineCount = lines.size();
        if (!this.linesSortedByPosition) {
            this.firstVisibleLine = 0;
            this.visibleLinesEnd = lineCount;
            return;
        }
        int low = 0;
        int high = lineCount;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (this.isLineAboveVisibleRange(lines.get(mid))) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        int first = low;
        high = lineCount;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (this.isLineBelowVisibleRange(lines.get(mid))) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        this.firstVisibleLine = Math.max(0, first - 1);
        this.visibleLinesEnd = Math.min(lineCount, low + 1);
    }

    /**
     * A line is visible when it is not completely above or below the visible vertical range of the renderer.
     */
    boolean isLineVisible(@NotNull MarkdownTextLine line) {
        return !this.isLineAboveVisibleRange(line) && !this.isLineBelowVisibleRange(line);
    }

    private boolean isLineAboveVisibleRange(@NotNull MarkdownTextLine line) {
        return (this.renderer.y + line.offsetY + line.lineHeight) < this.renderer.visibleTop;
    }

    private boolean isLineBelowVisibleRange(@NotNull MarkdownTextLine line) {
        return (this.renderer.y + line.offsetY) > this.renderer.visibleBottom;
    }

    /**
     * The width of a space, measured once per layout, because lines and code blocks need it a lot.
     */
    float getSpaceWidth() {
        if (Float.isNaN(this.spaceWidth)) this.spaceWidth = this.renderer.getUnscaledTextWidth(" ");
        return this.spaceWidth;
    }

    private static final class MeasuredImage {

        private final MarkdownTextFragment fragment;
        @Nullable
        private final ITexture texture;
        private final int width;
        private final int height;

        private MeasuredImage(@NotNull MarkdownTextFragment fragment) {
            this.fragment = fragment;
            this.texture = fragment.imageSupplier.get();
            this.width = (this.texture != null) ? this.texture.getWidth() : 0;
            this.height = (this.texture != null) ? this.texture.getHeight() : 0;
        }

        private boolean hasChanged() {
            ITexture current = this.fragment.imageSupplier.get();
            if (current != this.texture) return true;
            return (current != null) && ((current.getWidth() != this.width) || (current.getHeight() != this.height));
        }

    }

}
