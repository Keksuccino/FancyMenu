package de.keksuccino.fancymenu.util.rendering.ui.theme;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class UIColorThemeSerializer {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final TypeAdapter<DrawableColor> DRAWABLE_COLOR_TYPE_ADAPTER = new TypeAdapter<>() {
        @Override
        public void write(JsonWriter out, DrawableColor value) throws IOException {
            out.beginObject();
            out.name("hex").value(value.getHex());
            out.endObject();
        }
        @Override
        public DrawableColor read(JsonReader in) throws IOException {
            String hex = null;
            in.beginObject();
            while(in.hasNext()) {
                String name = in.nextName();
                if (name.equals("hex")) {
                    hex = in.nextString();
                    break;
                }
            }
            in.endObject();
            return (hex != null) ? DrawableColor.of(hex) : DrawableColor.WHITE;
        }
    };

    @Nullable
    public static UITheme deserializeTheme(@NotNull String json) {
        Objects.requireNonNull(json);
        try {
            Gson gson = buildGsonInstance();
            return gson.fromJson(json, UITheme.class);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize FancyMenu theme!", ex);
        }
        return null;
    }

    @Nullable
    public static UITheme deserializeThemeFromResource(@NotNull ResourceLocation resource) {
        InputStream in = null;
        try {
            StringBuilder json = new StringBuilder();
            in = Objects.requireNonNull(Minecraft.getInstance().getResourceManager().open(resource));
            for (String s : FileUtils.readTextLinesFrom(in)) {
                json.append(s);
            }
            CloseableUtils.closeQuietly(in);
            return deserializeTheme(json.toString());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize FancyMenu theme from ResourceLocation: " + resource, ex);
        }
        CloseableUtils.closeQuietly(in);
        return null;
    }

    @Nullable
    public static UITheme deserializeThemeFromFile(@NotNull File file) {
        StringBuilder json = new StringBuilder();
        for (String s : FileUtils.getFileLines(file)) {
            json.append(s);
        }
        return deserializeTheme(json.toString());
    }

    @Nullable
    public static String serializeTheme(@NotNull UITheme theme) {
        Objects.requireNonNull(theme);
        try {
            Gson gson = buildGsonInstance();
            return gson.toJson(theme);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize FancyMenu theme!", ex);
        }
        return null;
    }

    public static void serializeThemeToFile(@NotNull UITheme theme, @NotNull File file) {
        Objects.requireNonNull(theme);
        Objects.requireNonNull(file);
        try {
            Gson gson = buildGsonInstance();
            String json = gson.toJson(theme);
            if (json != null) {
                FileUtils.writeTextToFile(file, false, json);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize FancyMenu theme to file!", ex);
        }
    }

    private static Gson buildGsonInstance() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(DrawableColor.class, DRAWABLE_COLOR_TYPE_ADAPTER);
        return gsonBuilder.create();
    }

}
