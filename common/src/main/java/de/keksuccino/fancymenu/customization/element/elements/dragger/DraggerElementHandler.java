package de.keksuccino.fancymenu.customization.element.elements.dragger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//TODO übernehmen
public class DraggerElementHandler {

    public static final File DRAGGER_METAS_FILE = new File(FancyMenu.MOD_DIR, "/dragger_metas.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Map<String, Integer>> METAS = new HashMap<>();

    private static boolean initialized = false;

    protected static void readFile() {

        try {

            METAS.clear();

            if (!DRAGGER_METAS_FILE.isFile()) {
                DRAGGER_METAS_FILE.createNewFile();
            }

            List<String> json = FileUtils.readTextLinesFrom(DRAGGER_METAS_FILE);
            StringBuilder jsonString = new StringBuilder();
            for (String s : json) {
                jsonString.append(s);
            }

            if (jsonString.toString().isBlank()) return;

            TypeToken<Map<String, Map<String, Integer>>> token = new TypeToken<>() {};
            Map<String, Map<String, Integer>> metasFromJson = GSON.fromJson(jsonString.toString(), token);
            METAS.putAll(metasFromJson);

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read from Dragger metas file!", ex);
        }

    }

    protected static void writeFile() {

        try {

            if (!DRAGGER_METAS_FILE.isFile()) {
                DRAGGER_METAS_FILE.createNewFile();
            }

            String json = GSON.toJson(METAS);
            if (json == null) json = "";

            FileUtils.writeTextToFile(DRAGGER_METAS_FILE, false, json);

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to write to Dragger metas file!", ex);
        }

    }

    @Nullable
    public static DraggerMeta getMeta(@NotNull String elementIdentifier) {
        if (!initialized) readFile();
        initialized = true;
        Map<String, Integer> meta = METAS.get(elementIdentifier);
        if (meta != null) {
            return new DraggerMeta(toInt(meta.get("offset_x")), toInt(meta.get("offset_y")));
        }
        return null;
    }

    public static void putMeta(@NotNull String elementIdentifier, @NotNull DraggerMeta meta) {
        if (!initialized) readFile();
        initialized = true;
        Objects.requireNonNull(meta);
        Objects.requireNonNull(elementIdentifier);
        Map<String, Integer> metaMap = new HashMap<>();
        metaMap.put("offset_x", meta.offsetX);
        metaMap.put("offset_y", meta.offsetY);
        METAS.put(elementIdentifier, metaMap);
        writeFile();
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
