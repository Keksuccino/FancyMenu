package de.keksuccino.fancymenu.customization.customlocals;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("all")
public class CustomLocalsHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, String> LOCALIZATIONS = new ConcurrentHashMap<>();

    public static final File CUSTOM_LOCALS_DIR = new File("config/fancymenu/custom_locals");

    public static void loadLocalizations() {
        LOCALIZATIONS.clear();
        if (!CUSTOM_LOCALS_DIR.exists()) {
            CUSTOM_LOCALS_DIR.mkdirs();
        }
        File[] files = CUSTOM_LOCALS_DIR.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                loadLocalizationsFromDir(f);
            }
        }
    }

    public static String localize(String key) {
        return LOCALIZATIONS.getOrDefault(key, key);
    }

    private static void loadLocalizationsFromDir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                loadLocalizationsFromDir(file);
                continue;
            }
            String name = file.getName().toLowerCase();
            if (name.endsWith(".json")) {
                loadJson(file);
            } else if (name.endsWith(".lang") || name.endsWith(".properties")) {
                loadProperties(file);
            }
        }
    }

    private static void loadJson(File file) {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonObject()) {
                loadJsonObject("", element.getAsJsonObject());
            }
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load custom localization file: " + file.getPath(), e);
        }
    }

    private static void loadJsonObject(String prefix, JsonObject object) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                LOCALIZATIONS.put(key, value.getAsString());
            } else if (value.isJsonObject()) {
                loadJsonObject(key, value.getAsJsonObject());
            }
        }
    }

    private static void loadProperties(File file) {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            for (String key : properties.stringPropertyNames()) {
                LOCALIZATIONS.put(key, properties.getProperty(key));
            }
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load custom localization file: " + file.getPath(), e);
        }
    }

}
