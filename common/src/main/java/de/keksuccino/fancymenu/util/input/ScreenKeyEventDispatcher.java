package de.keksuccino.fancymenu.util.input;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.events.screen.ScreenKeyReleasedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/** Dispatches FancyMenu screen-key events after the exact screen invocation that handled the input. */
public final class ScreenKeyEventDispatcher {

    private ScreenKeyEventDispatcher() {
    }

    public static boolean dispatchPressedAfterScreenCall(@NotNull Screen screen, int key, int scanCode, int modifiers, @NotNull BooleanSupplier screenCall) {
        return dispatchAfterScreenCall(screen, new KeyInput(key, scanCode, modifiers), screenCall, ScreenKeyEventDispatcher::postKeyPressed);
    }

    public static boolean dispatchReleasedAfterScreenCall(@NotNull Screen screen, int key, int scanCode, int modifiers, @NotNull BooleanSupplier screenCall) {
        return dispatchAfterScreenCall(screen, new KeyInput(key, scanCode, modifiers), screenCall, ScreenKeyEventDispatcher::postKeyReleased);
    }

    static <S, E> boolean dispatchAfterScreenCall(@NotNull S screen, @NotNull E event, @NotNull BooleanSupplier screenCall, @NotNull BiConsumer<S, E> eventConsumer) {
        Objects.requireNonNull(screen);
        Objects.requireNonNull(event);
        Objects.requireNonNull(screenCall);
        Objects.requireNonNull(eventConsumer);

        boolean handled = screenCall.getAsBoolean();
        eventConsumer.accept(screen, event);
        return handled;
    }

    public static void postKeyPressed(@NotNull Screen screen, int key, int scanCode, int modifiers) {
        postKeyPressed(screen, new KeyInput(key, scanCode, modifiers));
    }

    public static void postKeyReleased(@NotNull Screen screen, int key, int scanCode, int modifiers) {
        postKeyReleased(screen, new KeyInput(key, scanCode, modifiers));
    }

    private static void postKeyPressed(@NotNull Screen screen, @NotNull KeyInput event) {
        EventHandler.INSTANCE.postEvent(new ScreenKeyPressedEvent(screen, event.key(), event.scanCode(), event.modifiers()));
        if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay overlay) overlay.keyPressed(event.key(), event.scanCode(), event.modifiers());
    }

    private static void postKeyReleased(@NotNull Screen screen, @NotNull KeyInput event) {
        EventHandler.INSTANCE.postEvent(new ScreenKeyReleasedEvent(screen, event.key(), event.scanCode(), event.modifiers()));
    }

    private record KeyInput(int key, int scanCode, int modifiers) {
    }
}
