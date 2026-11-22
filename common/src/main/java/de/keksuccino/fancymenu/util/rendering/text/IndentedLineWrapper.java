package de.keksuccino.fancymenu.util.rendering.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;

/**
 * Line-wraps indented lines and lines of bullet lists, like {@code "    • Some text"}, so their wrapped lines start below the text of their
 * first line instead of at the left edge (a hanging indent). That keeps nested lists readable when their lines get wrapped.
 */
public final class IndentedLineWrapper {

    /** Characters that start a list item when they come after the indentation of a line and a space follows them. */
    private static final String BULLETS = "•◦▪▫‣·-*";

    private IndentedLineWrapper() {
    }

    /**
     * Line-wraps the line. Wrapped lines of indented lines and list items get indented by the width of the indentation and bullet of the line.
     * Lines whose indentation takes more than half of the width wrap like lines without indentation, so deep nesting never leaves too little room for their text.
     *
     * @param measurer Measures the width of text, in the units of the max width.
     * @param wrapper Line-wraps text at a width, in the units of the max width.
     */
    @NotNull
    public static List<WrappedLine> wrap(@NotNull Component line, float maxWidth, @NotNull ToDoubleFunction<Component> measurer, @NotNull BiFunction<Component, Float, List<MutableComponent>> wrapper) {
        int indentLength = getHangingIndentLength(line.getString());
        if (indentLength > 0) {
            MutableComponent[] parts = split(line, indentLength);
            float indent = (float) measurer.applyAsDouble(parts[0]);
            if (indent <= (maxWidth / 2.0F)) {
                List<MutableComponent> wrapped = wrapper.apply(parts[1], maxWidth - indent);
                List<WrappedLine> lines = new ArrayList<>(wrapped.size());
                // The first line gets built as a new component, because components cache their text for rendering and measuring,
                // and appending to the indentation after measuring it would leave its cache without the appended text
                lines.add(new WrappedLine(Component.empty().append(parts[0]).append(wrapped.get(0)), 0.0F));
                for (int i = 1; i < wrapped.size(); i++) {
                    lines.add(new WrappedLine(wrapped.get(i), indent));
                }
                return lines;
            }
        }
        List<MutableComponent> wrapped = wrapper.apply(line, maxWidth);
        List<WrappedLine> lines = new ArrayList<>(wrapped.size());
        for (MutableComponent text : wrapped) {
            lines.add(new WrappedLine(text, 0.0F));
        }
        return lines;
    }

    /**
     * How many characters at the start of the text the wrapped lines of the text get indented by: the spaces it starts with, and a bullet that
     * follows them together with the spaces after the bullet.
     *
     * @return The number of characters, or 0 if the text doesn't start with spaces or a bullet, or has no text after them.
     */
    static int getHangingIndentLength(@NotNull String text) {
        int length = skipSpaces(text, 0);
        if (((length + 1) < text.length()) && (BULLETS.indexOf(text.charAt(length)) >= 0) && isSpace(text.charAt(length + 1))) {
            length = skipSpaces(text, length + 1);
        }
        return (length < text.length()) ? length : 0;
    }

    private static int skipSpaces(@NotNull String text, int start) {
        int index = start;
        while ((index < text.length()) && isSpace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    /**
     * Spaces and tabs, but no line breaks, because text after a line break starts a new line anyway.
     */
    private static boolean isSpace(char c) {
        return (c == ' ') || (c == '\t') || (c == ' ');
    }

    /**
     * Splits the text of the component into the part before the index and the part from the index on, keeping the style of every character.
     */
    @NotNull
    static MutableComponent[] split(@NotNull Component component, int index) {
        MutableComponent start = Component.empty();
        MutableComponent end = Component.empty();
        int[] position = new int[] { 0 };
        component.visit((style, text) -> {
            int from = position[0];
            int to = from + text.length();
            if (to <= index) {
                start.append(Component.literal(text).setStyle(style));
            } else if (from >= index) {
                end.append(Component.literal(text).setStyle(style));
            } else {
                start.append(Component.literal(text.substring(0, index - from)).setStyle(style));
                end.append(Component.literal(text.substring(index - from)).setStyle(style));
            }
            position[0] = to;
            return Optional.empty();
        }, Style.EMPTY);
        return new MutableComponent[] { start, end };
    }

    /**
     * A wrapped line and how far it has to be moved to the right, in the units of the max width of the wrapping.
     */
    public record WrappedLine(@NotNull MutableComponent text, float indent) {
    }

}
