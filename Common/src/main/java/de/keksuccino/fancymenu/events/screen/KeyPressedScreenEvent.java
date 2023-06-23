package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class KeyPressedScreenEvent extends EventBase {

    private final Screen screen;
    private final int keycode;
    private final int scancode;
    private final int modifiers;

    public KeyPressedScreenEvent(Screen screen, int keycode, int scancode, int modifiers) {
        this.screen = screen;
        this.keycode = keycode;
        this.scancode = scancode;
        this.modifiers = modifiers;
    }

    public Screen getScreen() {
        return this.screen;
    }

    public int getKeycode() {
        return this.keycode;
    }

    public int getScancode() {
        return this.scancode;
    }

    public int getModifiers() {
        return this.modifiers;
    }

    @NotNull
    public String getKeyName() {
        String key = GLFW.glfwGetKeyName(this.keycode, this.scancode);
        if (key == null) key = "";
        return key;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
