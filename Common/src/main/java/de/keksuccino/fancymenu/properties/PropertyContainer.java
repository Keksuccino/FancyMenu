package de.keksuccino.fancymenu.properties;

import java.util.LinkedHashMap;
import java.util.Map;

public class PropertyContainer {

    private String type;
    private final Map<String, String> entries = new LinkedHashMap<>();

    public PropertyContainer(String type) {
        this.type = type;
    }

    public void putProperty(String name, String value) {
        this.entries.put(name, value);
    }

    public Map<String, String> getProperties() {
        return this.entries;
    }

    public String getValue(String name) {
        return this.entries.get(name);
    }

    public void removeProperty(String name) {
        this.entries.remove(name);
    }

    public boolean hasProperty(String name) {
        return this.entries.containsKey(name);
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
