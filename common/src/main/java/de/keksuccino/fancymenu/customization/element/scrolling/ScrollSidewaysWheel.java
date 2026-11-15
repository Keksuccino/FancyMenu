package de.keksuccino.fancymenu.customization.element.scrolling;

import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;

/**
 * Knows how far the wheel really got turned in the scroll a screen is handling right now.<br>
 * Screens of this version of the game only get told how far a wheel got turned up or down. FancyMenu reads both directions where
 * the scroll comes in and posts them right before the screen gets its scroll, which is where this picks them up
 * for every scrollable element that takes that scroll.
 */
public final class ScrollSidewaysWheel {

    private static double delta = 0.0D;
    /** If the wheel got turned sideways and neither up nor down in the scroll that is getting handled right now. */
    private static boolean sidewaysOnly = false;

    /**
     * Returns how far the wheel got turned sideways in the scroll that is getting handled right now, or 0 if there is none.
     */
    public static double getDelta() {
        return delta;
    }

    /**
     * Returns how far the wheel got turned up or down in the scroll that is getting handled right now.<br>
     * On macOS, Forge tells screens that a wheel that only got turned sideways got turned up or down that far, so the lists of the game scroll with it.
     * A scrollable element scrolls sideways with such a turn, and would scroll up or down on top of that if it believed the screen.
     *
     * @param handedDelta How far the screen got told the wheel got turned up or down.
     */
    public static double getVerticalDelta(double handedDelta) {
        return sidewaysOnly ? 0.0D : handedDelta;
    }

    @EventListener
    public void onScreenMouseScrollPre(ScreenMouseScrollEvent.Pre event) {
        delta = event.getScrollDeltaX();
        sidewaysOnly = (delta != 0.0D) && (event.getScrollDeltaY() == 0.0D);
    }

    @EventListener
    public void onScreenMouseScrollPost(ScreenMouseScrollEvent.Post event) {
        delta = 0.0D;
        sidewaysOnly = false;
    }

}
