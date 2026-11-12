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
        assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled());
        String document = repeatedText.text.repeat((DOCUMENT_LENGTH / repeatedText.text.length()) + 1).substring(0, DOCUMENT_LENGTH);
        MarkdownRenderer renderer = new MarkdownRenderer(TEST_FONT);
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
        FONT_PREFIXES_WITHOUT_SUFFIX("%!!uniform%a "),
        COLOR_PREFIXES_WITHOUT_SUFFIX("%#ff0000%a "),
        OPEN_BRACKETS("[a "),
        HYPERLINK_TARGETS_WITHOUT_END("[a](b "),
        IMAGES_WITHOUT_END_IN_ONE_HYPERLINK_IMAGE_LINE("[![a](b "),
        INLINE_CODE_MARKERS_IN_SEPARATE_LINES("`a\n"),
        PLAIN_TEXT_MARKERS(";;a ;b "),
        BLOCK_MARKER_LINES("```\na\n;;;\nb\n^^^\nc\n|||\nd\n"),
        HEADLINES("# a\n\n## b\n   \n"),
        QUOTES("> a\nb\n   \n"),
        BULLET_LIST_ITEMS("- a\n  - b\n" + " ".repeat(40) + "- c\n"),
        SEPARATION_LINES("---\n--- - -\n"),
        TABLES("| a | b |\n| :-- | --: |\n| c | d |\n\n"),
        TABLE_CELLS_IN_ONE_ROW("| a "),
        TABLE_SEPARATOR_CELLS_IN_ONE_ROW("| --- "),
        TABLE_PLACEHOLDER_PREFIXES("[TABLE_\n[TABLE_1\n"),
        FORMATTING_CODES("§c&l§ra ");

        private final String text;

        RepeatedText(String text) {
            this.text = text;
        }

    }

}
