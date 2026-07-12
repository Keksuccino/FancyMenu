package de.keksuccino.fancymenu.util.rendering.text;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentParserTest {

    private static PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeAll
    static void disablePlaceholderCaching() {
        originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterAll
    static void restorePlaceholderCaching() {
        PlaceholderParser.setPlaceholderCachingController(originalCachingController);
    }

    @Test
    void parsesValidObjectComponentJson() {
        Component component = ComponentParser.fromJsonOrPlainText("{\"text\":\"Hello\",\"extra\":[{\"text\":\" world\"}]}");

        assertEquals("Hello world", component.getString());
    }

    @Test
    void parsesValidArrayComponentJson() {
        Component component = ComponentParser.fromJsonOrPlainText("[{\"text\":\"First\"},{\"text\":\" second\"}]");

        assertEquals("First second", component.getString());
    }

    @Test
    void malformedJsonFallsBackToExactLiteralText() {
        String text = "{not valid json";

        assertEquals(text, ComponentParser.fromJsonOrPlainText(text).getString());
    }

    @Test
    void trailingContentPreventsPartialJsonParsing() {
        String text = "{\"text\":\"parsed\"} trailing plain text";

        assertEquals(text, ComponentParser.fromJsonOrPlainText(text).getString());
    }

    @Test
    void invalidComponentJsonFallsBackToExactLiteralText() {
        String text = "{\"unsupported\":true}";

        assertEquals(text, ComponentParser.fromJsonOrPlainText(text).getString());
    }

    @Test
    void emptyComponentArrayFallsBackToExactLiteralText() {
        String text = "[]";

        assertEquals(text, ComponentParser.fromJsonOrPlainText(text).getString());
    }

    @Test
    void repeatedInvalidInputAlwaysUsesLiteralFallback() {
        String text = "[still plain text";

        for (int i = 0; i < 25; i++) {
            assertEquals(text, ComponentParser.fromJsonOrPlainText(text).getString());
        }
    }

    @Test
    void elementFallbackDoesNotLogExpectedParserFailures() {
        org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(AbstractElement.class);
        CollectingAppender appender = new CollectingAppender();
        appender.start();
        logger.addAppender(appender);
        try {
            for (int i = 0; i < 3; i++) {
                assertEquals("{not valid json", AbstractElement.buildComponent("{not valid json").getString());
                assertEquals("{\"unsupported\":true}", AbstractElement.buildComponent("{\"unsupported\":true}").getString());
            }
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }

        long parserFailureLogs = appender.events.stream().filter(event -> event.getMessage().getFormattedMessage().contains("Failed to deserialize Component!")).count();
        assertEquals(0, parserFailureLogs);
    }

    private static final class CollectingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private CollectingAppender() {
            super("ComponentParserTest", null, null, false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            this.events.add(event.toImmutable());
        }

    }

}
