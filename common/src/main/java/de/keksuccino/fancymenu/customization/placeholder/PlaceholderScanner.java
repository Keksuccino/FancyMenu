//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds placeholders in a text, in place and without copying it.
 *
 * <p><b>Start:</b> a <code>{</code> followed by <code>"placeholder"</code>, a colon and the opening quote of the identifier.
 * Structural whitespace (see {@link #skipStructuralWhitespace(String, int)}) can stand between these tokens, nothing else.</p>
 *
 * <p><b>End:</b> the first <code>}</code> after the start that is not escaped, not inside quotes and does not close a
 * <code>{</code> that got opened after the start. The quote state toggles at every unescaped <code>"</code>, and a backslash
 * escapes the next character, inside and outside of quotes. The start brace itself is never escaped.</p>
 *
 * <p>A start without an end is not a placeholder, and the search goes on right after it, so placeholders inside broken
 * placeholders and inside other JSON still get found. After a found placeholder, the search goes on after its end.</p>
 *
 * <p>These rules must stay exactly as they are, layouts depend on them. {@code PlaceholderScannerDifferentialTest} compares them
 * with the original implementation on many generated texts, so run it after every change here.</p>
 *
 * <p>An instance is a cursor over one text and is not thread-safe. The static methods are.</p>
 */
final class PlaceholderScanner {

    /** Every placeholder start contains this, so it gets searched first, which skips all other braces of the text at once. */
    static final String PLACEHOLDER_KEY = "\"placeholder\"";
    private static final char OPEN_BRACE = '{';
    private static final char CLOSE_BRACE = '}';
    private static final char QUOTE = '"';
    private static final char BACKSLASH = '\\';
    private static final char COLON = ':';
    private static final char PERCENT = '%';
    private static final char LOWERCASE_N = 'n';

    private final String text;
    private final int length;
    private int searchFrom = 0;
    private int start = -1;
    private int end = -1;
    private int identifierStart = -1;
    private boolean endScanFailed = false;
    @Nullable
    private int[] endTable;
    private int endTableStart;

    PlaceholderScanner(@NotNull String text) {
        this.text = text;
        this.length = text.length();
    }

    /**
     * Moves to the next placeholder of the text.
     *
     * @return False if there are no more placeholders.
     */
    boolean next() {
        while (this.searchFrom < this.length) {
            int keyIndex = this.text.indexOf(PLACEHOLDER_KEY, this.searchFrom);
            if (keyIndex < 0) break;
            int braceIndex = findStartBrace(this.text, keyIndex, this.searchFrom);
            if (braceIndex >= 0) {
                int identifierIndex = matchPlaceholderStart(this.text, braceIndex);
                if (identifierIndex >= 0) {
                    int endIndex = this.findEnd(braceIndex);
                    if (endIndex >= 0) {
                        this.start = braceIndex;
                        this.end = endIndex + 1;
                        this.identifierStart = identifierIndex;
                        this.searchFrom = endIndex + 1;
                        return true;
                    }
                }
            }
            // No other start can use this key, and the next start comes after it
            this.searchFrom = keyIndex + 1;
        }
        this.searchFrom = this.length;
        this.start = -1;
        this.end = -1;
        this.identifierStart = -1;
        return false;
    }

    /** The index of the opening brace of the current placeholder. */
    int start() {
        return this.start;
    }

    /** The index after the closing brace of the current placeholder. */
    int end() {
        return this.end;
    }

    /** The index of the first character of the identifier of the current placeholder, right after its opening quote. */
    int identifierStart() {
        return this.identifierStart;
    }

    /**
     * Finds the end of the brace block that starts at the given <code>{</code>, using the end rules of placeholders (see class comment).
     *
     * <p>A start without an end has to scan the rest of the text, so many broken starts would take quadratic time.
     * After the first start without an end, this switches to an end table for the rest of the text,
     * built in one pass, which answers every later start in constant time with the exact same result.
     * Texts without broken starts never build the table.</p>
     *
     * @return The index of the closing brace, or -1 if the block has no end.
     */
    int findEnd(int openBraceIndex) {
        int[] table = this.endTable;
        if ((table != null) && (openBraceIndex >= this.endTableStart)) return table[openBraceIndex - this.endTableStart];
        if (this.endScanFailed) {
            this.buildEndTable(openBraceIndex);
            return this.endTable[0];
        }
        int endIndex = scanEnd(this.text, openBraceIndex);
        if (endIndex < 0) this.endScanFailed = true;
        return endIndex;
    }

    /**
     * Checks if the text contains a placeholder start, so with the same rules as {@link #next()}, but without looking for its end.
     */
    static boolean containsPlaceholderStart(@NotNull String text) {
        int keyIndex = text.indexOf(PLACEHOLDER_KEY);
        while (keyIndex >= 0) {
            int braceIndex = findStartBrace(text, keyIndex, 0);
            if ((braceIndex >= 0) && (matchPlaceholderStart(text, braceIndex) >= 0)) return true;
            keyIndex = text.indexOf(PLACEHOLDER_KEY, keyIndex + 1);
        }
        return false;
    }

    /**
     * Checks if a placeholder starts at the given index.
     *
     * @return The index of the first character of the identifier, right after its opening quote, or -1 if no placeholder starts there.
     */
    static int matchPlaceholderStart(@NotNull String text, int openBraceIndex) {
        int length = text.length();
        if ((openBraceIndex < 0) || (openBraceIndex >= length) || (text.charAt(openBraceIndex) != OPEN_BRACE)) return -1;
        int index = skipStructuralWhitespace(text, openBraceIndex + 1);
        if (!text.startsWith(PLACEHOLDER_KEY, index)) return -1;
        index = skipStructuralWhitespace(text, index + PLACEHOLDER_KEY.length());
        if ((index >= length) || (text.charAt(index) != COLON)) return -1;
        index = skipStructuralWhitespace(text, index + 1);
        if ((index >= length) || (text.charAt(index) != QUOTE)) return -1;
        return index + 1;
    }

    /**
     * Skips structural whitespace, which is allowed between the tokens of a placeholder (outside of identifiers, value names and values):
     * spaces, tabs, line breaks and the <code>%n%</code> line break code of single-line texts.
     *
     * @return The index of the first character that is not structural whitespace, or the length of the text.
     */
    static int skipStructuralWhitespace(@NotNull String text, int index) {
        int length = text.length();
        while (index < length) {
            char c = text.charAt(index);
            if ((c == ' ') || (c == '\t') || (c == '\n') || (c == '\r')) {
                index++;
            } else if ((c == PERCENT) && ((index + 2) < length) && (text.charAt(index + 1) == LOWERCASE_N) && (text.charAt(index + 2) == PERCENT)) {
                index += 3;
            } else {
                break;
            }
        }
        return index;
    }

    /**
     * Checks if the character at the index is escaped, so if an odd number of backslashes stands right before it.
     */
    static boolean isEscaped(@NotNull String text, int index) {
        int backslashes = 0;
        for (int i = index - 1; (i >= 0) && (text.charAt(i) == BACKSLASH); i--) {
            backslashes++;
        }
        return (backslashes & 1) == 1;
    }

    /**
     * Only structural whitespace can stand between the start brace and the key, so the brace is the first character before the key
     * that can't be part of structural whitespace. {@link #matchPlaceholderStart(String, int)} then checks the whitespace exactly.
     *
     * @return The index of the brace, or -1 if the key has no brace before it.
     */
    private static int findStartBrace(@NotNull String text, int keyIndex, int lowerBound) {
        int index = keyIndex - 1;
        while (index >= lowerBound) {
            char c = text.charAt(index);
            if ((c != ' ') && (c != '\t') && (c != '\n') && (c != '\r') && (c != PERCENT) && (c != LOWERCASE_N)) break;
            index--;
        }
        return ((index >= lowerBound) && (text.charAt(index) == OPEN_BRACE)) ? index : -1;
    }

    private static int scanEnd(@NotNull String text, int openBraceIndex) {
        int depth = 0;
        boolean escaped = false;
        boolean inQuotes = false;
        for (int i = openBraceIndex + 1, length = text.length(); i < length; i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == BACKSLASH) {
                escaped = true;
            } else if (c == QUOTE) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == OPEN_BRACE) {
                    depth++;
                } else if (c == CLOSE_BRACE) {
                    if (depth == 0) return i;
                    depth--;
                }
            }
        }
        return -1;
    }

    /**
     * Builds the end of every <code>{</code> from the given index to the end of the text in one pass from back to front.
     *
     * <p>For a start brace at {@code s}, whether a later character is escaped only depends on the backslashes right before it,
     * which can't reach back past the start brace, so it is the same for every start. A later brace is inside quotes if an odd
     * number of unescaped quotes stands between the start and the brace. So all starts fall into two classes: the ones with an
     * even and the ones with an odd number of unescaped quotes after them. Within one class, the braces outside of quotes are the
     * same for every start, and the end of a start is the first of these closing braces that is not matched by an opening one.</p>
     *
     * <p>The pass keeps the next unmatched closing brace of both classes. At a closing brace, the table stores the next unmatched
     * closing brace after it, and the brace becomes the next unmatched one. At an opening brace, the table stores the end of the
     * start there (the current next unmatched closing brace of its class), and that closing brace gets matched by it, so the next
     * unmatched one becomes the one stored for that closing brace. Only brace positions of the table get used.</p>
     */
    private void buildEndTable(int from) {
        String text = this.text;
        int[] table = new int[this.length - from];
        int nextCloseEven = -1;
        int nextCloseOdd = -1;
        boolean oddQuotesAfter = false;
        for (int i = this.length - 1; i >= from; i--) {
            char c = text.charAt(i);
            if (c == QUOTE) {
                if (!isEscaped(text, i)) oddQuotesAfter = !oddQuotesAfter;
            } else if (c == OPEN_BRACE) {
                int endIndex = oddQuotesAfter ? nextCloseOdd : nextCloseEven;
                table[i - from] = endIndex;
                if (!isEscaped(text, i)) {
                    int nextClose = (endIndex < 0) ? -1 : table[endIndex - from];
                    if (oddQuotesAfter) {
                        nextCloseOdd = nextClose;
                    } else {
                        nextCloseEven = nextClose;
                    }
                }
            } else if (c == CLOSE_BRACE) {
                if (!isEscaped(text, i)) {
                    if (oddQuotesAfter) {
                        table[i - from] = nextCloseOdd;
                        nextCloseOdd = i;
                    } else {
                        table[i - from] = nextCloseEven;
                        nextCloseEven = i;
                    }
                }
            }
        }
        this.endTable = table;
        this.endTableStart = from;
    }

}
