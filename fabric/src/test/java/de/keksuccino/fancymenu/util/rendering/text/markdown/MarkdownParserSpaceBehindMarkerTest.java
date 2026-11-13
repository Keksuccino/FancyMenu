package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Predicate;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A space right behind a marker that opens something is part of what the marker opens. It does not join the fragment in front of the marker,
 * which is how a space behind the text of a fragment ends the word of that fragment.
 */
class MarkdownParserSpaceBehindMarkerTest {

    private enum Opened {

        CODE("`", "`", fragment -> fragment.codeBlockContext != null),
        PLAIN_TEXT(";;", ";;", fragment -> fragment.plainText),
        BOLD("**", "**", fragment -> fragment.bold),
        ITALIC_ASTERISK("*", "*", fragment -> fragment.italic),
        ITALIC_UNDERSCORE("_", "_", fragment -> fragment.italic),
        STRIKETHROUGH("~", "~", fragment -> fragment.strikethrough),
        STRIKETHROUGH_DOUBLE("~~", "~~", fragment -> fragment.strikethrough),
        LINK("[", "](target)", fragment -> fragment.hyperlink != null),
        CLICK_EVENT("[", "](click:event)", fragment -> fragment.clickEvent != null),
        FONT("%!!uniform%", "%!!%", fragment -> fragment.font != null),
        COLOR("%#ff0000%", "%#%", fragment -> fragment.textColor != null);

        private final String opening;
        private final String closing;
        private final Predicate<MarkdownTextFragment> applied;

        Opened(String opening, String closing, Predicate<MarkdownTextFragment> applied) {
            this.opening = opening;
            this.closing = closing;
            this.applied = applied;
        }

    }

    @ParameterizedTest
    @EnumSource(Opened.class)
    void spaceBehindAnOpeningMarkerBelongsToWhatItOpens(Opened opened) {
        List<MarkdownTextFragment> fragments = parse("before " + opened.opening + " inside" + opened.closing + " after");

        // The space behind the closing marker ends the word in front of that marker, like it does for every other word
        assertEquals(List.of("before ", " ", "inside ", "after"), texts(fragments));
        assertFalse(opened.applied.test(fragments.get(0)));
        assertTrue(opened.applied.test(fragments.get(1)));
        assertTrue(opened.applied.test(fragments.get(2)));
        assertFalse(opened.applied.test(fragments.get(3)));
    }

    @ParameterizedTest
    @EnumSource(Opened.class)
    void spaceBehindAnOpeningMarkerDoesNotEndTheWordInFrontOfTheMarker(Opened opened) {
        List<MarkdownTextFragment> fragments = parse("before" + opened.opening + " inside" + opened.closing + " after");

        assertEquals(List.of("before", " ", "inside ", "after"), texts(fragments));
        assertFalse(fragments.get(0).endOfWord);
        assertTrue(opened.applied.test(fragments.get(1)));
    }

    @Test
    void rowOfSpacesBehindAnOpeningMarkerStaysTogether() {
        List<MarkdownTextFragment> fragments = parse("before `   inside` after");

        assertEquals(List.of("before ", "   ", "inside ", "after"), texts(fragments));
        assertTrue(fragments.get(1).codeBlockContext != null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"#  text", ">  text", "-  text"})
    void spaceBehindThePrefixOfALineStaysInItsLine(String line) {
        List<MarkdownTextFragment> firstLine = parse(line);
        List<MarkdownTextFragment> laterLine = parse("first\n" + line);

        assertEquals(List.of(" ", "text"), texts(firstLine));
        assertEquals(List.of("first", " ", "text"), texts(laterLine));
        assertTrue(laterLine.get(0).naturalLineBreakAfter);
        MarkdownTextFragment space = laterLine.get(1);
        assertTrue((space.headlineType != MarkdownTextFragment.HeadlineType.NONE) || (space.quoteContext != null) || (space.bulletListLevel == 1));
    }

    @Test
    void spaceBehindAClosingMarkerStillEndsTheWordInFrontOfIt() {
        List<MarkdownTextFragment> fragments = parse("**bold** after");

        assertEquals("bold ", fragments.get(1).text);
        assertTrue(fragments.get(1).bold);
        assertTrue(fragments.get(1).endOfWord);
    }

}
