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
 * A single underscore opens and closes italic text and two underscores open and close bold text like asterisks do, but not inside of a word:
 * "_init_" is the italic and "__init__" the bold word "init", while "my_var_name" and "my__var__name" stay as they are. Three underscores are both markers at once.
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

    /**
     * The words say how they have to be formatted: "italic" or "none".
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "_italic_ none", "none _italic_", "none _italic_ none", "_italic italic_ none", "(_italic_) none", "\"_italic_\" none", "_italic_, none", "none _italic_.",
            "_italic_\nnone", "none\n_italic_", "_italic_ none _italic_", "§c_italic_§r none", "none §c_italic_ none", "_italic §citalic_ none", "none/_italic_/none"
    })
    void singleUnderscoresAroundWordsOpenAndCloseItalicText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals(text.replaceAll("§.", "").replaceAll("[_\n]", ""), joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) {
            String word = fragment.text.replaceAll("[ ()\",./]", "");
            if (word.isEmpty()) continue;
            assertEquals(word.equals("italic"), fragment.italic, () -> text + " <" + fragment.text + ">");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Inside of a word
            "snake_case_name", "config/my_mod/some_file.json", "2_000_000", "my_var and other_var", "a_b_c d_e_f", "日本_語_です", "_a_b", "a_b_",
            // Without an underscore that can open, or without a later one that can close
            "open_ text_", "_open _text", "a _ b _ c", "_ a _", "before _ inside_ after", "before_ inside_ after", "_open", "open_", "_", "a_ _b"
    })
    void singleUnderscoresThatAreNoMarkersStayText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals(text, joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.bold || fragment.italic));
    }

    @Test
    void underscoreInsideOfAWordClosesNoItalicText() {
        List<MarkdownTextFragment> closed = parse("_italic snake_case italic_ rest");
        List<MarkdownTextFragment> open = parse("_open snake_case rest");

        assertEquals("italic snake_case italic rest", joinText(closed));
        assertTrue(fragmentContaining(closed, "snake_case").italic);
        assertTrue(closed.get(closed.size() - 2).italic);
        assertFalse(fragmentContaining(closed, "rest").italic);
        assertEquals("_open snake_case rest", joinText(open));
        assertTrue(open.stream().noneMatch(fragment -> fragment.italic));
    }

    @Test
    void italicWordNextToWordsWithUnderscores() {
        List<MarkdownTextFragment> fragments = parse("set max_stack_size in _your_ config/my_mod/some_file.json to 2_000_000");

        assertEquals("set max_stack_size in your config/my_mod/some_file.json to 2_000_000", joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) {
            assertEquals(fragment.text.trim().equals("your"), fragment.italic, () -> "<" + fragment.text + ">");
        }
    }

    @Test
    void formattingCodeNextToAnUnderscoreInsideOfAWordMakesItNoMarker() {
        List<MarkdownTextFragment> fragments = parse("snake§c_case_§rname");

        assertEquals("snake_case_name", joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.italic));
    }

    @Test
    void thirdUnderscoreOfARowOnlyDoesWhatTheRowCanDo() {
        // The row can open but not close, so its last underscore is no end of the italic text in front of it
        List<MarkdownTextFragment> fragments = parse("_open ___text");

        assertEquals("_open ___text", joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.bold || fragment.italic));
    }

    @Test
    void asterisksInsideOfWordsStillOpenAndCloseItalicAndBoldText() {
        List<MarkdownTextFragment> italic = parse("snake*case*name");
        List<MarkdownTextFragment> bold = parse("snake**case**name");

        assertEquals("snakecasename", joinText(italic));
        assertTrue(fragmentContaining(italic, "case").italic);
        assertFalse(fragmentContaining(italic, "snake").italic);
        assertFalse(fragmentContaining(italic, "name").italic);
        assertEquals("snakecasename", joinText(bold));
        assertTrue(fragmentContaining(bold, "case").bold);
        assertFalse(fragmentContaining(bold, "snake").bold);
        assertFalse(fragmentContaining(bold, "name").bold);
    }

}
