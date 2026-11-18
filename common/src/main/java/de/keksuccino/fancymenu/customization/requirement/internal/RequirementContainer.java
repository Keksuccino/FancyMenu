package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("all")
public class RequirementContainer implements ValuePlaceholderHolder {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, Pair<Long, Boolean>> COOLDOWN_CACHE = new HashMap<>();

    protected final List<RequirementGroup> groups = new ArrayList<>();
    protected final List<RequirementInstance> instances = new ArrayList<>();
    /**
     * Placeholders do not get serialized, but get copied when calling copy().
     * They get added at runtime, mostly after creating a new {@link RequirementInstance}.
     */
    protected final Map<String, Supplier<String>> valuePlaceholders = new HashMap<>();
    @NotNull
    public String identifier = ScreenCustomization.generateUniqueIdentifier();
    protected boolean forceRequirementsMet = false;
    protected boolean forceRequirementsNotMet = false;
    protected final String cachingIdentifier = ScreenCustomization.generateUniqueIdentifier();

    public static void clearCooldownCache() {
        COOLDOWN_CACHE.clear();
    }

    public static boolean isCachingRequirements() {
        return FancyMenu.getOptions().requirementCachingDurationMs.getValue() > 0;
    }

    public static long getRequirementCachingDurationMs() {
        return FancyMenu.getOptions().requirementCachingDurationMs.getValue();
    }

    public boolean requirementsMet() {
        long now = System.currentTimeMillis();
        if (isCachingRequirements()) {
            // Use cache if last value update was less than 150ms ago for performance
            if (COOLDOWN_CACHE.containsKey(this.cachingIdentifier) && ((COOLDOWN_CACHE.get(this.cachingIdentifier).getFirst() + getRequirementCachingDurationMs()) > now)) {
                return COOLDOWN_CACHE.get(this.cachingIdentifier).getSecond();
            }
        }
        boolean b = this._requirementsMet();
        if (isCachingRequirements()) {
            COOLDOWN_CACHE.put(this.cachingIdentifier, Pair.of(now, b));
        }
        return b;
    }

