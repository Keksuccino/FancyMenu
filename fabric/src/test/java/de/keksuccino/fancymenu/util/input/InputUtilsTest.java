package de.keksuccino.fancymenu.util.input;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputUtilsTest {

    @AfterEach
    void resetActiveModifiers() {
        InputUtils.resetActiveModifiers();
    }

    @Test
    void usesSuperAsGuiShortcutModifierOnMacOS() {
        assertTrue(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_SUPER, true));
        assertTrue(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_SHIFT, true));
        assertFalse(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_CONTROL, true));
        assertFalse(InputUtils.isGuiShortcutModifierDown(0, true));
    }

    @Test
    void usesControlAsGuiShortcutModifierOnOtherPlatforms() {
        assertTrue(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_CONTROL, false));
        assertTrue(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT, false));
        assertFalse(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_SUPER, false));
        assertFalse(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_SHIFT, false));
    }

    @Test
    void replacesAndResetsCachedModifierState() {
        int shortcutModifiersOnEveryPlatform = GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT;
        InputUtils.updateActiveModifiers(shortcutModifiersOnEveryPlatform);
        assertEquals(shortcutModifiersOnEveryPlatform, InputUtils.getActiveModifiers());
        assertTrue(InputUtils.isGuiShortcutModifierDown());

        InputUtils.updateActiveModifiers(GLFW.GLFW_MOD_ALT);
        assertEquals(GLFW.GLFW_MOD_ALT, InputUtils.getActiveModifiers());
        assertFalse(InputUtils.isGuiShortcutModifierDown());

        InputUtils.resetActiveModifiers();
        assertEquals(0, InputUtils.getActiveModifiers());
        assertFalse(InputUtils.isGuiShortcutModifierDown());
    }

}
