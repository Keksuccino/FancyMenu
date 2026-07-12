package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
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
    void validatesClearableAndPopulatedEditorValuesWithoutAllowingSpaceOnlyText() {
        assertFalse(WidgetTooltipText.isEditorValueValid(null));
        assertTrue(WidgetTooltipText.isEditorValueValid(""));
        assertFalse(WidgetTooltipText.isEditorValueValid(" "));
        assertFalse(WidgetTooltipText.isEditorValueValid("     "));
        assertTrue(WidgetTooltipText.isEditorValueValid("Tooltip"));
        assertTrue(WidgetTooltipText.isEditorValueValid("  Tooltip  "));
    }

    @Test
    void normalizesOnlyMissingAndExactlyEmptyStoredValues() {
        assertNull(WidgetTooltipText.normalizeStoredValue(null));
        assertNull(WidgetTooltipText.normalizeStoredValue(""));
        assertEquals(" ", WidgetTooltipText.normalizeStoredValue(" "));
        assertEquals("Tooltip", WidgetTooltipText.normalizeStoredValue("Tooltip"));
    }

    @Test
    void convertsStoredNewlineTokensToEditorLineBreaks() {
        assertNull(WidgetTooltipText.toEditorValue(null));
        assertNull(WidgetTooltipText.toEditorValue(""));
        assertEquals("First\n\nSecond", WidgetTooltipText.toEditorValue("First%n%%n%Second"));
        assertEquals("Already\nMultiline", WidgetTooltipText.toEditorValue("Already\nMultiline"));
    }

    @Test
    void convertsEditorLineBreaksToStoredNewlineTokens() {
        assertNull(WidgetTooltipText.fromEditorValue(null));
        assertNull(WidgetTooltipText.fromEditorValue(""));
        assertEquals("First%n%%n%Second", WidgetTooltipText.fromEditorValue("First\n\nSecond"));
        assertEquals("Windows\r%n%Line", WidgetTooltipText.fromEditorValue("Windows\r\nLine"));
    }

    @Test
    void preservesCanonicalStoredValuesAcrossEditorRoundTrips() {
        String stored = "First%n%%n%Second%n%Third";

        assertEquals(stored, WidgetTooltipText.fromEditorValue(WidgetTooltipText.toEditorValue(stored)));
        assertNull(WidgetTooltipText.fromEditorValue(WidgetTooltipText.toEditorValue("")));
        assertNull(WidgetTooltipText.fromEditorValue(WidgetTooltipText.toEditorValue(null)));
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
