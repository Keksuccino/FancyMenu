//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;

/**
 * Reads the values of one placeholder in place, from its raw text.
 *
 * <p>Values can contain nested placeholders and quotes without escaping, so the end of a value can't be found by its quotes alone.
 * A value starts after <code>"name":"</code> of one of the value names of the placeholder. It ends at the first unescaped quote
 * that is followed by <code>,"name":"</code> of another value name, or by the two closing braces at the end of the placeholder.
 * Structural whitespace (see {@link PlaceholderScanner#skipStructuralWhitespace(String, int)}) can stand between these tokens.
 * The content of a value is kept exactly as it is, including all of its whitespace.</p>
 *
 * <p>Brace blocks inside a value, like nested placeholders, can't end the value, so quotes inside them get ignored.
 * A block uses the same end rules as placeholders (see {@link PlaceholderScanner}). A <code>{</code> without an end is a normal
 * character of the value.</p>
 */
final class PlaceholderValueParser {

    private static final char OPEN_BRACE = '{';
    private static final char CLOSE_BRACE = '}';
    private static final char QUOTE = '"';
    private static final char COLON = ':';
    private static final char COMMA = ',';

    private final String placeholder;
    private final int length;
    private final List<String> valueNames;
    @Nullable
    private PlaceholderScanner blockScanner;
    @Nullable
    private String matchedName;

    private PlaceholderValueParser(@NotNull String placeholder, @NotNull List<String> valueNames) {
        this.placeholder = placeholder;
        this.length = placeholder.length();
        this.valueNames = valueNames;
    }

    /**
     * @param placeholder The raw text of one placeholder, from its opening to its closing brace.
     * @param identifierStart The index of the first character of the identifier, or -1 if it is unknown, so the values get searched from the start.
     * @param valueNames The value names of the placeholder. Values with other names get ignored.
     */
    @NotNull
    static HashMap<String, String> parseValues(@NotNull String placeholder, int identifierStart, @NotNull List<String> valueNames) {
        HashMap<String, String> values = new HashMap<>();
        if (valueNames.isEmpty()) return values;
        int searchFrom = 0;
        if (identifierStart >= 0) {
            int identifierEnd = placeholder.indexOf(QUOTE, identifierStart);
            if (identifierEnd < 0) return values;
            searchFrom = identifierEnd + 1;
        }
        // The values come after the comma behind the identifier
        int commaIndex = placeholder.indexOf(COMMA, searchFrom);
        if (commaIndex < 0) return values;
        new PlaceholderValueParser(placeholder, valueNames).readValues(commaIndex + 1, values);
        return values;
    }

    private void readValues(int from, @NotNull HashMap<String, String> values) {
        int index = from;
        while (index < this.length) {
            int quoteIndex = this.placeholder.indexOf(QUOTE, index);
            if (quoteIndex < 0) return;
            int contentStart = this.matchValueStart(quoteIndex);
            if (contentStart < 0) {
                index = quoteIndex + 1;
                continue;
            }
            String name = this.matchedName;
            int contentEnd = this.findContentEnd(contentStart);
            // A value without an end takes the rest of the placeholder, so nothing after it can be a value
            if (contentEnd < 0) return;
            values.put(name, this.placeholder.substring(contentStart, contentEnd));
            index = contentEnd + 1;
        }
    }

    /**
     * Checks if <code>"name":"</code> of a value name starts at the quote, and remembers the name in {@link #matchedName}.
     *
     * @return The index of the first character of the value content, or -1 if no value starts at the quote.
     */
    private int matchValueStart(int quoteIndex) {
        int nameStart = quoteIndex + 1;
        for (String name : this.valueNames) {
            int nameEnd = nameStart + name.length();
            if ((nameEnd < this.length) && (this.placeholder.charAt(nameEnd) == QUOTE) && this.placeholder.startsWith(name, nameStart)) {
                int index = PlaceholderScanner.skipStructuralWhitespace(this.placeholder, nameEnd + 1);
                if ((index >= this.length) || (this.placeholder.charAt(index) != COLON)) continue;
                index = PlaceholderScanner.skipStructuralWhitespace(this.placeholder, index + 1);
                if ((index >= this.length) || (this.placeholder.charAt(index) != QUOTE)) continue;
                this.matchedName = name;
                return index + 1;
            }
        }
        return -1;
    }

    /**
     * @return The index of the quote that ends the value content, or -1 if the value has no end.
     */
    private int findContentEnd(int contentStart) {
        for (int i = contentStart; i < this.length; i++) {
            char c = this.placeholder.charAt(i);
            if (c == QUOTE) {
                if (!PlaceholderScanner.isEscaped(this.placeholder, i) && this.isValueEnd(i)) return i;
            } else if (c == OPEN_BRACE) {
                if (!PlaceholderScanner.isEscaped(this.placeholder, i)) {
                    int blockEnd = this.getBlockScanner().findEnd(i);
                    if (blockEnd >= 0) i = blockEnd;
                }
            }
        }
        return -1;
    }

    private boolean isValueEnd(int quoteIndex) {
        int index = PlaceholderScanner.skipStructuralWhitespace(this.placeholder, quoteIndex + 1);
        if (index >= this.length) return false;
        char c = this.placeholder.charAt(index);
        if (c == COMMA) {
            index = PlaceholderScanner.skipStructuralWhitespace(this.placeholder, index + 1);
            return (index < this.length) && (this.placeholder.charAt(index) == QUOTE) && (this.matchValueStart(index) >= 0);
        }
        if (c == CLOSE_BRACE) {
            // The closing braces of the values and of the placeholder
            index = PlaceholderScanner.skipStructuralWhitespace(this.placeholder, index + 1);
            if ((index >= this.length) || (this.placeholder.charAt(index) != CLOSE_BRACE)) return false;
            return PlaceholderScanner.skipStructuralWhitespace(this.placeholder, index + 1) >= this.length;
        }
        return false;
    }

    @NotNull
    private PlaceholderScanner getBlockScanner() {
        if (this.blockScanner == null) this.blockScanner = new PlaceholderScanner(this.placeholder);
        return this.blockScanner;
    }

}
