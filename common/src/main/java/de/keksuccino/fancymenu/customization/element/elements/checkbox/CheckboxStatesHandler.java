package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.LegacyHandler;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Remembers the checked states of checkbox elements. They belong to the active profile, so they live in its directory
 * and move with it when switching, exporting or copying profiles.
 */
public class CheckboxStatesHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "checkbox_states.json";
    private static final File CHECKBOX_STATE_FILE = new File(FancyMenu.MOD_DIR, FILE_NAME);
    private static final Map<String, Boolean> STATES_MAP = new HashMap<>();

    private static boolean loaded = false;

    /**
     * Moves the checkbox states that old FancyMenu versions kept in the game directory into the active profile, see {@link LegacyHandler#moveLegacyFileIntoActiveProfile}.
     * Has to get called once at launch, before the active profile can change, see {@link LegacyHandler#migrateLegacyFilesAtLaunch()}.
     */
    public static void migrateLegacyFile() {
        LegacyHandler.moveLegacyFileIntoActiveProfile(new File(GameDirectoryUtils.getGameDirectory(), FILE_NAME).toPath(), CHECKBOX_STATE_FILE.toPath());
    }

    /**
     * Replaces everything in memory with the content of the file of the active profile.
     */
    public static void reload() {
        readFromFile(CHECKBOX_STATE_FILE);
    }

    /**
     * Replaces everything in memory with the content of the given file. Without file, no checkbox is checked, like in a new profile.
     */
    static void readFromFile(@NotNull File file) {
        STATES_MAP.clear();
        // Also counts as loaded when reading fails, so a broken file doesn't get read again for every checkbox
        loaded = true;
        if (!file.isFile()) return;
        try (FileReader reader = new FileReader(file)) {
            Type mapType = new TypeToken<Map<String, Boolean>>(){}.getType();
            Map<String, Boolean> loadedMap = GSON.fromJson(reader, mapType);
            if (loadedMap != null) STATES_MAP.putAll(loadedMap);
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load checkbox states from file!", e);
        }
    }

    private static void saveToFile(@NotNull File file) {
        try {
            // Ensure parent directory exists
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(STATES_MAP, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[FANCYMENU] Failed to save checkbox states to file!", e);
        }
    }

    public static void setForCheckboxElement(@NotNull CheckboxElement element, boolean state) {
        setState(CHECKBOX_STATE_FILE, element.getInstanceIdentifier(), state);
    }

    public static boolean getForCheckboxElement(@NotNull CheckboxElement element) {
        return getState(CHECKBOX_STATE_FILE, element.getInstanceIdentifier());
    }

    /**
     * @param file the file of the active profile, which gets read first if nothing got loaded yet, and which the states get saved to.
     */
    static void setState(@NotNull File file, @NotNull String elementIdentifier, boolean state) {
        if (!loaded) readFromFile(file);
        STATES_MAP.put(elementIdentifier, state);
        saveToFile(file);
    }

    /**
     * @param file the file of the active profile, which gets read first if nothing got loaded yet.
     */
    static boolean getState(@NotNull File file, @NotNull String elementIdentifier) {
        if (!loaded) readFromFile(file);
        return Boolean.TRUE.equals(STATES_MAP.get(elementIdentifier));
    }

}
