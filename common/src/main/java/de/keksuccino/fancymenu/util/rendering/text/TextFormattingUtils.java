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
     * Line-wraps components using the SmoothTextRenderer measurement logic.
     */
    @NotNull
    public static <C extends Component> List<MutableComponent> lineWrapComponentsSmooth(@NotNull List<C> lines, @NotNull SmoothFont font, float textSize, float maxWidth, float renderScale) {
        Objects.requireNonNull(font);
        List<MutableComponent> wrappedLines = new ArrayList<>();
        for (Component line : lines) {
            if (line.getString().isBlank()) line = Component.literal(" ");
            List<StyledCodepoint> codepoints = collectStyledCodepoints(line);
            wrappedLines.addAll(wrapSmoothCodepoints(codepoints, font, textSize, maxWidth, renderScale));
        }
        return wrappedLines;
    }

    /**
     * Line-wraps components using the SmoothTextRenderer measurement logic.
     */
    @NotNull
    public static <C extends Component> List<MutableComponent> lineWrapComponentsSmooth(@NotNull C lines, @NotNull SmoothFont font, float textSize, float maxWidth, float renderScale) {
        return lineWrapComponentsSmooth(List.of(lines), font, textSize, maxWidth, renderScale);
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
    private static List<MutableComponent> wrapSmoothCodepoints(@NotNull List<StyledCodepoint> codepoints, @NotNull SmoothFont font, float textSize, float maxWidth, float renderScale) {
        List<MutableComponent> result = new ArrayList<>();
        if (codepoints.isEmpty()) {
            result.add(Component.literal(" "));
            return result;
        }

        List<StyledCodepoint> currentLine = new ArrayList<>();
        int lastBreakIndex = -1;

        for (int i = 0; i < codepoints.size(); i++) {
            StyledCodepoint entry = codepoints.get(i);
            if (entry.codepoint == '\n') {
                result.add(buildComponentFromCodepoints(currentLine));
                currentLine.clear();
                lastBreakIndex = -1;
                continue;
            }

            currentLine.add(entry);
            if (isBreakable(entry.codepoint)) {
                lastBreakIndex = currentLine.size() - 1;
            }

            if (maxWidth > 0.0F && currentLine.size() > 1) {
                float width = measureSmoothWidth(currentLine, font, textSize, renderScale);
                if (width > maxWidth) {
                    if (lastBreakIndex >= 0) {
                        int endIndex = trimTrailingWhitespaceIndex(currentLine, lastBreakIndex);
                        result.add(buildComponentFromCodepoints(currentLine.subList(0, endIndex)));
                        currentLine.subList(0, lastBreakIndex + 1).clear();
                        trimLeadingWhitespace(currentLine);
                    } else {
                        result.add(buildComponentFromCodepoints(currentLine.subList(0, currentLine.size() - 1)));
                        StyledCodepoint last = currentLine.get(currentLine.size() - 1);
                        currentLine.clear();
                        currentLine.add(last);
                    }
                    lastBreakIndex = findLastBreakIndex(currentLine);
                }
            }
        }

        if (!currentLine.isEmpty()) {
            result.add(buildComponentFromCodepoints(currentLine));
        }

        if (result.isEmpty()) {
            result.add(Component.literal(" "));
        }

        return result;
    }

    private static float measureSmoothWidth(@NotNull List<StyledCodepoint> codepoints, @NotNull SmoothFont font, float textSize, float renderScale) {
        if (codepoints.isEmpty()) return 0.0F;
        FormattedCharSequence sequence = sink -> {
            int index = 0;
            for (StyledCodepoint entry : codepoints) {
                if (!sink.accept(index++, entry.style, entry.codepoint)) {
                    return false;
                }
            }
            return true;
        };
        return SmoothTextRenderer.getTextWidth(font, sequence, textSize, renderScale);
    }

    private static int trimTrailingWhitespaceIndex(@NotNull List<StyledCodepoint> line, int endIndex) {
        int trimmed = Math.min(endIndex, line.size());
        while (trimmed > 0 && isBreakable(line.get(trimmed - 1).codepoint)) {
            trimmed--;
        }
        return trimmed;
    }

    private static void trimLeadingWhitespace(@NotNull List<StyledCodepoint> line) {
        while (!line.isEmpty() && isBreakable(line.get(0).codepoint)) {
            line.remove(0);
        }
    }

    private static int findLastBreakIndex(@NotNull List<StyledCodepoint> line) {
        for (int i = line.size() - 1; i >= 0; i--) {
            if (isBreakable(line.get(i).codepoint)) {
                return i;
            }
        }
        return -1;
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
