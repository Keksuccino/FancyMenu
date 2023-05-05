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
        Objects.requireNonNull(builder.getIdentifier(), "[FANCYMENU] Failed to register element! Identifier was NULL!");
        if (ELEMENT_BUILDERS.containsKey(builder.getIdentifier())) {
            LOGGER.warn("[FANCYMENU] Element with identifier '" + builder.getIdentifier() + "' already registered! Overriding element!");
        }
        ELEMENT_BUILDERS.put(builder.getIdentifier(), builder);
    }

    public static void unregister(@NotNull String identifier) {
        ELEMENT_BUILDERS.remove(identifier);
    }

    @NotNull
    public static List<ElementBuilder<?,?>> getBuilders() {
        return new ArrayList<>(ELEMENT_BUILDERS.values());
    }

    @Nullable
    public static ElementBuilder<?,?> getBuilder(@NotNull String itemIdentifier) {
        return ELEMENT_BUILDERS.get(itemIdentifier);
    }

}
