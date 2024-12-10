package de.keksuccino.fancymenu.util.rendering.ui;

import net.minecraft.client.gui.components.events.ContainerEventHandler;

/**
 * This interface only exists to make my UI components use the old mouseClicked logic in {@link ContainerEventHandler}
 * that does not only click the hovered UI component, but all of them. Thank you Mojang for killing my whole UI logic btw.
 */
public interface FancyMenuUiComponent {

}
