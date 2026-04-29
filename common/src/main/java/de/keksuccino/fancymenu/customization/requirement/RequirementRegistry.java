package de.keksuccino.fancymenu.customization.requirement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class RequirementRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final LinkedHashMap<String, Requirement> REQUIREMENTS = new LinkedHashMap<>();

    public static void register(@NotNull Requirement requirement) {
        if (REQUIREMENTS.containsKey(Objects.requireNonNull(requirement.getIdentifier()))) {
            LOGGER.warn("[FANCYMENU] A LoadingRequirement with the identifier '" + requirement.getIdentifier() + "' is already registered! Overriding requirement!");
        }
        REQUIREMENTS.put(requirement.getIdentifier(), requirement);
    }

    @NotNull
    public static List<Requirement> getRequirements() {
        List<Requirement> l = new ArrayList<>();
        REQUIREMENTS.forEach((key, value) -> {
            l.add(value);
        });
        return l;
    }

    @Nullable
    public static Requirement getRequirement(@NotNull String requirementIdentifier) {
        return REQUIREMENTS.get(requirementIdentifier);
    }

    @NotNull
    public static LinkedHashMap<String, List<Requirement>> getRequirementsOrderedByCategories() {
        LinkedHashMap<String, List<Requirement>> m = new LinkedHashMap<>();
        for (Requirement r : getRequirements()) {
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
    public static List<Requirement> getRequirementsWithoutCategory() {
        List<Requirement> l = new ArrayList<>();
        for (Requirement r : getRequirements()) {
            if (r.getCategory() == null) {
                l.add(r);
            }
        }
        return l;
    }

}
