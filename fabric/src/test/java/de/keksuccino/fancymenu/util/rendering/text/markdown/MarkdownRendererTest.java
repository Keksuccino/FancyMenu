package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownRendererTest {

    @Test
    void preservesLegacyFormattingInDirectTextForContextAwareParsing() {
        assertEquals("&aGreen &lBold&r", MarkdownRenderer.preprocessRenderText("&aGreen &lBold&r", true, text -> text));
    }

    @Test
    void expandsPlaceholdersWithoutGloballyNormalizingFormatting() {
        AtomicReference<String> expanderInput = new AtomicReference<>();

        String processed = MarkdownRenderer.preprocessRenderText("raw placeholder", true, text -> {
            expanderInput.set(text);
            return "Status: &cOffline";
        });

        assertEquals("raw placeholder", expanderInput.get());
        assertEquals("Status: &cOffline", processed);
    }

    @Test
    void preservesLiteralAmpersandsAndUnrecognizedCodes() {
        assertEquals("Bread & Butter &zUnknown &AUppercase", MarkdownRenderer.preprocessRenderText("Bread & Butter &zUnknown &AUppercase", true, text -> text));
    }

    @Test
    void preservesExistingSectionSignFormatting() {
        assertEquals("§bAqua §oItalic", MarkdownRenderer.preprocessRenderText("§bAqua §oItalic", true, text -> text));
    }

    @Test
    void retainsExistingNewlineAndHtmlNormalization() {
        String rawText = "Percent%n%Carriage\rEscaped\\nRemoved<br>Break";

        assertEquals("Percent\nCarriage\nEscaped\nRemovedBreak", MarkdownRenderer.preprocessRenderText(rawText, true, text -> text));
        assertEquals("Percent\nCarriage\nEscaped\nRemoved<br>Break", MarkdownRenderer.preprocessRenderText(rawText, false, text -> text));
    }

}
