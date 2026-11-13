package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TextEditorWindowBodyPastedTextTest {

    @Test
    void splitsAtLineFeeds() {
        assertLines("a\nb\nc", "a", "b", "c");
    }

    @Test
    void splitsWindowsLineBreaksOnceWithoutLeavingCarriageReturns() {
        assertLines("a\r\nb\r\nc", "a", "b", "c");
    }

    @Test
    void splitsAtLoneCarriageReturns() {
        assertLines("a\rb\rc", "a", "b", "c");
    }

    @Test
    void splitsAtVerticalTabsAndFormFeeds() {
        assertLines("a\u000Bb\u000Cc", "a", "b", "c");
    }

    @Test
    void splitsAtUnicodeLineBreaks() {
        assertLines("a\u0085b c d", "a", "b", "c", "d");
    }

    @Test
    void splitsMixedLineBreaks() {
        assertLines("a\r\nb\rc\nd", "a", "b", "c", "d");
    }

    @Test
    void carriageReturnInFrontOfWindowsLineBreakIsItsOwnBreak() {
        assertLines("a\r\r\nb", "a", "", "b");
    }

    @Test
    void keepsEmptyLines() {
        assertLines("a\n\nb", "a", "", "b");
        assertLines("a\r\rb", "a", "", "b");
    }

    @Test
    void keepsLeadingAndTrailingEmptyLines() {
        assertLines("\na\n", "", "a", "");
        assertLines("\r\na\r\n", "", "a", "");
        assertLines("\n", "", "");
    }

    @Test
    void textWithoutLineBreaksStaysOneLine() {
        assertLines("just one line", "just one line");
    }

    @Test
    void splittingKeepsTabsForTabExpansion() {
        assertLines("\ta\n\t\tb", "\ta", "\t\tb");
    }

    @Test
    void expandsLeadingTabsToFullTabStops() {
        assertEquals("    a", TextEditorWindowBody.expandTabs("\ta", 0));
        assertEquals("        a", TextEditorWindowBody.expandTabs("\t\ta", 0));
    }

    @Test
    void expandsTabsBehindTextToTheNextTabStop() {
        assertEquals("ab  c", TextEditorWindowBody.expandTabs("ab\tc", 0));
        assertEquals("abc d", TextEditorWindowBody.expandTabs("abc\td", 0));
        assertEquals("a       b", TextEditorWindowBody.expandTabs("a\t\tb", 0));
    }

    @Test
    void tabRightAtTabStopTakesAWholeTabWidth() {
        assertEquals("abcd    e", TextEditorWindowBody.expandTabs("abcd\te", 0));
    }

    @Test
    void keepsTabSeparatedColumnsAligned() {
        assertEquals("1   22  333 x", TextEditorWindowBody.expandTabs("1\t22\t333\tx", 0));
    }

    @Test
    void countsTabStopsFromTheStartColumn() {
        assertEquals("   a", TextEditorWindowBody.expandTabs("\ta", 1));
        assertEquals(" a", TextEditorWindowBody.expandTabs("\ta", 3));
        assertEquals("    a", TextEditorWindowBody.expandTabs("\ta", 4));
        assertEquals("b  a", TextEditorWindowBody.expandTabs("b\ta", 5));
    }

    @Test
    void keepsTrailingTabs() {
        assertEquals("a   ", TextEditorWindowBody.expandTabs("a\t", 0));
        assertEquals("    ", TextEditorWindowBody.expandTabs("\t", 0));
    }

    @Test
    void lineWithoutTabsStaysTheSameInstance() {
        String line = "no tabs in here";
        assertSame(line, TextEditorWindowBody.expandTabs(line, 3));
        assertEquals("", TextEditorWindowBody.expandTabs("", 0));
    }

    private static void assertLines(String text, String... expectedLines) {
        assertArrayEquals(expectedLines, TextEditorWindowBody.splitIntoLines(text));
    }

}
