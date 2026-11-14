package de.keksuccino.fancymenu.util.input;

import de.keksuccino.fancymenu.util.VanillaEvents;
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

    /**
     * Returns whether the key press is the select-all shortcut, which is Cmd+A on macOS and Ctrl+A on other platforms.<br>
     * All edit shortcut checks use Minecraft's own checks, so they always understand the key codes Minecraft reports.
     * Comparing key codes with fixed numbers instead silently stops working when Minecraft reports keys differently.
     */
    public static boolean isSelectAll(int keyCode, int scanCode, int modifiers) {
        return VanillaEvents.keyEvent(keyCode, scanCode, modifiers).isSelectAll();
    }

    /** Returns whether the key press is the copy shortcut, which is Cmd+C on macOS and Ctrl+C on other platforms. */
    public static boolean isCopy(int keyCode, int scanCode, int modifiers) {
        return VanillaEvents.keyEvent(keyCode, scanCode, modifiers).isCopy();
    }

    /** Returns whether the key press is the paste shortcut, which is Cmd+V on macOS and Ctrl+V on other platforms. */
    public static boolean isPaste(int keyCode, int scanCode, int modifiers) {
        return VanillaEvents.keyEvent(keyCode, scanCode, modifiers).isPaste();
    }

    /** Returns whether the key press is the cut shortcut, which is Cmd+X on macOS and Ctrl+X on other platforms. */
    public static boolean isCut(int keyCode, int scanCode, int modifiers) {
        return VanillaEvents.keyEvent(keyCode, scanCode, modifiers).isCut();
    }

}
