package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuBackgroundLifecycleControllerTest {

    @Test
    void seamlessReplacementHasPrecedenceAndUsesFullScreenBounds() {
        MenuBackgroundLifecycleController controller = new MenuBackgroundLifecycleController();
        List<Bounds> calls = new ArrayList<>();
        AtomicInteger globalCalls = new AtomicInteger();
        AtomicInteger fallbackCalls = new AtomicInteger();

        controller.renderLifecycle(1277, 694, (x, y, width, height) -> { calls.add(new Bounds(x, y, width, height)); return true; }, (x, y, width, height) -> { globalCalls.incrementAndGet(); return true; }, fallbackCalls::incrementAndGet);

        assertEquals(List.of(new Bounds(0, 0, 1277, 694)), calls);
        assertEquals(0, globalCalls.get());
        assertEquals(0, fallbackCalls.get());
    }

    @Test
    void globalReplacementSkipsBoundedSubclassFallback() {
        MenuBackgroundLifecycleController controller = new MenuBackgroundLifecycleController();
        List<Bounds> globalBounds = new ArrayList<>();
        AtomicInteger boundedFallbackCalls = new AtomicInteger();

        controller.renderLifecycle(1277, 694, (x, y, width, height) -> false, (x, y, width, height) -> { globalBounds.add(new Bounds(x, y, width, height)); return true; }, boundedFallbackCalls::incrementAndGet);

        assertEquals(List.of(new Bounds(0, 0, 1277, 694)), globalBounds);
        assertEquals(0, boundedFallbackCalls.get());
    }

    @Test
    void failedReplacementRunsFallbackOnceAndSuppressesNestedStaticHelper() {
        MenuBackgroundLifecycleController controller = new MenuBackgroundLifecycleController();
        AtomicInteger fallbackCalls = new AtomicInteger();
        AtomicInteger boundedCalls = new AtomicInteger();

        controller.renderLifecycle(320, 180, (x, y, width, height) -> false, (x, y, width, height) -> false, () -> {
            fallbackCalls.incrementAndGet();
            assertFalse(MenuBackgroundLifecycleController.isStaticHelperReplacementAllowed());
            controller.renderNestedBoundedCall(() -> {
                boundedCalls.incrementAndGet();
                assertFalse(MenuBackgroundLifecycleController.isStaticHelperReplacementAllowed());
            });
        });

        assertEquals(1, fallbackCalls.get());
        assertEquals(1, boundedCalls.get());
        assertTrue(MenuBackgroundLifecycleController.isStaticHelperReplacementAllowed());
    }

    @Test
    void directOneArgumentCallCanRenderFullScreenReplacement() {
        MenuBackgroundLifecycleController controller = new MenuBackgroundLifecycleController();
        List<Bounds> globalBounds = new ArrayList<>();

        boolean replaced = controller.renderDirectOneArgumentCall(854, 480, (x, y, width, height) -> false, (x, y, width, height) -> { globalBounds.add(new Bounds(x, y, width, height)); return true; });

        assertTrue(replaced);
        assertEquals(List.of(new Bounds(0, 0, 854, 480)), globalBounds);
    }

    @Test
    void failedDirectOneArgumentCallSuppressesItsNestedBoundedFallback() {
        MenuBackgroundLifecycleController controller = new MenuBackgroundLifecycleController();

        assertFalse(controller.renderDirectOneArgumentCall(320, 180, (x, y, width, height) -> false, (x, y, width, height) -> false));
        controller.renderNestedBoundedCall(() -> assertFalse(MenuBackgroundLifecycleController.isStaticHelperReplacementAllowed()));
        assertTrue(MenuBackgroundLifecycleController.isStaticHelperReplacementAllowed());
    }

    @Test
    void nestedScreenCanReplaceWhileOuterFallbackKeepsStaticHelperSuppressed() {
        MenuBackgroundLifecycleController outerController = new MenuBackgroundLifecycleController();
        MenuBackgroundLifecycleController nestedController = new MenuBackgroundLifecycleController();
        List<Bounds> nestedBounds = new ArrayList<>();

        outerController.renderLifecycle(320, 180, (x, y, width, height) -> false, (x, y, width, height) -> false, () -> {
            assertFalse(MenuBackgroundLifecycleController.isStaticHelperReplacementAllowed());
            nestedController.renderLifecycle(160, 90, (x, y, width, height) -> { assertFalse(MenuBackgroundLifecycleController.isStaticHelperReplacementAllowed()); nestedBounds.add(new Bounds(x, y, width, height)); return true; }, (x, y, width, height) -> false, () -> {});
            assertFalse(MenuBackgroundLifecycleController.isStaticHelperReplacementAllowed());
        });

        assertEquals(List.of(new Bounds(0, 0, 160, 90)), nestedBounds);
        assertTrue(MenuBackgroundLifecycleController.isStaticHelperReplacementAllowed());
    }

    @Test
    void reentrantExceptionRestoresBothDepthsForSubsequentCall() {
        MenuBackgroundLifecycleController controller = new MenuBackgroundLifecycleController();
        AtomicInteger fallbackCalls = new AtomicInteger();
        MenuBackgroundLifecycleController.FullScreenRenderer unavailable = (x, y, width, height) -> false;

        assertThrows(IllegalStateException.class, () -> controller.renderLifecycle(320, 180, unavailable, unavailable, () -> {
            fallbackCalls.incrementAndGet();
            controller.renderLifecycle(320, 180, unavailable, unavailable, () -> {
                fallbackCalls.incrementAndGet();
                assertFalse(controller.renderDirectOneArgumentCall(320, 180, (x, y, width, height) -> true, unavailable));
                assertFalse(MenuBackgroundLifecycleController.isStaticHelperReplacementAllowed());
                throw new IllegalStateException("nested fallback failed");
            });
        }));

        assertEquals(2, fallbackCalls.get());
        assertTrue(MenuBackgroundLifecycleController.isStaticHelperReplacementAllowed());
        assertTrue(controller.renderDirectOneArgumentCall(320, 180, (x, y, width, height) -> true, unavailable));
    }

    private record Bounds(int x, int y, int width, int height) {}
}
