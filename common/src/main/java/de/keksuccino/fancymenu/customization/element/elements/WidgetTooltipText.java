package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.util.input.TextValidators;
import org.jetbrains.annotations.Nullable;

public final class WidgetTooltipText {

    private WidgetTooltipText() {
    }

    /**
     * An exactly empty editor value clears an optional tooltip. Space-only input stays invalid to preserve the
     * validation behavior used by widget tooltips before empty values became clearable.
     */
    public static boolean isEditorValueValid(@Nullable String value) {
        return value != null && (value.isEmpty() || TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR.get(value));
    }

    public static @Nullable String toEditorValue(@Nullable String value) {
        value = normalizeStoredValue(value);
        return value != null ? value.replace("%n%", "\n") : null;
    }

    public static @Nullable String fromEditorValue(@Nullable String value) {
        value = normalizeStoredValue(value);
        return value != null ? value.replace("\n", "%n%") : null;
    }

    public static @Nullable String normalizeStoredValue(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
    }

}
