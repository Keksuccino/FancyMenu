package de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout;

import de.keksuccino.fancymenu.util.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;

/**
 * The ways a scrollable area places its children.
 */
public enum ScrollAreaLayoutMode {

    /** Children place themselves in the content with anchors, the way elements place themselves on the screen. */
    ANCHORS(null, false),
    /**
     * Children are placed freely, without anchors, on a content that scales with the area:
     * everything keeps its place and its size in relation to the area.
     */
    PROPORTIONAL(ScrollAreaArranger.FREE, false),
    STACK(ScrollAreaArranger.STACK, true),
    GRID(ScrollAreaArranger.GRID, true),
    FLOW(ScrollAreaArranger.FLOW, true);

    /** The key of the property that holds the mode of an area. Changing it breaks saved layouts. */
    public static final String PROPERTY_KEY = "layout_mode";
    private static final String LOCALIZATION_KEY = "fancymenu.elements.scroll_area.layout_mode";

    @Nullable
    private final ScrollAreaArranger arranger;
    private final boolean arrangesChildren;
    private final String localizationKey = LOCALIZATION_KEY + "." + this.name().toLowerCase(Locale.ROOT);

    ScrollAreaLayoutMode(@Nullable ScrollAreaArranger arranger, boolean arrangesChildren) {
        this.arranger = arranger;
        this.arrangesChildren = arrangesChildren;
    }

    @NotNull
    public static Property<ScrollAreaLayoutMode> buildProperty() {
        return Property.enumProperty(PROPERTY_KEY, ANCHORS, LOCALIZATION_KEY, ScrollAreaLayoutMode::getLocalizationKey);
    }

    /**
     * Returns what works out the boxes of the children in this mode, or NULL if the children work out their boxes themselves.
     */
    @Nullable
    public ScrollAreaArranger getArranger() {
        return this.arranger;
    }

    /**
     * Returns if the area decides where its children are. If not, the user places them.
     */
    public boolean arrangesChildren() {
        return this.arrangesChildren;
    }

    /**
     * Returns if the content is as big as the user sets it. If not, it is as big as the arranged children need it.
     */
    public boolean hasUserContentSize() {
        return !this.arrangesChildren;
    }

    /**
     * Returns if the area can scroll in both directions at once in this mode. Arranged children run in one direction, which is the one the area scrolls in.
     */
    public boolean supportsBothDirections() {
        return !this.arrangesChildren;
    }

    /**
     * Returns if children can fill the room they have across the direction they line up in. Children that flow sit next to each other
     * in that room with the size they have, so there is nothing for one of them to fill.
     */
    public boolean supportsFillAlignment() {
        return this != FLOW;
    }

    @NotNull
    public String getLocalizationKey() {
        return this.localizationKey;
    }

}
