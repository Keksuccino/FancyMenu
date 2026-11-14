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

    /** Returns whether the key press is the select-all shortcut, which is Cmd+A on macOS and Ctrl+A on other platforms. */
    public static boolean isSelectAll(int keyCode, int scanCode, int modifiers) {
        return isEditShortcut(keyCode, GLFW.GLFW_KEY_A, modifiers);
    }

    /** Returns whether the key press is the copy shortcut, which is Cmd+C on macOS and Ctrl+C on other platforms. */
    public static boolean isCopy(int keyCode, int scanCode, int modifiers) {
        return isEditShortcut(keyCode, GLFW.GLFW_KEY_C, modifiers);
    }

    /** Returns whether the key press is the paste shortcut, which is Cmd+V on macOS and Ctrl+V on other platforms. */
    public static boolean isPaste(int keyCode, int scanCode, int modifiers) {
        return isEditShortcut(keyCode, GLFW.GLFW_KEY_V, modifiers);
    }

    /** Returns whether the key press is the cut shortcut, which is Cmd+X on macOS and Ctrl+X on other platforms. */
    public static boolean isCut(int keyCode, int scanCode, int modifiers) {
        return isEditShortcut(keyCode, GLFW.GLFW_KEY_X, modifiers);
    }

    /**
     * Checks an edit shortcut the way Minecraft's own checks do: the GUI shortcut modifier has to be down, but neither Shift nor Alt.<br>
     * Minecraft's own checks of this version poll the keyboard instead of reading the modifiers of the key event, which ignores macOS modifier remapping.<br>
     * The scan code is not needed here, but keeps the parameters of all edit shortcut checks the same as the parameters of the key handlers that call them.
     */
    private static boolean isEditShortcut(int keyCode, int shortcutKeyCode, int modifiers) {
        return (keyCode == shortcutKeyCode) && isGuiShortcutModifierDown(modifiers) && ((modifiers & GLFW.GLFW_MOD_SHIFT) == 0) && ((modifiers & GLFW.GLFW_MOD_ALT) == 0);
    }

}
