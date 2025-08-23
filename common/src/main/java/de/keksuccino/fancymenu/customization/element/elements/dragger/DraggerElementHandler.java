package de.keksuccino.fancymenu.customization.element.elements.dragger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.FancyMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DraggerElementHandler {

    public static final File DRAGGER_METAS_FILE = new File(FancyMenu.INSTANCE_DATA_DIR, "/dragger_metas.json");
    private static final File OLD_DRAGGER_METAS_FILE = new File(FancyMenu.MOD_DIR, "/dragger_metas.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Map<String, Integer>> DRAGGER_METAS_MAP = new HashMap<>();

    private static boolean loaded = false;
    

    private static void loadFromFile() {
        if (loaded) return;
        try {
            File dir = DRAGGER_METAS_FILE;
            if (!dir.exists()) dir = OLD_DRAGGER_METAS_FILE;
            if (dir.exists()) {
                try (FileReader reader = new FileReader(dir)) {
                    Type mapType = new TypeToken<Map<String, Map<String, Integer>>>(){}.getType();
                    Map<String, Map<String, Integer>> loadedMap = GSON.fromJson(reader, mapType);
                    if (loadedMap != null) {
                        DRAGGER_METAS_MAP.clear();
                        DRAGGER_METAS_MAP.putAll(loadedMap);
                    }
                }
                if (dir == OLD_DRAGGER_METAS_FILE) {
                    saveToFile(); // save to new file
                }
            }
            loaded = true;
        } catch (IOException e) {
            LOGGER.error("[FANCYMENU] Failed to load Dragger offsets data from file!", e);
            loaded = true; // Set to true even on error to prevent repeated attempts
        }
    }

    private static void saveToFile() {
        try {
            // Ensure parent directory exists
            File parentDir = DRAGGER_METAS_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(DRAGGER_METAS_FILE)) {
                GSON.toJson(DRAGGER_METAS_MAP, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[FANCYMENU] Failed to save Dragger offsets data to file!", e);
        }
    }

    @Nullable
    public static DraggerMeta getMeta(@NotNull String elementIdentifier) {
        loadFromFile();
        Map<String, Integer> meta = DRAGGER_METAS_MAP.get(elementIdentifier);
        if (meta != null) {
            return new DraggerMeta(toInt(meta.get("offset_x")), toInt(meta.get("offset_y")));
        }
        return null;
    }

    public static void putMeta(@NotNull String elementIdentifier, @NotNull DraggerMeta meta) {
        loadFromFile();
        Objects.requireNonNull(meta);
        Objects.requireNonNull(elementIdentifier);
        Map<String, Integer> metaMap = new HashMap<>();
        metaMap.put("offset_x", meta.offsetX);
        metaMap.put("offset_y", meta.offsetY);
        DRAGGER_METAS_MAP.put(elementIdentifier, metaMap);
        saveToFile();
    }

    public static void putMeta(@NotNull String elementIdentifier, int offsetX, int offsetY) {
        putMeta(elementIdentifier, new DraggerMeta(offsetX, offsetY));
    }

    private static int toInt(@Nullable Integer integer) {
        if (integer == null) return 0;
        return integer;
    }

    public static class DraggerMeta {

        public int offsetX;
        public int offsetY;

        public DraggerMeta(int x, int y) {
            this.offsetX = x;
            this.offsetY = y;
        }

    }

}
