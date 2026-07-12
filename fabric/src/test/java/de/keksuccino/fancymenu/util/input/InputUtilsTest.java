package de.keksuccino.fancymenu.util.input;

import net.minecraft.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputUtilsTest {

    private static final int SHORTCUT_MODIFIER = InputUtils.getGuiShortcutModifier(Util.getPlatform() == Util.OS.OSX);

    @AfterEach
    void resetModifierCache() {
        InputUtils.resetActiveModifiers();
    }

    @Test
    void usesSuperForMacOSShortcutEvents() {
        assertTrue(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_SUPER, true));
        assertTrue(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_SHIFT, true));
        assertFalse(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_CONTROL, true));
        assertFalse(InputUtils.isGuiShortcutModifierDown(0, true));
    }

    @Test
    void usesControlForNonMacOSShortcutEvents() {
        assertTrue(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_CONTROL, false));
        assertTrue(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT, false));
        assertFalse(InputUtils.isGuiShortcutModifierDown(GLFW.GLFW_MOD_SUPER, false));
        assertFalse(InputUtils.isGuiShortcutModifierDown(0, false));
    }

    @ParameterizedTest
    @CsvSource({
            "65,65,true",
            "67,67,true",
            "86,86,true",
            "88,88,true",
            "65,67,false"
    })
    void editShortcutsRequireTheExpectedKey(int keyCode, int expectedKeyCode, boolean expected) {
        int modifiers = SHORTCUT_MODIFIER;
        boolean actual = switch (expectedKeyCode) {
            case GLFW.GLFW_KEY_A -> InputUtils.isSelectAll(keyCode, modifiers);
            case GLFW.GLFW_KEY_C -> InputUtils.isCopy(keyCode, modifiers);
            case GLFW.GLFW_KEY_V -> InputUtils.isPaste(keyCode, modifiers);
            case GLFW.GLFW_KEY_X -> InputUtils.isCut(keyCode, modifiers);
            default -> throw new IllegalArgumentException("Unexpected test key: " + expectedKeyCode);
        };
        assertEquals(expected, actual);
    }

    @Test
    void editShortcutsRejectShiftAltAndMissingShortcutModifier() {
        int shortcut = SHORTCUT_MODIFIER;
        assertFalse(InputUtils.isSelectAll(GLFW.GLFW_KEY_A, shortcut | GLFW.GLFW_MOD_SHIFT));
        assertFalse(InputUtils.isCopy(GLFW.GLFW_KEY_C, shortcut | GLFW.GLFW_MOD_ALT));
        assertFalse(InputUtils.isPaste(GLFW.GLFW_KEY_V, 0));
        assertFalse(InputUtils.isCut(GLFW.GLFW_KEY_X, GLFW.GLFW_MOD_SHIFT));
    }

    @Test
    void cachesOverwritesAndResetsModifierState() {
        InputUtils.updateActiveModifiers(GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_SHIFT);
        assertEquals(GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_SHIFT, InputUtils.getActiveModifiers());

        InputUtils.updateActiveModifiers(GLFW.GLFW_MOD_ALT);
        assertEquals(GLFW.GLFW_MOD_ALT, InputUtils.getActiveModifiers());

        InputUtils.resetActiveModifiers();
        assertEquals(0, InputUtils.getActiveModifiers());
    }

}
