package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownLayoutTest {

    @Test
    void inlineCodeSpanGetsOnePartPerLineItWrapsOver() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("some text `code span with quite a few words in it` and more text", 110);
        renderer.tick();

        int linesWithCode = 0;
        for (MarkdownTextLine line : renderer.lines) {
            MarkdownTextFragment firstCode = null;
            MarkdownTextFragment lastCode = null;
            for (MarkdownTextFragment fragment : line.fragments) {
                if (fragment.codeBlockContext == null) continue;
                if (firstCode == null) firstCode = fragment;
                lastCode = fragment;
            }
            if (firstCode == null) {
                assertTrue(line.singleLineCodeBlockStartEndPairs.isEmpty());
                continue;
            }
            linesWithCode++;
            MarkdownTextLine.SingleLineCodeBlockPart part = line.singleLineCodeBlockStartEndPairs.get(firstCode.codeBlockContext);
            assertNotNull(part);
            assertSame(firstCode, part.start);
            assertSame(lastCode, part.end);
        }
        assertTrue(linesWithCode > 1);
    }

}
