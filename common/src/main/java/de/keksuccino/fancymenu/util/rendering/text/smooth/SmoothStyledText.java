package de.keksuccino.fancymenu.util.rendering.text.smooth;

import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatter;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatterRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * Text as codepoints with one packed style per codepoint. This is the single input format of the text layout, no matter if the
 * text came from a legacy formatted string or a component, and it doubles as the content-based key of the layout cache.
 * <p>
 * A packed style keeps only what affects layout and drawing: the RGB color (or "base color" when {@link #HAS_COLOR} is unset)
 * and the format flags. Instances are mutable and get reused as lookup keys, so only {@linkplain #copy() copies} may be stored.
 * </p>
 */
final class SmoothStyledText {

    static final int COLOR_MASK = 0xFFFFFF;
    static final int HAS_COLOR = 1 << 24;
    static final int BOLD = 1 << 25;
    static final int ITALIC = 1 << 26;
    static final int UNDERLINE = 1 << 27;
    static final int STRIKETHROUGH = 1 << 28;
    static final int OBFUSCATED = 1 << 29;
    static final int FORMAT_FLAGS = BOLD | ITALIC | UNDERLINE | STRIKETHROUGH | OBFUSCATED;

    private static final char FORMAT_PREFIX = ChatFormatting.PREFIX_CODE;
    private static final int HEX_COLOR_CODE_LENGTH = 13;

    private int[] codepoints;
    private int[] styles;
    private int length;
    private boolean multiline;
    private int hash;
    private final FormattedCharSink sink = (index, style, codepoint) -> {
        this.add(codepoint, packStyle(style));
        return true;
    };

    SmoothStyledText(int initialCapacity) {
        int capacity = Math.max(1, initialCapacity);
        this.codepoints = new int[capacity];
        this.styles = new int[capacity];
    }

    private SmoothStyledText(@NotNull int[] codepoints, @NotNull int[] styles, int length, boolean multiline, int hash) {
        this.codepoints = codepoints;
        this.styles = styles;
        this.length = length;
        this.multiline = multiline;
        this.hash = hash;
    }

    /**
     * Clears the text, so the instance can get filled again.
     *
     * @param multiline whether line breaks start new lines; it is part of the cache key, because it changes the layout
     */
    @NotNull
    SmoothStyledText reset(boolean multiline) {
        this.length = 0;
        this.multiline = multiline;
        this.hash = multiline ? 1 : 0;
        return this;
    }

    void add(int codepoint, int style) {
        if (this.length == this.codepoints.length) {
            int capacity = this.codepoints.length * 2;
            this.codepoints = Arrays.copyOf(this.codepoints, capacity);
            this.styles = Arrays.copyOf(this.styles, capacity);
        }
        this.codepoints[this.length] = codepoint;
        this.styles[this.length] = style;
        this.length++;
        this.hash = (((this.hash * 31) + codepoint) * 31) + style;
    }

    @NotNull
    SmoothStyledText appendSequence(@NotNull FormattedCharSequence sequence) {
        sequence.accept(this.sink);
        return this;
    }

    /**
     * Appends a string with legacy formatting codes: the vanilla {@code §} codes, the {@code §x§R§R§G§G§B§B} hex color format,
     * and the codes of FancyMenu's {@link TextColorFormatterRegistry}. Color codes reset the format flags, like in vanilla.
     */
    @NotNull
    SmoothStyledText appendLegacy(@NotNull String text) {
        int style = 0;
        int textLength = text.length();
        int index = 0;
        while (index < textLength) {
            char c = text.charAt(index);
            if (c == FORMAT_PREFIX && index + 1 < textLength) {
                long parsed = parseFormatting(text, index + 1, style);
                int consumed = (int) (parsed >>> 32);
                if (consumed > 0) {
                    style = (int) parsed;
                    index += consumed + 1;
                    continue;
                }
            }
            int codepoint = text.codePointAt(index);
            index += Character.charCount(codepoint);
            this.add(codepoint, style);
        }
        return this;
    }

    /**
     * Parses the formatting code at {@code codeIndex}.
     *
     * @return the number of consumed chars in the upper 32 bits (0 when the code is unknown) and the new style in the lower 32 bits
     */
    private static long parseFormatting(@NotNull String text, int codeIndex, int style) {
        char code = text.charAt(codeIndex);
        ChatFormatting formatting = ChatFormatting.getByCode(code);
        if (formatting == null) {
            if (Character.toLowerCase(code) == 'x') {
                int hexColor = parseHexColor(text, codeIndex);
                if (hexColor >= 0) {
                    return parseResult(HEX_COLOR_CODE_LENGTH, HAS_COLOR | hexColor);
                }
            }
            TextColorFormatter formatter = TextColorFormatterRegistry.getByCode(code);
            if (formatter != null) {
                return parseResult(1, HAS_COLOR | (formatter.getColor().getColorInt() & COLOR_MASK));
            }
            return 0L;
        }
        if (formatting == ChatFormatting.RESET) {
            return parseResult(1, 0);
        }
        int color = legacyColor(formatting);
        if (color >= 0) {
            return parseResult(1, HAS_COLOR | color);
        }
        int flag = switch (formatting) {
            case BOLD -> BOLD;
            case ITALIC -> ITALIC;
            case UNDERLINE -> UNDERLINE;
            case STRIKETHROUGH -> STRIKETHROUGH;
            case OBFUSCATED -> OBFUSCATED;
            default -> 0;
        };
        return parseResult(1, style | flag);
    }

    /**
     * RGB value of a color formatting code, or {@code -1} for format codes. Listed explicitly, because {@link ChatFormatting}
     * does not expose its colors in every Minecraft version.
     */
    private static int legacyColor(@NotNull ChatFormatting formatting) {
        return switch (formatting) {
            case BLACK -> 0x000000;
            case DARK_BLUE -> 0x0000AA;
            case DARK_GREEN -> 0x00AA00;
            case DARK_AQUA -> 0x00AAAA;
            case DARK_RED -> 0xAA0000;
            case DARK_PURPLE -> 0xAA00AA;
            case GOLD -> 0xFFAA00;
            case GRAY -> 0xAAAAAA;
            case DARK_GRAY -> 0x555555;
            case BLUE -> 0x5555FF;
            case GREEN -> 0x55FF55;
            case AQUA -> 0x55FFFF;
            case RED -> 0xFF5555;
            case LIGHT_PURPLE -> 0xFF55FF;
            case YELLOW -> 0xFFFF55;
            case WHITE -> 0xFFFFFF;
            default -> -1;
        };
    }

    private static long parseResult(int consumed, int style) {
        return ((long) consumed << 32) | (style & 0xFFFFFFFFL);
    }

    /**
     * Parses the six {@code §}-separated hex digits that follow an {@code x} code, or returns {@code -1} when they are malformed.
     */
    private static int parseHexColor(@NotNull String text, int codeIndex) {
        if (codeIndex + 12 >= text.length()) {
            return -1;
        }
        int rgb = 0;
        for (int i = 0; i < 6; i++) {
            int prefixIndex = codeIndex + 1 + (i * 2);
            if (text.charAt(prefixIndex) != FORMAT_PREFIX) {
                return -1;
            }
            int digit = Character.digit(text.charAt(prefixIndex + 1), 16);
            if (digit < 0) {
                return -1;
            }
            rgb = (rgb << 4) | digit;
        }
        return rgb;
    }

    static int packStyle(@NotNull Style style) {
        int packed = 0;
        TextColor color = style.getColor();
        if (color != null) {
            packed = HAS_COLOR | (color.getValue() & COLOR_MASK);
        }
        if (style.isBold()) packed |= BOLD;
        if (style.isItalic()) packed |= ITALIC;
        if (style.isUnderlined()) packed |= UNDERLINE;
        if (style.isStrikethrough()) packed |= STRIKETHROUGH;
        if (style.isObfuscated()) packed |= OBFUSCATED;
        return packed;
    }

    /**
     * Resolves the ARGB color of a packed style, keeping the alpha of the base color like vanilla text rendering does.
     */
    static int resolveColor(int style, int baseColor) {
        if ((style & HAS_COLOR) == 0) {
            return baseColor;
        }
        return (baseColor & 0xFF000000) | (style & COLOR_MASK);
    }

    int length() {
        return this.length;
    }

    boolean multiline() {
        return this.multiline;
    }

    int codepointAt(int index) {
        return this.codepoints[index];
    }

    int styleAt(int index) {
        return this.styles[index];
    }

    /**
     * Direct access to the codepoint buffer; only the first {@link #length()} entries are valid.
     */
    @NotNull
    int[] codepointBuffer() {
        return this.codepoints;
    }

    /**
     * Returns an immutable, exactly-sized copy that can be stored as a cache key.
     */
    @NotNull
    SmoothStyledText copy() {
        return new SmoothStyledText(Arrays.copyOf(this.codepoints, this.length), Arrays.copyOf(this.styles, this.length), this.length, this.multiline, this.hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SmoothStyledText other)) {
            return false;
        }
        return this.hash == other.hash
                && this.length == other.length
                && this.multiline == other.multiline
                && Arrays.equals(this.codepoints, 0, this.length, other.codepoints, 0, other.length)
                && Arrays.equals(this.styles, 0, this.length, other.styles, 0, other.length);
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

}
