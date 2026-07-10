package de.keksuccino.fancymenu.util.rendering.text;

import com.google.gson.JsonParseException;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void invalidResourceLocationFallsBackToExactLiteralText() {
        String invalidResourceLocation = "{\"nbt\":\"path\",\"storage\":\"invalid resource location\"}";

        assertEquals(invalidResourceLocation, ComponentParser.fromJsonOrPlainText(invalidResourceLocation).getString());
    }

    @Test
    void targetSpecificUserInputFailuresAreExpected() {
        assertTrue(ComponentParser.isExpectedUserInputFailure(new JsonParseException("invalid component")));
        assertTrue(ComponentParser.isExpectedUserInputFailure(new ResourceLocationException("invalid identifier")));
        assertTrue(ComponentParser.isExpectedUserInputFailure(new IllegalArgumentException("invalid value")));
    }

    @Test
    void unrelatedRuntimeFailuresRemainUnexpected() {
        assertFalse(ComponentParser.isExpectedUserInputFailure(new IllegalStateException("unexpected parser failure")));
    }

    @Test
    void elementFormattingRunsBeforeDelegatingToComponentParser() {
        Component component = AbstractElement.buildComponent("&bElement text");

        assertEquals("\u00a7bElement text", component.getString());
    }
}
