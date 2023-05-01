package de.keksuccino.fancymenu.api.placeholder;

import de.keksuccino.fancymenu.customization.frontend.PlaceholderInputPopup;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder text container.<br><br>
 *
 * Needs to be registered to the {@link PlaceholderTextRegistry}.
 */
@Deprecated
public abstract class PlaceholderTextContainer {

    private final String placeholderIdentifier;

    @Deprecated
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
    @Deprecated
    public abstract String replacePlaceholders(String rawIn);

    /**
     * Returns the placeholder.<br>
     * Placeholders should always start and end with %.<br><br>
     *
     * <b>Example</b>: {@code %some_placeholder%}
     *
     * @return The placeholder.
     */
    @Deprecated
    public abstract String getPlaceholder();

    /**
     * Returns the category of this placeholder.<br>
     * Placeholders get categorized in the {@link PlaceholderInputPopup}.<br><br>
     *
     * When returning <b>NULL</b>, the placeholder will be put into the "Other" category.<br><br>
     *
     * You can localize the category name here.
     *
     * @return The category of the placeholder.
     */
    @Deprecated
    public abstract String getCategory();

    /**
     * The display name of this placeholder.<br>
     * Used in the editor.<br><br>
     *
     * You can localize the display name here.
     *
     * @return The display name.
     */
    @Deprecated
    public abstract String getDisplayName();

    /**
     * The description of this placeholder.<br>
     * Is displayed when hovering over the button for this placeholder in the {@link PlaceholderInputPopup}.<br><br>
     *
     * Every string in the returned string array is one line of text in the button tooltip.
     *
     * @return The placeholder description.
     */
    @Deprecated
    public abstract String[] getDescription();

    /**
     * @return The unique identifier of the placeholder.
     */
    @Deprecated
    public String getIdentifier() {
        return this.placeholderIdentifier;
    }

    /**
     * Will find and return all placeholders with a value behind it's base prefix.<br><br>
     *
     * An example would be "%placeholder:value%". The prefix "%placeholder:" is always the same, but<br>
     * the suffix with the value can change. The method returns this placeholder as "%placeholder:value%".
     *
     * @param in The raw string the method should search for the placeholder.
     * @param placeholderBase The prefix of the placeholder. Example: "%placeholder:"
     */
    @Deprecated
    public static List<String> getPlaceholdersWithValue(String in, String placeholderBase) {
        List<String> l = new ArrayList<String>();
        try {
            if (in.contains(placeholderBase)) {
                int index = -1;
                int i = 0;
                while (i < in.length()) {
                    String s = "" + in.charAt(i);
                    if (s.equals("%")) {
                        if (index == -1) {
                            index = i;
                        } else {
                            String sub = in.substring(index, i+1);
                            if (sub.startsWith(placeholderBase) && sub.endsWith("%")) {
                                l.add(sub);
                            }
                            index = -1;
                        }
                    }
                    i++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return l;
    }

    @Deprecated
    public static String getPlaceholderWithoutPercentPrefixSuffix(String placeholderWithPrefixSuffix) {
        return placeholderWithPrefixSuffix.substring(1, placeholderWithPrefixSuffix.length()-1);
    }

}
