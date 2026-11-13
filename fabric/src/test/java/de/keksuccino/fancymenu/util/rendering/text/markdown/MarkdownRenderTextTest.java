package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("PlaceholderParser global state")
class MarkdownRenderTextTest {

    private static final String PLACEHOLDER = "{\"placeholder\":\"markdown_render_text_test\"}";

    @Test
    void everySpellingOfALineBreakBecomesANewline() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("First%n%Second\rThird\\nFourth", 2000);

        assertEquals("First\nSecond\nThird\nFourth", renderer.buildRenderText());
    }

    private static Stream<String> textsWithFormattingCodes() {
        return Stream.of("&0&1&2&3&4&5&6&7&8&9&a&b&c&d&e&f&k&l&m&n&o&r", "Plain &aformatted", "§aGreen §lBold", "&A &L &z &x", "&&a", "[link](https://example.test/&a)");
    }

    @ParameterizedTest
    @MethodSource("textsWithFormattingCodes")
    void formattingCodesStayInTheRenderTextAsTheyAre(String text) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 2000);

        assertEquals(text, renderer.buildRenderText());
    }

    /**
     * The placeholder parser turns ampersand codes into codes with a section sign, which it only does in a text with a placeholder.
     * The renderer reads both kinds of codes itself, so it asks for its text with the codes as they are.
     */
    @ParameterizedTest
    @MethodSource("textsWithFormattingCodes")
    void formattingCodesStayInTheRenderTextOfATextWithAPlaceholder(String text) {
        long processorId = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, value -> value.replace(PLACEHOLDER, "value"));
        try {
            MarkdownTestRenderer renderer = new MarkdownTestRenderer(text + " " + PLACEHOLDER, 2000);

            assertEquals(text + " value", renderer.buildRenderText());
        } finally {
            PlaceholderParser.removeParsingProcessor(processorId);
        }
    }

    @Test
    void formattingCodesStayWhileLineBreaksBecomeNewlines() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("&aFirst%n%Second\rThird\\nFourth", 2000);

        assertEquals("&aFirst\nSecond\nThird\nFourth", renderer.buildRenderText());
    }

    @Test
    void renderTextOfARenderTextStaysTheSameAndTheTextOfTheRendererStaysUntouched() {
        String text = "&aFirst%n%Second<br>";
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 2000);
        String renderText = renderer.buildRenderText();

        assertEquals("&aFirst\nSecond", renderText);
        assertEquals(text, renderer.getText());

        renderer.setText(renderText);

        assertEquals(renderText, renderer.buildRenderText());
    }

}
