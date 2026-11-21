package de.keksuccino.fancymenu.customization.element.scrolling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollGeometryTest {

    @Test
    void contentIsNeverSmallerThanTheViewport() {
        ScrollGeometry geometry = new ScrollGeometry();
        geometry.setViewport(10, 20, 200, 100);
        geometry.setContentSize(50, 30, true, true);
        assertEquals(200, geometry.getContentWidth());
        assertEquals(100, geometry.getContentHeight());
        assertEquals(0, geometry.getMaxScrollX());
        assertEquals(0, geometry.getMaxScrollY());
    }

    @Test
    void contentOnlyGrowsOnAxesThatScroll() {
        ScrollGeometry geometry = new ScrollGeometry();
        geometry.setViewport(0, 0, 200, 100);

        geometry.setContentSize(900, 700, false, true);
        assertEquals(200, geometry.getContentWidth());
        assertEquals(700, geometry.getContentHeight());
        assertEquals(0, geometry.getMaxScrollX());
        assertEquals(600, geometry.getMaxScrollY());

        geometry.setContentSize(900, 700, true, false);
        assertEquals(900, geometry.getContentWidth());
        assertEquals(100, geometry.getContentHeight());
        assertEquals(700, geometry.getMaxScrollX());
        assertEquals(0, geometry.getMaxScrollY());

        geometry.setContentSize(900, 700, true, true);
        assertEquals(700, geometry.getMaxScrollX());
        assertEquals(600, geometry.getMaxScrollY());
    }

    @Test
    void wholePixelsOfTheScrollMoveTheContent() {
        ScrollGeometry geometry = new ScrollGeometry();
        geometry.setViewport(10, 20, 200, 100);
        geometry.setContentSize(0, 500, false, true);
        geometry.setScroll(0, 37);
        assertEquals(10, geometry.getContentScreenX());
        assertEquals(-17, geometry.getContentScreenY());
    }

    @Test
    void aScrollBetweenTwoPixelsMovesTheContentToTheCloserOne() {
        ScrollGeometry geometry = new ScrollGeometry();
        geometry.setViewport(0, 0, 200, 100);
        geometry.setContentSize(500, 500, true, true);

        geometry.setScroll(0, 37.3);
        assertEquals(-37, geometry.getContentScreenY());
        geometry.setScroll(0, 37.5);
        assertEquals(-38, geometry.getContentScreenY());
        geometry.setScroll(0, 37.9);
        assertEquals(-38, geometry.getContentScreenY());
        geometry.setScroll(12.4, 0);
        assertEquals(-12, geometry.getContentScreenX());
        assertEquals(0, geometry.getContentScreenY());
    }

    @Test
    void onlyPointsInsideTheViewportAreInIt() {
        ScrollGeometry geometry = new ScrollGeometry();
        geometry.setViewport(10, 20, 200, 100);
        assertTrue(geometry.isInViewport(10, 20));
        assertTrue(geometry.isInViewport(209.5, 119.5));
        assertFalse(geometry.isInViewport(210, 60));
        assertFalse(geometry.isInViewport(60, 120));
        assertFalse(geometry.isInViewport(9.9, 60));
        assertFalse(geometry.isInViewport(60, 19.9));
    }

    @Test
    void negativeViewportSizesCountAsEmpty() {
        ScrollGeometry geometry = new ScrollGeometry();
        geometry.setViewport(0, 0, -5, -8);
        geometry.setContentSize(300, 300, true, true);
        assertEquals(0, geometry.getViewportWidth());
        assertEquals(0, geometry.getViewportHeight());
        assertFalse(geometry.isInViewport(0, 0));
        assertEquals(300, geometry.getMaxScrollX());
    }

}
