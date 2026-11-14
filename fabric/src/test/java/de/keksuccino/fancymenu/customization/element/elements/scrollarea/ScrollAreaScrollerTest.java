package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollAreaScrollerTest {

    @Test
    void scrollingStaysInsideTheRange() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setSmooth(false);
        scroller.setRange(0, 100);

        assertTrue(scroller.y().scrollBy(40));
        assertEquals(40, scroller.y().getPosition());
        assertTrue(scroller.y().scrollBy(500));
        assertEquals(100, scroller.y().getPosition());
        assertTrue(scroller.y().scrollBy(-500));
        assertEquals(0, scroller.y().getPosition());
        // The X axis has no range, so it never moves
        assertFalse(scroller.x().scrollBy(30));
        assertEquals(0, scroller.x().getPosition());
    }

    @Test
    void scrollingAgainstAnEndReportsThatNothingChanged() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setSmooth(false);
        scroller.setRange(0, 100);

        assertFalse(scroller.y().scrollBy(-10));
        scroller.y().scrollBy(100);
        assertFalse(scroller.y().scrollBy(10));
        assertTrue(scroller.y().scrollBy(-10));
    }

    @Test
    void smoothScrollingMovesTowardsTheTargetAndArrives() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setRange(0, 1000);
        scroller.y().scrollBy(300);

        assertEquals(0, scroller.y().getPosition());
        assertEquals(300, scroller.y().getTarget());
        assertTrue(scroller.y().isScrolling());
        assertFalse(scroller.x().isScrolling());

        scroller.update(0.016);
        double afterOneFrame = scroller.y().getPosition();
        assertTrue((afterOneFrame > 0) && (afterOneFrame < 300));

        scroller.update(0.016);
        assertTrue(scroller.y().getPosition() > afterOneFrame);

        for (int i = 0; i < 200; i++) scroller.update(0.016);
        assertEquals(300, scroller.y().getPosition());
        assertFalse(scroller.y().isScrolling());
    }

    @Test
    void aNewScrollDuringARunningOneMovesTheTargetOn() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setRange(0, 1000);
        scroller.y().scrollBy(100);
        scroller.update(0.016);
        scroller.y().scrollBy(100);
        assertEquals(200, scroller.y().getTarget());
        // Scrolling back while it is still on its way turns it around from the target, not from where the content is
        scroller.y().scrollBy(-250);
        assertEquals(0, scroller.y().getTarget());
    }

    @Test
    void updatesWithoutTimeDoNotMoveSmoothScrolls() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setRange(0, 1000);
        scroller.y().scrollBy(100);
        scroller.update(0);
        assertEquals(0, scroller.y().getPosition());
    }

    @Test
    void aSmallerRangePullsPositionAndTargetIn() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setRange(200, 1000);
        scroller.y().scrollTo(800, false);
        scroller.y().scrollBy(150);
        scroller.x().scrollTo(120, false);

        scroller.setRange(50, 600);
        assertEquals(600, scroller.y().getPosition());
        assertEquals(600, scroller.y().getTarget());
        assertEquals(50, scroller.x().getPosition());

        // Content that fits has nothing to scroll and is at its start
        scroller.setRange(0, 0);
        assertEquals(0, scroller.y().getPosition());
        assertEquals(0, scroller.y().getTarget());
        assertFalse(scroller.y().canScroll());
    }

    @Test
    void aBiggerRangeKeepsThePosition() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setRange(0, 100);
        scroller.y().scrollTo(70, false);
        scroller.setRange(0, 5000);
        assertEquals(70, scroller.y().getPosition());
        assertEquals(70, scroller.y().getTarget());
    }

    @Test
    void restoredPositionsWaitForTheRange() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.y().restore(340);
        assertEquals(340, scroller.y().getPosition());
        scroller.setRange(0, 1000);
        assertEquals(340, scroller.y().getPosition());

        ScrollAreaScroller shrunk = new ScrollAreaScroller();
        shrunk.y().restore(340);
        shrunk.setRange(0, 120);
        assertEquals(120, shrunk.y().getPosition());
        assertEquals(120, shrunk.y().getTarget());

        ScrollAreaScroller negative = new ScrollAreaScroller();
        negative.y().restore(-5);
        assertEquals(0, negative.y().getPosition());
    }

    @Test
    void positionsAtTheEndsCountAsStartAndEnd() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setSmooth(false);
        scroller.setRange(0, 100);

        assertTrue(scroller.y().isAtStart());
        assertFalse(scroller.y().isAtEnd());
        assertEquals(0.0, scroller.y().getProgress());

        scroller.y().scrollTo(50, false);
        assertFalse(scroller.y().isAtStart());
        assertFalse(scroller.y().isAtEnd());
        assertEquals(0.5, scroller.y().getProgress());

        scroller.y().scrollTo(100, false);
        assertTrue(scroller.y().isAtEnd());
        assertEquals(1.0, scroller.y().getProgress());

        // Content that can't be scrolled is at its start and its end at once
        assertTrue(scroller.x().isAtStart());
        assertTrue(scroller.x().isAtEnd());
        assertEquals(0.0, scroller.x().getProgress());
    }

    @Test
    void thePercentHasTwoDigitsBehindThePointAtMost() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setSmooth(false);
        scroller.setRange(0, 300);
        assertEquals(0.0, scroller.y().getPercent());
        scroller.y().scrollTo(100, false);
        assertEquals(33.33, scroller.y().getPercent());
        scroller.y().scrollTo(200, false);
        assertEquals(66.67, scroller.y().getPercent());
        scroller.y().scrollTo(300, false);
        assertEquals(100.0, scroller.y().getPercent());
        assertEquals(0.0, scroller.x().getPercent());
    }

    @Test
    void driftingMovesPositionAndTargetTogetherAndReportsWhatIsLeft() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setRange(0, 100);
        scroller.y().scrollBy(30);

        assertEquals(0.0, scroller.y().drift(10));
        assertEquals(10, scroller.y().getPosition());
        assertEquals(40, scroller.y().getTarget());

        assertEquals(15.0, scroller.y().drift(105));
        assertEquals(100, scroller.y().getPosition());
        assertEquals(100, scroller.y().getTarget());

        assertEquals(-20.0, scroller.y().drift(-120));
        assertEquals(0, scroller.y().getPosition());
    }

    @Test
    void scrollingToNotANumberLeavesTheAxisUsable() {
        ScrollAreaScroller scroller = new ScrollAreaScroller();
        scroller.setSmooth(false);
        scroller.setRange(0, 100);
        scroller.y().scrollTo(40, false);
        scroller.y().scrollTo(Double.NaN, false);
        assertEquals(40, scroller.y().getPosition());
        assertEquals(40, scroller.y().getTarget());
    }

}
