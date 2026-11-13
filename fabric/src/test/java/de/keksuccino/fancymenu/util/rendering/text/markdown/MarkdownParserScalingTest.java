package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.management.ManagementFactory;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.TEST_FONT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Guards the parser against work that grows faster than its text, like copying the rest of the text for every character
 * or building a fragment again for every character that gets added to it.
 *
 * <p>The guard counts allocated bytes instead of measuring time, so it does not depend on the speed of the machine.</p>
 */
class MarkdownParserScalingTest {

    private static final int DOCUMENT_LENGTH = 40000;
    /**
     * The parser allocates less than 10 MB for each of the documents, most of it for the fragments themselves.
     * Copying the rest of the text for every character of a document of this length takes 800 MB or more.
     */
    private static final long MAX_ALLOCATED_BYTES = 64L * 1024L * 1024L;

    @ParameterizedTest
    @EnumSource(RepeatedText.class)
    void allocationStaysProportionalToTheTextLength(RepeatedText repeatedText) {
        assertAllocationStaysProportional(new MarkdownRenderer(TEST_FONT), repeatedText);
    }

    @ParameterizedTest
    @EnumSource(RepeatedText.class)
    void allocationStaysProportionalToTheTextLengthWithEverySyntaxSwitchedOn(RepeatedText repeatedText) {
        assertAllocationStaysProportional(new MarkdownRenderer(TEST_FONT).setExtendedBulletListMarkersEnabled(true).setNumberedListsEnabled(true).setCalloutsEnabled(true), repeatedText);
    }

    private static void assertAllocationStaysProportional(MarkdownRenderer renderer, RepeatedText repeatedText) {
        assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled());
        String document = repeatedText.text.repeat((DOCUMENT_LENGTH / repeatedText.text.length()) + 1).substring(0, DOCUMENT_LENGTH);
        // The first run loads all classes, which keeps their one-time allocations out of the measured run
        MarkdownParser.parse(renderer, document, true);

        long allocatedBefore = threads.getCurrentThreadAllocatedBytes();
        MarkdownParser.parse(renderer, document, true);
        long allocated = threads.getCurrentThreadAllocatedBytes() - allocatedBefore;

