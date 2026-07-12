package de.keksuccino.fancymenu.customization.gameintro;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameIntroLifecycleControllerTest {

    @Test
    void replacementKeepsIntroUnconsumedAndAllowsRetry() {
        AtomicBoolean introPlayed = new AtomicBoolean();
        AtomicInteger supplierCalls = new AtomicInteger();
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger stops = new AtomicInteger();
        Object intro = new Object();
        Object overlay = new Object();
        GameIntroLifecycleController lifecycle = new GameIntroLifecycleController(stops::incrementAndGet, () -> {}, () -> introPlayed.set(true), () -> {}, throwable -> {});

        assertTrue(GameIntroHandler.tryStartIntro(introPlayed.get(), () -> {
            supplierCalls.incrementAndGet();
            return intro;
        }, value -> starts.incrementAndGet()));

        lifecycle.replaceIfDisplaced(overlay, new Object());

        assertTrue(lifecycle.isClosed());
        assertEquals(1, stops.get());
        assertFalse(introPlayed.get());
        assertTrue(GameIntroHandler.tryStartIntro(introPlayed.get(), () -> {
            supplierCalls.incrementAndGet();
            return intro;
        }, value -> starts.incrementAndGet()));
        assertEquals(2, supplierCalls.get());
        assertEquals(2, starts.get());
    }

    @Test
    void normalCompletionUsesRequiredOrderAndConsumesIntro() {
        List<String> events = new ArrayList<>();
        AtomicBoolean introPlayed = new AtomicBoolean();
        GameIntroLifecycleController lifecycle = new GameIntroLifecycleController(() -> events.add("stop"), () -> events.add("init"), () -> {
            events.add("played");
            introPlayed.set(true);
        }, () -> events.add("clear"), throwable -> events.add("error"));

        lifecycle.complete();

        assertEquals(List.of("stop", "init", "played", "clear"), events);
        assertTrue(introPlayed.get());
        assertTrue(lifecycle.isClosed());
    }

    @Test
    void skipCompletionIsIdempotent() {
        AtomicInteger stops = new AtomicInteger();
        AtomicInteger initializations = new AtomicInteger();
        AtomicInteger playedMarks = new AtomicInteger();
        AtomicInteger clears = new AtomicInteger();
        GameIntroLifecycleController lifecycle = new GameIntroLifecycleController(stops::incrementAndGet, initializations::incrementAndGet, playedMarks::incrementAndGet, clears::incrementAndGet, throwable -> {});

        lifecycle.complete();
        lifecycle.complete();

        assertEquals(1, stops.get());
        assertEquals(1, initializations.get());
        assertEquals(1, playedMarks.get());
        assertEquals(1, clears.get());
    }

    @Test
    void repeatedReplacementDoesNotRunCompletionActions() {
        AtomicInteger stops = new AtomicInteger();
        AtomicInteger initializations = new AtomicInteger();
        AtomicInteger playedMarks = new AtomicInteger();
        AtomicInteger clears = new AtomicInteger();
        Object overlay = new Object();
        GameIntroLifecycleController lifecycle = new GameIntroLifecycleController(stops::incrementAndGet, initializations::incrementAndGet, playedMarks::incrementAndGet, clears::incrementAndGet, throwable -> {});

        lifecycle.replaceIfDisplaced(overlay, new Object());
        lifecycle.replaceIfDisplaced(overlay, null);
        lifecycle.complete();

        assertEquals(1, stops.get());
        assertEquals(0, initializations.get());
        assertEquals(0, playedMarks.get());
        assertEquals(0, clears.get());
    }

    @Test
    void completionContinuesAfterCleanupAndFailureReportingErrors() {
        List<String> events = new ArrayList<>();
        RuntimeException cleanupFailure = new RuntimeException("cleanup");
        GameIntroLifecycleController lifecycle = new GameIntroLifecycleController(() -> {
            events.add("stop");
            throw cleanupFailure;
        }, () -> events.add("init"), () -> events.add("played"), () -> events.add("clear"), throwable -> {
            assertSame(cleanupFailure, throwable);
            events.add("error");
            throw new RuntimeException("reporting");
        });

        lifecycle.complete();

        assertEquals(List.of("stop", "error", "init", "played", "clear"), events);
        assertTrue(lifecycle.isClosed());
    }

    @Test
    void replacementRemainsClosedAfterCleanupFailure() {
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger initializations = new AtomicInteger();
        AtomicInteger playedMarks = new AtomicInteger();
        RuntimeException cleanupFailure = new RuntimeException("cleanup");
        Object overlay = new Object();
        GameIntroLifecycleController lifecycle = new GameIntroLifecycleController(() -> {
            throw cleanupFailure;
        }, initializations::incrementAndGet, playedMarks::incrementAndGet, () -> {}, throwable -> {
            assertSame(cleanupFailure, throwable);
            errors.incrementAndGet();
        });

        lifecycle.replaceIfDisplaced(overlay, null);
        lifecycle.complete();

        assertTrue(lifecycle.isClosed());
        assertEquals(1, errors.get());
        assertEquals(0, initializations.get());
        assertEquals(0, playedMarks.get());
    }

    @Test
    void initializationFailureLeavesLifecycleRetryable() {
        AtomicInteger stops = new AtomicInteger();
        AtomicInteger initializationAttempts = new AtomicInteger();
        AtomicInteger playedMarks = new AtomicInteger();
        AtomicInteger clears = new AtomicInteger();
        RuntimeException initializationFailure = new RuntimeException("init");
        GameIntroLifecycleController lifecycle = new GameIntroLifecycleController(stops::incrementAndGet, () -> {
            if (initializationAttempts.incrementAndGet() == 1) throw initializationFailure;
        }, playedMarks::incrementAndGet, clears::incrementAndGet, throwable -> {});

        assertSame(initializationFailure, assertThrows(RuntimeException.class, lifecycle::complete));
        assertFalse(lifecycle.isClosed());
        assertEquals(0, playedMarks.get());
        assertEquals(0, clears.get());

        lifecycle.complete();

        assertTrue(lifecycle.isClosed());
        assertEquals(1, stops.get());
        assertEquals(2, initializationAttempts.get());
        assertEquals(1, playedMarks.get());
        assertEquals(1, clears.get());
    }

    @Test
    void sameInstanceDoesNotReplaceButNullDoes() {
        AtomicInteger stops = new AtomicInteger();
        Object overlay = new Object();
        GameIntroLifecycleController lifecycle = new GameIntroLifecycleController(stops::incrementAndGet, () -> {}, () -> {}, () -> {}, throwable -> {});

        lifecycle.replaceIfDisplaced(overlay, overlay);

        assertFalse(lifecycle.isClosed());
        assertEquals(0, stops.get());

        lifecycle.replaceIfDisplaced(overlay, null);

        assertTrue(lifecycle.isClosed());
        assertEquals(1, stops.get());
    }

    @Test
    void reentrantReplacementDuringStopAbortsCompletion() {
        List<String> events = new ArrayList<>();
        Object overlay = new Object();
        AtomicReference<GameIntroLifecycleController> lifecycleReference = new AtomicReference<>();
        GameIntroLifecycleController lifecycle = new GameIntroLifecycleController(() -> {
            events.add("stop");
            lifecycleReference.get().replaceIfDisplaced(overlay, new Object());
        }, () -> events.add("init"), () -> events.add("played"), () -> events.add("clear"), throwable -> events.add("error"));
        lifecycleReference.set(lifecycle);

        lifecycle.complete();

        assertEquals(List.of("stop"), events);
        assertTrue(lifecycle.isClosed());
    }

    @Test
    void reentrantReplacementDuringInitializationAbortsCompletion() {
        List<String> events = new ArrayList<>();
        Object overlay = new Object();
        AtomicReference<GameIntroLifecycleController> lifecycleReference = new AtomicReference<>();
        GameIntroLifecycleController lifecycle = new GameIntroLifecycleController(() -> events.add("stop"), () -> {
            events.add("init");
            lifecycleReference.get().replaceIfDisplaced(overlay, new Object());
        }, () -> events.add("played"), () -> events.add("clear"), throwable -> events.add("error"));
        lifecycleReference.set(lifecycle);

        lifecycle.complete();

        assertEquals(List.of("stop", "init"), events);
        assertTrue(lifecycle.isClosed());
    }

    @Test
    void startupQueriesSupplierOnceAndPassesTheSameIntro() {
        AtomicInteger supplierCalls = new AtomicInteger();
        AtomicReference<Object> startedIntro = new AtomicReference<>();
        Object intro = new Object();

        boolean started = GameIntroHandler.tryStartIntro(false, () -> {
            supplierCalls.incrementAndGet();
            return intro;
        }, startedIntro::set);

        assertTrue(started);
        assertEquals(1, supplierCalls.get());
        assertSame(intro, startedIntro.get());
    }

    @Test
    void startupSkipsConsumedAndUnavailableIntros() {
        AtomicInteger consumedSupplierCalls = new AtomicInteger();
        AtomicInteger starts = new AtomicInteger();

        assertFalse(GameIntroHandler.tryStartIntro(true, () -> {
            consumedSupplierCalls.incrementAndGet();
            return new Object();
        }, value -> starts.incrementAndGet()));
        assertFalse(GameIntroHandler.tryStartIntro(false, () -> null, value -> starts.incrementAndGet()));

        assertEquals(0, consumedSupplierCalls.get());
        assertEquals(0, starts.get());
    }

    @Test
    void failedStarterDoesNotConsumeFutureRetry() {
        AtomicInteger supplierCalls = new AtomicInteger();
        AtomicInteger starts = new AtomicInteger();
        Object intro = new Object();

        assertThrows(RuntimeException.class, () -> GameIntroHandler.tryStartIntro(false, () -> {
            supplierCalls.incrementAndGet();
            return intro;
        }, value -> {
            starts.incrementAndGet();
            throw new RuntimeException("start");
        }));
        assertTrue(GameIntroHandler.tryStartIntro(false, () -> {
            supplierCalls.incrementAndGet();
            return intro;
        }, value -> starts.incrementAndGet()));

        assertEquals(2, supplierCalls.get());
        assertEquals(2, starts.get());
    }

}
