package de.keksuccino.fancymenu.util.input;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Utf16CodeUnitDispatcherTest {

    private static final int MODIFIERS = 0x15;

    @Test
    void bmpCodePointMapsToOneCharacterEventWithUnchangedModifiers() {
        List<ForwardedEvent> dispatched = new ArrayList<>();

        boolean handled = Utf16CodeUnitDispatcher.dispatch('é', MODIFIERS, (codeUnit, modifiers) -> {
            dispatched.add(new ForwardedEvent(codeUnit, modifiers));
            return false;
        });

        assertFalse(handled);
        assertEquals(List.of(new ForwardedEvent('é', MODIFIERS)), dispatched);
    }

    @Test
    void supplementaryCharacterEventMapsToExactlyOneOrderedSurrogatePair() {
        List<Character> dispatched = new ArrayList<>();

        Utf16CodeUnitDispatcher.dispatch(0x1F600, MODIFIERS, (codeUnit, modifiers) -> {
            dispatched.add(codeUnit);
            return false;
        });

        assertEquals(List.of('\uD83D', '\uDE00'), dispatched);
    }

    @Test
    void everySurrogateUnitIsDispatchedWhenAnEarlierUnitWasHandled() {
        List<Character> dispatched = new ArrayList<>();

        boolean handled = Utf16CodeUnitDispatcher.dispatch(0x1F600, MODIFIERS, (codeUnit, modifiers) -> {
            dispatched.add(codeUnit);
            return codeUnit == '\uD83D';
        });

        assertTrue(handled);
        assertEquals(List.of('\uD83D', '\uDE00'), dispatched);
    }

    @Test
    void isolatedSurrogateUnitIsPreserved() {
        List<Character> dispatched = new ArrayList<>();

        Utf16CodeUnitDispatcher.dispatch('\uD83D', MODIFIERS, (codeUnit, modifiers) -> {
            dispatched.add(codeUnit);
            assertEquals(MODIFIERS, modifiers);
            return false;
        });

        assertEquals(List.of('\uD83D'), dispatched);
    }

    @Test
    void outOfRangeCodePointsAreRejectedBeforeDispatch() {
        AtomicInteger dispatches = new AtomicInteger();
        Utf16CodeUnitDispatcher.Handler handler = (codeUnit, modifiers) -> {
            dispatches.incrementAndGet();
            return false;
        };

        assertThrows(IllegalArgumentException.class, () -> Utf16CodeUnitDispatcher.dispatch(-1, MODIFIERS, handler));
        assertThrows(IllegalArgumentException.class, () -> Utf16CodeUnitDispatcher.dispatch(Character.MAX_CODE_POINT + 1, MODIFIERS, handler));
        assertEquals(0, dispatches.get());
    }

    private record ForwardedEvent(char codeUnit, int modifiers) {
    }

}
