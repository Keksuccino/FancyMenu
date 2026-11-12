package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownParserStrikethroughTest {

    @ParameterizedTest
    @ValueSource(strings = {"@text@", "before @two words@ after", "@first\nsecond@", "@a@ plain @b@", "**@bold@**", "# @headline@\n- @item@", "| @cell@ | b |\n| --- | --- |"})
    void doubleMarkersParseExactlyLikeSingleMarkers(String template) {
        // Every @ of the template is one strikethrough marker
        List<MarkdownTextFragment> doubleMarkers = parse(template.replace("@", "~~"));
        List<MarkdownTextFragment> singleMarkers = parse(template.replace("@", "~"));

        assertSameFragments(singleMarkers, doubleMarkers);
    }

    @Test
    void doubleMarkersStrikeTheTextBetweenThem() {
        List<MarkdownTextFragment> fragments = parse("keep ~~two words~~ keep");

        assertTrue(fragmentContaining(fragments, "two").strikethrough);
        assertTrue(fragmentContaining(fragments, "words").strikethrough);
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.text.contains("keep")) assertFalse(fragment.strikethrough);
        }
        assertEquals("keep two words keep", joinText(fragments));
    }

    @Test
    void doubleMarkersStrikeAtTheStartAndTheEndOfTheText() {
        List<MarkdownTextFragment> fragments = parse("~~first~~ middle ~~last~~");

        assertTrue(fragmentContaining(fragments, "first").strikethrough);
        assertFalse(fragmentContaining(fragments, "middle").strikethrough);
        assertTrue(fragmentContaining(fragments, "last").strikethrough);
        assertEquals("first middle last", joinText(fragments));
    }

    @Test
    void singleAndDoubleMarkersWorkInTheSameText() {
        List<MarkdownTextFragment> fragments = parse("~a~ ~~b~~ ~c~ d");

        assertTrue(fragmentContaining(fragments, "a").strikethrough);
        assertTrue(fragmentContaining(fragments, "b").strikethrough);
        assertTrue(fragmentContaining(fragments, "c").strikethrough);
        assertFalse(fragmentContaining(fragments, "d").strikethrough);
        assertEquals("a b c d", joinText(fragments));
    }

    @Test
    void singleTildeInsideDoubleMarkersStaysText() {
        List<MarkdownTextFragment> fragments = parse("~~a ~ b~~ c");

        assertTrue(fragmentContaining(fragments, "a").strikethrough);
        assertTrue(fragmentContaining(fragments, "~").strikethrough);
        assertTrue(fragmentContaining(fragments, "b").strikethrough);
        assertFalse(fragmentContaining(fragments, "c").strikethrough);
        assertEquals("a ~ b c", joinText(fragments));
    }

    @ParameterizedTest
    @ValueSource(strings = {"~~open", "~~open~", "open~~", "~~", "a ~~ b"})
    void doubleMarkerWithoutASecondOneStrikesNoWord(String text) {
        for (MarkdownTextFragment fragment : parse(text)) {
            assertFalse(fragment.strikethrough && !fragment.text.isBlank());
        }
    }

    @Test
    void singleMarkerStillClosesAtTheFirstTildeOfADoubleMarker() {
        List<MarkdownTextFragment> fragments = parse("~struck~~ plain");

        assertTrue(fragmentContaining(fragments, "struck").strikethrough);
        assertFalse(fragmentContaining(fragments, "plain").strikethrough);
    }

    @Test
    void escapedTildesFormNoDoubleMarker() {
        List<MarkdownTextFragment> fragments = parse("\\~\\~text\\~\\~");

        for (MarkdownTextFragment fragment : fragments) assertFalse(fragment.strikethrough);
        assertEquals("~~text~~", joinText(fragments));
    }

    @ParameterizedTest
    @ValueSource(strings = {"`~~text~~`", ";;~~text~~;;", "```\n~~text~~\n```", ";;;\n~~text~~\n;;;"})
    void doubleMarkersInsideCodeAndPlainTextStayText(String text) {
        MarkdownTextFragment fragment = fragmentContaining(parse(text), "text");

        assertFalse(fragment.strikethrough);
        assertEquals("~~text~~", fragment.text);
        assertTrue(fragment.plainText || (fragment.codeBlockContext != null));
    }

    @Test
    void doubleMarkersKeepTheOtherFormattingOfTheirText() {
        List<MarkdownTextFragment> fragments = parse("**~~[link](https://example.invalid)~~**");
        MarkdownTextFragment link = fragmentContaining(fragments, "link");

        assertTrue(link.strikethrough);
        assertTrue(link.bold);
        assertNotNull(link.hyperlink);
        assertEquals("link", joinText(fragments));
    }

    private static void assertSameFragments(List<MarkdownTextFragment> expected, List<MarkdownTextFragment> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            MarkdownTextFragment expectedFragment = expected.get(i);
            MarkdownTextFragment actualFragment = actual.get(i);
            assertEquals(expectedFragment.text, actualFragment.text);
            assertEquals(expectedFragment.strikethrough, actualFragment.strikethrough);
            assertEquals(expectedFragment.bold, actualFragment.bold);
            assertEquals(expectedFragment.headlineType, actualFragment.headlineType);
            assertEquals(expectedFragment.bulletListLevel, actualFragment.bulletListLevel);
            assertEquals(expectedFragment.naturalLineBreakAfter, actualFragment.naturalLineBreakAfter);
            assertEquals(expectedFragment.endOfWord, actualFragment.endOfWord);
            assertEquals(expectedFragment.unscaledTextWidth, actualFragment.unscaledTextWidth);
            assertEquals(expectedFragment.isTable(), actualFragment.isTable());
            if (expectedFragment.isTable()) {
                List<MarkdownTextFragment> expectedCell = expectedFragment.tableContext.rows.get(0).cells.get(0).fragments;
                assertSameFragments(expectedCell, actualFragment.tableContext.rows.get(0).cells.get(0).fragments);
                assertTrue(fragmentContaining(expectedCell, "cell").strikethrough);
            }
        }
    }

}
