package de.keksuccino.fancymenu.customization.loadingrequirement.internal;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("all")
public class LoadingRequirementGroup implements ValuePlaceholderHolder {

    public LoadingRequirementContainer parent;
    public String identifier;
    public GroupMode mode;
    /**
     * Placeholders do not get serialized, but get copied when calling copy().
     * They get added at runtime, mostly after creating a new {@link LoadingRequirementInstance}.
     */
    protected final Map<String, Supplier<String>> valuePlaceholders = new HashMap<>();
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
            this.valuePlaceholders.forEach(instance::addValuePlaceholder);
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

    /**
     * Value placeholders are for replacing parts of the {@link LoadingRequirementInstance#value}.<br>
     * All placeholders added to groups get automatically added to its child {@link LoadingRequirementInstance}s.<br><br>
     *
     * Placeholders use the $$ prefix, but don't include this prefix in the placeholder name.
     *
     * @param placeholder The placeholder base. Should be all lowercase with no special chars or spaces. Use only [a-z], [0-9], [_], [-].
     * @param replaceWithSupplier The supplier that returns the actual value this placeholder should get replaced with.
     */
    public void addValuePlaceholder(@NotNull String placeholder, @NotNull Supplier<String> replaceWithSupplier) {
        if (!CharacterFilter.buildResourceNameFilter().isAllowedText(placeholder)) {
            throw new RuntimeException("Illegal characters used in placeholder name! Use only [a-z], [0-9], [_], [-]!");
        }
        this.valuePlaceholders.put(placeholder, replaceWithSupplier);
        for (LoadingRequirementInstance i : this.instances) {
            i.addValuePlaceholder(placeholder, replaceWithSupplier);
        }
    }

    @NotNull
    @Override
    public Map<String, Supplier<String>> getValuePlaceholders() {
        return this.valuePlaceholders;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (o instanceof LoadingRequirementGroup other) {
            if (!Objects.equals(this.identifier, other.identifier)) return false;
            if (this.mode != other.mode) return false;
            if (!ListUtils.contentEqualIgnoreOrder(this.instances, other.instances)) return false;
            return true;
        }
        return false;
    }

    public LoadingRequirementGroup copy(boolean unique) {
        LoadingRequirementGroup g = new LoadingRequirementGroup(unique ? ScreenCustomization.generateUniqueIdentifier() : this.identifier, this.mode, null);
        this.instances.forEach((instance) -> {
            LoadingRequirementInstance i = instance.copy(unique);
            i.group = g;
            g.instances.add(i);
        });
        g.valuePlaceholders.putAll(this.valuePlaceholders);
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
