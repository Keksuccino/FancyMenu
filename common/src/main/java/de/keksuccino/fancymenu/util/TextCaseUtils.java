package de.keksuccino.fancymenu.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class TextCaseUtils {

    @NotNull
    public static String toTitleCase(@NotNull String input) {
        StringBuilder out = new StringBuilder(input.length());
        boolean newWord = true;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
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
        String lower = input.toLowerCase();
        StringBuilder out = new StringBuilder(lower.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
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

    @NotNull
    private static List<String> splitWords(@NotNull String input) {
        List<String> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int length = input.length();
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                if (current.length() > 0) {
                    words.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            if (current.length() > 0) {
                char prev = input.charAt(i - 1);
                boolean prevIsLetterOrDigit = Character.isLetterOrDigit(prev);
                if (prevIsLetterOrDigit) {
                    boolean currIsDigit = Character.isDigit(c);
                    boolean prevIsDigit = Character.isDigit(prev);
                    boolean currIsUpper = Character.isUpperCase(c);
                    boolean prevIsUpper = Character.isUpperCase(prev);
                    boolean prevIsLower = Character.isLowerCase(prev);
                    boolean nextIsLower = (i + 1 < length) && Character.isLowerCase(input.charAt(i + 1));
                    boolean split = (currIsDigit != prevIsDigit)
                        || (currIsUpper && prevIsLower)
                        || (currIsUpper && prevIsUpper && nextIsLower);
                    if (split) {
                        words.add(current.toString());
                        current.setLength(0);
                    }
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            words.add(current.toString());
        }

        return words;
    }

}
