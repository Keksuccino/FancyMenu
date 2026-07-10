package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderParser global state")
class WidgetTooltipTextTest {

    @Test
    void nullRemainsAbsentAcrossConversions() {
        assertFalse(WidgetTooltipText.isEditorValueValid(null));
        assertNull(WidgetTooltipText.normalizeStoredValue(null));
        assertNull(WidgetTooltipText.toEditorValue(null));
        assertNull(WidgetTooltipText.fromEditorValue(null));
    }

    @Test
    void exactlyEmptyEditorValueIsValidAndClearsStorage() {
        assertTrue(WidgetTooltipText.isEditorValueValid(""));
        assertNull(WidgetTooltipText.normalizeStoredValue(""));
        assertNull(WidgetTooltipText.toEditorValue(""));
        assertNull(WidgetTooltipText.fromEditorValue(""));
    }

    @Test
    void whitespaceOnlyValueRemainsInvalidWithoutBeingNormalizedAway() {
        String whitespace = "   ";

        assertFalse(WidgetTooltipText.isEditorValueValid(whitespace));
        assertEquals(whitespace, WidgetTooltipText.normalizeStoredValue(whitespace));
        assertEquals(whitespace, WidgetTooltipText.toEditorValue(whitespace));
        assertEquals(whitespace, WidgetTooltipText.fromEditorValue(whitespace));
    }

    @Test
    void convertsStoredLineMarkersToEditorNewlines() {
        assertEquals("First line\nSecond line", WidgetTooltipText.toEditorValue("First line%n%Second line"));
    }

    @Test
    void multilineEditorAndStoredValuesRoundTrip() {
        String stored = "First%n%%n%Third%n%";
        String editor = "First\n\nThird\n";

        assertEquals(editor, WidgetTooltipText.toEditorValue(stored));
        assertEquals(stored, WidgetTooltipText.fromEditorValue(editor));
    }

    @Test
    void nonemptyTextStaysValidAndUnchanged() {
        String text = "Useful tooltip";

        assertTrue(WidgetTooltipText.isEditorValueValid(text));
        assertEquals(text, WidgetTooltipText.normalizeStoredValue(text));
        assertEquals(text, WidgetTooltipText.toEditorValue(text));
        assertEquals(text, WidgetTooltipText.fromEditorValue(text));
    }

    @Test
    void normalizedBuilderWritesRemoveStaleEmptyProperties() {
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", "stale button tooltip");
        serialized.putProperty("tooltip", "stale slider tooltip");

        serialized.putProperty("description", WidgetTooltipText.normalizeStoredValue(""));
        serialized.putProperty("tooltip", WidgetTooltipText.normalizeStoredValue(""));

        assertFalse(serialized.hasProperty("description"));
        assertFalse(serialized.hasProperty("tooltip"));
    }

    @Test
    void normalizedBuilderReadsTreatEmptyAsAbsentWithoutDiscardingSpaces() {
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", "");
        serialized.putProperty("tooltip", "   ");

        assertNull(WidgetTooltipText.normalizeStoredValue(serialized.getValue("description")));
        assertEquals("   ", WidgetTooltipText.normalizeStoredValue(serialized.getValue("tooltip")));
    }

    @Test
    void checkboxPropertyRemovesStaleEmptyValueAfterRoundTrip() {
        Property<String> tooltip = Property.stringProperty("description", null, true, true, "unused").setUserInputTextValidator(WidgetTooltipText::isEditorValueValid).setValueGetProcessor(WidgetTooltipText::toEditorValue).setValueSetProcessor(WidgetTooltipText::fromEditorValue);
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", "");

        tooltip.deserialize(serialized);
        assertNull(tooltip.get());

        tooltip.serialize(serialized);
        assertFalse(serialized.hasProperty("description"));
    }

    @Test
    void checkboxPropertyPreservesMultilineStorageRoundTrip() {
        Property<String> tooltip = Property.stringProperty("description", null, true, true, "unused").setUserInputTextValidator(WidgetTooltipText::isEditorValueValid).setValueGetProcessor(WidgetTooltipText::toEditorValue).setValueSetProcessor(WidgetTooltipText::fromEditorValue);
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", "First%n%Second");

        tooltip.deserialize(serialized);
        assertEquals("First\nSecond", tooltip.get());

        tooltip.set("First\nChanged");
        tooltip.serialize(serialized);
        assertEquals("First%n%Changed", serialized.getValue("description"));
    }

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

}
