package de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollBarWheelTest {

    /** A vertical bar the mouse wheel scrolls, scrolled to its middle. The mouse is over it at 2, 50. */
    private static ScrollBar bar() {
        ScrollBar bar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, 4.0F, 20.0F, 0.0F, 0.0F, 4.0F, 100.0F, () -> null, () -> null);
        bar.setScrollWheelAllowed(true);
        bar.setScroll(0.5F);
        return bar;
    }

    @Test
    void aWheelThatDidNotTurnUpOrDownScrollsNothingAndIsLeftToOthers() {
        ScrollBar bar = bar();
        assertFalse(bar.mouseScrolled(2.0D, 50.0D, 0.0D));
        assertEquals(0.5F, bar.getScroll());
    }

    @Test
    void aWheelThatTurnedDownScrollsDown() {
        ScrollBar bar = bar();
        assertTrue(bar.mouseScrolled(2.0D, 50.0D, -1.0D));
        assertTrue(bar.getScroll() > 0.5F);
    }

    @Test
    void aWheelThatTurnedUpScrollsUp() {
        ScrollBar bar = bar();
        assertTrue(bar.mouseScrolled(2.0D, 50.0D, 1.0D));
        assertTrue(bar.getScroll() < 0.5F);
    }

}
