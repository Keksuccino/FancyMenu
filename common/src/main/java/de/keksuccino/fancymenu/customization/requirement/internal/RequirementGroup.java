package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Supplier;

/**
 * A group of requirements that acts like one big requirement. Groups can also contain other groups, to build compound logic
 * like {@code (A AND B) OR (C AND D)}.<br><br>
 *
 * Group identifiers need to be unique in the whole group tree of a {@link RequirementContainer}, including all nested groups,
 * because serialized groups and requirements reference their group by its identifier.
 */
@SuppressWarnings("all")
public class RequirementGroup implements ValuePlaceholderHolder {

    public RequirementContainer parent;
    /** The group this group is nested in, if it is nested in one. */
    @Nullable
    public RequirementGroup parentGroup;
    public String identifier;
    public GroupMode mode;
    /**
     * Placeholders do not get serialized, but get copied when calling copy().
     * They get added at runtime, mostly after creating a new {@link RequirementInstance}.
     */
    protected final Map<String, Supplier<String>> valuePlaceholders = new HashMap<>();
    protected final List<RequirementInstance> instances = new ArrayList<>();
    protected final List<RequirementGroup> groups = new ArrayList<>();

    public RequirementGroup(@NotNull String identifier, @NotNull GroupMode mode, @Nullable RequirementContainer parent) {
        this.parent = parent;
        this.identifier = identifier;
        this.mode = mode;
    }

    /**
     * Requirements get checked before nested groups, because a single requirement is usually cheaper to check than a whole group,
     * so the result is often known before any nested group needs to get checked.
     */
    public boolean requirementsMet() {
        boolean orMode = (this.mode == GroupMode.OR);
        // In OR mode, the first met child decides the result. In AND mode, the first child that is not met decides it.
        for (RequirementInstance i : this.instances) {
            if (i.requirementMet() == orMode) return orMode;
        }
        for (RequirementGroup g : this.groups) {
            if (g.requirementsMet() == orMode) return orMode;
        }
        return !orMode;
    }

