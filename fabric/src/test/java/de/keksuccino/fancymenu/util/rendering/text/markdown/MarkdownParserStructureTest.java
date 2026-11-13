package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.CodeBlockContext;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.HeadlineType;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.TableCell;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.TableRow;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.TEST_FONT;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.tableFragment;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the parts of the parser that look ahead in the text or work on index ranges of it.
 */
class MarkdownParserStructureTest {

    @ParameterizedTest
    @ValueSource(strings = {"**", "*", "_", "~"})
    void emphasisOpensOnlyWithALaterClosingMarker(String marker) {
        List<MarkdownTextFragment> fragments = parse(marker + "closed" + marker + " " + marker + "open");
        MarkdownTextFragment closed = fragmentContaining(fragments, "closed");
        MarkdownTextFragment open = fragmentContaining(fragments, "open");

        assertTrue(isEmphasized(closed));
        assertEquals("closed ", closed.text);
        assertFalse(isEmphasized(open));
        assertEquals(marker + "open", open.text);
    }

    @Test
    void italicAsterisksNestInsideBoldAsterisks() {
        List<MarkdownTextFragment> fragments = parse("**outer *both* outer**");
        MarkdownTextFragment both = fragmentContaining(fragments, "both");

        assertTrue(both.bold);
        assertTrue(both.italic);
        for (MarkdownTextFragment fragment : fragments) {
            if (!fragment.text.contains("outer")) continue;
            assertTrue(fragment.bold);
            assertFalse(fragment.italic);
        }
        assertEquals("outer both outer", joinText(fragments));
    }

    @Test
    void inlineCodeNeedsItsClosingMarkerInTheSameLine() {
        List<MarkdownTextFragment> fragments = parse("`code` and `open\nnext` line");
        MarkdownTextFragment code = fragmentContaining(fragments, "code");

        assertNotNull(code.codeBlockContext);
        assertTrue(code.codeBlockContext.singleLine);
        assertEquals("code ", code.text);
        assertNull(fragmentContaining(fragments, "`open").codeBlockContext);
        assertNull(fragmentContaining(fragments, "next`").codeBlockContext);
    }

