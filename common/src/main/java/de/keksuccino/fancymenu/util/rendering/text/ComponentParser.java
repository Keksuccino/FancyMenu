package de.keksuccino.fancymenu.util.rendering.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ComponentParser {

    private static final Logger LOGGER = LogManager.getLogger();

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
        return toJson(component, null);
    }

    @NotNull
    public static String toJson(@NotNull Component component, @Nullable HolderLookup.Provider registries) {
        try {
            Component serializableComponent = sanitizeUnsafeClickEvents(component);
            if (registries != null) {
                return Component.Serializer.toJson(serializableComponent, registries);
            }
            return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, serializableComponent).getOrThrow(JsonParseException::new).toString();
        } catch (Exception ex) {
            LOGGER.info("[FANCYMENU] Failed to serialize Component to JSON. Falling back to plain text.", ex);
            return new JsonPrimitive(component.getString()).toString();
        }
    }

    /**
     * Client-created components may contain actions such as OPEN_FILE which Minecraft intentionally refuses to encode with its network-safe component codec.
     * Flattening is only used for these components: it preserves their visible localized text and effective styles while removing unsafe click actions from all rendered segments.
     */
    private static @NotNull Component sanitizeUnsafeClickEvents(@NotNull Component component) {
        if (!containsUnsafeClickEvent(component)) {
            return component;
        }
        MutableComponent sanitized = Component.empty();
        for (Component segment : component.toFlatList()) {
            sanitized.append(segment.copy().setStyle(sanitizeUnsafeClickEvents(segment.getStyle())));
        }
        return sanitized;
    }

    private static boolean containsUnsafeClickEvent(@NotNull Component component) {
        return component.visit((style, contents) -> containsUnsafeClickEvent(style) ? Optional.of(true) : Optional.empty(), Style.EMPTY).orElse(false);
    }

    private static boolean containsUnsafeClickEvent(@NotNull Style style) {
        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent != null && !clickEvent.getAction().isAllowedFromServer()) {
            return true;
        }
        HoverEvent hoverEvent = style.getHoverEvent();
        if (hoverEvent == null) {
            return false;
        }
        Component hoverText = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
        if (hoverText != null) {
            return containsUnsafeClickEvent(hoverText);
        }
        HoverEvent.EntityTooltipInfo entity = hoverEvent.getValue(HoverEvent.Action.SHOW_ENTITY);
        return entity != null && entity.name.map(ComponentParser::containsUnsafeClickEvent).orElse(false);
    }

    private static @NotNull Style sanitizeUnsafeClickEvents(@NotNull Style style) {
        Style sanitized = style;
        ClickEvent clickEvent = sanitized.getClickEvent();
        if (clickEvent != null && !clickEvent.getAction().isAllowedFromServer()) {
            sanitized = sanitized.withClickEvent(null);
        }
        HoverEvent hoverEvent = sanitized.getHoverEvent();
        if (hoverEvent == null) {
            return sanitized;
        }
        Component hoverText = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
        if (hoverText != null && containsUnsafeClickEvent(hoverText)) {
            sanitized = sanitized.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, sanitizeUnsafeClickEvents(hoverText)));
        } else {
            HoverEvent.EntityTooltipInfo entity = hoverEvent.getValue(HoverEvent.Action.SHOW_ENTITY);
            if (entity != null && entity.name.map(ComponentParser::containsUnsafeClickEvent).orElse(false)) {
                sanitized = sanitized.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityTooltipInfo(entity.type, entity.id, entity.name.map(ComponentParser::sanitizeUnsafeClickEvents))));
            }
        }
        return sanitized;
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
