package de.keksuccino.fancymenu.util.input;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class InputUtils {

    @NotNull
    public static String getKeyName(int keycode, int scancode) {
        String key = GLFW.glfwGetKeyName(keycode, scancode);
        if (key == null) key = "";
        return key;
    }

}
