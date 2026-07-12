package de.keksuccino.fancymenu.customization.gameintro;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameIntroAttemptControllerTest {

    @Test
    void normalCompletionStopsInitializesConsumesAndCompletesInOrder() {
        GameIntroAttemptController controller = new GameIntroAttemptController();
        List<String> actions = new ArrayList<>();

        boolean completed = controller.completeAttempt(() -> actions.add("stop"), () -> actions.add("initialize"), () -> { assertTrue(controller.isCompleted()); actions.add("consume"); });

        assertTrue(completed);
        assertEquals(List.of("stop", "initialize", "consume"), actions);
        assertTrue(controller.isCompleted());
    }

    @Test
    void replacementStaysRetryableUntilLaterAttemptCompletes() {
        AtomicBoolean consumed = new AtomicBoolean(false);
        AtomicInteger stops = new AtomicInteger();
        GameIntroAttemptController replaced = new GameIntroAttemptController();

        assertTrue(replaced.replaceAttempt(() -> { assertTrue(replaced.isReplaced()); stops.incrementAndGet(); }));
        assertTrue(replaced.isReplaced());
        assertFalse(consumed.get());
        assertEquals(1, stops.get());

        GameIntroAttemptController retry = new GameIntroAttemptController();
        assertTrue(retry.completeAttempt(stops::incrementAndGet, () -> {}, () -> consumed.set(true)));
        assertTrue(consumed.get());
        assertTrue(retry.isCompleted());
        assertEquals(2, stops.get());
    }

    @Test
    void unavailableIntroRemainsRetryableAndResolverRunsOncePerAttempt() {
        Object intro = new Object();
        AtomicInteger resolutions = new AtomicInteger();

        assertNull(GameIntroAttemptController.resolveRetryableIntro(false, () -> { resolutions.incrementAndGet(); return null; }));
        assertSame(intro, GameIntroAttemptController.resolveRetryableIntro(false, () -> { resolutions.incrementAndGet(); return intro; }));
        assertEquals(2, resolutions.get());
    }

    @Test
    void skipCompletionConsumesExactlyOnce() {
        GameIntroAttemptController controller = new GameIntroAttemptController();
        AtomicInteger consumptions = new AtomicInteger();

        assertTrue(controller.completeAttempt(() -> {}, () -> {}, consumptions::incrementAndGet));
        assertFalse(controller.completeAttempt(() -> {}, () -> {}, consumptions::incrementAndGet));
        assertEquals(1, consumptions.get());
    }

    @Test
    void preconsumedManualIntroSkipsAutomaticResolutionAndStaysConsumedWhenReplaced() {
        AtomicBoolean consumed = new AtomicBoolean(true);
        AtomicInteger resolutions = new AtomicInteger();
        GameIntroAttemptController controller = new GameIntroAttemptController();

        assertNull(GameIntroAttemptController.resolveRetryableIntro(consumed.get(), () -> { resolutions.incrementAndGet(); return new Object(); }));
        assertTrue(controller.replaceAttempt(() -> {}));
        assertTrue(consumed.get());
        assertEquals(0, resolutions.get());
    }

    @Test
    void completionAndReplacementAreIdempotent() {
        AtomicInteger stops = new AtomicInteger();
        AtomicInteger initializations = new AtomicInteger();
        AtomicInteger consumptions = new AtomicInteger();
        GameIntroAttemptController completed = new GameIntroAttemptController();

        assertTrue(completed.completeAttempt(stops::incrementAndGet, initializations::incrementAndGet, consumptions::incrementAndGet));
        assertFalse(completed.completeAttempt(stops::incrementAndGet, initializations::incrementAndGet, consumptions::incrementAndGet));
        assertFalse(completed.replaceAttempt(stops::incrementAndGet));
        assertEquals(1, stops.get());
        assertEquals(1, initializations.get());
        assertEquals(1, consumptions.get());

        GameIntroAttemptController replaced = new GameIntroAttemptController();
        assertTrue(replaced.replaceAttempt(stops::incrementAndGet));
        assertFalse(replaced.replaceAttempt(stops::incrementAndGet));
        assertFalse(replaced.completeAttempt(stops::incrementAndGet, initializations::incrementAndGet, consumptions::incrementAndGet));
        assertEquals(2, stops.get());
    }

    @Test
    void failedInitializationRestoresActiveStateWithoutConsumption() {
        GameIntroAttemptController controller = new GameIntroAttemptController();
        AtomicInteger consumptions = new AtomicInteger();

        assertThrows(IllegalStateException.class, () -> controller.completeAttempt(() -> {}, () -> { throw new IllegalStateException("initialization failed"); }, consumptions::incrementAndGet));

        assertTrue(controller.isActive());
        assertEquals(0, consumptions.get());
        assertTrue(controller.completeAttempt(() -> {}, () -> {}, consumptions::incrementAndGet));
        assertEquals(1, consumptions.get());
    }

}
