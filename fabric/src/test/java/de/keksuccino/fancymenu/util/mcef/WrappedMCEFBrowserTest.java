package de.keksuccino.fancymenu.util.mcef;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrappedMCEFBrowserTest {

    @Test
    void injectsJavaScriptOnlyForTheCurrentOpenNavigation() {
        assertTrue(WrappedMCEFBrowser.shouldInjectJavaScript(false, 7L, 7L));
        assertFalse(WrappedMCEFBrowser.shouldInjectJavaScript(false, 8L, 7L));
        assertFalse(WrappedMCEFBrowser.shouldInjectJavaScript(true, 7L, 7L));
    }
}
