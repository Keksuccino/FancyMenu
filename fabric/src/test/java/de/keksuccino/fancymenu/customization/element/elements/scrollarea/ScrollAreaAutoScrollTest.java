package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollAreaAutoScrollTest {

    private static ScrollAreaScroller scroller(int max) {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setRange(0, max);
        return scroller;
    }

    /** Lets the given time pass in small frames. */
    private static void run(ScrollAreaAutoScroll autoScroll, ScrollAreaScroller scroller, double seconds, double speed, boolean loop) {
        for (double passed = 0; passed < seconds; passed += 0.05) {
            autoScroll.update(scroller.y(), 0.05, speed, loop);
        }
    }

    @Test
    void theContentRestsAtItsStartBeforeItMoves() {
        ScrollAreaScroller scroller = scroller(1000);
        ScrollAreaAutoScroll autoScroll = new ScrollAreaAutoScroll();
        autoScroll.update(scroller.y(), 0.1, 100, false);
        assertEquals(0, scroller.y().getPosition());
        run(autoScroll, scroller, 3, 100, false);
        assertTrue(scroller.y().getPosition() > 100);
    }

    @Test
    void theContentMovesAtASteadySpeed() {
        ScrollAreaScroller scroller = scroller(100000);
        ScrollAreaAutoScroll autoScroll = new ScrollAreaAutoScroll();
        run(autoScroll, scroller, 2, 100, false);
        double start = scroller.y().getPosition();
        run(autoScroll, scroller, 2, 100, false);
        assertEquals(200, scroller.y().getPosition() - start, 6);
        // The target moves along, so nothing pulls the content back
        assertEquals(scroller.y().getPosition(), scroller.y().getTarget());
    }

    @Test
    void theContentStaysAtItsEndWithoutALoop() {
        ScrollAreaScroller scroller = scroller(100);
        ScrollAreaAutoScroll autoScroll = new ScrollAreaAutoScroll();
        run(autoScroll, scroller, 30, 100, false);
        assertEquals(100, scroller.y().getPosition());
    }

    @Test
    void theContentStartsOverWithALoop() {
        ScrollAreaScroller scroller = scroller(100);
        ScrollAreaAutoScroll autoScroll = new ScrollAreaAutoScroll();
        boolean wasAtEnd = false;
        boolean startedOver = false;
        for (int i = 0; i < 400; i++) {
            autoScroll.update(scroller.y(), 0.05, 100, true);
            if (scroller.y().getPosition() == 100) wasAtEnd = true;
            if (wasAtEnd && (scroller.y().getPosition() < 50)) startedOver = true;
        }
        assertTrue(wasAtEnd);
        assertTrue(startedOver);
    }

    @Test
    void theContentRestsAfterTheUserScrolled() {
        ScrollAreaScroller scroller = scroller(100000);
        ScrollAreaAutoScroll autoScroll = new ScrollAreaAutoScroll();
        run(autoScroll, scroller, 2, 100, false);
        scroller.y().scrollTo(5000, false);
        autoScroll.onUserScrolled();
        run(autoScroll, scroller, 1, 100, false);
        assertEquals(5000, scroller.y().getPosition());
        run(autoScroll, scroller, 3, 100, false);
        // It goes on from where the user left the content
        assertTrue(scroller.y().getPosition() > 5100);
    }

    @Test
    void nothingMovesWithoutSpeedTimeOrRange() {
        ScrollAreaScroller scroller = scroller(1000);
        ScrollAreaAutoScroll autoScroll = new ScrollAreaAutoScroll();
        run(autoScroll, scroller, 5, 0, true);
        assertEquals(0, scroller.y().getPosition());
        for (int i = 0; i < 100; i++) autoScroll.update(scroller.y(), 0, 100, true);
        assertEquals(0, scroller.y().getPosition());
        ScrollAreaScroller fitting = scroller(0);
        run(new ScrollAreaAutoScroll(), fitting, 5, 100, true);
        assertEquals(0, fitting.y().getPosition());
    }

}
