package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrollAreaSnapPointsTest {

    private static ScrollAreaSnapPoints points(int max, double... positions) {
        ScrollAreaSnapPoints points = new ScrollAreaSnapPoints();
        for (double position : positions) points.add(position);
        points.finish(max);
        return points;
    }

    /** How many points there are: every one of them is the next one of the one before, from in front of the first. */
    private static int count(ScrollAreaSnapPoints points) {
        int count = 0;
        double position = -10;
        for (double next = points.next(position, true); next != position; next = points.next(position, true)) {
            position = next;
            count++;
        }
        return count;
    }

    @Test
    void theStartAndTheEndOfTheRangeAreAlwaysPoints() {
        ScrollAreaSnapPoints points = points(300);
        assertEquals(2, count(points));
        assertEquals(300, points.next(0, true));
        assertEquals(0, points.next(300, false));
    }

    @Test
    void scrollingGoesToTheNextPointInItsDirection() {
        ScrollAreaSnapPoints points = points(300, 200, 50, 120);
        assertEquals(50, points.next(0, true));
        assertEquals(120, points.next(50, true));
        assertEquals(200, points.next(130, true));
        assertEquals(300, points.next(200, true));
        assertEquals(120, points.next(130, false));
        assertEquals(50, points.next(120, false));
        assertEquals(0, points.next(50, false));
    }

    @Test
    void thereIsNoNextPointBeyondTheEnds() {
        ScrollAreaSnapPoints points = points(300, 100);
        assertEquals(300, points.next(300, true));
        assertEquals(0, points.next(0, false));
    }

    @Test
    void pointsOutsideOfTheRangeArePulledIntoIt() {
        ScrollAreaSnapPoints points = points(300, -40, 9000, 150);
        assertEquals(3, count(points));
        assertEquals(150, points.next(0, true));
        assertEquals(300, points.next(150, true));
    }

    @Test
    void pointsInTheSamePlaceCountOnce() {
        ScrollAreaSnapPoints points = points(300, 100, 100.2, 100, 299.8);
        assertEquals(3, count(points));
        assertEquals(100, points.next(0, true));
        // A point right in front of the end is the end
        assertEquals(300, points.next(100, true));
    }

    @Test
    void theNearestPointIsWhereTheContentComesToRest() {
        ScrollAreaSnapPoints points = points(300, 100, 200);
        assertEquals(100, points.nearest(70));
        assertEquals(100, points.nearest(149));
        assertEquals(200, points.nearest(151));
        assertEquals(0, points.nearest(20));
        assertEquals(300, points.nearest(290));
    }

    @Test
    void pagesAreAsBigAsWhatTheAreaShows() {
        ScrollAreaSnapPoints points = new ScrollAreaSnapPoints();
        points.addPages(100, 250);
        points.finish(250);
        assertEquals(4, count(points));
        assertEquals(100, points.next(0, true));
        assertEquals(200, points.next(100, true));
        // The last page is as long as what is left
        assertEquals(250, points.next(200, true));

        ScrollAreaSnapPoints none = new ScrollAreaSnapPoints();
        none.addPages(0, 250);
        none.finish(250);
        assertEquals(2, count(none));
    }

    @Test
    void manyPointsFitAndClearingStartsOver() {
        ScrollAreaSnapPoints points = new ScrollAreaSnapPoints();
        for (int i = 0; i < 100; i++) points.add(i * 10);
        points.finish(2000);
        assertEquals(101, count(points));
        points.clear();
        points.finish(50);
        assertEquals(2, count(points));
    }

    @Test
    void aRangeWithNothingToScrollHasOnePoint() {
        ScrollAreaSnapPoints points = points(0, 40, 80);
        assertEquals(1, count(points));
        assertEquals(0, points.nearest(30));
        assertEquals(0, points.next(0, true));
    }

}
