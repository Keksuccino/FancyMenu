package de.keksuccino.fancymenu.util.input;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScreenKeyEventDispatcherTest {

    @Test
    void handledAndUnhandledScreenCallsDispatchExactlyOnceOnBothLoaderPaths() {
        for (LoaderPath loaderPath : LoaderPath.values()) {
            for (boolean handled : List.of(false, true)) verifySingleDispatch(loaderPath, handled);
        }
    }

    @Test
    void screenFailureCannotProduceAnEventThatWasNeverHandled() {
        AtomicInteger eventCalls = new AtomicInteger();

        assertThrows(TestScreenException.class, () -> ScreenKeyEventDispatcher.dispatchAfterScreenCall(new Object(), new Object(), () -> {
            throw new TestScreenException();
        }, (screen, event) -> eventCalls.incrementAndGet()));
        assertEquals(0, eventCalls.get());
    }

    @Test
    void nullDependenciesAreRejectedBeforeTheScreenCall() {
        AtomicInteger screenCalls = new AtomicInteger();

        assertThrows(NullPointerException.class, () -> ScreenKeyEventDispatcher.dispatchAfterScreenCall(null, new Object(), () -> {
            screenCalls.incrementAndGet();
            return false;
        }, (screen, event) -> {}));
        assertThrows(NullPointerException.class, () -> ScreenKeyEventDispatcher.dispatchAfterScreenCall(new Object(), null, () -> {
            screenCalls.incrementAndGet();
            return false;
        }, (screen, event) -> {}));
        assertThrows(NullPointerException.class, () -> ScreenKeyEventDispatcher.dispatchAfterScreenCall(new Object(), new Object(), null, (screen, event) -> {}));
        assertThrows(NullPointerException.class, () -> ScreenKeyEventDispatcher.dispatchAfterScreenCall(new Object(), new Object(), () -> {
            screenCalls.incrementAndGet();
            return false;
        }, null));
        assertEquals(0, screenCalls.get());
    }

    private static void verifySingleDispatch(LoaderPath loaderPath, boolean handled) {
        Object screen = new Object();
        Object event = new Object();
        AtomicReference<Object> dispatchedScreen = new AtomicReference<>();
        AtomicReference<Object> dispatchedEvent = new AtomicReference<>();
        AtomicInteger dispatches = new AtomicInteger();
        List<String> order = new ArrayList<>();

        boolean result = ScreenKeyEventDispatcher.dispatchAfterScreenCall(screen, event, () -> {
            order.add(loaderPath + ":screen");
            return handled;
        }, (actualScreen, actualEvent) -> {
            order.add(loaderPath + ":event");
            dispatchedScreen.set(actualScreen);
            dispatchedEvent.set(actualEvent);
            dispatches.incrementAndGet();
        });

        assertEquals(handled, result);
        assertEquals(List.of(loaderPath + ":screen", loaderPath + ":event"), order);
        assertEquals(1, dispatches.get());
        assertSame(screen, dispatchedScreen.get());
        assertSame(event, dispatchedEvent.get());
    }

    private enum LoaderPath {
        FABRIC,
        FORGE
    }

    private static final class TestScreenException extends RuntimeException {
    }
}
