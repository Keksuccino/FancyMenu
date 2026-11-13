package de.keksuccino.fancymenu.util.rendering.text.markdown;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.renderer;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.tableFragment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Code shows the way it got typed, so an ampersand with the character of a formatting behind it is text inside of code and of a code block.
 * Everywhere else it is a formatting code, and so is a code with a section sign inside of code.
 */
class MarkdownParserAmpersandCodeInCodeTest {

    private static final TextColor RED = TextColor.fromLegacyFormat(ChatFormatting.RED);

    @ParameterizedTest
    @ValueSource(chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r'})
    void ampersandCodeInsideOfCodeStaysText(char code) {
        List<MarkdownTextFragment> fragments = parse("before `&" + code + "inside code` after");

        assertEquals("before &" + code + "inside code after", joinText(fragments));
        assertNotNull(fragmentContaining(fragments, "&" + code + "inside").codeBlockContext);
        for (MarkdownTextFragment fragment : fragments) assertSame(MinecraftFormattingState.EMPTY, fragment.minecraftFormatting, () -> "<" + fragment.text + ">");
    }

    @Test
    void ampersandCodeInsideOfACodeBlockStaysText() {
        List<MarkdownTextFragment> fragments = parse("before\n```\n&cfirst line\nsecond &l line &r\n```\nafter");

        assertEquals("before&cfirst linesecond &l line &rafter", joinText(fragments));
        assertFalse(fragmentContaining(fragments, "&cfirst").codeBlockContext.singleLine);
        assertFalse(fragmentContaining(fragments, "&l").codeBlockContext.singleLine);
        for (MarkdownTextFragment fragment : fragments) assertSame(MinecraftFormattingState.EMPTY, fragment.minecraftFormatting, () -> "<" + fragment.text + ">");
    }

    @Test
    void ampersandCodeBehindCodeIsAFormattingCodeAgain() {
        List<MarkdownTextFragment> fragments = parse("`&c` plain &cred\n```\n&l\n```\n&lbold");

        assertEquals("&c plain red&lbold", joinText(fragments));
        assertNull(styleOf(fragmentContaining(fragments, "plain")).getColor());
        assertEquals(RED, styleOf(fragmentContaining(fragments, "red")).getColor());
        assertFalse(styleOf(fragmentContaining(fragments, "&l")).isBold());
        assertTrue(styleOf(fragmentContaining(fragments, "bold")).isBold());
    }

    @Test
    void ampersandCodeInFrontOfCodeStillFormatsTheCode() {
        List<MarkdownTextFragment> fragments = parse("&cred `code` red\n```\nblock\n```");

        assertEquals("red code redblock", joinText(fragments));
        assertEquals(RED, styleOf(fragmentContaining(fragments, "code")).getColor());
        assertEquals(RED, styleOf(fragmentContaining(fragments, "block")).getColor());
    }

    @Test
    void sectionSignCodeInsideOfCodeIsStillAFormattingCode() {
        List<MarkdownTextFragment> fragments = parse("`§cred` and\n```\n§lbold\n```");

        assertEquals("red andbold", joinText(fragments));
        assertEquals(RED, styleOf(fragmentContaining(fragments, "red")).getColor());
        assertTrue(styleOf(fragmentContaining(fragments, "bold")).isBold());
    }

    @Test
    void ampersandCodeInsideOfPlainTextIsStillAFormattingCode() {
        List<MarkdownTextFragment> fragments = parse(";;&cred;; and\n;;;\n&lbold\n;;;");

        assertEquals("red andbold", joinText(fragments));
        assertTrue(fragmentContaining(fragments, "red").plainText);
        assertEquals(RED, styleOf(fragmentContaining(fragments, "red")).getColor());
        assertTrue(styleOf(fragmentContaining(fragments, "bold")).isBold());
    }

    @Test
    void markerLineBehindAnAmpersandCodeIsTextOfTheCodeBlock() {
        List<MarkdownTextFragment> fragments = parse("```\ncode\n&c```\nmore\n```\nafter");

        assertEquals("code&c```moreafter", joinText(fragments));
        assertNotNull(fragmentContaining(fragments, "&c```").codeBlockContext);
        assertNotNull(fragmentContaining(fragments, "more").codeBlockContext);
        assertNull(fragmentContaining(fragments, "after").codeBlockContext);
        for (MarkdownTextFragment fragment : fragments) assertSame(MinecraftFormattingState.EMPTY, fragment.minecraftFormatting, () -> "<" + fragment.text + ">");
    }

    @Test
    void markerLineBehindAnAmpersandCodeClosesNoCodeBlock() {
        // Nothing closes the block, so its marker line is text and the ampersand code a formatting code
        List<MarkdownTextFragment> fragments = parse("```\ncode\n&c```\nafter");

        assertEquals("```code```after", joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.codeBlockContext != null));
        assertEquals(RED, styleOf(fragmentContaining(fragments, "after")).getColor());
    }

    @Test
    void markerLineBehindASectionSignCodeStillClosesTheCodeBlock() {
        List<MarkdownTextFragment> fragments = parse("```\ncode\n§c```\nafter");

        assertEquals("codeafter", joinText(fragments));
        assertNotNull(fragmentContaining(fragments, "code").codeBlockContext);
        assertNull(fragmentContaining(fragments, "after").codeBlockContext);
    }

    @Test
    void ampersandCodeInsideOfCodeInATableCellAndInALinkStaysText() {
        MarkdownTextFragment table = tableFragment(parse("| `&c` | b |\n| --- | --- |"));
        List<MarkdownTextFragment> cell = table.tableContext.rows.get(0).cells.get(0).fragments;
        List<MarkdownTextFragment> link = parse("[label `&c` label](target) after");

        assertEquals("&c", joinText(cell).trim());
        assertNotNull(fragmentContaining(cell, "&c").codeBlockContext);
        assertEquals("label &c label after", joinText(link));
        assertNotNull(fragmentContaining(link, "&c").hyperlink);
        assertNull(styleOf(fragmentContaining(link, "after")).getColor());
    }

    @Test
    void textThatIsNoMarkdownHasNoCodeForAnAmpersandCodeToBeTextIn() {
        List<MarkdownTextFragment> fragments = MarkdownParser.parse(renderer(), "`&cred`", false);

        assertEquals("`red`", joinText(fragments));
        assertEquals(RED, styleOf(fragmentContaining(fragments, "red")).getColor());
    }

    private static Style styleOf(MarkdownTextFragment fragment) {
        return fragment.buildRenderComponent(false).getStyle();
    }

}
