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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The prefix of a font ("%!!font_name%") and of a color ("%#ff0000%") is no text: nothing inside of it is a marker for something else.
 */
class MarkdownParserFontPrefixTest {

    @Test
    void underscoreInTheNameOfAFontClosesNoItalicText() {
        List<MarkdownTextFragment> fragments = parse("_first %!!my_font%font%!!% second_ rest");
        MarkdownTextFragment font = fragmentContaining(fragments, "font");

        assertEquals("first font second rest", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "first").italic);
        assertTrue(font.italic);
        assertEquals("my_font", font.font.getPath());
        assertTrue(fragmentContaining(fragments, "second").italic);
        assertNull(fragmentContaining(fragments, "second").font);
        assertFalse(fragmentContaining(fragments, "rest").italic);
    }

    @Test
    void underscoreInTheNameOfAFontIsNoClosingMarkerForAnUnderscoreInFrontOfIt() {
        List<MarkdownTextFragment> fragments = parse("_first %!!my_font%font%!!% rest");

        assertEquals("_first font rest", joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.italic));
        assertNotNull(fragmentContaining(fragments, "font").font);
        assertNull(fragmentContaining(fragments, "rest").font);
    }

    @Test
    void underscoreInAFontPrefixWithoutSuffixIsNoMarkerEither() {
        List<MarkdownTextFragment> fragments = parse("_first %!!my_font% second_ rest");

        assertEquals("first %!!my_font% second rest", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "%!!my_font%").italic);
        assertTrue(fragmentContaining(fragments, "second").italic);
        assertFalse(fragmentContaining(fragments, "rest").italic);
        assertTrue(fragments.stream().allMatch(fragment -> fragment.font == null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%!!uniform%!!%", "%!!uniform%!!% text", "%#ff0000%#%", "%#ff0000%#% text", "before %!!my_font%!!% text"})
    void endOfAPrefixIsNoStartOfTheSuffixThatClosesIt(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals(text, joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) {
            assertNull(fragment.font);
            assertNull(fragment.textColor);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"%!!uniform%%!!%text", "%#ff0000%%#%text"})
    void suffixRightBehindItsPrefixClosesIt(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        assertEquals("text", joinText(fragments));
        assertNull(fragmentContaining(fragments, "text").font);
        assertNull(fragmentContaining(fragments, "text").textColor);
    }

    @Test
    void fontWithAnUnderscoreInItsNameStillApplies() {
        List<MarkdownTextFragment> fragments = parse("%!!my_font%font%!!% _italic_");

        assertEquals("my_font", fragmentContaining(fragments, "font").font.getPath());
        assertTrue(fragmentContaining(fragments, "italic").italic);
        assertNull(fragmentContaining(fragments, "italic").font);
    }

}
