package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ScreenKeyPressedEvent extends EventBase {

    private final Screen screen;
    private final int keycode;
    private final int scancode;
    private final int modifiers;

    public ScreenKeyPressedEvent(Screen screen, int keycode, int scancode, int modifiers) {
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

    public <T extends GuiEventListener & NarratableEntry> void addWidget(T widget) {
        this.getWidgets().add(widget);
        this.getNarratables().add(widget);
    }

    public <T extends GuiEventListener & NarratableEntry & Renderable> void addRenderableWidget(T widget) {
        this.addWidget(widget);
        this.getRenderables().add(widget);
    }

    public List<GuiEventListener> getWidgets() {
        return ((IMixinScreen)this.getScreen()).getChildrenFancyMenu();
    }

    public List<Renderable> getRenderables() {
        return ((IMixinScreen)this.getScreen()).getRenderablesFancyMenu();
    }

    public List<NarratableEntry> getNarratables() {
        return ((IMixinScreen)this.getScreen()).getNarratablesFancyMenu();
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
