package de.keksuccino.fancymenu.util.rendering.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComponentParser {

    private static final Logger LOGGER = LogManager.getLogger();

    public static @NotNull Component fromJsonOrPlainText(@NotNull String serializedComponentOrPlainText) {
        serializedComponentOrPlainText = PlaceholderParser.replacePlaceholders(serializedComponentOrPlainText);
        if (!serializedComponentOrPlainText.startsWith("{") && !serializedComponentOrPlainText.startsWith("[")) {
            return Component.literal(serializedComponentOrPlainText);
        } else {
            try {
                Component c = deserializeComponentFromJson(serializedComponentOrPlainText);
                if (c != null) {
                    return c;
                }
            } catch (Exception ignore) {}
            return Component.literal(serializedComponentOrPlainText);
        }
    }

    @NotNull
    public static String toJson(@NotNull Component component) {
        return toJson(component, null);
    }

    @NotNull
    public static String toJson(@NotNull Component component, @Nullable HolderLookup.Provider registries) {
        try {
            if (registries != null) {
                return de.keksuccino.fancymenu.util.rendering.ui.widget.component.ComponentSerialization.Serializer.toJson(component, registries);
            }
            return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).getOrThrow(JsonParseException::new).toString();
        } catch (Exception ex) {
            LOGGER.info("[FANCYMENU] Failed to serialize Component to JSON. Falling back to plain text.", ex);
            return new JsonPrimitive(component.getString()).toString();
        }
    }

    private static @Nullable MutableComponent deserializeComponentFromJson(@NotNull String json) {
        try {
            JsonElement jsonElement = JsonParser.parseString(json);
            return jsonElement == null ? null : deserializeComponent(jsonElement);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize Component!", ex);
            return null;
        }
    }

    private static MutableComponent deserializeComponent(JsonElement jsonElement) {
        Object var2 = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement).getOrThrow();
        if (var2 instanceof MutableComponent m) {
            return m;
        } else {
            throw new IllegalStateException("Deserialized component was not a MutableComponent!");
        }
    }

}
