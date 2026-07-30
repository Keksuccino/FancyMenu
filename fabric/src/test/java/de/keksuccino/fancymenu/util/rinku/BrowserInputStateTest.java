package de.keksuccino.fancymenu.util.rinku;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserInputStateTest {

    @Test
    void acceptedPressAndOwnedReleaseForwardExactlyOnceAndReturnConsumed() {
        BrowserInputState state = new BrowserInputState();
        AtomicInteger pressCalls = new AtomicInteger();
        AtomicInteger releaseCalls = new AtomicInteger();

        assertTrue(state.forwardMousePress(true, true, 0, pressCalls::incrementAndGet));
        assertEquals(1, pressCalls.get());
        assertTrue(state.isFocused());
        assertTrue(state.hasMouseButtonCapture(0));

        assertTrue(state.forwardMouseRelease(0, releaseCalls::incrementAndGet));
        assertEquals(1, releaseCalls.get());
        assertFalse(state.hasMouseButtonCapture(0));
        assertFalse(state.forwardMouseRelease(0, releaseCalls::incrementAndGet));
        assertEquals(1, releaseCalls.get());
    }

    @Test
    void rejectedPressClearsFocusWithoutForwardingOrClaimingRelease() {
        BrowserInputState state = new BrowserInputState();
        AtomicInteger calls = new AtomicInteger();
        state.setFocused(true);

        assertFalse(state.forwardMousePress(true, false, 1, calls::incrementAndGet));
        assertFalse(state.isFocused());
        assertFalse(state.hasMouseButtonCapture(1));
        assertFalse(state.forwardMouseRelease(1, calls::incrementAndGet));
        assertFalse(state.forwardMousePress(false, true, 1, calls::incrementAndGet));
        assertEquals(0, calls.get());
    }

    @Test
    void rejectedRepeatedPressClearsStaleCaptureForTheSameButton() {
        BrowserInputState state = new BrowserInputState();
        AtomicInteger calls = new AtomicInteger();
        state.forwardMousePress(true, true, 1, calls::incrementAndGet);

        assertFalse(state.forwardMousePress(true, false, 1, calls::incrementAndGet));

        assertEquals(1, calls.get());
        assertFalse(state.isFocused());
        assertFalse(state.hasMouseButtonCapture(1));
        assertFalse(state.forwardMouseRelease(1, calls::incrementAndGet));
        assertEquals(1, calls.get());
    }

    @Test
    void capturedButtonsRemainIndependentAcrossFocusLoss() {
        BrowserInputState state = new BrowserInputState();
        state.forwardMousePress(true, true, 0, () -> { });
        state.forwardMousePress(true, true, 2, () -> { });
        state.setFocused(false);

        assertTrue(state.forwardMouseRelease(2, () -> { }));
        assertTrue(state.hasMouseButtonCapture(0));
        assertTrue(state.forwardMouseRelease(0, () -> { }));
    }

    @Test
    void scrollForwardsOnlyWhenInteractableAndHovered() {
        BrowserInputState state = new BrowserInputState();
        AtomicInteger calls = new AtomicInteger();

        assertTrue(state.forwardMouseScroll(true, true, calls::incrementAndGet));
        assertFalse(state.forwardMouseScroll(true, false, calls::incrementAndGet));
        assertFalse(state.forwardMouseScroll(false, true, calls::incrementAndGet));
        assertEquals(1, calls.get());
    }

    @Test
    void keyboardForwardsOnlyWhileInteractableAndFocused() {
        BrowserInputState state = new BrowserInputState();
        AtomicInteger calls = new AtomicInteger();

        assertFalse(state.forwardKeyboardInput(true, calls::incrementAndGet));
        state.setFocused(true);
        assertTrue(state.forwardKeyboardInput(true, calls::incrementAndGet));
        assertFalse(state.forwardKeyboardInput(false, calls::incrementAndGet));
        state.setFocused(false);
        assertFalse(state.forwardKeyboardInput(true, calls::incrementAndGet));
        assertEquals(1, calls.get());
    }

    @Test
    void characterInputConsumesNulWithoutForwardingAndRejectsUnfocusedInput() {
        BrowserInputState state = new BrowserInputState();
        AtomicInteger calls = new AtomicInteger();
        state.setFocused(true);

        assertTrue(state.forwardCharacterInput(true, 'a', calls::incrementAndGet));
        assertTrue(state.forwardCharacterInput(true, (char)0, calls::incrementAndGet));
        assertFalse(state.forwardCharacterInput(false, 'b', calls::incrementAndGet));
        state.setFocused(false);
        assertFalse(state.forwardCharacterInput(true, 'c', calls::incrementAndGet));
        assertEquals(1, calls.get());
    }

    @Test
    void resetClearsFocusAndEveryCapturedButton() {
        BrowserInputState state = new BrowserInputState();
        state.forwardMousePress(true, true, 0, () -> { });
        state.forwardMousePress(true, true, 1, () -> { });

        state.reset();

        assertFalse(state.isFocused());
        assertFalse(state.hasMouseButtonCapture(0));
        assertFalse(state.hasMouseButtonCapture(1));
    }

}
