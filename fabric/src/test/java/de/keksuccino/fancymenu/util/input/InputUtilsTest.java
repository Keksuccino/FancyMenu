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
    void returnsPlatformSpecificGuiShortcutModifierMask() {
        assertEquals(GLFW.GLFW_MOD_SUPER, InputUtils.getGuiShortcutModifierMask(true));
        assertEquals(GLFW.GLFW_MOD_CONTROL, InputUtils.getGuiShortcutModifierMask(false));
    }

    @Test
    void detectsGuiShortcutModifierWithAdditionalModifierBits() {
        assertTrue(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT, true));
        assertTrue(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT, false));
    }

    @Test
    void rejectsNonShortcutModifierForEachPlatform() {
        assertFalse(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_CONTROL, true));
        assertFalse(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_SUPER, false));
        assertFalse(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT, true));
        assertFalse(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT, false));
    }

    @Test
    void updatesAndResetsActiveModifierCache() {
        int modifiers = GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_SHIFT;

        InputUtils.updateActiveModifiers(modifiers);

        assertEquals(modifiers, InputUtils.getActiveModifiers());
        InputUtils.resetActiveModifiers();
        assertEquals(0, InputUtils.getActiveModifiers());
    }

    @Test
    void focusGainPreservesCacheAndFocusLossClearsIt() {
        int modifiers = GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT;
        InputUtils.updateActiveModifiers(modifiers);

        InputUtils.onWindowFocusChanged(true);
        assertEquals(modifiers, InputUtils.getActiveModifiers());

        InputUtils.onWindowFocusChanged(false);
        assertEquals(0, InputUtils.getActiveModifiers());
    }

}
