package de.keksuccino.fancymenu.util.rendering.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class ComponentParser {

    public static @NotNull Component fromJsonOrPlainText(@NotNull String serializedComponentOrPlainText) {
        serializedComponentOrPlainText = PlaceholderParser.replacePlaceholders(serializedComponentOrPlainText);
        if (!serializedComponentOrPlainText.startsWith("{") && !serializedComponentOrPlainText.startsWith("[")) {
            return Component.literal(serializedComponentOrPlainText);
        }
        MutableComponent component = deserializeComponentFromJson(serializedComponentOrPlainText);
        return component != null ? component : Component.literal(serializedComponentOrPlainText);
    }

    @NotNull
    public static String toJson(@NotNull Component component) {
        return Component.Serializer.toJson(component);
    }

    private static @Nullable MutableComponent deserializeComponentFromJson(@NotNull String json) {
        final JsonElement jsonElement;
        try {
            jsonElement = JsonParser.parseString(json);
        } catch (JsonSyntaxException ignored) {
            // Text beginning with '[' or '{' can be plain text. Malformed JSON is therefore an expected fallback case.
            return null;
        }
        if (jsonElement == null) return null;
        try {
            MutableComponent component = Component.Serializer.fromJson(jsonElement);
            return isValidComponent(component, Collections.newSetFromMap(new IdentityHashMap<>())) ? component : null;
        } catch (RuntimeException ex) {
            if (isExpectedUserInputFailure(ex)) return null;
            throw ex;
        }
    }

    static boolean isExpectedUserInputFailure(@NotNull RuntimeException exception) {
        // 1.19.2 reports codec-invalid resource identifiers and values as runtime argument errors instead of structured decode failures.
        return exception instanceof JsonParseException || exception instanceof ResourceLocationException || exception instanceof IllegalArgumentException;
    }

    /**
     * Minecraft 1.19.2's Gson deserializer returns null for an empty component array and can embed that null in a
     * parent array, translation argument, or hover component. Reject the resulting tree before later traversal assumes
     * that all nested components are non-null.
     */
    private static boolean isValidComponent(@Nullable Component component, @NotNull Set<Component> visitedComponents) {
        if (component == null) return false;
        if (!visitedComponents.add(component)) return true;
        if (component.getContents() instanceof TranslatableContents translatableContents) {
            for (Object argument : translatableContents.getArgs()) {
                if (argument == null || argument instanceof Component nestedComponent && !isValidComponent(nestedComponent, visitedComponents)) return false;
            }
        }
        HoverEvent hoverEvent = component.getStyle().getHoverEvent();
        if (hoverEvent != null && hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT) {
            Component hoverText = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
            if (!isValidComponent(hoverText, visitedComponents)) return false;
        }
        for (Component sibling : component.getSiblings()) {
            if (!isValidComponent(sibling, visitedComponents)) return false;
        }
        return true;
    }

}
