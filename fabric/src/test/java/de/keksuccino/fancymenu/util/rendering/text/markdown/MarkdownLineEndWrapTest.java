package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A line break in the text gets an empty fragment of its own whenever the text in front of it is built already, which is the case
 * behind a space and behind every inline format. These tests cover lines that are wider than the renderer in front of that fragment.
 */
class MarkdownLineEndWrapTest {

    /** Six pixels per character make every first line below wider than this. */
    private static final float WIDTH = 60;

    @ParameterizedTest
    @ValueSource(strings = {"averyveryverylongword \nnext", "**averyveryverylongword** \nnext", "> averyveryverylongword \n> next", "- averyveryverylongword \n- next", "[averyveryverylongword](https://example.invalid) \nnext"})
    void lineEndBehindASpaceAddsNoEmptyLineWhenTheLineIsWiderThanTheRenderer(String text) {
        List<List<String>> lines = renderLines(text);

        assertEquals(2, lines.size());
        assertEquals("averyveryverylongword ", lines.get(0).get(lines.get(0).size() - 2));
        assertEquals("", lines.get(0).get(lines.get(0).size() - 1));
        assertEquals(List.of("next"), lines.get(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"averyveryverylongword next", "averyveryverylongword **next**", "averyveryverylongword ~~ next"})
    void lineStillWrapsInFrontOfTheNextWord(String text) {
        List<List<String>> lines = renderLines(text);

        assertEquals(2, lines.size());
        assertEquals("averyveryverylongword ", lines.get(0).get(lines.get(0).size() - 1));
        assertFalse(lines.get(1).isEmpty());
    }

    private static List<List<String>> renderLines(String text) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, WIDTH);
        renderer.tick();
        List<List<String>> lines = new ArrayList<>();
        for (MarkdownTextLine line : renderer.lines) lines.add(texts(line.fragments));
        return lines;
    }

}
