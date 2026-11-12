package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.QuoteContext;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownParserQuoteTest {

    @Test
    void prefixedLinesContinueTheQuoteWithoutShowingTheirPrefix() {
        List<MarkdownTextFragment> fragments = parse("> one\n> two\n> three");

        assertEquals(List.of("one", "two", "three"), texts(fragments));
        assertOneQuote(fragments);
    }

    @Test
    void linesWithAndWithoutPrefixContinueTheSameQuote() {
        List<MarkdownTextFragment> fragments = parse("> one\ntwo\n> three\nfour");

        assertEquals(List.of("one", "two", "three", "four"), texts(fragments));
        assertOneQuote(fragments);
    }

    @ParameterizedTest
    @ValueSource(strings = {"> one\n>\n> two", "> one\n> \n> two", "> one\n>\ntwo"})
    void lineWithOnlyTheQuoteCharacterIsAnEmptyLineOfTheQuote(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals(List.of("one", "", "two"), texts(fragments));
        assertOneQuote(fragments);
        assertTrue(fragments.get(1).naturalLineBreakAfter);
    }

    @ParameterizedTest
    @ValueSource(strings = {"> one\n>", "> one\n> "})
    void quoteCanEndWithAnEmptyLine(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals(List.of("one", ""), texts(fragments));
        assertOneQuote(fragments);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "  \t "})
    void blankLineEndsTheQuoteAndTheNextPrefixOpensANewOne(String blankLine) {
        List<MarkdownTextFragment> fragments = parse("> one\n> two\n" + blankLine + "\n> three\n> four\n" + blankLine + "\nplain");
        QuoteContext first = fragmentContaining(fragments, "one").quoteContext;
        QuoteContext second = fragmentContaining(fragments, "three").quoteContext;

        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second);
        assertEquals(List.of("one", "two"), texts(first.quoteFragments));
        assertEquals(List.of("three", "four"), texts(second.quoteFragments));
        assertNull(fragmentContaining(fragments, "plain").quoteContext);
        assertEquals("onetwothreefourplain", joinText(fragments).replace(" ", "").replace("\t", ""));
    }

    @Test
    void prefixedLinesKeepTheirInlineFormatting() {
        List<MarkdownTextFragment> fragments = parse("> **a**\n> _b_ [c](https://example.invalid)\n> ~~d~~");
        QuoteContext quote = fragmentContaining(fragments, "a").quoteContext;

        assertNotNull(quote);
        assertTrue(fragmentContaining(fragments, "a").bold);
        assertTrue(fragmentContaining(fragments, "b").italic);
        assertEquals("https://example.invalid", fragmentContaining(fragments, "c").hyperlink.link);
        assertTrue(fragmentContaining(fragments, "d").strikethrough);
        for (MarkdownTextFragment fragment : fragments) assertSame(quote, fragment.quoteContext);
        assertEquals("ab cd", joinText(fragments));
    }

    @Test
    void formattingCodesInFrontOfThePrefixKeepItWorking() {
        List<MarkdownTextFragment> fragments = parse("> one\n§c> two\n§c§l>\n&a> three");

        assertEquals(List.of("one", "two", "", "three"), texts(fragments));
        assertOneQuote(fragments);
    }

    @Test
    void quoteCharacterInsideALineStaysText() {
        List<MarkdownTextFragment> fragments = parse("> one > two\n> three > four\nfive > six >");

        assertEquals("one > twothree > fourfive > six >", joinText(fragments));
        assertOneQuote(fragments);
    }

    @Test
    void quoteCharacterWithoutSpaceStaysTextOfTheQuote() {
        List<MarkdownTextFragment> fragments = parse("> one\n>two\n>> three");

        assertEquals(List.of("one", ">two", ">> ", "three"), texts(fragments));
        assertOneQuote(fragments);
    }

    @ParameterizedTest
    @ValueSource(strings = {">", ">\n> quote", "plain\n>\nplain", ">\n>"})
    void lineWithOnlyTheQuoteCharacterStaysTextOutsideAQuote(String text) {
        List<MarkdownTextFragment> quoteCharacters = parse(text).stream().filter(fragment -> fragment.text.equals(">")).toList();

        assertFalse(quoteCharacters.isEmpty());
        for (MarkdownTextFragment fragment : quoteCharacters) assertNull(fragment.quoteContext);
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", ";;;"})
    void prefixInsideABlockOfAQuoteStaysText(String blockMarker) {
        List<MarkdownTextFragment> fragments = parse("> one\n" + blockMarker + "\n> two\n>\n" + blockMarker + "\n> three");

        assertEquals(List.of("one", "> ", "two", ">", "three"), texts(fragments));
        assertOneQuote(fragments);
    }

    private static void assertOneQuote(List<MarkdownTextFragment> fragments) {
        QuoteContext quote = fragments.get(0).quoteContext;
        assertNotNull(quote);
        for (MarkdownTextFragment fragment : fragments) assertSame(quote, fragment.quoteContext);
        assertEquals(fragments, quote.quoteFragments);
    }

}
