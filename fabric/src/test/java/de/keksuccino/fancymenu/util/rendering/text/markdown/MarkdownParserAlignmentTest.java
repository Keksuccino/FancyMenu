package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer.MarkdownLineAlignment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A line that is "^^^" or "|||" opens an aligned block if a later line with the same marker closes it. Lines of code blocks
 * and plain text are text, so they open and close nothing.
 */
class MarkdownParserAlignmentTest {

    @ParameterizedTest
    @CsvSource({"^^^,CENTERED", "|||,RIGHT"})
    void markerLinesAlignTheLinesBetweenThem(String marker, MarkdownLineAlignment alignment) {
        List<MarkdownTextFragment> fragments = parse("before\n" + marker + "\ninside\n" + marker + "\nafter");

        assertEquals(List.of("before", "inside", "after"), texts(fragments));
        assertEquals(MarkdownLineAlignment.LEFT, fragmentContaining(fragments, "before").alignment);
        assertEquals(alignment, fragmentContaining(fragments, "inside").alignment);
        assertEquals(MarkdownLineAlignment.LEFT, fragmentContaining(fragments, "after").alignment);
    }

    @ParameterizedTest
    @ValueSource(strings = {"^^^", "|||"})
    void markerLinesInsidePlainTextStayText(String marker) {
        List<MarkdownTextFragment> fragments = parse(";;;\n" + marker + "\ninside\n" + marker + "\n;;;\nafter");

        assertEquals(List.of(marker, "inside", marker, "after"), texts(fragments));
        for (MarkdownTextFragment fragment : fragments) assertEquals(MarkdownLineAlignment.LEFT, fragment.alignment);
        assertTrue(fragmentContaining(fragments, "inside").plainText);
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", ";;;"})
    void alignedBlockCanHoldCodeBlocksAndPlainText(String blockMarker) {
        List<MarkdownTextFragment> fragments = parse("^^^\n" + blockMarker + "\ninside\n" + blockMarker + "\n^^^\nafter");
        MarkdownTextFragment inside = fragmentContaining(fragments, "inside");

        assertEquals(List.of("inside", "after"), texts(fragments));
        assertEquals(MarkdownLineAlignment.CENTERED, inside.alignment);
        assertTrue(inside.plainText || (inside.codeBlockContext != null));
        assertEquals(MarkdownLineAlignment.LEFT, fragmentContaining(fragments, "after").alignment);
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", ";;;"})
    void markerLineInsideACodeBlockOrPlainTextClosesNoBlockAroundIt(String blockMarker) {
        List<MarkdownTextFragment> fragments = parse("^^^\nfirst\n" + blockMarker + "\n^^^\n" + blockMarker + "\nsecond\n^^^\nafter");

        assertEquals(List.of("first", "^^^", "second", "after"), texts(fragments));
        assertEquals(MarkdownLineAlignment.CENTERED, fragmentContaining(fragments, "first").alignment);
        assertEquals(MarkdownLineAlignment.CENTERED, fragmentContaining(fragments, "^^^").alignment);
        assertEquals(MarkdownLineAlignment.CENTERED, fragmentContaining(fragments, "second").alignment);
        assertEquals(MarkdownLineAlignment.LEFT, fragmentContaining(fragments, "after").alignment);
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", ";;;"})
    void markerLineInsideACodeBlockOrPlainTextIsNoClosingLineForAMarkerInFrontOfIt(String blockMarker) {
        List<MarkdownTextFragment> fragments = parse("^^^\nfirst\n" + blockMarker + "\n^^^\n" + blockMarker + "\nafter");

        assertEquals(List.of("^^^", "first", "^^^", "after"), texts(fragments));
        for (MarkdownTextFragment fragment : fragments) assertEquals(MarkdownLineAlignment.LEFT, fragment.alignment);
    }

    @ParameterizedTest
    @ValueSource(strings = {"^^^x", "^^^ ", "^^^^", " ^^^", "x^^^"})
    void lineThatIsMoreThanTheMarkerIsNoClosingLine(String line) {
        List<MarkdownTextFragment> fragments = parse("^^^\ninside\n" + line + "\nafter");

        assertEquals("^^^", fragments.get(0).text);
        for (MarkdownTextFragment fragment : fragments) assertEquals(MarkdownLineAlignment.LEFT, fragment.alignment);
    }

    @Test
    void markerLineBehindAFormattingCodeClosesItsBlock() {
        List<MarkdownTextFragment> fragments = parse("^^^\ninside\n§c^^^\nafter");

        assertEquals(List.of("inside", "after"), texts(fragments));
        assertEquals(MarkdownLineAlignment.CENTERED, fragmentContaining(fragments, "inside").alignment);
        assertEquals(MarkdownLineAlignment.LEFT, fragmentContaining(fragments, "after").alignment);
    }

    @Test
    void markerLinesOfTheOtherAlignmentInsideABlockStayText() {
        List<MarkdownTextFragment> fragments = parse("^^^\n|||\ninside\n|||\n^^^\nafter");

        assertEquals(List.of("|||", "inside", "|||", "after"), texts(fragments));
        assertEquals(MarkdownLineAlignment.CENTERED, fragmentContaining(fragments, "inside").alignment);
        assertEquals(MarkdownLineAlignment.LEFT, fragmentContaining(fragments, "after").alignment);
    }

    @Test
    void markerLineWithoutAClosingLineStaysTextAndLeavesLaterBlocksAlone() {
        List<MarkdownTextFragment> fragments = parse("^^^\n|||\ninside\n|||\nafter");

        assertEquals(List.of("^^^", "inside", "after"), texts(fragments));
        assertEquals(MarkdownLineAlignment.LEFT, fragments.get(0).alignment);
        assertEquals(MarkdownLineAlignment.RIGHT, fragmentContaining(fragments, "inside").alignment);
        assertEquals(MarkdownLineAlignment.LEFT, fragmentContaining(fragments, "after").alignment);
    }

    @Test
    void blocksOpenAndCloseOneAfterTheOther() {
        List<MarkdownTextFragment> fragments = parse("^^^\none\n^^^\ntwo\n^^^\nthree\n^^^\nfour\n^^^\nfive");

        assertEquals(MarkdownLineAlignment.CENTERED, fragmentContaining(fragments, "one").alignment);
        assertEquals(MarkdownLineAlignment.LEFT, fragmentContaining(fragments, "two").alignment);
        assertEquals(MarkdownLineAlignment.CENTERED, fragmentContaining(fragments, "three").alignment);
        assertEquals(MarkdownLineAlignment.LEFT, fragmentContaining(fragments, "four").alignment);
        // The fifth marker has no line that closes it
        assertEquals(MarkdownLineAlignment.LEFT, fragmentContaining(fragments, "five").alignment);
        assertNotNull(fragmentContaining(fragments, "^^^"));
    }

}
