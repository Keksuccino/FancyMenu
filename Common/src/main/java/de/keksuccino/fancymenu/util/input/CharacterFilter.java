package de.keksuccino.fancymenu.util.input;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.util.Legacy;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CharacterFilter {

    @NotNull
    public static CharacterFilter buildDoubleCharacterFiler() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "-", "+");
        return f;
    }

    @NotNull
    public static CharacterFilter buildIntegerCharacterFiler() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "-", "+");
        return f;
    }

    @NotNull
    public static CharacterFilter buildBasicFilenameCharacterFilter() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "_", "-");
        return f;
    }

    @NotNull
    public static CharacterFilter buildFilenameFilterWithUppercaseSupport() {
        CharacterFilter f = buildBasicFilenameCharacterFilter();
        f.addAllowedCharacters("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");
        return f;
    }

    @NotNull
    public static CharacterFilter buildUrlCharacterFilter() {
        CharacterFilter f = new CharacterFilter();
        f.addAllowedCharacters("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", ".", "-", "_", "~", "+", "#", ",", "%", "&", "=", "*", ";", ":", "@", "?", "/", "\\");
        return f;
    }

    private final List<Character> allowed = new ArrayList<>();
    private final List<Character> forbidden = new ArrayList<>();

    public boolean isAllowedText(@NotNull String text) {
        return this.filterForAllowedChars(text).equals(text);
    }

    public boolean isAllowedChar(char c) {
        if (!this.allowed.isEmpty()) {
            return this.allowed.contains(c);
        } else {
            return !this.forbidden.contains(c);
        }
    }

    public boolean isAllowedChar(@NotNull String charAsString) {
        return (charAsString.length() < 1) || this.isAllowedChar(charAsString.charAt(0));
    }

    @NotNull
    public String filterForAllowedChars(@NotNull String text) {
        String s = "";
        for (int i = 0; i < text.length(); ++i) {
            if (this.isAllowedChar(text.charAt(i))) {
                s = s + text.charAt(i);
            }
        }
        return s;
    }

    public void addAllowedCharacters(char... chars) {
        for (char c : chars) {
            if (!this.allowed.contains(c)) {
                this.allowed.add(c);
            }
        }
    }

    public void addAllowedCharacters(String... chars) {
        for (String s : chars) {
            if (s != null && s.length() >= 1 && !this.allowed.contains(s.charAt(0))) {
                this.allowed.add(s.charAt(0));
            }
        }
    }

    public void addForbiddenCharacters(char... chars) {
        for (char c : chars) {
            if (!this.forbidden.contains(c)) {
                this.forbidden.add(c);
            }
        }
    }

    public void addForbiddenCharacters(String... chars) {
        for (String s : chars) {
            if (s != null && s.length() >= 1 && !this.forbidden.contains(s.charAt(0))) {
                this.forbidden.add(s.charAt(0));
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
