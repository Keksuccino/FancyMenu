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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asterisks in pairs open and close bold text, so a single asterisk only opens italic text if a later asterisk is left to close it.
 */
class MarkdownParserEmphasisTest {

    @ParameterizedTest
    @ValueSource(strings = {"5 * 3 is **fifteen** and more", "*.png and **bold** and more", "a * b **c** d **e** and more", "**bold** a * b **bold** and more"})
    void singleAsteriskWithOnlyBoldTextBehindItOpensNoItalicText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        for (MarkdownTextFragment fragment : fragments) assertFalse(fragment.italic);
        assertEquals(text.replace("**", ""), joinText(fragments));
        assertFalse(fragmentContaining(fragments, "more").bold);
    }

    @Test
    void singleAsteriskInsideBoldTextOpensNoItalicTextThatOutlivesTheBoldText() {
        List<MarkdownTextFragment> fragments = parse("**bold *text** and more");

        for (MarkdownTextFragment fragment : fragments) assertFalse(fragment.italic);
        assertTrue(fragmentContaining(fragments, "bold").bold);
        assertTrue(fragmentContaining(fragments, "*text").bold);
        assertFalse(fragmentContaining(fragments, "more").bold);
        assertEquals("bold *text and more", joinText(fragments));
    }

    @Test
    void italicTextAroundBoldTextEndsAtTheSingleAsteriskBehindIt() {
        List<MarkdownTextFragment> fragments = parse("*outer **both** outer* and more");

        assertTrue(fragmentContaining(fragments, "outer").italic);
        assertFalse(fragmentContaining(fragments, "outer").bold);
        assertTrue(fragmentContaining(fragments, "both").italic);
        assertTrue(fragmentContaining(fragments, "both").bold);
        assertFalse(fragmentContaining(fragments, "more").italic);
        assertEquals("outer both outer and more", joinText(fragments));
    }

    @Test
    void italicTextInsideBoldTextEndsInsideOfIt() {
        List<MarkdownTextFragment> fragments = parse("**outer *both* outer** and ***all*** more");

        assertTrue(fragmentContaining(fragments, "both").italic);
        assertTrue(fragmentContaining(fragments, "both").bold);
        assertFalse(fragmentContaining(fragments, "outer").italic);
        assertTrue(fragmentContaining(fragments, "all").italic);
        assertTrue(fragmentContaining(fragments, "all").bold);
        assertFalse(fragmentContaining(fragments, "more").italic);
        assertFalse(fragmentContaining(fragments, "more").bold);
        assertEquals("outer both outer and all more", joinText(fragments));
    }

    @Test
    void pairOfAsterisksThatOpensNoBoldTextClosesItalicTextWithItsSecondAsterisk() {
        List<MarkdownTextFragment> fragments = parse("*italic **rest");

        assertTrue(fragmentContaining(fragments, "italic").italic);
        assertFalse(fragmentContaining(fragments, "rest").italic);
        assertEquals("italic *rest", joinText(fragments));
    }

    @Test
    void italicTextsNextToEachOtherAndSingleAsterisksWithoutPartnerStillWork() {
        List<MarkdownTextFragment> fragments = parse("*one* and *two* and * three");

        assertTrue(fragmentContaining(fragments, "one").italic);
        assertTrue(fragmentContaining(fragments, "two").italic);
        assertFalse(fragmentContaining(fragments, "three").italic);
        assertEquals("one and two and * three", joinText(fragments));
    }

}
