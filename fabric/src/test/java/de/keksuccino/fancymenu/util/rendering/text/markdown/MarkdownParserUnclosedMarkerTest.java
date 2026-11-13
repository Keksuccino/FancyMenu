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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Markers that never get closed stay in the text.
 */
class MarkdownParserUnclosedMarkerTest {

    @ParameterizedTest
    @ValueSource(strings = {"~~open", "open~~", "a ~~ b", "~~open~", "~~~", "~~", "a ~~b\nc ~d", "**open", "*open", "_open", "~open", "`open", ";;open", "%!!uniform%open", "%#ff0000%open", "[open", "[open](target"})
    void markerWithoutAClosingOneStaysText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals(text.replace("\n", ""), joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) {
            assertFalse(fragment.bold || fragment.italic || fragment.strikethrough || fragment.plainText);
            assertNull(fragment.codeBlockContext);
            assertNull(fragment.hyperlink);
            assertNull(fragment.font);
            assertNull(fragment.textColor);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", "```\ncode", "text\n```\nmore", "```java\ncode\n```", "````", "``", "a `` b", "```\n\n"})
    void graveAccentsWithoutAClosingMarkerStayText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals(text.replace("\n", ""), joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) assertNull(fragment.codeBlockContext);
    }

    @Test
    void closedMarkersStillWorkNextToUnclosedOnes() {
        List<MarkdownTextFragment> fragments = parse("~~struck~~ ~one~ `code` ~~open and ``");

        assertTrue(fragmentContaining(fragments, "struck").strikethrough);
        assertTrue(fragmentContaining(fragments, "one").strikethrough);
        assertNotNull(fragmentContaining(fragments, "code").codeBlockContext);
        assertFalse(fragmentContaining(fragments, "open").strikethrough);
        assertEquals("struck one code ~~open and ``", joinText(fragments));
    }

    @Test
    void multiLineCodeBlockStillOpensAndClosesAtItsMarkerLines() {
        List<MarkdownTextFragment> fragments = parse("```\ncode `` here\n```\n```");

        assertNotNull(fragmentContaining(fragments, "code").codeBlockContext);
        assertFalse(fragmentContaining(fragments, "code").codeBlockContext.singleLine);
        assertEquals("code `` here```", joinText(fragments));
        assertNull(fragments.get(fragments.size() - 1).codeBlockContext);
    }

    @Test
    void inlineCodeStillStartsAtTheSecondOfTwoGraveAccents() {
        // `` a ` b
        List<MarkdownTextFragment> fragments = parse("`` a ` b");

        assertEquals("` a  b", joinText(fragments));
        assertNotNull(fragmentContaining(fragments, "a").codeBlockContext);
        assertNull(fragmentContaining(fragments, "b").codeBlockContext);
    }

}
