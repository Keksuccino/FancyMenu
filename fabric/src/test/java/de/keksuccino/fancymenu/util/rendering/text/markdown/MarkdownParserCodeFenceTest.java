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
 * A code block starts at a line with three grave accents, which can have an info string like the name of a language behind them,
 * and ends at the next line that has nothing but whitespace behind its three grave accents.
 */
class MarkdownParserCodeFenceTest {

    @ParameterizedTest
    @ValueSource(strings = {"``` ", "```   ", "```\t", "``` \t ", "```\r"})
    void closingLineWithWhitespaceBehindItsMarkerClosesTheBlock(String closingLine) {
        List<MarkdownTextFragment> fragments = parse("```\ncode **text**\n" + closingLine + "\nafter **bold**");

        assertEquals("code **text**after bold", joinText(fragments));
        assertTrue(isInCodeBlock(fragmentContaining(fragments, "code")));
        assertTrue(isInCodeBlock(fragmentContaining(fragments, "**text**")));
        assertFalse(isInCodeBlock(fragmentContaining(fragments, "after")));
        assertTrue(fragmentContaining(fragments, "bold").bold);
    }

    @ParameterizedTest
    @ValueSource(strings = {"```java", "``` java", "```java  ", "```{.python title=\"x\"}", "```c++ linenums", "```~~~"})
    void openingLineCanHaveAnInfoStringThatIsNotShown(String openingLine) {
        List<MarkdownTextFragment> fragments = parse("before\n" + openingLine + "\ncode **text**\n```\nafter");

        assertEquals(List.of("before", "code ", "**text**", "after"), texts(fragments));
        assertFalse(isInCodeBlock(fragmentContaining(fragments, "before")));
        assertTrue(isInCodeBlock(fragmentContaining(fragments, "code")));
        assertTrue(isInCodeBlock(fragmentContaining(fragments, "**text**")));
        assertFalse(isInCodeBlock(fragmentContaining(fragments, "after")));
    }

    @Test
    void blocksWithAndWithoutInfoStringFollowEachOther() {
        List<MarkdownTextFragment> fragments = parse("```\nfirst\n```\nbetween\n```java\nsecond\n``` \nafter");

        assertEquals(List.of("first", "between", "second", "after"), texts(fragments));
        assertTrue(isInCodeBlock(fragmentContaining(fragments, "first")));
        assertFalse(isInCodeBlock(fragmentContaining(fragments, "between")));
        assertTrue(isInCodeBlock(fragmentContaining(fragments, "second")));
        assertFalse(isInCodeBlock(fragmentContaining(fragments, "after")));
        assertTrue(fragmentContaining(fragments, "first").codeBlockContext != fragmentContaining(fragments, "second").codeBlockContext);
    }

    @Test
    void markerLineWithInfoStringInsideABlockIsTextOfTheBlock() {
        List<MarkdownTextFragment> fragments = parse("```\n```java\ninside\n```\nafter");

        assertEquals(List.of("```java", "inside", "after"), texts(fragments));
        assertTrue(isInCodeBlock(fragmentContaining(fragments, "```java")));
        assertTrue(isInCodeBlock(fragmentContaining(fragments, "inside")));
        assertFalse(isInCodeBlock(fragmentContaining(fragments, "after")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"```java\ncode **bold**", "```\ncode **bold**\n```java", "```\ncode **bold**\n ```", "```\ncode **bold**\n```x", "```java\ncode **bold**\n````"})
    void markerLineWithoutALineThatClosesItsBlockStaysText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertTrue(fragments.stream().noneMatch(MarkdownParserCodeFenceTest::isInCodeBlock));
        assertTrue(fragmentContaining(fragments, "bold").bold);
        assertEquals(text.replace("**", "").replace("\n", ""), joinText(fragments));
    }

    @ParameterizedTest
    @ValueSource(strings = {"```code``` text\nmore **bold**\n```", "``` a `b` c\nmore **bold**\n```"})
    void lineWithGraveAccentsBehindTheMarkerOpensNoBlock(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertTrue(fragments.stream().noneMatch(MarkdownParserCodeFenceTest::isInCodeBlock));
        assertTrue(fragmentContaining(fragments, "bold").bold);
    }

    @Test
    void closingLineBehindAFormattingCodeClosesTheBlock() {
        List<MarkdownTextFragment> fragments = parse("```\ncode\n§c```\nafter");

        assertEquals(List.of("code", "after"), texts(fragments));
        assertTrue(isInCodeBlock(fragmentContaining(fragments, "code")));
        assertFalse(isInCodeBlock(fragmentContaining(fragments, "after")));
    }

    private static boolean isInCodeBlock(MarkdownTextFragment fragment) {
        return (fragment.codeBlockContext != null) && !fragment.codeBlockContext.singleLine;
    }

}
