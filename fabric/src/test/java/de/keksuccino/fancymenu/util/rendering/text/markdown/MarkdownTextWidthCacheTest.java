package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTextWidthCacheTest {

    @Test
    void parsingTheSameTextAgainMeasuresNothing() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("alpha **beta** gamma\n\n- [delta link](https://example.invalid) `epsilon`", 400);
        renderer.tick();
        int measuredFragments = renderer.testFont().measuredFragmentTexts.size();
        assertTrue(measuredFragments > 0);

        renderer.refreshRenderer();
        renderer.tick();

        assertEquals(2, renderer.parses);
        assertEquals(measuredFragments, renderer.testFont().measuredFragmentTexts.size());
    }

    @Test
    void onlyChangedWordsGetMeasuredOnTheNextParse() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("alpha beta gamma delta", 400);
        renderer.tick();
        int measuredFragments = renderer.testFont().measuredFragmentTexts.size();

        renderer.setText("alpha beta omega delta");
        renderer.tick();

        List<String> measuredTexts = renderer.testFont().measuredFragmentTexts;
        List<String> newlyMeasured = measuredTexts.subList(measuredFragments, measuredTexts.size());
        assertFalse(newlyMeasured.isEmpty());
        for (String text : newlyMeasured) assertTrue(text.contains("omega"));
    }

    @Test
    void sameWordInADifferentStyleGetsMeasuredOnItsOwn() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("word", 400);
        renderer.tick();
        int measuredFragments = renderer.testFont().measuredFragmentTexts.size();

        renderer.setText("**word**");
        renderer.tick();

        assertTrue(renderer.testFont().measuredFragmentTexts.size() > measuredFragments);
    }

    @Test
    void widthsOfWordsThatLeftTheTextGetForgotten() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("alpha beta", 400);
        renderer.tick();
        renderer.setText("alpha");
        renderer.tick();
        int measuredFragments = renderer.testFont().measuredFragmentTexts.size();

        renderer.setText("alpha beta");
        renderer.tick();

        List<String> measuredTexts = renderer.testFont().measuredFragmentTexts;
        List<String> newlyMeasured = measuredTexts.subList(measuredFragments, measuredTexts.size());
        assertTrue(newlyMeasured.stream().anyMatch(text -> text.contains("beta")));
    }

    @Test
    void cachedWidthsMatchAFreshMeasurement() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("alpha **beta** [gamma link](click:event) alpha **beta**", 400);
        renderer.tick();
        renderer.refreshRenderer();
        renderer.tick();

        for (MarkdownTextFragment fragment : renderer.fragments) {
            assertEquals(renderer.getUnscaledTextWidth(fragment.buildRenderComponent(true)), fragment.unscaledTextWidth);
        }
    }

    @Test
    void differentTextMeasuringMakesEveryWordGetMeasuredAgain() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("alpha beta gamma", 400);
        renderer.tick();
        int measuredFragments = renderer.testFont().measuredFragmentTexts.size();

        renderer.testFont().charWidth = 7;
        renderer.refreshRenderer();
        renderer.tick();

        assertEquals(measuredFragments * 2, renderer.testFont().measuredFragmentTexts.size());
        for (MarkdownTextFragment fragment : renderer.fragments) {
            assertEquals(fragment.text.length() * 7, fragment.unscaledTextWidth);
        }
    }

    @Test
    void fragmentsMeasuredOutsideOfAParseAreNotKept() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("alpha", 400);
        renderer.tick();
        MarkdownTextFragment fragment = renderer.fragments.get(0);
        int measuredFragments = renderer.testFont().measuredFragmentTexts.size();

        fragment.text = "changed";
        fragment.updateWidth();
        fragment.updateWidth();

        assertEquals(measuredFragments + 2, renderer.testFont().measuredFragmentTexts.size());
        assertEquals(7 * 6, fragment.unscaledTextWidth);
    }

}
