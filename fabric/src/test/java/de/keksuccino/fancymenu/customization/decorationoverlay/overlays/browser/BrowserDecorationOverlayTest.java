package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.browser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrowserDecorationOverlayTest {

    @Test
    @ResourceLock("PlaceholderParser global state")
    void resolvedUrlKeepsParametersThatLookLikeFormattingCodes() {
        String url = "https://example.com/?id={\"placeholder\":\"browser_overlay_unregistered_test\"}&embed=true&f=1&r=0";

        assertEquals(url, BrowserDecorationOverlay.resolveUrl(url));
    }

    @Test
    @ResourceLock("PlaceholderParser global state")
    void missingOrBlankUrlFallsBackToBlankPage() {
        assertEquals("about:blank", BrowserDecorationOverlay.resolveUrl(null));
        assertEquals("about:blank", BrowserDecorationOverlay.resolveUrl(""));
        assertEquals("about:blank", BrowserDecorationOverlay.resolveUrl("   "));
    }

}
