package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderParser global state")
class WidgetTooltipTextTest {

    @Test
    void preparesFormattingWithoutPlaceholders() {
        String tooltip = "&5Purple &lBold &mStrike &nUnderline &oItalic &kMagic &rReset";

        assertEquals("§5Purple §lBold §mStrike §nUnderline §oItalic §kMagic §rReset", WidgetTooltipText.replacePlaceholdersAndFormattingCodes(tooltip));
    }

    @Test
    void matchesEstablishedFormattingConversion() {
        String tooltip = "&0&1&2&3&4&5&6&7&8&9&a&b&c&d&e&f &A &x &&a §cExisting &zUnknown";
        String expected = TextFormattingUtils.replaceFormattingCodes(tooltip, "&", "§");

        assertEquals(expected, WidgetTooltipText.replacePlaceholdersAndFormattingCodes(tooltip));
        assertEquals("§0§1§2§3§4§5§6§7§8§9§a§b§c§d§e§f &A &x &§a §cExisting &zUnknown", expected);
    }

    @Test
    void doesNotExtendTheEstablishedConverterWithHexCodes() {
        assertEquals("&x§1§2§3§4§5§6", WidgetTooltipText.replacePlaceholdersAndFormattingCodes("&x&1&2&3&4&5&6"));
    }

    @Test
    void convertsFormattingProducedAtTheEndOfPlaceholderProcessingWithoutDecodingItsNewlines() {
        String placeholder = "{\"placeholder\":\"widget_tooltip_formatting_test\"}";
        PlaceholderParser.PlaceholderCachingController originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        long processorId = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, value -> value.replace(placeholder, "%n%&aDynamic"));
        try {
            PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
            assertEquals("%n%§aDynamic", WidgetTooltipText.replacePlaceholdersAndFormattingCodes(placeholder));
        } finally {
            PlaceholderParser.removeParsingProcessor(processorId);
            PlaceholderParser.setPlaceholderCachingController(originalCachingController);
        }
    }

    @Test
    void leavesNewlineDecodingToWidgetSpecificRenderingPaths() {
        String prepared = WidgetTooltipText.replacePlaceholdersAndFormattingCodes("&aFirst%n%&bSecond\\n&cThird");

        assertEquals("§aFirst%n%§bSecond\\n§cThird", prepared);
        assertEquals("§aFirst\n§bSecond\n§cThird", prepared.replace("%n%", "\n").replace("\\n", "\n"));
        assertEquals("§aFirst\n§bSecond\\n§cThird", WidgetTooltipText.replacePlaceholdersAndFormattingCodes("&aFirst%n%&bSecond\\n&cThird".replace("%n%", "\n")));
    }

    @Test
    void preservesPlainEmptyAndLiteralJsonText() {
        assertEquals("", WidgetTooltipText.replacePlaceholdersAndFormattingCodes(""));
        assertEquals("Plain tooltip", WidgetTooltipText.replacePlaceholdersAndFormattingCodes("Plain tooltip"));
        assertEquals("{\"text\":\"Literal tooltip\",\"color\":\"red\"}", WidgetTooltipText.replacePlaceholdersAndFormattingCodes("{\"text\":\"Literal tooltip\",\"color\":\"red\"}"));
    }

    @Test
    void exactlyEmptyEditorValueIsValidForClearing() {
        assertTrue(WidgetTooltipText.isEditorValueValid(""));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", "   "})
    void nullAndSpaceOnlyEditorValuesRemainInvalid(String value) {
        assertFalse(WidgetTooltipText.isEditorValueValid(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tooltip", "Tooltip with spaces", "\t", "\n"})
    void previouslyAcceptedNonEmptyEditorValuesRemainValid(String value) {
        assertTrue(WidgetTooltipText.isEditorValueValid(value));
    }

    @ParameterizedTest
    @MethodSource("storedToEditorValues")
    void storedValuesConvertToEditorValues(String stored, String expected) {
        assertEquals(expected, WidgetTooltipText.toEditorValue(stored));
    }

    @ParameterizedTest
    @MethodSource("editorToStoredValues")
    void editorValuesConvertToStoredValues(String editor, String expected) {
        assertEquals(expected, WidgetTooltipText.fromEditorValue(editor));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void nullAndEmptyStoredValuesNormalizeToNull(String value) {
        assertNull(WidgetTooltipText.normalizeStoredValue(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tooltip", " ", "\t", "\n", "%n%"})
    void nonEmptyStoredValuesRemainUnchanged(String value) {
        assertEquals(value, WidgetTooltipText.normalizeStoredValue(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tooltip", "First%n%Second", "First%n%%n%Third", " ", "\t"})
    void persistedValuesRoundTripThroughTheEditor(String stored) {
        assertEquals(stored, WidgetTooltipText.fromEditorValue(WidgetTooltipText.toEditorValue(stored)));
    }

    @Test
    void normalizedEmptyTooltipRemovesExistingSerializedProperty() {
        PropertyContainer container = new PropertyContainer("test");
        container.putProperty("description", "Existing tooltip");

        container.putProperty("description", WidgetTooltipText.normalizeStoredValue(""));

        assertFalse(container.hasProperty("description"));
    }

    private static Stream<Arguments> storedToEditorValues() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of("Tooltip", "Tooltip"),
                Arguments.of("First%n%Second", "First\nSecond"),
                Arguments.of("First%n%%n%Third", "First\n\nThird"),
                Arguments.of("Already\nMultiline", "Already\nMultiline")
        );
    }

    private static Stream<Arguments> editorToStoredValues() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of("Tooltip", "Tooltip"),
                Arguments.of("First\nSecond", "First%n%Second"),
                Arguments.of("First\n\nThird", "First%n%%n%Third"),
                Arguments.of("First\r\nSecond", "First\r%n%Second"),
                Arguments.of("Literal%n%Token", "Literal%n%Token")
        );
    }

}
