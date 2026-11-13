package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.ListItemMarker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.renderer;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.tableFragment;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The comments show the Markdown source without the escaping of the Java strings.
 */
class MarkdownParserListTest {

    @Test
    void everyBulletCharacterStartsAnItemWithItsOwnMarker() {
        List<MarkdownTextFragment> fragments = parseWithExtendedBullets("- dash\n+ plus\n* asterisk\n~ tilde");

        assertEquals(List.of("dash", "plus", "asterisk", "tilde"), texts(fragments));
        assertItem(fragments.get(0), 1, ListItemMarker.DASH);
        assertItem(fragments.get(1), 1, ListItemMarker.PLUS);
        assertItem(fragments.get(2), 1, ListItemMarker.ASTERISK);
        assertItem(fragments.get(3), 1, ListItemMarker.TILDE);
    }

    @Test
    void extendedBulletCharactersStayTextWhileTheyAreSwitchedOff() {
        List<MarkdownTextFragment> fragments = parse("- dash\n+ plus\n1 * 2\n~ tilde");

        assertItem(fragmentContaining(fragments, "dash"), 1, ListItemMarker.DASH);
        assertEquals("dash+ plus1 * 2~ tilde", joinText(fragments));
        for (MarkdownTextFragment fragment : fragments.subList(1, fragments.size())) {
            assertEquals(0, fragment.bulletListLevel);
            assertFalse(fragment.bulletListItemStart);
            assertEquals(ListItemMarker.NONE, fragment.listItemMarker);
        }
    }

    @Test
    void extendedBulletCharactersNestLikeDashes() {
        List<MarkdownTextFragment> fragments = parseWithExtendedBullets("+ one\n  * two\n    ~ three\n   - odd\n" + " ".repeat(18) + "+ ten\n" + " ".repeat(20) + "+ eleven");

        assertItem(fragmentContaining(fragments, "one"), 1, ListItemMarker.PLUS);
        assertItem(fragmentContaining(fragments, "two"), 2, ListItemMarker.ASTERISK);
        assertItem(fragmentContaining(fragments, "three"), 3, ListItemMarker.TILDE);
        assertItem(fragmentContaining(fragments, "odd"), 2, ListItemMarker.DASH);
        assertItem(fragmentContaining(fragments, "ten"), 10, ListItemMarker.PLUS);
        assertEquals(0, fragmentContaining(fragments, "eleven").bulletListLevel);
        assertEquals(ListItemMarker.NONE, fragmentContaining(fragments, "eleven").listItemMarker);
    }

    @Test
    void allFragmentsOfTheLineOfAnItemHaveItsMarker() {
        List<MarkdownTextFragment> fragments = parseWithExtendedBullets("+ one **two** three\nfour");

        for (String word : List.of("one", "two", "three")) {
            assertEquals(ListItemMarker.PLUS, fragmentContaining(fragments, word).listItemMarker);
            assertEquals(1, fragmentContaining(fragments, word).bulletListLevel);
        }
        assertTrue(fragmentContaining(fragments, "one").bulletListItemStart);
        assertFalse(fragmentContaining(fragments, "two").bulletListItemStart);
        assertEquals(ListItemMarker.NONE, fragmentContaining(fragments, "four").listItemMarker);
        assertEquals(0, fragmentContaining(fragments, "four").bulletListLevel);
    }

    @Test
    void asteriskItemWinsOverItalicOnlyWhileItIsSwitchedOn() {
        List<MarkdownTextFragment> on = parseWithExtendedBullets("* item *italic*");
        List<MarkdownTextFragment> off = parse("* item *italic*");

        assertItem(fragmentContaining(on, "item"), 1, ListItemMarker.ASTERISK);
        assertFalse(fragmentContaining(on, "item").italic);
        assertTrue(fragmentContaining(on, "italic").italic);
        assertEquals("item italic", joinText(on));
        assertEquals(0, fragmentContaining(off, "item").bulletListLevel);
        assertTrue(fragmentContaining(off, "item").italic);
        assertFalse(fragmentContaining(off, "italic").italic);
    }

    @Test
    void tildeItemWinsOverStrikethroughOnlyWhileItIsSwitchedOn() {
        List<MarkdownTextFragment> on = parseWithExtendedBullets("~ item ~struck~");
        List<MarkdownTextFragment> off = parse("~ item ~struck~");

        assertItem(fragmentContaining(on, "item"), 1, ListItemMarker.TILDE);
        assertFalse(fragmentContaining(on, "item").strikethrough);
        assertTrue(fragmentContaining(on, "struck").strikethrough);
        assertEquals("item struck", joinText(on));
        assertEquals(0, fragmentContaining(off, "item").bulletListLevel);
        assertTrue(fragmentContaining(off, "item").strikethrough);
        assertFalse(fragmentContaining(off, "struck").strikethrough);
    }

