package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.element.elements.dragger.DraggerElementHandler;
import de.keksuccino.fancymenu.customization.profile.ProfileHandler;
import de.keksuccino.fancymenu.util.AbstractOptions;
import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizerHandler;
import de.keksuccino.konkrete.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@Legacy("This is to safe stuff for legacy handling. Remove this in the future.")
public class LegacyHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static LegacyCheckList legacyCheckList = null;

    @NotNull
    public static LegacyHandler.LegacyCheckList getCheckList() {
        if (legacyCheckList == null) updateCheckList();
        return legacyCheckList;
    }

    public static void updateCheckList() {
        legacyCheckList = new LegacyCheckList();
    }

    /**
     * Migrates the legacy files that move between the profiles and the rest of the game directory.<br>
     * Has to get called once at launch, right after {@link ProfileHandler#init()}, so only the profile that is active when FancyMenu gets updated
     * takes part in these migrations. They leave nothing behind that they could pick up again, so profiles that get created, imported or loaded later never do.<br><br>
     *
     * Legacy files that stay inside a profile, like the FMv2 custom GUIs of the {@link LegacyCheckList}, get migrated when the profile gets loaded instead,
     * because they belong to that profile.
     */
    public static void migrateLegacyFilesAtLaunch() {
        // A part of an incomplete active profile is still in the profile store, so the migrations can't tell whether the profile has its own files already
        if (ProfileHandler.isActiveProfileIncomplete()) {
            LOGGER.warn("[FANCYMENU] Skipping the migration of legacy files, because the last profile switch is not complete yet. The next launch migrates them.");
            return;
        }
        ScrollScreenNormalizerHandler.migrateLegacyFile();
        DraggerElementHandler.migrateLegacyFile();
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
