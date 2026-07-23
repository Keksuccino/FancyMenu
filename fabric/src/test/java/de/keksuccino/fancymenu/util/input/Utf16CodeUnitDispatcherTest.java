package de.keksuccino.fancymenu.util.input;

import net.minecraft.client.input.CharacterEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Utf16CodeUnitDispatcherTest {

    private static final int MODIFIERS = 0x15;

    @Test
    void bmpCodePointMapsToOneCharacterEventWithUnchangedModifiers() {
        List<ForwardedEvent> dispatched = new ArrayList<>();

        boolean handled = Utf16CodeUnitDispatcher.dispatch('é', MODIFIERS, (codeUnit, modifiers) -> {
            dispatched.add(new ForwardedEvent(new CharacterEvent(codeUnit), modifiers));
            return false;
        });

        assertFalse(handled);
        assertEquals(List.of(new ForwardedEvent(new CharacterEvent('é'), MODIFIERS)), dispatched);
    }

    @Test
    void bmpCharacterEventOverloadReusesTheOriginalEvent() {
        CharacterEvent input = new CharacterEvent('é');
        List<CharacterEvent> dispatched = new ArrayList<>();

        Utf16CodeUnitDispatcher.dispatch(input, event -> {
            dispatched.add(event);
            return false;
        });

        assertEquals(1, dispatched.size());
        assertSame(input, dispatched.getFirst());
    }

    @Test
    void supplementaryCharacterEventMapsToExactlyOneOrderedSurrogatePair() {
        List<CharacterEvent> dispatched = new ArrayList<>();

        Utf16CodeUnitDispatcher.dispatch(new CharacterEvent(0x1F600), event -> {
            dispatched.add(event);
            return false;
        });

        assertEquals(List.of(new CharacterEvent('\uD83D'), new CharacterEvent('\uDE00')), dispatched);
    }

    @Test
    void everySurrogateUnitIsDispatchedWhenAnEarlierUnitWasHandled() {
        List<Character> dispatched = new ArrayList<>();

        boolean handled = Utf16CodeUnitDispatcher.dispatch(new CharacterEvent(0x1F600), event -> {
            dispatched.add((char)event.codepoint());
            return event.codepoint() == '\uD83D';
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

    private record ForwardedEvent(CharacterEvent event, int modifiers) {
    }

}
