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

/**
 * A single underscore opens and closes italic text. Two underscores open and close bold text like two asterisks do, but not inside of a word:
 * "__init__" is the bold word "init", and "my__var__name" stays as it is. Three underscores are both markers at once.
 */
class MarkdownParserUnderscoreTest {

    /**
     * The words say how they have to be formatted: "bold", "italic", "both", "struck" (bold and struck through) or "none".
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "__bold__ none", "none __bold__", "none __bold__ none", "__bold bold__ none", "(__bold__) none", "__bold__, none", "__bold__\nnone", "none\n__bold__",
            "__bold _both_ bold__ none", "_italic __both__ italic_ none", "___both___ none", "___both__ italic_ none", "___both_ bold__ none",
            "__bold _both___ none", "_italic __both___ none", "__bold *both* bold__ none", "*italic __both__ italic* none",
            "__bold ~struck~ bold__ none", "**bold __bold__ bold** none", "__bold **bold** bold__ none", "__bold__ _italic_ **bold** *italic* none",
            "§c__bold__§r none", "none §c__bold__ none", "__bold §cbold__ none"
    })
    void twoUnderscoresOpenAndCloseBoldText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals(text.replaceAll("§.", "").replaceAll("[_*~\n]", ""), joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) {
            String word = fragment.text.replaceAll("[ (),]", "");
            if (word.isEmpty()) continue;
            assertEquals(word.equals("bold") || word.equals("both") || word.equals("struck"), fragment.bold, () -> text + " <" + fragment.text + ">");
            assertEquals(word.equals("italic") || word.equals("both"), fragment.italic, () -> text + " <" + fragment.text + ">");
            assertEquals(word.equals("struck"), fragment.strikethrough, () -> text + " <" + fragment.text + ">");
        }
    }

    @Test
    void wordBetweenTwoPairsOfUnderscoresIsBold() {
        List<MarkdownTextFragment> fragments = parse("the __init__ and __str__ methods");

        assertEquals("the init and str methods", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "init").bold);
        assertTrue(fragmentContaining(fragments, "str").bold);
        assertFalse(fragmentContaining(fragments, "the").bold);
        assertFalse(fragmentContaining(fragments, "and").bold);
        assertFalse(fragmentContaining(fragments, "methods").bold);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Inside of a word
            "my__var__name", "a__b__c", "a__b__", "__a__b", "one__ two__",
            // Without a pair that can close
            "__open", "__open text", "__open __text", "open__ text__", "a __ b", "__ a __", "__", "a__",
            // Rows of underscores that are no markers
            "___", "____", "____bold____", "a___b", "_____",
            // A pair of asterisks closes no bold text that underscores opened
            "__open text**", "**open text__"
    })
    void underscoresThatAreNoMarkersStayText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals(text, joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.bold || fragment.italic));
    }

    @ParameterizedTest
    @ValueSource(strings = {"`__code__`", ";;__plain__;;", "```\n__code__\n```", ";;;\n__plain__\n;;;"})
    void twoUnderscoresInsideCodeAndPlainTextStayText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);
        MarkdownTextFragment inside = fragmentContaining(fragments, "__");

        assertFalse(inside.bold);
        assertTrue(inside.text.startsWith("__") && inside.text.endsWith("__"));
    }

    @Test
    void pairInsideCodeIsNoClosingPairForAPairInFrontOfIt() {
        List<MarkdownTextFragment> fragments = parse("__open `code__` text");

        assertEquals("__open code__ text", joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.bold));
    }

    @Test
    void pairInsideALinkTargetOrAFontNameIsNoMarker() {
        List<MarkdownTextFragment> link = parse("__open [label](https://example.invalid/__page__) text");
        List<MarkdownTextFragment> font = parse("__open %!!my__font%font%!!% text");

        assertEquals("__open label text", joinText(link));
        assertEquals("https://example.invalid/__page__", fragmentContaining(link, "label").hyperlink.link);
        assertTrue(link.stream().noneMatch(fragment -> fragment.bold));
        assertEquals("__open font text", joinText(font));
        assertNotNull(fragmentContaining(font, "font").font);
        assertTrue(font.stream().noneMatch(fragment -> fragment.bold));
    }

    @Test
    void boldTextCanHoldALinkAndCode() {
        List<MarkdownTextFragment> fragments = parse("__bold [label](target) and `code`__ rest");

        assertEquals("bold label and code rest", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "label").bold);
        assertNotNull(fragmentContaining(fragments, "label").hyperlink);
        assertTrue(fragmentContaining(fragments, "code").bold);
        assertFalse(fragmentContaining(fragments, "rest").bold);
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
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.italic || fragment.bold));
    }

    @Test
    void singleUnderscoresNextToBoldWordsStillWork() {
        List<MarkdownTextFragment> fragments = parse("__init__ and _italic_ and my__var__name");

        assertEquals("init and italic and my__var__name", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "italic").italic);
        assertFalse(fragmentContaining(fragments, "italic").bold);
        assertTrue(fragmentContaining(fragments, "init").bold);
        assertFalse(fragmentContaining(fragments, "init").italic);
        assertFalse(fragmentContaining(fragments, "my__var__name").bold);
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
