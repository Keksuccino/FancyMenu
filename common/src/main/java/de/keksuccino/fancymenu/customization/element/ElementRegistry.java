package de.keksuccino.fancymenu.customization.element;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ElementRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, ElementBuilder<?,?>> BUILDERS = new LinkedHashMap<>();

    /**
     * Here you can register elements which can then be used in layouts.<br>
     * {@link ElementBuilder}s should get registered during mod-init.
     **/
    public static void register(@NotNull ElementBuilder<?,?> builder) {
        if (BUILDERS.containsKey(Objects.requireNonNull(builder.getIdentifier()))) {
            LOGGER.warn("[FANCYMENU] ElementBuilder with identifier '" + builder.getIdentifier() + "' already registered! Overriding builder!");
        }
        BUILDERS.put(builder.getIdentifier(), builder);
    }

    @NotNull
    public static List<ElementBuilder<?,?>> getBuilders() {
        return new ArrayList<>(BUILDERS.values());
    }

    @Nullable
    public static ElementBuilder<?,?> getBuilder(@NotNull String identifier) {
        return BUILDERS.get(identifier);
    }

    public static boolean hasBuilder(@NotNull String identifier) {
        return BUILDERS.containsKey(identifier);
    }

}
