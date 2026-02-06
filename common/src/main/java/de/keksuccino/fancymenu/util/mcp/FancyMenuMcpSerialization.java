package de.keksuccino.fancymenu.util.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

final class FancyMenuMcpSerialization {

    private FancyMenuMcpSerialization() {
    }

    static @NotNull JsonObject toJson(@NotNull PropertyContainer container) {
        JsonObject out = new JsonObject();
        out.addProperty("type", container.getType());
        JsonObject properties = new JsonObject();
        for (Map.Entry<String, String> entry : container.getProperties().entrySet()) {
            properties.addProperty(entry.getKey(), entry.getValue());
        }
        out.add("properties", properties);
        return out;
    }

    static @NotNull JsonObject toJson(@NotNull PropertyContainerSet set) {
        JsonObject out = new JsonObject();
        out.addProperty("type", set.getType());
        JsonArray containers = new JsonArray();
        for (PropertyContainer container : set.getContainers()) {
            containers.add(toJson(container));
        }
        out.add("containers", containers);
        return out;
    }

    static @NotNull PropertyContainer fromJsonContainer(@NotNull JsonObject json) {
        String type = getString(json, "type", "container");
        PropertyContainer container = new PropertyContainer(type);
        JsonObject properties = json.has("properties") && json.get("properties").isJsonObject()
                ? json.getAsJsonObject("properties")
                : new JsonObject();
        for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
            if (entry.getValue().isJsonNull()) {
                container.putProperty(entry.getKey(), null);
            } else {
                container.putProperty(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return container;
    }

    static @NotNull PropertyContainerSet fromJsonSet(@NotNull JsonObject json) {
        String type = getString(json, "type", "fancymenu_data");
        PropertyContainerSet set = new PropertyContainerSet(type);
        if (json.has("containers") && json.get("containers").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("containers")) {
                if (element.isJsonObject()) {
                    set.putContainer(fromJsonContainer(element.getAsJsonObject()));
                }
            }
        }
        return set;
    }

    static @NotNull JsonObject parseFancyString(@NotNull String fancyString) {
        PropertyContainerSet set = Objects.requireNonNull(
                PropertiesParser.deserializeSetFromFancyString(fancyString),
                "Failed to deserialize fancy properties string."
        );
        JsonObject out = new JsonObject();
        out.add("set", toJson(set));
        return out;
    }

    static @NotNull String stringifyFancyString(@NotNull JsonObject setJson) {
        PropertyContainerSet set = fromJsonSet(setJson);
        return PropertiesParser.serializeSetToFancyString(set);
    }

    static @NotNull JsonObject scriptToJson(@NotNull GenericExecutableBlock script) {
        PropertyContainer serialized = new PropertyContainer("mcp_script");
        script.serializeToExistingPropertyContainer(serialized);
        JsonObject out = new JsonObject();
        out.addProperty("root_identifier", script.getIdentifier());
        out.add("container", toJson(serialized));
        return out;
    }

    static @NotNull GenericExecutableBlock scriptFromJson(@NotNull JsonObject json) {
        JsonObject containerJson = json.has("container") && json.get("container").isJsonObject()
                ? json.getAsJsonObject("container")
                : null;
        if (containerJson == null) {
            throw new IllegalArgumentException("Missing 'container' object for script.");
        }
        String rootId = getString(json, "root_identifier", null);
        if (rootId == null || rootId.isBlank()) {
            throw new IllegalArgumentException("Missing 'root_identifier' for script.");
        }
        PropertyContainer container = fromJsonContainer(containerJson);
        AbstractExecutableBlock block = ExecutableBlockDeserializer.deserializeWithIdentifier(container, rootId);
        if (!(block instanceof GenericExecutableBlock genericExecutableBlock)) {
            throw new IllegalArgumentException("Script root must deserialize to GenericExecutableBlock.");
        }
        return genericExecutableBlock;
    }

    static @NotNull JsonObject requirementContainerToJson(@NotNull RequirementContainer container) {
        JsonObject out = new JsonObject();
        out.add("container", toJson(container.serialize()));
        return out;
    }

    static @NotNull RequirementContainer requirementContainerFromJson(@NotNull JsonObject json) {
        if (!json.has("container") || !json.get("container").isJsonObject()) {
            throw new IllegalArgumentException("Missing requirement 'container' object.");
        }
        PropertyContainer serialized = fromJsonContainer(json.getAsJsonObject("container"));
        return RequirementContainer.deserializeToSingleContainer(serialized);
    }

    static @NotNull JsonObject parseJsonObjectString(@NotNull String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("Expected JSON object.");
        }
        return parsed.getAsJsonObject();
    }

    static @Nullable String getString(@NotNull JsonObject json, @NotNull String key, @Nullable String fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        return element.getAsString();
    }
}
