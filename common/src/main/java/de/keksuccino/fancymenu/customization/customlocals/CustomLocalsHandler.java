package de.keksuccino.fancymenu.customization.customlocals;

import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CustomLocalsHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final CustomLocalizationStorage STORAGE = new CustomLocalizationStorage();

    public static final File CUSTOM_LOCALS_DIR = new File("config/fancymenu/custom_locals");

    private static boolean initialized;

    public static synchronized void init() {
        if (!initialized) {
            initialized = true;
            MinecraftResourceReloadObserver.addReloadListener(CustomLocalsHandler::onMinecraftResourceReload);
        }
        loadLocalizations();
    }

    public static void loadLocalizations() {
        Path directory = CUSTOM_LOCALS_DIR.toPath();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            STORAGE.clear();
            LOGGER.error("[FANCYMENU] Failed to create the custom localization directory: {}", directory, e);
            return;
        }
        STORAGE.reload(directory, Minecraft.getInstance().getLanguageManager().getSelected().getCode(), CustomLocalsHandler::logLoadError);
    }

    public static @NotNull String localize(@NotNull String key) {
        return STORAGE.localize(key);
    }

    private static void onMinecraftResourceReload(@NotNull MinecraftResourceReloadObserver.ReloadAction action) {
        // Minecraft changes LanguageManager's selected translations during resource reload, so custom files must switch only after that reload finishes.
        if (action == MinecraftResourceReloadObserver.ReloadAction.FINISHED) loadLocalizations();
    }

    private static void logLoadError(@NotNull Path path, @NotNull Exception exception) {
        LOGGER.error("[FANCYMENU] Failed to load custom localization path: {}", path, exception);
    }
}
