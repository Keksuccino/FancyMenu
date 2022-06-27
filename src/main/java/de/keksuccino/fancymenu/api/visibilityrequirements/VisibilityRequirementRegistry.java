package de.keksuccino.fancymenu.api.visibilityrequirements;

import de.keksuccino.fancymenu.FancyMenu;

import java.util.*;

public class VisibilityRequirementRegistry {

    //TODO übernehmen (LinkedHashMap)
    protected static LinkedHashMap<String, VisibilityRequirement> requirements = new LinkedHashMap<>();

    /**
     * Register your custom visibility requirements here.
     */
    public static void registerRequirement(VisibilityRequirement requirement) {
        if (requirement != null) {
            if (requirement.getIdentifier() != null) {
                if (requirements.containsKey(requirement.getIdentifier())) {
                    FancyMenu.LOGGER.warn("[FANCYMENU] WARNING! A visibility requirement with the identifier '" + requirement.getIdentifier() + "' is already registered! Overriding requirement!");
                }
                requirements.put(requirement.getIdentifier(), requirement);
            } else {
                FancyMenu.LOGGER.error("[FANCYMENU] ERROR! Visibility requirement identifier cannot be null for VisibilityRequirements!");
            }
        }
    }

    /**
     * Unregister a previously added visibility requirement.
     */
    public static void unregisterRequirement(String requirementIdentifier) {
        requirements.remove(requirementIdentifier);
    }

    //TODO übernehmen
    public static List<VisibilityRequirement> getRequirements() {
        List<VisibilityRequirement> l = new ArrayList<>();
        requirements.forEach((key, value) -> {
            l.add(value);
        });
        return l;
    }

    public static VisibilityRequirement getRequirement(String requirementIdentifier) {
        return requirements.get(requirementIdentifier);
    }

}
