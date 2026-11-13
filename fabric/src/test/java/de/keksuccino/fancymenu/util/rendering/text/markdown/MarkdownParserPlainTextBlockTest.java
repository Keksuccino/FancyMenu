package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A plain text block is everything between two lines that are three semicolons. Whitespace behind the semicolons does not count.
 */
class MarkdownParserPlainTextBlockTest {

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", " \t ", "\r"})
    void markerLinesWithWhitespaceBehindThemOpenAndCloseTheBlock(String whitespace) {
        List<MarkdownTextFragment> fragments = parse("before\n;;;" + whitespace + "\n**plain** text\n;;;" + whitespace + "\nafter **bold** end");

        assertEquals(List.of("before", "**plain** ", "text", "after ", "bold ", "end"), texts(fragments));
        assertFalse(fragmentContaining(fragments, "before").plainText);
        assertTrue(fragmentContaining(fragments, "**plain**").plainText);
        assertTrue(fragmentContaining(fragments, "text").plainText);
        assertFalse(fragmentContaining(fragments, "after").plainText);
        assertTrue(fragmentContaining(fragments, "bold").bold);
    }

    @Test
    void markerLineWithWhitespaceBehindItClosesABlockThatAnExactMarkerLineOpened() {
        List<MarkdownTextFragment> fragments = parse(";;;\n**plain**\n;;;  \nafter");

        assertEquals(List.of("**plain**", "after"), texts(fragments));
        assertTrue(fragmentContaining(fragments, "**plain**").plainText);
        assertFalse(fragmentContaining(fragments, "after").plainText);
    }

    @Test
    void markerLineWithWhitespaceBehindItNeedsALineThatClosesItsBlock() {
        List<MarkdownTextFragment> fragments = parse(";;; \n**bold**");

        assertEquals(";;; bold", joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.plainText));
        assertTrue(fragmentContaining(fragments, "bold").bold);
    }

}
