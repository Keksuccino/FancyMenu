package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatter;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.fragmentContaining;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.tableFragment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A custom text color formatter can use any character for its formatting code, also one that Markdown uses for its markers.
 * The character behind the section sign belongs to the formatting code then and is no part of a marker.
 */
@ResourceLock("TextColorFormatterRegistry global state")
class MarkdownParserFormattingCodeCharacterTest {

    private static final String FORMATTER_ID_PREFIX = "markdown_parser_formatting_code_character_test_";
    /** Every character a formatter of this test uses as its code. */
    private static final String CODES = "`;[]|*_~\\";

    @BeforeAll
    static void registerFormatters() {
        for (int i = 0; i < CODES.length(); i++) {
            TextColorFormatterRegistry.register(FORMATTER_ID_PREFIX + i, new TextColorFormatter(CODES.charAt(i), DrawableColor.of(new Color(10 + i, 20, 30))));
        }
    }

    /**
     * The registry keeps formatters forever and has no way to remove one. Codes that are marker characters would change how the
     * texts of every test behind this one get parsed, so they get taken out of the registry again.
     */
    @AfterAll
    @SuppressWarnings("unchecked")
    static void removeFormatters() throws ReflectiveOperationException {
        Field formattersField = TextColorFormatterRegistry.class.getDeclaredField("FORMATTERS");
        formattersField.setAccessible(true);
        Map<String, TextColorFormatter> formatters = (Map<String, TextColorFormatter>) formattersField.get(null);
        formatters.keySet().removeIf(identifier -> identifier.startsWith(FORMATTER_ID_PREFIX));
        Field snapshotField = TextColorFormatterRegistry.class.getDeclaredField("formatterSnapshot");
        snapshotField.setAccessible(true);
        snapshotField.set(null, formatters.values().toArray(new TextColorFormatter[0]));
    }

    @ParameterizedTest
    @ValueSource(strings = {"before `open §`text end", "before ;;open §;;text end", "before *open §*text end", "before _open §_text end", "before ~open §~text end"})
    void characterOfAFormattingCodeIsNoClosingMarker(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        // Only the formatting code is gone
        assertEquals(text.replaceAll("§.", ""), joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) {
            assertNull(fragment.codeBlockContext);
            assertFalse(fragment.plainText || fragment.bold || fragment.italic || fragment.strikethrough);
        }
        assertSame(MinecraftFormattingState.EMPTY, fragmentContaining(fragments, "open").minecraftFormatting);
        assertTrue(fragmentContaining(fragments, "text").minecraftFormatting != MinecraftFormattingState.EMPTY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"`inside §`more` end", ";;inside §;more;; end"})
    void realMarkerBehindAFormattingCodeWithTheSameCharacterStillCloses(String text) {
        List<MarkdownTextFragment> fragments = parse(text);
        MarkdownTextFragment more = fragmentContaining(fragments, "more");
        MarkdownTextFragment end = fragmentContaining(fragments, "end");

        assertEquals("inside more end", joinText(fragments));
        assertTrue(more.plainText || (more.codeBlockContext != null));
        assertTrue(more.minecraftFormatting != MinecraftFormattingState.EMPTY);
        assertFalse(end.plainText || (end.codeBlockContext != null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[one §]two](target) rest", "[one §[two](target) rest", "[one §\\](target) rest"})
    void bracketOfAFormattingCodeDoesNotEndTheLabelOfALink(String text) {
        List<MarkdownTextFragment> fragments = parse(text);
        MarkdownTextFragment one = fragmentContaining(fragments, "one");

        assertNotNull(one.hyperlink);
        assertEquals("target", one.hyperlink.link);
        assertNull(fragmentContaining(fragments, "rest").hyperlink);
        assertTrue(joinText(fragments).endsWith(" rest"));
        assertFalse(joinText(fragments).contains("target"));
    }

    @Test
    void bracketOfAFormattingCodeWithATargetBehindItOpensNoLinkThatNeverCloses() {
        List<MarkdownTextFragment> fragments = parse("[label§](target) rest");

        assertEquals("[label(target) rest", joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) assertNull(fragment.hyperlink);
    }

    @Test
    void pipeOfAFormattingCodeSeparatesNoTableCells() {
        MarkdownTextFragment table = tableFragment(parse("left §|side | right §|\n--- | ---"));
        List<MarkdownTextFragment.TableCell> cells = table.tableContext.rows.get(0).cells;

        assertEquals(2, cells.size());
        assertEquals("left side", joinText(cells.get(0).fragments));
        assertEquals("right ", joinText(cells.get(1).fragments));
    }

    @Test
    void graveAccentOfAFormattingCodeInTheInfoStringOfACodeBlockKeepsTheBlock() {
        List<MarkdownTextFragment> fragments = parse("```java §`x\ncode\n```\nafter");

        assertEquals("codeafter", joinText(fragments));
        assertNotNull(fragmentContaining(fragments, "code").codeBlockContext);
        assertNull(fragmentContaining(fragments, "after").codeBlockContext);
    }

}
