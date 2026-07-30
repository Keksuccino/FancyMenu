package de.keksuccino.fancymenu.util.rinku;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserLoadEventListenerManagerTest {

    @Test
    void acceptsSuccessfulHttpStatusBoundaries() {
        assertTrue(BrowserLoadEventListenerManager.isSuccessfulLoad("https://example.com", 200));
        assertTrue(BrowserLoadEventListenerManager.isSuccessfulLoad("https://example.com", 299));
    }

    @Test
    void acceptsZeroStatusForSupportedLocalPages() {
        assertTrue(BrowserLoadEventListenerManager.isSuccessfulLoad("file:///tmp/page.html", 0));
        assertTrue(BrowserLoadEventListenerManager.isSuccessfulLoad("about:blank", 0));
        assertTrue(BrowserLoadEventListenerManager.isSuccessfulLoad("ABOUT:BLANK", 0));
    }

    @Test
    void rejectsUnsuccessfulOrUnknownLoads() {
        assertFalse(BrowserLoadEventListenerManager.isSuccessfulLoad("https://example.com", 0));
        assertFalse(BrowserLoadEventListenerManager.isSuccessfulLoad("https://example.com", 199));
        assertFalse(BrowserLoadEventListenerManager.isSuccessfulLoad("https://example.com", 300));
        assertFalse(BrowserLoadEventListenerManager.isSuccessfulLoad(null, 0));
    }

    @Test
    void detectsOnlyObsoletePreloadedBlankPageEvents() {
        assertTrue(BrowserLoadEventListenerManager.isStalePreloadedPage("about:blank", "https://example.com"));
        assertTrue(BrowserLoadEventListenerManager.isStalePreloadedPage("ABOUT:BLANK", "file:///tmp/page.html"));

        assertFalse(BrowserLoadEventListenerManager.isStalePreloadedPage("about:blank", "about:blank"));
        assertFalse(BrowserLoadEventListenerManager.isStalePreloadedPage("https://example.com", "https://example.com"));
        assertFalse(BrowserLoadEventListenerManager.isStalePreloadedPage(null, "https://example.com"));
        assertFalse(BrowserLoadEventListenerManager.isStalePreloadedPage("about:blank", null));
    }
}
