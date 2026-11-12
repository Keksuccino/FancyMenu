package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("PlaceholderParser global state")
class MarkdownRendererPlaceholderTest {

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

    @Test
    void changedPlaceholderValueParsesTheTextAgain() {
        String key = "markdown_renderer_changed_value";
        TestPlaceholders.setValue(key, "first");
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("Value: " + TestPlaceholders.value(key), 400);

        renderer.tick();
        renderer.tick();

        assertEquals(1, renderer.parses);
        assertEquals("first", renderer.fragmentContaining("first").text);

        TestPlaceholders.setValue(key, "second");
        renderer.tick();

        assertEquals(2, renderer.parses);
        assertEquals("second", renderer.fragmentContaining("second").text);
    }

    @Test
    void sizeQueryWhileReplacingPlaceholdersDoesNotStartAnotherRefresh() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("some text", 400);
        long processorId = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, value -> {
            renderer.getRealHeight();
            renderer.getRealWidth();
            return value;
        });
        try {
            renderer.tick();
            renderer.setText("some other text\nwith two lines");
            renderer.getRealHeight();
        } finally {
            PlaceholderParser.removeParsingProcessor(processorId);
        }

        assertEquals(2, renderer.parses);
        assertEquals(2, renderer.layouts);
        assertEquals(2, renderer.lines.size());
    }

}
