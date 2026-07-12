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

}
