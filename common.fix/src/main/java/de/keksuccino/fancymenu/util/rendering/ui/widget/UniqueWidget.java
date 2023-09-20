package de.keksuccino.fancymenu.util.rendering.ui.widget;

import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.Nullable;

/**
 * Gets applied to the {@link AbstractWidget} class, to be able to set identifiers to instances of it.
 */
public interface UniqueWidget {

    AbstractWidget setWidgetIdentifierFancyMenu(@Nullable String identifier);

    String getWidgetIdentifierFancyMenu();

}
