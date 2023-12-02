package de.keksuccino.fancymenu.customization.background;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

@SuppressWarnings("all")
public abstract class MenuBackgroundBuilder<T extends MenuBackground> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String identifier;

    public MenuBackgroundBuilder(String uniqueIdentifier) {
        this.identifier = uniqueIdentifier;
    }

    public boolean isDeprecated() {
        return false;
    }

    /**
     * Will build a new instance of the background type or edit an existing one.<br>
     * This is used in the {@link LayoutEditorScreen} when adding the background type to a layout and when editing an existing instance of the background type, so it's possible to open a screen here that has configuration options to construct/edit the {@link MenuBackground} instance.<br>
     * Once you're done constructing/editing the background, return the new/edited background instance by using the {@code backgroundConsumer} parameter.
     */
    public abstract void buildNewOrEditInstance(@Nullable Screen currentScreen, @Nullable T backgroundToEdit, @NotNull Consumer<T> backgroundConsumer);

    /**
     * Only for internal use! Don't touch this if you don't know what you're doing!
     */
    public void buildNewOrEditInstanceInternal(@Nullable Screen currentScreen, @Nullable MenuBackground backgroundToEdit, Consumer<MenuBackground> backgroundConsumer) {
        try {
            this.buildNewOrEditInstance(currentScreen, (T) backgroundToEdit, (Consumer<T>) backgroundConsumer);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Deserializes a {@link SerializedMenuBackground}.
     */
    public abstract T deserializeBackground(SerializedMenuBackground serializedMenuBackground);

    /**
     * Only for internal use! Don't touch this if you don't know what you're doing!
     */
    @Nullable
    public T deserializeBackgroundInternal(SerializedMenuBackground serializedMenuBackground) {

        try {
            return this.deserializeBackground(serializedMenuBackground);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize menu background: " + this.getIdentifier());
            ex.printStackTrace();
        }

        return null;

    }

    /**
     * Will serialize a {@link MenuBackground} instance to a {@link SerializedMenuBackground}.<br>
     * You can use every property key except of {@code background_type}, which is reserved for the identifier of the background.
     */
    public abstract SerializedMenuBackground serializedBackground(T background);

    /**
     * Only for internal use! Don't touch this if you don't know what you're doing!
     */
    @Nullable
    public SerializedMenuBackground serializedBackgroundInternal(MenuBackground background) {

        try {

            SerializedMenuBackground b = this.serializedBackground((T) background);

            b.putProperty("background_type", this.getIdentifier());

            return b;

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize menu background: " + this.getIdentifier());
            ex.printStackTrace();
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
     * @return
     */
    @Nullable
    public abstract Component[] getDescription();

}
