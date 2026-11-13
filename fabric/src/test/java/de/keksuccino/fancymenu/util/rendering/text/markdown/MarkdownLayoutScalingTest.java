package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Guards the layout against work that grows faster than its text. Text of a script without spaces is one fragment for a whole paragraph,
 * which the layout splits into a piece for every line, so copying or measuring the rest of that text for every line would be such work.
 *
 * <p>The guard counts allocated bytes instead of measuring time, so it does not depend on the speed of the machine.</p>
 */
class MarkdownLayoutScalingTest {

    private static final int TEXT_LENGTH = 40000;
    /**
     * The layout allocates less than 10 MB for each of the texts, most of it for the lines and the pieces.
     * Copying the rest of a text of this length for each of its 4000 lines takes 160 MB or more.
     */
    private static final long MAX_ALLOCATED_BYTES = 64L * 1024L * 1024L;

    @ParameterizedTest
    @EnumSource(LongText.class)
    void allocationStaysProportionalToTheTextLength(LongText longText) {
        assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled());
        String repeated = longText.repeatedText.repeat((TEXT_LENGTH / longText.repeatedText.length()) + 1).substring(0, TEXT_LENGTH);
        // Ten characters fit into a line
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(longText.start + repeated + longText.end, 64);
        // The first layout loads all classes, which keeps their one-time allocations out of the measured one
        renderer.tick();
        // The test font keeps every text it measures, which is no work of the layout
        renderer.testFont().measuredFragmentTexts.clear();
        renderer.setOptimalWidth(70);

        long allocatedBefore = threads.getCurrentThreadAllocatedBytes();
        renderer.tick();
        long allocated = threads.getCurrentThreadAllocatedBytes() - allocatedBefore;

        assertTrue(renderer.lines.size() >= longText.minLines, () -> "The text only has " + renderer.lines.size() + " lines");
        assertTrue(allocated < MAX_ALLOCATED_BYTES, () -> "Allocated " + allocated + " bytes for " + TEXT_LENGTH + " characters");
    }

    /**
     * Texts without a space, made of a text that gets repeated up to the text length.
     */
    private enum LongText {

        IDEOGRAPHS("", "一二三四五六七八九十", "", 3000),
        IDEOGRAPHS_AND_WORDS("", "日本語のMinecraftは「楽しい」です。", "", 3000),
        ONE_LINK("[", "一二三四五六七八九十", "](https://example.invalid)", 3000),
        ONE_QUOTE("> ", "一二三四五六七八九十", "", 3000),
        BOLD_AND_PLAIN_CHARACTERS("", "**字**文", "", 1000),
        // A line can only break in front of the last character
        WORD_WITH_AN_IDEOGRAPH_BEHIND_IT("", "a", "字", 2),
        IDEOGRAPH_WITH_A_WORD_BEHIND_IT("字", "a", "", 2),
        // No line can break inside of these
        CHARACTERS_THAT_MUST_NOT_START_A_LINE("", "。", "", 1),
        CHARACTERS_THAT_MUST_NOT_END_A_LINE("", "「", "", 1);

        private final String start;
        private final String repeatedText;
        private final String end;
        private final int minLines;

        LongText(String start, String repeatedText, String end, int minLines) {
            this.start = start;
            this.repeatedText = repeatedText;
            this.end = end;
            this.minLines = minLines;
        }

    }

}
