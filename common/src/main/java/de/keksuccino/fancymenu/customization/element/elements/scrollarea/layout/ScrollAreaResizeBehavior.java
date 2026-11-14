package de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout;

import de.keksuccino.fancymenu.util.properties.Property;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;

/**
 * What happens to the children of a scrollable area with a proportional content when the area changes its size.
 */
public enum ScrollAreaResizeBehavior {

    /** The content gets scaled like a picture: everything in it keeps its look, bigger or smaller. */
    SCALE,
    /** Only the places and the sizes of the children follow the area. What they draw in their new size is up to them. */
    RESIZE;

    /** The key of the property that holds the behavior of an area. Changing it breaks saved layouts. */
    public static final String PROPERTY_KEY = "resize_behavior";
    private static final String LOCALIZATION_KEY = "fancymenu.elements.scroll_area.resize_behavior";

    private final String localizationKey = LOCALIZATION_KEY + "." + this.name().toLowerCase(Locale.ROOT);

    @NotNull
    public static Property<ScrollAreaResizeBehavior> buildProperty() {
        return Property.enumProperty(PROPERTY_KEY, SCALE, LOCALIZATION_KEY, ScrollAreaResizeBehavior::getLocalizationKey);
    }

    @NotNull
    public String getLocalizationKey() {
        return this.localizationKey;
    }

}
