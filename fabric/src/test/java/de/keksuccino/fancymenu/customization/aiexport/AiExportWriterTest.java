package de.keksuccino.fancymenu.customization.aiexport;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiExportWriterTest {

    @Test
    void inlineCodeUsesFencesLongerThanTheBackticksOfTheValue() {
        assertEquals("`abc`", AiExportWriter.inlineCode("abc"));
        assertEquals("`` a`b ``", AiExportWriter.inlineCode("a`b"));
        assertEquals("``` a``b ```", AiExportWriter.inlineCode("a``b"));
    }

    @Test
    void emptyValuesStayVisible() {
        assertTrue(AiExportWriter.inlineCode("").length() > 0);
    }

    @Test
    void multiLineValuesBecomeIndentedCodeBlocksBelowTheirBullet() {
        AiExportWriter writer = new AiExportWriter();
        writer.bullet("parent");
        writer.nested(() -> writer.field("label", "first line\nsecond ```line"));
        List<String> lines = lines(writer.build());

        int labelLine = indexOfLineContaining(lines, "label");
        assertEquals(2, indentationOf(lines.get(labelLine)));
        String fence = lines.get(labelLine + 1).strip();
        assertTrue(fence.matches("`{4,}"), "The fence must be longer than the backticks in the value: " + fence);
        assertEquals(4, indentationOf(lines.get(labelLine + 1)));
        assertEquals("    first line", lines.get(labelLine + 2));
        assertEquals("    second ```line", lines.get(labelLine + 3));
        assertEquals("    " + fence, lines.get(labelLine + 4));
    }

    @Test
    void windowsLineBreaksDoNotLeaveCarriageReturns() {
        AiExportWriter writer = new AiExportWriter();
        writer.field("label", "a\r\nb\rc");
        assertTrue(writer.build().indexOf('\r') < 0);
    }

    @Test
    void nestedBulletsGetIndentedByTwoSpacesPerLevel() {
        AiExportWriter writer = new AiExportWriter();
        writer.bullet("level_zero");
        writer.nested(() -> {
            writer.bullet("level_one");
            writer.nested(() -> writer.bullet("level_two"));
        });
        writer.bullet("back_at_zero");
        List<String> lines = lines(writer.build());
        assertEquals(0, indentationOf(lines.get(indexOfLineContaining(lines, "level_zero"))));
        assertEquals(2, indentationOf(lines.get(indexOfLineContaining(lines, "level_one"))));
        assertEquals(4, indentationOf(lines.get(indexOfLineContaining(lines, "level_two"))));
        assertEquals(0, indentationOf(lines.get(indexOfLineContaining(lines, "back_at_zero"))));
    }

    @Test
    void headingsStartTopLevelSectionsAgain() {
        AiExportWriter writer = new AiExportWriter();
        writer.indent().indent();
        writer.heading(2, "section");
        writer.bullet("after_heading");
        List<String> lines = lines(writer.build());
        assertEquals(0, indentationOf(lines.get(indexOfLineContaining(lines, "after_heading"))));
        assertTrue(lines.get(indexOfLineContaining(lines, "section")).startsWith("## "));
        assertEquals("", lines.get(indexOfLineContaining(lines, "section") + 1), "Headings need a blank line after them");
    }

    @Test
    void outdentNeverGoesBelowTopLevel() {
        AiExportWriter writer = new AiExportWriter();
        writer.outdent().outdent();
        assertEquals(0, writer.getIndentLevel());
    }

    @Test
    void finishedTextEndsWithExactlyOneLineBreak() {
        AiExportWriter writer = new AiExportWriter();
        writer.paragraph("text");
        String built = writer.build();
        assertTrue(built.endsWith("text\n"), built);
    }

    private static List<String> lines(String text) {
        return Arrays.asList(text.split("\n", -1));
    }

    private static int indexOfLineContaining(List<String> lines, String token) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(token)) return i;
        }
        throw new AssertionError("No line contains " + token + ":\n" + String.join("\n", lines));
    }

    private static int indentationOf(String line) {
        int count = 0;
        while ((count < line.length()) && (line.charAt(count) == ' ')) count++;
        return count;
    }

}
