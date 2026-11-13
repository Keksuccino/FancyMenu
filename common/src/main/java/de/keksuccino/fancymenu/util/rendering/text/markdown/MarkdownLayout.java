package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Arranges the fragments of a {@link MarkdownRenderer} in render lines, and holds everything that only changes together with that arrangement:
 * the size and position of every line, the real size of the whole text, and the lookup of the lines inside the visible area.<br><br>
 *
 * A line ends behind a word, or between two characters of a script that puts no spaces between words ({@link MarkdownLineBreaks}).
 * Text of such a script is one fragment for a whole sentence or paragraph, so a line can end inside of a fragment. The layout splits that fragment
 * into one piece for every line it is in, and only that one: text that fits into its line stays the fragment the parser built.<br><br>
 *
 * Nothing in here runs per frame except {@link #updateVisibleLines()} and {@link #haveImagesChanged()}, which don't walk the text.
 */
final class MarkdownLayout {

    private final MarkdownRenderer renderer;
    /** The textures the image fragments got measured with, to notice images that finished loading or got replaced. */
    private final List<MeasuredImage> measuredImages = new ArrayList<>();
    /** The fragments a line ends inside of, which got split into one piece for every line they are in. They are whole again before the next layout. */
    private final List<MarkdownTextFragment> splitFragments = new ArrayList<>();
    /** Lines only can get looked up with a binary search while every line starts and ends at or below the line before it. */
    private boolean linesSortedByPosition = true;
    private float spaceWidth = Float.NaN;
    /** Index of the first line that can be inside the visible range. Set by {@link #updateVisibleLines()}. */
    int firstVisibleLine = 0;
    /** Index after the last line that can be inside the visible range. Set by {@link #updateVisibleLines()}. */
    int visibleLinesEnd = 0;
    /** The lines got replaced since {@link #updateVisibleLines()} ran last, so it can't tell anymore which lines left the visible range. */
    private boolean linesReplaced = true;
    /** The unscaled width of the text the line takes that {@link #findLineEnd(MarkdownTextFragment, Style, MarkdownTextFragment, int, float, float)} looked at last. */
    private float lineEndWidth;

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
        // Lines end at other places now, so every fragment starts as a whole again
        for (int i = 0; i < this.splitFragments.size(); i++) this.splitFragments.get(i).joinPieces();
        this.splitFragments.clear();

        boolean queueNewLine = true;
        boolean autoLineBreaks = renderer.isAutoLineBreakingEnabled();
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
            if (!isStartOfLine && autoLineBreaks) {
                f.startOfRenderLine = false;
                // A line can break behind a word, and between two characters of a script without spaces.
                // The word has to fit as the last one of the line, because that is what it becomes when the next word wraps.
                if ((lastFragment.endOfWord || MarkdownLineBreaks.isAllowedBetween(lastFragment.text, f.text)) && !isEmptyLineEnd(f) && ((lastFragment.codeBlockContext == null) || lastFragment.codeBlockContext.singleLine) && ((currentLineWidth + this.getWordWidthAtLineEnd(fragments, index, f, false, currentLineWidth) + padding) > renderer.optimalWidth)) {
                    currentLineWidth = padding;
                    isStartOfLine = true;
                    line.prepareLine();
                    lines.add(line);
                    line = new MarkdownTextLine(renderer);
                }
            }

            f.startOfRenderLine = isStartOfLine;

            //Handle Auto Line Break inside of the fragment
            if (autoLineBreaks && canBreakInside(f)) {
                // The fragment gets split into one piece for every line that ends inside of it. Its text can take many lines, so the text behind
                // the current line only becomes a piece when it is known to end in the next line: copying it for every line would take longer the longer the text is.
                String text = f.text;
                int textLength = text.length();
                Style style = null;
                int start = 0;
                // The width of the text behind the start, which is exact for the whole text and close to it after that
                float restWidth = f.unscaledTextWidth;
                MarkdownTextFragment piece = f;
                boolean pieceHoldsRest = true;
                while (true) {
                    if (pieceHoldsRest && this.fitsAtLineEnd(fragments, index, piece, currentLineWidth)) break;
                    if (style == null) style = f.buildRenderStyle(true);
                    int lineEnd = this.findLineEnd(f, style, piece, start, currentLineWidth, restWidth);
                    boolean lastBreak = (lineEnd != -1) && (MarkdownLineBreaks.next(text, lineEnd + 1, textLength) == -1);
                    if (!pieceHoldsRest && ((lineEnd == -1) || lastBreak)) {
                        // Nothing or only the text behind the last place to break at is left for the next line, so the rest is short enough to become a piece
                        f.setPieceText(piece, start, textLength, Float.NaN);
                        pieceHoldsRest = true;
                        if (lastBreak && this.fitsAtLineEnd(fragments, index, piece, currentLineWidth)) break;
                    }
                    if (lineEnd == -1) break;
                    if (start == 0) this.splitFragments.add(f);
                    f.setPieceText(piece, start, lineEnd, this.lineEndWidth);
                    restWidth -= piece.unscaledTextWidth;
                    start = lineEnd;
                    // The line measures its fragments when it is complete, and the piece is not the last one of the fragment, so the next one has to exist by then
                    MarkdownTextFragment nextPiece = f.addPiece();
                    nextPiece.startOfRenderLine = true;
                    line.fragments.add(piece);
                    line.prepareLine();
                    lines.add(line);
                    line = new MarkdownTextLine(renderer);
                    currentLineWidth = padding;
                    piece = nextPiece;
                    pieceHoldsRest = false;
                }
                f = piece;
            }

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
     * Returns the width the word that starts with the given fragment needs as the last word of a render line.<br>
     * A word is made of several fragments when its format changes inside of it, like the bold word and the comma in "**word**,",
     * and a line can only wrap in front of the first one of them.<br>
     * A word of a single fragment needs room for the space behind it as well, although a line drops the space at its end:
     * that is where lines of plain words wrap, and texts are written around it. A word of several fragments only needs the room it really takes.<br><br>
     *
     * In text of a script without spaces, a word is what stands between two places a line can break at, which {@link MarkdownLineBreaks} knows.
     * It ends in front of a fragment with such a place in front of it, and inside of a fragment with such a place inside of it.
     *
     * @param index The index of the given fragment in the fragments, or of the fragment it is a piece of.
     * @param wholeFirst If the word holds all of the given fragment, no matter if a line can break inside of it. That is the question if the rest of a fragment
     *                   that can get split fits into its line, which only takes the room the text really needs, like a word of several fragments.
     * @param lineWidth The width of the line in front of the word.
     */
    private float getWordWidthAtLineEnd(@NotNull List<MarkdownTextFragment> fragments, int index, @NotNull MarkdownTextFragment fragment, boolean wholeFirst, float lineWidth) {
        int first = index;
        float width = 0.0F;
        while (true) {
            if (((index > first) || !wholeFirst) && canBreakInside(fragment)) {
                // The word ends at the first place of the fragment a line can break at. It fits if all of the fragment does, which saves measuring it.
                float widthWithFragment = width + fragment.getRenderWidth(true);
                if ((lineWidth + widthWithFragment + this.renderer.scaling.padding()) <= this.renderer.optimalWidth) return widthWithFragment;
                int firstBreak = MarkdownLineBreaks.next(fragment.text, 1, fragment.text.length());
                return width + fragment.getLeadingSpace() + (fragment.measurePiece(fragment.buildRenderStyle(true), 0, firstBreak) * fragment.getScale()) + fragment.getTrailingSpace(true, false);
            }
            boolean endsWord = fragment.endOfWord || fragment.naturalLineBreakAfter || (index == (fragments.size() - 1)) || fragments.get(index + 1).isTable() || MarkdownLineBreaks.isAllowedBetween(fragment.text, fragments.get(index + 1).text);
            width += fragment.getRenderWidth(endsWord);
            if (endsWord) {
                if (((index > first) || wholeFirst) && fragment.text.endsWith(" ")) width -= this.spaceWidth * fragment.getScale();
                return width;
            }
            index++;
            fragment = fragments.get(index);
        }
    }

    /**
     * Returns if a line can break inside of the fragment. Lines of a code block never break.
     */
    private static boolean canBreakInside(@NotNull MarkdownTextFragment fragment) {
        return fragment.breaksInside && ((fragment.codeBlockContext == null) || fragment.codeBlockContext.singleLine);
    }

    /**
     * Returns if the fragment fits into its line as a whole, together with everything behind it that no line can break in front of.
     *
     * @param index The index of the fragment in the fragments, or of the fragment it is a piece of.
     * @param lineWidth The width of the line in front of the fragment.
     */
    private boolean fitsAtLineEnd(@NotNull List<MarkdownTextFragment> fragments, int index, @NotNull MarkdownTextFragment fragment, float lineWidth) {
        return (lineWidth + this.getWordWidthAtLineEnd(fragments, index, fragment, true, lineWidth) + this.renderer.scaling.padding()) <= this.renderer.optimalWidth;
    }

    /**
     * Finds the place inside of the text of a fragment the current line ends at: the last place a line can break at that has all text in front of it inside the line.
     * The line takes the text up to the first such place if not even that fits, because nothing less can go into a line. {@link #lineEndWidth} is the width of what the line takes.<br><br>
     *
     * The text can be much longer than a line, so the search never measures or walks all of it. It guesses where the line ends from the widths it knows,
     * measures the text up to the nearest place to break at, and narrows the range down with every measured width. Text of characters of the same width
     * takes two rounds: one for the text that fits, and one for the next place, which does not.
     *
     * @param fragment The fragment that got or gets split.
     * @param style The style for measuring text of the fragment.
     * @param piece The piece of the fragment that gets the text the line takes, which knows the room it needs around its text.
     * @param start The index in the whole text of the fragment the text of the piece starts at.
     * @param lineWidth The width of the line in front of the piece.
     * @param restWidth The unscaled width of the text behind the start, which does not have to be exact.
     * @return The index in the whole text of the fragment the next line starts at, or -1 if no line can break behind the start.
     */
    private int findLineEnd(@NotNull MarkdownTextFragment fragment, @NotNull Style style, @NotNull MarkdownTextFragment piece, int start, float lineWidth, float restWidth) {
        String text = fragment.getWholeText();
        float scale = piece.getScale();
        float padding = this.renderer.scaling.padding();
        float roomAroundText = piece.getLeadingSpace() + piece.getTrailingSpace(true, false);
        // The unscaled width text can have to fit, which is only good enough for guessing
        float fittingWidth = (scale > 0.0F) ? ((this.renderer.optimalWidth - padding - lineWidth - roomAroundText) / scale) : 0.0F;
        // The text in front of this index fits
        int fitting = start;
        float fittingTextWidth = 0.0F;
        // The text in front of this index does not fit, unless it is the end of the text, which is not measured
        int end = text.length();
        float endWidth = 0.0F;
        boolean endMeasured = false;
        for (int round = 0; (end - fitting) > 1; round++) {
            int range = end - fitting;
            int guess;
            if (endMeasured && ((round % 4) == 3)) {
                // Guessing gets close fast in text a line can break in everywhere, but only halving the range is sure to
                guess = fitting + (range / 2);
            } else {
                // The width of a character behind the fitting text is about what the characters between the two measured indexes have,
                // or the ones of the fitting text, or the ones of the whole rest
                float characterWidth = endMeasured ? ((endWidth - fittingTextWidth) / range) : (fitting > start) ? (fittingTextWidth / (fitting - start)) : (restWidth / range);
                int fittingCharacters = (characterWidth > 0.0F) ? (int) Math.min((fittingWidth - fittingTextWidth) / characterWidth, range - 1) : (range - 1);
                guess = fitting + Math.max(1, fittingCharacters);
            }
            int candidate = MarkdownLineBreaks.previous(text, guess, fitting);
            if (candidate == -1) candidate = MarkdownLineBreaks.next(text, guess + 1, end);
            if (candidate == -1) break;
            float candidateWidth = fragment.measurePiece(style, start, candidate);
            if ((lineWidth + (roomAroundText + (candidateWidth * scale)) + padding) <= this.renderer.optimalWidth) {
                fitting = candidate;
                fittingTextWidth = candidateWidth;
            } else {
                end = candidate;
                endWidth = candidateWidth;
                endMeasured = true;
            }
        }
        if (fitting > start) {
            this.lineEndWidth = fittingTextWidth;
            return fitting;
        }
        // No place to break at fits. The measured end is the first one behind the start then, because the search found no other one in front of it.
        this.lineEndWidth = endWidth;
        return endMeasured ? end : -1;
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
     * Makes the pieces of split fragments forget that the mouse was over them. They are no fragments of the renderer, only of its lines.
     */
    void resetHoveredPieces() {
        for (int i = 0; i < this.splitFragments.size(); i++) this.splitFragments.get(i).resetHoveredPieces();
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
