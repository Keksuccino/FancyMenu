package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;

/**
 * Knows how far the wheel got turned sideways in the scroll a screen is handling right now.<br>
 * Screens of this version of the game only get told how far a wheel got turned up or down. FancyMenu reads both directions where
 * the scroll comes in and posts them right before the screen gets its scroll, which is where this picks the sideways one up
 * for everything of a scrollable area that takes that scroll.
 */
public final class ScrollAreaSidewaysWheel {

    private static double delta = 0.0D;

    /**
     * Returns how far the wheel got turned sideways in the scroll that is getting handled right now, or 0 if there is none.
     */
    public static double getDelta() {
        return delta;
    }

    @EventListener
    public void onScreenMouseScrollPre(ScreenMouseScrollEvent.Pre event) {
        delta = event.getScrollDeltaX();
    }

    @EventListener
    public void onScreenMouseScrollPost(ScreenMouseScrollEvent.Post event) {
        delta = 0.0D;
    }

}
