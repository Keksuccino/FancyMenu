package de.keksuccino.fancymenu.util.input;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.events.screen.ScreenKeyReleasedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/** Dispatches FancyMenu screen-key events after the exact screen invocation that handled the input. */
public final class ScreenKeyEventDispatcher {

    private ScreenKeyEventDispatcher() {
    }

    public static boolean dispatchAfterScreenCall(long windowPointer, int action, @NotNull Screen screen, int key, int scanCode, int modifiers, @NotNull BooleanSupplier screenCall) {
        return dispatchAfterScreenCall(windowPointer, Minecraft.getInstance().getWindow().getWindow(), action, screen, new KeyInput(key, scanCode, modifiers), screenCall, ScreenKeyEventDispatcher::postKeyPressed, ScreenKeyEventDispatcher::postKeyReleased);
    }

    static <S, E> boolean dispatchAfterScreenCall(long windowPointer, long activeWindowPointer, int action, @NotNull S screen, @NotNull E event, @NotNull BooleanSupplier screenCall, @NotNull BiConsumer<S, E> keyPressedConsumer, @NotNull BiConsumer<S, E> keyReleasedConsumer) {
        Objects.requireNonNull(screen);
        Objects.requireNonNull(event);
        Objects.requireNonNull(screenCall);
        Objects.requireNonNull(keyPressedConsumer);
        Objects.requireNonNull(keyReleasedConsumer);

        boolean shouldDispatch = windowPointer == activeWindowPointer && isKeyAction(action);
        boolean handled = screenCall.getAsBoolean();
        if (!shouldDispatch) return handled;

        if (action == GLFW.GLFW_RELEASE) {
            keyReleasedConsumer.accept(screen, event);
        } else {
            keyPressedConsumer.accept(screen, event);
        }
        return handled;
    }

    private static boolean isKeyAction(int action) {
        return action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE || action == GLFW.GLFW_REPEAT;
    }

    private static void postKeyPressed(@NotNull Screen screen, @NotNull KeyInput input) {
        EventHandler.INSTANCE.postEvent(new ScreenKeyPressedEvent(screen, input.key(), input.scanCode(), input.modifiers()));

        if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay overlay) {
            overlay.keyPressed(input.key(), input.scanCode(), input.modifiers());
        }
    }

    private static void postKeyReleased(@NotNull Screen screen, @NotNull KeyInput input) {
        EventHandler.INSTANCE.postEvent(new ScreenKeyReleasedEvent(screen, input.key(), input.scanCode(), input.modifiers()));
    }

    private record KeyInput(int key, int scanCode, int modifiers) {
    }

}
