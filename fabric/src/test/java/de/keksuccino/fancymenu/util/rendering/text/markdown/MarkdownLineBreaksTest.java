package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A line may break between the characters of scripts that put no spaces between words, with the exceptions these scripts make.
 * All other text only breaks at its spaces, which is nothing {@link MarkdownLineBreaks} deals with.
 */
class MarkdownLineBreaksTest {

    @ParameterizedTest
    @ValueSource(strings = {"日本語", "简体中文", "繁體中文", "ひらがな", "カタカナ", "ㄅㄆㄇ", "ＡＢＣ", "ｱｲｳ", "漢字かなカナ", "㐀㐁", "豈更"})
    void lineMayBreakBetweenAnyTwoCharactersOfAScriptWithoutSpaces(String text) {
        assertEquals(breaksBetweenAllCharacters(text), breaks(text));
        assertTrue(MarkdownLineBreaks.hasBreak(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"word", "Wörter", "слово", "λέξη", "12345", "a-b_c.d", "한국어", "한국어문장입니다", "ㄱㄴㄷ", "ﾡﾢﾣ", "a", ""})
    void lineDoesNotBreakInsideOfTextOfAScriptWithSpaces(String text) {
        assertEquals(List.of(), breaks(text));
        assertFalse(MarkdownLineBreaks.hasBreak(text));
    }

    @Test
    void lineMayBreakBetweenACharacterOfAScriptWithoutSpacesAndAnyOtherCharacter() {
        assertEquals(List.of(9, 10), breaks("Minecraftの世"));
        assertEquals(List.of(1, 2), breaks("日本Minecraft"));
        assertEquals(List.of(1, 5), breaks("約1000個"));
        assertEquals(List.of(3, 4), breaks("한국어日本"));
    }

    @ParameterizedTest
    @ValueSource(chars = {'、', '。', '，', '．', '！', '？', '：', '；', '」', '』', '）', '】', '〕', '］', '｝', '〉', '》', '・', 'ー', '々', 'っ', 'ゃ', 'ゅ', 'ょ', 'ぁ', 'ッ', 'ャ', 'ュ', 'ョ', 'ァ', 'ヶ', 'ゝ', 'ヽ', '％', ')', ']', ',', '.', '!', '?', '%', '”', '’'})
    void characterThatMustNotStartALineStaysBehindTheCharacterInFrontOfIt(char character) {
        String text = "日" + character + "本";

        assertFalse(MarkdownLineBreaks.isAllowed(text, 1));
        assertTrue(MarkdownLineBreaks.isAllowed(text, 2));
        assertFalse(MarkdownLineBreaks.isAllowedBetween("日", character + "本"));
    }

    @ParameterizedTest
    @ValueSource(chars = {'「', '『', '（', '【', '〔', '［', '｛', '〈', '《', '(', '[', '{', '“', '‘', '￥', '$'})
    void characterThatMustNotEndALineStaysInFrontOfTheCharacterBehindIt(char character) {
        String text = "日" + character + "本";

        assertTrue(MarkdownLineBreaks.isAllowed(text, 1));
        assertFalse(MarkdownLineBreaks.isAllowed(text, 2));
        assertFalse(MarkdownLineBreaks.isAllowedBetween("日" + character, "本"));
    }

    @Test
    void quoteThatCanOpenAndCloseStaysWithBothOfItsNeighbors() {
        assertEquals(List.of(), breaks("日\"本"));
        assertEquals(List.of(), breaks("日'本"));
    }

    @Test
    void rowsOfClosingAndOpeningCharactersStayTogether() {
        assertEquals(List.of(1, 2, 8), breaks("日本「（語）」。次"));
    }

    @Test
    void twoHalvesOfACharacterOutsideTheBasicMultilingualPlaneStayTogether() {
        String ideograph = new String(Character.toChars(0x2000B));

        assertEquals(List.of(2, 4), breaks(ideograph + ideograph + "日"));
        assertEquals(List.of(1, 3), breaks("日" + ideograph + "本"));
        assertEquals(List.of(3), breaks("abc" + ideograph));
    }

    @Test
    void combiningMarkStaysBehindItsCharacter() {
        // か and a combining voiced sound mark
        assertEquals(List.of(2), breaks("か\u3099き"));
    }

    @Test
    void lineBreaksBehindAnIdeographicSpaceButNotInFrontOfIt() {
        assertEquals(List.of(2), breaks("日\u3000本"));
        assertEquals(List.of(4), breaks("abc\u3000def"));
    }

    @Test
    void lineDoesNotBreakNextToOtherSpaces() {
        assertEquals(List.of(), breaks("日 本"));
        assertEquals(List.of(), breaks("日\u00A0本"));
        assertEquals(List.of(), breaks("日\t本"));
    }

    @Test
    void dotsAndDashesThatAreNoPartOfTheseScriptsStayTogether() {
        assertEquals(List.of(1, 3), breaks("日……本"));
        assertEquals(List.of(1, 3), breaks("日——本"));
    }

    @Test
    void breakBetweenTwoTextsFollowsTheSameRules() {
        assertTrue(MarkdownLineBreaks.isAllowedBetween("日本", "語"));
        assertTrue(MarkdownLineBreaks.isAllowedBetween("word", "語"));
        assertTrue(MarkdownLineBreaks.isAllowedBetween("語", "word"));
        assertFalse(MarkdownLineBreaks.isAllowedBetween("word", "word"));
        assertFalse(MarkdownLineBreaks.isAllowedBetween("한국", "어"));
        assertFalse(MarkdownLineBreaks.isAllowedBetween("日本 ", "語"));
        assertFalse(MarkdownLineBreaks.isAllowedBetween("", "語"));
        assertFalse(MarkdownLineBreaks.isAllowedBetween("語", ""));
        assertTrue(MarkdownLineBreaks.isAllowedBetween(new String(Character.toChars(0x2000B)), new String(Character.toChars(0x2000B))));
    }

    @Test
    void nextAndPreviousBreakStayInsideTheirRange() {
        String text = "abc日本語def";

        assertEquals(3, MarkdownLineBreaks.next(text, 0, text.length()));
        assertEquals(3, MarkdownLineBreaks.next(text, 3, text.length()));
        assertEquals(4, MarkdownLineBreaks.next(text, 4, text.length()));
        assertEquals(6, MarkdownLineBreaks.next(text, 6, text.length()));
        assertEquals(-1, MarkdownLineBreaks.next(text, 7, text.length()));
        assertEquals(-1, MarkdownLineBreaks.next(text, 4, 4));
        assertEquals(-1, MarkdownLineBreaks.next(text, 0, 3));
        assertEquals(6, MarkdownLineBreaks.previous(text, text.length(), 0));
        assertEquals(6, MarkdownLineBreaks.previous(text, 8, 0));
        assertEquals(5, MarkdownLineBreaks.previous(text, 5, 0));
        assertEquals(3, MarkdownLineBreaks.previous(text, 3, 0));
        assertEquals(-1, MarkdownLineBreaks.previous(text, 2, 0));
        assertEquals(-1, MarkdownLineBreaks.previous(text, 3, 3));
        assertEquals(4, MarkdownLineBreaks.previous(text, 4, 3));
    }

    private static List<Integer> breaks(String text) {
        List<Integer> breaks = new ArrayList<>();
        for (int index = 1; index < text.length(); index++) {
            if (MarkdownLineBreaks.isAllowed(text, index)) breaks.add(index);
        }
        return breaks;
    }

    private static List<Integer> breaksBetweenAllCharacters(String text) {
        List<Integer> breaks = new ArrayList<>();
        for (int index = 1; index < text.length(); index++) breaks.add(index);
        return breaks;
    }

}
