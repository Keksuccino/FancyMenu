package de.keksuccino.fancymenu.util.rendering.ui.widget;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * This interface gets applied to the {@link AbstractWidget} class to add a bunch of helper methods for easier customization.
 */
@SuppressWarnings("unused")
public interface CustomizableWidget {

    @Nullable
    default Component getOriginalMessageFancyMenu() {
        Component custom = this.getCustomLabelFancyMenu();
        Component hover = this.getHoverLabelFancyMenu();
        this.setCustomLabelFancyMenu(null);
        this.setHoverLabelFancyMenu(null);
        Component original = null;
        if (this instanceof AbstractWidget w) original = w.getMessage();
        this.setCustomLabelFancyMenu(custom);
        this.setHoverLabelFancyMenu(hover);
        return original;
    }

    void setCustomLabelFancyMenu(@Nullable Component label);

    @Nullable
    Component getCustomLabelFancyMenu();

    void setHoverLabelFancyMenu(@Nullable Component hoverLabel);

    @Nullable
    Component getHoverLabelFancyMenu();

    void setCustomClickSoundFancyMenu(@Nullable String wavClickSoundPath);

    @Nullable
    String getCustomClickSoundFancyMenu();

    void setHoverSoundFancyMenu(@Nullable String wavHoverSoundPath);

    @Nullable
    String getHoverSoundFancyMenu();

    void setHiddenFancyMenu(boolean hidden);

    boolean isHiddenFancyMenu();

}
