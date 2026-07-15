package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownRendererRenderTextTest {

    @Test
    void preservesRecognizedDirectAmpersandFormattingCodesForTheContextAwareParser() {
        assertEquals("&0Black &aGreen &lBold &rReset", MarkdownRenderer.buildRenderText("&0Black &aGreen &lBold &rReset", true));
    }

    @Test
    void preservesFormattingCodesProducedBeforePlaceholderExpansion() {
        String marker = "[fancymenu_test_markdown_dynamic_formatting]";
        long processorId = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, input -> marker.equals(input) ? "&cPlaceholder &nText" : input);
        try {
            assertEquals("&cPlaceholder &nText", PlaceholderParser.replacePlaceholders(marker));
            assertEquals("&cPlaceholder &nText", MarkdownRenderer.buildRenderText(marker, true));
        } finally {
            PlaceholderParser.removeParsingProcessor(processorId);
        }
    }

    @Test
    void leavesUnrecognizedAmpersandCodesAndOrdinaryAmpersandsUntouched() {
        assertEquals("&qUnknown &ZUppercase bread & butter", MarkdownRenderer.buildRenderText("&qUnknown &ZUppercase bread & butter", true));
    }

    @Test
    void preservesNewlineAndHtmlBreakNormalizationOrder() {
        String input = "&aFirst%n%Second\rThird\\nFourth<br>Fifth";

        assertEquals("&aFirst\nSecond\nThird\nFourthFifth", MarkdownRenderer.buildRenderText(input, true));
        assertEquals("&aFirst\nSecond\nThird\nFourth<br>Fifth", MarkdownRenderer.buildRenderText(input, false));
    }
}
