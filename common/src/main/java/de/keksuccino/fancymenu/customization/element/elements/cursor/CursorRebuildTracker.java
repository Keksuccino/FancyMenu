package de.keksuccino.fancymenu.customization.element.elements.cursor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Prevents a failed native cursor build from blocking the render thread again on every frame. Failures are cached only
 * for one exact effective configuration; texture replacement, source/hotspot changes, preview eligibility changes, an
 * explicit reset, or loss of a previously valid registry entry each permit one new attempt.
 */
final class CursorRebuildTracker {

    @Nullable
    private Object textureIdentity;
    @Nullable
    private Object source;
    private int hotspotX;
    private int hotspotY;
    private boolean eligible;
    private boolean initialized;
    private boolean failed;

    boolean shouldAttempt(@NotNull Object textureIdentity, @Nullable Object source, int hotspotX, int hotspotY, boolean eligible, boolean registrationCurrent) {
        boolean configurationChanged = !this.initialized || this.textureIdentity != textureIdentity || !Objects.equals(this.source, source) || this.hotspotX != hotspotX || this.hotspotY != hotspotY || this.eligible != eligible;
        if (configurationChanged) {
            this.textureIdentity = textureIdentity;
            this.source = source;
            this.hotspotX = hotspotX;
            this.hotspotY = hotspotY;
            this.eligible = eligible;
            this.initialized = true;
            this.failed = false;
        }
        if (!eligible) return false;
        if (registrationCurrent) {
            this.failed = false;
            return false;
        }
        return !this.failed;
    }

    void recordResult(boolean successful) {
        this.failed = !successful;
    }

    void reset() {
        this.textureIdentity = null;
        this.source = null;
        this.hotspotX = 0;
        this.hotspotY = 0;
        this.eligible = false;
        this.initialized = false;
        this.failed = false;
    }

}
