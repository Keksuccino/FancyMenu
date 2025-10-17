package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public class ScreenKeyPressedEvent extends EventBase {

    private final Screen screen;
    private final KeyEvent keyEvent;

    public ScreenKeyPressedEvent(@NotNull Screen screen, @NotNull KeyEvent keyEvent) {
        this.screen = screen;
        this.keyEvent = keyEvent;
    }

    @NotNull
    public KeyEvent getKeyEvent() {
        return this.keyEvent;
    }

    @NotNull
    public Screen getScreen() {
        return this.screen;
    }

    public int getKeycode() {
        return this.keyEvent.key();
    }

    public int getScancode() {
        return this.keyEvent.scancode();
    }

    public int getModifiers() {
        return this.keyEvent.modifiers();
    }

    @NotNull
    public String getKeyName() {
        String key = GLFW.glfwGetKeyName(this.keyEvent.key(), this.keyEvent.scancode());
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
