package de.keksuccino.fancymenu.util.input;

import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class InputUtils {

    private static final int GUI_SHORTCUT_MODIFIER = getGuiShortcutModifier(Util.getPlatform() == Util.OS.OSX);

    private static volatile int activeModifiers;

    @NotNull
    public static String getKeyName(int keycode, int scancode) {
        String key = GLFW.glfwGetKeyName(keycode, scancode);
        if (key == null) key = "";
        return key;
    }

    /**
     * Stores the semantic modifier mask reported by the latest GLFW input event.
     *
     * Polling physical modifier key identities is not reliable on macOS because System Settings can remap them. Mouse drag and nested widget callbacks in Minecraft 1.21.1 do not carry their originating modifier mask, so they use this cache instead.
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

    static int getGuiShortcutModifier(boolean macOS) {
        return macOS ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
    }

    public static boolean isGuiShortcutModifierDown() {
        return isGuiShortcutModifierDown(getActiveModifiers());
    }

    public static boolean isGuiShortcutModifierDown(int modifiers) {
        return (modifiers & GUI_SHORTCUT_MODIFIER) != 0;
    }

    static boolean isGuiShortcutModifierDown(int modifiers, boolean macOS) {
        return (modifiers & getGuiShortcutModifier(macOS)) != 0;
    }

    public static boolean isSelectAll(int keyCode, int modifiers) {
        return isEditShortcut(keyCode, GLFW.GLFW_KEY_A, modifiers);
    }

    public static boolean isCopy(int keyCode, int modifiers) {
        return isEditShortcut(keyCode, GLFW.GLFW_KEY_C, modifiers);
    }

    public static boolean isPaste(int keyCode, int modifiers) {
        return isEditShortcut(keyCode, GLFW.GLFW_KEY_V, modifiers);
    }

    public static boolean isCut(int keyCode, int modifiers) {
        return isEditShortcut(keyCode, GLFW.GLFW_KEY_X, modifiers);
    }

    private static boolean isEditShortcut(int keyCode, int expectedKeyCode, int modifiers) {
        return (keyCode == expectedKeyCode) && isGuiShortcutModifierDown(modifiers) && ((modifiers & GLFW.GLFW_MOD_SHIFT) == 0) && ((modifiers & GLFW.GLFW_MOD_ALT) == 0);
    }

}
