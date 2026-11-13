package de.keksuccino.fancymenu.util.input;

import java.util.BitSet;
import de.keksuccino.fancymenu.util.Legacy;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CharacterFilter {

    @NotNull
    public static CharacterFilter buildDecimalFiler() {
        CharacterFilter f = buildIntegerFilter();
        f.addAllowedCharacters(".");
        return f;
    }

    @NotNull
    public static CharacterFilter buildIntegerFilter() {
        CharacterFilter f = buildDigitFilter();
        f.addAllowedCharacters("-", "+");
        return f;
    }

    @NotNull
    public static CharacterFilter buildDigitFilter() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        return f;
    }

    @NotNull
    public static CharacterFilter buildResourceNameFilter() {
        //Support for dots (".") is needed for file extensions
        return buildOnlyLowercaseFileNameFilter();
    }

    @NotNull
    public static CharacterFilter buildOnlyLowercaseFileNameFilter() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters(
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "_", "-"
        );
        return f;
    }

    @NotNull
    public static CharacterFilter buildLowercaseAndUppercaseFileNameFilter() {
        CharacterFilter f = buildOnlyLowercaseFileNameFilter();
        f.addAllowedCharacters(
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
                "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
        );
        return f;
    }

    @NotNull
    public static CharacterFilter buildUrlFilter() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters(
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h",
                "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
                "S", "T", "U", "V", "W", "X", "Y", "Z", ".", "-", "_", "~", "+", "#", ",", "%", "&", "=",
                "*", ";", ":", "@", "?", "/", "\\"
        );
        return f;
    }

    // One bit per character, so checking a character takes the same short time for every filter and never creates objects
    private final BitSet allowed = new BitSet();
    private final BitSet forbidden = new BitSet();

    public boolean isAllowedText(@NotNull String text) {
        return this.filterForAllowedChars(text).equals(text);
    }

    public boolean isAllowedChar(char c) {
        if (!this.allowed.isEmpty()) {
            return this.allowed.get(c);
        } else {
            return !this.forbidden.get(c);
        }
    }

    public boolean isAllowedChar(@NotNull String charAsString) {
        return (charAsString.length() < 1) || this.isAllowedChar(charAsString.charAt(0));
    }

    /**
     * Returns the text without the characters this filter does not allow.<br>
     * A text that only holds allowed characters gets returned as it is, without a copy.
     */
    @NotNull
    public String filterForAllowedChars(@NotNull String text) {
        int length = text.length();
        for (int i = 0; i < length; i++) {
            if (!this.isAllowedChar(text.charAt(i))) {
                // Everything in front of the first character that is not allowed stays, the rest gets checked one by one.
                // The result is built in one buffer, because adding to a String copies all of it for every single character.
                StringBuilder filtered = new StringBuilder(length - 1).append(text, 0, i);
                for (i++; i < length; i++) {
                    char c = text.charAt(i);
                    if (this.isAllowedChar(c)) filtered.append(c);
                }
                return filtered.toString();
            }
        }
        return text;
    }

    public void addAllowedCharacters(char... chars) {
        for (char c : chars) {
            this.allowed.set(c);
        }
    }

    public void addAllowedCharacters(String... chars) {
        for (String s : chars) {
            if (s != null && s.length() >= 1) {
                this.allowed.set(s.charAt(0));
            }
        }
    }

    public void addForbiddenCharacters(char... chars) {
        for (char c : chars) {
            this.forbidden.set(c);
        }
    }

    public void addForbiddenCharacters(String... chars) {
        for (String s : chars) {
            if (s != null && s.length() >= 1) {
                this.forbidden.set(s.charAt(0));
            }
        }
    }

    @Legacy("Converts the new CharacterFilter to the old one from Konkrete. Remove this in the future.")
    @NotNull
    public de.keksuccino.konkrete.input.CharacterFilter convertToLegacyFilter() {
        return new de.keksuccino.konkrete.input.CharacterFilter() {
            @Override
            public boolean isAllowed(char c) {
                return CharacterFilter.this.isAllowedChar(c);
            }
            @Override
            public boolean isAllowed(@NotNull String charString) {
                return CharacterFilter.this.isAllowedChar(charString);
            }
            @Override
            public void addAllowedCharacters(char... chars) {
                CharacterFilter.this.addAllowedCharacters(chars);
            }
            @Override
            public void addAllowedCharacters(@NotNull String... chars) {
                CharacterFilter.this.addAllowedCharacters(chars);
            }
            @Override
            public void addForbiddenCharacters(char... chars) {
                CharacterFilter.this.addForbiddenCharacters(chars);
            }
            @Override
            public void addForbiddenCharacters(@NotNull String... chars) {
                CharacterFilter.this.addForbiddenCharacters(chars);
            }
            @Override
            public String filterForAllowedChars(@NotNull String text) {
                return CharacterFilter.this.filterForAllowedChars(text);
            }
        };
    }

}
