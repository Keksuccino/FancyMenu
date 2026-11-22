package de.keksuccino.fancymenu.customization.variables;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Immutable, same-generation view of one user variable.
 *
 * @param value The text form of the variable. For lists, it is the text form of the entries (see {@link VariableLists#toText(List)}).
 * @param listEntries The entries of a list variable. Always empty for text variables.
 */
public record UserVariableSnapshot(@NotNull String name, @NotNull String value, boolean resetOnLaunch, @NotNull VariableType type, @NotNull List<String> listEntries) {

    public UserVariableSnapshot {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        Objects.requireNonNull(type);
        // Copies nothing when the entries already are an immutable list, which is what the store hands out
        listEntries = List.copyOf(listEntries);
        if ((type == VariableType.TEXT) && !listEntries.isEmpty()) throw new IllegalArgumentException("Text variable snapshots can't have list entries: " + name);
    }

    /** Creates the snapshot of a text variable. */
    public UserVariableSnapshot(@NotNull String name, @NotNull String value, boolean resetOnLaunch) {
        this(name, value, resetOnLaunch, VariableType.TEXT, List.of());
    }

    /** Creates the snapshot of a list variable. */
    @NotNull
    public static UserVariableSnapshot ofList(@NotNull String name, @NotNull List<String> entries, boolean resetOnLaunch) {
        List<String> immutableEntries = List.copyOf(entries);
        return new UserVariableSnapshot(name, VariableLists.toText(immutableEntries), resetOnLaunch, VariableType.LIST, immutableEntries);
    }

    public boolean isList() {
        return this.type == VariableType.LIST;
    }

}
