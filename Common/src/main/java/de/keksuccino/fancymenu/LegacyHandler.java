package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.util.AbstractOptions;
import de.keksuccino.konkrete.config.Config;
import org.jetbrains.annotations.NotNull;

public class LegacyHandler {

    protected static LegacyCheckList legacyCheckList = null;

    @NotNull
    public static LegacyHandler.LegacyCheckList getCheckList() {
        if (legacyCheckList == null) updateCheckList();
        return legacyCheckList;
    }

    public static void updateCheckList() {
        legacyCheckList = new LegacyCheckList();
    }

    public static class LegacyCheckList extends AbstractOptions {

        protected final Config config = new Config(FancyMenu.MOD_DIR.getAbsolutePath().replace("\\", "/") + "/legacy_checklist.txt");

        public final Option<Boolean> customGuisPorted = new Option<>(config, "custom_guis_ported", false, "legacy");

        public LegacyCheckList() {
            this.config.syncConfig();
            this.config.clearUnusedValues();
        }

    }

}
