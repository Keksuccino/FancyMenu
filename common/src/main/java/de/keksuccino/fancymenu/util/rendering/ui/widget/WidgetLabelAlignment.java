package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.util.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;

/**
 * Where the label of a button or slider sits horizontally.
 * Labels that are wider than their widget always scroll from side to side, no matter the alignment.
 */
public enum WidgetLabelAlignment {

    LEFT,
    CENTERED,
    RIGHT;

    /** The key of the property that holds the label alignment of a widget element. Changing it breaks saved layouts. */
    public static final String PROPERTY_KEY = "label_alignment";
    public static final String LOCALIZATION_KEY = "fancymenu.elements.widgets.label.alignment";
    /**
     * Extra room between a left or right label and its side of the widget, on top of the label margin of the widget.<br>
     * Together with the margin of 2 pixels, labels keep the same distance to the edge as the text of Vanilla's text fields.
     */
    public static final int SIDE_PADDING = 2;
    // Cached, because values() copies the array on every call and the global alignments get resolved every frame
    private static final WidgetLabelAlignment[] VALUES = values();

    private final String serializedName = this.name().toLowerCase(Locale.ROOT);
    private final String localizationKey = LOCALIZATION_KEY + "." + this.serializedName;

    @NotNull
    public static Property<WidgetLabelAlignment> buildProperty() {
        return Property.enumProperty(PROPERTY_KEY, CENTERED, LOCALIZATION_KEY, WidgetLabelAlignment::getLocalizationKey);
    }

    /**
     * Returns the alignment of a widget that has its own alignment and also follows a global one.<br>
     * Centered is the default of both, so the own alignment only wins when it is not centered.
     */
    @NotNull
    public static WidgetLabelAlignment resolve(@NotNull WidgetLabelAlignment ownAlignment, @NotNull WidgetLabelAlignment globalAlignment) {
        return (ownAlignment != CENTERED) ? ownAlignment : globalAlignment;
    }

    /**
     * Returns the alignment with the given serialized name, ignoring case, or {@link #CENTERED} for unknown or missing names.
     */
    @NotNull
    public static WidgetLabelAlignment getBySerializedName(@Nullable String serializedName) {
        if (serializedName == null) return CENTERED;
        String trimmedName = serializedName.trim();
        for (WidgetLabelAlignment alignment : VALUES) {
            if (alignment.serializedName.equalsIgnoreCase(trimmedName)) return alignment;
        }
        return CENTERED;
    }

    /**
     * Returns the left X of a label that fits into the room between minX and maxX.<br>
     * Left and right labels keep {@link #SIDE_PADDING} away from their side, as long as they fit into the room with it.<br>
     * Centered labels use the same math as Vanilla's widget labels, so they don't move by a pixel compared to Vanilla.
     */
    public int getLabelX(int minX, int maxX, int labelWidth) {
        if (this == LEFT) return Math.max(minX, Math.min(minX + SIDE_PADDING, maxX - labelWidth));
        if (this == RIGHT) return Math.max(minX, maxX - SIDE_PADDING - labelWidth);
        int halfWidth = labelWidth / 2;
        return Math.min(Math.max((minX + maxX) / 2, minX + halfWidth), maxX - halfWidth) - halfWidth;
    }

    /**
     * Returns the left X of a label that fits into the room between minX and maxX, for labels that get positioned with sub-pixel precision.
     *
     * @param renderScale The scale the label gets rendered with, when the room is in the space of that scale, so the side padding stays the same on the screen.
     */
    public float getLabelX(float minX, float maxX, float labelWidth, float renderScale) {
        float sidePadding = (renderScale > 0.0F) ? SIDE_PADDING / renderScale : SIDE_PADDING;
        if (this == LEFT) return Math.max(minX, Math.min(minX + sidePadding, maxX - labelWidth));
        if (this == RIGHT) return Math.max(minX, maxX - sidePadding - labelWidth);
        return ((minX + maxX) / 2F) - (labelWidth / 2F);
    }

    @NotNull
    public String getSerializedName() {
        return this.serializedName;
    }

    @NotNull
    public String getLocalizationKey() {
        return this.localizationKey;
    }

}
