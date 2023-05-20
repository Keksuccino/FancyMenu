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
        register(builder.getIdentifier(), true, builder);
    }

    private static void register(@NotNull String identifier, boolean registerAlternativeIdentifiers, @NotNull ElementBuilder<?,?> builder) {
        Objects.requireNonNull(identifier, "[FANCYMENU] Failed to register ElementBuilder! Identifier was NULL!");
        if (BUILDERS.containsKey(identifier)) {
            LOGGER.warn("[FANCYMENU] ElementBuilder with identifier '" + identifier + "' already registered! Overriding builder!");
        }
        BUILDERS.put(identifier, builder);
        if (registerAlternativeIdentifiers) {
            for (String altIdentifier : builder.getAlternativeIdentifiers()) {
                register(altIdentifier, false, builder);
            }
        }
    }

    public static void unregister(@NotNull String identifier) {
        BUILDERS.remove(identifier);
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
