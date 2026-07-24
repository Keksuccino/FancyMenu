package de.keksuccino.fancymenu.util.rendering.ui;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Tracks the FancyMenu component that consumed each active mouse-button press. */
public final class FancyMenuPointerTracker {

    private final Map<Integer, GuiEventListener> pressOwners = new HashMap<>();

    @Nullable
    public GuiEventListener routeMouseClicked(@NotNull Iterable<? extends GuiEventListener> listeners, @NotNull MouseButtonEvent event, boolean isDoubleClick) {
        this.pressOwners.remove(event.button());
        GuiEventListener listener = FancyMenuInputRouter.routeContainerMouseClicked(listeners, event, isDoubleClick);
        if (listener != null) this.pressOwners.put(event.button(), listener);
        return listener;
    }

    public boolean dispatchMouseReleased(@NotNull MouseButtonEvent event) {
        GuiEventListener listener = this.pressOwners.remove(event.button());
        if (listener == null) return false;
        listener.mouseReleased(event);
        return true;
    }

    public boolean dispatchMouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        GuiEventListener listener = this.pressOwners.get(event.button());
        if (listener == null) return false;
        listener.mouseDragged(event, dragX, dragY);
        return true;
    }

}
