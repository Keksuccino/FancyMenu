package de.keksuccino.fancymenu.util.input;

import net.minecraft.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputUtilsTest {

    private static final int SHORTCUT = (Util.getPlatform() == Util.OS.OSX) ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;

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

    @Test
    void detectsEachEditShortcut() {
        assertTrue(InputUtils.isSelectAll(GLFW.GLFW_KEY_A, 0, SHORTCUT));
        assertTrue(InputUtils.isCopy(GLFW.GLFW_KEY_C, 0, SHORTCUT));
        assertTrue(InputUtils.isPaste(GLFW.GLFW_KEY_V, 0, SHORTCUT));
        assertTrue(InputUtils.isCut(GLFW.GLFW_KEY_X, 0, SHORTCUT));
    }

    @Test
    void editShortcutsNeedTheirOwnKey() {
        assertFalse(InputUtils.isSelectAll(GLFW.GLFW_KEY_V, 0, SHORTCUT));
        assertFalse(InputUtils.isCopy(GLFW.GLFW_KEY_X, 0, SHORTCUT));
        assertFalse(InputUtils.isPaste(GLFW.GLFW_KEY_A, 0, SHORTCUT));
        assertFalse(InputUtils.isCut(GLFW.GLFW_KEY_C, 0, SHORTCUT));
    }

    @Test
    void editShortcutsNeedThePlatformShortcutModifier() {
        int otherModifier = (SHORTCUT == GLFW.GLFW_MOD_SUPER) ? GLFW.GLFW_MOD_CONTROL : GLFW.GLFW_MOD_SUPER;
        assertFalse(InputUtils.isPaste(GLFW.GLFW_KEY_V, 0, 0));
        assertFalse(InputUtils.isPaste(GLFW.GLFW_KEY_V, 0, otherModifier));
        assertFalse(InputUtils.isSelectAll(GLFW.GLFW_KEY_A, 0, otherModifier));
    }

    @Test
    void editShortcutsDoNotMatchWithShiftOrAlt() {
        assertFalse(InputUtils.isSelectAll(GLFW.GLFW_KEY_A, 0, SHORTCUT | GLFW.GLFW_MOD_SHIFT));
        assertFalse(InputUtils.isCopy(GLFW.GLFW_KEY_C, 0, SHORTCUT | GLFW.GLFW_MOD_ALT));
        assertFalse(InputUtils.isPaste(GLFW.GLFW_KEY_V, 0, SHORTCUT | GLFW.GLFW_MOD_SHIFT));
        assertFalse(InputUtils.isCut(GLFW.GLFW_KEY_X, 0, SHORTCUT | GLFW.GLFW_MOD_ALT));
    }

}
