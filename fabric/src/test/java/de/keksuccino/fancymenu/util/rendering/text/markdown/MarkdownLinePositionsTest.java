package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownLinePositionsTest {

    private static final int CHAR_WIDTH = 6;
    /** Wide enough for every line of the texts below, so only their line breaks end lines. */
    private static final float WIDE = 2000.0F;
    /** Room for ten characters, so lines with more words than that wrap. */
    private static final float NARROW = 2 + (10 * CHAR_WIDTH) + 2;

    private static MarkdownTestRenderer renderer(float optimalWidth, String... lines) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(String.join("\n", lines), optimalWidth);
        renderer.setAutoLineBreakingEnabled(true);
        renderer.tick();
        return renderer;
    }

    /** Where the render line of the fragment with the given text is, the way the positions count. */
    private static double positionOfLineWith(MarkdownTestRenderer renderer, String text) {
        return positionOf(renderer, renderer.fragmentContaining(text).parentLine);
    }

    /** The top of the line, without the room that stays above it: the padding of the text, or the space to the line in front of it if that is less. */
    private static double positionOf(MarkdownTestRenderer renderer, MarkdownTextLine line) {
        MarkdownTextLine previous = line.getPreviousLine();
        float endOfPrevious = (previous != null) ? (previous.offsetY + previous.getLineHeight()) : 0.0F;
        return line.offsetY - Math.min(renderer.scaling.padding(), Math.max(0.0F, line.offsetY - endOfPrevious));
    }

    @Test
    void theFirstLineIsWhereTheTextStarts() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha", "beta", "gamma");
        assertEquals(positionOfLineWith(renderer, "alpha"), renderer.getLinePositions().find(1));
        // The padding of the text stays above the line, so the first line never needs the text to move
        assertTrue(renderer.getLinePositions().find(1) < renderer.lines.get(0).getLineHeight());
    }

    @Test
    void nothingOfTheLineInFrontOfALineShowsAboveIt() {
        MarkdownTestRenderer renderer = renderer(NARROW, "# Headline", "one two three four five six", "", "omega");
        renderer.getStyle().padding.set(6.0F);
        renderer.tick();
        List<Double> positions = new ArrayList<>();
        renderer.getLinePositions().forEach(false, positions::add);
        assertTrue(positions.size() > 3);
        int position = 0;
        for (MarkdownTextLine line : renderer.lines) {
            if (line.collapsed) continue;
            double top = positions.get(position++);
            MarkdownTextLine previous = line.getPreviousLine();
            // What shows starts at the position, which is never in the line in front of this one, and never below the top of this one
            if (previous != null) assertTrue(top >= (previous.offsetY + previous.getLineHeight()), "line " + line.index);
            assertTrue(top <= line.offsetY, "line " + line.index);
            // The padding of the text is the most room a line has above itself
            assertTrue((line.offsetY - top) <= renderer.scaling.padding(), "line " + line.index);
        }
        assertEquals(positions.size(), position);
    }

    @Test
    void linesGetFoundByTheNumberTheyHaveInTheText() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha", "beta", "", "gamma");
        MarkdownLinePositions positions = renderer.getLinePositions();
        assertEquals(positionOfLineWith(renderer, "beta"), positions.find(2));
        assertEquals(positionOfLineWith(renderer, "gamma"), positions.find(4));
        assertTrue(positions.find(4) > positions.find(2));
        assertTrue(positions.find(2) > positions.find(1));
    }

    @Test
    void aLineThatWrapsStillIsOneLineOfTheText() {
        MarkdownTestRenderer renderer = renderer(NARROW, "alpha", "beta beta beta beta beta beta", "gamma");
        // The second line shows as several lines, which all come in front of the third one
        MarkdownTextLine gamma = renderer.fragmentContaining("gamma").parentLine;
        assertTrue(gamma.index > 2);
        assertEquals(positionOf(renderer, gamma), renderer.getLinePositions().find(3));
        assertEquals(positionOf(renderer, renderer.lines.get(1)), renderer.getLinePositions().find(2));
    }

    @Test
    void numbersOutsideOfTheTextAreItsFirstAndItsLastLine() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha", "beta", "gamma");
        MarkdownLinePositions positions = renderer.getLinePositions();
        assertEquals(positions.find(1), positions.find(0));
        assertEquals(positions.find(1), positions.find(-5));
        assertEquals(positions.find(3), positions.find(4));
        assertEquals(positions.find(3), positions.find(900));
    }

    @Test
    void aLineThatShowsNothingIsWhereTheNextLineIs() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha", "```", "code", "```", "omega");
        MarkdownLinePositions positions = renderer.getLinePositions();
        // The fences of a code block are lines of the text that never show
        assertEquals(positionOfLineWith(renderer, "code"), positions.find(2));
        assertEquals(positionOfLineWith(renderer, "code"), positions.find(3));
        assertEquals(positionOfLineWith(renderer, "omega"), positions.find(4));
        assertEquals(positionOfLineWith(renderer, "omega"), positions.find(5));
    }

    @Test
    void everyLineOfATableIsWhereTheTableIs() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha", "| a | b |", "|---|---|", "| c | d |", "omega");
        MarkdownLinePositions positions = renderer.getLinePositions();
        double table = positionOf(renderer, renderer.tableFragment(0).parentLine);
        assertEquals(table, positions.find(2));
        assertEquals(table, positions.find(3));
        assertEquals(table, positions.find(4));
        assertEquals(positionOfLineWith(renderer, "omega"), positions.find(5));
        assertNotEquals(table, positions.find(5));
    }

    @Test
    void aLineGetsFoundByAPartOfItsText() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha one", "beta two", "gamma three");
        MarkdownLinePositions positions = renderer.getLinePositions();
        assertEquals(positions.find(2), positions.find("ta tw"));
        assertEquals(positions.find(3), positions.find("three"));
        // The first line that holds the text wins
        assertEquals(positions.find(1), positions.find("a"));
    }

    @Test
    void upperAndLowerCaseMakeNoDifferenceWhenLookingForText() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha", "Beta Two", "gamma");
        assertEquals(renderer.getLinePositions().find(2), renderer.getLinePositions().find("bETA tWO"));
    }

    @Test
    void textGetsFoundTheWayItShowsWithoutWhatFormatsIt() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha", "some **bold** words", "## The Headline", "omega");
        MarkdownLinePositions positions = renderer.getLinePositions();
        assertEquals(positions.find(2), positions.find("some bold words"));
        assertEquals(positions.find(3), positions.find("the headline"));
        // The asterisks are no part of what the line shows
        assertTrue(Double.isNaN(positions.find("**bold**")));
    }

    @Test
    void textGetsFoundWhereItsLineWrapsInTheMiddleOfIt() {
        MarkdownTestRenderer renderer = renderer(NARROW, "alpha", "one two three four five six", "omega");
        MarkdownLinePositions positions = renderer.getLinePositions();
        MarkdownTextLine three = renderer.fragmentContaining("three").parentLine;
        MarkdownTextLine four = renderer.fragmentContaining("four").parentLine;
        assertNotEquals(three.index, four.index, "The words have to be in two lines for this test");
        // The text starts in the line of its first word, which is the one that gets scrolled to
        assertEquals(positionOf(renderer, three), positions.find("three four"));
        assertEquals(positionOf(renderer, four), positions.find("four"));
    }

    @Test
    void textInATableIsWhereTheTableIs() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha", "| first | second |", "|---|---|", "| third | fourth |", "omega");
        MarkdownLinePositions positions = renderer.getLinePositions();
        assertEquals(positions.find(2), positions.find("fourth"));
        // Cells are lines of their own, so nothing gets found across two of them
        assertTrue(Double.isNaN(positions.find("first second")));
    }

    @Test
    void textNoLineHoldsGetsNoPosition() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha", "beta");
        assertTrue(Double.isNaN(renderer.getLinePositions().find("missing")));
        assertTrue(Double.isNaN(renderer.getLinePositions().find("")));
        // Two lines of the text never are one line to look in
        assertTrue(Double.isNaN(renderer.getLinePositions().find("alphabeta")));
    }

    @Test
    void everyLineThatShowsHasAPositionInOrder() {
        MarkdownTestRenderer renderer = renderer(NARROW, "alpha", "one two three four five six", "omega");
        List<Double> positions = new ArrayList<>();
        renderer.getLinePositions().forEach(false, positions::add);
        assertEquals(renderer.lines.size(), positions.size());
        for (int i = 0; i < positions.size(); i++) {
            assertEquals(positionOf(renderer, renderer.lines.get(i)), positions.get(i));
            if (i > 0) assertTrue(positions.get(i) > positions.get(i - 1));
        }
    }

    @Test
    void onlyHeadlinesHaveAPositionWhenOnlyHeadlinesCount() {
        MarkdownTestRenderer renderer = renderer(WIDE, "# First", "words", "more words", "## Second", "words", "### Third", "#notaheadline");
        List<Double> positions = new ArrayList<>();
        renderer.getLinePositions().forEach(true, positions::add);
        assertEquals(List.of(positionOfLineWith(renderer, "First"), positionOfLineWith(renderer, "Second"), positionOfLineWith(renderer, "Third")), positions);
    }

    @Test
    void aTextWithoutHeadlinesHasNoHeadlinePositions() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha", "beta");
        List<Double> positions = new ArrayList<>();
        renderer.getLinePositions().forEach(true, positions::add);
        assertTrue(positions.isEmpty());
    }

    @Test
    void positionsFollowTheTextWhenItChanges() {
        MarkdownTestRenderer renderer = renderer(WIDE, "alpha", "beta", "gamma");
        double before = renderer.getLinePositions().find("gamma");
        renderer.setText("alpha\nbeta\nmore\nlines\ngamma");
        // Nothing rendered since the text changed, so the positions have to lay the text out themselves
        assertTrue(renderer.getLinePositions().find("gamma") > before);
        assertEquals(renderer.getLinePositions().find(5), renderer.getLinePositions().find("gamma"));
    }

}
