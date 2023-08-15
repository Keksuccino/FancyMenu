package de.keksuccino.fancymenu.util.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PropertyContainerSet {

    @NotNull
    private String type;
    @NotNull
    private final List<PropertyContainer> containers = new ArrayList<>();

    public PropertyContainerSet(@NotNull String type) {
        this.type = type;
    }

    public void putContainer(@NotNull PropertyContainer data) {
        this.containers.add(data);
    }

    @NotNull
    public List<PropertyContainer> getContainers() {
        return this.containers;
    }

    @NotNull
    public List<PropertyContainer> getContainersOfType(@NotNull String type) {
        List<PropertyContainer> sections = new ArrayList<>();
        for (PropertyContainer sec : this.containers) {
            if (sec.getType().equals(type)) {
                sections.add(sec);
            }
        }
        return sections;
    }

    @Nullable
    public PropertyContainer getFirstContainerOfType(@NotNull String type) {
        for (PropertyContainer sec : this.containers) {
            if (sec.getType().equals(type)) {
                return sec;
            }
        }
        return null;
    }

    @NotNull
    public String getType() {
        return this.type;
    }

    public void setType(@NotNull String type) {
        this.type = type;
    }

}
