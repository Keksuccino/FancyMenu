package de.keksuccino.fancymenu.util.rendering.text;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class TextFormattingUtils {

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

}
