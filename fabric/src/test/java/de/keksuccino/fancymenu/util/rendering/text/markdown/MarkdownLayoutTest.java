package de.keksuccino.fancymenu.util.rendering.text.markdown;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownLayoutTest {

    private static final int CHAR_WIDTH = 6;
    private static final int LINE_HEIGHT = 9;

    @Test
    void lineSizesStayTheSizesTheirFragmentsHaveAfterTheLayout() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(document(40), 180);
        renderer.tick();

        for (MarkdownTextLine line : renderer.lines) {
            float width = line.getLineWidth();
            float height = line.getLineHeight();
            float expectedWidth = 0;
            float expectedHeight = 0;
            for (MarkdownTextFragment fragment : line.fragments) {
                expectedWidth += fragment.getRenderWidth();
                expectedHeight = Math.max(expectedHeight, fragment.getRenderHeight());
            }
            MarkdownTextFragment last = line.fragments.get(line.fragments.size() - 1);
            if (last.text.endsWith(" ")) expectedWidth -= CHAR_WIDTH * last.getScale();

            assertEquals(Math.max(0, expectedWidth), width);
            assertEquals(line.paddingTop + expectedHeight + line.paddingBottom, height);
            line.updateSize();
            assertEquals(width, line.getLineWidth());
            assertEquals(height, line.getLineHeight());
        }
    }

    @Test
    void linesStackWithTheLineSpacingBetweenThem() {
        // Blocks without a space of their own above or below them
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("## Headline\n\nA paragraph with **bold** words and `inline code` that wraps around in a narrow renderer.\n\n> quote\n\n```\ncode\nmore code\n```\n\nlast", 180);
        renderer.setLineSpacing(4);
        renderer.setBorder(6);
        renderer.setX(30).setY(50);
        renderer.tick();

        float offsetY = 6;
        for (MarkdownTextLine line : renderer.lines) {
            assertEquals(offsetY, line.offsetY);
            for (MarkdownTextFragment fragment : line.fragments) {
                assertEquals(50 + offsetY, fragment.y);
            }
            assertEquals(30 + line.offsetX, line.fragments.get(0).x);
            offsetY += line.getLineHeight() + 4;
        }
    }

    @Test
    void multiLineCodeBlockGetsItsPaddingOnItsFirstAndLastLineOnly() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("before\n```\nfirst\nmiddle\nlast\n```\nafter", 400);
        renderer.tick();

        assertEquals(LINE_HEIGHT, renderer.fragmentContaining("before").parentLine.getLineHeight());
        assertEquals(LINE_HEIGHT + 10, renderer.fragmentContaining("first").parentLine.getLineHeight());
        assertEquals(LINE_HEIGHT, renderer.fragmentContaining("middle").parentLine.getLineHeight());
        assertEquals(LINE_HEIGHT + 10, renderer.fragmentContaining("last").parentLine.getLineHeight());
        assertEquals(LINE_HEIGHT, renderer.fragmentContaining("after").parentLine.getLineHeight());

        // The sizes don't depend on where the renderer is or was
        renderer.setY(500);
        renderer.setLineSpacing(0);
        renderer.tick();

        assertEquals(LINE_HEIGHT + 10, renderer.fragmentContaining("first").parentLine.getLineHeight());
        assertEquals(LINE_HEIGHT, renderer.fragmentContaining("middle").parentLine.getLineHeight());
        assertEquals(LINE_HEIGHT + 10, renderer.fragmentContaining("last").parentLine.getLineHeight());
    }

    @Test
    void alignedLinesGetTheirOffsetFromTheRealWidth() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("left\n^^^\nmid\n^^^\n|||\nright\n|||", 200);
        renderer.tick();

        float innerWidth = 200 - 2 - 2;
        assertEquals(2.0F, renderer.fragmentContaining("left").parentLine.offsetX);
        assertEquals(2.0F + ((innerWidth - (3 * CHAR_WIDTH)) / 2.0F), renderer.fragmentContaining("mid").parentLine.offsetX);
        assertEquals(2.0F + (innerWidth - (5 * CHAR_WIDTH)), renderer.fragmentContaining("right").parentLine.offsetX);
    }

    @Test
    void inlineCodeSpanGetsOnePartPerLineItWrapsOver() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("some text `code span with quite a few words in it` and more text", 110);
        renderer.tick();

        int linesWithCode = 0;
        for (MarkdownTextLine line : renderer.lines) {
            MarkdownTextFragment firstCode = null;
            MarkdownTextFragment lastCode = null;
            for (MarkdownTextFragment fragment : line.fragments) {
                if (fragment.codeBlockContext == null) continue;
                if (firstCode == null) firstCode = fragment;
                lastCode = fragment;
            }
            if (firstCode == null) {
                assertTrue(line.singleLineCodeBlockStartEndPairs.isEmpty());
                continue;
            }
            linesWithCode++;
            MarkdownTextLine.SingleLineCodeBlockPart part = line.singleLineCodeBlockStartEndPairs.get(firstCode.codeBlockContext);
            assertNotNull(part);
            assertSame(firstCode, part.start);
            assertSame(lastCode, part.end);
        }
        assertTrue(linesWithCode > 1);
    }

    @Test
    void everyFragmentOfALineHasItsPositionBeforeTheFirstOneRenders() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("", 400);
        List<Float> lastFragmentPositions = new ArrayList<>();
        MarkdownTextFragment last = new MarkdownTextFragment(renderer, "last") {
            @Override
            public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            }
        };
        // Fragments draw things that reach to fragments after them, like the background of an inline code span
        MarkdownTextFragment first = new MarkdownTextFragment(renderer, "first") {
            @Override
            public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
                lastFragmentPositions.add(last.x);
                lastFragmentPositions.add(last.y);
            }
        };
        first.unscaledTextWidth = 30;
        last.unscaledTextWidth = 24;
        MarkdownTextLine line = new MarkdownTextLine(renderer);
        line.fragments.add(first);
        line.fragments.add(last);
        line.offsetX = 2;
        line.offsetY = 5;

        renderer.setX(100).setY(200);
        line.extractRenderState(null, 0, 0, 0);
        renderer.setX(150).setY(300);
        line.extractRenderState(null, 0, 0, 0);

        assertEquals(List.of(132.0F, 205.0F, 182.0F, 305.0F), lastFragmentPositions);
    }

    @Test
    void visibleLineLookupFindsTheSameLinesAsWalkingAllLines() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(document(110), 150);
        renderer.tick();
        assertTrue(renderer.lines.size() > 300);
        float textHeight = renderer.getRealHeight();
        Random random = new Random(42);

        for (int i = 0; i < 2000; i++) {
            float y = (random.nextFloat() * textHeight * 1.5F) - (textHeight * 1.2F);
            float top = (random.nextFloat() * 400.0F) - 100.0F;
            float bottom = top + (random.nextFloat() * 300.0F);
            assertVisibleLinesMatchFullWalk(renderer, y, top, bottom);
        }
        // Edges of the visible range that are exactly on the edges of lines
        for (MarkdownTextLine line : renderer.lines) {
            assertVisibleLinesMatchFullWalk(renderer, 0, line.offsetY, line.offsetY + line.getLineHeight());
            assertVisibleLinesMatchFullWalk(renderer, 0, line.offsetY + line.getLineHeight(), line.offsetY + line.getLineHeight());
        }
    }

    @Test
    void visibleLineLookupOnlyLooksAtTheLinesAroundTheVisibleRange() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(document(110), 150);
        renderer.setY(-2000);
        renderer.setVisibleVerticalRange(0, 100);
        renderer.tick();

        renderer.layout.updateVisibleLines();

        int visible = visibleLines(renderer).size();
        assertTrue(visible > 0);
        assertTrue((renderer.layout.visibleLinesEnd - renderer.layout.firstVisibleLine) <= (visible + 2));
    }

    @Test
    void everyLineIsVisibleWithoutAVisibleRange() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(document(20), 150);
        renderer.setY(-5000);
        renderer.tick();

        assertEquals(renderer.lines, visibleLines(renderer));
    }

    @Test
    void visibleLineLookupWorksWithLinesThatOverlap() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(document(40), 150);
        renderer.setLineSpacing(-30);
        renderer.tick();
        Random random = new Random(7);

        for (int i = 0; i < 500; i++) {
            float top = (random.nextFloat() * 3000.0F) - 3000.0F;
            assertVisibleLinesMatchFullWalk(renderer, 0, top, top + 60.0F);
        }
    }

    @Test
    void emptyTextStillHasOneLine() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("", 150);
        renderer.tick();

        assertEquals(1, renderer.lines.size());
        assertEquals(LINE_HEIGHT + 2 + 2 + 2, renderer.getRealHeight());
        assertFalse(renderer.fragments.isEmpty());
    }

    private static void assertVisibleLinesMatchFullWalk(MarkdownTestRenderer renderer, float y, float top, float bottom) {
        renderer.setY(y);
        renderer.setVisibleVerticalRange(top, bottom);
        List<MarkdownTextLine> expected = new ArrayList<>();
        for (MarkdownTextLine line : renderer.lines) {
            if ((y + line.offsetY + line.getLineHeight()) < top) continue;
            if ((y + line.offsetY) > bottom) continue;
            expected.add(line);
        }
        assertEquals(expected, visibleLines(renderer));
    }

    private static List<MarkdownTextLine> visibleLines(MarkdownTestRenderer renderer) {
        renderer.layout.updateVisibleLines();
        List<MarkdownTextLine> visible = new ArrayList<>();
        for (int i = renderer.layout.firstVisibleLine; i < renderer.layout.visibleLinesEnd; i++) {
            MarkdownTextLine line = renderer.lines.get(i);
            if (renderer.layout.isLineVisible(line)) visible.add(line);
        }
        return visible;
    }

    private static String document(int sections) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < sections; i++) {
            text.append("## Section ").append(i).append("\n\n");
            text.append("Paragraph ").append(i).append(" with **bold** words and `inline code` that wraps.\n\n");
            text.append("- item one\n  - nested item ").append(i).append("\n\n");
            if ((i % 3) == 0) text.append("> quote ").append(i).append("\n\n");
            if ((i % 3) == 1) text.append("```\ncode ").append(i).append("\nmore code\n```\n\n");
            if ((i % 3) == 2) text.append("| A | B |\n| --- | --- |\n| ").append(i).append(" | x |\n\n---\n\n");
        }
        return text.toString();
    }

}
