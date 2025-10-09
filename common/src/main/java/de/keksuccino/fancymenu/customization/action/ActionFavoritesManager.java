package de.keksuccino.fancymenu.customization.action;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ActionFavoritesManager {

    public static final File FAVORITES_FILE = new File(FancyMenu.INSTANCE_DATA_DIR, "/action_favorites.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type FAVORITES_TYPE = new TypeToken<List<String>>() {}.getType();
    private static final List<String> FAVORITES = new ArrayList<>();

    private static boolean initialized = false;

    private ActionFavoritesManager() {
    }

    protected static void read() {

        FAVORITES.clear();

        try {

            if (!FancyMenu.INSTANCE_DATA_DIR.exists()) {
                // Ensure FancyMenu directory exists before interacting with file.
                FancyMenu.INSTANCE_DATA_DIR.mkdirs();
            }
            FAVORITES_FILE.createNewFile();

            List<String> jsonLines = FileUtils.readTextLinesFrom(FAVORITES_FILE);
            StringBuilder builder = new StringBuilder();
            jsonLines.forEach(builder::append);

            if (builder.toString().isBlank()) {
                return;
            }

            List<String> identifiers = GSON.fromJson(builder.toString(), FAVORITES_TYPE);
            if (identifiers != null) {
                // Preserve insertion order without duplicates.
                LinkedHashSet<String> unique = new LinkedHashSet<>();
                for (String identifier : identifiers) {
                    if ((identifier != null) && !identifier.isBlank()) {
                        unique.add(identifier);
                    }
                }
                FAVORITES.addAll(unique);
            }

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read action favorites from file!", ex);
        }

    }

    protected static void write() {

        if (!initialized) read();
        initialized = true;

        try {

            if (!FancyMenu.INSTANCE_DATA_DIR.exists()) {
                FancyMenu.INSTANCE_DATA_DIR.mkdirs();
            }
            FAVORITES_FILE.createNewFile();

            String json = GSON.toJson(FAVORITES, FAVORITES_TYPE);
            FileUtils.writeTextToFile(FAVORITES_FILE, false, Objects.requireNonNull(json));

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to write action favorites to file!", ex);
        }

    }

    @NotNull
    public static List<String> getFavorites() {
        if (!initialized) read();
        initialized = true;
        return new ArrayList<>(FAVORITES);
    }

    public static boolean isFavorite(@NotNull String identifier) {
        Objects.requireNonNull(identifier);
        if (!initialized) read();
        initialized = true;
        return FAVORITES.contains(identifier);
    }

    public static void addFavorite(@NotNull String identifier) {
        Objects.requireNonNull(identifier);
        if (!initialized) read();
        initialized = true;
        if (!FAVORITES.contains(identifier)) {
            FAVORITES.add(identifier);
            write();
        }
    }

    public static void removeFavorite(@NotNull String identifier) {
        Objects.requireNonNull(identifier);
        if (!initialized) read();
        initialized = true;
        if (FAVORITES.remove(identifier)) {
            write();
        }
    }

    public static void toggleFavorite(@NotNull String identifier) {
        Objects.requireNonNull(identifier);
        if (isFavorite(identifier)) {
            removeFavorite(identifier);
        } else {
            addFavorite(identifier);
        }
    }

    public static void retainFavorites(@NotNull Set<String> validIdentifiers) {
        Objects.requireNonNull(validIdentifiers);
        if (!initialized) read();
        initialized = true;
        boolean changed = FAVORITES.removeIf(identifier -> !validIdentifiers.contains(identifier));
        if (changed) {
            write();
        }
    }

}
