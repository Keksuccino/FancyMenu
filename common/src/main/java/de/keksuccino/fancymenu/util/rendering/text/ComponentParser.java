package de.keksuccino.fancymenu.util.rendering.text;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ComponentParser {

    private static final Logger LOGGER = LogManager.getLogger();

    public static @NotNull Component fromJsonOrPlainText(@NotNull String serializedComponentOrPlainText) {
        serializedComponentOrPlainText = PlaceholderParser.replacePlaceholders(serializedComponentOrPlainText);
        if (!serializedComponentOrPlainText.startsWith("{") && !serializedComponentOrPlainText.startsWith("[")) {
            return Component.literal(serializedComponentOrPlainText);
        } else {
            try {
                Component c = Component.Serializer.fromJson(serializedComponentOrPlainText);
                if (c != null) {
                    return c;
                }
            } catch (Exception ignore) {}
            return Component.literal(serializedComponentOrPlainText);
        }
    }

    @NotNull
    public static String toJson(@NotNull Component component) {
        return Component.Serializer.toJson(component);
    }

}
