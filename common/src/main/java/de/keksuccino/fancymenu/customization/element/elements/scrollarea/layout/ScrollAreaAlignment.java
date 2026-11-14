package de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout;

import de.keksuccino.fancymenu.util.properties.Property;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;

/**
 * Where arranged children of a scrollable area sit across the direction they are arranged in:
 * left, center or right for children that run from top to bottom, and top, center or bottom for children that run sideways.
 */
public enum ScrollAreaAlignment {

    START,
    CENTER,
    END,
    /** Children get as big as the room they have. */
    FILL;

    /** The key of the property that holds the alignment of an area. Changing it breaks saved layouts. */
    public static final String PROPERTY_KEY = "child_alignment";
    private static final String LOCALIZATION_KEY = "fancymenu.elements.scroll_area.alignment";

    private final String localizationKey = LOCALIZATION_KEY + "." + this.name().toLowerCase(Locale.ROOT);

    @NotNull
    public static Property<ScrollAreaAlignment> buildProperty() {
        return Property.enumProperty(PROPERTY_KEY, START, LOCALIZATION_KEY, ScrollAreaAlignment::getLocalizationKey);
    }

    /**
     * Returns how far something has to move from the start of its room to sit in it the way this alignment says.
     *
     * @param freeRoom The room that is left when the thing is in it.
     */
    public int offset(int freeRoom) {
        if (freeRoom <= 0) return 0;
        if (this == CENTER) return freeRoom / 2;
        if (this == END) return freeRoom;
        return 0;
    }

    @NotNull
    public String getLocalizationKey() {
        return this.localizationKey;
    }

}
