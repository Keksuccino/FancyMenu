package de.keksuccino.fancymenu.customization.variables;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableTextUtilsTest {

    @Test
    void typedLineBreaksInSeparatorsBecomeRealLineBreaks() {
        assertAll(() -> assertEquals("\n", VariableTextUtils.resolveSeparator("\\n")), () -> assertEquals(",\n", VariableTextUtils.resolveSeparator(",\\n")),
                () -> assertEquals(", ", VariableTextUtils.resolveSeparator(", ")), () -> assertEquals("", VariableTextUtils.resolveSeparator("")));
    }

    @Test
    void splittingUsesTheSeparatorWithTrimAndSkipOptions() {
        String text = " apple, banana,, cherry ,";

        assertAll(() -> assertEquals(List.of("apple", "banana", "cherry"), VariableTextUtils.splitToEntries(text, ",", true, true)),
                () -> assertEquals(List.of("apple", "banana", "", "cherry", ""), VariableTextUtils.splitToEntries(text, ",", true, false)),
                () -> assertEquals(List.of(" apple", " banana", " cherry "), VariableTextUtils.splitToEntries(text, ",", false, true)),
                () -> assertEquals(List.of(" apple", " banana", "", " cherry ", ""), VariableTextUtils.splitToEntries(text, ",", false, false)));
    }

    @Test
    void typedLineBreakSeparatorSplitsAtEveryKindOfLineBreak() {
        assertAll(() -> assertEquals(List.of("a", "b", "c", "d"), VariableTextUtils.splitToEntries("a\nb\r\nc\rd", "\\n", false, false)),
                () -> assertEquals(List.of("a", "b"), VariableTextUtils.splitToEntries("a,\nb", ",\\n", false, false)));
    }

    @Test
    void emptySeparatorKeepsTheWholeTextAsOneEntry() {
        assertAll(() -> assertEquals(List.of("a, b\nc"), VariableTextUtils.splitToEntries("a, b\nc", "", false, false)),
                () -> assertEquals(List.of("text"), VariableTextUtils.splitToEntries("  text  ", "", true, false)),
                () -> assertEquals(List.of(), VariableTextUtils.splitToEntries("   ", "", true, true)),
                () -> assertEquals(List.of(""), VariableTextUtils.splitToEntries("   ", "", true, false)));
    }

    @Test
    void emptyTextGivesNoEntriesForEverySeparator() {
        assertAll(() -> assertEquals(List.of(), VariableTextUtils.splitToEntries("", ",", false, false)), () -> assertEquals(List.of(), VariableTextUtils.splitToEntries("", "", false, false)),
                () -> assertEquals(List.of(), VariableTextUtils.splitToEntries("", "\\n", false, false)));
    }

    @Test
    void joiningIsTheInverseOfSplittingWithTheSameSeparator() {
        List<String> entries = List.of("apple", " banana ", "", "cherry");

        assertAll(() -> assertEquals("apple,  banana , , cherry", VariableTextUtils.joinEntries(entries, ", ")), () -> assertEquals("apple\n banana \n\ncherry", VariableTextUtils.joinEntries(entries, "\\n")),
                () -> assertEquals(entries, VariableTextUtils.splitToEntries(VariableTextUtils.joinEntries(entries, "|"), "|", false, false)),
                () -> assertEquals(entries, VariableTextUtils.splitToEntries(VariableTextUtils.joinEntries(entries, "\\n"), "\\n", false, false)),
                () -> assertEquals("", VariableTextUtils.joinEntries(List.of(), ", ")));
    }

    @Test
    void lineBreaksAreDetectedInAllForms() {
        assertAll(() -> assertTrue(VariableTextUtils.containsLineBreak("a\nb")), () -> assertTrue(VariableTextUtils.containsLineBreak("a\rb")), () -> assertTrue(VariableTextUtils.containsLineBreak("a\r\nb")),
                () -> assertFalse(VariableTextUtils.containsLineBreak("a\\nb")), () -> assertFalse(VariableTextUtils.containsLineBreak("")));
    }

    @Test
    void singleLinesReplaceEveryLineBreakWithOneMarker() {
        assertAll(() -> assertEquals("a|b|c|d", VariableTextUtils.toSingleLine("a\nb\r\nc\rd", "|", 100)), () -> assertEquals("||", VariableTextUtils.toSingleLine("\n\r\n", "|", 100)),
                () -> assertEquals("plain", VariableTextUtils.toSingleLine("plain", "|", 100)));
    }

    @Test
    void singleLinesGetCutOffAfterTheCharacterLimit() {
        assertAll(() -> assertEquals("abc" + VariableTextUtils.ELLIPSIS, VariableTextUtils.toSingleLine("abcdef", "|", 3)), () -> assertEquals("abc", VariableTextUtils.toSingleLine("abc", "|", 3)),
                () -> assertEquals(VariableTextUtils.ELLIPSIS, VariableTextUtils.toSingleLine("abc", "|", 0)));
    }

    @Test
    void cutsNeverKeepHalfOfASurrogatePair() {
        String text = "a😀b";

        assertEquals("a" + VariableTextUtils.ELLIPSIS, VariableTextUtils.toSingleLine(text, "|", 2));
    }

    @Test
    void entryPreviewsJoinEntriesAndStopAtTheCharacterLimit() {
        List<String> entries = List.of("apple", "banana", "cherry");

        assertAll(() -> assertEquals("apple, banana, cherry", VariableTextUtils.toEntriesPreview(entries, ", ", "|", 100)),
                () -> assertEquals("apple, banana" + VariableTextUtils.ELLIPSIS, VariableTextUtils.toEntriesPreview(entries, ", ", "|", 13)),
                () -> assertEquals("apple, ban" + VariableTextUtils.ELLIPSIS, VariableTextUtils.toEntriesPreview(entries, ", ", "|", 10)),
                () -> assertEquals("a|b, c", VariableTextUtils.toEntriesPreview(List.of("a\nb", "c"), ", ", "|", 100)),
                () -> assertEquals("", VariableTextUtils.toEntriesPreview(List.of(), ", ", "|", 100)));
    }

    @Test
    void entryPreviewsOfHugeListsOnlyReadWhatTheyShow() {
        List<String> entries = Collections.nCopies(1_000_000, "entry");

        String preview = VariableTextUtils.toEntriesPreview(entries, ", ", "|", 50);

        assertTrue(preview.length() <= 51 + VariableTextUtils.ELLIPSIS.length(), preview);
        assertTrue(preview.endsWith(VariableTextUtils.ELLIPSIS), preview);
    }

}
