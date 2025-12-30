//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class PlaceholderRegistry {

    private static final Map<String, Placeholder> PLACEHOLDERS = new LinkedHashMap<>();

    public static void register(@NotNull Placeholder placeholder) {
        if (PLACEHOLDERS.containsKey(Objects.requireNonNull(placeholder.getIdentifier()))) {
            throw new IllegalStateException("[FANCYMENU] A placeholder with the identifier '" + placeholder.getIdentifier() + "' is already registered!");
        }
        PLACEHOLDERS.put(placeholder.getIdentifier(), placeholder);
        placeholder.onRegistered();
    }

    @NotNull
    public static List<Placeholder> getPlaceholders() {
        return new ArrayList<>(PLACEHOLDERS.values());
    }

    @Nullable
    public static Placeholder getPlaceholder(String identifier) {
        Placeholder ph = PLACEHOLDERS.get(identifier);
        if (ph != null) return ph;
        for (Placeholder p : PLACEHOLDERS.values()) {
            List<String> alt = p.getAlternativeIdentifiers();
            if ((alt != null) && alt.contains(identifier)) return p;
        }
        return null;
    }

}
