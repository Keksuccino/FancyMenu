package de.keksuccino.fancymenu.util.rendering.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComponentParser {

    public static @NotNull Component fromJsonOrPlainText(@NotNull String serializedComponentOrPlainText) {
        serializedComponentOrPlainText = PlaceholderParser.replacePlaceholders(serializedComponentOrPlainText);
        if (!serializedComponentOrPlainText.startsWith("{") && !serializedComponentOrPlainText.startsWith("[")) {
            return Component.literal(serializedComponentOrPlainText);
        }
        MutableComponent component = fromJson(serializedComponentOrPlainText);
        return component != null ? component : Component.literal(serializedComponentOrPlainText);
    }

    @NotNull
    public static String toJson(@NotNull Component component) {
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).getOrThrow(JsonParseException::new).toString();
    }

    /**
     * Deserializes JSON into a component without treating malformed or codec-invalid user input as an exceptional failure.
     *
     * @return the deserialized component, or {@code null} when the input is malformed JSON or is not accepted by the component codec
     */
    public static @Nullable MutableComponent fromJson(@NotNull String json) {
        final JsonElement jsonElement;
        try {
            jsonElement = JsonParser.parseString(json);
        } catch (JsonSyntaxException ignored) {
            // Text beginning with '[' or '{' can be plain text. Malformed JSON is therefore an expected fallback case.
            return null;
        }
        return jsonElement == null ? null : deserializeComponent(jsonElement);
    }

    private static @Nullable MutableComponent deserializeComponent(@NotNull JsonElement jsonElement) {
        // Codec errors describe unsupported user input and use the same plain-text fallback. Unexpected runtime failures must still propagate.
        Component component = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement).result().orElse(null);
        if (component == null) return null;
        if (component instanceof MutableComponent mutableComponent) return mutableComponent;
        throw new IllegalStateException("Deserialized component was not a MutableComponent!");
    }

}