    @ParameterizedTest
    @ValueSource(strings = {"*italic*", "**bold**", "~struck~", "~~struck~~", "+text", "*", "+", "~", "text + more", "text * more", "\\+ escaped", "\\* escaped", "\\~ escaped"})
    void markerCharacterWithoutASpaceBehindItOrOutsideTheLineStartIsNoItem(String text) {
        for (MarkdownTextFragment fragment : parseWithExtendedBullets(text)) {
            assertEquals(0, fragment.bulletListLevel);
            assertFalse(fragment.bulletListItemStart);
            assertEquals(ListItemMarker.NONE, fragment.listItemMarker);
        }
    }

    @Test
    void formatsStillWorkInsideAndNextToExtendedItems() {
        List<MarkdownTextFragment> fragments = parseWithExtendedBullets("*italic* text\n* **bold** and *italic*\n~ ~~struck~~\n~struck~");

        assertTrue(fragments.get(1).italic);
        assertEquals(0, fragments.get(1).bulletListLevel);
        assertTrue(fragmentContaining(fragments, "bold").bold);
        assertEquals(ListItemMarker.ASTERISK, fragmentContaining(fragments, "bold").listItemMarker);
        MarkdownTextFragment struckItem = fragments.stream().filter(fragment -> fragment.listItemMarker == ListItemMarker.TILDE).filter(fragment -> fragment.text.equals("struck")).findFirst().orElseThrow();
        assertTrue(struckItem.strikethrough);
        MarkdownTextFragment lastStruck = fragments.get(fragments.size() - 2);
        assertEquals("struck", lastStruck.text);
        assertTrue(lastStruck.strikethrough);
        assertEquals(ListItemMarker.NONE, lastStruck.listItemMarker);
    }

    @Test
    void extendedItemsWorkInsideTableCells() {
        MarkdownRenderer renderer = renderer().setExtendedBulletListMarkersEnabled(true);
        MarkdownTextFragment table = tableFragment(parse(renderer, "| + added | ~ changed |\n| --- | --- |"));

        assertItem(fragmentContaining(table.tableContext.rows.get(0).cells.get(0).fragments, "added"), 1, ListItemMarker.PLUS);
        assertItem(fragmentContaining(table.tableContext.rows.get(0).cells.get(1).fragments, "changed"), 1, ListItemMarker.TILDE);
    }

    @Test
    void switchingExtendedBulletCharactersParsesTheTextAgain() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("+ item", 400);
        renderer.tick();

        assertFalse(renderer.isExtendedBulletListMarkersEnabled());
        assertEquals(ListItemMarker.NONE, renderer.fragmentContaining("item").listItemMarker);

        renderer.setExtendedBulletListMarkersEnabled(true);
        renderer.tick();

        assertTrue(renderer.isExtendedBulletListMarkersEnabled());
        assertEquals(2, renderer.parses);
        assertItem(renderer.fragmentContaining("item"), 1, ListItemMarker.PLUS);

        renderer.setExtendedBulletListMarkersEnabled(true);
        renderer.tick();

