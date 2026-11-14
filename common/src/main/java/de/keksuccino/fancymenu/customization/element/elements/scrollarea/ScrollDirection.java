package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.util.properties.Property;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;

/**
 * The directions the content of a scrollable area can be scrolled in. On an axis that does not scroll,
 * the content is exactly as big as what the area shows of it.
 */
public enum ScrollDirection {

    VERTICAL(false, true),
    HORIZONTAL(true, false),
    BOTH(true, true);

    /** The key of the property that holds the direction of an area. Changing it breaks saved layouts. */
    public static final String PROPERTY_KEY = "scroll_direction";
    private static final String LOCALIZATION_KEY = "fancymenu.elements.scroll_area.scroll_direction";

    private final boolean scrollsX;
    private final boolean scrollsY;
    private final String localizationKey = LOCALIZATION_KEY + "." + this.name().toLowerCase(Locale.ROOT);

    ScrollDirection(boolean scrollsX, boolean scrollsY) {
        this.scrollsX = scrollsX;
        this.scrollsY = scrollsY;
    }

    @NotNull
    public static Property<ScrollDirection> buildProperty() {
        return Property.enumProperty(PROPERTY_KEY, VERTICAL, LOCALIZATION_KEY, ScrollDirection::getLocalizationKey);
    }

    public boolean scrollsX() {
        return this.scrollsX;
    }

    public boolean scrollsY() {
        return this.scrollsY;
    }

    @NotNull
    public String getLocalizationKey() {
        return this.localizationKey;
    }

}
