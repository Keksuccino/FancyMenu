package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.util.ClassExtender;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.Nullable;

/**
 * Gets applied to the {@link AbstractWidget} class, to be able to set identifiers to instances of it.
 */
@ClassExtender(AbstractWidget.class)
public interface UniqueWidget {

    AbstractWidget setWidgetIdentifierFancyMenu(@Nullable String identifier);

    String getWidgetIdentifierFancyMenu();

}
