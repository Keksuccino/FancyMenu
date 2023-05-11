package de.keksuccino.fancymenu.customization.element;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ElementRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, ElementBuilder<?,?>> ELEMENT_BUILDERS = new LinkedHashMap<>();

    /**
     * Here you can register elements which can then be used in layouts.<br>
     * {@link ElementBuilder}s should get registered during mod-init.
     **/
    public static void register(@NotNull ElementBuilder<?,?> builder) {
        register(builder.getIdentifier(), builder);
    }

    /**
     * Here you can register elements which can then be used in layouts.<br>
     * {@link ElementBuilder}s should get registered during mod-init.
     **/
    public static void register(@NotNull String identifier, @NotNull ElementBuilder<?,?> builder) {
        Objects.requireNonNull(identifier, "[FANCYMENU] Failed to register element! Identifier was NULL!");
        if (ELEMENT_BUILDERS.containsKey(identifier)) {
            LOGGER.warn("[FANCYMENU] Element with identifier '" + identifier + "' already registered! Overriding element!");
        }
        ELEMENT_BUILDERS.put(identifier, builder);
        for (String altIdentifier : builder.getAlternativeIdentifiers()) {
            register(altIdentifier, builder);
        }
    }

    public static void unregister(@NotNull String identifier) {
        ELEMENT_BUILDERS.remove(identifier);
    }

    @NotNull
    public static List<ElementBuilder<?,?>> getBuilders() {
        return new ArrayList<>(ELEMENT_BUILDERS.values());
    }

    @Nullable
    public static ElementBuilder<?,?> getBuilder(@NotNull String identifier) {
        return ELEMENT_BUILDERS.get(identifier);
    }

    public static boolean hasBuilder(@NotNull String identifier) {
        return getBuilder(identifier) != null;
    }

}
