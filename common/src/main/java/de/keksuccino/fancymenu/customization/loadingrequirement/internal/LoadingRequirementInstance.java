package de.keksuccino.fancymenu.customization.loadingrequirement.internal;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirementRegistry;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("all")
public class LoadingRequirementInstance implements ValuePlaceholderHolder {

    private static final Logger LOGGER = LogManager.getLogger();

    public LoadingRequirementContainer parent;
    public LoadingRequirement requirement;
    @Nullable
    public String value;
    /**
     * Placeholders do not get serialized, but get copied when calling copy().
     * They get added at runtime, mostly after creating a new {@link LoadingRequirementInstance}.
     */
    protected final Map<String, Supplier<String>> valuePlaceholders = new HashMap<>();
    /** The group the requirement is part of, if it is part of one. */
    @Nullable
    public LoadingRequirementGroup group;
    public RequirementMode mode;
    public String instanceIdentifier = ScreenCustomization.generateUniqueIdentifier();

    public LoadingRequirementInstance(LoadingRequirement requirement, @Nullable String value, RequirementMode mode, LoadingRequirementContainer parent) {
        this.parent = parent;
        this.requirement = requirement;
        this.value = value;
        this.mode = mode;
    }

    public boolean requirementMet() {
        if (!this.requirement.checkAsync()) return false;
        String v = this.value;
        if (v != null) {
            for (Map.Entry<String, Supplier<String>> m : this.valuePlaceholders.entrySet()) {
                String replaceWith = m.getValue().get();
                if (replaceWith == null) replaceWith = "";
                v = v.replace(VALUE_PLACEHOLDER_PREFIX + m.getKey(), replaceWith);
            }
            v = PlaceholderParser.replacePlaceholders(v);
        }
        this.requirement.setCurrentInstance(this);
        boolean met = this.requirement.isRequirementMet(v);
        if (this.mode == RequirementMode.IF_NOT) {
            return !met;
        }
        return met;
    }

    /**
     * Value placeholders are for replacing parts of the {@link LoadingRequirementInstance#value}.<br><br>
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
        if (o instanceof LoadingRequirementInstance other) {
            if (!Objects.equals(this.instanceIdentifier, other.instanceIdentifier)) return false;
            if (this.requirement != other.requirement) return false;
            if (!Objects.equals(this.value, other.value)) return false;
            if (this.mode != other.mode) return false;
            return true;
        }
        return false;
    }

    public LoadingRequirementInstance copy(boolean unique) {
        LoadingRequirementInstance i = new LoadingRequirementInstance(this.requirement, this.value, this.mode, null);
        if (!unique) i.instanceIdentifier = this.instanceIdentifier;
        i.valuePlaceholders.putAll(this.valuePlaceholders);
        return i;
    }

    @NotNull
    public static List<String> serializeRequirementInstance(@NotNull LoadingRequirementInstance instance) {
        List<String> l = new ArrayList<>();
        String key = "[loading_requirement:" + instance.requirement.getIdentifier() + "][requirement_mode:" + instance.mode.name + "]";
        if (instance.group != null) {
            key += "[group:" + instance.group.identifier + "]";
        }
        key += "[req_id:" + instance.instanceIdentifier + "]";
        l.add(key);
        if (instance.requirement.hasValue() && (instance.value != null)) {
            l.add(instance.value);
        } else {
            l.add("");
        }
        return l;
    }

    @Nullable
    public static LoadingRequirementInstance deserializeRequirementInstance(@NotNull String key, @Nullable String value, @NotNull LoadingRequirementContainer parent) {
        if (key.startsWith("[loading_requirement:")) {
            String reqId = key.split(":", 2)[1].split("\\]", 2)[0];
            LoadingRequirement req = LoadingRequirementRegistry.getRequirement(reqId);
            if ((req != null) && key.contains("[requirement_mode:")) {
                String modeName = key.split("\\[requirement_mode:", 2)[1].split("\\]", 2)[0];
                RequirementMode mode = RequirementMode.getByName(modeName);
                if (mode != null) {
                    LoadingRequirementGroup group = null;
                    if (key.contains("[group:")) {
                        String groupId = key.split("\\[group:", 2)[1].split("\\]", 2)[0];
                        if (!parent.groupExists(groupId)) {
                            parent.createAndAddGroup(groupId, LoadingRequirementGroup.GroupMode.AND);
                        }
                        group = parent.getGroup(groupId);
                    }
                    if (key.contains("[req_id:")) {
                        String id = key.split("\\[req_id:", 2)[1].split("\\]", 2)[0];
                        LoadingRequirementInstance instance = new LoadingRequirementInstance(req, value, mode, parent);
                        if (!req.hasValue()) instance.value = null;
                        instance.instanceIdentifier = id;
                        instance.group = group;
                        return instance;
                    }
                }
            }
        }
        return null;
    }

    public enum RequirementMode {

        IF("if"),
        IF_NOT("if_not");

        public final String name;

        RequirementMode(String name) {
            this.name = name;
        }

        @Nullable
        public static RequirementMode getByName(String name) {
            for (RequirementMode m : RequirementMode.values()) {
                if (m.name.equals(name)) {
                    return m;
                }
            }
            return null;
        }

    }

}
