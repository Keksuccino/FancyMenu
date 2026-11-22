package de.keksuccino.fancymenu.customization.variables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The kind of value a variable holds.
 *
 * <p>Every variable also has a text form, which is what everything that only knows text gets ({@code $$name} references,
 * the Get Variable placeholder, the Is Variable Value requirement, listeners, commands). For lists, that text form is a JSON
 * array of strings, see {@link VariableLists#toText(java.util.List)}.</p>
 */
public enum VariableType {

    TEXT("text"),
    LIST("list");

    @NotNull
    private final String serializedName;

    VariableType(@NotNull String serializedName) {
        this.serializedName = serializedName;
    }

    /** The name of the type in the variable database. Text variables don't write it, so databases of older versions stay valid. */
    @NotNull
    public String getSerializedName() {
        return this.serializedName;
    }

    @Nullable
    public static VariableType getBySerializedName(@Nullable String serializedName) {
        if (serializedName == null) return null;
        for (VariableType type : values()) {
            if (type.serializedName.equals(serializedName)) return type;
        }
        return null;
    }

}
