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
     * Updates the semantic modifier state reported with the latest GLFW input event.
     *
     * GLFW key identities cannot be polled for this state on macOS because System Settings can remap physical modifier keys. The event mask already reflects that remapping and is also available while FancyMenu handles drag and character paths without a new event object.
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
     * Returns whether Minecraft's platform-specific GUI/edit shortcut modifier is active.
     * This is Command on macOS and Control on other platforms; it is intentionally distinct from raw Control checks such as macOS Control-click right-click emulation.
     */
    public static boolean isGuiShortcutModifierDown() {
        return isGuiShortcutModifierDown(getActiveModifiers());
    }

    public static boolean isGuiShortcutModifierDown(int modifiers) {
        return isGuiShortcutModifierDown(modifiers, Minecraft.ON_OSX);
    }

    static boolean isGuiShortcutModifierDown(int modifiers, boolean onMacOS) {
        int shortcutModifier = onMacOS ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
        return (modifiers & shortcutModifier) != 0;
    }

}
