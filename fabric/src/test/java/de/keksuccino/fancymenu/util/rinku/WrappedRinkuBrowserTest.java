package de.keksuccino.fancymenu.util.rinku;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrappedRinkuBrowserTest {

    @Test
    void keepsDelayedApiInjectionForTheCurrentOpenNavigation() {
        assertTrue(WrappedRinkuBrowser.isApiInjectionCurrent(false, 4L, 4L));
    }

    @Test
    void invalidatesDelayedApiInjectionAfterNavigationChanges() {
        assertFalse(WrappedRinkuBrowser.isApiInjectionCurrent(false, 5L, 4L));
        assertFalse(WrappedRinkuBrowser.isApiInjectionCurrent(false, 3L, 4L));
    }

    @Test
    void invalidatesDelayedApiInjectionAfterBrowserClosure() {
        assertFalse(WrappedRinkuBrowser.isApiInjectionCurrent(true, 4L, 4L));
    }

}
