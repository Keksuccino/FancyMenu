package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Code and plain text show their characters as they are, so a format marker inside of them is text: it closes no format
 * that is open around it, and it is no closing marker for a format in front of it.
 */
class MarkdownParserMarkerInCodeTest {

    /** Everything that shows its content as it is. "TEXT" stands for the content. */
    private static final String[] LITERAL_TEXTS = {" `TEXT` ", " ;;TEXT;; ", "\n```\nTEXT\n```\n", "\n;;;\nTEXT\n;;;\n"};

    private enum Format {

        BOLD("**", "**", fragment -> fragment.bold),
        ITALIC_ASTERISK("*", "*", fragment -> fragment.italic),
        ITALIC_UNDERSCORE("_", "_", fragment -> fragment.italic),
        STRIKETHROUGH("~", "~", fragment -> fragment.strikethrough),
        STRIKETHROUGH_DOUBLE("~~", "~~", fragment -> fragment.strikethrough),
        FONT("%!!uniform%", "%!!%", fragment -> fragment.font != null),
        COLOR("%#ff0000%", "%#%", fragment -> fragment.textColor != null);

        private final String opening;
        private final String closing;
        private final Predicate<MarkdownTextFragment> applied;

        Format(String opening, String closing, Predicate<MarkdownTextFragment> applied) {
            this.opening = opening;
            this.closing = closing;
            this.applied = applied;
        }

    }

    static List<Arguments> formatsAndLiteralTexts() {
        List<Arguments> arguments = new ArrayList<>();
        for (Format format : Format.values()) {
            for (String literalText : LITERAL_TEXTS) arguments.add(Arguments.of(format, literalText));
        }
        return arguments;
    }

    @ParameterizedTest
    @MethodSource("formatsAndLiteralTexts")
    void closingMarkerInsideCodeOrPlainTextStaysTextAndClosesNothing(Format format, String literalText) {
        String content = "inner1 " + format.closing + " inner2";
        List<MarkdownTextFragment> fragments = parse(format.opening + "before" + literalText.replace("TEXT", content) + "after" + format.closing + " end");

        // Fragments end at spaces, and their texts hold no line breaks
        assertTrue(joinText(fragments).contains(content), () -> joinText(fragments));
        assertTrue(format.applied.test(fragmentContaining(fragments, "before")));
        assertTrue(format.applied.test(fragmentContaining(fragments, "inner1")));
        assertTrue(format.applied.test(fragmentContaining(fragments, "inner2")));
        assertTrue(format.applied.test(fragmentContaining(fragments, "after")));
        assertFalse(format.applied.test(fragmentContaining(fragments, "end")));
        assertEquals("after", fragmentContaining(fragments, "after").text.trim());
    }

    @ParameterizedTest
    @MethodSource("formatsAndLiteralTexts")
    void markerInsideCodeOrPlainTextIsNoClosingMarkerForAFormatInFrontOfIt(Format format, String literalText) {
        List<MarkdownTextFragment> fragments = parse(format.opening + "before" + literalText.replace("TEXT", "inner " + format.closing) + "after");

        assertTrue(fragments.stream().noneMatch(format.applied));
        assertTrue(joinText(fragments).startsWith(format.opening + "before"), () -> joinText(fragments));
        assertTrue(joinText(fragments).contains("inner " + format.closing), () -> joinText(fragments));
    }

    @ParameterizedTest
    @ValueSource(strings = {"**", "*", "_", "~", "~~"})
    void markersInsideCodeStayTextWhenNoFormatIsOpen(String marker) {
        List<MarkdownTextFragment> fragments = parse("before `one" + marker + " two" + marker + "` after");

        assertEquals("before one" + marker + " two" + marker + " after", joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.bold || fragment.italic || fragment.strikethrough));
    }

    @ParameterizedTest
    @ValueSource(strings = {"`code ;;plain;; code`", "```\ncode ;;plain;; code\n```", "```\ncode\n;;;\nplain\n;;;\ncode\n```"})
    void plainTextMarkersInsideCodeStayText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        // Only the markers of the code are gone, and the text between the plain text markers is code like the text around them
        assertEquals(text.replace("`", "").replace("\n", ""), joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) {
            assertFalse(fragment.plainText);
            // Code inside of a line leaves an empty fragment in front of it when it starts the line
            assertTrue((fragment.codeBlockContext != null) || fragment.text.isEmpty(), () -> "<" + fragment.text + ">");
        }
    }

    @Test
    void codeEndsAtItsMarkerBehindPlainTextMarkersInsideOfIt() {
        List<MarkdownTextFragment> fragments = parse("`one ;;two` three;; four");

        assertEquals("one ;;two three;; four", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "two").codeBlockContext != null);
        assertTrue(fragmentContaining(fragments, "three").codeBlockContext == null);
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.plainText));
    }

    @Test
    void formatAroundCodeStillClosesBehindIt() {
        List<MarkdownTextFragment> fragments = parse("**bold `code` bold** plain");

        assertTrue(fragmentContaining(fragments, "code").bold);
        assertFalse(fragmentContaining(fragments, "plain").bold);
        assertEquals("bold code bold plain", joinText(fragments));
    }

}
