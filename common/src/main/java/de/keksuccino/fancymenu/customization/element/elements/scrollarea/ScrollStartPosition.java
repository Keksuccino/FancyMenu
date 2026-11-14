package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.util.properties.Property;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;

/**
 * Where the content of a scrollable area is when the area shows up.
 */
public enum ScrollStartPosition {

    START,
    /** At its end, like a log whose newest lines are the last ones. */
    END;

    /** The key of the property that holds the start position of an area. Changing it breaks saved layouts. */
    public static final String PROPERTY_KEY = "scroll_start_position";
    private static final String LOCALIZATION_KEY = "fancymenu.elements.scroll_area.scrolling.start_position";

    private final String localizationKey = LOCALIZATION_KEY + "." + this.name().toLowerCase(Locale.ROOT);

    @NotNull
    public static Property<ScrollStartPosition> buildProperty() {
        return Property.enumProperty(PROPERTY_KEY, START, LOCALIZATION_KEY, ScrollStartPosition::getLocalizationKey);
    }

    @NotNull
    public String getLocalizationKey() {
        return this.localizationKey;
    }

}
