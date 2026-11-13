package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownLineWrapTest {

    private static final int CHAR_WIDTH = 6;
    /** Text without tables, code blocks and long words, which are the only things that are allowed to be wider than the optimal width. */
    private static final String TEXT_THAT_CAN_WRAP_EVERYWHERE = String.join("\n",
            "# Headline **one**, which is long enough to wrap",
            "## Headline *two*, which is long enough to wrap",
            "Plain words with **bold**, *italic*, ~~struck~~, `code`, [a link](https://example.invalid), and more words, so it wraps often.",
            "Second paragraph with `inline code that has several words in it` and **bold words that follow each other**, then the end.",
            "",
            "- First item with a **bold**, long text that wraps around to the next line",
            "  - Nested item with *italic*, ~~struck~~, and `code`, which wraps as well",
            "    - Third level item with [a link](https://example.invalid), and some more words",
            "+ Added item with a **bold**, long text that wraps around to the next line",
            "",
            "1. Numbered item with a **bold**, long text that wraps around to the next line",
            "12. Another numbered item with *italic*, and a long text that wraps",
            "",
            "> Quote with **bold**, *italic*, and `code`, that is long enough to wrap around",
            "> - Item in a quote with a **bold**, long text that wraps around",
            "",
            "> [!TIP]",
            "> Callout with **bold**, *italic*, and `code`, that is long enough to wrap around",
            "",
            "^^^",
            "Centered words with **bold**, *italic*, and `code`, long enough to wrap around",
            "> Centered quote with **bold**, *italic*, and `code`, long enough to wrap",
            "^^^",
            "",
            "|||",
            "Right-aligned words with **bold**, *italic*, and `code`, long enough to wrap",
            "> Right-aligned quote with **bold**, *italic*, and `code`, long enough to wrap",
            "|||",
            "",
            "Last paragraph, with a few more **words**."
    );

    @Test
    void lineWrapsInFrontOfAWordWhoseFormatChangesInsideOfIt() {
        // The line has room for "aaaa bbbb", but not for the comma that belongs to the bold word
        float optimalWidth = 2 + (9 * CHAR_WIDTH) + 2;
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("aaaa **bbbb**, cc", optimalWidth);
        renderer.setAutoLineBreakingEnabled(true);
        renderer.tick();
        MarkdownTextFragment bold = renderer.fragmentContaining("bbbb");
        MarkdownTextFragment comma = renderer.fragmentContaining(",");

        assertEquals("bbbb", bold.text);
        assertNotSame(renderer.fragmentContaining("aaaa").parentLine, bold.parentLine);
        assertSame(bold.parentLine, comma.parentLine);
        assertTrue(renderer.getRealWidth() <= optimalWidth, "real width " + renderer.getRealWidth());
    }

    @Test
    void wordWhoseFormatChangesInsideOfItStaysInItsLineWhileItFits() {
        // The line has exactly the room "aaaa bbbb," takes
        float optimalWidth = 2 + (10 * CHAR_WIDTH) + 2;
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("aaaa **bbbb**, cc", optimalWidth);
        renderer.setAutoLineBreakingEnabled(true);
        renderer.tick();

        assertSame(renderer.fragmentContaining("aaaa").parentLine, renderer.fragmentContaining("bbbb").parentLine);
        assertSame(renderer.fragmentContaining("aaaa").parentLine, renderer.fragmentContaining(",").parentLine);
        assertNotSame(renderer.fragmentContaining("aaaa").parentLine, renderer.fragmentContaining("cc").parentLine);
        assertEquals(optimalWidth, renderer.getRealWidth());
    }

    @Test
    void wordOfASingleFragmentNeedsRoomForTheSpaceBehindIt() {
        // The line has exactly the room "aaaa bbbb" takes, which is one space less than a line of plain words wraps at
        float optimalWidth = 2 + (9 * CHAR_WIDTH) + 2;
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("aaaa bbbb cc", optimalWidth);
        renderer.setAutoLineBreakingEnabled(true);
        renderer.tick();

        assertNotSame(renderer.fragmentContaining("aaaa").parentLine, renderer.fragmentContaining("bbbb").parentLine);

        renderer.setOptimalWidth(optimalWidth + CHAR_WIDTH);
        renderer.tick();

        assertSame(renderer.fragmentContaining("aaaa").parentLine, renderer.fragmentContaining("bbbb").parentLine);
    }

    @ParameterizedTest
    @EnumSource(MarkdownLook.class)
    void longCenteredLineOfPlainWordsWrapsAtTheOptimalWidth(MarkdownLook look) {
        String line = "This Font Software is licensed under the SIL Open Font License, Version 1.1 . This license is copied below, and is also available with a FAQ at: https://openfontlicense.org";
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("^^^\n### Noto Sans\nCopyright 2022 The Noto Project Authors\n" + line + "\n^^^\n", 480);
        renderer.setParentRenderScale(2.0F);
        renderer.getStyle().applyLook(look);
        renderer.setAutoLineBreakingEnabled(true);
        renderer.tick();
        float limit = 480 - (2 * renderer.scaling.padding());
        MarkdownTextLine first = renderer.fragmentContaining("Software").parentLine;
        MarkdownTextLine last = renderer.fragmentContaining("openfontlicense").parentLine;

        // The line is far wider than the renderer, so it becomes several lines that all stay inside of it and all are centered
        assertTrue((last.index - first.index) >= 2);
        for (int i = first.index; i <= last.index; i++) {
            MarkdownTextLine renderLine = renderer.lines.get(i);
            assertTrue(renderLine.getLineWidth() <= limit, "line " + i + " is " + renderLine.getLineWidth() + " wide");
            assertEquals(MarkdownRenderer.MarkdownLineAlignment.CENTERED, renderLine.alignment);
        }
        assertTrue(renderer.getRealWidth() <= 480);
    }

    @ParameterizedTest
    @EnumSource(MarkdownLook.class)
    void noLineOfTextThatCanWrapIsWiderThanTheOptimalWidth(MarkdownLook look) {
        for (float optimalWidth : new float[] {331.0F, 300.0F, 262.0F, 238.0F}) {
            MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT_THAT_CAN_WRAP_EVERYWHERE, optimalWidth);
            renderer.setParentRenderScale(3.0F);
            renderer.getStyle().applyLook(look);
            renderer.setAutoLineBreakingEnabled(true);
            renderer.setExtendedBulletListMarkersEnabled(true);
            renderer.setNumberedListsEnabled(true);
            renderer.setCalloutsEnabled(true);
            renderer.tick();
            float limit = optimalWidth - (2 * renderer.scaling.padding());

            assertTrue(renderer.lines.size() > 40);
            for (MarkdownTextLine line : renderer.lines) {
                assertTrue(line.getLineWidth() <= limit, () -> look + " at " + optimalWidth + ": line " + line.index + " is " + line.getLineWidth() + " wide, limit " + limit + ", starts with " + (line.fragments.isEmpty() ? "" : line.fragments.get(0).text));
            }
            assertTrue(renderer.getRealWidth() <= optimalWidth);
        }
    }

}
