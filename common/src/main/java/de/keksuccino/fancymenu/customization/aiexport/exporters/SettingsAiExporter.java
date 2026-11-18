package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.util.AbstractOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exports FancyMenu's settings (its options.txt) for AI chats, which includes the global customizations.
 */
public final class SettingsAiExporter {

    private static final Logger LOGGER = LogManager.getLogger();

    private SettingsAiExporter() {
    }

    @NotNull
    public static AiExportDocument export() {
        List<AbstractOptions.Option<?>> options = collectOptions();
        List<AbstractOptions.Option<?>> changed = options.stream().filter(option -> !Objects.equals(option.getValue(), option.getDefaultValue())).toList();
        Map<String, List<AbstractOptions.Option<?>>> unchangedByCategory = new LinkedHashMap<>();
        options.stream().filter(option -> !changed.contains(option)).forEach(option -> unchangedByCategory.computeIfAbsent(option.getCategory(), category -> new ArrayList<>()).add(option));
        return AiExports.create("FancyMenu Settings", "settings", writer -> {
            writer.heading(2, "FancyMenu Settings");
            writer.paragraph("All settings of FancyMenu, like they are saved in `config/fancymenu/options.txt`. This includes the global customizations, "
                    + "like the global button textures and the game intro, which apply to all screens. Settings with texture or file values have an empty value when they are not set.");
            writer.heading(3, "Settings Changed From Their Defaults (" + changed.size() + ")");
            if (changed.isEmpty()) writer.paragraph("All settings are at their defaults.");
            changed.forEach(option -> writer.field(option.getKey(), option.getValue(), "default: " + AiExportWriter.inlineCode(String.valueOf(option.getDefaultValue())) + ", category: " + option.getCategory()));
            writer.heading(3, "Settings at Their Defaults (" + (options.size() - changed.size()) + ")");
            unchangedByCategory.forEach((category, categoryOptions) -> {
                writer.bullet("**Category " + category + ":**");
                writer.nested(() -> categoryOptions.forEach(option -> writer.field(option.getKey(), option.getValue())));
            });
        });
    }

    /**
     * Finds all options of FancyMenu by reflection, so new options never need to get added here.
     */
    @NotNull
    private static List<AbstractOptions.Option<?>> collectOptions() {
        List<AbstractOptions.Option<?>> options = new ArrayList<>();
        Object holder = FancyMenu.getOptions();
        for (Field field : holder.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !AbstractOptions.Option.class.isAssignableFrom(field.getType())) continue;
            try {
                options.add((AbstractOptions.Option<?>) field.get(holder));
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to read FancyMenu option for an AI export: " + field.getName(), ex);
            }
        }
        return options;
    }

}
