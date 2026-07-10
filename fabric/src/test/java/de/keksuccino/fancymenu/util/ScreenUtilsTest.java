package de.keksuccino.fancymenu.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScreenUtilsTest {

    @Test
    void successfulTransitionKeepsTargetScreen() {
        AtomicReference<Object> current = new AtomicReference<>(new Object());
        Object target = new Object();

        ScreenUtils.setScreenWithRollback(target, current::get, current::set);

        assertSame(target, current.get());
    }

    @Test
    void failedTransitionRestoresPreviousScreen() {
        Object previous = new Object();
        Object target = new Object();
        AtomicReference<Object> current = new AtomicReference<>(previous);
        List<Object> setterCalls = new ArrayList<>();
        IllegalStateException openingFailure = new IllegalStateException("opening failed");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ScreenUtils.setScreenWithRollback(target, current::get, screen -> {
            setterCalls.add(screen);
            current.set(screen);
            if (screen == target) throw openingFailure;
        }));

        assertSame(openingFailure, thrown);
        assertSame(previous, current.get());
        assertEquals(List.of(target, previous), setterCalls);
    }

    @Test
    void failedTransitionRestoresNullState() {
        Object target = new Object();
        AtomicReference<Object> current = new AtomicReference<>();
        List<Object> setterCalls = new ArrayList<>();

        assertThrows(IllegalStateException.class, () -> ScreenUtils.setScreenWithRollback(target, current::get, screen -> {
            setterCalls.add(screen);
            current.set(screen);
            if (screen == target) throw new IllegalStateException("opening failed");
        }));

        assertNull(current.get());
        assertEquals(2, setterCalls.size());
        assertSame(target, setterCalls.get(0));
        assertNull(setterCalls.get(1));
    }

    @Test
    void rollbackFailureIsSuppressedOnOriginalFailure() {
        Object previous = new Object();
        Object target = new Object();
        AtomicReference<Object> current = new AtomicReference<>(previous);
        IllegalStateException openingFailure = new IllegalStateException("opening failed");
        IllegalArgumentException rollbackFailure = new IllegalArgumentException("rollback failed");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ScreenUtils.setScreenWithRollback(target, current::get, screen -> {
            current.set(screen);
            if (screen == target) throw openingFailure;
            if (screen == previous) throw rollbackFailure;
        }));

        assertSame(openingFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(rollbackFailure, thrown.getSuppressed()[0]);
    }

}
