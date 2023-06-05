package de.keksuccino.fancymenu.rendering.ui.colorscheme;

import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.events.UIColorSchemeChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class UIColorSchemeRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final UIColorScheme FALLBACK_SCHEME = new UIColorScheme();
    private static final Map<String, UIColorScheme> SCHEMES = new HashMap<>();

    private static UIColorScheme activeScheme;

    public static void register(@NotNull String identifier, @NotNull UIColorScheme scheme) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(scheme);
        if (SCHEMES.containsKey(identifier)) {
            LOGGER.warn("[FANCYMENU] UIColorScheme with identifier '" + identifier + "' already exists! Overriding scheme!");
        }
        SCHEMES.put(identifier, scheme);
    }

    @NotNull
    public static UIColorScheme getActiveScheme() {
        if (activeScheme != null) {
            return activeScheme;
        }
        return FALLBACK_SCHEME;
    }

    public static void setActiveScheme(@NotNull String identifier) {
        activeScheme = getScheme(identifier);
        EventHandler.INSTANCE.postEvent(new UIColorSchemeChangedEvent(getActiveScheme()));
    }

    @Nullable
    public static UIColorScheme getScheme(@NotNull String identifier) {
        Objects.requireNonNull(identifier);
        return SCHEMES.get(identifier);
    }

    @NotNull
    public static List<UIColorScheme> getSchemes() {
        return new ArrayList<>(SCHEMES.values());
    }

}
