package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * Tells where a line may break inside of text that has no space to break at.
 *
 * <p>Chinese and Japanese put no spaces between words, so a line may break between any two characters of their scripts: Han ideographs,
 * Hiragana, Katakana, Bopomofo and Yi, the punctuation and symbols made for them, and the full-width and half-width forms.
 * It may break between one of them and any other character as well, like a Latin letter or a digit.
 * Korean puts spaces between words, so Hangul is no such character and Korean text only breaks at its spaces.</p>
 *
 * <p>Some characters still stay together with a neighbor:</p>
 * <ul>
 *     <li>A character that must not start a line stays behind the character in front of it: closing brackets and quotes, commas, full stops
 *     and other marks that end something, small Kana, the marks that prolong or repeat a sound, signs behind a number, and combining marks.</li>
 *     <li>A character that must not end a line stays in front of the character behind it: opening brackets and quotes and signs in front of a number.</li>
 * </ul>
 *
 * <p>An ideographic space works like every other space: a line may break behind it, but not in front of it.</p>
 *
 * <p>An index of a break is the index of the character the next line would start with.</p>
 */
final class MarkdownLineBreaks {

    /** Every character in front of this one belongs to a script that only breaks at spaces. */
    private static final char FIRST_SPACELESS_CHARACTER = '\u2E80';
    private static final char IDEOGRAPHIC_SPACE = '\u3000';
    private static final char ZERO_WIDTH_JOINER = '\u200D';

    private static final char[] NO_LINE_START = sorted(
            // Closing brackets and quotes
            ")]}»’”›〉》」』】〕〗〙〛〞〟︶︸︺︼︾﹀﹂﹄﹚﹜﹞）］｝｠｣"
            // Commas, full stops and other marks that end or separate something
            + ",.:;!?\u00B7\u2027‼⁇⁈⁉、。\u30FB﹐﹑﹒﹔﹕﹖﹗！，．：；？｡､\uFF65"
            // Hyphens and wave dashes
            + "\u2010\u2013\u301C\u30A0\uFF5E"
            // Small Hiragana and Katakana
            + "ぁぃぅぇぉっゃゅょゎゕゖァィゥェォッャュョヮヵヶ"
            + "ㇰㇱㇲㇳㇴㇵㇶㇷㇸㇹㇺㇻㇼㇽㇾㇿｧｨｩｪｫｬｭｮｯ"
            // Marks that prolong, repeat or voice a sound
            + "々〻゛゜ゝゞ\u30FCヽヾ\uFF70ﾞﾟ"
            // Signs behind a number
            + "%¢°‰\u2032\u2033℃％￠"
            // Quotes that can open and close
            + "\"'");

    private static final char[] NO_LINE_END = sorted(
            // Opening brackets and quotes
            "([{«‘“‹〈《「『【〔〖〘〚〝︵︷︹︻︽︿﹁﹃﹙﹛﹝（［｛｟｢"
            // Signs in front of a number
            + "$£¥€＄￡￥"
            // Quotes that can open and close
            + "\"'");

    private MarkdownLineBreaks() {
    }

    @NotNull
    private static char[] sorted(@NotNull String characters) {
        char[] sorted = characters.toCharArray();
        Arrays.sort(sorted);
        return sorted;
    }

    /**
     * Returns if a line may break somewhere inside of the text.
     */
    static boolean hasBreak(@NotNull String text) {
        return next(text, 1, text.length()) != -1;
    }

    /**
     * Returns the first index a line may break at that is at or behind the given index and in front of the end index, or -1 if there is none.
     */
    static int next(@NotNull String text, int fromIndex, int endIndex) {
        for (int index = Math.max(1, fromIndex); index < endIndex; index++) {
            if (isAllowed(text, index)) return index;
        }
        return -1;
    }

    /**
     * Returns the last index a line may break at that is at or in front of the given index and behind the start index, or -1 if there is none.
     */
    static int previous(@NotNull String text, int fromIndex, int startIndex) {
        for (int index = Math.min(text.length() - 1, fromIndex); index > startIndex; index--) {
            if (isAllowed(text, index)) return index;
        }
        return -1;
    }

