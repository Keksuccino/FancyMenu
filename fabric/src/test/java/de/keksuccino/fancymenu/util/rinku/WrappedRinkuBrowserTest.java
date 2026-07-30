package de.keksuccino.fancymenu.util.rinku;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrappedRinkuBrowserTest {

    @Test
    void injectsJavaScriptOnlyForTheCurrentOpenNavigation() {
        assertTrue(WrappedRinkuBrowser.shouldInjectJavaScript(false, 7L, 7L));
        assertFalse(WrappedRinkuBrowser.shouldInjectJavaScript(false, 8L, 7L));
        assertFalse(WrappedRinkuBrowser.shouldInjectJavaScript(true, 7L, 7L));
    }
}
