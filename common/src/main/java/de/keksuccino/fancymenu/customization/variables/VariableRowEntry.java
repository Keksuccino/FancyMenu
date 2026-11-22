package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A row of the variable windows: a leading part (like a type icon or an index) and one line of text segments in different colors,
 * cut off with {@link VariableTextUtils#ELLIPSIS} where the row ends.
 *
 * <p>Rows fill the width of their scroll area, so the scroll area should use {@link ScrollArea#makeEntriesWidthOfArea} and not make
 * entries as wide as the widest one. The cut-off line gets cached for the width it got cut for, so rows only measure their text again
 * when the width or the text changes.</p>
 */
public abstract class VariableRowEntry extends ScrollAreaEntry {

    protected static final float PADDING = 5.0F;
    protected static final float LEADING_GAP = 5.0F;
    /** The text of segments that are too long for any row, so previews of huge values never measure all of them. */
    protected static final int MAX_SEGMENT_CHARS = 300;
    /** What line breaks look like in single-line text. */
    protected static final String LINE_BREAK_MARKER = " ⏎ ";

    @Nullable
    private List<Segment> segments;
    @Nullable
    private List<FittedSegment> fittedSegments;
    private float fittedWidth = -1.0F;

    protected VariableRowEntry(@NotNull ScrollArea parent, float height) {
        super(parent, 0, height);
        this.playClickSound = false;
    }

    /**
     * @return The width of the leading part, or 0 if the row has none.
     */
    protected abstract float getLeadingWidth();

    protected abstract void renderLeading(@NotNull GuiGraphicsExtractor graphics, float x, float centerY);

    /**
     * @return The width of the space at the end of the row, which the text never covers, or 0 if the row needs none.
     */
    protected float getTrailingWidth() {
        return 0.0F;
    }

    /**
     * Renders something into the space at the end of the row, see {@link #getTrailingWidth()}.
     *
     * @param x The left edge of the space.
     */
    protected void renderTrailing(@NotNull GuiGraphicsExtractor graphics, float x, float centerY, int mouseX, int mouseY) {
    }

    /**
     * @return If the row renders its text. Rows that get edited in place hide it behind their edit box.
     */
    protected boolean shouldRenderText() {
        return true;
    }

    /**
     * Builds the text segments of the row. They get built once and cached until {@link #invalidateText()} gets called.
     */
    @NotNull
    protected abstract List<Segment> buildSegments();

    /**
     * Makes the row build its text segments again the next time it renders.
     */
    protected void invalidateText() {
        this.segments = null;
        this.fittedSegments = null;
    }

    @Override
    public void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        float centerY = this.getY() + (this.getHeight() / 2.0F);
        float x = this.getX() + PADDING;
        float leadingWidth = this.getLeadingWidth();
        if (leadingWidth > 0.0F) {
            this.renderLeading(graphics, x, centerY);
            x += leadingWidth + LEADING_GAP;
        }
        float textEndX = this.getTextEndX();
        float trailingWidth = this.getTrailingWidth();
        if (trailingWidth > 0.0F) {
            this.renderTrailing(graphics, textEndX + LEADING_GAP, centerY, mouseX, mouseY);
        }
        if (!this.shouldRenderText()) return;
        float textY = centerY - (UIBase.getUITextHeightNormal() / 2.0F);
        for (FittedSegment segment : this.getFittedSegments(textEndX - x)) {
            UIBase.renderText(graphics, segment.text(), x, textY, segment.style().getColor());
            x += segment.width();
        }
    }

    /**
     * @return The x position where the text of the row starts.
     */
    public float getTextX() {
        float leadingWidth = this.getLeadingWidth();
        return this.getX() + PADDING + ((leadingWidth > 0.0F) ? (leadingWidth + LEADING_GAP) : 0.0F);
    }

    /**
     * @return The x position where the text of the row has to end.
     */
    public float getTextEndX() {
        float trailingWidth = this.getTrailingWidth();
        return (this.getX() + this.getWidth()) - PADDING - ((trailingWidth > 0.0F) ? (trailingWidth + LEADING_GAP) : 0.0F);
    }

    @NotNull
    private List<FittedSegment> getFittedSegments(float availableWidth) {
        List<FittedSegment> fitted = this.fittedSegments;
        if ((fitted != null) && (this.fittedWidth == availableWidth)) return fitted;
        if (this.segments == null) this.segments = this.buildSegments();
        fitted = fitSegments(this.segments, availableWidth);
        this.fittedSegments = fitted;
        this.fittedWidth = availableWidth;
        return fitted;
    }

    /**
     * Fits the segments into the width: segments that fit stay as they are, the first one that doesn't gets cut off with
     * {@link VariableTextUtils#ELLIPSIS}, and everything after it gets left out.
     */
    @NotNull
    private static List<FittedSegment> fitSegments(@NotNull List<Segment> segments, float availableWidth) {
        List<FittedSegment> fitted = new ArrayList<>(segments.size());
        float remaining = availableWidth;
        for (Segment segment : segments) {
            float width = UIBase.getUITextWidth(segment.text());
            if (width <= remaining) {
                fitted.add(new FittedSegment(segment.text(), segment.style(), width));
                remaining -= width;
                continue;
            }
            String cut = cutToWidth(segment.text(), remaining);
            if (cut != null) fitted.add(new FittedSegment(cut, segment.style(), UIBase.getUITextWidth(cut)));
            break;
        }
        return fitted;
    }

    /**
     * @return The longest start of the text that fits into the width together with an ellipsis, or {@code null} if not even the ellipsis fits.
     */
    @Nullable
    private static String cutToWidth(@NotNull String text, float width) {
        float ellipsisWidth = UIBase.getUITextWidth(VariableTextUtils.ELLIPSIS);
        if (ellipsisWidth > width) return null;
        // Binary search for the longest start that fits, measuring only about log2(length) candidates
        int low = 0;
        int high = text.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if ((UIBase.getUITextWidth(text.substring(0, middle)) + ellipsisWidth) <= width) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        // Never keep half of a surrogate pair
        if ((low > 0) && Character.isHighSurrogate(text.charAt(low - 1))) low--;
        return text.substring(0, low).stripTrailing() + VariableTextUtils.ELLIPSIS;
    }

    /**
     * Renders an icon of the size of the text, centered on the given y.
     */
    protected static void renderIcon(@NotNull GuiGraphicsExtractor graphics, @NotNull MaterialIcon icon, float x, float centerY, int color) {
        float size = getIconSize();
        icon.render(graphics, x, centerY - (size / 2.0F), size, size, color);
    }

    /**
     * Icons get a bit taller than the text, because Material icons leave some room around their symbol.
     */
    protected static float getIconSize() {
        return UIBase.getUITextHeightNormal() + 2.0F;
    }

    /**
     * @return The color of icons that only mark something, like the type of a variable, which shouldn't stand out more than the text next to them.
     */
    protected static int getSubtleIconColor() {
        return UIBase.getUITheme().getUIIconColor(0.6F);
    }

    /**
     * @return The width of the column that shows the indexes of list entries up to the given index.
     */
    protected static float getIndexColumnWidth(int maxIndex) {
        // Digits of the UI font all have the same width, so zeros measure every index with the same number of digits
        return Math.max(getIconSize(), UIBase.getUITextWidth("0".repeat(String.valueOf(Math.max(0, maxIndex)).length())));
    }

    /**
     * Renders the index of a list entry right-aligned in its column, see {@link #getIndexColumnWidth(int)}.
     */
    protected static void renderIndex(@NotNull GuiGraphicsExtractor graphics, @NotNull String index, float x, float columnWidth, float centerY) {
        float textY = centerY - (UIBase.getUITextHeightNormal() / 2.0F);
        UIBase.renderText(graphics, index, x + columnWidth - UIBase.getUITextWidth(index), textY, TextStyle.MUTED.getColor());
    }

    /**
     * Adds the text as one line (see {@link VariableTextUtils#toSingleLine(String, String, int)}), with dimmed line break markers.
     */
    protected static void addSingleLineSegments(@NotNull List<Segment> segments, @NotNull String text, @NotNull TextStyle style) {
        if (!VariableTextUtils.containsLineBreak(text)) {
            segments.add(new Segment(VariableTextUtils.toSingleLine(text, LINE_BREAK_MARKER, MAX_SEGMENT_CHARS), style));
            return;
        }
        String singleLine = VariableTextUtils.toSingleLine(text, "\n", MAX_SEGMENT_CHARS);
        int start = 0;
        int lineBreak;
        while ((lineBreak = singleLine.indexOf('\n', start)) >= 0) {
            if (lineBreak > start) segments.add(new Segment(singleLine.substring(start, lineBreak), style));
            segments.add(new Segment(LINE_BREAK_MARKER, TextStyle.MUTED));
            start = lineBreak + 1;
        }
        if (start < singleLine.length()) segments.add(new Segment(singleLine.substring(start), style));
    }

    /**
     * The colors of the text in rows.
     */
    public enum TextStyle {

        /** Names and other main text. */
        LABEL,
        /** Hints and other secondary text. */
        MUTED,
        /** Values of variables. */
        VALUE;

        public int getColor() {
            boolean blur = UIBase.shouldBlur();
            return switch (this) {
                case LABEL -> blur ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt() : UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();
                case MUTED -> blur ? UIBase.getUITheme().ui_blur_interface_widget_label_color_inactive.getColorInt() : UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt();
                case VALUE -> UIBase.getUITheme().warning_color.getColorInt();
            };
        }

    }

    public record Segment(@NotNull String text, @NotNull TextStyle style) {

        public Segment {
            Objects.requireNonNull(text);
            Objects.requireNonNull(style);
        }

    }

    private record FittedSegment(@NotNull String text, @NotNull TextStyle style, float width) {
    }

}
