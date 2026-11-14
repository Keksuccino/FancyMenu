package de.keksuccino.fancymenu.util.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.InputQuirks;
import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDLKeycode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputUtilsTest {

    private static final int SHORTCUT = InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER;

    @Test
    void detectsEachEditShortcut() {
        assertTrue(InputUtils.isSelectAll(InputConstants.KEY_A, SDLKeycode.SDLK_A, SHORTCUT));
        assertTrue(InputUtils.isCopy(InputConstants.KEY_C, SDLKeycode.SDLK_C, SHORTCUT));
        assertTrue(InputUtils.isPaste(InputConstants.KEY_V, SDLKeycode.SDLK_V, SHORTCUT));
        assertTrue(InputUtils.isCut(InputConstants.KEY_X, SDLKeycode.SDLK_X, SHORTCUT));
    }

    @Test
    void editShortcutsNeedTheirOwnKey() {
        assertFalse(InputUtils.isSelectAll(InputConstants.KEY_V, SDLKeycode.SDLK_V, SHORTCUT));
        assertFalse(InputUtils.isCopy(InputConstants.KEY_X, SDLKeycode.SDLK_X, SHORTCUT));
        assertFalse(InputUtils.isPaste(InputConstants.KEY_A, SDLKeycode.SDLK_A, SHORTCUT));
        assertFalse(InputUtils.isCut(InputConstants.KEY_C, SDLKeycode.SDLK_C, SHORTCUT));
    }

    @Test
    void editShortcutsNeedThePlatformShortcutModifier() {
        int otherModifier = (SHORTCUT == SDLKeycode.SDL_KMOD_GUI) ? SDLKeycode.SDL_KMOD_CTRL : SDLKeycode.SDL_KMOD_GUI;
        assertFalse(InputUtils.isPaste(InputConstants.KEY_V, SDLKeycode.SDLK_V, 0));
        assertFalse(InputUtils.isPaste(InputConstants.KEY_V, SDLKeycode.SDLK_V, otherModifier));
        assertFalse(InputUtils.isSelectAll(InputConstants.KEY_A, SDLKeycode.SDLK_A, otherModifier));
    }

    @Test
    void editShortcutsDoNotMatchWithShiftOrAlt() {
        assertFalse(InputUtils.isSelectAll(InputConstants.KEY_A, SDLKeycode.SDLK_A, SHORTCUT | SDLKeycode.SDL_KMOD_LSHIFT));
        assertFalse(InputUtils.isCopy(InputConstants.KEY_C, SDLKeycode.SDLK_C, SHORTCUT | SDLKeycode.SDL_KMOD_LALT));
        assertFalse(InputUtils.isPaste(InputConstants.KEY_V, SDLKeycode.SDLK_V, SHORTCUT | SDLKeycode.SDL_KMOD_LSHIFT));
        assertFalse(InputUtils.isCut(InputConstants.KEY_X, SDLKeycode.SDLK_X, SHORTCUT | SDLKeycode.SDL_KMOD_LALT));
    }

    @Test
    void editShortcutsFollowTheKeyboardLayoutLikeMinecraft() {
        // The key in the A position of a QWERTY keyboard types Q on an AZERTY keyboard, and the other way around
        assertTrue(InputUtils.isSelectAll(InputConstants.KEY_Q, SDLKeycode.SDLK_A, SHORTCUT));
        assertFalse(InputUtils.isSelectAll(InputConstants.KEY_A, SDLKeycode.SDLK_Q, SHORTCUT));
    }

}