    private boolean _requirementsMet() {
        if (this.forceRequirementsMet) {
            return true;
        }
        if (this.forceRequirementsNotMet) {
            return false;
        }
        try {
            for (RequirementGroup g : this.groups) {
                if (!g.requirementsMet()) {
                    return false;
                }
            }
            for (RequirementInstance i : this.instances) {
                if (!i.requirementMet()) {
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Error while checking LoadingRequirements of LoadingRequirementContainer!", e);
            return false;
        }
        return true;
    }

    /**
     * Creates a new top-level group.
     *
     * @return The new group or NULL if a group with this identifier already exists in this container, including nested groups.
     */
    @Nullable
    public RequirementGroup createAndAddGroup(@NotNull String identifier, @NotNull RequirementGroup.GroupMode mode) {
        RequirementGroup g = new RequirementGroup(identifier, mode, this);
        return this.addGroup(g) ? g : null;
    }

    /**
     * Adds a top-level group.
     *
     * @return FALSE if the group is nested in another group, or if its identifier is already used in this container, including nested groups.
     */
    public boolean addGroup(@NotNull RequirementGroup group) {
        if ((group.parentGroup == null) && !this.groupExists(group.identifier)) {
            this.groups.add(group);
            group.setParentContainer(this);
            this.valuePlaceholders.forEach(group::addValuePlaceholder);
            return true;
        }
        return false;
    }

    /**
     * @return The top-level groups of this container, without the groups nested in them.
     */
    public List<RequirementGroup> getGroups() {
        return new ArrayList<>(this.groups);
    }

    /**
     * @return All groups of this container, including the groups nested in other groups.
     */
    @NotNull
    public List<RequirementGroup> getAllGroups() {
        List<RequirementGroup> allGroups = new ArrayList<>();
        RequirementGroup.collectGroups(this.groups, allGroups);
        return allGroups;
    }

    /**
     * Searches all groups of this container, including the groups nested in other groups.
     */
    @Nullable
    public RequirementGroup getGroup(@NotNull String identifier) {
        for (RequirementGroup g : this.groups) {
            if (g.identifier.equals(identifier)) {
                return g;
            }
            RequirementGroup nested = g.getGroup(identifier);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    /**
     * Checks all groups of this container, including the groups nested in other groups.
     */
    public boolean groupExists(@NotNull String identifier) {
        return this.getGroup(identifier) != null;
    }

    /**
     * Removes a top-level group.
     */
    public boolean removeGroup(@NotNull RequirementGroup group) {
        return this.groups.remove(Objects.requireNonNull(group));
    }

    /**
     * Removes a group of this container, also if it is nested in another group.
     */
    public boolean removeGroupByIdentifier(@NotNull String identifier) {
        RequirementGroup g = this.getGroup(identifier);
        if (g != null) {
            return (g.parentGroup != null) ? g.parentGroup.removeGroup(g) : this.groups.remove(g);
        }
        return false;
    }

    public boolean addInstance(@NotNull RequirementInstance instance) {
        if (!this.instances.contains(instance)) {
            this.instances.add(instance);
            this.valuePlaceholders.forEach(instance::addValuePlaceholder);
            return true;
        }
        return false;
    }

    public boolean removeInstance(@NotNull RequirementInstance instance) {
        return this.instances.remove(instance);
    }

    public List<RequirementInstance> getInstances() {
        return new ArrayList<>(this.instances);
    }

    public RequirementContainer forceRequirementsMet(boolean forceMet) {
        this.forceRequirementsMet = forceMet;
        this.forceRequirementsNotMet = false;
        return this;
    }

    public RequirementContainer forceRequirementsNotMet(boolean forceNotMet) {
        this.forceRequirementsNotMet = forceNotMet;
        this.forceRequirementsMet = false;
        return this;
    }

    public boolean isEmpty() {
        return this.groups.isEmpty() && this.instances.isEmpty();
    }

    /**
     * Value placeholders are for replacing parts of the {@link RequirementInstance#value}.<br>
     * All placeholders added to containers get automatically added to its child {@link RequirementGroup}s and
     * {@link RequirementInstance}s.<br><br>
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

    public RequirementContainer addValuePlaceholderToContainer(@NotNull String placeholder, @NotNull Supplier<String> replaceWithSupplier) {
        this.addValuePlaceholder(placeholder, replaceWithSupplier);
        return this;
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
        if (o instanceof RequirementContainer other) {
            if (!Objects.equals(this.identifier, other.identifier)) return false;
            if (!ListUtils.contentEqualIgnoreOrder(this.groups, other.groups)) return false;
            if (!ListUtils.contentEqualIgnoreOrder(this.instances, other.instances)) return false;
            return true;
        }
        return false;
    }

    public RequirementContainer copy(boolean unique) {
        RequirementContainer c = new RequirementContainer();
        if (!unique) c.identifier = this.identifier;
        this.groups.forEach((group) -> {
            RequirementGroup g = group.copy(unique);
            g.setParentContainer(c);
            c.groups.add(g);
        });
        this.instances.forEach((instance) -> {
            RequirementInstance i = instance.copy(unique);
            i.parent = c;
            c.instances.add(i);
        });
        c.valuePlaceholders.putAll(this.valuePlaceholders);
        return c;
    }

    public void serializeToExistingPropertyContainer(@NotNull PropertyContainer target) {
        PropertyContainer sec = serialize();
        for (Map.Entry<String, String> m : sec.getProperties().entrySet()) {
            target.putProperty(m.getKey(), m.getValue());
        }
    }

    @NotNull
    public PropertyContainer serialize() {
        PropertyContainer container = new PropertyContainer("loading_requirement_container");
        //Serialize container meta, which only lists top-level groups, because nested groups get serialized with their parent group
        String containerMetaKey = "[loading_requirement_container_meta:" + this.identifier + "]";
        String containerMetaValue = "[groups:";
        for (RequirementGroup g : this.groups) {
            containerMetaValue += g.identifier + ";";
        }
        containerMetaValue += "][instances:";
        for (RequirementInstance i : this.instances) {
            containerMetaValue += i.instanceIdentifier + ";";
        }
        containerMetaValue += "]";
        container.putProperty(containerMetaKey, containerMetaValue);
        //Serialize groups, including their nested groups
        for (RequirementGroup g : this.groups) {
            PropertyContainer sg = RequirementGroup.serializeRequirementGroup(g);
            for (Map.Entry<String, String> m : sg.getProperties().entrySet()) {
                container.putProperty(m.getKey(), m.getValue());
            }
        }
        //Serialize instances
        for (RequirementInstance i : this.instances) {
            List<String> l = RequirementInstance.serializeRequirementInstance(i);
            container.putProperty(l.get(0), l.get(1));
        }
        return container;
    }

    @Nullable
    public static RequirementContainer deserializeWithIdentifier(@NotNull String identifier, @NotNull PropertyContainer serialized) {
        for (RequirementContainer c : deserializeAll(serialized)) {
            if (c.identifier.equals(Objects.requireNonNull(identifier))) return c;
        }
        return null;
    }

    @NotNull
    public static List<RequirementContainer> deserializeAll(@NotNull PropertyContainer serialized) {
        List<RequirementContainer> containers = new ArrayList<>();
        List<List<String>> containerMetas = new ArrayList<>();
        for (Map.Entry<String, String> m : Objects.requireNonNull(serialized).getProperties().entrySet()) {
            if (m.getKey().startsWith("[loading_requirement_container_meta:")) {
                containerMetas.add(ListUtils.of(m.getKey(), m.getValue()));
            }
        }
        RequirementContainer combined = deserializeToSingleContainer(serialized);
        if (containerMetas.isEmpty()) {
            //Legacy support for when requirement containers had no meta and there was only one requirement container per PropertyContainer
            containers.add(combined);
        } else {
            Map<String, RequirementGroup> groupsByIdentifier = new LinkedHashMap<>();
            Map<String, RequirementInstance> instancesByIdentifier = new LinkedHashMap<>();
            combined.groups.forEach(group -> groupsByIdentifier.putIfAbsent(group.identifier, group));
            combined.instances.forEach(instance -> instancesByIdentifier.putIfAbsent(instance.instanceIdentifier, instance));
            for (List<String> meta : containerMetas) {
                String key = meta.get(0);
                String value = meta.get(1);
                if (key.contains("[loading_requirement_container_meta:")) {
                    String identifier = key.split("\\[loading_requirement_container_meta:", 2)[1];
                    if (identifier.contains("]")) {
                        identifier = identifier.split("]",2)[0];
                        List<String> groupIdentifiers = new ArrayList<>();
                        List<String> instanceIdentifiers = new ArrayList<>();
                        if (value.contains("[groups:")) {
                            String groupsRaw = value.split("\\[groups:", 2)[1];
                            if (groupsRaw.contains("]")) {
                                groupsRaw = groupsRaw.split("]", 2)[0];
                                if (groupsRaw.contains(";")) {
                                    groupIdentifiers = Arrays.asList(groupsRaw.split(";"));
                                }
                            }
                        }
                        if (value.contains("[instances:")) {
                            String instancesRaw = value.split("\\[instances:", 2)[1];
                            if (instancesRaw.contains("]")) {
                                instancesRaw = instancesRaw.split("]", 2)[0];
                                if (instancesRaw.contains(";")) {
                                    instanceIdentifiers = Arrays.asList(instancesRaw.split(";"));
                                }
                            }
                        }
                        if (!identifier.replace(" ", "").isEmpty()) {
                            RequirementContainer container = new RequirementContainer();
                            container.identifier = identifier;
                            //Find groups of container
                            for (String groupId : groupIdentifiers) {
                                RequirementGroup group = groupsByIdentifier.get(groupId);
                                if (group != null) addDeserializedGroupCopy(container, group);
                            }
                            //Find instances of container
                            for (String instanceId : instanceIdentifiers) {
                                RequirementInstance instance = instancesByIdentifier.get(instanceId);
                                if (instance != null) addDeserializedInstanceCopy(container, instance);
                            }
                            containers.add(container);
                        }
                    }
                }
            }
        }
        return containers;
    }

    /**
     * Copies a selected group out of the temporary combined deserialization container. Identified container metadata
     * can be malformed and select the same source object more than once or for multiple containers, so the returned
     * containers must never share mutable groups or instances with each other.
     */
    private static void addDeserializedGroupCopy(@NotNull RequirementContainer target, @NotNull RequirementGroup source) {
        target.addGroup(source.copy(false));
    }

    private static void addDeserializedInstanceCopy(@NotNull RequirementContainer target, @NotNull RequirementInstance source) {
        RequirementInstance instance = source.copy(false);
        instance.parent = target;
        instance.group = null;
        target.addInstance(instance);
    }

    @NotNull
    public static RequirementContainer deserializeToSingleContainer(@NotNull PropertyContainer serialized) {
        RequirementContainer c = new RequirementContainer();
        Map<String, RequirementGroup> groupsByIdentifier = new LinkedHashMap<>();
        Map<String, String> parentGroupIdentifiers = new HashMap<>();
        for (Map.Entry<String, String> m : Objects.requireNonNull(serialized).getProperties().entrySet()) {
            if (m.getKey().startsWith("[loading_requirement_group:")) {
                RequirementGroup g = RequirementGroup.deserializeRequirementGroup(m.getKey(), m.getValue(), c);
                if ((g != null) && !groupsByIdentifier.containsKey(g.identifier)) {
                    groupsByIdentifier.put(g.identifier, g);
                    String parentGroupIdentifier = RequirementGroup.deserializeParentGroupIdentifier(m.getValue());
                    if (parentGroupIdentifier != null) {
                        parentGroupIdentifiers.put(g.identifier, parentGroupIdentifier);
                    }
                }
            }
        }
        //Nest groups in their parent groups only after all groups got deserialized, because a parent group can get serialized after its nested groups
        for (RequirementGroup g : groupsByIdentifier.values()) {
            String parentGroupIdentifier = parentGroupIdentifiers.get(g.identifier);
            RequirementGroup parentGroup = (parentGroupIdentifier != null) ? groupsByIdentifier.get(parentGroupIdentifier) : null;
            //Groups with a missing parent group, or that would end up containing themselves, stay top-level groups, so they don't get lost
            if ((parentGroup == null) || !parentGroup.addGroup(g)) {
                c.addGroup(g);
            }
        }
        for (Map.Entry<String, String> m : serialized.getProperties().entrySet()) {
            if (m.getKey().startsWith("[loading_requirement:")) {
                RequirementInstance i = RequirementInstance.deserializeRequirementInstance(m.getKey(), m.getValue(), c);
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

    public static RequirementContainer stackContainers(@NotNull RequirementContainer... containers) {
        RequirementContainer stack = new RequirementContainer();
        for (RequirementContainer c : containers) {
            RequirementContainer copy = c.copy(true);
            stack.instances.addAll(copy.instances);
            stack.groups.addAll(copy.groups);
        }
        for (RequirementInstance i : stack.instances) {
            i.parent = stack;
        }
        for (RequirementGroup g : stack.groups) {
            g.setParentContainer(stack);
        }
        return stack;
    }

}
