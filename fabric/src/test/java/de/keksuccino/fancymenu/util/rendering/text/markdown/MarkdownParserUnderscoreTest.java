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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A single underscore opens and closes italic text. Underscores in a row are text, like the ones of names such as "__init__".
 */
class MarkdownParserUnderscoreTest {

    @ParameterizedTest
    @ValueSource(strings = {"__init__", "the __init__ and __str__ methods", "a __ b", "__", "___", "____", "a__b__c", "__a", "a__"})
    void underscoresInARowStayText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals(text, joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.italic));
    }

    @Test
    void underscoresInARowCloseNoItalicText() {
        List<MarkdownTextFragment> fragments = parse("_start a__b end_ rest");

        assertEquals("start a__b end rest", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "start").italic);
        assertTrue(fragmentContaining(fragments, "a__b").italic);
        assertTrue(fragmentContaining(fragments, "end").italic);
        assertFalse(fragmentContaining(fragments, "rest").italic);
    }

    @Test
    void underscoresInARowAreNoClosingMarkerForASingleUnderscore() {
        List<MarkdownTextFragment> fragments = parse("_open a__ b");

        assertEquals("_open a__ b", joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.italic));
    }

    @Test
    void singleUnderscoresNextToUnderscoresInARowStillWork() {
        List<MarkdownTextFragment> fragments = parse("__init__ and _italic_ and __str__");

        assertEquals("__init__ and italic and __str__", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "italic").italic);
        assertFalse(fragmentContaining(fragments, "__init__").italic);
        assertFalse(fragmentContaining(fragments, "__str__").italic);
    }

    @Test
    void escapedUnderscoreFormsNoRowWithTheSingleUnderscoreBehindIt() {
        // \__italic_ rest
        List<MarkdownTextFragment> fragments = parse("\\__italic_ rest");

        assertEquals("_italic rest", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "italic").italic);
        assertFalse(fragmentContaining(fragments, "rest").italic);
    }

}
