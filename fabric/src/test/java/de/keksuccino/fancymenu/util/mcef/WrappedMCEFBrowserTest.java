package de.keksuccino.fancymenu.util.mcef;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrappedMCEFBrowserTest {

    @Test
    void keepsDelayedApiInjectionForTheCurrentOpenNavigation() {
        assertTrue(WrappedMCEFBrowser.isApiInjectionCurrent(false, 4L, 4L));
    }

    @Test
    void invalidatesDelayedApiInjectionAfterNavigationChanges() {
        assertFalse(WrappedMCEFBrowser.isApiInjectionCurrent(false, 5L, 4L));
        assertFalse(WrappedMCEFBrowser.isApiInjectionCurrent(false, 3L, 4L));
    }

    @Test
    void invalidatesDelayedApiInjectionAfterBrowserClosure() {
        assertFalse(WrappedMCEFBrowser.isApiInjectionCurrent(true, 4L, 4L));
    }

}
