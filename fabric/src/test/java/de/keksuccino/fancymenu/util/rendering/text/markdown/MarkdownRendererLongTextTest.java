package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import net.minecraft.client.resources.language.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The renderer accepts texts of up to 45,000 characters and replaces their placeholders without the length limit of the placeholder parser,
 * because it limits the length of its text itself.
 */
@ResourceLock("PlaceholderParser global state")
class MarkdownRendererLongTextTest {

    private static final int RENDERER_LIMIT = 45000;

    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void disableCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restoreCaching() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @ParameterizedTest
    @ValueSource(ints = {1000, 30000, RENDERER_LIMIT - 1, RENDERER_LIMIT})
    void textUpToTheLimitOfTheRendererRenders(int length) {
        String text = words(length);
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.tick();

        assertEquals(text, renderer.getText());
        assertEquals(text, joinText(renderer.fragments));
    }

    @Test
    void textAboveTheLimitOfTheRendererGetsReplacedWithTheErrorTextOfTheRenderer() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(words(RENDERER_LIMIT + 1), 400);
        renderer.tick();

        assertEquals(I18n.get("fancymenu.markdown.error.text_too_long"), renderer.getText());
    }

    @Test
    void placeholdersOfALongTextGetReplacedAndTheirChangesNoticed() {
        String key = "markdown_renderer_long_text";
        TestPlaceholders.setValue(key, "first");
        // The placeholders alone are almost as long as the limit of the renderer
        StringBuilder text = new StringBuilder();
        while (text.length() < (RENDERER_LIMIT - 1000)) text.append(TestPlaceholders.value(key)).append(' ');
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text.toString(), 400);

        renderer.tick();
        int words = renderer.fragments.size();

        assertEquals("first ", renderer.fragments.get(0).text);
        assertEquals("first ", renderer.fragments.get(words - 2).text);

        TestPlaceholders.setValue(key, "second");
        renderer.tick();

        assertEquals(2, renderer.parses);
        assertEquals(words, renderer.fragments.size());
        assertEquals("second ", renderer.fragments.get(0).text);
        assertEquals("second ", renderer.fragments.get(words - 2).text);
    }

    @Test
    void longValueInsideAPlaceholderGetsItsPlaceholdersReplacedAsWell() {
        // The placeholder parser replaces the placeholders inside the value of a placeholder with a call of its own
        String value = words(RENDERER_LIMIT - 1000);
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TestPlaceholders.echo(value), 400);
        renderer.tick();

        assertEquals(value, joinText(renderer.fragments));
    }

    /**
     * Returns a text of exactly the given length that is made of short words, so it has no markers and wraps like normal text.
     */
    private static String words(int length) {
        StringBuilder text = new StringBuilder(length + 5);
        while (text.length() < length) text.append("word ");
        text.setLength(length - 1);
        return text.append('x').toString();
    }

}
