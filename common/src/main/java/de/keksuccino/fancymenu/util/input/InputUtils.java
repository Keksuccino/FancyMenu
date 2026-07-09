package de.keksuccino.fancymenu.util.input;

import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.InputWithModifiers;
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
     * Updates the semantic modifier state reported with the latest GLFW input event.
     *
     * GLFW key identities cannot be polled for this state on macOS because System Settings can remap physical modifier keys. The event mask already reflects that remapping and is also available while FancyMenu handles drag and repeat paths without a new event object.
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
