package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.CalloutType;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.QuoteContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.renderer;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownParserCalloutTest {

    @ParameterizedTest
    @EnumSource(CalloutType.class)
    void quoteThatStartsWithAMarkerLineIsACalloutOfItsType(CalloutType type) {
        List<MarkdownTextFragment> fragments = parseWithCallouts("> [!" + type.name() + "]\n> first line\nsecond line\n\nafter");
        MarkdownTextFragment marker = fragments.get(0);
        QuoteContext callout = marker.quoteContext;

        assertEquals(List.of("", "first ", "line", "second ", "line", "", "after"), texts(fragments));
        assertNotNull(callout);
        assertEquals(type, callout.calloutType);
        assertEquals(fragments.subList(0, 5), callout.quoteFragments);
        assertTrue(marker.calloutMarker);
        assertTrue(marker.naturalLineBreakAfter);
        for (MarkdownTextFragment fragment : fragments.subList(1, fragments.size())) assertFalse(fragment.calloutMarker);
        assertNull(fragmentContaining(fragments, "after").quoteContext);
    }

    @ParameterizedTest
    @ValueSource(strings = {"> [!note]", "> [!Note]", "> [!nOtE]", "> [!NOTE]   ", "§c> [!NOTE]"})
    void markerTypeIgnoresCaseAndSpacesBehindIt(String markerLine) {
        List<MarkdownTextFragment> fragments = parseWithCallouts(markerLine + "\n> body");

        assertEquals(List.of("", "body"), texts(fragments));
        assertEquals(CalloutType.NOTE, fragments.get(0).quoteContext.calloutType);
        assertTrue(fragments.get(0).calloutMarker);
        assertSame(fragments.get(0).quoteContext, fragments.get(1).quoteContext);
    }

    @ParameterizedTest
    @ValueSource(strings = {"> [!TIP]", "> [!TIP]\n", "> [!TIP]\n\nafter", "> [!TIP]\n   \nafter"})
    void calloutWithoutABodyStillIsACallout(String text) {
        List<MarkdownTextFragment> fragments = parseWithCallouts(text);
        MarkdownTextFragment marker = fragments.get(0);

        assertEquals("", marker.text);
        assertTrue(marker.calloutMarker);
        assertEquals(CalloutType.TIP, marker.quoteContext.calloutType);
        assertEquals(List.of(marker), marker.quoteContext.quoteFragments);
        for (MarkdownTextFragment fragment : fragments.subList(1, fragments.size())) assertNull(fragment.quoteContext);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[!UNKNOWN]", "[!]", "[!NOTE] text", "[!NOTE]x", " [!NOTE]", "[!NOTE", "[NOTE]", "[! NOTE]", "[!NOTE ]", "[!NOTES]", "[!NOTE]\t"})
    void quoteWithAnythingElseInItsFirstLineStaysANormalQuote(String firstLine) {
        List<MarkdownTextFragment> fragments = parseWithCallouts("> " + firstLine + "\n> body");
        QuoteContext quote = fragments.get(0).quoteContext;

        assertNotNull(quote);
        assertNull(quote.calloutType);
        assertEquals(fragments, quote.quoteFragments);
        assertFalse(fragments.get(0).text.isEmpty());
        for (MarkdownTextFragment fragment : fragments) assertFalse(fragment.calloutMarker);
    }

    @Test
    void markerLineStaysTextWhileCalloutsAreSwitchedOff() {
        List<MarkdownTextFragment> fragments = parse("> [!NOTE]\n> body");

        assertEquals(List.of("[!NOTE]", "body"), texts(fragments));
        assertNull(fragments.get(0).quoteContext.calloutType);
        assertFalse(fragments.get(0).calloutMarker);
    }

    @Test
    void markerOnlyCountsInTheFirstLineOfAQuote() {
        List<MarkdownTextFragment> fragments = parseWithCallouts("> quote\n> [!NOTE]\n> more");

        assertEquals(List.of("quote", "[!NOTE]", "more"), texts(fragments));
        assertNull(fragments.get(0).quoteContext.calloutType);
        for (MarkdownTextFragment fragment : fragments) {
            assertSame(fragments.get(0).quoteContext, fragment.quoteContext);
            assertFalse(fragment.calloutMarker);
        }
    }

    @Test
    void calloutsAndQuotesBehindEachOtherKeepTheirOwnContexts() {
        List<MarkdownTextFragment> fragments = parseWithCallouts("> [!WARNING]\n> **bold** warning\n\n> normal quote\n\n> [!CAUTION]\n> caution");
        QuoteContext warning = fragmentContaining(fragments, "warning").quoteContext;
        QuoteContext quote = fragmentContaining(fragments, "normal").quoteContext;
        QuoteContext caution = fragmentContaining(fragments, "caution").quoteContext;

        assertEquals(CalloutType.WARNING, warning.calloutType);
        assertNull(quote.calloutType);
        assertEquals(CalloutType.CAUTION, caution.calloutType);
        assertNotSame(warning, quote);
        assertNotSame(quote, caution);
        assertTrue(fragmentContaining(fragments, "bold").bold);
        assertSame(warning, fragmentContaining(fragments, "bold").quoteContext);
        assertTrue(warning.getQuoteStart().calloutMarker);
        assertFalse(quote.getQuoteStart().calloutMarker);
        assertTrue(caution.getQuoteStart().calloutMarker);
    }

    @Test
    void switchingCalloutsParsesTheTextAgain() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> [!NOTE]\n> body", 400);
        renderer.tick();

        assertFalse(renderer.isCalloutsEnabled());
        assertNull(renderer.fragmentContaining("body").quoteContext.calloutType);

        renderer.setCalloutsEnabled(true);
        renderer.tick();

        assertTrue(renderer.isCalloutsEnabled());
        assertEquals(2, renderer.parses);
        assertEquals(CalloutType.NOTE, renderer.fragmentContaining("body").quoteContext.calloutType);

        renderer.setCalloutsEnabled(true);
        renderer.tick();

        assertEquals(2, renderer.parses);
    }

    private static List<MarkdownTextFragment> parseWithCallouts(String text) {
        return parse(renderer().setCalloutsEnabled(true), text);
    }

}
