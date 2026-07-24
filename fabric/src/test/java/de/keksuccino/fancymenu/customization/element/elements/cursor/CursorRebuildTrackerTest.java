package de.keksuccino.fancymenu.customization.element.elements.cursor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorRebuildTrackerTest {

    @Test
    void failedAttemptIsNotRepeatedForTheSameEffectiveConfiguration() {
        CursorRebuildTracker tracker = new CursorRebuildTracker();
        Object texture = new Object();

        assertTrue(tracker.shouldAttempt(texture, "fancymenu:cursor", 1, 2, true, false));
        tracker.recordResult(false);

        assertFalse(tracker.shouldAttempt(texture, new String("fancymenu:cursor"), 1, 2, true, false));
    }

    @Test
    void everyConfigurationKeyDimensionAllowsOneNewAttempt() {
        Object texture = new Object();
        CursorRebuildTracker tracker = failedTracker(texture, "fancymenu:cursor", 1, 2, true);
        assertTrue(tracker.shouldAttempt(new Object(), "fancymenu:cursor", 1, 2, true, false));

        texture = new Object();
        tracker = failedTracker(texture, "fancymenu:cursor", 1, 2, true);
        assertTrue(tracker.shouldAttempt(texture, "fancymenu:other_cursor", 1, 2, true, false));

        texture = new Object();
        tracker = failedTracker(texture, "fancymenu:cursor", 1, 2, true);
        assertTrue(tracker.shouldAttempt(texture, "fancymenu:cursor", 2, 2, true, false));

        texture = new Object();
        tracker = failedTracker(texture, "fancymenu:cursor", 1, 2, true);
        assertTrue(tracker.shouldAttempt(texture, "fancymenu:cursor", 1, 3, true, false));
    }

    @Test
    void ineligibleOrNullLocationIsNotNegativelyCached() {
        CursorRebuildTracker tracker = new CursorRebuildTracker();
        Object texture = new Object();

        assertFalse(tracker.shouldAttempt(texture, null, 1, 2, false, false));
        assertFalse(tracker.shouldAttempt(texture, null, 1, 2, false, false));
        assertTrue(tracker.shouldAttempt(texture, "fancymenu:cursor", 1, 2, true, false));
    }

    @Test
    void previewEligibilityChangeAllowsOneAttemptWithoutAnotherKeyChange() {
        CursorRebuildTracker tracker = new CursorRebuildTracker();
        Object texture = new Object();

        assertFalse(tracker.shouldAttempt(texture, "fancymenu:cursor", 1, 2, false, false));
        assertTrue(tracker.shouldAttempt(texture, "fancymenu:cursor", 1, 2, true, false));
        tracker.recordResult(false);
        assertFalse(tracker.shouldAttempt(texture, "fancymenu:cursor", 1, 2, true, false));
    }

    @Test
    void externalRegistryLossGetsOneAttemptForUnchangedConfiguration() {
        CursorRebuildTracker tracker = new CursorRebuildTracker();
        Object texture = new Object();

        assertFalse(tracker.shouldAttempt(texture, "fancymenu:cursor", 1, 2, true, true));
        assertTrue(tracker.shouldAttempt(texture, "fancymenu:cursor", 1, 2, true, false));
        tracker.recordResult(false);
        assertFalse(tracker.shouldAttempt(texture, "fancymenu:cursor", 1, 2, true, false));
    }

    @Test
    void explicitResetClearsFailedAttempt() {
        Object texture = new Object();
        CursorRebuildTracker tracker = failedTracker(texture, "fancymenu:cursor", 1, 2, true);

        tracker.reset();

        assertTrue(tracker.shouldAttempt(texture, "fancymenu:cursor", 1, 2, true, false));
    }

    private static CursorRebuildTracker failedTracker(Object texture, Object source, int hotspotX, int hotspotY, boolean eligible) {
        CursorRebuildTracker tracker = new CursorRebuildTracker();
        assertTrue(tracker.shouldAttempt(texture, source, hotspotX, hotspotY, eligible, false));
        tracker.recordResult(false);
        return tracker;
    }
}
