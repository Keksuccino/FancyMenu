
//Copyright (c) 2022-2023 Keksuccino.
//This code is licensed under DSMSLv2.
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
        if (PLACEHOLDERS.containsKey(Objects.requireNonNull(placeholder.getIdentifier()))) {
            LOGGER.warn("[FANCYMENU] A placeholder with the identifier '" + placeholder.getIdentifier() + "' is already registered! Overriding placeholder!");
        }
        PLACEHOLDERS.put(placeholder.getIdentifier(), placeholder);
    }

    @NotNull
    public static List<Placeholder> getPlaceholders() {
        return new ArrayList<>(PLACEHOLDERS.values());
    }

    @Nullable
    public static Placeholder getPlaceholder(String identifier) {
        return PLACEHOLDERS.get(identifier);
    }

}
