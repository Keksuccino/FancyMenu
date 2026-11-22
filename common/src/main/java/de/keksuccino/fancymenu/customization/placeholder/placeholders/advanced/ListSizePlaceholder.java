package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/**
 * Gets the number of entries of a list variable, or 0 for missing variables and text variables.
 */
public class ListSizePlaceholder extends AbstractListVariablePlaceholder {

    private static final String NO_ENTRIES = "0";

    public ListSizePlaceholder() {
        this(VariableHandler::getListEntries);
    }

    ListSizePlaceholder(@NotNull Function<String, List<String>> listLookup) {
        super("list_size", "fancymenu.placeholders.list_size", listLookup);
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        List<String> entries = this.getListEntries(dps);
        return ((entries == null) || entries.isEmpty()) ? NO_ENTRIES : Integer.toString(entries.size());
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of(NAME_VALUE);
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(NAME_VALUE, DEFAULT_LIST_NAME);
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
