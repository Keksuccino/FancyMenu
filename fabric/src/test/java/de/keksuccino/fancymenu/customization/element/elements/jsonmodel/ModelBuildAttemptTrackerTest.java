package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import net.minecraft.client.resources.model.cuboid.CuboidModel;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelBuildAttemptTrackerTest {

    @Test
    void unchangedMalformedInputBuildsAndReportsOnlyOnce() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        AtomicInteger buildAttempts = new AtomicInteger();
        AtomicInteger diagnostics = new AtomicInteger();

        for (int frame = 0; frame < 10; frame++) {
            tracker.observe(4L, List.of("{"));
            if (tracker.beginAttempt()) {
                buildAttempts.incrementAndGet();
                assertThrows(RuntimeException.class, () -> CuboidModel.fromStream(new StringReader(tracker.modelJson())));
                diagnostics.incrementAndGet();
            }
        }

        assertAll(() -> assertEquals(1, buildAttempts.get()), () -> assertEquals(1, diagnostics.get()));
    }

    @Test
    void immutableSnapshotDetectsMutationOfTheSameAsynchronousList() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        List<String> asynchronousLines = new ArrayList<>(List.of("old", "model"));

        assertTrue(tracker.observe(1L, asynchronousLines).changed());
        assertTrue(tracker.beginAttempt());
        List<String> firstSnapshot = tracker.linesSnapshot();
        asynchronousLines.set(0, "new");

        assertAll(() -> assertEquals(List.of("old", "model"), firstSnapshot), () -> assertThrows(UnsupportedOperationException.class, () -> firstSnapshot.add("mutation")));
        ModelBuildAttemptTracker.Observation changed = tracker.observe(1L, asynchronousLines);
        assertAll(() -> assertTrue(changed.changed()), () -> assertTrue(tracker.beginAttempt()), () -> assertEquals("new\nmodel", tracker.modelJson()));
    }

    @Test
    void equalReplacementDoesNotAllocateAnotherSnapshotOrResetAttempt() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(2L, List.of("same"));
        assertTrue(tracker.beginAttempt());
        List<String> firstSnapshot = tracker.linesSnapshot();

        ModelBuildAttemptTracker.Observation unchanged = tracker.observe(2L, new ArrayList<>(List.of("same")));

        assertAll(() -> assertFalse(unchanged.changed()), () -> assertFalse(tracker.beginAttempt()), () -> assertSame(firstSnapshot, tracker.linesSnapshot()));
    }

    @Test
    void revisionChangeRetriesUnchangedContentForResourceAndReloadInvalidation() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(8L, List.of("valid"));
        assertTrue(tracker.beginAttempt());

        ModelBuildAttemptTracker.Observation changed = tracker.observe(9L, List.of("valid"));

        assertAll(() -> assertTrue(changed.changed()), () -> assertFalse(changed.contentChanged()), () -> assertTrue(tracker.beginAttempt()), () -> assertEquals("valid", tracker.modelJson()));
    }

    @Test
    void nullAndEmptyAsyncStatesRemainRetryableWithoutStartingAnAttempt() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();

        assertAll(() -> assertFalse(tracker.observe(1L, null).hasContent()), () -> assertFalse(tracker.beginAttempt()), () -> assertFalse(tracker.observe(1L, List.of()).hasContent()), () -> assertFalse(tracker.beginAttempt()));
        ModelBuildAttemptTracker.Observation loaded = tracker.observe(1L, List.of("loaded"));
        assertAll(() -> assertTrue(loaded.changed()), () -> assertTrue(loaded.hasContent()), () -> assertTrue(tracker.beginAttempt()));
    }

    @Test
    void lineOrderIsRetainedAndJsonCannotBeReadBeforeAttemptStarts() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(1L, List.of("first", "second", "third"));

        assertThrows(IllegalStateException.class, tracker::modelJson);
        assertTrue(tracker.beginAttempt());
        assertEquals("first\nsecond\nthird", tracker.modelJson());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void materializationFailureRemainsAttemptedUntilInputChanges() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        List<String> brokenLines = (List) List.of(new Object());
        tracker.observe(1L, brokenLines);

        assertTrue(tracker.beginAttempt());
        assertThrows(RuntimeException.class, tracker::modelJson);
        tracker.observe(1L, brokenLines);
        assertFalse(tracker.beginAttempt());

        tracker.observe(2L, brokenLines);
        assertTrue(tracker.beginAttempt());
    }

}
