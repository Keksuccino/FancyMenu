package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public final class WidgetTooltipText {

    private WidgetTooltipText() {}

    /**
     * An exactly empty editor value clears an optional tooltip. Space-only input stays invalid to
     * preserve the validation behavior used by widget tooltips before empty values became clearable.
     */
    public static boolean isEditorValueValid(@Nullable String value) {
        return value != null && (value.isEmpty() || TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR.get(value));
    }

    @Nullable
    public static String toEditorValue(@Nullable String value) {
        value = normalizeStoredValue(value);
        return value != null ? value.replace("%n%", "\n") : null;
    }

    @Nullable
    public static String fromEditorValue(@Nullable String value) {
        value = normalizeStoredValue(value);
        return value != null ? value.replace("\n", "%n%") : null;
    }

    /**
     * Prepares literal widget tooltip text at its rendering boundary. Formatting conversion intentionally happens
     * after placeholder replacement so formatting codes returned by dynamic placeholders are handled as well.
     * Newline decoding stays with each widget because the supported stored codes differ between tooltip renderers.
     */
    public static @NotNull String replacePlaceholdersAndFormattingCodes(@NotNull String value) {
        String prepared = PlaceholderParser.replacePlaceholders(value);
        if (prepared.indexOf('&') < 0) return prepared;
        return TextFormattingUtils.replaceFormattingCodes(prepared, "&", "§");
    }

    @Nullable
    public static String normalizeStoredValue(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
    }

}
