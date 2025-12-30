//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class DeserializedPlaceholderString {

    @NotNull
    public String placeholderIdentifier;
    /**
     * This map should never be NULL, but can be EMPTY, especially if the placeholder has no values.
     */
    @NotNull
    public HashMap<String, String> values = new LinkedHashMap<>();
    @NotNull
    public String placeholderString;

    @Deprecated(forRemoval = true)
    public static DeserializedPlaceholderString build(@NotNull String placeholderIdentifier, @Nullable Map<String, String> values) {
        if ((values != null) && !(values instanceof HashMap<String, String>)) throw new RuntimeException("Values list has to be a HashMap!");
        return new DeserializedPlaceholderString(placeholderIdentifier, (HashMap<String, String>) values, "");
    }

    @Deprecated(forRemoval = true)
    public DeserializedPlaceholderString() {
        this("", null, "");
    }

    public DeserializedPlaceholderString(@NotNull String placeholderIdentifier, @Nullable HashMap<String, String> values, @NotNull String placeholderString) {
        this.placeholderIdentifier = Objects.requireNonNull(placeholderIdentifier);
        if (values != null) this.values = values;
        this.placeholderString = Objects.requireNonNull(placeholderString);
    }

    @NotNull
    public String toString() {
        if (!this.values.isEmpty()) {
            StringBuilder values = new StringBuilder();
            for (Map.Entry<String, String> m : this.values.entrySet()) {
                if (values.length() > 0) {
                    values.append(",");
                }
                values.append("\"").append(m.getKey()).append("\":\"").append(m.getValue()).append("\"");
            }
            return "{\"placeholder\":\"" + this.placeholderIdentifier + "\",\"values\":{" + values + "}}";
        } else {
            return "{\"placeholder\":\"" + this.placeholderIdentifier + "\"}";
        }
    }

}
