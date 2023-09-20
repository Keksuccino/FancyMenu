package de.keksuccino.fancymenu.util.rendering.text.color;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TextColorFormatterRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, TextColorFormatter> FORMATTERS = new HashMap<>();

    public static void register(@NotNull String identifier, @NotNull TextColorFormatter formatter) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(formatter);
        if (FORMATTERS.containsKey(identifier)) {
            LOGGER.warn("[FANCYMENU] TextColorFormatter with identifier '" + identifier + "' already exists! Overriding formatter!");
        }
        FORMATTERS.put(identifier, formatter);
    }

    @Nullable
    public static TextColorFormatter getFormatter(@NotNull String identifier) {
        return FORMATTERS.get(identifier);
    }

    @Nullable
    public static TextColorFormatter getByCode(char code) {
        for (TextColorFormatter f : getFormatters()) {
            if (("" + f.getCode()).equals("" + code)) return f;
        }
        return null;
    }

    @NotNull
    public static List<TextColorFormatter> getFormatters() {
        return new ArrayList<>(FORMATTERS.values());
    }

}
