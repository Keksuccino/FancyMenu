package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;

/**
 * Bridges the package-private pointer controller into the Minecraft class that receives the mixin members.
 * The mixed target lives in a different package at runtime, so it must only reference this public bridge and
 * cannot safely construct or invoke the intentionally package-private controller directly.
 */
public final class ContainerWidgetPointerRoutingBridge {

    private ContainerWidgetPointerRoutingBridge() {
    }

    public static Object createRouter() {
        return new ContainerWidgetPointerRouter();
    }

    public static boolean mouseClicked(Object router, Host host, MouseButtonEvent event, boolean isDoubleClick) {
        return getRouter(router).mouseClicked(host, event, isDoubleClick);
    }

    public static boolean mouseDragged(Object router, MouseButtonEvent event, double dragX, double dragY) {
        return getRouter(router).mouseDragged(event, dragX, dragY);
    }

    public static boolean mouseReleased(Object router, Host host, MouseButtonEvent event) {
        return getRouter(router).mouseReleased(host, event);
    }

    private static ContainerWidgetPointerRouter getRouter(Object router) {
        return (ContainerWidgetPointerRouter) router;
    }

    public interface Host {

        List<? extends GuiEventListener> children();

        void setFocused(GuiEventListener listener);

        boolean isDragging();

        void setDragging(boolean dragging);

    }

}
