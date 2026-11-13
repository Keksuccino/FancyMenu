package de.keksuccino.fancymenu.util.rendering.text.markdown;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * The text of a fragment, set into cells of the same width, so the characters of several lines stand in columns like in a fixed-width font.<br><br>
 *
 * Fonts draw text as a whole and decide where every character goes, so every character that needs to move would need a draw call of its own.
 * To keep those few, the text gets cut into runs: a run is a row of characters the font puts into their cells by itself, because each of them
 * is exactly as wide as a cell. That is the case for most letters and digits of the Minecraft font. Narrower characters get centered in their cell
 * and start a run of their own, and spaces get no run at all.
 */
final class MarkdownFixedWidthText {

    private final Component[] runs;
    /** Where every run starts, in pixels of text at its normal size. */
    private final float[] offsets;

    private MarkdownFixedWidthText(@NotNull Component[] runs, @NotNull float[] offsets) {
        this.runs = runs;
        this.offsets = offsets;
    }

    /**
     * Returns the width of a cell, which is the one the style sets, or the width of the letter "m" in the font of the renderer if the style sets none.
     */
    static float getCellWidth(@NotNull MarkdownRenderer renderer) {
        float cellWidth = renderer.style.codeBlockCellWidth.get();
        return (cellWidth > 0.0F) ? cellWidth : renderer.textFont.metrics().cellWidth;
    }

    /**
     * Returns the width of the given text set into cells.
     */
    static float getWidth(@NotNull MarkdownRenderer renderer, @NotNull String text) {
        return text.codePointCount(0, text.length()) * getCellWidth(renderer);
    }

    @NotNull
    static MarkdownFixedWidthText of(@NotNull MarkdownRenderer renderer, @NotNull String text, @NotNull Style style) {
        float cellWidth = getCellWidth(renderer);
        boolean pixelFont = renderer.textFont.metrics().pixelFont;
        List<Component> runs = new ArrayList<>();
        List<Float> offsets = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        float runOffset = 0.0F;
        // Where the font puts the next character of the current run
        float pen = 0.0F;
        int cell = 0;
        for (int index = 0; index < text.length(); cell++) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)) {
                flush(runs, offsets, run, runOffset, style);
                continue;
            }
            String character = new String(Character.toChars(codePoint));
            float width = renderer.textFont.width(character);
            float inset = Math.max(0.0F, (cellWidth - width) / 2.0F);
            // The Minecraft font only stays sharp on whole pixels
            if (pixelFont) inset = (float) Math.floor(inset);
            float position = (cell * cellWidth) + inset;
            if ((run.length() > 0) && (Math.abs(pen - position) > 0.001F)) {
                flush(runs, offsets, run, runOffset, style);
            }
            if (run.length() == 0) {
                runOffset = position;
                pen = position;
            }
            run.append(character);
            pen += width;
        }
        flush(runs, offsets, run, runOffset, style);
        float[] offsetArray = new float[offsets.size()];
        for (int i = 0; i < offsetArray.length; i++) offsetArray[i] = offsets.get(i);
        return new MarkdownFixedWidthText(runs.toArray(new Component[0]), offsetArray);
    }

    private static void flush(@NotNull List<Component> runs, @NotNull List<Float> offsets, @NotNull StringBuilder run, float runOffset, @NotNull Style style) {
        if (run.length() == 0) return;
        runs.add(Component.literal(run.toString()).setStyle(style));
        offsets.add(runOffset);
        run.setLength(0);
    }

    int getRunCount() {
        return this.runs.length;
    }

    @NotNull
    Component getRun(int index) {
        return this.runs[index];
    }

    float getRunOffset(int index) {
        return this.offsets[index];
    }

    /**
     * Draws the text. The render pose has to be at the origin of the text and scaled to its size.
     */
    void render(@NotNull GuiGraphicsExtractor graphics, @NotNull MarkdownRenderer renderer, int color) {
        for (int i = 0; i < this.runs.length; i++) {
            renderer.textFont.draw(graphics, this.runs[i], this.offsets[i], 0.0F, color, false);
        }
    }

}
