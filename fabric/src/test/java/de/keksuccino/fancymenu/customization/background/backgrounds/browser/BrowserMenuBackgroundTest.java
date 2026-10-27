package de.keksuccino.fancymenu.customization.background.backgrounds.browser;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("PlaceholderParser global state")
class BrowserMenuBackgroundTest {

    @Test
    void resolvedUrlKeepsParametersThatLookLikeFormattingCodes() {
        String url = "https://example.com/?id=" + TestPlaceholders.echo("42") + "&embed=true&f=1&r=0";

        assertEquals("https://example.com/?id=42&embed=true&f=1&r=0", resolveUrl(url));
    }

    @Test
    void missingOrBlankUrlFallsBackToBlankPage() {
        assertEquals("about:blank", resolveUrl(null));
        assertEquals("about:blank", resolveUrl(""));
        assertEquals("about:blank", resolveUrl("   "));
        assertEquals("about:blank", resolveUrl(TestPlaceholders.echo("")));
    }

    private static String resolveUrl(@Nullable String url) {
        PlaceholderParser.PlaceholderCachingController originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        try {
            PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
            BrowserMenuBackground background = new BrowserMenuBackground(new BrowserMenuBackgroundBuilder());
            background.url.set(url);
            return background.resolveUrl();
        } finally {
            PlaceholderParser.setPlaceholderCachingController(originalCachingController);
        }
    }

}
