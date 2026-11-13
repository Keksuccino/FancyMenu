package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The comments show the Markdown source without the escaping of the Java strings.
 */
class MarkdownParserHyperlinkTest {

    @Test
    void escapedBracketsAreTextOfTheLabel() {
        // [a\]b \[c](target) d
        List<MarkdownTextFragment> fragments = parse("[a\\]b \\[c](target) d");
        MarkdownTextFragment first = fragmentContaining(fragments, "a]b");
        MarkdownTextFragment second = fragmentContaining(fragments, "[c");

        assertNotNull(first.hyperlink);
        assertSame(first.hyperlink, second.hyperlink);
        assertEquals("target", first.hyperlink.link);
        assertNull(fragmentContaining(fragments, "d").hyperlink);
        assertEquals("a]b [c d", joinText(fragments));
    }

    @Test
    void labelWithOnlyAnEscapedClosingBracketIsNoLink() {
        // [a\](x) and [b](a longer target) c
        List<MarkdownTextFragment> fragments = parse("[a\\](x) and [b](a longer target) c");
        MarkdownTextFragment link = fragmentContaining(fragments, "b");

        assertEquals("a longer target", link.hyperlink.link);
        assertEquals(List.of(link), link.hyperlink.hyperlinkFragments);
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment != link) assertNull(fragment.hyperlink);
        }
        assertEquals("[a](x) and b c", joinText(fragments));
    }

    @Test
    void escapedBackslashInFrontOfTheClosingBracketLeavesItActive() {
        // [a\\](target) b
        List<MarkdownTextFragment> fragments = parse("[a\\\\](target) b");
        MarkdownTextFragment link = fragmentContaining(fragments, "a");

        assertEquals("a\\ ", link.text);
        assertEquals("target", link.hyperlink.link);
        assertNull(fragmentContaining(fragments, "b").hyperlink);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[a\\]](click:event) b", "[a\\]](hover:event) b"})
    void textEventsEndAtTheSameBracketAsHyperlinks(String text) {
        // [a\]](click:event) b
        List<MarkdownTextFragment> fragments = parse(text);
        MarkdownTextFragment event = fragmentContaining(fragments, "a]");

        assertEquals("event", (event.clickEvent != null) ? event.clickEvent.identifier : event.hoverEvent.identifier);
        assertNull(fragmentContaining(fragments, "b").clickEvent);
        assertNull(fragmentContaining(fragments, "b").hoverEvent);
        assertEquals("a] b", joinText(fragments));
    }

    @Test
    void escapedBracketInsideInlineCodeOfALabelStaysWithItsBackslash() {
        // [a `\]` b](target) c
        List<MarkdownTextFragment> fragments = parse("[a `\\]` b](target) c");
        MarkdownTextFragment code = fragmentContaining(fragments, "\\]");

        assertNotNull(code.codeBlockContext);
        assertEquals("target", code.hyperlink.link);
        assertSame(code.hyperlink, fragmentContaining(fragments, "b").hyperlink);
        assertNull(fragmentContaining(fragments, "c").hyperlink);
        assertEquals("a \\] b c", joinText(fragments));
    }

    @Test
    void escapedBracketsAreTextOfAnImageLabel() {
        // ![a\]b](image.png)
        List<MarkdownTextFragment> fragments = parse("![a\\]b](image.png)");

        assertEquals(1, fragments.size());
        assertEquals("image.png", fragments.get(0).imageSupplier.getSourceWithoutPrefix());
    }

}
