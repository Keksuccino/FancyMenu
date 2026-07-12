package de.keksuccino.fancymenu.util.mcef;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrappedMCEFBrowserTest {

    @Test
    void keepsInjectionForOpenBrowserAtSameNavigation() {
        assertTrue(WrappedMCEFBrowser.isJavaScriptInjectionCurrent(false, 4L, 4L));
    }

    @Test
    void cancelsInjectionAfterNavigationChanges() {
        assertFalse(WrappedMCEFBrowser.isJavaScriptInjectionCurrent(false, 4L, 5L));
    }

    @Test
    void cancelsInjectionAfterBrowserCloses() {
        assertFalse(WrappedMCEFBrowser.isJavaScriptInjectionCurrent(true, 4L, 4L));
    }
}
