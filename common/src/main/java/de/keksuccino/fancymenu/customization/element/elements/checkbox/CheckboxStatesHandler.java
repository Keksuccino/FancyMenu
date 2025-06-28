package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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

public class CheckboxStatesHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CHECKBOX_STATE_FILE = new File(GameDirectoryUtils.getGameDirectory(), "checkbox_states.json");
    private static final Map<String, Boolean> STATES_MAP = new HashMap<>();

    private static boolean loaded = false;

    private static void loadFromFile() {
        if (loaded) return;
        try {
            if (CHECKBOX_STATE_FILE.exists()) {
                try (FileReader reader = new FileReader(CHECKBOX_STATE_FILE)) {
                    Type mapType = new TypeToken<Map<String, Boolean>>(){}.getType();
                    Map<String, Boolean> loadedMap = GSON.fromJson(reader, mapType);
                    if (loadedMap != null) {
                        STATES_MAP.clear();
                        STATES_MAP.putAll(loadedMap);
                    }
                }
            }
            loaded = true;
        } catch (IOException e) {
            LOGGER.error("[FANCYMENU] Failed to load checkbox states from file!", e);
            loaded = true; // Set to true even on error to prevent repeated attempts
        }
    }

    private static void saveToFile() {
        try {
            // Ensure parent directory exists
            File parentDir = CHECKBOX_STATE_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CHECKBOX_STATE_FILE)) {
                GSON.toJson(STATES_MAP, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[FANCYMENU] Failed to save checkbox states to file!", e);
        }
    }

    public static void setForCheckboxElement(@NotNull CheckboxElement element, boolean state) {
        loadFromFile();
        STATES_MAP.put(element.getInstanceIdentifier(), state);
        saveToFile();
    }

    public static boolean getForCheckboxElement(@NotNull CheckboxElement element) {
        loadFromFile();
        String id = element.getInstanceIdentifier();
        if (!STATES_MAP.containsKey(id)) return false;
        return STATES_MAP.get(id);
    }

}
