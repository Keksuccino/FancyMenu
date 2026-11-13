package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownRenderTextTest {

    @Test
    void everySpellingOfALineBreakBecomesANewline() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("First%n%Second\rThird\\nFourth", 2000);

        assertEquals("First\nSecond\nThird\nFourth", renderer.buildRenderText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"&0&1&2&3&4&5&6&7&8&9&a&b&c&d&e&f&k&l&m&n&o&r", "Plain &aformatted", "§aGreen §lBold", "&A &L &z &x", "&&a", "[link](https://example.test/&a)"})
    void formattingCodesStayInTheRenderTextAsTheyAre(String text) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 2000);

        assertEquals(text, renderer.buildRenderText());
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
