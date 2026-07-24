package de.keksuccino.fancymenu.util.rendering.ui;

import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Tracks the FancyMenu component that consumed each active mouse-button press. */
public final class FancyMenuPointerTracker {

    private final Map<Integer, GuiEventListener> pressOwners = new HashMap<>();

    @Nullable
    public GuiEventListener routeMouseClicked(@NotNull Iterable<? extends GuiEventListener> listeners, double mouseX, double mouseY, int button) {
        this.pressOwners.remove(button);
        GuiEventListener listener = FancyMenuInputRouter.routeContainerMouseClicked(listeners, mouseX, mouseY, button);
        if (listener != null) this.pressOwners.put(button, listener);
        return listener;
    }

    public boolean dispatchMouseReleased(double mouseX, double mouseY, int button) {
        GuiEventListener listener = this.pressOwners.remove(button);
        if (listener == null) return false;
        listener.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    public boolean dispatchMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        GuiEventListener listener = this.pressOwners.get(button);
        if (listener == null) return false;
        listener.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return true;
    }

}
