package de.keksuccino.fancymenu.util.rinku;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserLoadEventListenerManagerTest {

    @Test
    void treatsSuccessfulHttpStatusRangeAsSuccessful() {
        assertTrue(BrowserLoadEventListenerManager.isSuccessfulLoad("https://example.com", 200));
        assertTrue(BrowserLoadEventListenerManager.isSuccessfulLoad("https://example.com", 299));
    }

    @Test
    void rejectsHttpStatusesOutsideSuccessfulRange() {
        assertFalse(BrowserLoadEventListenerManager.isSuccessfulLoad("https://example.com", 199));
        assertFalse(BrowserLoadEventListenerManager.isSuccessfulLoad("https://example.com", 300));
    }

    @Test
    void acceptsLocalAndBlankPagesWithoutHttpStatus() {
        assertTrue(BrowserLoadEventListenerManager.isSuccessfulLoad("file:///tmp/menu.html", 0));
        assertTrue(BrowserLoadEventListenerManager.isSuccessfulLoad("ABOUT:BLANK", 0));
    }

    @Test
    void rejectsOtherPagesWithoutHttpStatus() {
        assertFalse(BrowserLoadEventListenerManager.isSuccessfulLoad("https://example.com", 0));
        assertFalse(BrowserLoadEventListenerManager.isSuccessfulLoad(null, 0));
    }

    @Test
    void identifiesPooledBlankCompletionAfterTargetNavigationWasRequested() {
        assertTrue(BrowserLoadEventListenerManager.isStalePreloadedPage("about:blank", "https://example.com"));
        assertTrue(BrowserLoadEventListenerManager.isStalePreloadedPage("ABOUT:BLANK", "file:///tmp/menu.html"));
    }

    @Test
    void keepsExpectedBlankAndNonBlankEvents() {
        assertFalse(BrowserLoadEventListenerManager.isStalePreloadedPage("about:blank", "ABOUT:BLANK"));
        assertFalse(BrowserLoadEventListenerManager.isStalePreloadedPage("https://example.com", "https://example.com"));
        assertFalse(BrowserLoadEventListenerManager.isStalePreloadedPage(null, "https://example.com"));
        assertFalse(BrowserLoadEventListenerManager.isStalePreloadedPage("about:blank", null));
    }
}
