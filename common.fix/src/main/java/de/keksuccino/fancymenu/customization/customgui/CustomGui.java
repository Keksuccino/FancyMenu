package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.LegacyHandler;
import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CustomGui {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final File LEGACY_CUSTOM_GUIS_DIR = new File(FancyMenu.MOD_DIR, "/customguis");

    @NotNull
    public String identifier = "";
    @NotNull
    public String title = "";
    public boolean allowEsc = true;

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

        return gui;

    }

    @Legacy("Deserializes old FMv2 GUIs. Remove this in the future.")
    @NotNull
    public static List<CustomGui> deserializeLegacyGuis() {
        List<CustomGui> guis = new ArrayList<>();
        if (LEGACY_CUSTOM_GUIS_DIR.isDirectory() && !LegacyHandler.getCheckList().customGuisPorted.getValue()) {
            LOGGER.info("[FANCYMENU] Trying to port old FMv2 custom GUIs to the new FMv3 system..");
            for (String s : FileUtils.getFiles(LEGACY_CUSTOM_GUIS_DIR.getPath())) {
                File f = new File(s);
                String identifier = null;
                String title = null;
                boolean allowEsc = false;
                for (String s2 : FileUtils.getFileLines(f)) {
                    if (s2.contains("=")) {
                        String variable = s2.replace(" ", "").split("=", 2)[0].toLowerCase();
                        String value = "";
                        String rawValue = s2.split("=", 2)[1];
                        int i = 0;
                        while (i < rawValue.length()) {
                            if (rawValue.charAt(i) != ' ') {
                                value = rawValue.substring(i);
                                break;
                            }
                            i++;
                        }
                        if (variable.equals("identifier")) {
                            identifier = value;
                        }
                        if (variable.equals("title")) {
                            title = value;
                        }
                        if (variable.equals("allowesc")) {
                            if (value.equalsIgnoreCase("true")) {
                                allowEsc = true;
                            }
                        }
                    }
                }
                if (identifier != null) {
                    CustomGui gui = new CustomGui();
                    gui.identifier = identifier;
                    gui.title = title;
                    gui.allowEsc = allowEsc;
                    guis.add(gui);
                }
            }
            LegacyHandler.getCheckList().customGuisPorted.setValue(true);
            try {
                LOGGER.info("[FANCYMENU] Successfully ported old FMv2 custom GUIs to new FMv3 system! Renaming old FMv2 'customguis' directory now..");
                File renamedDir = FileUtils.generateUniqueFileName(new File(LEGACY_CUSTOM_GUIS_DIR.getPath() + "_old"), true);
                org.apache.commons.io.FileUtils.moveDirectory(LEGACY_CUSTOM_GUIS_DIR, renamedDir);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to rename old FMv2 'customguis' directory!");
                ex.printStackTrace();
            }
        }
        return guis;
    }

}
