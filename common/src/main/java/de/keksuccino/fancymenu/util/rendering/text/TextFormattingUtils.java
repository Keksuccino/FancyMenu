package de.keksuccino.fancymenu.util.rendering.text;

import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothFont;
import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothTextRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TextFormattingUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String FORMATTING_CODE_BLACK = "0";
    private static final String FORMATTING_CODE_DARK_BLUE = "1";
    private static final String FORMATTING_CODE_DARK_GREEN = "2";
    private static final String FORMATTING_CODE_DARK_AQUA = "3";
    private static final String FORMATTING_CODE_DARK_RED = "4";
    private static final String FORMATTING_CODE_DARK_PURPLE = "5";
    private static final String FORMATTING_CODE_ORANGE = "6";
    private static final String FORMATTING_CODE_GREY = "7";
    private static final String FORMATTING_CODE_DARK_GREY = "8";
    private static final String FORMATTING_CODE_BLUE = "9";
    private static final String FORMATTING_CODE_GREEN = "a";
    private static final String FORMATTING_CODE_AQUA = "b";
    private static final String FORMATTING_CODE_RED = "c";
    private static final String FORMATTING_CODE_PURPLE = "d";
    private static final String FORMATTING_CODE_YELLOW = "e";
    private static final String FORMATTING_CODE_WHITE = "f";
    private static final String FORMATTING_CODE_BOLD = "l";
    private static final String FORMATTING_CODE_STRIKE = "m";
    private static final String FORMATTING_CODE_UNDERLINE = "n";
    private static final String FORMATTING_CODE_ITALIC = "o";
    private static final String FORMATTING_CODE_MAGIC = "k";
    private static final String FORMATTING_CODE_RESET = "r";

    @NotNull
    public static String replaceFormattingCodes(@NotNull String in, @NotNull String oldPrefix, @NotNull String newPrefix) {
        Objects.requireNonNull(in);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_BLACK, newPrefix + FORMATTING_CODE_BLACK);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_DARK_BLUE, newPrefix + FORMATTING_CODE_DARK_BLUE);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_DARK_GREEN, newPrefix + FORMATTING_CODE_DARK_GREEN);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_DARK_AQUA, newPrefix + FORMATTING_CODE_DARK_AQUA);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_DARK_RED, newPrefix + FORMATTING_CODE_DARK_RED);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_DARK_PURPLE, newPrefix + FORMATTING_CODE_DARK_PURPLE);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_ORANGE, newPrefix + FORMATTING_CODE_ORANGE);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_GREY, newPrefix + FORMATTING_CODE_GREY);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_DARK_GREY, newPrefix + FORMATTING_CODE_DARK_GREY);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_BLUE, newPrefix + FORMATTING_CODE_BLUE);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_GREEN, newPrefix + FORMATTING_CODE_GREEN);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_AQUA, newPrefix + FORMATTING_CODE_AQUA);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_RED, newPrefix + FORMATTING_CODE_RED);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_PURPLE, newPrefix + FORMATTING_CODE_PURPLE);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_YELLOW, newPrefix + FORMATTING_CODE_YELLOW);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_WHITE, newPrefix + FORMATTING_CODE_WHITE);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_BOLD, newPrefix + FORMATTING_CODE_BOLD);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_STRIKE, newPrefix + FORMATTING_CODE_STRIKE);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_UNDERLINE, newPrefix + FORMATTING_CODE_UNDERLINE);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_ITALIC, newPrefix + FORMATTING_CODE_ITALIC);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_MAGIC, newPrefix + FORMATTING_CODE_MAGIC);
        in = StringUtils.replace(in, oldPrefix + FORMATTING_CODE_RESET, newPrefix + FORMATTING_CODE_RESET);
        return in;
    }

    /**
     * Converts a {@link Component} to a string while trying to preserve as much formatting as possible.
     */
    public static String convertComponentToString(@NotNull Component textComponent) {
        StringBuilder sb = new StringBuilder();
        appendComponent(sb, textComponent, textComponent.getStyle());
        return sb.toString();
    }

    private static void appendComponent(@NotNull StringBuilder sb, @NotNull Component component, @NotNull Style parentStyle) {

        Style style = component.getStyle().applyTo(parentStyle);
        if (style.isBold()) {
            sb.append(ChatFormatting.BOLD);
        }
        if (style.isItalic()) {
            sb.append(ChatFormatting.ITALIC);
        }
        if (style.isUnderlined()) {
            sb.append(ChatFormatting.UNDERLINE);
        }
        if (style.isStrikethrough()) {
            sb.append(ChatFormatting.STRIKETHROUGH);
        }
        if (style.isObfuscated()) {
            sb.append(ChatFormatting.OBFUSCATED);
        }
        if (style.getColor() != null) {
            sb.append(style.getColor().toString());
        }

        sb.append(component.getContents());

        sb.append(ChatFormatting.RESET);

        for (Component sibling : component.getSiblings()) {
            appendComponent(sb, sibling, style);
        }

    }

    @NotNull
    public static MutableComponent convertFormattedTextToComponent(@NotNull FormattedText text) {
        MutableComponent component = Component.literal("");
        text.visit((style, string) -> {
            component.append(Component.literal(string).withStyle(style));
            return Optional.empty(); // Continue visiting
        }, Style.EMPTY);
        return component;
    }

    @NotNull
    public static <C extends Component> List<MutableComponent> lineWrapComponents(@NotNull List<C> lines, int maxLength) {
        List<MutableComponent> wrappedLines = new ArrayList<>();
        for (Component line : lines) {
            if (line.getString().isBlank()) line = Component.literal(" ");
            for (FormattedText text : Minecraft.getInstance().font.getSplitter().splitLines(line, maxLength, Style.EMPTY)) {
                wrappedLines.add(convertFormattedTextToComponent(text));
            }
        }
        return wrappedLines;
    }

    @NotNull
    public static <C extends Component> List<MutableComponent> lineWrapComponents(@NotNull C lines, int maxLength) {
        return lineWrapComponents(List.of(lines), maxLength);
    }

    /**
     * Line-wraps components with the measurements of a smooth font. Lines break at whitespace when possible, otherwise
     * between characters, and never inside a character cluster (like an emoji sequence).
     */
    @NotNull
    public static <C extends Component> List<MutableComponent> lineWrapComponentsSmooth(@NotNull List<C> lines, @NotNull SmoothFont font, float textSize, float maxWidth) {
        Objects.requireNonNull(font);
        List<MutableComponent> wrappedLines = new ArrayList<>();
        for (Component line : lines) {
            if (line.getString().isBlank()) line = Component.literal(" ");
            List<StyledCodepoint> codepoints = collectStyledCodepoints(line);
            wrappedLines.addAll(wrapSmoothCodepoints(codepoints, font, textSize, maxWidth));
        }
        return wrappedLines;
    }

    /**
     * Line-wraps a component with the measurements of a smooth font.
     */
    @NotNull
    public static <C extends Component> List<MutableComponent> lineWrapComponentsSmooth(@NotNull C lines, @NotNull SmoothFont font, float textSize, float maxWidth) {
        return lineWrapComponentsSmooth(List.of(lines), font, textSize, maxWidth);
    }

    @NotNull
    private static List<StyledCodepoint> collectStyledCodepoints(@NotNull Component component) {
        List<StyledCodepoint> codepoints = new ArrayList<>();
        FormattedCharSequence sequence = component.getVisualOrderText();
        sequence.accept((index, style, codepoint) -> {
            codepoints.add(new StyledCodepoint(codepoint, style != null ? style : Style.EMPTY));
            return true;
        });
        return codepoints;
    }

    @NotNull
    private static List<MutableComponent> wrapSmoothCodepoints(@NotNull List<StyledCodepoint> codepoints, @NotNull SmoothFont font, float textSize, float maxWidth) {
        List<MutableComponent> result = new ArrayList<>();
        if (codepoints.isEmpty()) {
            result.add(Component.literal(" "));
            return result;
        }
        // One layout pass gives the caret position before every codepoint, so the width of any line is a caret difference.
        float[] carets = SmoothTextRenderer.getCaretPositions(font, toSequence(codepoints), textSize);
        int size = codepoints.size();
        int lineStart = 0;
        int clusterStart = 0;
        int lastBreak = -1;
        for (int i = 0; i < size; i++) {
            int codepoint = codepoints.get(i).codepoint;
            if (codepoint == '\n') {
                result.add(buildComponentFromCodepoints(codepoints.subList(lineStart, i)));
                lineStart = i + 1;
                clusterStart = i + 1;
                lastBreak = -1;
                continue;
            }
            if (isBreakable(codepoint)) {
                lastBreak = i;
            }
            if (Float.isNaN(carets[i + 1])) {
                // Inside a cluster: lines can't end here.
                continue;
            }
            if (maxWidth > 0.0F && clusterStart > lineStart && (carets[i + 1] - carets[lineStart]) > maxWidth) {
                if (lastBreak >= lineStart) {
                    int end = lastBreak;
                    while (end > lineStart && isBreakable(codepoints.get(end - 1).codepoint)) {
                        end--;
                    }
                    result.add(buildComponentFromCodepoints(codepoints.subList(lineStart, end)));
                    lineStart = lastBreak + 1;
                    while (lineStart <= i && isBreakable(codepoints.get(lineStart).codepoint)) {
                        lineStart++;
                    }
                } else {
                    result.add(buildComponentFromCodepoints(codepoints.subList(lineStart, clusterStart)));
                    lineStart = clusterStart;
                }
                lastBreak = -1;
            }
            clusterStart = i + 1;
        }
        if (lineStart < size) {
            result.add(buildComponentFromCodepoints(codepoints.subList(lineStart, size)));
        }
        if (result.isEmpty()) {
            result.add(Component.literal(" "));
        }
        return result;
    }

    @NotNull
    private static FormattedCharSequence toSequence(@NotNull List<StyledCodepoint> codepoints) {
        return sink -> {
            int index = 0;
            for (StyledCodepoint entry : codepoints) {
                if (!sink.accept(index++, entry.style, entry.codepoint)) {
                    return false;
                }
            }
            return true;
        };
    }

    private static boolean isBreakable(int codepoint) {
        return Character.isWhitespace(codepoint);
    }

    @NotNull
    private static MutableComponent buildComponentFromCodepoints(@NotNull List<StyledCodepoint> codepoints) {
        if (codepoints.isEmpty()) {
            return Component.literal(" ");
        }
        MutableComponent line = Component.literal("");
        StringBuilder run = new StringBuilder();
        Style runStyle = null;
        for (StyledCodepoint entry : codepoints) {
            if (entry.codepoint == '\n') {
                continue;
            }
            Style style = entry.style != null ? entry.style : Style.EMPTY;
            if (runStyle == null) {
                runStyle = style;
            }
            if (!runStyle.equals(style)) {
                if (!run.isEmpty()) {
                    line.append(Component.literal(run.toString()).withStyle(runStyle));
                    run.setLength(0);
                }
                runStyle = style;
            }
            run.appendCodePoint(entry.codepoint);
        }
        if (!run.isEmpty()) {
            line.append(Component.literal(run.toString()).withStyle(runStyle == null ? Style.EMPTY : runStyle));
        }
        if (line.getString().isBlank()) {
            return Component.literal(" ");
        }
        return line;
    }

    private static final class StyledCodepoint {
        private final int codepoint;
        private final Style style;

        private StyledCodepoint(int codepoint, @NotNull Style style) {
            this.codepoint = codepoint;
            this.style = style;
        }
    }

}
