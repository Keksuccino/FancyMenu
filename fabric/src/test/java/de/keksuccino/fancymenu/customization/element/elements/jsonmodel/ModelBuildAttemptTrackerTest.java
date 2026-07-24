package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import net.minecraft.client.resources.model.cuboid.CuboidModel;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
            ModelBuildAttemptTracker.Attempt attempt = tracker.beginAttempt();
            if (attempt != null) {
                buildAttempts.incrementAndGet();
                assertThrows(RuntimeException.class, () -> CuboidModel.fromStream(new StringReader(attempt.modelJson())));
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
        assertTrue(tracker.beginAttempt() != null);
        List<String> firstSnapshot = tracker.linesSnapshot();
        asynchronousLines.set(0, "new");

        assertAll(() -> assertEquals(List.of("old", "model"), firstSnapshot), () -> assertThrows(UnsupportedOperationException.class, () -> firstSnapshot.add("mutation")));
        ModelBuildAttemptTracker.Observation changed = tracker.observe(1L, asynchronousLines);
        ModelBuildAttemptTracker.Attempt attempt = tracker.beginAttempt();
        assertAll(() -> assertTrue(changed.changed()), () -> assertTrue(attempt != null), () -> assertEquals("new\nmodel", attempt.modelJson()));
    }

    @Test
    void equalReplacementDoesNotAllocateAnotherSnapshotOrResetAttempt() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(2L, List.of("same"));
        assertTrue(tracker.beginAttempt() != null);
        List<String> firstSnapshot = tracker.linesSnapshot();

        ModelBuildAttemptTracker.Observation unchanged = tracker.observe(2L, new ArrayList<>(List.of("same")));

        assertAll(() -> assertFalse(unchanged.changed()), () -> assertTrue(tracker.beginAttempt() == null), () -> assertSame(firstSnapshot, tracker.linesSnapshot()));
    }

    @Test
    void revisionChangeRetriesUnchangedContentForResourceAndReloadInvalidation() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(8L, List.of("valid"));
        assertTrue(tracker.beginAttempt() != null);

        ModelBuildAttemptTracker.Observation changed = tracker.observe(9L, List.of("valid"));

        ModelBuildAttemptTracker.Attempt attempt = tracker.beginAttempt();
        assertAll(() -> assertTrue(changed.changed()), () -> assertFalse(changed.contentChanged()), () -> assertTrue(attempt != null), () -> assertEquals("valid", attempt.modelJson()));
    }

    @Test
    void nullAndEmptyAsyncStatesRemainRetryableWithoutStartingAnAttempt() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();

        assertAll(() -> assertFalse(tracker.observe(1L, null).hasContent()), () -> assertTrue(tracker.beginAttempt() == null), () -> assertFalse(tracker.observe(1L, List.of()).hasContent()), () -> assertTrue(tracker.beginAttempt() == null));
        ModelBuildAttemptTracker.Observation loaded = tracker.observe(1L, List.of("loaded"));
        assertAll(() -> assertTrue(loaded.changed()), () -> assertTrue(loaded.hasContent()), () -> assertTrue(tracker.beginAttempt() != null));
    }

    @Test
    void lineOrderIsRetainedAndJsonCannotBeReadBeforeAttemptStarts() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(1L, List.of("first", "second", "third"));

        ModelBuildAttemptTracker.Attempt attempt = tracker.beginAttempt();
        assertTrue(attempt != null);
        assertEquals("first\nsecond\nthird", attempt.modelJson());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void materializationFailureRemainsAttemptedUntilInputChanges() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        List<String> brokenLines = (List) List.of(new Object());
        tracker.observe(1L, brokenLines);

        ModelBuildAttemptTracker.Attempt firstAttempt = tracker.beginAttempt();
        assertTrue(firstAttempt != null);
        assertThrows(RuntimeException.class, firstAttempt::modelJson);
        tracker.observe(1L, brokenLines);
        assertTrue(tracker.beginAttempt() == null);

        tracker.observe(2L, brokenLines);
        assertTrue(tracker.beginAttempt() != null);
    }

    @Test
    void transientUnavailableObservationDoesNotInvalidateFailedExactRevision() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(5L, List.of("malformed"));
        assertTrue(tracker.beginAttempt() != null);

        assertFalse(tracker.observe(5L, null).hasContent());
        assertTrue(tracker.beginAttempt() == null);
        ModelBuildAttemptTracker.Observation availableAgain = tracker.observe(5L, List.of("malformed"));

        assertAll(() -> assertFalse(availableAgain.changed()), () -> assertTrue(availableAgain.hasContent()), () -> assertTrue(tracker.beginAttempt() == null));
    }

    @Test
    void sourceRevisionChangeWhileUnavailableAdmitsSameContentWhenItReturns() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(5L, List.of("same"));
        assertTrue(tracker.beginAttempt() != null);

        ModelBuildAttemptTracker.Observation reset = tracker.observe(6L, null);
        ModelBuildAttemptTracker.Observation returned = tracker.observe(6L, List.of("same"));

        assertAll(() -> assertTrue(reset.changed()), () -> assertFalse(reset.hasContent()), () -> assertTrue(returned.changed()), () -> assertTrue(tracker.beginAttempt() != null));
    }

    @Test
    void attemptKeepsImmutableExactRevisionAfterNewContentArrives() {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(10L, new ArrayList<>(List.of("old")));
        ModelBuildAttemptTracker.Attempt oldAttempt = tracker.beginAttempt();
        tracker.observe(10L, new ArrayList<>(List.of("new")));
        ModelBuildAttemptTracker.Attempt newAttempt = tracker.beginAttempt();

        assertAll(() -> assertEquals(10L, oldAttempt.revision()), () -> assertEquals("old", oldAttempt.modelJson()), () -> assertEquals("new", newAttempt.modelJson()), () -> assertThrows(UnsupportedOperationException.class, () -> oldAttempt.lines().add("mutation")));
    }

    @Test
    void concurrentCallersCanClaimRevisionOnlyOnce() throws Exception {
        ModelBuildAttemptTracker tracker = new ModelBuildAttemptTracker();
        tracker.observe(12L, List.of("model"));
        int threadCount = 12;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<ModelBuildAttemptTracker.Attempt>> futures = new ArrayList<>();
            for (int thread = 0; thread < threadCount; thread++) futures.add(executor.submit(() -> {
                if (!start.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting to start concurrent model-build claims");
                return tracker.beginAttempt();
            }));
            start.countDown();
            int claims = 0;
            for (Future<ModelBuildAttemptTracker.Attempt> future : futures) {
                if (future.get(5L, TimeUnit.SECONDS) != null) claims++;
            }
            assertEquals(1, claims);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }
    }

}
