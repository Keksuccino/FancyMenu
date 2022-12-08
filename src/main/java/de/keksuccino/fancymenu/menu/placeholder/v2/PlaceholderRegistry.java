//TODO übernehmen

//Copyright (c) 2022 Keksuccino.
//This code is licensed under DSMSL.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.menu.placeholder.v2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlaceholderRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<String, Placeholder> placeholders = new LinkedHashMap<>();

    public static void registerPlaceholder(Placeholder placeholder) {
        if (placeholder != null) {
            if (!placeholders.containsKey(placeholder.getIdentifier())) {
                placeholders.put(placeholder.getIdentifier(), placeholder);
            } else {
                LOGGER.error("[FANCYMENU] Unable to register placeholder! Placeholder ID already registered: " + placeholder.getIdentifier());
            }
        } else {
            LOGGER.error("[FANCYMENU] Unable to register placeholder! Placeholder was NULL!");
        }
    }

    @Nullable
    public static Placeholder getPlaceholderForIdentifier(String identifier) {
        return placeholders.get(identifier);
    }

    public static Map<String, Placeholder> getPlaceholders() {
        return placeholders;
    }

    public static List<Placeholder> getPlaceholdersList() {
        List<Placeholder> l = new ArrayList<>();
        l.addAll(placeholders.values());
        return l;
    }

}
