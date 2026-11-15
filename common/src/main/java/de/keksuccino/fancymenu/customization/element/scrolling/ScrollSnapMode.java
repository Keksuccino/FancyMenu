package de.keksuccino.fancymenu.customization.element.scrolling;

import de.keksuccino.fancymenu.util.properties.Property;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;
import java.util.Set;

/**
 * Where the content of a scrollable element comes to rest when the user scrolls it.<br>
 * Not every element has all of these: only content that holds elements can snap to them, and only text has lines and headlines.
 */
public enum ScrollSnapMode {

    /** Wherever the user scrolled it to. */
    NONE,
    /** At the start of one of the elements in the content. */
    ELEMENTS,
    /** At the start of a line of the text in the content. */
    LINES,
    /** At the start of a headline of the text in the content. */
    HEADLINES,
    /** At the start of a page, which is as big as what the element shows. */
    PAGES;

    /** The key of the property that holds the mode of an element. Changing it breaks saved layouts. */
    public static final String PROPERTY_KEY = "scroll_snap_mode";
    private static final String LOCALIZATION_KEY = "fancymenu.elements.scroll_area.scrolling.snap";
    private static final ScrollSnapMode[] VALUES = values();

    private final String localizationKey = LOCALIZATION_KEY + "." + this.name().toLowerCase(Locale.ROOT);

    /**
     * Builds the property that holds the mode of an element. Setting it to a mode the element does not have
     * makes it the next one the element has, so going through the modes one by one skips the others,
     * and a saved mode the element does not have never gets loaded.
     *
     * @param supported The modes the element has.
     */
    @NotNull
    public static Property<ScrollSnapMode> buildProperty(@NotNull Set<ScrollSnapMode> supported) {
        Property<ScrollSnapMode> property = Property.enumProperty(PROPERTY_KEY, NONE, LOCALIZATION_KEY, ScrollSnapMode::getLocalizationKey);
        property.setValueSetProcessor(mode -> mode.orNextOf(supported));
        return property;
    }

    /**
     * Returns this mode if it is one of the given ones, and the next one of them otherwise.
     */
    @NotNull
    public ScrollSnapMode orNextOf(@NotNull Set<ScrollSnapMode> supported) {
        for (int i = 0; i < VALUES.length; i++) {
            ScrollSnapMode mode = VALUES[(this.ordinal() + i) % VALUES.length];
            if (supported.contains(mode)) return mode;
        }
        return NONE;
    }

    @NotNull
    public String getLocalizationKey() {
        return this.localizationKey;
    }

}
