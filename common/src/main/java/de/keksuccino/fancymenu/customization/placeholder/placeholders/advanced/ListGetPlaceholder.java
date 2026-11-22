package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.customization.variables.VariableLists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Gets the entry of a list variable at an index. Indexes start at 0, and negative ones count from the end (see {@link VariableLists}).
 * Missing variables, text variables and indexes without an entry give the {@code fallback} value, which is empty by default.
 */
public class ListGetPlaceholder extends AbstractListVariablePlaceholder {

    private static final String INDEX_VALUE = "index";
    private static final String FALLBACK_VALUE = "fallback";

    public ListGetPlaceholder() {
        this(VariableHandler::getListEntries);
    }

    ListGetPlaceholder(@NotNull Function<String, List<String>> listLookup) {
        super("list_get", "fancymenu.placeholders.list_get", listLookup);
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String fallback = Objects.requireNonNullElse(dps.values.get(FALLBACK_VALUE), "");
        List<String> entries = this.getListEntries(dps);
        if (entries == null) return fallback;
        Integer index = VariableLists.parseIndex(dps.values.get(INDEX_VALUE));
        if (index == null) return fallback;
        int resolved = VariableLists.resolveEntryIndex(index, entries.size());
        return (resolved >= 0) ? entries.get(resolved) : fallback;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of(NAME_VALUE, INDEX_VALUE, FALLBACK_VALUE);
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(NAME_VALUE, DEFAULT_LIST_NAME);
        values.put(INDEX_VALUE, "0");
        values.put(FALLBACK_VALUE, "");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
