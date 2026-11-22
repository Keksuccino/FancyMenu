package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.element.elements.checkbox.CheckboxStatesHandler;
import de.keksuccino.fancymenu.customization.element.elements.dragger.DraggerElementHandler;
import de.keksuccino.fancymenu.customization.profile.ProfileHandler;
import de.keksuccino.fancymenu.util.AbstractOptions;
import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizerHandler;
import de.keksuccino.konkrete.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

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
        CheckboxStatesHandler.migrateLegacyFile();
        DraggerElementHandler.migrateLegacyFile();
    }

    /**
     * Moves a file that old FancyMenu versions kept outside of the profiles into the active profile, if the profile has no such file yet.
     * Otherwise the profile has its own file already, which FancyMenu uses instead of the legacy file, so the legacy file only gets renamed and stays as backup,
     * like "name_old.json", followed by a number if that name is taken already.<br>
     * Either way, the legacy file is gone afterwards, so profiles that get created, imported or loaded later never pick it up.
     * Nothing gets deleted, and if moving or renaming fails, the legacy file stays and the next launch tries again.<br><br>
     *
     * Has to get called once at launch, from {@link #migrateLegacyFilesAtLaunch()}.
     */
    public static void moveLegacyFileIntoActiveProfile(@NotNull Path legacyFile, @NotNull Path profileFile) {
        if (!Files.isRegularFile(legacyFile, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            if (!Files.exists(profileFile, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectories(profileFile.toAbsolutePath().getParent());
                Files.move(legacyFile, profileFile);
                LOGGER.info("[FANCYMENU] Moved a legacy file into the active profile: " + legacyFile + " -> " + profileFile);
                return;
            }
            Path backup = renameToBackup(legacyFile);
            LOGGER.info("[FANCYMENU] The active profile has its own " + profileFile.getFileName() + " file already, so the outdated legacy file got renamed to: " + backup);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to move a legacy file into the active profile: " + legacyFile, ex);
        }
    }

    @NotNull
    private static Path renameToBackup(@NotNull Path legacyFile) throws IOException {
        Path directory = legacyFile.toAbsolutePath().getParent();
        String fileName = legacyFile.getFileName().toString();
        int extensionStart = fileName.lastIndexOf('.');
        String baseName = (extensionStart > 0) ? fileName.substring(0, extensionStart) : fileName;
        String extension = (extensionStart > 0) ? fileName.substring(extensionStart) : "";
        int number = 0;
        while (true) {
            Path backup = directory.resolve(baseName + "_old" + ((number > 0) ? ("_" + number) : "") + extension);
            number++;
            if (Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) continue;
            // Without REPLACE_EXISTING, the move fails instead of replacing a file that got created after the check
            Files.move(legacyFile, backup);
            return backup;
        }
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
