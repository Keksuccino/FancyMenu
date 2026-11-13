package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.tableFragment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The comments show the Markdown source without the escaping of the Java strings.
 */
class MarkdownParserEscapeTest {

    @Test
    void escapedBackslashStaysInFrontOfPunctuation() {
        // C:\\Users\\.minecraft\\-data\\(1\\)
        assertEquals("C:\\Users\\.minecraft\\-data\\(1\\)", joinText(parse("C:\\\\Users\\\\.minecraft\\\\-data\\\\(1\\\\)")));
    }

    @Test
    void escapedBackslashStaysInFrontOfAMarkerThatOpensNothing() {
        // a\\*b
        List<MarkdownTextFragment> fragments = parse("a\\\\*b");

        assertEquals("a\\*b", joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) assertFalse(fragment.italic);
    }

    @Test
    void everyPairOfBackslashesBecomesOneBackslash() {
        // \\\\ and \\\\\\
        assertEquals("\\\\", joinText(parse("\\\\\\\\")));
        assertEquals("\\\\\\", joinText(parse("\\\\\\\\\\\\")));
    }

    @Test
    void escapedBackslashAndEscapedMarkerBothStay() {
        // \\\*x*
        List<MarkdownTextFragment> fragments = parse("\\\\\\*x*");

        assertEquals("\\*x*", joinText(fragments));
        assertFalse(fragmentContaining(fragments, "x").italic);
    }

    @Test
    void escapedBackslashLeavesTheMarkerBehindItActive() {
        // \\*x* and \\\\*y*
        List<MarkdownTextFragment> oneBackslash = parse("\\\\*x*");
        List<MarkdownTextFragment> twoBackslashes = parse("\\\\\\\\*y*");

        assertEquals("\\x", joinText(oneBackslash));
        assertTrue(fragmentContaining(oneBackslash, "x").italic);
        assertEquals("\\\\y", joinText(twoBackslashes));
        assertTrue(fragmentContaining(twoBackslashes, "y").italic);
    }

    @Test
    void escapedBackslashStaysInsideATableCell() {
        // | a\\.b | c |
        MarkdownTextFragment table = tableFragment(parse("| a\\\\.b | c |\n| --- | --- |"));

        assertEquals("a\\.b", joinText(table.tableContext.rows.get(0).cells.get(0).fragments));
    }

    @Test
    void escapedBoldMarkerClosesNoBoldText() {
        // **open \** text
        List<MarkdownTextFragment> fragments = parse("**open \\** text");

        assertEquals("*open * text", joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) assertFalse(fragment.bold);
        // The second asterisk of the marker and the one behind the escaped one are single markers, which is how CommonMark reads them too
        assertTrue(fragmentContaining(fragments, "open").italic);
        assertFalse(fragmentContaining(fragments, "text").italic);
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "_", "~", "~~"})
    void escapedMarkerClosesNothing(String marker) {
        // *open \* text
        String escapedMarker = "\\" + marker;
        List<MarkdownTextFragment> fragments = parse(marker + "open " + escapedMarker + " text");

        assertEquals(marker + "open " + marker + " text", joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) {
            assertFalse(fragment.bold);
            assertFalse(fragment.italic);
            assertFalse(fragment.strikethrough);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "_", "~", "~~"})
    void markerBehindAnEscapedOneStillCloses(String marker) {
        // *closed \* here* text
        String escapedMarker = "\\" + marker;
        List<MarkdownTextFragment> fragments = parse(marker + "closed " + escapedMarker + " here" + marker + " text");

        assertEquals("closed " + marker + " here text", joinText(fragments));
        for (String word : List.of("closed", "here")) {
            MarkdownTextFragment fragment = fragmentContaining(fragments, word);
            assertTrue(fragment.bold || fragment.italic || fragment.strikethrough);
        }
        MarkdownTextFragment text = fragmentContaining(fragments, "text");
        assertFalse(text.bold || text.italic || text.strikethrough);
    }

    @Test
    void markerBehindAnEscapedBackslashStillCloses() {
        // **bold \\** text
        List<MarkdownTextFragment> fragments = parse("**bold \\\\** text");

        assertEquals("bold \\ text", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "bold").bold);
        assertFalse(fragmentContaining(fragments, "text").bold);
    }

    @Test
    void boldMarkerRightBehindAnEscapedAsteriskStillCloses() {
        // **bold \*** text
        List<MarkdownTextFragment> fragments = parse("**bold \\*** text");

        assertEquals("bold * text", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "bold").bold);
        assertTrue(fragmentContaining(fragments, "*").bold);
        assertFalse(fragmentContaining(fragments, "text").bold);
    }

    @Test
    void singleBackslashOnlyEscapesEscapableCharacters() {
        // \*a\* \b \
        assertEquals("*a* \\b \\", joinText(parse("\\*a\\* \\b \\")));
    }

}
