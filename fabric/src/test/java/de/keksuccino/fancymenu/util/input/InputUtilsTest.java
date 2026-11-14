package de.keksuccino.fancymenu.util.input;

import net.minecraft.client.input.InputQuirks;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputUtilsTest {

    private static final int SHORTCUT = InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER;

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
