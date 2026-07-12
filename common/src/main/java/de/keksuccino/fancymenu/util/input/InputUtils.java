package de.keksuccino.fancymenu.util.input;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class InputUtils {

    private static volatile int activeModifiers;

    @NotNull
    public static String getKeyName(int keycode, int scancode) {
        String key = GLFW.glfwGetKeyName(keycode, scancode);
        if (key == null) key = "";
        return key;
    }

    /**
     * Returns the semantic GUI shortcut modifier mask for the supplied platform.
     *
     * The platform is an argument so the mapping stays deterministic and testable without depending on the host running Minecraft.
     */
    public static int getGuiShortcutModifierMask(boolean macOs) {
        return macOs ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
    }

    /**
     * Updates the semantic modifier state reported with the latest GLFW key, character, or mouse-button event.
     *
     * GLFW key identities cannot be polled for this state on macOS because System Settings can remap physical modifier keys. The event mask already reflects that remapping and remains available to drag paths that do not receive a modifier mask.
     */
    public static void updateActiveModifiers(int modifiers) {
        activeModifiers = modifiers;
    }

    public static void resetActiveModifiers() {
        activeModifiers = 0;
    }

    public static int getActiveModifiers() {
        return activeModifiers;
    }

    /**
     * Clears cached event state when the game window loses focus, preventing a modifier held during focus loss from remaining logically pressed.
     */
    public static void onWindowFocusChanged(boolean focused) {
        if (!focused) resetActiveModifiers();
    }

    /**
     * Returns whether Minecraft's platform-specific GUI shortcut modifier is active in the latest cached event state.
     * This is Command on macOS and Control on other platforms; it is intentionally distinct from raw Control checks such as macOS Control-click right-click emulation.
     */
    public static boolean isGuiShortcutModifierDown() {
        return isGuiShortcutModifierDown(getActiveModifiers());
    }

    public static boolean isGuiShortcutModifierDown(int modifiers) {
        return isGuiShortcutModifierDown(modifiers, Minecraft.ON_OSX);
    }

    public static boolean isGuiShortcutModifierDown(int modifiers, boolean macOs) {
        return (modifiers & getGuiShortcutModifierMask(macOs)) != 0;
    }

}
