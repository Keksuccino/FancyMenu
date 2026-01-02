package de.keksuccino.fancymenu.customization.background;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.SerializationHelper;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

@SuppressWarnings("unchecked")
public abstract class MenuBackgroundBuilder<T extends MenuBackground<?>> implements SerializationHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    private final String identifier;

    public MenuBackgroundBuilder(@NotNull String uniqueBackgroundIdentifier) {
        this.identifier = Objects.requireNonNull(uniqueBackgroundIdentifier);
    }

    public boolean isDeprecated() {
        return false;
    }

    @NotNull
    public abstract T buildDefaultInstance();

    /**
     * This lets you control if it should be possible to add a new instance of this background type to a layout.<br>
     * For example, by using this you can control if the background should only be available for specific types of {@link Screen}s.
     */
    public boolean shouldShowUpInEditorBackgroundMenu(@NotNull LayoutEditorScreen editor) {
        return true;
    }

    /**
     * Deserializes a background<br><br>
     *
     * If you only use {@link Property} instances for serializable data in your {@link MenuBackground},
     * you can leave this method empty, because {@link Property} instances get serialized and deserialized automatically.
     */
    public abstract void deserializeBackground(@NotNull PropertyContainer serializedBackground, @NotNull T deserializeTo);

    /**
     * Only for internal use! Don't touch this if you don't know what you're doing!
     */
    @ApiStatus.Internal
    @Nullable
    public T _deserializeBackground(PropertyContainer serializedBackground) {

        try {

            T background = this.buildDefaultInstance();

            background.instanceIdentifier = Objects.requireNonNullElse(serializedBackground.getValue("instance_identifier"), ScreenCustomization.generateUniqueIdentifier());

            background.getPropertyMap().values().forEach(property -> property.deserialize(serializedBackground));

            this.deserializeBackground(Objects.requireNonNull(serializedBackground), background);

            // Legacy background support
            if (serializedBackground.getValue("show_background") == null) {
                background.showBackground.set(true);
            }

            return background;

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize menu background: " + this.getIdentifier(), ex);
        }

        return null;

    }

    /**
     * Will serialize a {@link MenuBackground} instance to the given {@link PropertyContainer}.<br>
     * You can use every property key except {@code background_type} and {@code instance_identifier}.<br><br>
     *
     * If you only use {@link Property} instances for serializable data in your {@link MenuBackground},
     * you can leave this method empty, because {@link Property} instances get serialized and deserialized automatically.
     */
    public abstract void serializeBackground(@NotNull T background, @NotNull PropertyContainer serializeTo);

    /**
     * Only for internal use! Don't touch this if you don't know what you're doing!
     */
    @ApiStatus.Internal
    @Nullable
    public PropertyContainer _serializeBackground(MenuBackground<?> background) {

        try {

            PropertyContainer serialized = new PropertyContainer("menu_background");

            serialized.putProperty("instance_identifier", background.getInstanceIdentifier());
            serialized.putProperty("background_type", this.getIdentifier());

            background.getPropertyMap().values().forEach(property -> property.serialize(serialized));

            this.serializeBackground((T) background, serialized);

            return serialized;

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize menu background: " + this.getIdentifier(), ex);
        }

        return null;

    }

    /**
     * The UNIQUE identifier of the background type.
     */
    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * The display name of the background type. Used in the {@link LayoutEditorScreen}.
     */
    @NotNull
    public abstract Component getDisplayName();

    /**
     * The description of the background type. Used in the {@link LayoutEditorScreen}.
     */
    @Nullable
    public abstract Component getDescription();

}
