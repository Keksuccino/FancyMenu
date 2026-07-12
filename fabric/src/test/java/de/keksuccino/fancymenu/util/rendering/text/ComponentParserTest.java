package de.keksuccino.fancymenu.util.rendering.text;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    void unsafeClickActionsAreRemovedFromDirectAndInheritedStylesWithoutLosingFormatting() {
        Style unsafeParentStyle = Style.EMPTY.withColor(ChatFormatting.RED).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, "/tmp/example"));
        Component input = Component.literal("Parent").setStyle(unsafeParentStyle).append(Component.literal("Child").setStyle(Style.EMPTY.withItalic(true)));

        MutableComponent decoded = ComponentParser.fromJson(ComponentParser.toJson(input));

        assertNotNull(decoded);
        assertEquals("ParentChild", decoded.getString());
        List<Component> segments = decoded.toFlatList();
        assertEquals(2, segments.size());
        assertTrue(segments.stream().allMatch(segment -> segment.getStyle().getClickEvent() == null));
        assertEquals(ChatFormatting.RED.getColor(), segments.get(0).getStyle().getColor().getValue());
        assertTrue(segments.get(0).getStyle().isBold());
        assertEquals(ChatFormatting.RED.getColor(), segments.get(1).getStyle().getColor().getValue());
        assertTrue(segments.get(1).getStyle().isBold());
        assertTrue(segments.get(1).getStyle().isItalic());
    }

    @Test
    void unsafeClickOnSiblingIsRemovedWhileSafeClickAndTextRemain() {
        ClickEvent safeClick = new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com");
        Component input = Component.literal("Safe").setStyle(Style.EMPTY.withClickEvent(safeClick)).append(Component.literal("Unsafe").setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, "/tmp/example"))));

        MutableComponent decoded = ComponentParser.fromJson(ComponentParser.toJson(input));

        assertNotNull(decoded);
        assertEquals("SafeUnsafe", decoded.getString());
        List<Component> segments = decoded.toFlatList();
        assertEquals(safeClick, segments.get(0).getStyle().getClickEvent());
        assertNull(segments.get(1).getStyle().getClickEvent());
    }

    @Test
    void unsafeClicksAreSanitizedRecursivelyInsideShowTextHoverEvents() {
        Component hoverText = Component.literal("Hover").append(Component.literal(" file").setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, "/tmp/example"))));
        Component input = Component.literal("Visible").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));

        MutableComponent decoded = ComponentParser.fromJson(ComponentParser.toJson(input));

        assertNotNull(decoded);
        HoverEvent hoverEvent = decoded.toFlatList().getFirst().getStyle().getHoverEvent();
        assertNotNull(hoverEvent);
        Component decodedHoverText = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
        assertNotNull(decodedHoverText);
        assertEquals("Hover file", decodedHoverText.getString());
        assertTrue(decodedHoverText.toFlatList().stream().allMatch(segment -> segment.getStyle().getClickEvent() == null));
    }

    @Test
    void unsafeClicksAreSanitizedRecursivelyInsideShowEntityNames() {
        UUID entityId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        Component entityName = Component.literal("Entity").setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, "/tmp/example")));
        HoverEvent.EntityTooltipInfo entityInfo = new HoverEvent.EntityTooltipInfo(EntityType.PIG, entityId, entityName);
        Component input = Component.literal("Visible").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY, entityInfo)));

        MutableComponent decoded = ComponentParser.fromJson(ComponentParser.toJson(input));

        assertNotNull(decoded);
        HoverEvent hoverEvent = decoded.toFlatList().getFirst().getStyle().getHoverEvent();
        assertNotNull(hoverEvent);
        HoverEvent.EntityTooltipInfo decodedInfo = hoverEvent.getValue(HoverEvent.Action.SHOW_ENTITY);
        assertNotNull(decodedInfo);
        assertSame(EntityType.PIG, decodedInfo.type);
        assertEquals(entityId, decodedInfo.id);
        assertTrue(decodedInfo.name.isPresent());
        assertEquals("Entity", decodedInfo.name.get().getString());
        assertTrue(decodedInfo.name.get().toFlatList().stream().allMatch(segment -> segment.getStyle().getClickEvent() == null));
    }

    @Test
    void registryAwareSerializationUsesTheSameSanitizer() {
        Component input = Component.literal("Registry").setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, "/tmp/example")));

        MutableComponent decoded = ComponentParser.fromJson(ComponentParser.toJson(input, RegistryAccess.EMPTY));

        assertNotNull(decoded);
        assertEquals("Registry", decoded.getString());
        assertNull(decoded.toFlatList().getFirst().getStyle().getClickEvent());
    }

    @Test
    void safeShowItemHoverSurvivesFlatteningTriggeredByAnotherSegment() {
        HoverEvent itemHover = new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(new ItemStack(Items.DIAMOND)));
        Component input = Component.literal("Unsafe").setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, "/tmp/example"))).append(Component.literal("Item").setStyle(Style.EMPTY.withHoverEvent(itemHover)));

        MutableComponent decoded = ComponentParser.fromJson(ComponentParser.toJson(input));

        assertNotNull(decoded);
        List<Component> segments = decoded.toFlatList();
        assertEquals(2, segments.size());
        assertNull(segments.get(0).getStyle().getClickEvent());
        HoverEvent decodedHover = segments.get(1).getStyle().getHoverEvent();
        assertNotNull(decodedHover);
        HoverEvent.ItemStackInfo decodedItem = decodedHover.getValue(HoverEvent.Action.SHOW_ITEM);
        assertNotNull(decodedItem);
        assertTrue(decodedItem.getItemStack().is(Items.DIAMOND));
    }

    @Test
    void serializationDoesNotMutateTheOriginalComponentStyleGraph() {
        ClickEvent unsafeClick = new ClickEvent(ClickEvent.Action.OPEN_FILE, "/tmp/example");
        Style unsafeStyle = Style.EMPTY.withColor(ChatFormatting.GOLD).withClickEvent(unsafeClick);
        MutableComponent input = Component.literal("Original").setStyle(unsafeStyle);

        ComponentParser.toJson(input);

        assertSame(unsafeStyle, input.getStyle());
        assertSame(unsafeClick, input.getStyle().getClickEvent());
        assertEquals(ChatFormatting.GOLD.getColor(), input.getStyle().getColor().getValue());
    }

    @Test
    void serializationFailureFallsBackToJsonQuotedVisibleText() {
        HolderLookup.Provider failingRegistries = (HolderLookup.Provider)Proxy.newProxyInstance(HolderLookup.Provider.class.getClassLoader(), new Class<?>[]{HolderLookup.Provider.class}, (proxy, method, args) -> {
            throw new IllegalStateException("intentional test failure");
        });

        String serialized = ComponentParser.toJson(Component.literal("Fallback \"text\""), failingRegistries);

        assertEquals("\"Fallback \\\"text\\\"\"", serialized);
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
