package de.keksuccino.fancymenu.customization.background.backgrounds.browser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrowserMenuBackgroundTest {

    @Test
    @ResourceLock("PlaceholderParser global state")
    void resolvedUrlKeepsParametersThatLookLikeFormattingCodes() {
        String url = "https://example.com/?id={\"placeholder\":\"browser_background_unregistered_test\"}&embed=true&f=1&r=0";

        assertEquals(url, BrowserMenuBackground.resolveUrl(url));
    }

    @Test
    @ResourceLock("PlaceholderParser global state")
    void missingOrBlankUrlFallsBackToBlankPage() {
        assertEquals("about:blank", BrowserMenuBackground.resolveUrl(null));
        assertEquals("about:blank", BrowserMenuBackground.resolveUrl(""));
        assertEquals("about:blank", BrowserMenuBackground.resolveUrl("   "));
    }

}
