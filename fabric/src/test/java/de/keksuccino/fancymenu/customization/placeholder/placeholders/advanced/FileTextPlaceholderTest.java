package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTextPlaceholderTest {

    @Test
    void convertsDocumentedTextualNewlineSeparator() {
        assertEquals("first\nsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "\\n"));
    }

    @Test
    void preservesNormalAndEmptySeparators() {
        assertEquals("first | second", FileTextPlaceholder.joinLines(List.of("first", "second"), " | "));
        assertEquals("firstsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), ""));
    }

    @Test
    void returnsEmptyTextForEmptyInput() {
        assertEquals("", FileTextPlaceholder.joinLines(List.of(), "\\n"));
    }

    @Test
    void preservesActualLineEndings() {
        assertEquals("first\nsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "\n"));
        assertEquals("first\r\nsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "\r\n"));
    }

    @Test
    void decodesOnlyTextualNewlinesAndPreservesOtherBackslashes() {
        assertEquals("first\\r\nsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "\\r\\n"));
        assertEquals("first\\\nsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "\\\\n"));
        assertEquals("firstC:\\temp\\foldersecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "C:\\temp\\folder"));
    }

    @Test
    void resolvesSeparatorForEachUseWithoutMutatingCachedLines() {
        List<String> cachedLines = new ArrayList<>(List.of("first", "second"));

        assertEquals("first\nsecond", FileTextPlaceholder.joinLines(cachedLines, "\\n"));
        assertEquals("first, second", FileTextPlaceholder.joinLines(cachedLines, ", "));
        assertEquals(List.of("first", "second"), cachedLines);
    }

}
