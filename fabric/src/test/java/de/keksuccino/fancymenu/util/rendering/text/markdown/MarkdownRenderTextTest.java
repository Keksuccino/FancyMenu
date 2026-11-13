package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
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

    @Test
    void ampersandsThatStartNoFormattingCodeStayInTheRenderTextAsTheyAre() {
        String text = "Bread & Butter &zUnknown &AUppercase &";
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 2000);

        assertEquals(text, renderer.buildRenderText());
    }

    @Test
    void htmlBreaksOnlyStayInTheRenderTextWhileTheRendererKeepsThem() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("&aFirst%n%Second\rThird\\nFourth<br>Fifth", 2000);

        renderer.setRemoveHtmlBreaks(false);
        assertEquals("&aFirst\nSecond\nThird\nFourth<br>Fifth", renderer.buildRenderText());

        renderer.setRemoveHtmlBreaks(true);
        assertEquals("&aFirst\nSecond\nThird\nFourthFifth", renderer.buildRenderText());
    }

    @Test
    @ResourceLock("PlaceholderParser global state")
    void formattingCodesOfAPlaceholderValueStayInTheRenderTextAsTheyAre() {
        PlaceholderParser.PlaceholderCachingController cachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
        try {
            String key = "markdown_render_text_formatting_codes";
            TestPlaceholders.setValue(key, "&cOffline &nnow &zUnknown");
            MarkdownTestRenderer renderer = new MarkdownTestRenderer("Status: " + TestPlaceholders.value(key), 2000);

            assertEquals("Status: &cOffline &nnow &zUnknown", renderer.buildRenderText());
        } finally {
            PlaceholderParser.setPlaceholderCachingController(cachingController);
        }
    }

}
