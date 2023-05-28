package de.keksuccino.fancymenu.customization.loadingrequirement.internal;

import de.keksuccino.fancymenu.properties.PropertyContainer;
import de.keksuccino.fancymenu.utils.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LoadingRequirementGroup {

    public LoadingRequirementContainer parent;
    public String identifier;
    public GroupMode mode;
    protected final List<LoadingRequirementInstance> instances = new ArrayList<>();

    public LoadingRequirementGroup(@NotNull String identifier, @NotNull GroupMode mode, @NotNull LoadingRequirementContainer parent) {
        this.parent = parent;
        this.identifier = identifier;
        this.mode = mode;
    }

    public boolean requirementsMet() {
        for (LoadingRequirementInstance i : this.instances) {
            boolean met = i.requirementMet();
            if (met && (this.mode == GroupMode.OR)) {
                return true;
            }
            if (!met && (this.mode == GroupMode.AND)) {
                return false;
            }
        }
        if (this.mode == GroupMode.OR) {
            return false;
        }
        return true;
    }

    public void addInstance(LoadingRequirementInstance instance) {
        if (!this.instances.contains(instance)) {
            this.instances.add(instance);
        }
        instance.group = this;
    }

    /**
     * @return The removed instance or NULL if it was not part of the container.
     */
    @Nullable
    public LoadingRequirementInstance removeInstance(LoadingRequirementInstance instance) {
        instance.group = null;
        return this.instances.remove(instance) ? instance : null;
    }

    public List<LoadingRequirementInstance> getInstances() {
        return new ArrayList<>(this.instances);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (o instanceof LoadingRequirementGroup other) {
            if (!Objects.equals(this.identifier, other.identifier)) return false;
            if (this.mode != other.mode) return false;
            if (!ListUtils.contentEqual(this.instances, other.instances)) return false;
            return true;
        }
        return false;
    }

    public LoadingRequirementGroup copy(boolean copyRequirementInstanceIdentifiers) {
        LoadingRequirementGroup g = new LoadingRequirementGroup(this.identifier, this.mode, null);
        this.instances.forEach((instance) -> {
            LoadingRequirementInstance i = instance.copy(copyRequirementInstanceIdentifiers);
            i.group = g;
            g.instances.add(i);
        });
        return g;
    }

    @NotNull
    public static PropertyContainer serializeRequirementGroup(@NotNull LoadingRequirementGroup group) {
        PropertyContainer sec = new PropertyContainer("requirement_group");
        String key = "[loading_requirement_group:" + group.identifier + "]";
        String value = "[group_mode:" + group.mode.name + "]";
        sec.putProperty(key, value);
        for (LoadingRequirementInstance i : group.instances) {
            i.group = group;
            List<String> l = LoadingRequirementInstance.serializeRequirementInstance(i);
            sec.putProperty(l.get(0), l.get(1));
        }
        return sec;
    }

    @Nullable
    public static LoadingRequirementGroup deserializeRequirementGroup(@NotNull String key, @NotNull String value, @NotNull LoadingRequirementContainer parent) {
        if (key.startsWith("[loading_requirement_group:")) {
            String groupId = key.split("\\[loading_requirement_group:", 2)[1].split("\\]", 2)[0];
            if (value.startsWith("[group_mode:")) {
                String modeString = value.split("\\[group_mode:", 2)[1].split("\\]", 2)[0];
                GroupMode mode = GroupMode.getByName(modeString);
                if (mode != null) {
                    return new LoadingRequirementGroup(groupId, mode, parent);
                }
            }
        }
        return null;
    }

    public enum GroupMode {

        AND("and"),
        OR("or");

        public final String name;

        GroupMode(String name) {
            this.name = name;
        }

        @Nullable
        public static GroupMode getByName(String name) {
            for (GroupMode m : GroupMode.values()) {
                if (m.name.equals(name)) {
                    return m;
                }
            }
            return null;
        }

    }

}
