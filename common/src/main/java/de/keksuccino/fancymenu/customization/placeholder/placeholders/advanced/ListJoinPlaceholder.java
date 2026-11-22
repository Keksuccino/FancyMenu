package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/**
 * Joins the entries of a list variable with the {@code separator} value, to show the whole list as text. Missing variables and text
 * variables give an empty text. The separator is optional and {@code ", "} by default, and {@code \n} in it becomes a line break, like in
 * the separator of the File Text placeholder.
 */
public class ListJoinPlaceholder extends AbstractListVariablePlaceholder {

    private static final String SEPARATOR_VALUE = "separator";
    private static final String DEFAULT_SEPARATOR = ", ";
    private static final String LINE_BREAK_CODE = "\\n";
    private static final String LINE_BREAK = "\n";

    public ListJoinPlaceholder() {
        this(VariableHandler::getListEntries);
    }

    ListJoinPlaceholder(@NotNull Function<String, List<String>> listLookup) {
        super("list_join", "fancymenu.placeholders.list_join", listLookup);
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        List<String> entries = this.getListEntries(dps);
        if ((entries == null) || entries.isEmpty()) return "";
        // One entry needs no separator, so it gets returned as it is
        if (entries.size() == 1) return entries.get(0);
        String separator = dps.values.get(SEPARATOR_VALUE);
        separator = (separator != null) ? separator.replace(LINE_BREAK_CODE, LINE_BREAK) : DEFAULT_SEPARATOR;
        return String.join(separator, entries);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of(NAME_VALUE, SEPARATOR_VALUE);
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(NAME_VALUE, DEFAULT_LIST_NAME);
        values.put(SEPARATOR_VALUE, DEFAULT_SEPARATOR);
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
