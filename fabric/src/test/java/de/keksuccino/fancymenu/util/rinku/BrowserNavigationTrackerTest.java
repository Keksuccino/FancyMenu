package de.keksuccino.fancymenu.util.rinku;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserNavigationTrackerTest {

    @Test
    void preservesInitialExpectedUrlAndGeneration() {
        BrowserNavigationTracker tracker = new BrowserNavigationTracker("https://initial.example");
        long generation = tracker.captureGeneration();

        assertEquals("https://initial.example", tracker.getExpectedMainFrameUrl());
        assertTrue(tracker.isCurrentGeneration(generation));
    }

    @Test
    void beginningNavigationUpdatesExpectedUrlAndInvalidatesDelayedWork() {
        BrowserNavigationTracker tracker = new BrowserNavigationTracker("https://initial.example");
        long initialGeneration = tracker.captureGeneration();

        tracker.beginMainFrameNavigation("https://next.example");
        long nextGeneration = tracker.captureGeneration();

        assertEquals("https://next.example", tracker.getExpectedMainFrameUrl());
        assertFalse(tracker.isCurrentGeneration(initialGeneration));
        assertTrue(tracker.isCurrentGeneration(nextGeneration));

        tracker.beginMainFrameNavigation(null);
        assertNull(tracker.getExpectedMainFrameUrl());
        assertFalse(tracker.isCurrentGeneration(nextGeneration));
    }

    @Test
    void invalidationCancelsDelayedWorkWithoutChangingExpectedUrl() {
        BrowserNavigationTracker tracker = new BrowserNavigationTracker("https://current.example");
        long scheduledGeneration = tracker.captureGeneration();

        tracker.invalidateGeneration();

        assertEquals("https://current.example", tracker.getExpectedMainFrameUrl());
        assertFalse(tracker.isCurrentGeneration(scheduledGeneration));
        assertTrue(tracker.isCurrentGeneration(tracker.captureGeneration()));
    }

}
