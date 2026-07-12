package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderParser global state")
class WidgetTooltipTextTest {

    @Test
    void editorValidationAllowsExactEmptyToClearTooltip() {
        assertTrue(WidgetTooltipText.isEditorValueValid(""));
        assertTrue(WidgetTooltipText.isEditorValueValid("Tooltip"));
        assertTrue(WidgetTooltipText.isEditorValueValid("\n"));
        assertFalse(WidgetTooltipText.isEditorValueValid(null));
        assertFalse(WidgetTooltipText.isEditorValueValid("   "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void legacyMissingOrEmptyStoredValueNormalizesToNull(String value) {
        assertNull(WidgetTooltipText.normalizeStoredValue(value));
        assertNull(WidgetTooltipText.toEditorValue(value));
        assertNull(WidgetTooltipText.fromEditorValue(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "Tooltip", "%n%"})
    void normalizationPreservesEveryNonEmptyStoredValue(String value) {
        assertEquals(value, WidgetTooltipText.normalizeStoredValue(value));
    }

    @Test
    void storedMultilineTokensAreDecodedForEditing() {
        assertEquals("First\nSecond\nThird", WidgetTooltipText.toEditorValue("First%n%Second%n%Third"));
    }

    @Test
    void editorNewlinesAreEncodedForPersistentStorage() {
        assertEquals("First%n%Second%n%Third", WidgetTooltipText.fromEditorValue("First\nSecond\nThird"));
    }

    @Test
    void multilineConversionRoundTripsWithoutChangingOtherText() {
        String stored = "Prefix %value%&a%n%Suffix";
        assertEquals(stored, WidgetTooltipText.fromEditorValue(WidgetTooltipText.toEditorValue(stored)));
    }

    @Test
    void unconditionalNormalizedWritesRemoveStaleTooltipProperties() {
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", "stale button tooltip");
        serialized.putProperty("tooltip", "stale slider tooltip");

        serialized.putProperty("description", WidgetTooltipText.normalizeStoredValue(""));
        serialized.putProperty("tooltip", WidgetTooltipText.normalizeStoredValue(null));

        assertFalse(serialized.hasProperty("description"));
        assertFalse(serialized.hasProperty("tooltip"));
    }

    @Test
    void whitespaceIsNotNormalizedIntoAValidClearOperation() {
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", WidgetTooltipText.normalizeStoredValue("   "));

        assertTrue(serialized.hasProperty("description"));
        assertEquals("   ", serialized.getValue("description"));
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
