package de.keksuccino.fancymenu.util.rendering.text.markdown;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownParserBlockClosingTest {

    private static final Font TEST_FONT = new Font(identifier -> null, false) {
        @Override
        public int width(String text) {
            return text.length();
        }

        @Override
        public int width(FormattedText text) {
            return text.getString().length();
        }
    };

    @ParameterizedTest
    @ValueSource(strings = {"^^^", "|||", ";;;"})
    void terminalClosingMarkerDoesNotCreateAnEmptyFragment(String marker) {
        List<MarkdownTextFragment> fragments = parse(marker + "\ntest\n" + marker);

        assertEquals(1, fragments.size());
        assertEquals("test", fragments.get(0).text);
        assertTrue(fragments.get(0).naturalLineBreakAfter);
    }

    @ParameterizedTest
    @ValueSource(strings = {"^^^", "|||", ";;;"})
    void closingMarkerBeforeFollowingContentPreservesTheContentLine(String marker) {
        List<MarkdownTextFragment> fragments = parse(marker + "\ntest\n" + marker + "\nafter");

        assertEquals(2, fragments.size());
        assertEquals("test", fragments.get(0).text);
        assertTrue(fragments.get(0).naturalLineBreakAfter);
        assertEquals("after", fragments.get(1).text);
        assertTrue(fragments.get(1).naturalLineBreakAfter);
    }

    @ParameterizedTest
    @ValueSource(strings = {"^^^", "|||", ";;;"})
    void terminalClosingMarkerWithLineTerminatorDoesNotCreateAnEmptyFragment(String marker) {
        List<MarkdownTextFragment> fragments = parse(marker + "\ntest\n" + marker + "\n");

        assertEquals(1, fragments.size());
        assertEquals("test", fragments.get(0).text);
    }

    private static List<MarkdownTextFragment> parse(String text) {
        return MarkdownParser.parse(new MarkdownRenderer(TEST_FONT), text, true);
    }

}
