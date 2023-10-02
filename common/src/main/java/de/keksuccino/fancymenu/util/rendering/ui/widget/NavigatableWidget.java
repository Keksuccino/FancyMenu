package de.keksuccino.fancymenu.util.rendering.ui.widget;

import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * {@link GuiEventListener}s that implement this interface can control if they should be navigatable and/or focusable.
 */
public interface NavigatableWidget {

    boolean isFocusable();

    void setFocusable(boolean focusable);

    boolean isNavigatable();

    void setNavigatable(boolean navigatable);

}
