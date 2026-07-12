package de.keksuccino.fancymenu.util.input;

import net.minecraft.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditBoxInputControllerTest {

    private static final int SHORTCUT_MODIFIER = Util.getPlatform() == Util.OS.OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
    private static final int NON_SHORTCUT_MODIFIER = Util.getPlatform() == Util.OS.OSX ? GLFW.GLFW_MOD_CONTROL : GLFW.GLFW_MOD_SUPER;

    @ParameterizedTest
    @CsvSource({
            "259,DELETE_WORD_BACKWARD,DELETE_CHARACTER_BACKWARD",
            "261,DELETE_WORD_FORWARD,DELETE_CHARACTER_FORWARD"
    })
    void deletionUsesTheSemanticShortcutForWholeWords(int keyCode, EditBoxInputController.Action wordAction, EditBoxInputController.Action characterAction) {
        assertEquals(wordAction, EditBoxInputController.resolve(keyCode, SHORTCUT_MODIFIER));
        assertEquals(characterAction, EditBoxInputController.resolve(keyCode, NON_SHORTCUT_MODIFIER));
    }

    @ParameterizedTest
    @CsvSource({
            "263,MOVE_WORD_LEFT,MOVE_CHARACTER_LEFT",
            "262,MOVE_WORD_RIGHT,MOVE_CHARACTER_RIGHT"
    })
    void horizontalMovementUsesTheSemanticShortcutForWords(int keyCode, EditBoxInputController.Action wordAction, EditBoxInputController.Action characterAction) {
        assertEquals(wordAction, EditBoxInputController.resolve(keyCode, SHORTCUT_MODIFIER | GLFW.GLFW_MOD_SHIFT));
        assertEquals(characterAction, EditBoxInputController.resolve(keyCode, NON_SHORTCUT_MODIFIER | GLFW.GLFW_MOD_SHIFT));
    }

    @ParameterizedTest
    @CsvSource({
            "268,MOVE_START",
            "269,MOVE_END"
    })
    void resolvesHomeAndEnd(int keyCode, EditBoxInputController.Action action) {
        assertEquals(action, EditBoxInputController.resolve(keyCode, GLFW.GLFW_MOD_SHIFT));
    }

    @ParameterizedTest
    @CsvSource({
            "65,SELECT_ALL",
            "67,COPY",
            "86,PASTE",
            "88,CUT"
    })
    void resolvesStrictClipboardAndSelectionShortcuts(int keyCode, EditBoxInputController.Action action) {
        assertEquals(action, EditBoxInputController.resolve(keyCode, SHORTCUT_MODIFIER));
        assertEquals(EditBoxInputController.Action.NONE, EditBoxInputController.resolve(keyCode, SHORTCUT_MODIFIER | GLFW.GLFW_MOD_SHIFT));
        assertEquals(EditBoxInputController.Action.NONE, EditBoxInputController.resolve(keyCode, SHORTCUT_MODIFIER | GLFW.GLFW_MOD_ALT));
        assertEquals(EditBoxInputController.Action.NONE, EditBoxInputController.resolve(keyCode, NON_SHORTCUT_MODIFIER));
    }

    @Test
    void ignoresUnrelatedKeys() {
        assertEquals(EditBoxInputController.Action.NONE, EditBoxInputController.resolve(GLFW.GLFW_KEY_F, SHORTCUT_MODIFIER));
    }

}
