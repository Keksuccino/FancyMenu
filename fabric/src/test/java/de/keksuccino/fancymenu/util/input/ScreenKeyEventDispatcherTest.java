package de.keksuccino.fancymenu.util.input;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenKeyEventDispatcherTest {

    private static final long ACTIVE_WINDOW = 42L;

    @Test
    void handledAndUnhandledScreenCallsDispatchExactlyOnceOnBothLoaderPaths() {
        for (LoaderPath loaderPath : LoaderPath.values()) {
            for (boolean handled : List.of(false, true)) {
                verifySingleDispatch(loaderPath, handled, InputConstants.PRESS, "pressed");
                verifySingleDispatch(loaderPath, handled, InputConstants.RELEASE, "released");
            }
        }
    }

    @Test
    void pressRepeatAndReleaseUseTheMatchingEventChannel() {
        assertEquals(List.of("pressed"), dispatchAndCollectChannels(InputConstants.PRESS));
        assertEquals(List.of("pressed"), dispatchAndCollectChannels(InputConstants.REPEAT));
        assertEquals(List.of("released"), dispatchAndCollectChannels(InputConstants.RELEASE));
    }

    @Test
    void wrongWindowStillRunsTheScreenCallButDoesNotDispatch() {
        AtomicInteger screenCalls = new AtomicInteger();
        AtomicInteger eventCalls = new AtomicInteger();

        boolean handled = ScreenKeyEventDispatcher.dispatchAfterScreenCall(ACTIVE_WINDOW + 1L, ACTIVE_WINDOW, InputConstants.PRESS, new Object(), new Object(), () -> {
            screenCalls.incrementAndGet();
            return true;
        }, (screen, event) -> eventCalls.incrementAndGet(), (screen, event) -> eventCalls.incrementAndGet());

        assertTrue(handled);
        assertEquals(1, screenCalls.get());
        assertEquals(0, eventCalls.get());
    }

    @Test
    void invalidActionsDoNotDispatchAsKeyEvents() {
        for (int action : List.of(-2, 2, 3, Integer.MAX_VALUE)) {
            AtomicInteger screenCalls = new AtomicInteger();
            AtomicInteger eventCalls = new AtomicInteger();

            boolean handled = ScreenKeyEventDispatcher.dispatchAfterScreenCall(ACTIVE_WINDOW, ACTIVE_WINDOW, action, new Object(), new Object(), () -> {
                screenCalls.incrementAndGet();
                return false;
            }, (screen, event) -> eventCalls.incrementAndGet(), (screen, event) -> eventCalls.incrementAndGet());

            assertFalse(handled);
            assertEquals(1, screenCalls.get());
            assertEquals(0, eventCalls.get());
        }
    }

    @Test
    void screenFailureCannotProduceAnEventThatWasNeverHandled() {
        AtomicInteger eventCalls = new AtomicInteger();

        assertThrows(TestScreenException.class, () -> ScreenKeyEventDispatcher.dispatchAfterScreenCall(ACTIVE_WINDOW, ACTIVE_WINDOW, InputConstants.PRESS, new Object(), new Object(), () -> {
            throw new TestScreenException();
        }, (screen, event) -> eventCalls.incrementAndGet(), (screen, event) -> eventCalls.incrementAndGet()));
        assertEquals(0, eventCalls.get());
    }

    private static void verifySingleDispatch(LoaderPath loaderPath, boolean handled, int action, String expectedChannel) {
        Object screen = new Object();
        Object event = new Object();
        AtomicReference<Object> dispatchedScreen = new AtomicReference<>();
        AtomicReference<Object> dispatchedEvent = new AtomicReference<>();
        AtomicInteger dispatches = new AtomicInteger();
        List<String> order = new ArrayList<>();

        boolean result = ScreenKeyEventDispatcher.dispatchAfterScreenCall(ACTIVE_WINDOW, ACTIVE_WINDOW, action, screen, event, () -> {
            order.add(loaderPath + ":screen");
            return handled;
        }, (actualScreen, actualEvent) -> {
            order.add(loaderPath + ":pressed");
            dispatchedScreen.set(actualScreen);
            dispatchedEvent.set(actualEvent);
            dispatches.incrementAndGet();
        }, (actualScreen, actualEvent) -> {
            order.add(loaderPath + ":released");
            dispatchedScreen.set(actualScreen);
            dispatchedEvent.set(actualEvent);
            dispatches.incrementAndGet();
        });

        assertEquals(handled, result);
        assertEquals(List.of(loaderPath + ":screen", loaderPath + ":" + expectedChannel), order);
        assertEquals(1, dispatches.get());
        assertSame(screen, dispatchedScreen.get());
        assertSame(event, dispatchedEvent.get());
    }

    private static List<String> dispatchAndCollectChannels(int action) {
        List<String> channels = new ArrayList<>();
        ScreenKeyEventDispatcher.dispatchAfterScreenCall(ACTIVE_WINDOW, ACTIVE_WINDOW, action, new Object(), new Object(), () -> false, (screen, event) -> channels.add("pressed"), (screen, event) -> channels.add("released"));
        return channels;
    }

    private enum LoaderPath {
        FABRIC,
        NEOFORGE
    }

    private static final class TestScreenException extends RuntimeException {
    }

}