    @Test
    void multiLineCodeBlockKeepsItsLinesUnparsedInOneContext() {
        List<MarkdownTextFragment> fragments = parse("```\n**stays** `x`\nsecond\n```\nafter **bold**");
        CodeBlockContext context = fragmentContaining(fragments, "**stays**").codeBlockContext;

        assertNotNull(context);
        assertFalse(context.singleLine);
        assertEquals(List.of("**stays** ", "`x`", "second"), texts(context.codeBlockFragments));
        for (MarkdownTextFragment fragment : context.codeBlockFragments) {
            assertSame(context, fragment.codeBlockContext);
            assertFalse(fragment.bold);
        }
        MarkdownTextFragment after = fragmentContaining(fragments, "bold");
        assertNull(after.codeBlockContext);
        assertTrue(after.bold);
        assertNull(fragmentContaining(fragments, "after").codeBlockContext);
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", ";;;", "^^^", "|||"})
    void blockMarkerWithoutAClosingLineOpensNoBlock(String marker) {
        MarkdownTextFragment text = fragmentContaining(parse(marker + "\ntext"), "text");

        assertNull(text.codeBlockContext);
        assertFalse(text.plainText);
        assertEquals(MarkdownRenderer.MarkdownLineAlignment.LEFT, text.alignment);
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", ";;;", "^^^", "|||"})
    void blockMarkerClosesAtALaterLineThatStartsWithIt(String marker) {
        List<MarkdownTextFragment> fragments = parse("before\n" + marker + "\ninside\n" + marker + "\nafter");
        MarkdownTextFragment inside = fragmentContaining(fragments, "inside");
        MarkdownTextFragment after = fragmentContaining(fragments, "after");

        assertEquals(List.of("before", "inside", "after"), texts(fragments));
        assertTrue((inside.codeBlockContext != null) || inside.plainText || (inside.alignment != MarkdownRenderer.MarkdownLineAlignment.LEFT));
        assertNull(after.codeBlockContext);
        assertFalse(after.plainText);
        assertEquals(MarkdownRenderer.MarkdownLineAlignment.LEFT, after.alignment);
    }

    @Test
    void plainTextKeepsMarkersAndBackslashesUntouched() {
        List<MarkdownTextFragment> fragments = parse(";;**a** \\*;; **b**");
        MarkdownTextFragment markers = fragmentContaining(fragments, "**a**");
        MarkdownTextFragment backslash = fragmentContaining(fragments, "\\*");
        MarkdownTextFragment bold = fragmentContaining(fragments, "b");

        assertTrue(markers.plainText);
        assertFalse(markers.bold);
        assertTrue(backslash.plainText);
        assertFalse(bold.plainText);
        assertTrue(bold.bold);
        assertEquals("b", bold.text);
    }

    @Test
    void plainTextNeedsALaterClosingMarker() {
        List<MarkdownTextFragment> fragments = parse(";;open **bold**");

        assertFalse(fragmentContaining(fragments, ";;open").plainText);
        assertTrue(fragmentContaining(fragments, "bold").bold);
    }

    @Test
    void hyperlinkFragmentsShareOneHyperlinkAndHideItsTarget() {
        List<MarkdownTextFragment> fragments = parse("see [two words](https://example.invalid/a?b=1) now");
        MarkdownTextFragment first = fragmentContaining(fragments, "two");
        MarkdownTextFragment second = fragmentContaining(fragments, "words");

        assertNotNull(first.hyperlink);
        assertSame(first.hyperlink, second.hyperlink);
        assertEquals("https://example.invalid/a?b=1", first.hyperlink.link);
        assertEquals(List.of(first, second), first.hyperlink.hyperlinkFragments);
        assertNull(fragmentContaining(fragments, "see").hyperlink);
        assertNull(fragmentContaining(fragments, "now").hyperlink);
        assertEquals("see two words now", joinText(fragments));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[label](target", "[label]target)", "[label] (target)", "[la]bel](target)", "[label](tar(get)", "[label](tar]get)", "[label\n](target)", "[label](tar\nget)", "[la[bel", "[", "[]", "]("})
    void invalidHyperlinksStayText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        for (MarkdownTextFragment fragment : fragments) {
            assertNull(fragment.hyperlink);
            assertNull(fragment.clickEvent);
            assertNull(fragment.hoverEvent);
        }
        assertEquals(text.replace("\n", ""), joinText(fragments));
    }

    @Test
    void manyUnclosedBracketsBeforeAHyperlinkDoNotBreakIt() {
        List<MarkdownTextFragment> fragments = parse("[a [b [c] [d](target) [e");
        MarkdownTextFragment link = fragmentContaining(fragments, "d");

        assertNotNull(link.hyperlink);
        assertEquals("target", link.hyperlink.link);
        assertEquals("[a [b [c] d [e", joinText(fragments));
    }

    @Test
    void clickAndHoverTargetsCreateTextEventsInsteadOfHyperlinks() {
        List<MarkdownTextFragment> fragments = parse("[first](click:open_settings) [second](HOVER: tip ) [third](click:)");
        MarkdownTextFragment click = fragmentContaining(fragments, "first");
        MarkdownTextFragment hover = fragmentContaining(fragments, "second");
        MarkdownTextFragment emptyId = fragmentContaining(fragments, "third");

        assertEquals("open_settings", click.clickEvent.identifier);
        assertEquals(List.of(click), click.clickEvent.eventFragments);
        assertNull(click.hyperlink);
        assertEquals("tip", hover.hoverEvent.identifier);
        assertEquals(List.of(hover), hover.hoverEvent.eventFragments);
        assertNull(hover.hyperlink);
        assertNull(emptyId.clickEvent);
        assertEquals("click:", emptyId.hyperlink.link);
        assertEquals("first second third", joinText(fragments));
    }

    @Test
    void imageLineBecomesOneImageFragment() {
        List<MarkdownTextFragment> fragments = parse("![alt](image.png)\ntext");

        assertEquals(2, fragments.size());
        assertEquals("image.png", fragments.get(0).imageSupplier.getSourceWithoutPrefix());
        assertNull(fragments.get(0).hyperlink);
        assertTrue(fragments.get(0).naturalLineBreakAfter);
        assertNull(fragments.get(1).imageSupplier);
    }

    @Test
    void hyperlinkImageLineBecomesOneLinkedImageFragment() {
        List<MarkdownTextFragment> fragments = parse("[![alt](image.png)](https://example.invalid)\ntext");
        MarkdownTextFragment image = fragments.get(0);

        assertEquals(2, fragments.size());
        assertEquals("image.png", image.imageSupplier.getSourceWithoutPrefix());
        assertEquals("https://example.invalid", image.hyperlink.link);
        assertEquals(List.of(image), image.hyperlink.hyperlinkFragments);
        assertNull(fragments.get(1).imageSupplier);
        assertNull(fragments.get(1).hyperlink);
    }

    @ParameterizedTest
    @ValueSource(strings = {"![alt](image.png", "![al[t](image.png)", "![alt](ima(ge.png)", "[![alt](image.png)]", "[![alt](image.png)](target"})
    void invalidImageLinesCreateNoImageFragment(String text) {
        for (MarkdownTextFragment fragment : parse(text)) {
            assertNull(fragment.imageSupplier);
        }
    }

    @Test
    void headlineSwallowsOneEmptyLineBelowIt() {
        List<MarkdownTextFragment> fragments = parse("# Title\n\ntext\n## Second\ndirect\n### Third\n\n\nlast");

        assertEquals(List.of("Title", "text", "Second", "direct", "Third", "", "last"), texts(fragments));
        assertEquals(HeadlineType.BIGGEST, fragments.get(0).headlineType);
        assertEquals(HeadlineType.NONE, fragments.get(1).headlineType);
        assertEquals(HeadlineType.BIGGER, fragments.get(2).headlineType);
        assertEquals(HeadlineType.NONE, fragments.get(3).headlineType);
        assertEquals(HeadlineType.BIG, fragments.get(4).headlineType);
        assertEquals(HeadlineType.NONE, fragments.get(5).headlineType);
    }

    @Test
    void fontAppliesBetweenItsPrefixAndSuffix() {
        List<MarkdownTextFragment> fragments = parse("a %!!uniform%b c%!!% d");

        assertEquals(ResourceLocation.parse("uniform"), fragmentContaining(fragments, "b").font);
        assertEquals(ResourceLocation.parse("uniform"), fragmentContaining(fragments, "c").font);
        assertNull(fragmentContaining(fragments, "a").font);
        assertNull(fragmentContaining(fragments, "d").font);
        assertEquals("a b c d", joinText(fragments));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%!!uniform%open", "%!!Bad Name%x%!!%", "%!!uniform", "%!!%x%!!%"})
    void fontWithoutSuffixOrValidNameStaysText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        for (MarkdownTextFragment fragment : fragments) assertNull(fragment.font);
        assertEquals(text, joinText(fragments));
    }

    @Test
    void colorAppliesBetweenItsPrefixAndSuffix() {
        List<MarkdownTextFragment> fragments = parse("a %#ff0000%b c%#% d");

        assertEquals(0xFF0000, fragmentContaining(fragments, "b").textColor.getColorInt() & 0xFFFFFF);
        assertEquals(0xFF0000, fragmentContaining(fragments, "c").textColor.getColorInt() & 0xFFFFFF);
        assertNull(fragmentContaining(fragments, "a").textColor);
        assertNull(fragmentContaining(fragments, "d").textColor);
        assertEquals("a b c d", joinText(fragments));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%#ff0000%open", "%#nocolor%x%#%", "%#ff0000", "%#%x%#%"})
    void colorWithoutSuffixOrValidValueStaysText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        for (MarkdownTextFragment fragment : fragments) assertNull(fragment.textColor);
        assertEquals(text, joinText(fragments));
    }

    @Test
    void bulletListLevelFollowsTheIndent() {
        List<MarkdownTextFragment> fragments = parse("- one\n  - two\n    - three\n - odd\n" + " ".repeat(18) + "- ten\n" + " ".repeat(20) + "- eleven");

        assertBullet(fragmentContaining(fragments, "one"), 1);
        assertBullet(fragmentContaining(fragments, "two"), 2);
        assertBullet(fragmentContaining(fragments, "three"), 3);
        assertBullet(fragmentContaining(fragments, "odd"), 1);
        assertBullet(fragmentContaining(fragments, "ten"), 10);
        assertEquals(0, fragmentContaining(fragments, "eleven").bulletListLevel);
        assertEquals("onetwothreeoddten" + " ".repeat(20) + "- eleven", joinText(fragments));
    }

    @ParameterizedTest
    @ValueSource(strings = {"---", "----------", "--- - --", "---   "})
    void separationLineAcceptsDashesAndSpaces(String line) {
        List<MarkdownTextFragment> fragments = parse("above\n" + line + "\nbelow");

        assertEquals(3, fragments.size());
        assertFalse(fragments.get(0).separationLine);
        assertTrue(fragments.get(1).separationLine);
        assertFalse(fragments.get(2).separationLine);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--", "---x", "--- | ---", "x---"})
    void otherLinesAreNoSeparationLines(String line) {
        for (MarkdownTextFragment fragment : parse("above\n" + line + "\nbelow")) {
            assertFalse(fragment.separationLine);
        }
    }

    @Test
    void quoteSpansItsLinesUntilAWhitespaceOnlyLine() {
        List<MarkdownTextFragment> fragments = parse("> one\ntwo\n \t \nthree");
        MarkdownTextFragment one = fragmentContaining(fragments, "one");
        MarkdownTextFragment two = fragmentContaining(fragments, "two");

        assertNotNull(one.quoteContext);
        assertSame(one.quoteContext, two.quoteContext);
        assertEquals(List.of(one, two), one.quoteContext.quoteFragments);
        assertNull(fragmentContaining(fragments, "three").quoteContext);
    }

    @Test
    void escapedMarkersStayTextWithoutTheirBackslash() {
        List<MarkdownTextFragment> fragments = parse("\\*a\\* \\_b\\_ \\~c\\~ \\`d\\` \\[e\\](f) \\# g");

        for (MarkdownTextFragment fragment : fragments) {
            assertFalse(isEmphasized(fragment));
            assertNull(fragment.codeBlockContext);
            assertNull(fragment.hyperlink);
        }
        assertEquals("*a* _b_ ~c~ `d` [e](f) # g", joinText(fragments));
    }

    @Test
    void rowsOfSpacesStayInTheFragmentOfTheWordBeforeThem() {
        List<MarkdownTextFragment> fragments = parse("a    **b**   c");

        assertEquals(List.of("a    ", "b   ", "c"), texts(fragments));
        assertTrue(fragments.get(0).endOfWord);
        assertTrue(fragments.get(1).endOfWord);
        assertTrue(fragments.get(1).bold);
        assertEquals(5.0F, fragments.get(0).unscaledTextWidth);
        assertEquals(4.0F, fragments.get(1).unscaledTextWidth);
    }

    @Test
    void tableBecomesOneFragmentWithParsedCells() {
        List<MarkdownTextFragment> fragments = parse("before\n| Left | Center | Right |\n| :-- | :-: | --: |\n| **b** | a \\| b | [l](x) |\nafter");
        MarkdownTextFragment table = tableFragment(fragments);
        List<TableRow> rows = table.tableContext.rows;

        assertEquals(List.of("before", "[TABLE]", "after"), texts(fragments));
        assertEquals(2, rows.size());
        assertTrue(rows.get(0).isHeader);
        assertFalse(rows.get(1).isHeader);
        assertEquals(TableCell.TableCellAlignment.LEFT, rows.get(1).cells.get(0).alignment);
        assertEquals(TableCell.TableCellAlignment.CENTER, rows.get(1).cells.get(1).alignment);
        assertEquals(TableCell.TableCellAlignment.RIGHT, rows.get(1).cells.get(2).alignment);
        assertEquals(List.of("Left", "Center", "Right"), List.of(joinText(rows.get(0).cells.get(0).fragments), joinText(rows.get(0).cells.get(1).fragments), joinText(rows.get(0).cells.get(2).fragments)));
        MarkdownTextFragment bold = fragmentContaining(rows.get(1).cells.get(0).fragments, "b");
        assertTrue(bold.bold);
        assertSame(table.tableContext, bold.tableContext);
        assertEquals("a | b", joinText(rows.get(1).cells.get(1).fragments));
        assertEquals("x", fragmentContaining(rows.get(1).cells.get(2).fragments, "l").hyperlink.link);
    }

    @Test
    void tableRowWithThousandsOfCellsGetsParsed() {
        int columns = 20000;
        MarkdownTextFragment table = tableFragment(parse("| a ".repeat(columns) + "|\n" + "| :-: ".repeat(columns) + "|"));
        List<TableCell> cells = table.tableContext.rows.get(0).cells;

        assertEquals(columns, cells.size());
        assertEquals(TableCell.TableCellAlignment.CENTER, cells.get(columns - 1).alignment);
    }

    @Test
    void lineBreaksAtTheEndCreateNoFragments() {
        assertEquals(List.of("text"), texts(parse("text\n\n\n")));
        assertEquals(List.of("te|xt"), texts(parse("te|xt\n\n\n")));
    }

    @Test
    void disabledMarkdownKeepsAllMarkersAsText() {
        String text = "# **a** `b` [c](d) ~e~\n- f\n> g";
        List<MarkdownTextFragment> fragments = MarkdownParser.parse(new MarkdownRenderer(TEST_FONT), text, false);

        for (MarkdownTextFragment fragment : fragments) {
            assertFalse(isEmphasized(fragment));
            assertEquals(HeadlineType.NONE, fragment.headlineType);
            assertNull(fragment.codeBlockContext);
            assertNull(fragment.hyperlink);
            assertNull(fragment.quoteContext);
            assertEquals(0, fragment.bulletListLevel);
        }
        assertEquals(text.replace("\n", ""), joinText(fragments));
    }

    private static boolean isEmphasized(MarkdownTextFragment fragment) {
        return fragment.bold || fragment.italic || fragment.strikethrough;
    }

    private static void assertBullet(MarkdownTextFragment fragment, int level) {
        assertEquals(level, fragment.bulletListLevel);
        assertTrue(fragment.bulletListItemStart);
    }

}
