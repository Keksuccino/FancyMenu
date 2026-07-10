package de.keksuccino.fancymenu.util.rendering.text;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentParserTest {

    @Test
    void plainTextRemainsLiteral() {
        Component component = ComponentParser.fromJsonOrPlainText("Plain text");

        assertEquals("Plain text", component.getString());
    }

    @Test
    void validObjectJsonBecomesComponent() {
        Component component = ComponentParser.fromJsonOrPlainText("{\"text\":\"Object text\",\"bold\":true}");

        assertEquals("Object text", component.getString());
        assertTrue(component.getStyle().isBold());
    }

    @Test
    void validArrayJsonBecomesCombinedComponent() {
        Component component = ComponentParser.fromJsonOrPlainText("[\"First\",{\"text\":\" second\"}]");

        assertEquals("First second", component.getString());
    }

    @Test
    void malformedJsonFallsBackToExactLiteralText() {
        String malformedJson = "{plain text";

        assertEquals(malformedJson, ComponentParser.fromJsonOrPlainText(malformedJson).getString());
    }

    @Test
    void syntacticallyValidInvalidComponentJsonFallsBackToExactLiteralText() {
        String invalidComponentJson = "{}";

        assertEquals(invalidComponentJson, ComponentParser.fromJsonOrPlainText(invalidComponentJson).getString());
    }

    @Test
    void emptyComponentArrayFallsBackToExactLiteralText() {
        String emptyArray = "[]";

        assertEquals(emptyArray, ComponentParser.fromJsonOrPlainText(emptyArray).getString());
    }

    @Test
    void nestedEmptyComponentArrayFallsBackInsteadOfReturningNullSibling() {
        String invalidComponentJson = "[{\"text\":\"First\"},[]]";

        assertEquals(invalidComponentJson, ComponentParser.fromJsonOrPlainText(invalidComponentJson).getString());
    }

    @Test
    void nestedEmptyTranslationArgumentFallsBackInsteadOfReturningNullArgument() {
        String invalidComponentJson = "{\"translate\":\"component.parser.test\",\"with\":[[]]}";

        assertEquals(invalidComponentJson, ComponentParser.fromJsonOrPlainText(invalidComponentJson).getString());
    }

    @Test
    void nestedEmptyHoverTextFallsBackInsteadOfReturningNullSibling() {
        String invalidComponentJson = "{\"text\":\"Root\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[{\"text\":\"Hover\"},[]]}}";

        assertEquals(invalidComponentJson, ComponentParser.fromJsonOrPlainText(invalidComponentJson).getString());
    }

    @Test
    void unexpectedRuntimeParserFailuresPropagate() {
        String invalidResourceLocation = "{\"nbt\":\"path\",\"storage\":\"invalid resource location\"}";

        assertThrows(RuntimeException.class, () -> ComponentParser.fromJsonOrPlainText(invalidResourceLocation));
    }

    @Test
    void elementFormattingRunsBeforeDelegatingToComponentParser() {
        Component component = AbstractElement.buildComponent("&bElement text");

        assertEquals("\u00a7bElement text", component.getString());
    }
}
