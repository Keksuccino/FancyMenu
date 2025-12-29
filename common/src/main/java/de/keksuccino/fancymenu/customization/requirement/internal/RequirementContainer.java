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
            if (COOLDOWN_CACHE.containsKey(this.cachingIdentifier) && ((COOLDOWN_CACHE.get(this.cachingIdentifier).getKey() + getRequirementCachingDurationMs()) > now)) {
                return COOLDOWN_CACHE.get(this.cachingIdentifier).getValue();
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

    @Nullable
    public RequirementGroup createAndAddGroup(@NotNull String identifier, @NotNull RequirementGroup.GroupMode mode) {
        if (!this.groupExists(identifier)) {
            RequirementGroup g = new RequirementGroup(identifier, mode, this);
            this.groups.add(g);
            this.valuePlaceholders.forEach(g::addValuePlaceholder);
            return g;
        }
        return null;
    }

    public boolean addGroup(@NotNull RequirementGroup group) {
        if (!this.groupExists(group.identifier)) {
            this.groups.add(group);
            this.valuePlaceholders.forEach(group::addValuePlaceholder);
            return true;
        }
        return false;
    }

    public List<RequirementGroup> getGroups() {
        return new ArrayList<>(this.groups);
    }

    @Nullable
    public RequirementGroup getGroup(@NotNull String identifier) {
        for (RequirementGroup g : this.groups) {
            if (g.identifier.equals(identifier)) {
                return g;
            }
        }
        return null;
    }

    public boolean groupExists(@NotNull String identifier) {
        return this.getGroup(identifier) != null;
    }

    public boolean removeGroup(@NotNull RequirementGroup group) {
        return this.groups.remove(Objects.requireNonNull(group));
    }

    public boolean removeGroupByIdentifier(@NotNull String identifier) {
        RequirementGroup g = this.getGroup(identifier);
        if (g != null) {
            return this.groups.remove(g);
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
            g.parent = c;
            for (RequirementInstance i : g.instances) {
                i.parent = c;
            }
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
        //Serialize container meta
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
        //Serialize groups
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
                                for (RequirementGroup g : combined.groups) {
                                    if (g.identifier.equals(groupId)) {
                                        container.groups.add(g);
                                        break;
                                    }
                                }
                            }
                            //Find instances of container
                            for (String instanceId : instanceIdentifiers) {
                                for (RequirementInstance i : combined.instances) {
                                    if (i.instanceIdentifier.equals(instanceId)) {
                                        container.instances.add(i);
                                        break;
                                    }
                                }
                            }
                            containers.add(container);
                        }
                    }
                }
            }
        }
        return containers;
    }

    @NotNull
    public static RequirementContainer deserializeToSingleContainer(@NotNull PropertyContainer serialized) {
        RequirementContainer c = new RequirementContainer();
        for (Map.Entry<String, String> m : Objects.requireNonNull(serialized).getProperties().entrySet()) {
            if (m.getKey().startsWith("[loading_requirement_group:")) {
                RequirementGroup g = RequirementGroup.deserializeRequirementGroup(m.getKey(), m.getValue(), c);
                if (g != null) {
                    c.addGroup(g);
                }
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
            g.parent = stack;
            for (RequirementInstance i : g.instances) {
                i.parent = stack;
            }
        }
        return stack;
    }

}