        assertTrue(allocated < MAX_ALLOCATED_BYTES, () -> "Allocated " + allocated + " bytes for " + DOCUMENT_LENGTH + " characters");
    }

    /**
     * Texts that get repeated up to the document length. Most of them hold markers that never get closed, which makes
     * every one of them look ahead for a closing marker.
     */
    private enum RepeatedText {

        MIXED_DOCUMENT("# Headline\n\nSome **bold**, *italic*, _italic_ and ~struck~ words with `code` and a [link](https://example.invalid/page?a=1&b=2).\n\n- first item\n  - nested item\n\n> quote line\nsecond quote line\n\n```\ncode line\n```\n\n| Key | Value |\n| :-- | --: |\n| a | **b** |\n\n---\n\n^^^\n%#ff8800%centered%#% %!!uniform%text%!!%\n^^^\n\n;;plain **text**;; \\*escaped\\* [click](click:event) §6gold§r &agreen&r\n![image](image.png)\n[![image](image.png)](https://example.invalid)\n"),
        WORDS("word "),
        SPACES(" "),
        SPACES_BEHIND_A_WORD("a" + " ".repeat(199)),
        BACKSLASHES("\\"),
        EMPHASIS_MARKERS("*a _b ~c **d ~~e "),
        ESCAPED_MARKERS("*a \\*b _c \\_d ~e \\~f **g \\**h "),
        SINGLE_ASTERISKS_BETWEEN_BOLD_TEXT("a * b **c** "),
        SINGLE_ASTERISKS_INSIDE_BOLD_TEXT("**a * b** "),
        SINGLE_ASTERISKS_IN_FRONT_OF_BOLD_TEXT("* ".repeat(50) + "**a** ".repeat(50)),
        DOUBLE_TILDES_WITHOUT_END("~~a "),
        GRAVE_ACCENT_PAIRS("``a "),
        FONT_PREFIXES_WITHOUT_SUFFIX("%!!uniform%a "),
        COLOR_PREFIXES_WITHOUT_SUFFIX("%#ff0000%a "),
        OPEN_BRACKETS("[a "),
        HYPERLINK_TARGETS_WITHOUT_END("[a](b "),
        HYPERLINK_TARGETS_WITH_OPEN_ROUND_BRACKETS("[a](b(c "),
        MARKERS_IN_HYPERLINK_TARGETS("a_b ~c *d [e](f_g~h*i) "),
        MARKERS_IN_IMAGE_LINES("a_b ~c *d\n![e_f](g_h.png)\n[![i_j](k_l.png)](m_n)\n"),
        IMAGES_WITHOUT_END_IN_ONE_HYPERLINK_IMAGE_LINE("[![a](b "),
        INLINE_CODE_MARKERS_IN_SEPARATE_LINES("`a\n"),
        PLAIN_TEXT_MARKERS(";;a ;b "),
        BLOCK_MARKER_LINES("```\na\n;;;\nb\n^^^\nc\n|||\nd\n"),
        HEADLINES("# a\n\n## b\n   \n"),
        QUOTES("> a\n> b\n>\nc\n   \n"),
        BULLET_LIST_ITEMS("- a\n  - b\n" + " ".repeat(40) + "- c\n"),
        LIST_ITEMS_OF_EVERY_MARKER("+ a\n  * b\n    ~ c\n1. d\n  22) e\n" + " ".repeat(40) + "* f\n1234567890. g\n"),
        LIST_MARKERS_BEHIND_SINGLE_MARKERS("a * b ~ c\n* d\n~ e\n"),
        CALLOUTS("> [!NOTE]\n> a\n\n> [!tip]  \n\n> [!UNKNOWN]\nb\n\n"),
        SEPARATION_LINES("---\n--- - -\n"),
        TABLES("| a | b |\n| :-- | --: |\n| c | d |\n\n"),
        TABLE_SYNTAX_IN_CODE_BLOCKS("```\n| a | b |\n| --- | --- |\n\n| ;; | ** |\n| --- | --- |\n```\n"),
        TABLE_SYNTAX_IN_ONE_PLAIN_TEXT_BLOCK("| a | ;; |\n| --- | --- |\n\n;;;\n"),
        TABLE_CELLS_IN_ONE_ROW("| a "),
        TABLE_SEPARATOR_CELLS_IN_ONE_ROW("| --- "),
        FORMATTING_CODES("§c&l§ra "),
        MARKERS_IN_INLINE_CODE("*a _b ~c **d ~~e `*f _g ~h **i ~~j %!!% %#%` "),
        MARKERS_IN_PLAIN_TEXT("*a _b ~c **d ;;*e _f ~g **h;; "),
        MARKERS_IN_CODE_BLOCKS("*a _b ~c **d\n```\n*e _f ~g **h\n```\n"),
        MARKERS_IN_TABLE_SYNTAX_INSIDE_ONE_CODE_BLOCK("| *a | _b |\n| --- | --- |\n| ~c | **d |\n\n*e\n"),
        PLAIN_TEXT_MARKERS_IN_INLINE_CODE("`a ;;b` ;;c "),
        LINK_LABELS_THAT_END_IN_INLINE_CODE("[a `b](c) d` "),
        LINK_LABELS_THAT_END_IN_INLINE_CODE_OF_ONE_LABEL("[a `b](c)` `d](e)` "),
        LINK_LABELS_THAT_END_IN_PLAIN_TEXT("[a ;;b](c) d;; "),
        UNDERSCORES_IN_ROWS("_a __b__ c___ "),
        TRIPLE_ASTERISKS("***a** b ***c* d "),
        FONT_PREFIXES_WITH_UNDERSCORES_WITHOUT_SUFFIX("_a %!!b_c%d "),
        FONT_PREFIXES_WITHOUT_END("%!!a_b "),
        FONT_AND_COLOR_PREFIXES_THAT_END_IN_A_SUFFIX("%!!a%!!% %#f00%#% "),
        ALIGNMENT_MARKERS_WITHOUT_END("^^^\na\n|||\nb\n```\n^^^\n|||\n```\n"),
        ALIGNMENT_BLOCKS("^^^\na\n^^^\n|||\nb\n|||\n;;;\n^^^\n|||\n;;;\n"),
        SPACES_BEHIND_OPENING_MARKERS("* a* ` b` [ c](d) #  e\n-  f\n>  g\n");

        private final String text;

        RepeatedText(String text) {
            this.text = text;
        }

    }

}
