
package de.keksuccino.fancymenu.menu.fancy.customlocals;

import de.keksuccino.konkrete.localization.Locals;

import java.io.File;

public class CustomLocalsHandler {

    public static final File CUSTOM_LOCALS_DIR = new File("config/fancymenu/custom_locals");

    public static void loadLocalizations() {
        if (!CUSTOM_LOCALS_DIR.exists()) {
            CUSTOM_LOCALS_DIR.mkdirs();
        }
        for (File f : CUSTOM_LOCALS_DIR.listFiles()) {
            if (f.isDirectory()) {
                Locals.getLocalsFromDir(f.getPath());
            }
        }
    }

}
