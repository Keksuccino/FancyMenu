package de.keksuccino.fancymenu.test;

import net.minecraft.client.gui.screens.Screen;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public final class ScreenTestFactory {

    private static final Unsafe UNSAFE = findUnsafe();

    private ScreenTestFactory() {
    }

    /**
     * Allocates screen identity objects without invoking constructors that require a bootstrapped Minecraft client.
     * Only tests that read the runtime type or explicitly initialized fields may use these instances.
     */
    public static <T extends Screen> T allocateScreen(Class<T> screenClass) {
        try {
            return screenClass.cast(UNSAFE.allocateInstance(screenClass));
        } catch (InstantiationException ex) {
            throw new IllegalArgumentException("Unable to allocate test screen " + screenClass.getName(), ex);
        }
    }

    private static Unsafe findUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

}
