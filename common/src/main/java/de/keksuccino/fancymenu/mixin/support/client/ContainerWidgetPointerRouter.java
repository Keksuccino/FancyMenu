package de.keksuccino.fancymenu.mixin.support.client;

import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.HashMap;
import java.util.Map;

public final class ContainerWidgetPointerRouter {

    private final Map<Integer, GuiEventListener> ownersByButton = new HashMap<>();

    public ContainerWidgetPointerRouter() {
    }

    public boolean mouseClicked(ContainerEventHandler host, MouseButtonEvent event, boolean isDoubleClick) {
        GuiEventListener previousOwner = this.ownersByButton.remove(event.button());
        if (previousOwner != null && event.button() == 0 && host.isDragging()) host.setDragging(false);

        for (GuiEventListener listener : host.children()) {
            if (canHandle(listener, event) && listener.mouseClicked(event, isDoubleClick)) {
                this.ownersByButton.put(event.button(), listener);
                if (listener.shouldTakeFocusAfterInteraction()) {
                    host.setFocused(listener);
                    if (event.button() == 0) host.setDragging(true);
                }
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        GuiEventListener owner = this.ownersByButton.get(event.button());
        if (owner == null) return false;
        owner.mouseDragged(event, dragX, dragY);
        return true;
    }

    public boolean mouseReleased(ContainerEventHandler host, MouseButtonEvent event) {
        GuiEventListener owner = this.ownersByButton.remove(event.button());
        if (owner == null) return false;
        if (event.button() == 0 && host.isDragging()) host.setDragging(false);
        owner.mouseReleased(event);
        return true;
    }

    private static boolean canHandle(GuiEventListener listener, MouseButtonEvent event) {
        return listener instanceof FancyMenuWidget && listener.isMouseOver(event.x(), event.y());
    }

}
