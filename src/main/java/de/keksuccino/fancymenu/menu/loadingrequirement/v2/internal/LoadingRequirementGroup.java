//TODO übernehmenn
package de.keksuccino.fancymenu.menu.loadingrequirement.v2.internal;

import de.keksuccino.konkrete.properties.PropertiesSection;



import java.util.ArrayList;
import java.util.List;

public class LoadingRequirementGroup {

    public LoadingRequirementContainer parent;
    public String identifier;
    public GroupMode mode;
    protected final List<LoadingRequirementInstance> instances = new ArrayList<>();

    public LoadingRequirementGroup( String identifier,  GroupMode mode,  LoadingRequirementContainer parent) {
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
    
    public LoadingRequirementInstance removeInstance(LoadingRequirementInstance instance) {
        instance.group = null;
        return this.instances.remove(instance) ? instance : null;
    }

    public List<LoadingRequirementInstance> getInstances() {
        return new ArrayList<>(this.instances);
    }

    
    public static PropertiesSection serializeRequirementGroup( LoadingRequirementGroup group) {
        PropertiesSection sec = new PropertiesSection("requirement_group");
        String key = "[loading_requirement_group:" + group.identifier + "]";
        String value = "[group_mode:" + group.mode.name + "]";
        sec.addEntry(key, value);
        for (LoadingRequirementInstance i : group.instances) {
            i.group = group;
            List<String> l = LoadingRequirementInstance.serializeRequirementInstance(i);
            sec.addEntry(l.get(0), l.get(1));
        }
        return sec;
    }

    
    public static LoadingRequirementGroup deserializeRequirementGroup( String key,  String value,  LoadingRequirementContainer parent) {
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
