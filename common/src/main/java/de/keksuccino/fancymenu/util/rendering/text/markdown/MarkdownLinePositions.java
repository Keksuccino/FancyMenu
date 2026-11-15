package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * Tells where the lines of the text of a {@link MarkdownRenderer} are, for whatever scrolls the text to one of them.<br>
 * A position is how far the text has to move up for a line to be the first thing that shows of it: the distance from the top of the text
 * to the top of the line, without the room that stays above the line. That room is the padding of the text, like the first line has it above itself,
 * or the space between the line and the one in front of it if that is less, so nothing of the line in front of it shows.<br><br>
 *
 * Nothing in here runs per frame. Everything walks the lines of the text, so it is for when something scrolls, not for while something renders.
 */
public final class MarkdownLinePositions {

    private final MarkdownRenderer renderer;
    /** Where the render lines of the line of the text that gets searched start in its text. Kept between searches, so searching makes no garbage. */
    private int[] renderLineStarts = new int[8];

    MarkdownLinePositions(@NotNull MarkdownRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Hands the position of every line the text shows to the consumer, from the first one to the last one.
     * A line of the text that wraps is as many lines as it shows as. Blank lines that take no space are none.
     *
     * @param headlinesOnly If only the lines of headlines count.
     */
    public void forEach(boolean headlinesOnly, @NotNull DoubleConsumer consumer) {
        this.renderer.refresh(false);
        List<MarkdownTextLine> lines = this.renderer.lines;
        for (int i = 0; i < lines.size(); i++) {
            MarkdownTextLine line = lines.get(i);
            if (line.collapsed || line.fragments.isEmpty()) continue;
            if (headlinesOnly && (line.fragments.get(0).headlineType == MarkdownTextFragment.HeadlineType.NONE)) continue;
            consumer.accept(this.positionOf(line));
        }
    }

    /**
     * Returns the position of a line of the text by its number, the way an editor numbers the lines of the text: every line break starts a new one,
     * no matter how often a line wraps when it shows.<br>
     * A line that shows nothing itself, like the one that opens a code block, is where the next line is. A line of a table is where the table is.
     * A number behind the last line is the last line.
     *
     * @param lineNumber The number of the line, starting at 1.
     * @return The position, or NaN if the text has no lines.
     */
    public double find(int lineNumber) {
        this.renderer.refresh(false);
        List<MarkdownTextLine> lines = this.renderer.lines;
        int sourceLine = Math.max(0, lineNumber - 1);
        MarkdownTextLine last = null;
        for (int i = 0; i < lines.size(); i++) {
            MarkdownTextLine line = lines.get(i);
            if (line.fragments.isEmpty()) continue;
            if (line.fragments.get(0).lastSourceLine >= sourceLine) return this.positionOf(line);
            last = line;
        }
        return (last != null) ? this.positionOf(last) : Double.NaN;
    }

    /**
     * Returns the position of the first line that holds the given text, the way the line shows it: without what formats it, like the asterisks around bold text.
     * Upper and lower case make no difference.<br>
     * The text gets searched in the lines of the text as they got typed, so it gets found where a line wraps in the middle of it.
     * The position is the one of the line the found text starts in. Text in a table is where the table is.
     *
     * @return The position, or NaN if no line holds the text.
     */
    public double find(@NotNull String text) {
        if (text.isEmpty()) return Double.NaN;
        this.renderer.refresh(false);
        List<MarkdownTextLine> lines = this.renderer.lines;
        StringBuilder lineText = new StringBuilder();
        int firstRenderLine = 0;
        for (int i = 0; i < lines.size(); i++) {
            MarkdownTextLine line = lines.get(i);
            int renderLine = i - firstRenderLine;
            if (renderLine == this.renderLineStarts.length) this.renderLineStarts = Arrays.copyOf(this.renderLineStarts, renderLine * 2);
            this.renderLineStarts[renderLine] = lineText.length();
            boolean endsLineOfText = line.fragments.isEmpty();
            for (int f = 0; f < line.fragments.size(); f++) {
                MarkdownTextFragment fragment = line.fragments.get(f);
                if (fragment.isTable()) {
                    appendTableText(lineText, fragment.tableContext);
                } else {
                    lineText.append(fragment.text);
                }
                if (fragment.naturalLineBreakAfter) endsLineOfText = true;
            }
            if (!endsLineOfText && (i < (lines.size() - 1))) continue;
            int found = StringUtils.indexOfIgnoreCase(lineText, text);
            if (found != -1) {
                int foundRenderLine = renderLine;
                while ((foundRenderLine > 0) && (this.renderLineStarts[foundRenderLine] > found)) foundRenderLine--;
                return this.positionOf(lines.get(firstRenderLine + foundRenderLine));
            }
            lineText.setLength(0);
            firstRenderLine = i + 1;
        }
        return Double.NaN;
    }

    /**
     * Adds the text of every cell of a table. The cells are lines of their own, so nothing gets found across two of them.
     */
    private static void appendTableText(@NotNull StringBuilder text, @NotNull MarkdownTextFragment.TableContext table) {
        for (MarkdownTextFragment.TableRow row : table.rows) {
            for (MarkdownTextFragment.TableCell cell : row.cells) {
                for (MarkdownTextFragment fragment : cell.fragments) {
                    text.append(fragment.text);
                }
                text.append('\n');
            }
        }
    }

    private double positionOf(@NotNull MarkdownTextLine line) {
        MarkdownTextLine previous = line.getPreviousLine();
        // Lines can overlap, which is no space between them
        float spaceAbove = Math.max(0.0F, line.offsetY - ((previous != null) ? (previous.offsetY + previous.lineHeight) : 0.0F));
        return line.offsetY - Math.min(this.renderer.scaling.padding(), spaceAbove);
    }

}
