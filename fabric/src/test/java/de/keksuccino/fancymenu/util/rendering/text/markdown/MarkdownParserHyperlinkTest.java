package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @ParameterizedTest
    @ValueSource(strings = {"https://example.invalid", "click:event", "hover:event"})
    void spaceBehindALinkJoinsItsLastFragmentAndEndsItsWord(String target) {
        List<MarkdownTextFragment> fragments = parse("[two words](" + target + ") next");
        MarkdownTextFragment last = fragmentContaining(fragments, "words");

        assertEquals("words ", last.text);
        assertTrue(last.endOfWord);
        assertEquals("next", fragments.get(fragments.indexOf(last) + 1).text);
    }

    @ParameterizedTest
    @ValueSource(strings = {"next", ".", ")", "**bold**"})
    void textDirectlyBehindALinkStaysInItsWord(String textBehindLink) {
        MarkdownTextFragment link = fragmentContaining(parse("[link](https://example.invalid)" + textBehindLink), "link");

        assertEquals("link", link.text);
        assertFalse(link.endOfWord);
    }

    @ParameterizedTest
    @ValueSource(strings = {"word[link](https://example.invalid)", "word [link](https://example.invalid)"})
    void textInFrontOfALinkOnlyEndsItsWordWithASpace(String text) {
        MarkdownTextFragment word = fragmentContaining(parse(text), "word");

        assertEquals(text.contains("word "), word.endOfWord);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[link](https://example.invalid)\nnext", "[link](https://example.invalid)"})
    void lineEndBehindALinkGetsAFragmentOfItsOwn(String text) {
        List<MarkdownTextFragment> fragments = parse(text);
        MarkdownTextFragment link = fragmentContaining(fragments, "link");
        MarkdownTextFragment lineEnd = fragments.get(fragments.indexOf(link) + 1);

        assertFalse(link.endOfWord);
        assertFalse(link.naturalLineBreakAfter);
        assertEquals("", lineEnd.text);
        assertTrue(lineEnd.naturalLineBreakAfter);
    }

    @Test
    void lineWrapsBehindTheSpaceThatFollowsALink() {
        // The label and the word behind it are too wide for one line
        assertEquals(List.of(List.of("", "alonglinklabel "), List.of("next")), renderLines("[alonglinklabel](https://example.invalid) next", 100));
    }

    @Test
    void lineDoesNotWrapBetweenALinkAndTheTextDirectlyBehindIt() {
        // "aa bb" fits into the line and the "cc" behind the link does not, but it belongs to the word the link ends in
        assertEquals(List.of(List.of("aa ", "bb", "cc "), List.of("dd")), renderLines("aa [bb](https://example.invalid)cc dd", 40));
    }

    @Test
    void lineBreakBehindALinkThatIsWiderThanTheRendererAddsNoEmptyLine() {
        assertEquals(List.of(List.of("", "averyveryverylonglinklabel", ""), List.of("next")), renderLines("[averyveryverylonglinklabel](https://example.invalid)\nnext", 60));
    }

    @Test
    void escapedBracketsAreTextOfAnImageLabel() {
        // ![a\]b](image.png)
        List<MarkdownTextFragment> fragments = parse("![a\\]b](image.png)");

        assertEquals(1, fragments.size());
        assertEquals("image.png", fragments.get(0).imageSupplier.getSourceWithoutPrefix());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Logo (dark)", "a) b", "(c", "plain"})
    void hyperlinkImageKeepsItsLinkWhenItsLabelHoldsRoundBrackets(String label) {
        List<MarkdownTextFragment> fragments = parse("[![" + label + "](logo.png)](https://example.invalid)\ntext");
        MarkdownTextFragment image = fragments.get(0);

        assertEquals(2, fragments.size());
        assertEquals("logo.png", image.imageSupplier.getSourceWithoutPrefix());
        assertEquals("https://example.invalid", image.hyperlink.link);
        assertNull(fragments.get(1).imageSupplier);
    }

    @Test
    void imageKeepsItsSourceWhenItsLabelHoldsRoundBrackets() {
        List<MarkdownTextFragment> fragments = parse("![Screenshot (1)](screenshot.png)");

        assertEquals(1, fragments.size());
        assertEquals("screenshot.png", fragments.get(0).imageSupplier.getSourceWithoutPrefix());
        assertNull(fragments.get(0).hyperlink);
    }

    /**
     * Returns the fragment texts of all render lines the text gets at the given width, measured with 6 pixels per character.
     */
    private static List<List<String>> renderLines(String text, float width) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, width);
        renderer.tick();
        List<List<String>> lines = new ArrayList<>();
        for (MarkdownTextLine line : renderer.lines) lines.add(texts(line.fragments));
        return lines;
    }

}
