package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollAreaGeometryTest {

    @Test
    void contentIsNeverSmallerThanTheViewport() {
        ScrollAreaGeometry geometry = new ScrollAreaGeometry();
        geometry.setViewport(10, 20, 200, 100, 1.0F);
        geometry.setContentSize(50, 30, true, true);
        assertEquals(200, geometry.getContentWidth());
        assertEquals(100, geometry.getContentHeight());
        assertEquals(0, geometry.getMaxScrollX());
        assertEquals(0, geometry.getMaxScrollY());
    }

    @Test
    void contentOnlyGrowsOnAxesThatScroll() {
        ScrollAreaGeometry geometry = new ScrollAreaGeometry();
        geometry.setViewport(0, 0, 200, 100, 1.0F);

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
    void aScaledContentIsMeasuredInItsOwnUnits() {
        ScrollAreaGeometry geometry = new ScrollAreaGeometry();
        geometry.setViewport(0, 0, 400, 200, 2.0F);
        // The viewport shows 200x100 units of the content
        geometry.setContentSize(0, 250, false, true);
        assertEquals(200, geometry.getContentWidth());
        assertEquals(250, geometry.getContentHeight());
        // 250 units are 500 pixels on the screen, 300 more than the viewport shows
        assertEquals(300, geometry.getMaxScrollY());

        geometry.setViewport(0, 0, 100, 50, 0.5F);
        geometry.setContentSize(0, 250, false, true);
        assertEquals(200, geometry.getContentWidth());
        assertEquals(75, geometry.getMaxScrollY());
    }

    @Test
    void anInvalidScaleCountsAsNoScale() {
        ScrollAreaGeometry geometry = new ScrollAreaGeometry();
        geometry.setViewport(0, 0, 100, 50, 0.0F);
        assertEquals(1.0F, geometry.getScale());
        geometry.setViewport(0, 0, 100, 50, -3.0F);
        assertEquals(1.0F, geometry.getScale());
    }

    @Test
    void wholePixelsOfTheScrollMoveTheContent() {
        ScrollAreaGeometry geometry = new ScrollAreaGeometry();
        geometry.setViewport(10, 20, 200, 100, 1.0F);
        geometry.setContentSize(0, 500, false, true);
        geometry.setScroll(0, 37);
        assertEquals(10, geometry.getContentScreenX());
        assertEquals(-17, geometry.getContentScreenY());
    }

    @Test
    void aScrollBetweenTwoPixelsMovesTheContentToTheCloserOne() {
        ScrollAreaGeometry geometry = new ScrollAreaGeometry();
        geometry.setViewport(0, 0, 200, 100, 1.0F);
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
        ScrollAreaGeometry geometry = new ScrollAreaGeometry();
        geometry.setViewport(10, 20, 200, 100, 1.0F);
        assertTrue(geometry.isInViewport(10, 20));
        assertTrue(geometry.isInViewport(209.5, 119.5));
        assertFalse(geometry.isInViewport(210, 60));
        assertFalse(geometry.isInViewport(60, 120));
        assertFalse(geometry.isInViewport(9.9, 60));
        assertFalse(geometry.isInViewport(60, 19.9));
    }

    @Test
    void negativeViewportSizesCountAsEmpty() {
        ScrollAreaGeometry geometry = new ScrollAreaGeometry();
        geometry.setViewport(0, 0, -5, -8, 1.0F);
        geometry.setContentSize(300, 300, true, true);
        assertEquals(0, geometry.getViewportWidth());
        assertEquals(0, geometry.getViewportHeight());
        assertFalse(geometry.isInViewport(0, 0));
        assertEquals(300, geometry.getMaxScrollX());
    }

}
