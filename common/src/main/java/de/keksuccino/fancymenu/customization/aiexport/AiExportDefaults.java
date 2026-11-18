package de.keksuccino.fancymenu.customization.aiexport;

import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * The default properties of element, background and decoration overlay types, so AI exports only need to list what users changed.<br>
 * Builds one default instance per type and keeps it for the lifetime of this object, which should be one export.
 */
public class AiExportDefaults {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<String, TypeDefaults> elementDefaults = new HashMap<>();
    private final Map<String, TypeDefaults> backgroundDefaults = new HashMap<>();
    private final Map<String, TypeDefaults> decorationOverlayDefaults = new HashMap<>();

    @NotNull
    public TypeDefaults forElement(@NotNull ElementBuilder<?, ?> builder) {
        return this.elementDefaults.computeIfAbsent(builder.getIdentifier(), id -> {
            try {
                AbstractElement element = builder.buildDefaultInstance();
                return new TypeDefaults(builder.serializeElementInternal(element), element.getPropertyMap());
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to build the defaults of an element type for an AI export: " + id, ex);
            }
            return TypeDefaults.UNKNOWN;
        });
    }

    @NotNull
    public TypeDefaults forBackground(@NotNull MenuBackgroundBuilder<?> builder) {
        return this.backgroundDefaults.computeIfAbsent(builder.getIdentifier(), id -> {
            try {
                MenuBackground<?> background = builder.buildDefaultInstance();
                return new TypeDefaults(builder._serializeBackground(background), background.getPropertyMap());
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to build the defaults of a menu background type for an AI export: " + id, ex);
            }
            return TypeDefaults.UNKNOWN;
        });
    }

    @NotNull
    public TypeDefaults forDecorationOverlay(@NotNull AbstractDecorationOverlayBuilder<?> builder) {
        return this.decorationOverlayDefaults.computeIfAbsent(builder.getIdentifier(), id -> {
            try {
                AbstractDecorationOverlay<?> overlay = builder.buildDefaultInstance();
                return new TypeDefaults(builder._serialize(overlay), overlay.getPropertyMap());
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to build the defaults of a decoration overlay type for an AI export: " + id, ex);
            }
            return TypeDefaults.UNKNOWN;
        });
    }

    /**
     * @param serialized The serialized default instance of the type, or NULL if it could not get built.
     * @param properties The properties of the type, to get their names.
     */
    public record TypeDefaults(@Nullable PropertyContainer serialized, @NotNull Map<String, Property<?>> properties) {

        public static final TypeDefaults UNKNOWN = new TypeDefaults(null, Map.of());

    }

}
