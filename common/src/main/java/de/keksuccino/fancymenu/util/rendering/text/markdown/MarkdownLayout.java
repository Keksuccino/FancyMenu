package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.AspectRatio;
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
    /** The lines got replaced since {@link #updateVisibleLines()} ran last, so it can't tell anymore which lines left the visible range. */
    private boolean linesReplaced = true;

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
        this.linesReplaced = true;
        this.spaceWidth = renderer.textFont.width(" ");

        boolean queueNewLine = true;
        float padding = renderer.scaling.padding();
        float currentLineWidth = padding;
        MarkdownTextFragment lastFragment = null;
        MarkdownTextLine line = new MarkdownTextLine(renderer);

        List<MarkdownTextFragment> fragments = renderer.fragments;
        for (int index = 0; index < fragments.size(); index++) {

            MarkdownTextFragment f = fragments.get(index);
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
                currentLineWidth = padding;
                queueNewLine = true;
                continue;
            }

            //Handle Auto Line Break
            if (!isStartOfLine && renderer.isAutoLineBreakingEnabled()) {
                f.startOfRenderLine = false;
                // The word has to fit as the last one of the line, because that is what it becomes when the next word wraps
                if (lastFragment.endOfWord && !isEmptyLineEnd(f) && ((lastFragment.codeBlockContext == null) || lastFragment.codeBlockContext.singleLine) && ((currentLineWidth + this.getWordWidthAtLineEnd(fragments, index) + padding) > renderer.optimalWidth)) {
                    currentLineWidth = padding;
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
                currentLineWidth = padding;
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
     * Returns the width the word that starts with the fragment at the given index needs as the last word of a render line.<br>
     * A word is made of several fragments when its format changes inside of it, like the bold word and the comma in "**word**,",
     * and a line can only wrap in front of the first one of them.<br>
     * A word of a single fragment needs room for the space behind it as well, although a line drops the space at its end:
     * that is where lines of plain words wrap, and texts are written around it. A word of several fragments only needs the room it really takes.
     */
    private float getWordWidthAtLineEnd(@NotNull List<MarkdownTextFragment> fragments, int index) {
        int first = index;
        float width = 0.0F;
        while (true) {
            MarkdownTextFragment fragment = fragments.get(index);
            boolean endsWord = fragment.endOfWord || fragment.naturalLineBreakAfter || (index == (fragments.size() - 1)) || fragments.get(index + 1).isTable();
            width += fragment.getRenderWidth(endsWord);
            if (endsWord) {
                if ((index > first) && fragment.text.endsWith(" ")) width -= this.spaceWidth * fragment.getScale();
                return width;
            }
            index++;
        }
    }

    /**
     * Returns if the fragment holds nothing and only ends its line. A line never wraps in front of such a fragment:
     * the line would not get any shorter, and the fragment would become an empty line of its own.
     */
    private static boolean isEmptyLineEnd(@NotNull MarkdownTextFragment fragment) {
        return fragment.naturalLineBreakAfter && fragment.text.isEmpty() && (fragment.imageSupplier == null);
    }

    /**
     * Measures the real size of the text and positions all lines and their fragments.<br>
     * Lines keep the size they got measured with when they got prepared. Only lines with images get measured again, because an image is never
     * wider than the text around it, which makes its size depend on all other lines.
     */
    void updateMetrics() {

        MarkdownRenderer renderer = this.renderer;
        List<MarkdownTextLine> lines = renderer.lines;
        float padding = renderer.scaling.padding();
        boolean containsImages = false;

        // Images get measured against the real width, so the real width they see has to be the one of all lines without images
        float maxLineWidth = 0;
        for (MarkdownTextLine line : lines) {
            if (line.containsImages) {
                containsImages = true;
            } else if (line.lineWidth > maxLineWidth) {
                maxLineWidth = line.lineWidth;
            }
        }
        renderer.realWidth = maxLineWidth;
        renderer.realWidth += padding + padding;

        this.measuredImages.clear();
        if (containsImages) {
            for (MarkdownTextLine line : lines) {
                if (!line.containsImages) continue;
                // Textures load on another thread, so an image can get its size at any moment. What a texture says gets remembered before its line
                // gets measured with it: a size that arrives in between then differs from the remembered one, which gets the line measured again.
                // Remembering it after measuring would hide such a size for good and leave the line at the size of an image that is not loaded.
                for (MarkdownTextFragment f : line.fragments) {
                    if (f.imageSupplier != null) this.measuredImages.add(new MeasuredImage(f));
                }
                line.updateSize();
                if (line.lineWidth > maxLineWidth) {
                    maxLineWidth = line.lineWidth;
                }
            }
            // An image is never wider than the real width it got measured against, so this doesn't change the size of the images
            renderer.realWidth = maxLineWidth;
            renderer.realWidth += padding + padding;
        }

        float realInnerWidth = renderer.getRealWidth() - padding - padding;
        float lineOffsetY = padding;
        // Lines only stay sorted by their position while no space between two of them is negative
        boolean sorted = true;
        MarkdownTextLine previousLine = null;
        boolean collapsedBlankLineBetween = false;
        int blankLineRow = 0;
        int lineIndex = 0;
        for (MarkdownTextLine line : lines) {
            line.index = lineIndex++;
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
            line.layoutOffsetX = padding + renderer.scaling.snapOffset(lineAlignmentOffsetX);
            line.offsetX = line.layoutOffsetX;
            blankLineRow = (line.blockType == MarkdownBlockType.BLANK) ? (blankLineRow + 1) : 0;
            line.collapsed = (blankLineRow > 0) && MarkdownBlockSpacing.isCollapsedBlankLine(renderer, blankLineRow);
            if (line.collapsed) {
                // The line stays where the content in front of it ends and takes no space, so the blocks around it are all that makes the space between them
                line.lineHeight = 0;
                collapsedBlankLineBetween = true;
            } else {
                float space = (previousLine == null) ? MarkdownBlockSpacing.aboveFirst(renderer, line) : MarkdownBlockSpacing.between(renderer, previousLine, line, collapsedBlankLineBetween);
                if (space < 0.0F) sorted = false;
                lineOffsetY += space;
                previousLine = line;
                collapsedBlankLineBetween = false;
            }
            line.offsetY = lineOffsetY;
            // Lines outside the visible range don't render, so this is where their fragments get a position
            line.onRender(null, 0, 0, 0, false);
            lineOffsetY += line.lineHeight;
        }
        if (previousLine != null) lineOffsetY += MarkdownBlockSpacing.belowLast(renderer, previousLine);
        renderer.realHeight = lineOffsetY + padding;

        this.linesSortedByPosition = sorted;

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
     * Looks up the lines inside the visible vertical range of the renderer, and sets {@link #firstVisibleLine} and {@link #visibleLinesEnd} to them.<br>
     * While the lines are sorted by their position, the range gets found with a binary search instead of walking all lines, and holds nothing
     * but visible lines. Otherwise it holds all lines, and every line in it still has to pass {@link #isLineVisible(MarkdownTextLine)}.<br><br>
     *
     * Fragments only update their hover state while they render, so the fragments of every line that left the visible range lose it here.
     */
    void updateVisibleLines() {
        List<MarkdownTextLine> lines = this.renderer.lines;
        int lineCount = lines.size();
        int first = 0;
        int end = lineCount;
        if (this.linesSortedByPosition) {
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
            first = low;
            high = lineCount;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (this.isLineBelowVisibleRange(lines.get(mid))) {
                    high = mid;
                } else {
                    low = mid + 1;
                }
            }
            // Rounding can make two lines that are almost at the same position disagree about which one of them is outside the range,
            // so the range gets widened by one line on both ends before it gets cut down to the lines that really are visible
            first = Math.max(0, first - 1);
            end = Math.min(lineCount, low + 1);
            while ((first < end) && !this.isLineVisible(lines.get(first))) first++;
            while ((end > first) && !this.isLineVisible(lines.get(end - 1))) end--;
        }
        this.resetHoverOfHiddenLines(first, end);
        this.firstVisibleLine = first;
        this.visibleLinesEnd = end;
    }

    /**
     * Makes the fragments of the lines forget the mouse that are not part of the given new range of visible lines anymore.
     * Only the lines of the last range can be hovered, so only those get checked, except when it is unknown which lines these are.
     */
    private void resetHoverOfHiddenLines(int newFirstVisibleLine, int newVisibleLinesEnd) {
        List<MarkdownTextLine> lines = this.renderer.lines;
        boolean checkAllLines = this.linesReplaced || !this.linesSortedByPosition;
        this.linesReplaced = false;
        int first = checkAllLines ? 0 : this.firstVisibleLine;
        int end = checkAllLines ? lines.size() : Math.min(this.visibleLinesEnd, lines.size());
        for (int i = first; i < end; i++) {
            MarkdownTextLine line = lines.get(i);
            // A range of sorted lines holds nothing but visible lines
            if ((i >= newFirstVisibleLine) && (i < newVisibleLinesEnd) && (this.linesSortedByPosition || this.isLineVisible(line))) continue;
            this.renderer.interactions.resetHoveredFragments(line.fragments);
        }
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
        if (Float.isNaN(this.spaceWidth)) this.spaceWidth = this.renderer.textFont.width(" ");
        return this.spaceWidth;
    }

    /**
     * What the texture of an image said about its size right before the line of the image got measured.<br>
     * A texture that loads sets its width, its height and its aspect ratio one after the other, and the size of an image comes from its width
     * and its aspect ratio, so all of them count: the line got measured with something else as soon as one of them differs.
     */
    private static final class MeasuredImage {

        private final MarkdownTextFragment fragment;
        @Nullable
        private final ITexture texture;
        private final int width;
        private final int height;
        private final int aspectRatioWidth;
        private final int aspectRatioHeight;

        private MeasuredImage(@NotNull MarkdownTextFragment fragment) {
            this.fragment = fragment;
            this.texture = fragment.imageSupplier.get();
            if (this.texture != null) {
                this.width = this.texture.getWidth();
                this.height = this.texture.getHeight();
                AspectRatio aspectRatio = this.texture.getAspectRatio();
                this.aspectRatioWidth = aspectRatio.getInputWidth();
                this.aspectRatioHeight = aspectRatio.getInputHeight();
            } else {
                this.width = 0;
                this.height = 0;
                this.aspectRatioWidth = 0;
                this.aspectRatioHeight = 0;
            }
        }

        private boolean hasChanged() {
            ITexture current = this.fragment.imageSupplier.get();
            if (current != this.texture) return true;
            if (current == null) return false;
            if ((current.getWidth() != this.width) || (current.getHeight() != this.height)) return true;
            AspectRatio aspectRatio = current.getAspectRatio();
            return (aspectRatio.getInputWidth() != this.aspectRatioWidth) || (aspectRatio.getInputHeight() != this.aspectRatioHeight);
        }

    }

}
