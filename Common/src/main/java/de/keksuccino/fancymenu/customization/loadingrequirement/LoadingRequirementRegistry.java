package de.keksuccino.fancymenu.customization.loadingrequirement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class LoadingRequirementRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final LinkedHashMap<String, LoadingRequirement> REQUIREMENTS = new LinkedHashMap<>();

    public static void register(@NotNull LoadingRequirement requirement) {
        if (REQUIREMENTS.containsKey(Objects.requireNonNull(requirement.getIdentifier()))) {
            LOGGER.warn("[FANCYMENU] A LoadingRequirement with the identifier '" + requirement.getIdentifier() + "' is already registered! Overriding requirement!");
        }
        REQUIREMENTS.put(requirement.getIdentifier(), requirement);
    }

    @NotNull
    public static List<LoadingRequirement> getRequirements() {
        List<LoadingRequirement> l = new ArrayList<>();
        REQUIREMENTS.forEach((key, value) -> {
            l.add(value);
        });
        return l;
    }

    @Nullable
    public static LoadingRequirement getRequirement(@NotNull String requirementIdentifier) {
        return REQUIREMENTS.get(requirementIdentifier);
    }

    @NotNull
    public static LinkedHashMap<String, List<LoadingRequirement>> getRequirementsOrderedByCategories() {
        LinkedHashMap<String, List<LoadingRequirement>> m = new LinkedHashMap<>();
        for (LoadingRequirement r : getRequirements()) {
            if (r.getCategory() != null) {
                if (!m.containsKey(r.getCategory())) {
                    m.put(r.getCategory(), new ArrayList<>());
                }
                m.get(r.getCategory()).add(r);
            }
        }
        return m;
    }

    @NotNull
    public static List<LoadingRequirement> getRequirementsWithoutCategory() {
        List<LoadingRequirement> l = new ArrayList<>();
        for (LoadingRequirement r : getRequirements()) {
            if (r.getCategory() == null) {
                l.add(r);
            }
        }
        return l;
    }

}
