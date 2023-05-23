package de.keksuccino.fancymenu.customization.loadingrequirement.internal;

import de.keksuccino.fancymenu.properties.PropertyContainer;
import de.keksuccino.fancymenu.utils.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LoadingRequirementContainer {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final List<LoadingRequirementGroup> groups = new ArrayList<>();
    protected final List<LoadingRequirementInstance> instances = new ArrayList<>();
    public boolean forceRequirementsMet = false;
    public boolean forceRequirementsNotMet = false;

    public boolean requirementsMet() {
        if (this.forceRequirementsMet) {
            return true;
        }
        if (this.forceRequirementsNotMet) {
            return false;
        }
        try {
            for (LoadingRequirementGroup g : this.groups) {
                if (!g.requirementsMet()) {
                    return false;
                }
            }
            for (LoadingRequirementInstance i : this.instances) {
                if (!i.requirementMet()) {
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Error while checking LoadingRequirements of LoadingRequirementContainer!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Nullable
    public LoadingRequirementGroup createAndAddGroup(@NotNull String identifier, @NotNull LoadingRequirementGroup.GroupMode mode) {
        if (!this.groupExists(identifier)) {
            LoadingRequirementGroup g = new LoadingRequirementGroup(identifier, mode, this);
            this.groups.add(g);
            return g;
        }
        return null;
    }

    public boolean addGroup(LoadingRequirementGroup group) {
        if (!this.groupExists(group.identifier)) {
            this.groups.add(group);
            return true;
        }
        return false;
    }

    public List<LoadingRequirementGroup> getGroups() {
        return new ArrayList<>(this.groups);
    }

    @Nullable
    public LoadingRequirementGroup getGroup(String identifier) {
        for (LoadingRequirementGroup g : this.groups) {
            if (g.identifier.equals(identifier)) {
                return g;
            }
        }
        return null;
    }

    public boolean groupExists(String identifier) {
        return this.getGroup(identifier) != null;
    }

    public boolean removeGroup(LoadingRequirementGroup group) {
        return this.groups.remove(group);
    }

    public boolean removeGroupByIdentifier(String identifier) {
        LoadingRequirementGroup g = this.getGroup(identifier);
        if (g != null) {
            return this.groups.remove(g);
        }
        return false;
    }

    public boolean addInstance(LoadingRequirementInstance instance) {
        if (!this.instances.contains(instance)) {
            this.instances.add(instance);
            return true;
        }
        return false;
    }

    public boolean removeInstance(LoadingRequirementInstance instance) {
        return this.instances.remove(instance);
    }

    public List<LoadingRequirementInstance> getInstances() {
        return new ArrayList<>(this.instances);
    }

    public void serializeContainerToExistingPropertiesSection(@NotNull PropertyContainer target) {
        PropertyContainer sec = serializeRequirementContainer(this);
        for (Map.Entry<String, String> m : sec.getProperties().entrySet()) {
            target.putProperty(m.getKey(), m.getValue());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (o instanceof LoadingRequirementContainer other) {
            if (!ListUtils.contentEquals(this.groups, other.groups)) return false;
            if (!ListUtils.contentEquals(this.instances, other.instances)) return false;
            return true;
        }
        return false;
    }

    public LoadingRequirementContainer copy() {
        LoadingRequirementContainer c = new LoadingRequirementContainer();
        this.groups.forEach((group) -> {
            LoadingRequirementGroup g = group.copy();
            g.parent = c;
            for (LoadingRequirementInstance i : g.instances) {
                i.parent = c;
            }
            c.groups.add(g);
        });
        this.instances.forEach((instance) -> {
            LoadingRequirementInstance i = instance.copy();
            i.parent = c;
            c.instances.add(i);
        });
        return c;
    }

    @NotNull
    public static PropertyContainer serializeRequirementContainer(LoadingRequirementContainer container) {
        PropertyContainer sec = new PropertyContainer("loading_requirement_container");
        for (LoadingRequirementGroup g : container.groups) {
            PropertyContainer sg = LoadingRequirementGroup.serializeRequirementGroup(g);
            for (Map.Entry<String, String> m : sg.getProperties().entrySet()) {
                sec.putProperty(m.getKey(), m.getValue());
            }
        }
        for (LoadingRequirementInstance i : container.instances) {
            List<String> l = LoadingRequirementInstance.serializeRequirementInstance(i);
            sec.putProperty(l.get(0), l.get(1));
        }
        return sec;
    }

    @NotNull
    public static LoadingRequirementContainer deserializeRequirementContainer(PropertyContainer sec) {
        LoadingRequirementContainer c = new LoadingRequirementContainer();
        for (Map.Entry<String, String> m : sec.getProperties().entrySet()) {
            if (m.getKey().startsWith("[loading_requirement_group:")) {
                LoadingRequirementGroup g = LoadingRequirementGroup.deserializeRequirementGroup(m.getKey(), m.getValue(), c);
                if (g != null) {
                    c.addGroup(g);
                }
            }
        }
        for (Map.Entry<String, String> m : sec.getProperties().entrySet()) {
            if (m.getKey().startsWith("[loading_requirement:")) {
                LoadingRequirementInstance i = LoadingRequirementInstance.deserializeRequirementInstance(m.getKey(), m.getValue(), c);
                if (i != null) {
                    if (i.group != null) {
                        i.group.addInstance(i);
                    } else {
                        c.addInstance(i);
                    }
                }
            }
        }
        return c;
    }

}
