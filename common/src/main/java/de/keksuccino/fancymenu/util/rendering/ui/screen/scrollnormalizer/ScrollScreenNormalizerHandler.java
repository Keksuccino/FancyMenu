package de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.LegacyHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ScrollScreenNormalizerHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "normalized_scroll_screens.json";
    /** The name an outdated legacy file in the game directory gets, followed by a number if the name is taken already. */
    private static final String LEGACY_BACKUP_NAME = "normalized_scroll_screens_old";
    private static final File NORMALIZE_SCREEN_FILE = new File(FancyMenu.MOD_DIR, FILE_NAME);
    private static final Map<String, Boolean> NORMALIZE_SCREEN_MAP = new HashMap<>();

    private static boolean loaded = false;

    /**
     * Moves the normalized screens that old FancyMenu versions kept in the game directory into the active profile.
     * Has to get called once at launch, before the active profile can change, see {@link LegacyHandler#migrateLegacyFilesAtLaunch()}.
     */
    public static void migrateLegacyFile() {
        migrateLegacyFile(new File(GameDirectoryUtils.getGameDirectory(), FILE_NAME).toPath(), NORMALIZE_SCREEN_FILE.toPath());
    }

    /**
     * Moves the legacy file into the profile if the profile has no file yet. Otherwise the profile has its own normalized screens already,
     * which FancyMenu uses instead of the legacy file, so the legacy file only gets renamed and stays as backup.<br>
     * Either way, the legacy file is gone afterwards, so profiles that get created, imported or loaded later never pick it up.
     * If moving or renaming fails, the legacy file stays and the next launch tries again.
     */
    static void migrateLegacyFile(@NotNull Path legacyFile, @NotNull Path profileFile) {
        if (!Files.isRegularFile(legacyFile, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            if (!Files.exists(profileFile, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectories(profileFile.toAbsolutePath().getParent());
                Files.move(legacyFile, profileFile);
                LOGGER.info("[FANCYMENU] Moved the normalized scroll screens of the game directory into the active profile: " + profileFile);
                return;
            }
            Path backup = renameToBackup(legacyFile);
            LOGGER.info("[FANCYMENU] The active profile has its own normalized scroll screens, so the outdated file of the game directory got renamed to: " + backup);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to migrate the normalized scroll screens of the game directory: " + legacyFile, ex);
        }
    }

    @NotNull
    private static Path renameToBackup(@NotNull Path legacyFile) throws IOException {
        Path directory = legacyFile.toAbsolutePath().getParent();
        int number = 0;
        while (true) {
            Path backup = directory.resolve(LEGACY_BACKUP_NAME + ((number > 0) ? ("_" + number) : "") + ".json");
            number++;
            if (Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) continue;
            // Without REPLACE_EXISTING, the move fails instead of replacing a file that got created after the check
            Files.move(legacyFile, backup);
            return backup;
        }
    }

    /**
     * Replaces everything in memory with the content of the file of the active profile.
     */
    public static void reload() {
        loaded = false;
        loadFromFile();
    }

    private static void loadFromFile() {
        if (!loaded) readFromFile(NORMALIZE_SCREEN_FILE);
    }

    /**
     * Replaces everything in memory with the content of the given file. Without file, no screen gets normalized, like in a new profile.
     */
    static void readFromFile(@NotNull File file) {
        NORMALIZE_SCREEN_MAP.clear();
        // Also counts as loaded when reading fails, so a broken file doesn't get read again for every screen
        loaded = true;
        if (!file.isFile()) return;
        try (FileReader reader = new FileReader(file)) {
            Type mapType = new TypeToken<Map<String, Boolean>>(){}.getType();
            Map<String, Boolean> loadedMap = GSON.fromJson(reader, mapType);
            if (loadedMap != null) NORMALIZE_SCREEN_MAP.putAll(loadedMap);
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load normalized scroll screen states from file!", e);
        }
    }

    private static void saveToFile() {
        try {
            // Ensure parent directory exists
            File parentDir = NORMALIZE_SCREEN_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(NORMALIZE_SCREEN_FILE)) {
                GSON.toJson(NORMALIZE_SCREEN_MAP, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[FANCYMENU] Failed to save normalized scroll screen states to file!", e);
        }
    }

    public static void setForScreen(@NotNull Screen screen, boolean normalize) {
        loadFromFile();
        NORMALIZE_SCREEN_MAP.put(ScreenIdentifierHandler.getIdentifierOfScreen(screen), normalize);
        saveToFile();
    }

    public static boolean shouldNormalize(Screen screen) {
        if (screen == null) return false;
        return shouldNormalize(ScreenIdentifierHandler.getIdentifierOfScreen(screen));
    }

    static boolean shouldNormalize(@NotNull String screenIdentifier) {
        loadFromFile();
        return Boolean.TRUE.equals(NORMALIZE_SCREEN_MAP.get(screenIdentifier));
    }

}
