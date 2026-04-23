package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.util.List;
import java.util.Objects;

public class BrowserAudioSettings {

    public static final File SETTINGS_FILE = new File(FancyMenu.INSTANCE_DATA_DIR, "/browser_audio_settings.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean initialized = false;
    private static float volume = 1.0F;

    private BrowserAudioSettings() {
    }

    public static float getVolume() {
        if (!initialized) read();
        initialized = true;
        return volume;
    }

    public static void setVolume(float newVolume) {
        if (!initialized) read();
        initialized = true;
        volume = clampVolume(newVolume);
        write();
    }

    private static void read() {
        try {
            if (!FancyMenu.INSTANCE_DATA_DIR.exists()) {
                FancyMenu.INSTANCE_DATA_DIR.mkdirs();
            }
            SETTINGS_FILE.createNewFile();
            List<String> lines = FileUtils.readTextLinesFrom(SETTINGS_FILE);
            StringBuilder builder = new StringBuilder();
            lines.forEach(builder::append);
            String raw = builder.toString();
            if (raw.isBlank()) return;
            SettingsData data = GSON.fromJson(raw, SettingsData.class);
            if (data != null) {
                volume = clampVolume(data.volume);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read browser audio settings!", ex);
        }
    }

    private static void write() {
        try {
            if (!FancyMenu.INSTANCE_DATA_DIR.exists()) {
                FancyMenu.INSTANCE_DATA_DIR.mkdirs();
            }
            SETTINGS_FILE.createNewFile();
            String json = GSON.toJson(new SettingsData(volume));
            FileUtils.writeTextToFile(SETTINGS_FILE, false, Objects.requireNonNull(json));
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to write browser audio settings!", ex);
        }
    }

    private static float clampVolume(float value) {
        if (value < 0.0F) return 0.0F;
        if (value > 1.0F) return 1.0F;
        return value;
    }

    private static class SettingsData {

        public float volume = 1.0F;

        public SettingsData(float volume) {
            this.volume = volume;
        }

    }

}
