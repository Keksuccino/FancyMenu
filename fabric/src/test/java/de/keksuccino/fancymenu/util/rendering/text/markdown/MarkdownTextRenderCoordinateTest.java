package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

}
