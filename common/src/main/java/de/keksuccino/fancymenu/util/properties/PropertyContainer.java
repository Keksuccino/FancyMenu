package de.keksuccino.fancymenu.util.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PropertyContainer {

    @NotNull
    private String type;
    @NotNull
    private final Map<String, String> entries = new LinkedHashMap<>();

    public PropertyContainer(@NotNull String type) {
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Adds a property to the property map.<br>
     * Properties with NULLED values will NOT get ADDED to the property map and EXISTING entries with the given name will be REMOVED.
     */
    public void putProperty(@NotNull String name, @Nullable String value) {
        if (value == null) {
            this.removeProperty(name);
            return;
        }
        this.entries.put(Objects.requireNonNull(name), value);
    }

    @NotNull
    public Map<String, String> getProperties() {
        return this.entries;
    }

    @Nullable
    public String getValue(@NotNull String name) {
        return this.entries.get(Objects.requireNonNull(name));
    }

    public void removeProperty(@NotNull String name) {
        this.entries.remove(Objects.requireNonNull(name));
    }

    public boolean hasProperty(@NotNull String name) {
        return this.entries.containsKey(Objects.requireNonNull(name));
    }

    @NotNull
    public String getType() {
        return this.type;
    }

    public void setType(@NotNull String type) {
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public String toString() {
        return "PropertyContainer{" +
                "type='" + type + '\'' +
                ", entries=" + entries +
                '}';
    }

}