    /**
     * Returns if a line may break in front of the character at the given index, which has to be behind the first character of the text.
     */
    static boolean isAllowed(@NotNull String text, int index) {
        char before = text.charAt(index - 1);
        char after = text.charAt(index);
        // Most text is of scripts that only break at spaces
        if ((before < FIRST_SPACELESS_CHARACTER) && (after < FIRST_SPACELESS_CHARACTER)) return false;
        // The two halves of a character outside the Basic Multilingual Plane stay together
        if (Character.isLowSurrogate(after)) {
            if (Character.isHighSurrogate(before)) return false;
            return isAllowedBetween(text.codePointBefore(index), after);
        }
        return isAllowedBetween(text.codePointBefore(index), text.codePointAt(index));
    }

    /**
     * Returns if a line may break between the last character of the first text and the first character of the second one.
     */
    static boolean isAllowedBetween(@NotNull String before, @NotNull String after) {
        if (before.isEmpty() || after.isEmpty()) return false;
        if ((before.charAt(before.length() - 1) < FIRST_SPACELESS_CHARACTER) && (after.charAt(0) < FIRST_SPACELESS_CHARACTER)) return false;
        return isAllowedBetween(before.codePointBefore(before.length()), after.codePointAt(0));
    }

    /**
     * Returns if a line may break between the two characters, which are code points.
     */
    static boolean isAllowedBetween(int before, int after) {
        if (Character.isWhitespace(after) || Character.isSpaceChar(after)) return false;
        if (before != IDEOGRAPHIC_SPACE) {
            if (Character.isWhitespace(before) || Character.isSpaceChar(before)) return false;
            if (!isSpaceless(before) && !isSpaceless(after)) return false;
        }
        if (isIn(NO_LINE_END, before) || isIn(NO_LINE_START, after)) return false;
        return (before != ZERO_WIDTH_JOINER) && (after != ZERO_WIDTH_JOINER) && !isCombiningMark(after);
    }

    /**
     * Returns if the character belongs to a script that puts no spaces between words, or to the punctuation, symbols and forms made for such scripts.
     */
    static boolean isSpaceless(int character) {
        if (character < FIRST_SPACELESS_CHARACTER) return false;
        // CJK radicals, ideographic description characters, CJK symbols and punctuation, Hiragana, Katakana, Bopomofo
        if (character <= 0x312F) return true;
        // Hangul compatibility Jamo
        if (character < 0x3190) return false;
        // Kanbun, extended Bopomofo, CJK strokes, Katakana phonetic extensions, enclosed CJK letters, CJK compatibility, Han ideographs, Yi
        if (character <= 0xA4CF) return true;
        // Scripts that break at spaces, Hangul syllables, surrogates and private use characters
        if (character < 0xF900) return false;
        // CJK compatibility ideographs
        if (character <= 0xFAFF) return true;
        if (character < 0xFE30) return false;
        // CJK compatibility forms and small form variants
        if (character <= 0xFE6F) return true;
        if (character < 0xFF00) return false;
        // Half-width and full-width forms, without the half-width Hangul
        if (character <= 0xFFEF) return (character < 0xFFA0) || (character > 0xFFDC);
        // Kana of the supplementary planes
        if ((character >= 0x1AFF0) && (character <= 0x1B16F)) return true;
        // Han ideographs of the supplementary planes
        return (character >= 0x20000) && (character <= 0x3FFFD);
    }

    private static boolean isCombiningMark(int character) {
        int type = Character.getType(character);
        return (type == Character.NON_SPACING_MARK) || (type == Character.ENCLOSING_MARK) || (type == Character.COMBINING_SPACING_MARK);
    }

    private static boolean isIn(@NotNull char[] sortedCharacters, int character) {
        return (character <= Character.MAX_VALUE) && (Arrays.binarySearch(sortedCharacters, (char) character) >= 0);
    }

}
