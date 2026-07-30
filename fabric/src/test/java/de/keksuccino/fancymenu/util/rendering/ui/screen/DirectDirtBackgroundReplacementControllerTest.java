package de.keksuccino.fancymenu.util.rendering.ui.screen;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectDirtBackgroundReplacementControllerTest {

    @Test
    void screenReplacementHandlesDirectDirtCallBeforeGlobalFallback() {
        AtomicInteger screenCalls = new AtomicInteger();
        AtomicInteger globalCalls = new AtomicInteger();

        boolean replaced = DirectDirtBackgroundReplacementController.renderReplacement(false, () -> incrementAndReturn(screenCalls, true), () -> incrementAndReturn(globalCalls, true));

        assertAll(() -> assertTrue(replaced), () -> assertEquals(1, screenCalls.get()), () -> assertEquals(0, globalCalls.get()));
    }

    @Test
    void missingScreenReplacementFallsBackToGlobalReplacement() {
        AtomicInteger screenCalls = new AtomicInteger();
        AtomicInteger globalCalls = new AtomicInteger();

        boolean replaced = DirectDirtBackgroundReplacementController.renderReplacement(false, () -> incrementAndReturn(screenCalls, false), () -> incrementAndReturn(globalCalls, true));

        assertAll(() -> assertTrue(replaced), () -> assertEquals(1, screenCalls.get()), () -> assertEquals(1, globalCalls.get()));
    }

    @Test
    void unavailableReplacementsPreserveVanillaDirtBackground() {
        boolean replaced = DirectDirtBackgroundReplacementController.renderReplacement(false, () -> false, () -> false);

        assertFalse(replaced);
    }

    @Test
    void wrappedDirtCallRemainsOwnedByRenderBackgroundPath() {
        AtomicInteger screenCalls = new AtomicInteger();
        AtomicInteger globalCalls = new AtomicInteger();

        boolean replaced = DirectDirtBackgroundReplacementController.renderReplacement(true, () -> incrementAndReturn(screenCalls, true), () -> incrementAndReturn(globalCalls, true));

        assertAll(() -> assertFalse(replaced), () -> assertEquals(0, screenCalls.get()), () -> assertEquals(0, globalCalls.get()));
    }

    private static boolean incrementAndReturn(AtomicInteger counter, boolean result) {
        counter.incrementAndGet();
        return result;
    }

}
