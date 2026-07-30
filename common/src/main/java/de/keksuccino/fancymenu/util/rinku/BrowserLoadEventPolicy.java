package de.keksuccino.fancymenu.util.rinku;

import org.jetbrains.annotations.Nullable;

final class BrowserLoadEventPolicy {

    private BrowserLoadEventPolicy() {
    }

    static boolean isSuccessfulLoad(@Nullable String url, int httpStatusCode) {
        if ((httpStatusCode >= 200) && (httpStatusCode < 300)) return true;
        if (httpStatusCode != 0 || url == null) return false;
        return url.startsWith("file:") || url.equalsIgnoreCase("about:blank");
    }

    static boolean isStalePreloadedPage(@Nullable String eventUrl, @Nullable String expectedMainFrameUrl) {
        return (eventUrl != null) && eventUrl.equalsIgnoreCase("about:blank") && (expectedMainFrameUrl != null) && !expectedMainFrameUrl.equalsIgnoreCase("about:blank");
    }

}
