package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownSeparatorStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownSeparatorStyleTest {

    @Test
    void classicSeparatorIsALineThatIsAsThickAsOnePixelOfTheText() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("before\n---\nafter", 400);
        renderer.tick();
        MarkdownTextFragment separator = renderer.fragmentContaining("---");

        assertEquals(MarkdownBlockType.SEPARATOR, separator.parentLine.blockType);
        assertEquals(1, separator.parentLine.getLineHeight());

        renderer.setTextBaseScale(3.0F);
        renderer.tick();

        assertEquals(3, renderer.fragmentContaining("---").parentLine.getLineHeight());
    }

    @Test
    void ornamentSeparatorIsAsTallAsItsOrnament() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("before\n---\nafter", 400);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.tick();

        assertEquals(5, renderer.fragmentContaining("---").parentLine.getLineHeight());

        renderer.getStyle().separatorThickness.set(7.0F);
        renderer.tick();
        assertEquals(7, renderer.fragmentContaining("---").parentLine.getLineHeight());

        renderer.getStyle().separatorStyle.set(MarkdownSeparatorStyle.LINE);
        renderer.getStyle().separatorThickness.set(2.0F);
        renderer.tick();
        assertEquals(2, renderer.fragmentContaining("---").parentLine.getLineHeight());
    }

    @Test
    void fadeOfTheLeftLineMirrorsTheFadeOfTheRightLine() {
        for (MarkdownPixelArt.Pattern pattern : MarkdownPixelArt.Pattern.values()) {
            for (int start = 0; start < 9; start++) {
                for (int length = 1; length < 12; length++) {
                    int end = start + length;
                    // The right line counts its pixels away from the ornament, and so does the left line, which runs the other way on screen
                    int rightPhase = start % pattern.period;
                    int leftPhase = MarkdownSeparatorPainter.mirroredPhase(end, pattern);
                    for (int i = 0; i < length; i++) {
                        boolean right = ((rightPhase + i) % pattern.period) == 0;
                        boolean left = ((leftPhase + (length - 1 - i)) % pattern.period) == 0;
                        assertEquals(right, left, pattern + " from " + start + " to " + end + " at " + i);
                    }
                }
            }
        }
    }

}
