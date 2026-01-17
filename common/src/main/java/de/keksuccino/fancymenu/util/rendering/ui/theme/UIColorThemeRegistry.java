package de.keksuccino.fancymenu.util.rendering.ui.theme;

import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.UIColorThemeChangedEvent;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.UIColorThemes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class UIColorThemeRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, UITheme> THEMES = new LinkedHashMap<>();

    private static UITheme activeTheme;

    public static void register(@NotNull UITheme theme) {
        Objects.requireNonNull(theme);
        Objects.requireNonNull(theme.getIdentifier());
        if (THEMES.containsKey(theme.identifier)) {
            LOGGER.warn("[FANCYMENU] UIColorTheme with identifier '" + theme.getIdentifier() + "' already exists! Overriding theme!");
        }
        THEMES.put(theme.getIdentifier(), theme);
    }

    @NotNull
    public static UITheme getActiveTheme() {
        if (activeTheme != null) {
            return activeTheme;
        }
        return UIColorThemes.DARK;
    }

    public static void setActiveTheme(@NotNull String identifier) {
        activeTheme = getTheme(identifier);
        if (activeTheme == null) {
            LOGGER.error("[FANCYMENU] Unable to switch theme! Theme not found: " + identifier);
            LOGGER.error("[FANCYMENU] Falling back to DARK theme!");
            activeTheme = UIColorThemes.DARK;
        }
        EventHandler.INSTANCE.postEvent(new UIColorThemeChangedEvent(getActiveTheme()));
    }

    @Nullable
    public static UITheme getTheme(@NotNull String identifier) {
        Objects.requireNonNull(identifier);
        return THEMES.get(identifier);
    }

    @NotNull
    public static List<UITheme> getThemes() {
        return new ArrayList<>(THEMES.values());
    }

    public static void clearThemes() {
        THEMES.clear();
        activeTheme = null;
    }

}
