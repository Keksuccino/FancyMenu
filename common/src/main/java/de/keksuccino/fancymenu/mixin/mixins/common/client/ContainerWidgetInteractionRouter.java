package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import javax.annotation.Nonnull;
import java.util.List;

/**
 * Routes pointer interactions to FancyMenu widgets before container slot handling. A handled press owns its button until release, even when later callbacks return false, so a single gesture can never leak into slot logic halfway through.
 */
final class ContainerWidgetInteractionRouter {

    private ContainerWidgetInteractionRouter() {}

    static boolean mouseClicked(@Nonnull ContainerEventHandler parent, @Nonnull ContainerWidgetPointerTracker<GuiEventListener> tracker, @Nonnull List<? extends GuiEventListener> children, double mouseX, double mouseY, int button) {
        tracker.begin(button);
        for (GuiEventListener listener : children) {
            if ((listener instanceof FancyMenuWidget) && (listener instanceof AbstractWidget widget) && widget.isMouseOver(mouseX, mouseY) && listener.mouseClicked(mouseX, mouseY, button)) {
                // Manual routing must mirror ContainerEventHandler's focus contract. Edit boxes rely on their parent to focus them.
                tracker.claim(button, listener);
                parent.setFocused(listener);
                if (button == 0) {
                    parent.setDragging(true);
                }
                return true;
            }
        }
        return false;
    }

    static boolean mouseDragged(@Nonnull ContainerWidgetPointerTracker<GuiEventListener> tracker, double mouseX, double mouseY, int button, double dragX, double dragY) {
        GuiEventListener owner = tracker.owner(button);
        if (owner == null) return false;
        owner.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return true;
    }

    static boolean mouseReleased(@Nonnull ContainerEventHandler parent, @Nonnull ContainerWidgetPointerTracker<GuiEventListener> tracker, double mouseX, double mouseY, int button) {
        GuiEventListener owner = tracker.release(button);
        if (owner == null) return false;
        if ((button == 0) && parent.isDragging()) {
            parent.setDragging(false);
        }
        owner.mouseReleased(mouseX, mouseY, button);
        return true;
    }

}
