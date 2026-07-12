package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownRenderTextFormatterTest {

    @Test
    void convertsDirectColorCodes() {
        String input = "&0&1&2&3&4&5&6&7&8&9&a&b&c&d&e&f";
        String expected = "§0§1§2§3§4§5§6§7§8§9§a§b§c§d§e§f";

        assertEquals(expected, format(input));
    }

    @Test
    void convertsStyleAndResetCodes() {
        assertEquals("§kMagic §lBold §mStrike §nUnderline §oItalic §rReset", format("&kMagic &lBold &mStrike &nUnderline &oItalic &rReset"));
    }

    @Test
    void convertsCodesProducedByPlaceholderExpansion() {
        String placeholder = "{dynamic_text}";

        assertEquals("§cDynamic §ltext", MarkdownRenderTextFormatter.expandPlaceholdersAndReplaceFormattingCodes(placeholder, input -> input.replace(placeholder, "&cDynamic &ltext")));
    }

    @Test
    void preservesExistingSectionCodes() {
        assertEquals("§aGreen §lBold", format("§aGreen §lBold"));
    }

    @Test
    void preservesOrdinaryUppercaseAndUnsupportedAmpersands() {
        String input = "Fish & chips &A &L &R &g &z trailing&";

        assertEquals(input, format(input));
    }

    @Test
    void conversionIsBlindToEscapingAndWordBoundaries() {
        assertEquals("&§a \\§b §copy §reset", format("&&a \\&b &copy &reset"));
    }

    @Test
    void nonTextNoPlaceholderParsingRemainsUnchanged() {
        String input = "&aNon-text value";
        PlaceholderParser.PlaceholderCachingController originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        try {
            PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
            assertEquals(input, PlaceholderParser.replacePlaceholders(input));
        } finally {
            PlaceholderParser.setPlaceholderCachingController(originalCachingController);
        }
    }

    private static String format(String input) {
        return MarkdownRenderTextFormatter.expandPlaceholdersAndReplaceFormattingCodes(input, text -> text);
    }

}
