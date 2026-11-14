package de.keksuccino.fancymenu.util.input;

import net.minecraft.client.input.InputQuirks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputUtilsTest {

    private static final int SHORTCUT = InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER;

    @BeforeEach
    void resetModifiersBeforeTest() {
        InputUtils.resetActiveModifiers();
    }

    @AfterEach
    void resetModifiersAfterTest() {
        InputUtils.resetActiveModifiers();
    }

    @Test
    void platformSemanticModifierMatchesMinecraftInputQuirk() {
        int expectedModifier = InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
        assertEquals(expectedModifier, InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER);
        assertTrue(InputUtils.isGuiShortcutModifierDown(expectedModifier));
    }

    @Test
    void platformSemanticModifierAllowsUnrelatedModifierBits() {
        int modifiers = InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER | GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_CAPS_LOCK;
        assertTrue(InputUtils.isGuiShortcutModifierDown(modifiers));
    }

    @Test
    void nonPlatformShortcutModifierDoesNotMatchSemanticModifier() {
        int nonPlatformModifier = InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER == GLFW.GLFW_MOD_SUPER ? GLFW.GLFW_MOD_CONTROL : GLFW.GLFW_MOD_SUPER;
        assertFalse(InputUtils.isGuiShortcutModifierDown(nonPlatformModifier | GLFW.GLFW_MOD_SHIFT));
    }

    @Test
    void activeModifierCachePreservesTheCompleteEventMask() {
        int modifiers = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_CAPS_LOCK | GLFW.GLFW_MOD_NUM_LOCK;
        InputUtils.updateActiveModifiers(modifiers);
        assertEquals(modifiers, InputUtils.getActiveModifiers());
        assertTrue(InputUtils.isGuiShortcutModifierDown());
    }

    @Test
    void activeModifierUpdatesReplaceRatherThanAccumulateMasks() {
        InputUtils.updateActiveModifiers(InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER | GLFW.GLFW_MOD_SHIFT);
        InputUtils.updateActiveModifiers(GLFW.GLFW_MOD_ALT);
        assertEquals(GLFW.GLFW_MOD_ALT, InputUtils.getActiveModifiers());
        assertFalse(InputUtils.isGuiShortcutModifierDown());
    }

    @Test
    void zeroEventMaskClearsCachedShortcutState() {
        InputUtils.updateActiveModifiers(InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER);
        InputUtils.updateActiveModifiers(0);
        assertEquals(0, InputUtils.getActiveModifiers());
        assertFalse(InputUtils.isGuiShortcutModifierDown());
    }

    @Test
    void resettingActiveModifiersClearsTheCompleteMask() {
        InputUtils.updateActiveModifiers(InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER | GLFW.GLFW_MOD_ALT);
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
