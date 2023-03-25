//TODO übernehmen
package de.keksuccino.fancymenu.menu.loadingrequirement.v2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class LoadingRequirementRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final LinkedHashMap<String, LoadingRequirement> REQUIREMENTS = new LinkedHashMap<>();

    /**
     * Register LoadingRequirements here.<br>
     * <b>This should be done on mod init.</b>
     */
    public static void registerRequirement(LoadingRequirement requirement) {
        if (requirement != null) {
            if (requirement.getIdentifier() != null) {
                if (REQUIREMENTS.containsKey(requirement.getIdentifier())) {
                    LOGGER.warn("[FANCYMENU] A LoadingRequirement with the identifier '" + requirement.getIdentifier() + "' is already registered! Overriding requirement!");
                }
                REQUIREMENTS.put(requirement.getIdentifier(), requirement);
            } else {
                LOGGER.error("[FANCYMENU] LoadingRequirement identifier cannot be NULL!");
            }
        }
    }

    /**
     * Unregister a requirement.
     * @param requirementIdentifier The identifier of the requirement that should get unregistered.
     * @return The requirement that got unregistered or NULL if no requirement with the given identifier was found.
     */
    @Nullable
    public static LoadingRequirement unregisterRequirement(String requirementIdentifier) {
        return REQUIREMENTS.remove(requirementIdentifier);
    }

    /**
     * @return A new list with all registered requirements. Adding/Removing requirements to/from this list will not register/unregister them.
     */
    @NotNull
    public static List<LoadingRequirement> getRequirements() {
        return new ArrayList<>(REQUIREMENTS.values());
    }

    /**
     * @param requirementIdentifier The identifier of the reuqirement.
     * @return The requirement with the given identifier or NULL if no requirement was found.
     */
    @Nullable
    public static LoadingRequirement getRequirement(String requirementIdentifier) {
        return REQUIREMENTS.get(requirementIdentifier);
    }

}
