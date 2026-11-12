package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownRendererRefreshTest {

    private static final String TEXT = "# Headline\n\nSome words that are long enough to wrap around when the renderer gets narrow enough for it.\n\n- a bullet item\n\n> a quoted line\n\n| A | B |\n| --- | --- |\n| 1 | 2 |\n\nlast line";

    @Test
    void settersBeforeTheFirstQueryCauseOneParseAndOneLayout() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer();
        renderer.setText(TEXT);
        renderer.setOptimalWidth(200);
        renderer.setTextBaseScale(1.5F);
        renderer.setBorder(4);
        renderer.setLineSpacing(3);
        renderer.setQuoteIndent(10);
        renderer.setBulletListIndent(10);
        renderer.setBulletListSpacing(4);
        renderer.setTableCellPadding(6);
        renderer.setParseMarkdown(false);
        renderer.setParseMarkdown(true);
        renderer.setAutoLineBreakingEnabled(false);
        renderer.refreshRenderer();

        assertEquals(0, renderer.parses);
        assertEquals(0, renderer.layouts);

        renderer.getRealHeight();
        renderer.getRealWidth();
        renderer.tick();

        assertEquals(1, renderer.parses);
        assertEquals(1, renderer.layouts);
    }

    @Test
    void widthChangeLaysOutAgainWithoutParsingAgain() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 2000);
        renderer.tick();
        List<MarkdownTextFragment> fragmentsBefore = List.copyOf(renderer.fragments);
        List<MarkdownTextLine> linesBefore = List.copyOf(renderer.lines);
        float heightBefore = renderer.getRealHeight();

        renderer.setOptimalWidth(120);
        float heightAfter = renderer.getRealHeight();

        assertEquals(1, renderer.parses);
        assertEquals(2, renderer.layouts);
        assertEquals(fragmentsBefore.size(), renderer.fragments.size());
        for (int i = 0; i < fragmentsBefore.size(); i++) {
            assertSame(fragmentsBefore.get(i), renderer.fragments.get(i));
        }
        assertTrue(renderer.lines.size() > linesBefore.size());
        assertTrue(heightAfter > heightBefore);
        for (MarkdownTextLine line : renderer.lines) {
            for (MarkdownTextFragment fragment : line.fragments) assertSame(line, fragment.parentLine);
        }
    }

    @ParameterizedTest
    @MethodSource("layoutSettings")
    void layoutSettingCausesOneLayoutAndNoParse(Consumer<MarkdownRenderer> setting) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 200);
        renderer.tick();

        setting.accept(renderer);
        renderer.tick();
        renderer.getRealWidth();

        assertEquals(1, renderer.parses);
        assertEquals(2, renderer.layouts);

        // Setting the same value again changes nothing
        setting.accept(renderer);
        renderer.tick();

        assertEquals(2, renderer.layouts);
    }

    static Stream<Arguments> layoutSettings() {
        return Stream.<Consumer<MarkdownRenderer>>of(
                renderer -> renderer.setOptimalWidth(150),
                renderer -> renderer.setTextBaseScale(2.0F),
                renderer -> renderer.setBorder(8),
                renderer -> renderer.setLineSpacing(7),
                renderer -> renderer.setQuoteIndent(20),
                renderer -> renderer.setBulletListIndent(20),
                renderer -> renderer.setBulletListSpacing(9),
                renderer -> renderer.setTableCellPadding(3),
                renderer -> renderer.setTableLineThickness(2),
                renderer -> renderer.setTableMargin(10),
                renderer -> renderer.setAutoLineBreakingEnabled(false)
        ).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parseSettings")
    void parseSettingCausesOneParse(Consumer<MarkdownRenderer> setting) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 200);
        renderer.tick();

        setting.accept(renderer);
        renderer.tick();

        assertEquals(2, renderer.parses);
        assertEquals(2, renderer.layouts);

        setting.accept(renderer);
        renderer.tick();

        assertEquals(2, renderer.parses);
    }

    static Stream<Arguments> parseSettings() {
        return Stream.<Consumer<MarkdownRenderer>>of(
                renderer -> renderer.setParseMarkdown(false),
                renderer -> renderer.setTextCase(MarkdownRenderer.TextCase.ALL_UPPER),
                renderer -> renderer.setQuoteItalic(true)
        ).map(Arguments::of);
    }

    @Test
    void settingsThatOnlyChangeDrawingCauseNoWork() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 200);
        renderer.tick();

        renderer.setX(50).setY(70);
        renderer.setTextBaseColor(DrawableColor.BLACK);
        renderer.setTextShadow(false);
        renderer.setTextOpacity(0.5F);
        renderer.setHyperlinkColor(DrawableColor.BLACK);
        renderer.setQuoteColor(DrawableColor.BLACK);
        renderer.setCodeBlockMultiLineColor(DrawableColor.BLACK);
        renderer.setTableLineColor(DrawableColor.BLACK);
        renderer.setParentRenderScale(2.0F);
        renderer.setVisibleVerticalRange(0, 100);
        renderer.tick();
        renderer.getRealHeight();

        assertEquals(1, renderer.parses);
        assertEquals(1, renderer.layouts);
    }

    @Test
    void idleFramesDoNothing() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 200);
        for (int i = 0; i < 10; i++) {
            renderer.setOptimalWidth(200);
            renderer.getRealWidth();
            renderer.getRealHeight();
            renderer.tick();
        }

        assertEquals(1, renderer.parses);
        assertEquals(1, renderer.layouts);
    }

    @Test
    void settingABorderBelowTheMinimumEveryFrameCausesNoWork() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 200);
        renderer.tick();

        // Owners sync their own border setting every frame, and theirs can be smaller than the minimum the renderer keeps
        for (int i = 0; i < 5; i++) {
            renderer.setBorder(0);
            renderer.tick();
        }

        assertEquals(2.0F, renderer.getBorder());
        assertEquals(1, renderer.parses);
        assertEquals(1, renderer.layouts);
    }

    @Test
    void sizeQueryShowsANewTextWithoutWaitingForTheNextFrame() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("one line", 200);
        float oneLineHeight = renderer.getRealHeight();

        renderer.setText("one line\nsecond line\nthird line");

        assertTrue(renderer.getRealHeight() > oneLineHeight);
        assertEquals(3, renderer.lines.size());
    }

    @Test
    void settingTheSameTextAgainDoesNotParseAgain() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 200);
        renderer.tick();

        renderer.setText(TEXT);
        renderer.tick();

        assertEquals(1, renderer.parses);
        assertEquals(1, renderer.layouts);
    }

    @Test
    void refreshRendererParsesAgain() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 200);
        renderer.tick();
        MarkdownTextFragment fragmentBefore = renderer.fragments.get(0);

        renderer.refreshRenderer();
        renderer.tick();

        assertEquals(2, renderer.parses);
        assertEquals(2, renderer.layouts);
        assertNotSame(fragmentBefore, renderer.fragments.get(0));
    }

    @Test
    void skipRefreshKeepsFragmentsAndLinesUntilItGetsCleared() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("before", 200);
        renderer.tick();

        renderer.skipRefresh = true;
        renderer.setText("after the change\nwith a second line");
        renderer.setOptimalWidth(400);
        renderer.refreshRenderer();
        renderer.tick();
        renderer.getRealHeight();

        assertEquals(1, renderer.parses);
        assertEquals(1, renderer.layouts);
        assertEquals("before", renderer.fragments.get(0).text);

        renderer.skipRefresh = false;
        renderer.getRealHeight();

        assertEquals(2, renderer.parses);
        assertEquals(2, renderer.layouts);
        assertEquals(2, renderer.lines.size());
    }

    @Test
    void changingHtmlBreakRemovalBuildsTheRenderTextAgain() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("first<br>second", 200);
        renderer.tick();
        assertEquals("firstsecond", renderer.fragments.get(0).text);

        renderer.setRemoveHtmlBreaks(false);
        renderer.tick();

        assertEquals("first<br>second", renderer.fragments.get(0).text);
        assertEquals(2, renderer.parses);
    }

    @Test
    void realSizeCoversAllLines() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 200);
        renderer.setLineSpacing(3);
        renderer.setBorder(5);
        float expectedHeight = 0;
        float expectedWidth = 0;
        renderer.tick();
        for (MarkdownTextLine line : renderer.lines) {
            expectedHeight += line.getLineHeight() + 3;
            expectedWidth = Math.max(expectedWidth, line.getLineWidth());
        }

        assertEquals(expectedHeight + 10, renderer.getRealHeight());
        assertEquals(Math.max(expectedWidth + 10, 200), renderer.getRealWidth());
    }

}
