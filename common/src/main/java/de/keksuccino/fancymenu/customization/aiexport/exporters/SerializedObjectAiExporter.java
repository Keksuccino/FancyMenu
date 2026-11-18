package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.aiexport.AiExportDefaults;
import de.keksuccino.fancymenu.customization.aiexport.AiExportReferences;
import de.keksuccino.fancymenu.customization.aiexport.AiExportRuntimeInfo;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Describes serialized elements, menu backgrounds and decoration overlays in AI exports.<br>
 * Only lists properties that differ from the defaults of the type, and turns the action scripts and requirements stored in the properties into readable lists.
 */
public final class SerializedObjectAiExporter {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, String> KNOWN_REQUIREMENT_LABELS = Map.of(
            "element_loading_requirement_container_identifier", "Loading requirements (the element only shows while they are met)",
            "widget_active_state_requirement_container_identifier", "Active state requirements (the widget can only be used while they are met)"
    );
    private static final Map<String, String> KNOWN_SCRIPT_LABELS = Map.of(
            "button_element_executable_block_identifier", "Action script (runs when the button gets clicked)",
            "ticker_element_executable_block_identifier", "Action script (runs on every tick of the ticker)",
            "slider_element_executable_block_identifier", "Action script (runs when the value of the slider changes)",
            "checkbox_element_executable_block_identifier", "Action script of the checkbox"
    );

    private SerializedObjectAiExporter() {
    }

    /**
     * Finds what users changed in the serialized object.
     *
     * @param defaults       The defaults of the type of the object.
     * @param shownElsewhere Properties the caller writes itself, like the identifier of the object.
     */
    @NotNull
    public static Analysis analyze(@NotNull PropertyContainer serialized, @NotNull AiExportDefaults.TypeDefaults defaults, @NotNull Set<String> shownElsewhere) {

        Map<String, RequirementContainer> containersById = new HashMap<>();
        Map<String, AbstractExecutableBlock> scriptsById = new HashMap<>();
        try {
            RequirementContainer.deserializeAll(serialized).forEach(container -> containersById.put(container.identifier, container));
            ExecutableBlockDeserializer.deserializeAll(serialized).forEach(block -> scriptsById.put(block.getIdentifier(), block));
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read the requirements and action scripts of an object for an AI export!", ex);
        }

        List<String> changedKeys = new ArrayList<>();
        Map<String, RequirementContainer> containers = new LinkedHashMap<>();
        Map<String, AbstractExecutableBlock> scripts = new LinkedHashMap<>();
        PropertyContainer serializedDefaults = defaults.serialized();
        for (String key : serialized.getProperties().keySet()) {
            // Keys in brackets are the serialized content of action scripts and requirements, which get written as lists instead
            if (key.startsWith("[") || shownElsewhere.contains(key)) continue;
            String value = serialized.getValue(key);
            if (value == null) continue;
            RequirementContainer container = containersById.get(value);
            AbstractExecutableBlock script = scriptsById.get(value);
            if (container != null) {
                if (!container.isEmpty()) containers.put(key, container);
            } else if (script != null) {
                if (!script.getExecutables().isEmpty() || !script.getValuePlaceholders().isEmpty()) scripts.put(key, script);
            } else if ((serializedDefaults == null) || !Objects.equals(serializedDefaults.getValue(key), value)) {
                changedKeys.add(key);
            }
        }

        return new Analysis(serialized, defaults, changedKeys, containers, scripts);

    }

    /**
     * Writes the changed properties, requirements and action scripts as bullets at the current indentation.
     *
     * @param runtimeObject The live object, if there is one, to add info only it knows, see {@link AiExportRuntimeInfo}.
     */
    public static void write(@NotNull AiExportWriter writer, @NotNull Analysis analysis, @NotNull AiExportReferences references, @Nullable Object runtimeObject) {

        boolean defaultsKnown = (analysis.defaults().serialized() != null);
        if (analysis.changedKeys().isEmpty()) {
            writer.fieldText("Changed properties", defaultsKnown ? "none, everything is at its default" : "none");
        } else {
            writer.bullet(defaultsKnown ? "**Changed properties** (only the ones that differ from the defaults of this type):" : "**Properties:**");
            writer.nested(() -> analysis.changedKeys().forEach(key -> writer.field(propertyLabel(key, analysis.defaults().properties()), analysis.serialized().getValue(key))));
        }

        analysis.requirements().forEach((key, container) -> {
            String label = KNOWN_REQUIREMENT_LABELS.getOrDefault(key, nameOfReference(key, "Requirements", analysis.defaults().properties()));
            RequirementAiExporter.writeContainer(writer, label, container, references, true);
        });

        analysis.scripts().forEach((key, script) -> {
            String label = KNOWN_SCRIPT_LABELS.getOrDefault(key, nameOfReference(key, "Action script", analysis.defaults().properties()));
            writer.bullet("**" + label + ":**");
            writer.nested(() -> ActionScriptAiExporter.writeScript(writer, script, references));
        });

        if (runtimeObject instanceof AiExportRuntimeInfo runtimeInfo) {
            writer.bullet("**Runtime state** (what it knows only while it runs):");
            writer.nested(() -> {
                try {
                    runtimeInfo.writeAiExportRuntimeInfo(writer);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to write runtime info for an AI export!", ex);
                    writer.bullet("(failed to read it)");
                }
            });
        }

    }

    @NotNull
    private static String propertyLabel(@NotNull String key, @NotNull Map<String, Property<?>> properties) {
        String name = nameOfProperty(key, properties);
        return AiExportWriter.inlineCode(key) + ((name != null) ? (" (" + name + ")") : "");
    }

    @NotNull
    private static String nameOfReference(@NotNull String key, @NotNull String kind, @NotNull Map<String, Property<?>> properties) {
        String name = nameOfProperty(key, properties);
        return kind + " " + AiExportWriter.inlineCode(key) + ((name != null) ? (" (" + name + ")") : "");
    }

    @Nullable
    private static String nameOfProperty(@NotNull String key, @NotNull Map<String, Property<?>> properties) {
        Property<?> property = properties.get(key);
        if (property == null) return null;
        String name = AiExportText.safe(() -> AiExportText.labelOfLocalizationKey(property.getContextMenuEntryLocalizationKeyBase()), "");
        return name.isBlank() ? null : name;
    }

    /**
     * What users changed in a serialized object.
     *
     * @param changedKeys  Keys of properties that differ from the defaults.
     * @param requirements Requirement containers that have requirements, by the key of the property that holds their identifier.
     * @param scripts      Action scripts that have content, by the key of the property that holds their identifier.
     */
    public record Analysis(@NotNull PropertyContainer serialized, @NotNull AiExportDefaults.TypeDefaults defaults, @NotNull List<String> changedKeys, @NotNull Map<String, RequirementContainer> requirements, @NotNull Map<String, AbstractExecutableBlock> scripts) {

        public boolean isUntouched() {
            return this.changedKeys.isEmpty() && this.requirements.isEmpty() && this.scripts.isEmpty();
        }

    }

}
