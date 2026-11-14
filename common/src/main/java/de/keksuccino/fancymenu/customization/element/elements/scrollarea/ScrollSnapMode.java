package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.util.properties.Property;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;

/**
 * Where the content of a scrollable area comes to rest when the user scrolls it.
 */
public enum ScrollSnapMode {

    /** Wherever the user scrolled it to. */
    NONE,
    /** At the start of one of the elements in the area. */
    ELEMENTS,
    /** At the start of a page, which is as big as what the area shows. */
    PAGES;

    /** The key of the property that holds the mode of an area. Changing it breaks saved layouts. */
    public static final String PROPERTY_KEY = "scroll_snap_mode";
    private static final String LOCALIZATION_KEY = "fancymenu.elements.scroll_area.scrolling.snap";

    private final String localizationKey = LOCALIZATION_KEY + "." + this.name().toLowerCase(Locale.ROOT);

    @NotNull
    public static Property<ScrollSnapMode> buildProperty() {
        return Property.enumProperty(PROPERTY_KEY, NONE, LOCALIZATION_KEY, ScrollSnapMode::getLocalizationKey);
    }

    @NotNull
    public String getLocalizationKey() {
        return this.localizationKey;
    }

}
