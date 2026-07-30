package de.keksuccino.fancymenu.util.rinku;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrappedRinkuBrowserTest {

    @Test
    void keepsInjectionForOpenBrowserAtSameNavigation() {
        assertTrue(WrappedRinkuBrowser.isJavaScriptInjectionCurrent(false, 4L, 4L));
    }

    @Test
    void cancelsInjectionAfterNavigationChanges() {
        assertFalse(WrappedRinkuBrowser.isJavaScriptInjectionCurrent(false, 4L, 5L));
    }

    @Test
    void cancelsInjectionAfterBrowserCloses() {
        assertFalse(WrappedRinkuBrowser.isJavaScriptInjectionCurrent(true, 4L, 4L));
    }
}
