package de.keksuccino.fancymenu.util.rendering.text;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.Bootstrap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentParserTest {

    private static final String COMPONENT_PARSER_LOGGER_NAME = ComponentParser.class.getName();
    // Keep this literal so the Minecraft-heavy AbstractElement class is not loaded before @BeforeAll bootstraps Vanilla.
    private static final String ABSTRACT_ELEMENT_LOGGER_NAME = "de.keksuccino.fancymenu.customization.element.AbstractElement";
    private static final List<String> CAPTURED_LOGGER_NAMES = List.of(COMPONENT_PARSER_LOGGER_NAME, ABSTRACT_ELEMENT_LOGGER_NAME);
    private static PlaceholderParser.PlaceholderCachingController previousPlaceholderCachingController;

    @BeforeAll
    static void initializeMinecraftRegistriesAndPlaceholderParser() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        previousPlaceholderCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
        ComponentParser.fromJsonOrPlainText("Initialize parser");
    }

    @AfterAll
    static void restorePlaceholderCachingController() {
        PlaceholderParser.setPlaceholderCachingController(previousPlaceholderCachingController);
    }

    @Test
    void repeatedIssueInputFallsBackToLiteralWithoutErrorLogs() {
        String input = "[checkbox_of_doom] oh no";

        assertProducesNoErrorLogs(() -> {
            for (int i = 0; i < 100; i++) {
                assertEquals(input, ComponentParser.fromJsonOrPlainText(input).getString());
                assertEquals(input, AbstractElement.buildComponent(input).getString());
            }
        });
    }

    @Test
    void malformedJsonFallsBackToLiteralWithoutErrorLogs() {
        assertProducesNoErrorLogs(() -> {
            assertEquals("{", ComponentParser.fromJsonOrPlainText("{").getString());
            assertEquals("[", ComponentParser.fromJsonOrPlainText("[").getString());
            assertEquals("{\"text\":", ComponentParser.fromJsonOrPlainText("{\"text\":").getString());
            assertNull(ComponentParser.fromJson("{\"text\":"));
        });
    }

    @Test
    void codecInvalidJsonFallsBackToLiteralWithoutErrorLogs() {
        assertProducesNoErrorLogs(() -> {
            assertEquals("{}", ComponentParser.fromJsonOrPlainText("{}").getString());
            assertEquals("[]", ComponentParser.fromJsonOrPlainText("[]").getString());
            assertNull(ComponentParser.fromJson("{}"));
            assertNull(ComponentParser.fromJson("[]"));
        });
    }

    @Test
    void validObjectAndArrayJsonDeserializeAsComponents() {
        Component objectComponent = ComponentParser.fromJsonOrPlainText("{\"text\":\"Object\",\"color\":\"red\",\"extra\":[{\"text\":\"Child\",\"bold\":true}]}");
        Component arrayComponent = ComponentParser.fromJsonOrPlainText("[\"First\",{\"text\":\"Second\",\"italic\":true}]");
        MutableComponent jsonStringComponent = ComponentParser.fromJson("\"Quoted\"");

        assertEquals("ObjectChild", objectComponent.getString());
        assertNotNull(objectComponent.getStyle().getColor());
        assertEquals(0xFF5555, objectComponent.getStyle().getColor().getValue());
        assertEquals(1, objectComponent.getSiblings().size());
        assertEquals("Child", objectComponent.getSiblings().getFirst().getString());
        assertTrue(objectComponent.getSiblings().getFirst().getStyle().isBold());
        assertEquals("FirstSecond", arrayComponent.getString());
        assertEquals(1, arrayComponent.getSiblings().size());
        assertEquals("Second", arrayComponent.getSiblings().getFirst().getString());
        assertTrue(arrayComponent.getSiblings().getFirst().getStyle().isItalic());
        assertNotNull(jsonStringComponent);
        assertEquals("Quoted", jsonStringComponent.getString());
    }

    @Test
    void plainTextAndWhitespacePrefixSemanticsRemainUnchanged() {
        assertEquals("Plain text", ComponentParser.fromJsonOrPlainText("Plain text").getString());
        assertEquals("", ComponentParser.fromJsonOrPlainText("").getString());
        assertEquals(" \t{\"text\":\"Not parsed\"}", ComponentParser.fromJsonOrPlainText(" \t{\"text\":\"Not parsed\"}").getString());
        assertEquals("\"Quoted\"", ComponentParser.fromJsonOrPlainText("\"Quoted\"").getString());
    }

    @Test
    void abstractElementPreservesFormattingBeforeDelegatingToSharedParser() {
        assertEquals("§aGreen", AbstractElement.buildComponent("&aGreen").getString());
        assertEquals("Delegated", AbstractElement.buildComponent("{\"text\":\"Delegated\"}").getString());
    }

    @Test
    void abstractElementFormatsBeforeSharedPlaceholderPassAndDecodesProcessorJson() {
        AtomicReference<String> processorInput = new AtomicReference<>();
        long processorId = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, input -> {
            processorInput.set(input);
            return "{\"text\":\"Processed\",\"extra\":[{\"text\":\" JSON\"}]}";
        });
        try {
            Component component = AbstractElement.buildComponent("&aFormatting before placeholder processing");

            assertEquals("§aFormatting before placeholder processing", processorInput.get());
            assertEquals("Processed JSON", component.getString());
            assertEquals(1, component.getSiblings().size());
            assertEquals(" JSON", component.getSiblings().getFirst().getString());
        } finally {
            PlaceholderParser.removeParsingProcessor(processorId);
        }
    }

    @Test
    void deprecatedAbstractElementBridgeRetainsProtectedStaticAbiAndNullableSemantics() throws Exception {
        Method bridge = AbstractElement.class.getDeclaredMethod("deserializeComponentFromJson", String.class);

        assertTrue(Modifier.isProtected(bridge.getModifiers()));
        assertTrue(Modifier.isStatic(bridge.getModifiers()));
        assertTrue(bridge.isAnnotationPresent(Deprecated.class));
        assertTrue(bridge.trySetAccessible());
        assertEquals("Bridge", ((MutableComponent)bridge.invoke(null, "{\"text\":\"Bridge\"}")).getString());
        assertNull(bridge.invoke(null, "[invalid"));
        assertNull(bridge.invoke(null, "{}"));
    }

    private static void assertProducesNoErrorLogs(Runnable invocation) {
        LoggerContext context = (LoggerContext)LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        Map<String, LoggerConfig> previousLoggers = new LinkedHashMap<>();
        RecordingAppender appender = new RecordingAppender("ComponentParserTest-" + System.nanoTime());
        appender.start();
        for (String loggerName : CAPTURED_LOGGER_NAMES) {
            previousLoggers.put(loggerName, configuration.getLoggers().get(loggerName));
            LoggerConfig logger = new LoggerConfig(loggerName, Level.ALL, false);
            logger.addAppender(appender, Level.ERROR, null);
            configuration.addLogger(loggerName, logger);
        }
        context.updateLoggers();
        try {
            invocation.run();
            assertTrue(appender.events.isEmpty(), () -> "Expected no error logs, but captured: " + appender.events);
        } finally {
            for (Map.Entry<String, LoggerConfig> entry : previousLoggers.entrySet()) {
                configuration.removeLogger(entry.getKey());
                if (entry.getValue() != null) configuration.addLogger(entry.getKey(), entry.getValue());
            }
            context.updateLoggers();
            appender.stop();
        }
    }

    private static final class RecordingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private RecordingAppender(String name) {
            super(name, null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            this.events.add(event.toImmutable());
        }

    }

}
