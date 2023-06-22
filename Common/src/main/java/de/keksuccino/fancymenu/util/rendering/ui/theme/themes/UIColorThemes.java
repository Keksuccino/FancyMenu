package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorThemeRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorThemeSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UIColorThemes {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final File THEME_DIR = FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/ui_themes"));

    public static final DarkUIColorTheme DARK = new DarkUIColorTheme();
    public static final LightUIColorTheme LIGHT = new LightUIColorTheme();

    public static final UIColorTheme[] DEFAULT_THEMES = new UIColorTheme[]{ DARK, LIGHT };

    public static void registerAll() {

        registerDefaultTheme(DARK);
        registerDefaultTheme(LIGHT);

        registerCustomThemes();

        setActiveThemeFromOptions();

    }

    public static void reloadThemes() {
        LOGGER.info("[FANCYMENU] Reloading UI Themes..");
        UIColorThemeRegistry.clearThemes();
        registerAll();
        setActiveThemeFromOptions();
    }

    private static void registerDefaultTheme(UIColorTheme theme) {
        UIColorThemeRegistry.register(theme);
        UIColorThemeSerializer.serializeThemeToFile(theme, new File(THEME_DIR, theme.getIdentifier() + ".json"));
    }

    private static void registerCustomThemes() {
        for (UIColorTheme theme : readThemesFromFiles()) {
            if (!isIdentifierOfDefaultTheme(theme.getIdentifier())) {
                UIColorThemeRegistry.register(theme);
            }
        }
    }

    @NotNull
    private static List<UIColorTheme> readThemesFromFiles() {
        List<UIColorTheme> themes = new ArrayList<>();
        try {
            File[] files = THEME_DIR.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().toLowerCase().endsWith(".json")) {
                        UIColorTheme theme = UIColorThemeSerializer.deserializeThemeFromFile(f);
                        if (theme != null) {
                            themes.add(theme);
                        } else {
                            LOGGER.error("[FANCYMENU] Failed to read UI Theme from file: " + f.getPath());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return themes;
    }

    private static void setActiveThemeFromOptions() {
        UIColorThemeRegistry.setActiveTheme(FancyMenu.getOptions().uiTheme.getValue());
    }

    private static boolean isIdentifierOfDefaultTheme(@NotNull String identifier) {
        for (UIColorTheme t : DEFAULT_THEMES) {
            if (t.getIdentifier().equals(identifier)) return true;
        }
        return false;
    }

}
