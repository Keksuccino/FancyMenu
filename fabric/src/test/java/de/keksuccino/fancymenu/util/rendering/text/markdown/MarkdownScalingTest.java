package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownScalingTest {

    private static final int CHAR_WIDTH = 6;
    private static final int LINE_HEIGHT = 9;

    @ParameterizedTest
    @CsvSource({"1.0, 1.0", "1.0, 2.0", "1.0, 3.0", "1.6, 3.0", "1.2, 3.0", "0.5, 3.0", "0.1, 2.0", "1.3, 2.5", "2.0, 1.5", "0.75, 4.0", "3.7, 1.0"})
    void snappedTextScaleMakesOneFontPixelCoverAWholeNumberOfScreenPixels(float baseScale, float renderScale) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("# headline\ntext", 2000);
        renderer.setParentRenderScale(renderScale);
        renderer.getStyle().snapTextScale.set(true);
        renderer.setTextBaseScale(baseScale);
        renderer.tick();

        for (MarkdownTextFragment fragment : renderer.fragments) {
            float screenPixelsPerFontPixel = fragment.getScale() * renderScale;
            assertEquals(Math.round(screenPixelsPerFontPixel), screenPixelsPerFontPixel, 0.0001F);
            assertTrue(screenPixelsPerFontPixel >= 1.0F, "a font pixel is smaller than a screen pixel");
            // Rounding moves the size by half a screen pixel at most, except where it keeps text from vanishing
            float wanted = ((fragment.headlineType == MarkdownTextFragment.HeadlineType.NONE) ? 1.0F : 2.0F) * baseScale * renderScale;
            assertTrue(Math.abs(screenPixelsPerFontPixel - Math.max(1.0F, wanted)) <= 0.5001F, "the scale got rounded too far");
        }
    }

    @Test
    void textScaleStaysAsItIsWithoutSnapping() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("# headline\ntext", 2000);
        renderer.setTextBaseScale(1.3F);
        renderer.tick();

        assertEquals(1.3F, renderer.fragmentContaining("text").getScale());
        assertEquals(2.6F, renderer.fragmentContaining("headline").getScale());
    }

    @Test
    void brokenTextScaleHidesTheTextInsteadOfBreakingTheLayout() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("text", 2000);
        renderer.setParentRenderScale(2.0F);
        renderer.getStyle().snapTextScale.set(true);

        for (float scale : new float[]{0.0F, -1.0F, Float.NaN, Float.POSITIVE_INFINITY}) {
            renderer.setTextBaseScale(scale);
            renderer.tick();
            assertEquals(0.0F, renderer.fragmentContaining("text").getScale());
        }
    }

    @Test
    void changedRenderScaleLaysTheLinesOutAgainOnlyWhileTextScalesGetSnapped() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("text", 2000);
        renderer.setParentRenderScale(2.0F);
        renderer.tick();
        renderer.setParentRenderScale(3.0F);
        renderer.tick();
        assertEquals(1, renderer.layouts);

        renderer.getStyle().snapTextScale.set(true);
        renderer.setTextBaseScale(1.2F);
        renderer.tick();
        assertEquals(2, renderer.layouts);
        assertEquals(4.0F / 3.0F, renderer.fragmentContaining("text").getScale());

        renderer.setParentRenderScale(2.0F);
        renderer.tick();
        assertEquals(3, renderer.layouts);
        assertEquals(1.0F, renderer.fragmentContaining("text").getScale());

        renderer.tick();
        assertEquals(3, renderer.layouts);
    }

    @Test
    void sizesOfTheStyleOnlyGrowWithTheTextIfTheStyleSaysSo() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("one\ntwo", 2000);
        renderer.setBorder(5);
        renderer.setLineSpacing(3);
        renderer.setTextBaseScale(2.0F);
        renderer.tick();
        MarkdownTextLine one = renderer.fragmentContaining("one").parentLine;
        MarkdownTextLine two = renderer.fragmentContaining("two").parentLine;

        assertEquals(5, one.offsetY);
        assertEquals(5, one.offsetX);
        assertEquals(5 + (LINE_HEIGHT * 2) + 3, two.offsetY);

        renderer.getStyle().scaleSizesWithText.set(true);
        renderer.tick();
        one = renderer.fragmentContaining("one").parentLine;
        two = renderer.fragmentContaining("two").parentLine;

        assertEquals(10, one.offsetY);
        assertEquals(10, one.offsetX);
        assertEquals(10 + (LINE_HEIGHT * 2) + 6, two.offsetY);
        assertEquals(("one".length() * CHAR_WIDTH * 2) + 20, renderer.realWidth);
    }

    @Test
    void snappedPositionsPutEveryLineOnWholeScreenPixels() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("one\n^^^\ntwo\n^^^\n- item\n\n> quote", 201);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.setX(10.4F).setY(-37.77F);
        renderer.tick();

        for (MarkdownTextLine line : renderer.lines) {
            assertOnScreenPixel(renderer.getX() + renderer.getRenderOffsetX(line), 3.0F);
            assertOnScreenPixel(line.getRenderY(), 3.0F);
            for (MarkdownTextFragment fragment : line.fragments) {
                assertEquals(line.getRenderY(), fragment.getRenderLineY());
            }
            // Positions get rounded down, by less than one screen pixel
            float moved = (renderer.getY() + line.offsetY) - line.getRenderY();
            assertTrue((moved >= 0.0F) && (moved < (1.0F / 3.0F) + 0.0001F));
        }
    }

    @Test
    void positionsStayAsTheyAreWithoutSnapping() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("one\ntwo", 200);
        renderer.setX(10.4F).setY(-37.77F);
        renderer.tick();

        for (MarkdownTextLine line : renderer.lines) {
            assertEquals(renderer.getY() + line.offsetY, line.getRenderY());
            assertEquals(line.offsetX, renderer.getRenderOffsetX(line));
        }
    }

    @Test
    void boldTextGetsItsOwnColorExceptInHeadlines() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("plain **bold** text\n# headline **strong**", 2000);
        renderer.tick();
        assertFalse(renderer.getStyle().boldTextColor.isSet());
        assertEquals(null, renderer.fragmentContaining("bold").getRenderComponent().getStyle().getColor());

        renderer.getStyle().boldTextColor.set(DrawableColor.of(1, 2, 3));

        assertEquals(0x010203, renderer.fragmentContaining("bold").getRenderComponent().getStyle().getColor().getValue());
        assertEquals(null, renderer.fragmentContaining("plain").getRenderComponent().getStyle().getColor());
        assertEquals(null, renderer.fragmentContaining("strong").getRenderComponent().getStyle().getColor());
        assertEquals(1, renderer.parses);
        assertEquals(1, renderer.layouts);
    }

    private static void assertOnScreenPixel(float guiPosition, float renderScale) {
        float screenPosition = guiPosition * renderScale;
        assertEquals(Math.round(screenPosition), screenPosition, 0.001F);
    }

}
