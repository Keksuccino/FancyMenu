package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;

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
    void singleBackslashOnlyEscapesEscapableCharacters() {
        // \*a\* \b \
        assertEquals("*a* \\b \\", joinText(parse("\\*a\\* \\b \\")));
    }

}
