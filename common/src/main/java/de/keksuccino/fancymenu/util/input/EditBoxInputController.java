package de.keksuccino.fancymenu.util.input;

import org.lwjgl.glfw.GLFW;

public final class EditBoxInputController {

    private EditBoxInputController() {
    }

    public static Action resolve(int keyCode, int modifiers) {
        boolean shortcutModifierDown = InputUtils.isGuiShortcutModifierDown(modifiers);
        return switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> shortcutModifierDown ? Action.DELETE_WORD_BACKWARD : Action.DELETE_CHARACTER_BACKWARD;
            case GLFW.GLFW_KEY_DELETE -> shortcutModifierDown ? Action.DELETE_WORD_FORWARD : Action.DELETE_CHARACTER_FORWARD;
            case GLFW.GLFW_KEY_LEFT -> shortcutModifierDown ? Action.MOVE_WORD_LEFT : Action.MOVE_CHARACTER_LEFT;
            case GLFW.GLFW_KEY_RIGHT -> shortcutModifierDown ? Action.MOVE_WORD_RIGHT : Action.MOVE_CHARACTER_RIGHT;
            case GLFW.GLFW_KEY_HOME -> Action.MOVE_START;
            case GLFW.GLFW_KEY_END -> Action.MOVE_END;
            default -> resolveEditShortcut(keyCode, modifiers);
        };
    }

    private static Action resolveEditShortcut(int keyCode, int modifiers) {
        if (InputUtils.isSelectAll(keyCode, modifiers)) return Action.SELECT_ALL;
        if (InputUtils.isCopy(keyCode, modifiers)) return Action.COPY;
        if (InputUtils.isPaste(keyCode, modifiers)) return Action.PASTE;
        if (InputUtils.isCut(keyCode, modifiers)) return Action.CUT;
        return Action.NONE;
    }

    public enum Action {
        NONE,
        DELETE_CHARACTER_BACKWARD,
        DELETE_WORD_BACKWARD,
        DELETE_CHARACTER_FORWARD,
        DELETE_WORD_FORWARD,
        MOVE_CHARACTER_LEFT,
        MOVE_WORD_LEFT,
        MOVE_CHARACTER_RIGHT,
        MOVE_WORD_RIGHT,
        MOVE_START,
        MOVE_END,
        SELECT_ALL,
        COPY,
        PASTE,
        CUT
    }

}