    public void addInstance(RequirementInstance instance) {
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
    public RequirementInstance removeInstance(RequirementInstance instance) {
        instance.group = null;
        return this.instances.remove(instance) ? instance : null;
    }

    public List<RequirementInstance> getInstances() {
        return new ArrayList<>(this.instances);
    }

    /**
     * Nests a group in this group.
     *
     * @return FALSE if the group is already nested in a group, if its identifier is already used in the group tree of this group,
     *         or if nesting it would make a group contain itself.
     */
    public boolean addGroup(@NotNull RequirementGroup group) {
        Objects.requireNonNull(group);
        if ((group.parentGroup != null) || this.isSameOrNestedIn(group) || this.isGroupIdentifierUsedInTree(group.identifier)) {
            return false;
        }
        this.groups.add(group);
        group.parentGroup = this;
        group.setParentContainer(this.parent);
        this.valuePlaceholders.forEach(group::addValuePlaceholder);
        return true;
    }

    public boolean removeGroup(@NotNull RequirementGroup group) {
        if (this.groups.remove(Objects.requireNonNull(group))) {
            group.parentGroup = null;
            return true;
        }
        return false;
    }

    /**
     * @return The groups nested directly in this group.
     */
    public List<RequirementGroup> getGroups() {
        return new ArrayList<>(this.groups);
    }

    /**
     * @return All groups nested in this group, at any depth.
     */
    @NotNull
    public List<RequirementGroup> getAllGroups() {
        List<RequirementGroup> allGroups = new ArrayList<>();
        collectGroups(this.groups, allGroups);
        return allGroups;
    }

    /**
     * Searches the groups nested in this group, at any depth.
     */
    @Nullable
    public RequirementGroup getGroup(@NotNull String identifier) {
        for (RequirementGroup g : this.groups) {
            if (g.identifier.equals(identifier)) return g;
            RequirementGroup nested = g.getGroup(identifier);
            if (nested != null) return nested;
        }
        return null;
    }

    /**
     * @return TRUE if this group is the given group or is nested in it, at any depth.
     */
    public boolean isSameOrNestedIn(@NotNull RequirementGroup group) {
        for (RequirementGroup g = this; g != null; g = g.parentGroup) {
            if (g == group) return true;
        }
        return false;
    }

    /**
     * Sets the container of this group and of all groups and requirements nested in it.
     */
    public void setParentContainer(@Nullable RequirementContainer container) {
        this.parent = container;
        for (RequirementInstance i : this.instances) {
            i.parent = container;
        }
        for (RequirementGroup g : this.groups) {
            g.setParentContainer(container);
        }
    }

    private boolean isGroupIdentifierUsedInTree(@NotNull String identifier) {
        if ((this.parent != null) && this.parent.groupExists(identifier)) return true;
        RequirementGroup root = this;
        while (root.parentGroup != null) {
            root = root.parentGroup;
        }
        return root.identifier.equals(identifier) || (root.getGroup(identifier) != null);
    }

    static void collectGroups(@NotNull List<RequirementGroup> groups, @NotNull List<RequirementGroup> target) {
        for (RequirementGroup g : groups) {
            target.add(g);
            collectGroups(g.groups, target);
        }
    }

    /**
     * Value placeholders are for replacing parts of the {@link RequirementInstance#value}.<br>
     * All placeholders added to groups get automatically added to its child {@link RequirementInstance}s and nested groups.<br><br>
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
        for (RequirementInstance i : this.instances) {
            i.addValuePlaceholder(placeholder, replaceWithSupplier);
        }
        for (RequirementGroup g : this.groups) {
            g.addValuePlaceholder(placeholder, replaceWithSupplier);
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
        if (o instanceof RequirementGroup other) {
            if (!Objects.equals(this.identifier, other.identifier)) return false;
            if (this.mode != other.mode) return false;
            if (!ListUtils.contentEqualIgnoreOrder(this.instances, other.instances)) return false;
            if (!ListUtils.contentEqualIgnoreOrder(this.groups, other.groups)) return false;
            return true;
        }
        return false;
    }

    /**
     * Copies this group with all its requirements and nested groups. The copy is not part of any container or group.
     */
    public RequirementGroup copy(boolean unique) {
        RequirementGroup g = new RequirementGroup(unique ? ScreenCustomization.generateUniqueIdentifier() : this.identifier, this.mode, null);
        this.instances.forEach((instance) -> {
            RequirementInstance i = instance.copy(unique);
            i.group = g;
            g.instances.add(i);
        });
        this.groups.forEach((group) -> {
            RequirementGroup nested = group.copy(unique);
            nested.parentGroup = g;
            g.groups.add(nested);
        });
        g.valuePlaceholders.putAll(this.valuePlaceholders);
        return g;
    }

    /**
     * Serializes the group with all its requirements and nested groups. Nested groups store the identifier of their
     * parent group, so they can get nested again when deserializing them.
     */
    @NotNull
    public static PropertyContainer serializeRequirementGroup(@NotNull RequirementGroup group) {
        PropertyContainer sec = new PropertyContainer("requirement_group");
        serializeRequirementGroupTo(group, null, sec);
        return sec;
    }

    private static void serializeRequirementGroupTo(@NotNull RequirementGroup group, @Nullable RequirementGroup parentGroup, @NotNull PropertyContainer target) {
        String key = "[loading_requirement_group:" + group.identifier + "]";
        String value = "[group_mode:" + group.mode.name + "]";
        if (parentGroup != null) {
            value += "[parent_group:" + parentGroup.identifier + "]";
        }
        target.putProperty(key, value);
        for (RequirementInstance i : group.instances) {
            i.group = group;
            List<String> l = RequirementInstance.serializeRequirementInstance(i);
            target.putProperty(l.get(0), l.get(1));
        }
        for (RequirementGroup g : group.groups) {
            serializeRequirementGroupTo(g, group, target);
        }
    }

    /**
     * Deserializes the group itself, without its requirements and nested groups.
     */
    @Nullable
    public static RequirementGroup deserializeRequirementGroup(@NotNull String key, @NotNull String value, @NotNull RequirementContainer parent) {
        if (key.startsWith("[loading_requirement_group:")) {
            String groupId = key.split("\\[loading_requirement_group:", 2)[1].split("\\]", 2)[0];
            if (value.startsWith("[group_mode:")) {
                String modeString = value.split("\\[group_mode:", 2)[1].split("\\]", 2)[0];
                GroupMode mode = GroupMode.getByName(modeString);
                if (mode != null) {
                    return new RequirementGroup(groupId, mode, parent);
                }
            }
        }
        return null;
    }

    /**
     * @return The identifier of the group a serialized group is nested in, or NULL if it is not nested in a group.
     */
    @Nullable
    public static String deserializeParentGroupIdentifier(@NotNull String value) {
        if (!value.contains("[parent_group:")) return null;
        String parentGroupId = value.split("\\[parent_group:", 2)[1].split("\\]", 2)[0];
        return parentGroupId.isBlank() ? null : parentGroupId;
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
