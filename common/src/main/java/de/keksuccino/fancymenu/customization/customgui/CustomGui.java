package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomGui {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    public String identifier = "";
    @NotNull
    public String title = "";
    public boolean allowEsc = true;
    //TODO übernehmen
    public boolean worldBackground = true;
    public boolean worldBackgroundOverlay = true;
    public boolean pauseGame = true;
    //-------------

    @NotNull
    public CustomGui copy() {
        CustomGui copy = deserialize(this.serialize());
        //Should never happen
        if (copy == null) copy = new CustomGui();
        return copy;
    }

    @NotNull
    public PropertyContainer serialize() {
        PropertyContainer container = new PropertyContainer("custom_gui");
        container.putProperty("identifier", this.identifier);
        container.putProperty("title", this.title);
        container.putProperty("allow_esc", "" + this.allowEsc);
        //TODO übernehmen
        container.putProperty("transparent_world_background", "" + this.worldBackground);
        container.putProperty("transparent_world_background_overlay", "" + this.worldBackgroundOverlay);
        container.putProperty("pause_game", "" + this.pauseGame);
        //-----------------------
        return container;
    }

    @Nullable
    public static CustomGui deserialize(@NotNull PropertyContainer serialized) {

        CustomGui gui = new CustomGui();

        String id = serialized.getValue("identifier");
        if (id == null) return null;
        if (id.replace(" ", "").isEmpty()) return null;
        gui.identifier = id;

        String title = serialized.getValue("title");
        if (title != null) gui.title = title;

        String allowEsc = serialized.getValue("allow_esc");
        if (allowEsc != null) {
            if (allowEsc.equals("true")) gui.allowEsc = true;
            if (allowEsc.equals("false")) gui.allowEsc = false;
        }

        //TODO übernehmen
        gui.worldBackground = SerializationUtils.deserializeBoolean(gui.worldBackground, serialized.getValue("transparent_world_background"));
        gui.worldBackgroundOverlay = SerializationUtils.deserializeBoolean(gui.worldBackgroundOverlay, serialized.getValue("transparent_world_background_overlay"));
        gui.pauseGame = SerializationUtils.deserializeBoolean(gui.pauseGame, serialized.getValue("pause_game"));
        //-----------------------

        return gui;

    }

}
