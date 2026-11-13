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

    /**
     * The words say how they have to be formatted: "bold", "italic", "both" or "none".
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "***both*** none", "***both** italic* none", "***both* bold** none", "**bold *both*** none", "*italic **both*** none",
            "***both** italic **both*** none", "***both* bold *both*** none", "none ***both** italic* none ***both* bold** none",
            "**bold *both* bold** none", "*italic **both** italic* none", "**bold***italic* none", "*italic***bold** none",
            "**bold** *italic* ***both*** none", "_italic **both**_ none", "**_both_ bold** none", "***both** italic\nitalic* none"
    })
    void asterisksInARowOpenAndCloseBoldAndItalicTextInEveryOrder(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals(text.replace("*", "").replace("_", "").replace("\n", ""), joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) {
            String word = fragment.text.trim();
            if (word.isEmpty()) continue;
            assertEquals(word.equals("bold") || word.equals("both"), fragment.bold, () -> text + " <" + fragment.text + ">");
            assertEquals(word.equals("italic") || word.equals("both"), fragment.italic, () -> text + " <" + fragment.text + ">");
        }
    }

    @Test
    void thirdAsteriskOfARowStaysTextWithoutASingleAsteriskToCloseItalicText() {
        List<MarkdownTextFragment> fragments = parse("***bold** and more");

        for (MarkdownTextFragment fragment : fragments) assertFalse(fragment.italic);
        assertTrue(fragmentContaining(fragments, "bold").bold);
        assertFalse(fragmentContaining(fragments, "more").bold);
        assertEquals("*bold and more", joinText(fragments));
    }

    @Test
    void pairOfAsterisksThatOpensNoBoldTextClosesItalicTextWithItsSecondAsterisk() {
        List<MarkdownTextFragment> fragments = parse("*italic **rest");

        assertTrue(fragmentContaining(fragments, "italic").italic);
        assertFalse(fragmentContaining(fragments, "rest").italic);
        assertEquals("italic *rest", joinText(fragments));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "snake_case and [link](https://example.invalid/some_page) and more",
            "2 * 3 and [link](https://example.invalid/*) and more",
            "about ~5 and [link](https://example.invalid/~user) and more",
            "about ~~5 and [link](https://example.invalid/~~user) and more",
            "**open and [link](https://example.invalid/**) and more",
            "snake_case and [click](click:some_event) and more",
            "snake_case\n![alternative text](some_image.png)\nand more",
            "snake_case\n![alternative_text](image.png)\nand more",
            "snake_case\n[![alternative_text](some_image.png)](https://example.invalid/some_page)\nand more"
    })
    void markerInsideALinkTargetOrAnImageLineClosesNothing(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        for (MarkdownTextFragment fragment : fragments) {
            assertFalse(fragment.bold);
            assertFalse(fragment.italic);
            assertFalse(fragment.strikethrough);
        }
        assertTrue(joinText(fragments).endsWith("and more"));
        // The text in front of the link or image keeps its marker
        int textEnd = text.length();
        for (char linkOrLineStart : new char[] {'[', '!', '\n'}) {
            int index = text.indexOf(linkOrLineStart);
            if ((index != -1) && (index < textEnd)) textEnd = index;
        }
        assertTrue(joinText(fragments).startsWith(text.substring(0, textEnd)));
    }

    @Test
    void markerBehindALinkStillCloses() {
        List<MarkdownTextFragment> fragments = parse("_italic [link](https://example.invalid/some_page) italic_ and more");

        assertTrue(fragmentContaining(fragments, "italic").italic);
        assertTrue(fragmentContaining(fragments, "link").italic);
        assertEquals("https://example.invalid/some_page", fragmentContaining(fragments, "link").hyperlink.link);
        assertFalse(fragmentContaining(fragments, "more").italic);
        assertEquals("italic link italic and more", joinText(fragments));
    }

    @Test
    void markerInTextThatOnlyLooksLikeALinkTargetStaysAMarker() {
        // No link can end at a closing square bracket that is escaped, so this is no target
        List<MarkdownTextFragment> fragments = parse("_italic \\](some_text) italic_ and more");

        assertFalse(fragmentContaining(fragments, "more").italic);
        assertTrue(fragmentContaining(fragments, "italic").italic);
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
