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
        geometry.setScroll(0, 37, 1.0);
        assertEquals(10, geometry.getContentScreenX());
        assertEquals(-17, geometry.getContentScreenY());
        assertEquals(0.0F, geometry.getSubPixelScrollY());
    }

    @Test
    void theRestOfTheScrollSnapsToPixelsOfTheWindow() {
        ScrollAreaGeometry geometry = new ScrollAreaGeometry();
        geometry.setViewport(0, 0, 200, 100, 1.0F);
        geometry.setContentSize(0, 500, false, true);

        // At a GUI scale of 2, the smallest step is half a screen pixel
        geometry.setScroll(0, 37.3, 2.0);
        assertEquals(-37, geometry.getContentScreenY());
        assertEquals(0.5F, geometry.getSubPixelScrollY());

        geometry.setScroll(0, 37.2, 2.0);
        assertEquals(-37, geometry.getContentScreenY());
        assertEquals(0.0F, geometry.getSubPixelScrollY());

        // A rest that rounds up to a whole pixel moves the content instead
        geometry.setScroll(0, 37.9, 2.0);
        assertEquals(-38, geometry.getContentScreenY());
        assertEquals(0.0F, geometry.getSubPixelScrollY());

        // At a GUI scale of 1, there is nothing smaller than a screen pixel
        geometry.setScroll(12.4, 0, 1.0);
        assertEquals(-12, geometry.getContentScreenX());
        assertEquals(0.0F, geometry.getSubPixelScrollX());
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
