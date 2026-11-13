package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTextRenderCoordinateTest {

    private static final float EPSILON = 0.0001F;
    private static final String TEXT = "plain\n- item\n\n```\ncode\n```";

    @ParameterizedTest
    @ValueSource(floats = {0.5F, 0.75F, 1.0F, 1.2F, 1.6F, 2.0F})
    void renderCoordinatesAreTheTextOriginDividedByTheTextScale(float scale) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 2000);
        renderer.setTextBaseScale(scale);
        renderer.setX(10.25F).setY(-3.5F);
        renderer.tick();

        boolean textStartsBehindItsFragment = false;
        for (MarkdownTextFragment fragment : renderer.fragments) {
            assertEquals(scale, fragment.getScale());
            assertEquals(fragment.getTextX() / scale, fragment.getTextRenderX(), EPSILON);
            assertEquals(fragment.getTextY() / scale, fragment.getTextRenderY(), EPSILON);
            if (fragment.getTextX() > fragment.x) textStartsBehindItsFragment = true;
        }
        // The item and the code block keep room in front of their text, so not every origin is the position of its fragment
        assertTrue(textStartsBehindItsFragment);
    }

    @ParameterizedTest
    @ValueSource(floats = {0.5F, 0.75F, 1.0F, 1.2F, 1.6F, 2.0F})
    void textWithoutRoomInFrontOfItStartsExactlyAtTheFractionalPositionOfItsFragment(float scale) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 2000);
        renderer.setTextBaseScale(scale);
        renderer.setX(17.375F).setY(-4.625F);
        renderer.tick();
        MarkdownTextFragment plain = renderer.fragmentContaining("plain");

        assertEquals(17.375F + renderer.scaling.padding(), plain.x);
        assertEquals(-4.625F + renderer.scaling.padding(), plain.y);
        assertEquals(plain.x, plain.getTextX());
        assertEquals(plain.y, plain.getTextY());
    }

    @ParameterizedTest
    @ValueSource(floats = {0.0F, -0.5F, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void brokenTextScaleKeepsTheTextOriginAtItsFragmentAndTheRenderCoordinatesAtZero(float scale) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 2000);
        renderer.setTextBaseScale(scale);
        renderer.setX(7.25F).setY(7.25F);
        renderer.tick();

        for (MarkdownTextFragment fragment : renderer.fragments) {
            assertEquals(0.0F, fragment.getScale());
            // Room in front of text grows with the text, so text without a size has none
            assertEquals(fragment.x, fragment.getTextX());
            assertEquals(0.0F, fragment.getTextRenderX());
            assertEquals(0.0F, fragment.getTextRenderY());
        }
    }

    @ParameterizedTest
    @ValueSource(floats = {0.5F, 0.67F, 0.75F, 0.87F, 1.2F, 1.6F, 2.0F})
    void roomInFrontOfTextGrowsWithTheTextScaleExactlyOnce(float scale) {
        MarkdownTestRenderer unscaled = new MarkdownTestRenderer(TEXT, 2000);
        unscaled.setX(101.25F).setY(-43.375F);
        unscaled.tick();
        MarkdownTestRenderer scaled = new MarkdownTestRenderer(TEXT, 2000);
        scaled.setTextBaseScale(scale);
        scaled.setX(101.25F).setY(-43.375F);
        scaled.tick();

        for (String text : new String[]{"item", "code"}) {
            MarkdownTextFragment unscaledFragment = unscaled.fragmentContaining(text);
            MarkdownTextFragment scaledFragment = scaled.fragmentContaining(text);
            float room = unscaledFragment.getTextX() - unscaledFragment.x;

            assertTrue(room > 0.0F);
            assertEquals(room * scale, scaledFragment.getTextX() - scaledFragment.x, EPSILON);
        }
    }

    @Test
    void smallestAndBiggestTextScaleStayAsTheyAreAndAScaleThatOverflowsCountsAsBroken() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 2000);

        renderer.setTextBaseScale(Float.MIN_VALUE);
        assertEquals(Float.MIN_VALUE, renderer.scaling.textScale(1.0F));

        renderer.setTextBaseScale(Float.MAX_VALUE);
        assertEquals(Float.MAX_VALUE, renderer.scaling.textScale(1.0F));
        // A headline is twice as big as normal text, which no number can hold at this scale
        assertEquals(0.0F, renderer.scaling.textScale(2.0F));
    }

    @Test
    void mouseIsOverTextFromItsOriginUpToItsFarEdges() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 2000);
        renderer.setTextBaseScale(1.6F);
        renderer.setX(5.75F).setY(9.25F);
        renderer.tick();
        MarkdownTextFragment item = renderer.fragmentContaining("item");
        float x = item.getTextX();
        float y = item.getTextY();
        float width = item.getTextWidth();
        float height = item.getTextHeight();

        // The text of the item starts behind its fragment, and the mouse only is over the text
        assertTrue(x > item.x);
        assertTrue(item.isMouseOver(x, y));
        assertTrue(item.isMouseOver(x + width - 0.001F, y + height - 0.001F));
        assertFalse(item.isMouseOver(x - 0.001F, y));
        assertFalse(item.isMouseOver(x, y - 0.001F));
        assertFalse(item.isMouseOver(x + width, y));
        assertFalse(item.isMouseOver(x, y + height));
    }

}
