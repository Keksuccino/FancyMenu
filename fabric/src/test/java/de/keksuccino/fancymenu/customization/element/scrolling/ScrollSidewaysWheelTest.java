package de.keksuccino.fancymenu.customization.element.scrolling;

import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrollSidewaysWheelTest {

    private static final ScrollSidewaysWheel WHEEL = new ScrollSidewaysWheel();

    private static void scroll(double deltaX, double deltaY) {
        WHEEL.onScreenMouseScrollPre(new ScreenMouseScrollEvent.Pre(null, 0.0D, 0.0D, deltaX, deltaY));
    }

    private static void endScroll() {
        WHEEL.onScreenMouseScrollPost(new ScreenMouseScrollEvent.Post(null, 0.0D, 0.0D, 0.0D, 0.0D));
    }

    @Test
    void aWheelThatOnlyGotTurnedSidewaysDidNotGetTurnedUpOrDown() {
        scroll(-1.0D, 0.0D);
        assertEquals(-1.0D, ScrollSidewaysWheel.getDelta());
        // What a screen got told the wheel got turned up or down is made up from the sideways turn
        assertEquals(0.0D, ScrollSidewaysWheel.getVerticalDelta(-1.0D));
        endScroll();
    }

    @Test
    void aWheelThatGotTurnedUpOrDownKeepsWhatTheScreenGotTold() {
        scroll(0.0D, -2.0D);
        assertEquals(0.0D, ScrollSidewaysWheel.getDelta());
        assertEquals(-2.0D, ScrollSidewaysWheel.getVerticalDelta(-2.0D));
        endScroll();

        scroll(1.5D, 3.0D);
        assertEquals(1.5D, ScrollSidewaysWheel.getDelta());
        assertEquals(3.0D, ScrollSidewaysWheel.getVerticalDelta(3.0D));
        endScroll();
    }

    @Test
    void nothingIsLeftOfAScrollThatGotHandled() {
        scroll(-1.0D, 0.0D);
        endScroll();
        assertEquals(0.0D, ScrollSidewaysWheel.getDelta());
        // A scroll that reaches an element in another way than through the screen is what it got told
        assertEquals(-4.0D, ScrollSidewaysWheel.getVerticalDelta(-4.0D));
    }

}
