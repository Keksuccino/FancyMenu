package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.util.properties.Property;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;

/**
 * When a scrollable area shows the scroll bars of the directions it can be scrolled in.
 */
public enum ScrollBarVisibility {

    ALWAYS,
    /** The bars show while the content moves and while the mouse is on them, and fade out when nothing happens. */
    WHILE_SCROLLING,
    NEVER;

    /** The key of the property that holds the visibility of the bars of an area. Changing it breaks saved layouts. */
    public static final String PROPERTY_KEY = "scroll_bar_visibility";
    private static final String LOCALIZATION_KEY = "fancymenu.elements.scroll_area.scroll_bars.visibility";

    private final String localizationKey = LOCALIZATION_KEY + "." + this.name().toLowerCase(Locale.ROOT);

    @NotNull
    public static Property<ScrollBarVisibility> buildProperty() {
        return Property.enumProperty(PROPERTY_KEY, ALWAYS, LOCALIZATION_KEY, ScrollBarVisibility::getLocalizationKey);
    }

    @NotNull
    public String getLocalizationKey() {
        return this.localizationKey;
    }

}
