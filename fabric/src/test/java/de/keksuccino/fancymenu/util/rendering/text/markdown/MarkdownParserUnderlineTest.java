package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorMarkdownFormatting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.renderer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownParserUnderlineTest {

    @Test
    void plusPairsUnderlineTheTextBetweenThem() {
        List<MarkdownTextFragment> fragments = parse("keep ++two words++ keep");

        assertTrue(fragmentContaining(fragments, "two").underlined);
        assertTrue(fragmentContaining(fragments, "words").underlined);
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.text.contains("keep")) assertFalse(fragment.underlined);
        }
        assertEquals("keep two words keep", joinText(fragments));
    }

    @Test
    void underlineWorksAtTheStartAndTheEndOfTheText() {
        List<MarkdownTextFragment> fragments = parse("++first++ middle ++last++");

        assertTrue(fragmentContaining(fragments, "first").underlined);
        assertFalse(fragmentContaining(fragments, "middle").underlined);
        assertTrue(fragmentContaining(fragments, "last").underlined);
        assertEquals("first middle last", joinText(fragments));
    }

    @Test
    void underlineCanStartAndEndInsideOfAWord() {
        List<MarkdownTextFragment> fragments = parse("un++der++line");

        assertTrue(fragmentContaining(fragments, "der").underlined);
        assertFalse(fragmentContaining(fragments, "un").underlined);
        assertFalse(fragmentContaining(fragments, "line").underlined);
        assertEquals("underline", joinText(fragments));
    }

    @Test
    void underlineWorksInTextWithoutSpaces() {
        List<MarkdownTextFragment> fragments = parse("这是++重要++的句子");

        assertTrue(fragmentContaining(fragments, "重要").underlined);
        assertFalse(fragmentContaining(fragments, "这是").underlined);
        assertEquals("这是重要的句子", joinText(fragments));
    }

    @Test
    void underlineSpansLines() {
        List<MarkdownTextFragment> fragments = parse("++first\nsecond++ third");

        assertTrue(fragmentContaining(fragments, "first").underlined);
        assertTrue(fragmentContaining(fragments, "second").underlined);
        assertFalse(fragmentContaining(fragments, "third").underlined);
    }

    @Test
    void underlineWorksTogetherWithOtherFormats() {
        List<MarkdownTextFragment> fragments = parse("**++both++** ++*italic*++ ~~++struck++~~");

        MarkdownTextFragment both = fragmentContaining(fragments, "both");
        assertTrue(both.bold);
        assertTrue(both.underlined);
        MarkdownTextFragment italic = fragmentContaining(fragments, "italic");
        assertTrue(italic.italic);
        assertTrue(italic.underlined);
        MarkdownTextFragment struck = fragmentContaining(fragments, "struck");
        assertTrue(struck.strikethrough);
        assertTrue(struck.underlined);
        assertEquals("both italic struck", joinText(fragments));
    }

    @Test
    void formattingCodesNextToTheMarkersDoNotKeepThemFromWorking() {
        List<MarkdownTextFragment> fragments = parse("&l++word++&r rest");

        assertTrue(fragmentContaining(fragments, "word").underlined);
        assertFalse(fragmentContaining(fragments, "rest").underlined);
    }

    @ParameterizedTest
    @ValueSource(strings = {"C++ and C++", "C++, Java and C++.", "Notepad++ is better than Notepad++", "i++; j++;", "++i and ++j", "a ++ b ++ c", "x++ ++y"})
    void plusSignsOfNamesAndCodeStayText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        for (MarkdownTextFragment fragment : fragments) assertFalse(fragment.underlined, fragment.text);
        assertEquals(text, joinText(fragments));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+single+", "+++triple+++", "++++quadruple++++", "++never closed", "never opened++"})
    void otherRowsOfPlusSignsAndUnclosedPairsAreText(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        for (MarkdownTextFragment fragment : fragments) assertFalse(fragment.underlined, fragment.text);
        assertEquals(text, joinText(fragments));
    }

    @Test
    void escapedPlusSignsAreText() {
        List<MarkdownTextFragment> fragments = parse("\\+\\+word\\+\\+");

        assertFalse(fragmentContaining(fragments, "word").underlined);
        assertEquals("++word++", joinText(fragments));
    }

    @Test
    void plusSignsInsideOfCodeAreText() {
        List<MarkdownTextFragment> fragments = parse("`++code++` ++text++");

        MarkdownTextFragment code = fragmentContaining(fragments, "++code++");
        assertNotNull(code.codeBlockContext);
        assertFalse(code.underlined);
        assertTrue(fragmentContaining(fragments, "text").underlined);
    }

    @Test
    void listItemThatStartsWithAPlusSignCanHoldUnderlinedText() {
        MarkdownRenderer renderer = renderer();
        renderer.setExtendedBulletListMarkersEnabled(true);
        List<MarkdownTextFragment> fragments = parse(renderer, "+ ++item++ text");

        MarkdownTextFragment item = fragmentContaining(fragments, "item");
        assertEquals(MarkdownTextFragment.ListItemMarker.PLUS, item.listItemMarker);
        assertTrue(item.underlined);
        assertFalse(fragmentContaining(fragments, "text").underlined);
    }

    @ParameterizedTest
    @ValueSource(strings = {"C++ is [cool]", "un[der]line", "[two words]", "这是[重要]的", "keep [this]!", "a [(note)] b"})
    void textTheUnderlineShortcutFormatsGetsUnderlined(String template) {
        // The brackets of the template are the selection of the shortcut
        int start = template.indexOf('[');
        int end = template.indexOf(']') - 1;
        String selected = template.substring(start + 1, end + 1);
        TextEditorMarkdownFormatting.Edit edit = TextEditorMarkdownFormatting.apply(TextEditorMarkdownFormatting.Shortcut.UNDERLINE, template.replace("[", "").replace("]", ""), start, end);
        assertNotNull(edit);

        List<MarkdownTextFragment> fragments = parse(edit.text());

        StringBuilder underlined = new StringBuilder();
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.underlined) underlined.append(fragment.text);
        }
        assertEquals(selected, underlined.toString().trim());
        assertEquals(template.replace("[", "").replace("]", ""), joinText(fragments));
    }

}
