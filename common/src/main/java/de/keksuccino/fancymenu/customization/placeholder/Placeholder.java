
//Copyright (c) 2022-2023 Keksuccino.
//This code is licensed under DSMSLv2.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public abstract class Placeholder {

    protected final String id;
    protected volatile boolean asyncErrorShown = false;

    public Placeholder(String id) {
        this.id = id;
    }

    /**
     * Returns the replacement (actual text) of the given placeholder string.
     *
     * @param dps The deserialized placeholder string with placeholder values.
     */
    public abstract String getReplacementFor(DeserializedPlaceholderString dps);

    /**
     * This should return the {@link Placeholder}'s value names or NULL, if it has no values.
     */
    @Nullable
    public abstract List<String> getValueNames();

    @NotNull
    public abstract String getDisplayName();

    @Nullable
    public abstract List<String> getDescription();

    public abstract String getCategory();

    @NotNull
    public abstract DeserializedPlaceholderString getDefaultPlaceholderString();

    public String getIdentifier() {
        return this.id;
    }

    @Nullable
    public List<String> getAlternativeIdentifiers() {
        return null;
    }

    /**
     * This lets you control if it should be possible to add a new instance of this placeholder type to a layout.<br>
     * For example, by using this you can control if the placeholder should only be available for specific types of {@link Screen}s.
     *
     * @param editor The {@link LayoutEditorScreen} instance if the editor is open. This is NULL if the placeholder menu is open outside the editor.
     */
    public boolean shouldShowUpInPlaceholderMenu(@Nullable LayoutEditorScreen editor) {
        return true;
    }

    public boolean canRunAsync() {
        return true;
    }

    public boolean checkAsync() {
        boolean sameThread = Minecraft.getInstance().isSameThread();
        if (!sameThread && !this.canRunAsync() && !this.asyncErrorShown) {
            this.asyncErrorShown = true;
            QueueableScreenHandler.addToQueue(new AsyncPlaceholderErrorScreen(Component.literal(this.getDisplayName())));
        }
        return this.canRunAsync() || sameThread; // should parse placeholder
    }

    /**
     * Called right after the placeholder gets registered.
     */
    public void onRegistered() {
    }

}
