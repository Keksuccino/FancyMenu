package de.keksuccino.fancymenu.customization.customlocals;

import de.keksuccino.konkrete.localization.Locals;
import java.io.File;

@SuppressWarnings("all")
public class CustomLocalsHandler {

    public static final File CUSTOM_LOCALS_DIR = new File("config/fancymenu/custom_locals");

    public static void loadLocalizations() {
        if (!CUSTOM_LOCALS_DIR.exists()) {
            CUSTOM_LOCALS_DIR.mkdirs();
        }
        File[] files = CUSTOM_LOCALS_DIR.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                Locals.getLocalsFromDir(f.getPath());
            }
        }
    }

}
