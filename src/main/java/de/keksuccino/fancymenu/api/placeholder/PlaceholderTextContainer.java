package de.keksuccino.fancymenu.api.placeholder;

import javax.annotation.Nonnull;

/**
 * A placeholder text container.<br><br>
 *
 * Needs to be registered to the {@link PlaceholderTextRegistry}.
 */
public abstract class PlaceholderTextContainer {

    private final String placeholderIdentifier;

    public PlaceholderTextContainer(@Nonnull String uniquePlaceholderIdentifier) {
        this.placeholderIdentifier = uniquePlaceholderIdentifier;
    }

    /**
     * Returns the given string with all placeholders replaced with real values.<br><br>
     *
     * Here you need to search for the placeholder occurrences in the given string and replace it with the real value shown in the actual menu.<br><br>
     *
     * This method gets called every tick for every item with text content in it, so don't do too much performance-killing stuff here!
     *
     * @param rawIn The raw string with placeholders.
     * @return The given string with real values instead of placeholders.
     */
    public abstract String replacePlaceholders(String rawIn);

    /**
     * Returns the placeholder.<br>
     * Placeholders should always start and end with %.<br><br>
     *
     * <b>Example</b>: {@code %some_placeholder%}
     *
     * @return The placeholder.
     */
    public abstract String getPlaceholder();

    /**
     * Returns the category of this placeholder.<br>
     * Placeholders get categorized in the {@link de.keksuccino.fancymenu.menu.fancy.helper.DynamicValueInputPopup}.<br><br>
     *
     * When returning <b>NULL</b>, the placeholder will be put into the "Other" category.<br><br>
     *
     * You can localize the category name here.
     *
     * @return The category of the placeholder.
     */
    public abstract String getCategory();

    /**
     * The display name of this placeholder.<br>
     * Used in the editor.<br><br>
     *
     * You can localize the display name here.
     *
     * @return The display name.
     */
    public abstract String getDisplayName();

    /**
     * The description of this placeholder.<br>
     * Is displayed when hovering over the button for this placeholder in the {@link de.keksuccino.fancymenu.menu.fancy.helper.DynamicValueInputPopup}.<br><br>
     *
     * Every string in the returned string array is one line of text in the button tooltip.
     *
     * @return The placeholder description.
     */
    public abstract String[] getDescription();

    /**
     * @return The unique identifier of the placeholder.
     */
    public String getIdentifier() {
        return this.placeholderIdentifier;
    }

}
