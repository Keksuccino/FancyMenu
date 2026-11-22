package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorMarkdownFormatting.Edit;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorMarkdownFormatting.Shortcut;
import org.junit.jupiter.api.Test;

import static de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorMarkdownFormatting.Shortcut.ALIGN_CENTER;
import static de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorMarkdownFormatting.Shortcut.ALIGN_LEFT;
import static de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorMarkdownFormatting.Shortcut.ALIGN_RIGHT;
import static de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorMarkdownFormatting.Shortcut.BOLD;
import static de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorMarkdownFormatting.Shortcut.ITALIC;
import static de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorMarkdownFormatting.Shortcut.UNDERLINE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The texts of these tests mark the selection with square brackets, so "a [b] c" has the "b" selected and "a [] c" has the cursor in front of the "c".
 */
class TextEditorMarkdownFormattingTest {

    @Test
    void shortcutsGetFoundByTheNamesOfTheirKeys() {
        assertEquals(BOLD, Shortcut.byKeyName("b"));
        assertEquals(ITALIC, Shortcut.byKeyName("i"));
        assertEquals(UNDERLINE, Shortcut.byKeyName("u"));
        assertEquals(ALIGN_LEFT, Shortcut.byKeyName("l"));
        assertEquals(ALIGN_CENTER, Shortcut.byKeyName("e"));
        assertEquals(ALIGN_RIGHT, Shortcut.byKeyName("r"));
        assertNull(Shortcut.byKeyName("x"));
        assertNull(Shortcut.byKeyName(""));
        assertNull(Shortcut.byKeyName(null));
    }

    @Test
    void formatsTheSelectedText() {
        assertFormats(BOLD, "make [this] bold", "make **[this]** bold");
        assertFormats(ITALIC, "make [this] italic", "make *[this]* italic");
        assertFormats(UNDERLINE, "make [this] fancy", "make ++[this]++ fancy");
    }

    @Test
    void takesTheFormatAwayFromTheTextBetweenItsMarkers() {
        assertFormats(BOLD, "make **[this]** plain", "make [this] plain");
        assertFormats(ITALIC, "make *[this]* plain", "make [this] plain");
        assertFormats(UNDERLINE, "make ++[this]++ plain", "make [this] plain");
    }

    @Test
    void takesTheFormatAwayFromSelectedTextThatStartsAndEndsWithItsMarkers() {
        assertFormats(BOLD, "make [**this**] plain", "make [this] plain");
        assertFormats(ITALIC, "make [*this*] plain", "make [this] plain");
        assertFormats(UNDERLINE, "make [++this++] plain", "make [this] plain");
    }

    @Test
    void formattingTwiceGivesBackTheText() {
        for (Shortcut shortcut : new Shortcut[] {BOLD, ITALIC, UNDERLINE}) {
            Edit formatted = apply(shortcut, "a [few words] here");
            Edit unformatted = TextEditorMarkdownFormatting.apply(shortcut, formatted.text(), formatted.selectionStart(), formatted.selectionEnd());
            assertEquals("a [few words] here", mark(unformatted), shortcut.name());
        }
    }

    @Test
    void boldAndItalicMarkersShareTheirAsterisks() {
        assertFormats(BOLD, "*[word]*", "***[word]***");
        assertFormats(ITALIC, "**[word]**", "***[word]***");
        assertFormats(BOLD, "***[word]***", "*[word]*");
        assertFormats(ITALIC, "***[word]***", "**[word]**");
        assertFormats(BOLD, "[***word***]", "[*word*]");
        assertFormats(ITALIC, "[***word***]", "[**word**]");
    }

    @Test
    void formatsWholeMarkerRowsEvenIfTheSelectionCutsThem() {
        assertFormats(BOLD, "*[**word**]*", "[*word*]");
    }

    @Test
    void leavesWhitespaceAtTheEdgesOfTheSelectionOutside() {
        assertFormats(BOLD, "make[ this ]bold", "make **[this]** bold");
        assertFormats(UNDERLINE, "[\n  two words  \n]", "\n  ++[two words]++  \n");
    }

    @Test
    void selectionOfNothingButWhitespaceChangesNothing() {
        assertChangesNothing(BOLD, "a[   ]b");
        assertChangesNothing(UNDERLINE, "a[\n\n]b");
    }

    @Test
    void cursorGetsPutBetweenNewMarkers() {
        assertFormats(BOLD, "a []b", "a **[]**b");
        assertFormats(ITALIC, "a []b", "a *[]*b");
        assertFormats(UNDERLINE, "a []b", "a ++[]++b");
    }

    @Test
    void cursorBetweenMarkersOfNothingTakesThemAway() {
        assertFormats(BOLD, "a **[]**b", "a []b");
        assertFormats(ITALIC, "a *[]*b", "a []b");
        assertFormats(UNDERLINE, "a ++[]++b", "a []b");
        assertFormats(ITALIC, "***[]***", "**[]**");
    }

    @Test
    void formatsPartlyFormattedTextAsAWhole() {
        assertFormats(BOLD, "[a **b** c]", "**[a b c]**");
        assertFormats(UNDERLINE, "[a ++b++ c ++d++]", "++[a b c d]++");
        // Italic markers are no bold markers, so they stay
        assertFormats(BOLD, "[a *b* c]", "**[a *b* c]**");
    }

    @Test
    void selectionThatStartsOrEndsInsideOfFormattedTextChangesNothing() {
        assertChangesNothing(BOLD, "**a [b** c]");
        assertChangesNothing(UNDERLINE, "[a ++b] c++");
    }

