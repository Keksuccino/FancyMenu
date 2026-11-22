package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.variables.UserVariableSnapshot;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariablesAiExporterTest {

    @Test
    void listVariablesListTheirEntriesInOrderOneLevelBelowTheirName() {
        List<String> lines = write(UserVariableSnapshot.ofList("fruits", List.of("apple", "banana", "cherry"), false));

        int nameLine = indexOfLineContaining(lines, "$$fruits");
        assertAll(() -> assertEquals(0, indentationOf(lines.get(nameLine))),
                () -> assertTrue(lines.get(nameLine + 1).contains("`apple`")), () -> assertEquals(2, indentationOf(lines.get(nameLine + 1))),
                () -> assertTrue(lines.get(nameLine + 2).contains("`banana`")), () -> assertEquals(2, indentationOf(lines.get(nameLine + 2))),
                () -> assertTrue(lines.get(nameLine + 3).contains("`cherry`")), () -> assertEquals(2, indentationOf(lines.get(nameLine + 3))),
                () -> assertEquals(-1, indexOfLineContaining(lines, "[\"apple\"")));
    }

    @Test
    void hugeListsOnlyListTheirFirstEntries() {
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < VariablesAiExporter.MAX_LISTED_ENTRIES + 50; i++) entries.add("entry_" + i);

        List<String> lines = write(UserVariableSnapshot.ofList("big", entries, false));

        long listed = lines.stream().filter(line -> line.contains("`entry_")).count();
        assertAll(() -> assertEquals(VariablesAiExporter.MAX_LISTED_ENTRIES, listed), () -> assertEquals(-1, indexOfLineContaining(lines, "`entry_" + VariablesAiExporter.MAX_LISTED_ENTRIES + "`")));
    }

    @Test
    void textVariablesShowTheirValueAsRawValue() {
        List<String> lines = write(new UserVariableSnapshot("player", "Steve", false));

        int nameLine = indexOfLineContaining(lines, "$$player");
        assertAll(() -> assertTrue(lines.get(nameLine).contains("`Steve`")), () -> assertEquals(nameLine, lines.size() - 1));
    }

    private static List<String> write(UserVariableSnapshot variable) {
        AiExportWriter writer = new AiExportWriter();
        VariablesAiExporter.writeVariable(writer, variable);
        return Arrays.asList(writer.build().split("\n"));
    }

    private static int indexOfLineContaining(List<String> lines, String part) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(part)) return i;
        }
        return -1;
    }

    private static int indentationOf(String line) {
        int indentation = 0;
        while ((indentation < line.length()) && (line.charAt(indentation) == ' ')) indentation++;
        return indentation;
    }

}
