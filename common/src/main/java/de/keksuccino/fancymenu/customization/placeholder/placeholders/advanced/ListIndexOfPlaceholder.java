package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.customization.variables.VariableLists;
import de.keksuccino.fancymenu.util.SerializationHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Gets the index of the first entry of a list variable that matches the {@code value} value, or -1 if no entry matches, also for missing
 * variables and text variables. The {@code ignore_case} value is optional and false by default.
 */
public class ListIndexOfPlaceholder extends AbstractListVariablePlaceholder {

    private static final String VALUE_VALUE = "value";
    private static final String IGNORE_CASE_VALUE = "ignore_case";
    private static final String NOT_FOUND = "-1";

    public ListIndexOfPlaceholder() {
        this(VariableHandler::getListEntries);
    }

    ListIndexOfPlaceholder(@NotNull Function<String, List<String>> listLookup) {
        super("list_index_of", "fancymenu.placeholders.list_index_of", listLookup);
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        List<String> entries = this.getListEntries(dps);
        if ((entries == null) || entries.isEmpty()) return NOT_FOUND;
        String value = Objects.requireNonNullElse(dps.values.get(VALUE_VALUE), "");
        boolean ignoreCase = SerializationHelper.INSTANCE.deserializeBoolean(false, dps.values.get(IGNORE_CASE_VALUE));
        int index = VariableLists.indexOf(entries, value, ignoreCase);
        return (index < 0) ? NOT_FOUND : Integer.toString(index);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of(NAME_VALUE, VALUE_VALUE, IGNORE_CASE_VALUE);
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(NAME_VALUE, DEFAULT_LIST_NAME);
        values.put(VALUE_VALUE, "some_entry");
        values.put(IGNORE_CASE_VALUE, "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
