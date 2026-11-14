package de.keksuccino.fancymenu.customization.placeholder.placeholders.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollAreaElement;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A placeholder for one of the numbers of a Scrollable Area element that belong to one of its two axes:
 * how far its content is scrolled, in pixels or in percent, and how big its content is.<br>
 * All of them take the identifier of the area and the axis. Without an axis, they answer for the one the area mainly scrolls on.
 */
public class ScrollAreaValuePlaceholder extends Placeholder {

    private static final String AXIS_VERTICAL = "vertical";
    private static final String AXIS_HORIZONTAL = "horizontal";

    @NotNull
    private final String localizationKey;
    @NotNull
    private final ValueReader valueReader;
    @NotNull
    private final Function<String, AbstractElement> elementLookup;

    /** How far the content is scrolled from its start, in pixels. */
    @NotNull
    public static ScrollAreaValuePlaceholder scrollPosition() {
        return scrollPosition(AbstractElement::getElementByInstanceIdentifier);
    }

    @NotNull
    static ScrollAreaValuePlaceholder scrollPosition(@NotNull Function<String, AbstractElement> elementLookup) {
        return new ScrollAreaValuePlaceholder("scroll_area_scroll_position", "fancymenu.placeholders.scroll_area_scroll_position", (area, horizontal) -> Math.round(area.getScroller().axis(horizontal).getPosition()), elementLookup);
    }

    /** How far the content is scrolled, from 0 at its start to 100 at its end. */
    @NotNull
    public static ScrollAreaValuePlaceholder scrollPercent() {
        return scrollPercent(AbstractElement::getElementByInstanceIdentifier);
    }

    @NotNull
    static ScrollAreaValuePlaceholder scrollPercent(@NotNull Function<String, AbstractElement> elementLookup) {
        return new ScrollAreaValuePlaceholder("scroll_area_scroll_percent", "fancymenu.placeholders.scroll_area_scroll_percent", (area, horizontal) -> area.getScroller().axis(horizontal).getPercent(), elementLookup);
    }

    /** How big the content is, in pixels at the size the content has without the area scaling it. */
    @NotNull
    public static ScrollAreaValuePlaceholder contentSize() {
        return contentSize(AbstractElement::getElementByInstanceIdentifier);
    }

    @NotNull
    static ScrollAreaValuePlaceholder contentSize(@NotNull Function<String, AbstractElement> elementLookup) {
        return new ScrollAreaValuePlaceholder("scroll_area_content_size", "fancymenu.placeholders.scroll_area_content_size", (area, horizontal) -> horizontal ? area.getContentWidth() : area.getContentHeight(), elementLookup);
    }

    /**
     * @param localizationKey The key of the name of the placeholder. The key of its description is this one with ".desc" behind it.
     */
    private ScrollAreaValuePlaceholder(@NotNull String identifier, @NotNull String localizationKey, @NotNull ValueReader valueReader, @NotNull Function<String, AbstractElement> elementLookup) {
        super(identifier);
        this.localizationKey = Objects.requireNonNull(localizationKey);
        this.valueReader = Objects.requireNonNull(valueReader);
        this.elementLookup = Objects.requireNonNull(elementLookup);
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String elementIdentifier = dps.values.get("element_identifier");
        if ((elementIdentifier == null) || elementIdentifier.isBlank()) return "0";
        if (!(this.elementLookup.apply(elementIdentifier) instanceof ScrollAreaElement area)) return "0";
        String axis = dps.values.get("axis");
        boolean horizontal = AXIS_HORIZONTAL.equalsIgnoreCase(axis) || (!AXIS_VERTICAL.equalsIgnoreCase(axis) && !area.getScrollDirection().scrollsY());
        double value = this.valueReader.read(area, horizontal);
        // Whole numbers go out without a point, so they can be used wherever a whole number is asked for
        return (value == Math.rint(value)) ? String.valueOf((long) value) : String.valueOf(value);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("element_identifier", "axis");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get(this.localizationKey);
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines(this.localizationKey + ".desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.gui");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("element_identifier", "some.element.id");
        values.put("axis", AXIS_VERTICAL);
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

    /**
     * Reads the number of a placeholder from an area.
     */
    @FunctionalInterface
    private interface ValueReader {

        /**
         * @param horizontal If the number is the one of the horizontal axis. Otherwise it is the one of the vertical axis.
         */
        double read(@NotNull ScrollAreaElement area, boolean horizontal);

    }

}
