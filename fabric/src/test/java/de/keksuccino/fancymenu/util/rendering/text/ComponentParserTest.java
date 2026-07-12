package de.keksuccino.fancymenu.util.rendering.text;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    private static void assertLiteral(String input) {
        Component component = ComponentParser.fromResolvedJsonOrPlainText(input);
        assertEquals(input, component.getString());
        assertEquals(0, component.getSiblings().size());
        assertTrue(component.getStyle().isEmpty());
    }

}
