package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer.MarkdownLineAlignment;
import org.junit.jupiter.api.Test;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer.MarkdownLineAlignment.CENTERED;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer.MarkdownLineAlignment.LEFT;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer.MarkdownLineAlignment.RIGHT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MarkdownParserLineAlignmentsTest {

    @Test
    void markerLinesHaveNoAlignmentAndTheLinesBetweenThemHaveTheAlignmentOfTheirBlock() {
        assertAlignments("a\n^^^\nb\n^^^\n|||\nc\n|||\nd", LEFT, null, CENTERED, null, null, RIGHT, null, LEFT);
    }

    @Test
    void everyLineGetsAnAlignment() {
        assertAlignments("", LEFT);
        assertAlignments("a\n", LEFT, LEFT);
        assertAlignments("\n\n", LEFT, LEFT, LEFT);
    }

    @Test
    void markerLinesMayHaveWhitespaceBehindTheirMarker() {
        assertAlignments("^^^  \na\n^^^ ", null, CENTERED, null);
    }

    @Test
    void markerLineWithoutALaterOneToCloseItsBlockIsText() {
        assertAlignments("^^^\na", LEFT, LEFT);
        assertAlignments("^^^\na\n^^^\n^^^\nb", null, CENTERED, null, LEFT, LEFT);
    }

    @Test
    void markerLineOfTheOtherAlignmentInsideOfABlockIsText() {
        assertAlignments("^^^\n|||\na\n^^^\n|||", null, CENTERED, CENTERED, null, LEFT);
    }

    @Test
    void markerLinesInsideOfCodeBlocksAndPlainTextAreText() {
        assertAlignments("```\n^^^\nx\n^^^\n```", LEFT, LEFT, LEFT, LEFT, LEFT);
        assertAlignments(";;;\n|||\nx\n|||\n;;;", LEFT, LEFT, LEFT, LEFT, LEFT);
    }

    @Test
    void markersInsideOfLinesAreText() {
        assertAlignments("a ^^^\nb\nc ^^^", LEFT, LEFT, LEFT);
    }

    private static void assertAlignments(String text, MarkdownLineAlignment... expected) {
        assertArrayEquals(expected, MarkdownParser.getLineAlignments(text), text);
    }

}
