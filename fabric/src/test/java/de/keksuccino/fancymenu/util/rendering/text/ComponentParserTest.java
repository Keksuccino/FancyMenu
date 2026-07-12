package de.keksuccino.fancymenu.util.rendering.text;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentParserTest {

    @BeforeAll
    static void bootStrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void plainTextRemainsLiteral() {
        assertLiteral("plain text");
    }

    @Test
    void validObjectBecomesComponent() {
        Component component = ComponentParser.fromResolvedJsonOrPlainText("{\"text\":\"object text\"}");

        assertEquals("object text", component.getString());
    }

    @Test
    void validArrayPreservesOrderStyleAndSiblings() {
        Component component = ComponentParser.fromResolvedJsonOrPlainText("[{\"text\":\"first\",\"color\":\"red\"},{\"text\":\" second\",\"bold\":true},\" third\"]");

        assertEquals("first second third", component.getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), component.getStyle().getColor());
        assertEquals(2, component.getSiblings().size());
        assertEquals(" second", component.getSiblings().get(0).getString());
        assertEquals(" third", component.getSiblings().get(1).getString());
        assertTrue(component.getSiblings().get(0).getStyle().isBold());
    }

    @Test
    void validObjectPreservesStyledSiblings() {
        Component component = ComponentParser.fromResolvedJsonOrPlainText("{\"text\":\"root\",\"extra\":[{\"text\":\" child\",\"italic\":true},{\"text\":\" tail\"}]}");

        assertEquals("root child tail", component.getString());
        assertEquals(2, component.getSiblings().size());
        assertTrue(component.getSiblings().get(0).getStyle().isItalic());
        assertFalse(component.getSiblings().get(1).getStyle().isItalic());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "[", "{\"text\":\"broken\"", "[\"broken\""})
    void malformedJsonWithComponentPrefixRemainsLiteral(String input) {
        assertLiteral(input);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "{\"text\":{}}", "[1]"})
    void codecInvalidJsonRemainsLiteral(String input) {
        assertLiteral(input);
    }

    @Test
    void leadingWhitespacePreventsJsonParsing() {
        assertLiteral(" {\"text\":\"not parsed\"}");
    }

    @Test
    void quotedPrimitiveDoesNotEnterJsonParsing() {
        assertLiteral("\"quoted primitive\"");
    }

    @Test
    void repeatedMalformedInputNeverThrows() {
        for (int i = 0; i < 100; i++) {
            Component component = assertDoesNotThrow(() -> ComponentParser.fromResolvedJsonOrPlainText("{malformed"));
            assertEquals("{malformed", component.getString());
        }
    }

    @Test
    void unsafeClicksAreRemovedWithoutDroppingSafeSiblingStyles() {
        Component unsafeRoot = Component.literal("unsafe").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent.OpenFile("/tmp/unsafe")));
        Component safeSibling = Component.literal(" safe").withStyle(Style.EMPTY.withBold(true).withClickEvent(new ClickEvent.RunCommand("/help")));
        Component source = unsafeRoot.copy().append(safeSibling);

        String json = ComponentParser.toJson(source);
        Component parsed = ComponentParser.fromResolvedJsonOrPlainText(json);

        assertFalse(json.contains("open_file"));
        assertTrue(json.contains("run_command"));
        assertEquals("unsafe safe", parsed.getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), parsed.toFlatList().get(0).getStyle().getColor());
        assertTrue(parsed.toFlatList().stream().anyMatch(component -> component.getStyle().isBold() && component.getStyle().getClickEvent() instanceof ClickEvent.RunCommand));
    }

    @Test
    void unsafeClickInsideHoverTextIsSanitizedRecursively() {
        Component hoverText = Component.literal("hover").withStyle(Style.EMPTY.withItalic(true).withClickEvent(new ClickEvent.OpenFile("/tmp/hover")));
        Component source = Component.literal("root").withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(hoverText)));

        String json = ComponentParser.toJson(source);
        Component parsed = ComponentParser.fromResolvedJsonOrPlainText(json);

        assertFalse(json.contains("open_file"));
        HoverEvent.ShowText sanitizedHover = parsed.toFlatList().stream().map(component -> component.getStyle().getHoverEvent()).filter(HoverEvent.ShowText.class::isInstance).map(HoverEvent.ShowText.class::cast).findFirst().orElseThrow();
        assertEquals("hover", sanitizedHover.value().getString());
        assertTrue(sanitizedHover.value().toFlatList().stream().anyMatch(component -> component.getStyle().isItalic()));
        assertTrue(sanitizedHover.value().toFlatList().stream().allMatch(component -> component.getStyle().getClickEvent() == null || component.getStyle().getClickEvent().action().isAllowedFromServer()));
        assertTrue(hoverText.getStyle().getClickEvent() instanceof ClickEvent.OpenFile);
    }

    @Test
    void unsafeClickInsideHoverEntityNameIsSanitizedRecursively() {
        Component entityName = Component.literal("entity").withStyle(Style.EMPTY.withUnderlined(true).withClickEvent(new ClickEvent.OpenFile("/tmp/entity")));
        HoverEvent.EntityTooltipInfo entity = new HoverEvent.EntityTooltipInfo(EntityType.PIG, UUID.fromString("00000000-0000-0000-0000-000000000001"), entityName);
        Component source = Component.literal("root").withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowEntity(entity)));

        String json = ComponentParser.toJson(source);
        Component parsed = ComponentParser.fromResolvedJsonOrPlainText(json);

        assertFalse(json.contains("open_file"));
        HoverEvent.ShowEntity sanitizedHover = parsed.toFlatList().stream().map(component -> component.getStyle().getHoverEvent()).filter(HoverEvent.ShowEntity.class::isInstance).map(HoverEvent.ShowEntity.class::cast).findFirst().orElseThrow();
        Component sanitizedName = sanitizedHover.entity().name.orElseThrow();
        assertEquals("entity", sanitizedName.getString());
        assertTrue(sanitizedName.toFlatList().stream().anyMatch(component -> component.getStyle().isUnderlined()));
        assertTrue(sanitizedName.toFlatList().stream().allMatch(component -> component.getStyle().getClickEvent() == null || component.getStyle().getClickEvent().action().isAllowedFromServer()));
        assertTrue(entityName.getStyle().getClickEvent() instanceof ClickEvent.OpenFile);
    }

    private static void assertLiteral(String input) {
        Component component = ComponentParser.fromResolvedJsonOrPlainText(input);
        assertEquals(input, component.getString());
        assertEquals(0, component.getSiblings().size());
        assertTrue(component.getStyle().isEmpty());
    }

}
