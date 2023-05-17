
//Copyright (c) 2022 Keksuccino.
//This code is licensed under DSMSL.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlaceholderRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, Placeholder> PLACEHOLDERS = new LinkedHashMap<>();

    public static void register(@NotNull Placeholder placeholder) {
        Objects.requireNonNull(placeholder.getIdentifier());
        if (PLACEHOLDERS.containsKey(placeholder.getIdentifier())) {
            LOGGER.warn("[FANCYMENU] A placeholder with the identifier '" + placeholder.getIdentifier() + "' is already registered! Overriding placeholder!");
        }
        PLACEHOLDERS.put(placeholder.getIdentifier(), placeholder);
    }

    @Nullable
    public static Placeholder getPlaceholder(String identifier) {
        return PLACEHOLDERS.get(identifier);
    }

    @NotNull
    public static Map<String, Placeholder> getPlaceholders() {
        return PLACEHOLDERS;
    }

    @NotNull
    public static List<Placeholder> getPlaceholdersList() {
        return new ArrayList<>(PLACEHOLDERS.values());
    }

}
