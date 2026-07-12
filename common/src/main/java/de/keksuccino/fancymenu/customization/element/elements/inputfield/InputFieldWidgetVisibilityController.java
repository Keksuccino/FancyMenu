package de.keksuccino.fancymenu.customization.element.elements.inputfield;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Synchronizes a separately registered input widget with its owning element. Hidden widgets must also release focus through the parent container or keyboard events can remain routed to an invisible child.
 */
final class InputFieldWidgetVisibilityController {

    private InputFieldWidgetVisibilityController() {}

    static void synchronize(@Nonnull AbstractWidget widget, boolean visible, @Nullable ContainerEventHandler parent) {
        if (!visible) {
            if ((parent != null) && (parent.getFocused() == widget)) {
                parent.setFocused(null);
            } else {
                widget.setFocused(false);
            }
        }
        widget.visible = visible;
    }

}
