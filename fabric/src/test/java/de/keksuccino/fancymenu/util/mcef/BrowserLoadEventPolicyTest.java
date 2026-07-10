package de.keksuccino.fancymenu.util.mcef;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserLoadEventPolicyTest {

    @Test
    void acceptsSuccessfulHttpStatusRange() {
        assertTrue(BrowserLoadEventPolicy.isSuccessfulLoad("https://example.com", 200));
        assertTrue(BrowserLoadEventPolicy.isSuccessfulLoad("https://example.com", 299));
        assertFalse(BrowserLoadEventPolicy.isSuccessfulLoad("https://example.com", 199));
        assertFalse(BrowserLoadEventPolicy.isSuccessfulLoad("https://example.com", 300));
        assertFalse(BrowserLoadEventPolicy.isSuccessfulLoad("https://example.com", 404));
    }

    @Test
    void acceptsStatuslessLocalAndBlankPages() {
        assertTrue(BrowserLoadEventPolicy.isSuccessfulLoad("file:/tmp/page.html", 0));
        assertTrue(BrowserLoadEventPolicy.isSuccessfulLoad("about:blank", 0));
        assertTrue(BrowserLoadEventPolicy.isSuccessfulLoad("ABOUT:BLANK", 0));
        assertFalse(BrowserLoadEventPolicy.isSuccessfulLoad("https://example.com", 0));
        assertFalse(BrowserLoadEventPolicy.isSuccessfulLoad(null, 0));
        assertFalse(BrowserLoadEventPolicy.isSuccessfulLoad("file:/tmp/page.html", 500));
    }

    @Test
    void rejectsOnlyBlankPreloadEventsSupersededByAnotherExpectedPage() {
        assertTrue(BrowserLoadEventPolicy.isStalePreloadedPage("about:blank", "https://example.com"));
        assertTrue(BrowserLoadEventPolicy.isStalePreloadedPage("ABOUT:BLANK", "file:/tmp/page.html"));
        assertFalse(BrowserLoadEventPolicy.isStalePreloadedPage("about:blank", "about:blank"));
        assertFalse(BrowserLoadEventPolicy.isStalePreloadedPage("about:blank", "ABOUT:BLANK"));
        assertFalse(BrowserLoadEventPolicy.isStalePreloadedPage("https://old.example", "https://new.example"));
        assertFalse(BrowserLoadEventPolicy.isStalePreloadedPage(null, "https://example.com"));
        assertFalse(BrowserLoadEventPolicy.isStalePreloadedPage("about:blank", null));
    }

}
