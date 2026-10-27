package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
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

    @Test
    void getScreenReturnsNullBeforeMinecraftGuiIsAssigned() throws ReflectiveOperationException {
        Minecraft startingMinecraft = allocateWithoutConstructor(Minecraft.class);

        withMinecraftInstance(startingMinecraft, () -> assertNull(ScreenUtils.getScreen()));
    }

    @Test
    void getScreenReturnsCurrentGuiScreen() throws ReflectiveOperationException {
        Screen screen = allocateWithoutConstructor(GenericMessageScreen.class);
        Gui gui = allocateWithoutConstructor(Gui.class);
        setField(Gui.class, "screen", gui, screen);
        Minecraft minecraft = allocateWithoutConstructor(Minecraft.class);
        setField(Minecraft.class, "gui", minecraft, gui);

        withMinecraftInstance(minecraft, () -> assertSame(screen, ScreenUtils.getScreen()));
    }

    /**
     * Skips the constructor, so final fields like {@link Minecraft#gui} stay null, which matches the state of a
     * {@link Minecraft} instance whose constructor is still running, without starting any game systems.
     */
    @SuppressWarnings("unchecked")
    private static <T> T allocateWithoutConstructor(Class<T> type) throws ReflectiveOperationException {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (T) ((Unsafe) unsafeField.get(null)).allocateInstance(type);
    }

    private static void setField(Class<?> owner, String name, Object target, Object value) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void withMinecraftInstance(Minecraft minecraft, Runnable action) throws ReflectiveOperationException {
        Field instanceField = Minecraft.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        Object previousInstance = instanceField.get(null);
        instanceField.set(null, minecraft);
        try {
            action.run();
        } finally {
            instanceField.set(null, previousInstance);
        }
    }

}
