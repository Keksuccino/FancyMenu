package de.keksuccino.fancymenu.util.rendering.ui.widget.interaction;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuPointerTracker;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;

import javax.annotation.Nonnull;

/**
 * Applies the 1.21.1 container focus and dragging contract around the shared FancyMenu pointer tracker. Keeping this
 * target-specific boundary prevents browser and FancyMenu widget gestures from leaking into inventory slot handling.
 */
public final class ContainerWidgetInteractionRouter {

    private ContainerWidgetInteractionRouter() {
    }

    public static boolean mouseClicked(@Nonnull ContainerEventHandler parent, @Nonnull FancyMenuPointerTracker tracker, @Nonnull Iterable<? extends GuiEventListener> children, double mouseX, double mouseY, int button) {
        GuiEventListener listener = tracker.routeMouseClicked(children, mouseX, mouseY, button);
        if (listener == null) return false;
        // Manual routing must mirror ContainerEventHandler's focus contract. Edit boxes and browsers rely on their parent to focus them.
        parent.setFocused(listener);
        if (button == 0) parent.setDragging(true);
        return true;
    }

    public static boolean mouseDragged(@Nonnull FancyMenuPointerTracker tracker, double mouseX, double mouseY, int button, double dragX, double dragY) {
        return tracker.dispatchMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public static boolean mouseReleased(@Nonnull ContainerEventHandler parent, @Nonnull FancyMenuPointerTracker tracker, double mouseX, double mouseY, int button) {
        if (!tracker.dispatchMouseReleased(mouseX, mouseY, button)) return false;
        if ((button == 0) && parent.isDragging()) parent.setDragging(false);
        return true;
    }

}
