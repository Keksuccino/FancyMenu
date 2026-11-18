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

    /**
     * The codes {@link #replaceFormattingCodes(String, String, String)} converts, in the order of its replacement passes.
     * {@link #isFormattingCode(char)} checks for the same characters and has to stay in sync with this.
     */
    private static final String[] FORMATTING_CODES = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "l", "m", "n", "o", "k", "r"};

    /**
     * Replaces the prefix of every formatting code in the text, for example {@code &a} with {@code §a}. Formatting codes are
     * {@code 0-9} and the lowercase letters {@code a-f}, {@code k-o} and {@code r}; uppercase letters and all other characters stay as they are.
     *
     * <p>The result is defined as one full replacement pass per code, in the order of {@link #FORMATTING_CODES}, where every pass
     * replaces each non-overlapping occurrence of the old prefix followed by its code from left to right. When neither prefix contains
     * a code character, no pass can create, break or overlap an occurrence of another pass, so a single left-to-right pass that replaces
     * every old prefix followed by a code gives exactly the same result. Prefixes with code characters (never used by FancyMenu itself)
     * can affect later passes, so they get the passes one by one.</p>
     *
     * @return The text itself if it contains no formatting code with the old prefix, otherwise the converted text.
     */
    @NotNull
    public static String replaceFormattingCodes(@NotNull String in, @NotNull String oldPrefix, @NotNull String newPrefix) {
        Objects.requireNonNull(in);
        Objects.requireNonNull(oldPrefix);
        Objects.requireNonNull(newPrefix);
        if (containsFormattingCode(oldPrefix) || containsFormattingCode(newPrefix)) return replaceFormattingCodesPassByPass(in, oldPrefix, newPrefix);
        int prefixLength = oldPrefix.length();
        // A prefix needs a code character behind it, so prefixes that start after this index can't be part of a code
        int lastCodePrefixStart = in.length() - prefixLength - 1;
        StringBuilder replaced = null;
        int copiedUntil = 0;
        int prefixStart = indexOfPrefix(in, oldPrefix, 0);
        while ((prefixStart >= 0) && (prefixStart <= lastCodePrefixStart)) {
            char code = in.charAt(prefixStart + prefixLength);
            if (isFormattingCode(code)) {
                if (replaced == null) replaced = new StringBuilder(in.length() + 16);
                replaced.append(in, copiedUntil, prefixStart).append(newPrefix).append(code);
                copiedUntil = prefixStart + prefixLength + 1;
                prefixStart = indexOfPrefix(in, oldPrefix, copiedUntil);
            } else {
                // Continue right behind the start of this prefix, because the next one can overlap it (like the second & of &&a)
                prefixStart = indexOfPrefix(in, oldPrefix, prefixStart + 1);
            }
        }
        if (replaced == null) return in;
        return replaced.append(in, copiedUntil, in.length()).toString();
    }

    private static int indexOfPrefix(@NotNull String text, @NotNull String prefix, int fromIndex) {
        // The char search is much faster for short texts, and all of FancyMenu's prefixes are one char long
        return (prefix.length() == 1) ? text.indexOf(prefix.charAt(0), fromIndex) : text.indexOf(prefix, fromIndex);
    }

    @NotNull
    private static String replaceFormattingCodesPassByPass(@NotNull String in, @NotNull String oldPrefix, @NotNull String newPrefix) {
        for (String code : FORMATTING_CODES) {
            in = StringUtils.replace(in, oldPrefix + code, newPrefix + code);
        }
        return in;
    }

    private static boolean containsFormattingCode(@NotNull String text) {
        for (int i = 0; i < text.length(); i++) {
            if (isFormattingCode(text.charAt(i))) return true;
        }
        return false;
    }

    private static boolean isFormattingCode(char c) {
        return ((c >= '0') && (c <= '9')) || ((c >= 'a') && (c <= 'f')) || ((c >= 'k') && (c <= 'o')) || (c == 'r');
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
