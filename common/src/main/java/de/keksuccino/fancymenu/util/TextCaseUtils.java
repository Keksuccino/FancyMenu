package de.keksuccino.fancymenu.util;

import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;

/**
 * Case transforms for texts.
 *
 * <p>Formatting codes (see {@link TextFormattingUtils#isFormattingCodeAt(String, int)}) are no visible text, so every transform keeps them
 * as they are and handles the text around them as if they weren't there. A case transform must never touch a code: {@code &a} would turn
 * into {@code &A}, which doesn't get converted to a formatting code anymore and shows up as text. Snake and kebab case only keep letters and
 * digits, so they leave out formatting codes like all other characters.</p>
 */
public class TextCaseUtils {

    /**
     * Converts the text to uppercase with the rules of the default locale, like {@link String#toUpperCase()}, but keeps formatting codes as they are.
     */
    @NotNull
    public static String toUpperCase(@NotNull String input) {
        return transformBetweenFormattingCodes(input, String::toUpperCase);
    }

    /**
     * Converts the text to lowercase with the rules of the default locale, like {@link String#toLowerCase()}, but keeps formatting codes as they are.
     */
    @NotNull
    public static String toLowerCase(@NotNull String input) {
        return transformBetweenFormattingCodes(input, String::toLowerCase);
    }

    @NotNull
    public static String toTitleCase(@NotNull String input) {
        StringBuilder out = new StringBuilder(input.length());
        boolean newWord = true;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (TextFormattingUtils.isFormattingCodeAt(input, i)) {
                out.append(c).append(input.charAt(++i));
                continue;
            }
            if (isApostropheInWord(input, i)) {
                out.append(c);
                continue;
            }
            if (Character.isLetterOrDigit(c)) {
                if (Character.isLetter(c)) {
                    if (newWord) {
                        out.append(String.valueOf(c).toUpperCase());
                        newWord = false;
                    } else {
                        out.append(String.valueOf(c).toLowerCase());
                    }
                } else {
                    out.append(c);
                }
            } else {
                out.append(c);
                newWord = true;
            }
        }
        return out.toString();
    }

    @NotNull
    public static String toSentenceCase(@NotNull String input) {
        String lower = toLowerCase(input);
        StringBuilder out = new StringBuilder(lower.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (TextFormattingUtils.isFormattingCodeAt(lower, i)) {
                out.append(c).append(lower.charAt(++i));
                continue;
            }
            if (Character.isLetter(c)) {
                if (capitalizeNext) {
                    out.append(String.valueOf(c).toUpperCase());
                    capitalizeNext = false;
                } else {
                    out.append(c);
                }
            } else {
                out.append(c);
                if (c == '.' || c == '!' || c == '?') {
                    capitalizeNext = true;
                }
            }
        }
        return out.toString();
    }

    @NotNull
    public static String toSnakeCase(@NotNull String input) {
        return joinWords(splitWords(input), "_");
    }

    @NotNull
    public static String toKebabCase(@NotNull String input) {
        return joinWords(splitWords(input), "-");
    }

    @NotNull
    public static String toAlternatingCase(@NotNull String input) {
        StringBuilder out = new StringBuilder(input.length());
        boolean upper = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (TextFormattingUtils.isFormattingCodeAt(input, i)) {
                out.append(c).append(input.charAt(++i));
                continue;
            }
            if (Character.isLetter(c)) {
                if (upper) {
                    out.append(String.valueOf(c).toUpperCase());
                } else {
                    out.append(String.valueOf(c).toLowerCase());
                }
                upper = !upper;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    @NotNull
    public static String toToggleCase(@NotNull String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (TextFormattingUtils.isFormattingCodeAt(input, i)) {
                out.append(c).append(input.charAt(++i));
                continue;
            }
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    out.append(String.valueOf(c).toLowerCase());
                } else {
                    out.append(String.valueOf(c).toUpperCase());
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Applies the transform to each part of the text between its formatting codes and keeps the codes between the parts as they are.
     *
     * @return The result of the transform for the whole text if the text has no formatting codes.
     */
    @NotNull
    private static String transformBetweenFormattingCodes(@NotNull String input, @NotNull UnaryOperator<String> transform) {
        StringBuilder out = null;
        int partStart = 0;
        int length = input.length();
        for (int i = 0; i < length; i++) {
            if (!TextFormattingUtils.isFormattingCodeAt(input, i)) continue;
            if (out == null) out = new StringBuilder(length);
            if (i > partStart) out.append(transform.apply(input.substring(partStart, i)));
            out.append(input, i, i + 2);
            partStart = i + 2;
            i++;
        }
        if (out == null) return transform.apply(input);
        if (partStart < length) out.append(transform.apply(input.substring(partStart)));
        return out.toString();
    }

    private static boolean isApostropheInWord(@NotNull String input, int index) {
        if (input.charAt(index) != '\'') {
            return false;
        }
        if (index == 0 || index + 1 >= input.length()) {
            return false;
        }
        return Character.isLetter(input.charAt(index - 1)) && Character.isLetter(input.charAt(index + 1));
    }

    @NotNull
    private static String joinWords(@NotNull List<String> words, @NotNull String separator) {
        if (words.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(separator);
        for (String word : words) {
            joiner.add(word.toLowerCase());
        }
        return joiner.toString();
    }

    /**
     * Splits the text into words of letters and digits. Formatting codes are neither part of a word nor a word break, so the characters
     * around a code get compared as if they stood next to each other.
     */
    @NotNull
    private static List<String> splitWords(@NotNull String input) {
        List<String> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int length = input.length();
        for (int i = 0; i < length; i++) {
            if (TextFormattingUtils.isFormattingCodeAt(input, i)) {
                i++;
                continue;
            }
            char c = input.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                if (current.length() > 0) {
                    words.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            if (current.length() > 0) {
                // The last character of the word, which is a letter or digit, and the next character that is not part of a formatting code
                char prev = current.charAt(current.length() - 1);
                int nextIndex = skipFormattingCodes(input, i + 1);
                boolean currIsDigit = Character.isDigit(c);
                boolean prevIsDigit = Character.isDigit(prev);
                boolean currIsUpper = Character.isUpperCase(c);
                boolean prevIsUpper = Character.isUpperCase(prev);
                boolean prevIsLower = Character.isLowerCase(prev);
                boolean nextIsLower = (nextIndex < length) && Character.isLowerCase(input.charAt(nextIndex));
                boolean split = (currIsDigit != prevIsDigit)
                    || (currIsUpper && prevIsLower)
                    || (currIsUpper && prevIsUpper && nextIsLower);
                if (split) {
                    words.add(current.toString());
                    current.setLength(0);
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            words.add(current.toString());
        }

        return words;
    }

    /**
     * @return The first index from the given one on where no formatting code starts.
     */
    private static int skipFormattingCodes(@NotNull String input, int index) {
        while (TextFormattingUtils.isFormattingCodeAt(input, index)) {
            index += 2;
        }
        return index;
    }

}
