package de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
import java.util.HashMap;
import java.util.Map;

public class ScrollScreenNormalizerHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File NORMALIZE_SCREEN_FILE = new File(GameDirectoryUtils.getGameDirectory(), "normalized_scroll_screens.json");
    private static final Map<String, Boolean> NORMALIZE_SCREEN_MAP = new HashMap<>();

    private static boolean loaded = false;

    private static void loadFromFile() {
        if (loaded) return;
        try {
            if (NORMALIZE_SCREEN_FILE.exists()) {
                try (FileReader reader = new FileReader(NORMALIZE_SCREEN_FILE)) {
                    Type mapType = new TypeToken<Map<String, Boolean>>(){}.getType();
                    Map<String, Boolean> loadedMap = GSON.fromJson(reader, mapType);
                    if (loadedMap != null) {
                        NORMALIZE_SCREEN_MAP.clear();
                        NORMALIZE_SCREEN_MAP.putAll(loadedMap);
                    }
                }
            }
            loaded = true;
        } catch (IOException e) {
            LOGGER.error("[FANCYMENU] Failed to load normalized scroll screen states from file!", e);
            loaded = true; // Set to true even on error to prevent repeated attempts
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

    public static boolean shouldNormalize(@NotNull Screen screen) {
        loadFromFile();
        String id = ScreenIdentifierHandler.getIdentifierOfScreen(screen);
        if (!NORMALIZE_SCREEN_MAP.containsKey(id)) return false;
        return NORMALIZE_SCREEN_MAP.get(id);
    }

}
