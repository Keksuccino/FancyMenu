

//Copyright (c) 2022 Keksuccino.
//This code is licensed under DSMSL.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.menu.placeholder.v2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class DeserializedPlaceholderString {

    public String placeholder;
    public Map<String, String> values;
    public String originalString;

    public DeserializedPlaceholderString() {
        this.values = new LinkedHashMap<>();
    }

    public String toString() {
        if ((this.values != null) && !this.values.isEmpty()) {
            String values = "";
            for (Map.Entry<String, String> m : this.values.entrySet()) {
                if (values.length() > 0) {
                    values += ",";
                }
                values += "\"" + m.getKey() + "\":\"" + m.getValue() + "\"";
            }
            return "{\"placeholder\":\"" + this.placeholder + "\",\"values\":{" + values + "}}";
        } else {
            return "{\"placeholder\":\"" + this.placeholder + "\"}";
        }
    }

    public static DeserializedPlaceholderString build(@NotNull String placeholderId, @Nullable Map<String, String> values) {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = placeholderId;
        dps.values = values;
        if (dps.values == null) {
            dps.values = new LinkedHashMap<>();
        }
        return dps;
    }

}
