package de.keksuccino.fancymenu.mixin.support.client;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetifiedTextReplacementControllerTest {

    @Test
    void defaultsToTheVanillaPathBeforeScreenInitialization() {
        WidgetifiedTextReplacementController<Object> controller = new WidgetifiedTextReplacementController<>();

        assertTrue(controller.shouldRenderVanillaText());
    }

    @Test
    void disabledCustomizationUsesOnlyTheVanillaTextPath() {
        WidgetifiedTextReplacementController<Object> controller = new WidgetifiedTextReplacementController<>();
        AtomicInteger factoryCalls = new AtomicInteger();
        Supplier<Object> replacementFactory = () -> {
            factoryCalls.incrementAndGet();
            return new Object();
        };

        Object replacement = controller.initialize(false, replacementFactory);

        assertAll(() -> assertNull(replacement), () -> assertTrue(controller.shouldRenderVanillaText()), () -> assertEquals(0, factoryCalls.get()));
    }

    @Test
    void enabledCustomizationUsesOnlyTheReplacementTextPath() {
        WidgetifiedTextReplacementController<Object> controller = new WidgetifiedTextReplacementController<>();
        AtomicInteger factoryCalls = new AtomicInteger();
        Object expectedReplacement = new Object();
        Supplier<Object> replacementFactory = () -> {
            factoryCalls.incrementAndGet();
            return expectedReplacement;
        };

        Object replacement = controller.initialize(true, replacementFactory);

        assertAll(() -> assertSame(expectedReplacement, replacement), () -> assertFalse(controller.shouldRenderVanillaText()), () -> assertEquals(1, factoryCalls.get()));
    }

    @Test
    void missingReplacementFallsBackToVanillaText() {
        WidgetifiedTextReplacementController<Object> controller = new WidgetifiedTextReplacementController<>();

        Object replacement = controller.initialize(true, () -> null);

        assertAll(() -> assertNull(replacement), () -> assertTrue(controller.shouldRenderVanillaText()));
    }

    @Test
    void repeatedInitializationCannotRetainAStaleReplacement() {
        WidgetifiedTextReplacementController<Object> controller = new WidgetifiedTextReplacementController<>();
        controller.initialize(true, Object::new);

        Object replacement = controller.initialize(true, () -> null);

        assertAll(() -> assertNull(replacement), () -> assertTrue(controller.shouldRenderVanillaText()));
    }

    @Test
    void disablingOnReinitializationClearsTheReplacementPath() {
        WidgetifiedTextReplacementController<Object> controller = new WidgetifiedTextReplacementController<>();
        controller.initialize(true, Object::new);

        Object replacement = controller.initialize(false, Object::new);

        assertAll(() -> assertNull(replacement), () -> assertTrue(controller.shouldRenderVanillaText()));
    }

    @Test
    void enablingOnReinitializationReplacesTheVanillaPath() {
        WidgetifiedTextReplacementController<Object> controller = new WidgetifiedTextReplacementController<>();
        controller.initialize(false, Object::new);
        Object expectedReplacement = new Object();

        Object replacement = controller.initialize(true, () -> expectedReplacement);

        assertAll(() -> assertSame(expectedReplacement, replacement), () -> assertFalse(controller.shouldRenderVanillaText()));
    }

}
