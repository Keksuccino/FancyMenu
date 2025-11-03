package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorThemeRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorThemeSerializer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UIColorThemes {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final File THEME_DIR = FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/ui_themes"));

    // HARDCODED DEFAULT THEMES
    public static final DarkUIColorTheme DARK = new DarkUIColorTheme();
    public static final LightUIColorTheme LIGHT = new LightUIColorTheme();

    // ASSET THEMES
    public static final ResourceLocation OLED_PURPLE_THEME_LOCATION = new ResourceLocation("fancymenu", "themes/oled_purple.json");
    public static final ResourceLocation NETHER_THEME_LOCATION = new ResourceLocation("fancymenu", "themes/nether.json");
    public static final ResourceLocation BUTTER_DARK_THEME_LOCATION = new ResourceLocation("fancymenu", "themes/butter_dark.json");
    public static final ResourceLocation BUTTER_OLED_THEME_LOCATION = new ResourceLocation("fancymenu", "themes/butter_oled.json");

    public static final UIColorTheme[] DEFAULT_THEMES = new UIColorTheme[]{ DARK, LIGHT };

    public static void registerAll() {

        registerDefaultThemes();

        registerAssetThemes();

        registerCustomThemes();

        setActiveThemeFromOptions();

    }

    public static void reloadThemes() {
        LOGGER.info("[FANCYMENU] Reloading UI Themes..");
        UIColorThemeRegistry.clearThemes();
        registerAll();
        setActiveThemeFromOptions();
    }

    private static void registerAssetThemes() {

        registerAssetTheme(OLED_PURPLE_THEME_LOCATION);

        registerAssetTheme(NETHER_THEME_LOCATION);

        registerAssetTheme(BUTTER_DARK_THEME_LOCATION);

        registerAssetTheme(BUTTER_OLED_THEME_LOCATION);

    }

    private static void registerAssetTheme(@NotNull ResourceLocation themeLocation) {
        UIColorTheme theme = UIColorThemeSerializer.deserializeThemeFromResource(themeLocation);
        if (theme != null) {
            UIColorThemeRegistry.register(theme);
        } else {
            LOGGER.error("[FANCYMENU] Failed to register FancyMenu theme from assets! Deserialization failed and returned NULL for: " + themeLocation, new NullPointerException("Theme was NULL"));
        }
    }

    private static void registerDefaultThemes() {

        registerDefaultTheme(DARK);

        registerDefaultTheme(LIGHT);

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
