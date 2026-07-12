package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownRenderTextNormalizerTest {

    @Test
    void convertsEveryRecognizedLowercaseLegacyFormattingCode() {
        String source = "&0&1&2&3&4&5&6&7&8&9&a&b&c&d&e&f&k&l&m&n&o&r";

        assertEquals("§0§1§2§3§4§5§6§7§8§9§a§b§c§d§e§f§k§l§m§n§o§r", MarkdownRenderTextNormalizer.normalize(source, true));
    }

    @Test
    void convertsFormattingInTextWithoutPlaceholders() {
        assertEquals("Plain §aformatted", MarkdownRenderTextNormalizer.normalize("Plain &aformatted", true));
    }

    @Test
    void preservesExistingSectionCodes() {
        assertEquals("§aGreen §lBold", MarkdownRenderTextNormalizer.normalize("§aGreen §lBold", true));
    }

    @Test
    void preservesUppercaseUnknownAndHexPrefixCodes() {
        assertEquals("&A &L &z &x", MarkdownRenderTextNormalizer.normalize("&A &L &z &x", true));
    }

    @Test
    void convertsRecognizedCodeAfterAnExtraAmpersand() {
        assertEquals("&§a", MarkdownRenderTextNormalizer.normalize("&&a", true));
    }

    @Test
    void convertsPlaceholderProducedFormattingBeforeNormalizingNewlines() {
        assertEquals("§aFirst\nSecond\nThird\nFourth", MarkdownRenderTextNormalizer.normalize("&aFirst%n%Second\rThird\\nFourth", true));
    }

    @Test
    void removesHtmlBreakOnlyWhenEnabled() {
        assertEquals("FirstSecond", MarkdownRenderTextNormalizer.normalize("First<br>Second", true));
        assertEquals("First<br>Second", MarkdownRenderTextNormalizer.normalize("First<br>Second", false));
    }

    @Test
    void normalizationIsIdempotentAndDoesNotMutateSourceText() {
        String source = new String("&aFirst%n%Second<br>");
        String normalized = MarkdownRenderTextNormalizer.normalize(source, true);

        assertEquals("&aFirst%n%Second<br>", source);
        assertEquals("§aFirst\nSecond", normalized);
        assertEquals(normalized, MarkdownRenderTextNormalizer.normalize(normalized, true));
    }

    @Test
    void preservesUpstreamFormattingConversionInsideMarkdownDestinations() {
        assertEquals("[link](https://example.test/§a)", MarkdownRenderTextNormalizer.normalize("[link](https://example.test/&a)", true));
    }

}
