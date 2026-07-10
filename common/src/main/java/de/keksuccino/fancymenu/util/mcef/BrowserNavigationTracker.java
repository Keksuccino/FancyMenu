package de.keksuccino.fancymenu.util.mcef;

import org.jetbrains.annotations.Nullable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks main-frame navigation independently from CEF. Delayed work must capture and validate a generation so it
 * cannot target a page that was replaced or a browser that was closed while the work was queued.
 */
final class BrowserNavigationTracker {

    private final AtomicLong generation = new AtomicLong();
    @Nullable private volatile String expectedMainFrameUrl;

    BrowserNavigationTracker(@Nullable String expectedMainFrameUrl) {
        this.expectedMainFrameUrl = expectedMainFrameUrl;
    }

    void beginMainFrameNavigation(@Nullable String expectedMainFrameUrl) {
        this.expectedMainFrameUrl = expectedMainFrameUrl;
        this.generation.incrementAndGet();
    }

    void invalidateGeneration() {
        this.generation.incrementAndGet();
    }

    long captureGeneration() {
        return this.generation.get();
    }

    boolean isCurrentGeneration(long capturedGeneration) {
        return this.generation.get() == capturedGeneration;
    }

    @Nullable
    String getExpectedMainFrameUrl() {
        return this.expectedMainFrameUrl;
    }

}