        assertEquals(2, renderer.parses);
    }

    @Test
    void numberWithDotOrBracketStartsANumberedItem() {
        List<MarkdownTextFragment> fragments = parseWithNumberedLists("1. first\n2) second\n10. tenth\n007) seventh\n123456789. long");

        assertEquals(List.of("first", "second", "tenth", "seventh", "long"), texts(fragments));
        assertNumberedItem(fragments.get(0), 1, "1.");
        assertNumberedItem(fragments.get(1), 1, "2)");
        assertNumberedItem(fragments.get(2), 1, "10.");
        assertNumberedItem(fragments.get(3), 1, "007)");
        assertNumberedItem(fragments.get(4), 1, "123456789.");
    }

    @Test
    void numbersStayTextWhileNumberedListsAreSwitchedOff() {
        List<MarkdownTextFragment> fragments = parse("1. first\n2) second");

        assertEquals("1. first2) second", joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) {
            assertEquals(0, fragment.bulletListLevel);
            assertFalse(fragment.bulletListItemStart);
            assertEquals(ListItemMarker.NONE, fragment.listItemMarker);
            assertNull(fragment.listItemNumber);
        }
    }

    @Test
    void numbersStayAsTheyWereTyped() {
        List<MarkdownTextFragment> fragments = parseWithNumberedLists("5. a\n5. b\n3. c\n0. d");

        assertEquals(List.of("5.", "5.", "3.", "0."), fragments.stream().map(fragment -> fragment.listItemNumber).toList());
    }

    @Test
    void numberedAndBulletItemsNestAndMixByTheirIndent() {
        List<MarkdownTextFragment> fragments = parse(renderer().setNumberedListsEnabled(true).setExtendedBulletListMarkersEnabled(true), "- bullet\n  1. nested\n    + deeper\n      2) deepest\n3. back\n" + " ".repeat(18) + "4. ten\n" + " ".repeat(20) + "5. eleven");

        assertItem(fragmentContaining(fragments, "bullet"), 1, ListItemMarker.DASH);
        assertNull(fragmentContaining(fragments, "bullet").listItemNumber);
        assertNumberedItem(fragmentContaining(fragments, "nested"), 2, "1.");
        assertItem(fragmentContaining(fragments, "deeper"), 3, ListItemMarker.PLUS);
        assertNumberedItem(fragmentContaining(fragments, "deepest"), 4, "2)");
        assertNumberedItem(fragmentContaining(fragments, "back"), 1, "3.");
        assertNumberedItem(fragmentContaining(fragments, "ten"), 10, "4.");
        assertEquals(0, fragmentContaining(fragments, "eleven").bulletListLevel);
        assertNull(fragmentContaining(fragments, "eleven").listItemNumber);
    }

    @Test
    void allFragmentsOfTheLineOfANumberedItemHaveItsNumber() {
        List<MarkdownTextFragment> fragments = parseWithNumberedLists("12. one **two** three\nfour");

        for (String word : List.of("one", "two", "three")) {
            assertEquals(ListItemMarker.NUMBER, fragmentContaining(fragments, word).listItemMarker);
            assertEquals("12.", fragmentContaining(fragments, word).listItemNumber);
            assertEquals(1, fragmentContaining(fragments, word).bulletListLevel);
        }
        assertTrue(fragmentContaining(fragments, "one").bulletListItemStart);
        assertFalse(fragmentContaining(fragments, "three").bulletListItemStart);
        assertEquals(ListItemMarker.NONE, fragmentContaining(fragments, "four").listItemMarker);
        assertNull(fragmentContaining(fragments, "four").listItemNumber);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.text", "1 . text", "1.", "1)", "1", "a1. text", "text 1. more", "1234567890. text", "1: text", "1] text", "\u0661. text", "1\\. text", "\\1. text"})
    void numberWithoutDotOrBracketAndSpaceOrOutsideTheLineStartIsNoItem(String text) {
        for (MarkdownTextFragment fragment : parseWithNumberedLists(text)) {
            assertEquals(0, fragment.bulletListLevel);
            assertFalse(fragment.bulletListItemStart);
            assertEquals(ListItemMarker.NONE, fragment.listItemMarker);
            assertNull(fragment.listItemNumber);
        }
    }

    @Test
    void numberedItemsWorkInsideTableCells() {
        MarkdownTextFragment table = tableFragment(parse(renderer().setNumberedListsEnabled(true), "| 1. first | 2) second |\n| --- | --- |"));

        assertNumberedItem(fragmentContaining(table.tableContext.rows.get(0).cells.get(0).fragments, "first"), 1, "1.");
        assertNumberedItem(fragmentContaining(table.tableContext.rows.get(0).cells.get(1).fragments, "second"), 1, "2)");
    }

    @Test
    void switchingNumberedListsParsesTheTextAgain() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("1. item", 400);
        renderer.tick();

        assertFalse(renderer.isNumberedListsEnabled());
        assertEquals(ListItemMarker.NONE, renderer.fragmentContaining("item").listItemMarker);

        renderer.setNumberedListsEnabled(true);
        renderer.tick();

        assertTrue(renderer.isNumberedListsEnabled());
        assertEquals(2, renderer.parses);
        assertNumberedItem(renderer.fragmentContaining("item"), 1, "1.");

        renderer.setNumberedListsEnabled(true);
        renderer.tick();

        assertEquals(2, renderer.parses);
    }

    private static List<MarkdownTextFragment> parseWithNumberedLists(String text) {
        return parse(renderer().setNumberedListsEnabled(true), text);
    }

    private static void assertNumberedItem(MarkdownTextFragment itemStart, int level, String number) {
        assertItem(itemStart, level, ListItemMarker.NUMBER);
        assertEquals(number, itemStart.listItemNumber);
    }

    private static List<MarkdownTextFragment> parseWithExtendedBullets(String text) {
        return parse(renderer().setExtendedBulletListMarkersEnabled(true), text);
    }

    private static void assertItem(MarkdownTextFragment itemStart, int level, ListItemMarker marker) {
        assertEquals(level, itemStart.bulletListLevel);
        assertTrue(itemStart.bulletListItemStart);
        assertEquals(marker, itemStart.listItemMarker);
    }

}
