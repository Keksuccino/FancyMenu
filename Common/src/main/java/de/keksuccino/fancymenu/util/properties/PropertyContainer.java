package de.keksuccino.fancymenu.util.properties;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class PropertyContainer {

    @NotNull
    private String type;
    @NotNull
    private final Map<String, String> entries = new LinkedHashMap<>();

    public PropertyContainer(@NotNull String type) {
        this.type = type;
    }

    public void putProperty(@NotNull String name, @NotNull String value) {
        this.entries.put(name, value);
    }

    @NotNull
    public Map<String, String> getProperties() {
        return this.entries;
    }

    public String getValue(@NotNull String name) {
        return this.entries.get(name);
    }

    public void removeProperty(@NotNull String name) {
        this.entries.remove(name);
    }

    public boolean hasProperty(@NotNull String name) {
        return this.entries.containsKey(name);
    }

    @NotNull
    public String getType() {
        return this.type;
    }

    public void setType(@NotNull String type) {
        this.type = type;
    }

}
