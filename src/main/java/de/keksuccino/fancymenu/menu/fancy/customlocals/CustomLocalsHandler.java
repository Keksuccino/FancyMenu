package de.keksuccino.fancymenu.menu.fancy.customlocals;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.localization.Locals;
import java.io.File;

public class CustomLocalsHandler {

    public static final File CUSTOM_LOCALS_DIR = new File(FancyMenu.MOD_DIR, "/custom_locals");

    public static void loadLocalizations() {
        if (!CUSTOM_LOCALS_DIR.exists()) {
            CUSTOM_LOCALS_DIR.mkdirs();
        }
        File[] files = CUSTOM_LOCALS_DIR.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    Locals.getLocalsFromDir(f.getPath());
                }
            }
        }
    }

}
