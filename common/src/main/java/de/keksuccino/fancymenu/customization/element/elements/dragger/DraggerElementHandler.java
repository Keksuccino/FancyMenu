package de.keksuccino.fancymenu.customization.element.elements.dragger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.LegacyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DraggerElementHandler {

    public static final File DRAGGER_METAS_FILE = new File(FancyMenu.INSTANCE_DATA_DIR, "/dragger_metas.json");
    private static final File LEGACY_DRAGGER_METAS_FILE = new File(FancyMenu.MOD_DIR, "/dragger_metas.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Map<String, Integer>> DRAGGER_METAS_MAP = new HashMap<>();

    private static boolean loaded = false;

    /**
     * Copies the dragger offsets that old FancyMenu versions kept in the config directory into the instance data, where they stay
     * no matter which profile is active. Has to get called once at launch, before the active profile can change, see {@link LegacyHandler#migrateLegacyFilesAtLaunch()}.
     */
    public static void migrateLegacyFile() {
        migrateLegacyFile(LEGACY_DRAGGER_METAS_FILE.toPath(), DRAGGER_METAS_FILE.toPath());
    }

    /**
     * Copies the legacy file of the active profile into the instance data, or creates an empty instance file if the profile has none.<br>
     * The instance file always exists afterwards, so the legacy files that stay in profiles never get read again,
     * also not the ones of profiles that get created, imported or loaded later. If that fails, the next launch tries again.
     */
    static void migrateLegacyFile(@NotNull Path legacyFile, @NotNull Path instanceFile) {
        if (Files.exists(instanceFile, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            Files.createDirectories(instanceFile.toAbsolutePath().getParent());
            if (Files.isRegularFile(legacyFile, LinkOption.NOFOLLOW_LINKS)) {
                Files.copy(legacyFile, instanceFile);
                LOGGER.info("[FANCYMENU] Copied the dragger offsets of the active profile into the instance data: " + instanceFile);
            } else {
                Files.writeString(instanceFile, "{}", StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to migrate the dragger offsets of the active profile: " + legacyFile, ex);
        }
    }

    private static void loadFromFile() {
        if (loaded) return;
        // Also counts as loaded when reading fails, so a broken file doesn't get read again for every dragger
        loaded = true;
        if (!DRAGGER_METAS_FILE.isFile()) return;
        try (FileReader reader = new FileReader(DRAGGER_METAS_FILE)) {
            Type mapType = new TypeToken<Map<String, Map<String, Integer>>>(){}.getType();
            Map<String, Map<String, Integer>> loadedMap = GSON.fromJson(reader, mapType);
            if (loadedMap != null) {
                DRAGGER_METAS_MAP.clear();
                DRAGGER_METAS_MAP.putAll(loadedMap);
            }
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load Dragger offsets data from file!", e);
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
