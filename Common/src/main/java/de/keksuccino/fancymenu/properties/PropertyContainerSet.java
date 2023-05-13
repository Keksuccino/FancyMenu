package de.keksuccino.fancymenu.properties;

import java.util.ArrayList;
import java.util.List;

public class PropertyContainerSet {

    private String type;
    private final List<PropertyContainer> containers = new ArrayList<>();

    public PropertyContainerSet(String type) {
        this.type = type;
    }

    public void putContainer(PropertyContainer data) {
        this.containers.add(data);
    }

    public List<PropertyContainer> getContainers() {
        return this.containers;
    }

    public List<PropertyContainer> getSectionsOfType(String type) {
        List<PropertyContainer> sections = new ArrayList<>();
        for (PropertyContainer sec : this.containers) {
            if (sec.getType().equals(type)) {
                sections.add(sec);
            }
        }
        return sections;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