    @Test
    void escapedMarkersAreText() {
        assertFormats(BOLD, "[a \\*\\* b]", "**[a \\*\\* b]**");
    }

    @Test
    void formatsTheTextOfLinesWithoutTheirPrefixes() {
        assertFormats(BOLD, "[- item]", "- **[item]**");
        assertFormats(BOLD, "[  * nested item]", "  * **[nested item]**");
        assertFormats(BOLD, "[1. first]", "1. **[first]**");
        assertFormats(BOLD, "[# Title]", "# **[Title]**");
        assertFormats(BOLD, "[> quote]", "> **[quote]**");
        assertFormats(ITALIC, "[- one\n- two]", "- *[one\n- two]*");
        assertFormats(UNDERLINE, "[first\n- ]second", "++[first]++\n- second");
    }

    @Test
    void formatsTextAcrossLines() {
        assertFormats(BOLD, "[first\nsecond]", "**[first\nsecond]**");
    }

    @Test
    void leavesMarkerLinesOfBlocksAtTheEdgesOfTheSelectionOutside() {
        assertFormats(BOLD, "[^^^\ncentered\n^^^]", "^^^\n**[centered]**\n^^^");
        assertFormats(BOLD, "[|||\nright\n|||]", "|||\n**[right]**\n|||");
        assertChangesNothing(BOLD, "[^^^]");
    }

    @Test
    void alignsTheLineOfTheCursor() {
        assertFormats(ALIGN_CENTER, "a\n[]b\nc", "a\n^^^\n[]b\n^^^\nc");
        assertFormats(ALIGN_RIGHT, "a\n[]b\nc", "a\n|||\n[]b\n|||\nc");
    }

    @Test
    void alignsTheSelectedLines() {
        assertFormats(ALIGN_RIGHT, "[a\nb]\nc", "|||\n[a\nb]\n|||\nc");
        assertFormats(ALIGN_CENTER, "a\nb[c\nd]e\nf", "a\n^^^\nb[c\nd]e\n^^^\nf");
    }

    @Test
    void selectionThatEndsAtTheStartOfALineLeavesThatLineOut() {
        assertFormats(ALIGN_CENTER, "[a\n]b", "^^^\n[a\n^^^\n]b");
    }

    @Test
    void centeringOrRightAligningAlignedLinesAlignsThemLeftAgain() {
        assertFormats(ALIGN_CENTER, "a\n^^^\n[]b\n^^^\nc", "a\n[]b\nc");
        assertFormats(ALIGN_RIGHT, "|||\n[a\nb]\n|||", "[a\nb]");
    }

    @Test
    void alignmentReplacesTheOtherAlignment() {
        assertFormats(ALIGN_RIGHT, "a\n^^^\n[]b\n^^^\nc", "a\n|||\n[]b\n|||\nc");
        assertFormats(ALIGN_LEFT, "^^^\n[a\nb]\n^^^", "[a\nb]");
    }

    @Test
    void aligningLinesInsideOfABlockSplitsIt() {
        assertFormats(ALIGN_LEFT, "^^^\nx\n[]y\nz\n^^^", "^^^\nx\n^^^\n[]y\n^^^\nz\n^^^");
        assertFormats(ALIGN_RIGHT, "^^^\nx\n[]y\n^^^", "^^^\nx\n^^^\n|||\n[]y\n|||");
    }

    @Test
    void aligningLinesNextToABlockOfTheSameAlignmentJoinsThem() {
        assertFormats(ALIGN_CENTER, "^^^\na\n^^^\n[]b", "^^^\na\n[]b\n^^^");
        assertFormats(ALIGN_CENTER, "[]a\n^^^\nb\n^^^", "^^^\n[]a\nb\n^^^");
        assertFormats(ALIGN_CENTER, "^^^\na\n^^^\n[]b\n^^^\nc\n^^^", "^^^\na\n[]b\nc\n^^^");
    }

    @Test
    void selectionOfABlockWithItsMarkerLinesSelectsTheAlignedLinesAfterwards() {
        assertFormats(ALIGN_RIGHT, "[^^^\na\n^^^]", "|||\n[a]\n|||");
    }

    @Test
    void aligningLinesThatHaveTheAlignmentAlreadyChangesNothing() {
        assertChangesNothing(ALIGN_LEFT, "a\n[]b\nc");
        assertChangesNothing(ALIGN_LEFT, "[]^^^\na\n^^^");
    }

    @Test
    void markerLinesInsideOfCodeBlocksStayText() {
        assertFormats(ALIGN_CENTER, "[```\n^^^\n```]", "^^^\n[```\n^^^\n```]\n^^^");
    }

    private static void assertFormats(Shortcut shortcut, String before, String expected) {
        assertEquals(expected, mark(apply(shortcut, before)), before);
    }

    private static void assertChangesNothing(Shortcut shortcut, String before) {
        int start = before.indexOf('[');
        int end = before.indexOf(']') - 1;
        assertNull(TextEditorMarkdownFormatting.apply(shortcut, before.replace("[", "").replace("]", ""), start, end), before);
    }

    private static Edit apply(Shortcut shortcut, String before) {
        int start = before.indexOf('[');
        int end = before.indexOf(']') - 1;
        Edit edit = TextEditorMarkdownFormatting.apply(shortcut, before.replace("[", "").replace("]", ""), start, end);
        if (edit == null) throw new AssertionError("Changed nothing: " + before);
        return edit;
    }

    private static String mark(Edit edit) {
        return new StringBuilder(edit.text()).insert(edit.selectionEnd(), ']').insert(edit.selectionStart(), '[').toString();
    }

}
