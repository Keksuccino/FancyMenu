package de.keksuccino.fancymenu.util.input;

import net.minecraft.client.input.InputQuirks;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDLMouse;
import java.util.Locale;
import net.minecraft.client.input.InputWithModifiers;
import org.jetbrains.annotations.NotNull;

public class InputUtils {

    private static volatile int activeModifiers;

    @NotNull
    public static String getKeyName(int scancode, int keycode) {
        String key = SDLKeyboard.SDL_GetKeyName(keycode != SDLKeycode.SDLK_UNKNOWN ? keycode : SDLKeyboard.SDL_GetKeyFromScancode(scancode, (short) 0, false));
        if (key == null) key = "";
        return key.toLowerCase(Locale.ROOT);
    }

    public static boolean isMouseButtonDown(int button) {
        // SDL buttons are one-based; the state is a bit mask, not a GLFW-style per-button value.
        return button > 0 && button <= 32 && (SDLMouse.SDL_GetMouseState((java.nio.FloatBuffer) null, (java.nio.FloatBuffer) null) & (1 << (button - 1))) != 0;
    }

    /**
     * Updates the semantic modifier state reported with the latest SDL input event.
     *
     * SDL key identities cannot be polled for this state on macOS because System Settings can remap physical modifier keys. The event mask already reflects that remapping and is also available while FancyMenu handles drag and repeat paths without a new event object.
     */
    public static void updateActiveModifiers(@InputWithModifiers.Modifiers int modifiers) {
        activeModifiers = modifiers;
    }

    public static void resetActiveModifiers() {
        activeModifiers = 0;
    }

    @InputWithModifiers.Modifiers
    public static int getActiveModifiers() {
        return activeModifiers;
    }

    /**
     * Returns whether Minecraft's platform-specific GUI/edit shortcut modifier is active.
     * This is Command on macOS and Control on other platforms; it is intentionally distinct from raw Control checks such as macOS Control-click right-click emulation.
     */
    public static boolean isGuiShortcutModifierDown() {
        return isGuiShortcutModifierDown(getActiveModifiers());
    }

    public static boolean isGuiShortcutModifierDown(@InputWithModifiers.Modifiers int modifiers) {
        return (modifiers & InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER) != 0;
    }

}
