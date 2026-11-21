package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollAreaDragScrollTest {

    /** An area that shows its content from 60 to 220 on the axis. */
    private static double speed(int mouse) {
        return ScrollAreaEditorElement.getDragScrollSpeed(mouse, 60, 160);
    }

    @Test
    void theMiddleOfTheAreaScrollsNothing() {
        assertEquals(0.0D, speed(140));
        assertEquals(0.0D, speed(76));
        assertEquals(0.0D, speed(203));
    }

    @Test
    void theEdgesScrollTowardsThemFasterTheCloserTheMouseIs() {
        assertTrue(speed(75) < 0.0D);
        assertTrue(speed(60) < speed(70));
        assertTrue(speed(204) > 0.0D);
        assertTrue(speed(219) > speed(210));
        // Both edges are as fast at the same distance
        assertEquals(-speed(60), speed(219));
        assertEquals(-speed(68), speed(211));
    }

    @Test
    void aMouseOutsideOfTheAreaScrollsNothing() {
        // It drags the elements out of the area
        assertEquals(0.0D, speed(59));
        assertEquals(0.0D, speed(220));
        assertEquals(0.0D, speed(-500));
    }

    @Test
    void theZonesOfASmallAreaLeaveItsMiddleFree() {
        // A quarter of 20 is 5, so only the outer 5 pixels at each edge scroll
        assertTrue(ScrollAreaEditorElement.getDragScrollSpeed(0, 0, 20) < 0.0D);
        assertEquals(0.0D, ScrollAreaEditorElement.getDragScrollSpeed(5, 0, 20));
        assertEquals(0.0D, ScrollAreaEditorElement.getDragScrollSpeed(14, 0, 20));
        assertTrue(ScrollAreaEditorElement.getDragScrollSpeed(15, 0, 20) > 0.0D);
        // An area too small for zones scrolls nothing
        assertEquals(0.0D, ScrollAreaEditorElement.getDragScrollSpeed(1, 0, 3));
    }

}
