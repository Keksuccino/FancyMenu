package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelBuildAttemptTrackerTest {

    @Test
    void unchangedInputCanBeClaimedOnlyOnce() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(1L, List.of("model"));

        assertEquals("model", tracker.beginAttempt().modelJson());
        assertNull(tracker.beginAttempt());
        assertFalse(tracker.observe(1L, List.of("model")).changed());
        assertNull(tracker.beginAttempt());
    }

    @Test
    void mutatedAsynchronousListPublishesAnImmutableNewSnapshot() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        List<String> lines = new ArrayList<>(List.of("old", "model"));
        tracker.observe(1L, lines);
        ModelBuildAttemptTracker.Attempt oldAttempt = tracker.beginAttempt();
        lines.set(0, "new");

        assertTrue(tracker.observe(1L, lines).contentChanged());
        ModelBuildAttemptTracker.Attempt newAttempt = tracker.beginAttempt();
        assertEquals("old\nmodel", oldAttempt.modelJson());
        assertEquals("new\nmodel", newAttempt.modelJson());
        assertThrows(UnsupportedOperationException.class, () -> oldAttempt.lines().add("mutation"));
    }

    @Test
    void readinessBlipDoesNotRetryTheSameFailedRevision() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(4L, List.of("malformed"));
        assertTrue(tracker.beginAttempt() != null);

        assertFalse(tracker.observe(4L, null).hasContent());
        assertTrue(tracker.observe(4L, List.of("malformed")).hasContent());
        assertNull(tracker.beginAttempt());
    }

    @Test
    void revisionChangeRetriesEqualContent() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(8L, List.of("same"));
        tracker.beginAttempt();

        ModelBuildAttemptTracker.Observation observation = tracker.observe(9L, List.of("same"));

        assertTrue(observation.changed());
        assertFalse(observation.contentChanged());
        assertTrue(tracker.beginAttempt() != null);
    }
}
