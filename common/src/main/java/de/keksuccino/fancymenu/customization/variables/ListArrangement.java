package de.keksuccino.fancymenu.customization.variables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The ways the entries of a list variable can get rearranged, see {@link VariableLists#arrange(java.util.List, ListArrangement, java.util.Random)}.
 */
public enum ListArrangement {

    /** Alphabetical, with accented letters next to the letters they are based on. Case only decides between entries that are otherwise the same. */
    SORT_ASCENDING("sort_ascending"),
    /** The reverse of {@link #SORT_ASCENDING}. */
    SORT_DESCENDING("sort_descending"),
    /** Smallest number first. Entries that are no numbers go to the end, in the order they had. */
    SORT_NUMBERS_ASCENDING("sort_numbers_ascending"),
    /** Biggest number first. Entries that are no numbers go to the end, in the order they had. */
    SORT_NUMBERS_DESCENDING("sort_numbers_descending"),
    REVERSE("reverse"),
    SHUFFLE("shuffle"),
    /** Keeps the first entry of every value. */
    REMOVE_DUPLICATES("remove_duplicates");

    @NotNull
    private final String serializedName;

    ListArrangement(@NotNull String serializedName) {
        this.serializedName = serializedName;
    }

    @NotNull
    public String getSerializedName() {
        return this.serializedName;
    }

    @Nullable
    public static ListArrangement getBySerializedName(@Nullable String serializedName) {
        if (serializedName == null) return null;
        for (ListArrangement arrangement : values()) {
            if (arrangement.serializedName.equals(serializedName)) return arrangement;
        }
        return null;
    }

}
