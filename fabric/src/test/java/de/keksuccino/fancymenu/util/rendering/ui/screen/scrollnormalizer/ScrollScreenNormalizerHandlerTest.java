package de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer;

import de.keksuccino.fancymenu.LegacyHandler;
import de.keksuccino.fancymenu.customization.profile.ProfileMetadata;
import de.keksuccino.fancymenu.customization.profile.ProfileStorage;
import de.keksuccino.fancymenu.customization.profile.ProfileSwitcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers moving the normalized scroll screens of the game directory, where old FancyMenu versions kept them, into the profile that is active
 * at launch, and that no other profile picks them up later. Profiles get created and switched like in the game, through {@link ProfileSwitcher}.
 */
class ScrollScreenNormalizerHandlerTest {

    private static final String FILE_NAME = "normalized_scroll_screens.json";
    private static final String LEGACY_CONTENT = "{\"legacy_screen\": true}";
    private static final String PROFILE_CONTENT = "{\"profile_screen\": true}";

    @TempDir
    Path gameDirectory;
    private Path legacyFile;
    private Path activeProfileFile;
    private ProfileStorage storage;
    private ProfileSwitcher switcher;

    @BeforeEach
    void setUp() throws IOException {
        this.legacyFile = this.gameDirectory.resolve(FILE_NAME);
        this.storage = new ProfileStorage(this.gameDirectory.resolve("config").resolve("fancymenu"));
        this.switcher = new ProfileSwitcher(this.storage);
        this.activeProfileFile = this.storage.getRootDirectory().resolve(FILE_NAME);
        this.storage.writeMetadata(this.storage.getRootDirectory(), new ProfileMetadata("original"));
        Files.writeString(this.legacyFile, LEGACY_CONTENT, StandardCharsets.UTF_8);
    }

    @AfterEach
    void clearScreens() {
        ScrollScreenNormalizerHandler.readFromFile(this.gameDirectory.resolve("missing.json").toFile());
    }

    @Test
    void firstLaunchAfterUpdateMovesTheLegacyFileIntoTheActiveProfileOnce() throws IOException {
        this.launch();

        assertEquals(LEGACY_CONTENT, read(this.activeProfileFile));
        assertEquals(List.of(), this.legacyFilesOfGameDirectory());
        assertTrue(this.loadActiveProfile("legacy_screen"));

        this.launch();

        assertEquals(LEGACY_CONTENT, read(this.activeProfileFile));
        assertEquals(List.of(), this.legacyFilesOfGameDirectory());
    }

    @Test
    void newEmptyProfileDoesNotImportTheLegacyFile() throws IOException {
        this.launch();
        this.createEmptyProfile("fresh");

        assertTrue(this.switcher.switchTo("original", "fresh", true));

        assertFalse(Files.exists(this.activeProfileFile));
        assertFalse(this.loadActiveProfile("legacy_screen"));
        // The next launch happens with the new profile active
        this.launch();
        assertFalse(Files.exists(this.activeProfileFile));
        assertFalse(this.loadActiveProfile("legacy_screen"));

        assertTrue(this.switcher.switchTo("fresh", "original", true));

        assertEquals(LEGACY_CONTENT, read(this.activeProfileFile));
        assertTrue(this.loadActiveProfile("legacy_screen"));
        assertFalse(Files.exists(this.storage.getStoredProfileDirectory("fresh").resolve(FILE_NAME)));
    }

    @Test
    void alreadyMigratedInstallKeepsItsOwnFileAndOnlyRenamesTheLegacyFile() throws IOException {
        Files.writeString(this.activeProfileFile, PROFILE_CONTENT, StandardCharsets.UTF_8);

        this.launch();

        assertEquals(PROFILE_CONTENT, read(this.activeProfileFile));
        assertTrue(this.loadActiveProfile("profile_screen"));
        assertFalse(this.loadActiveProfile("legacy_screen"));
        Path backup = this.gameDirectory.resolve("normalized_scroll_screens_old.json");
        assertEquals(List.of(backup), this.legacyFilesOfGameDirectory());
        assertEquals(LEGACY_CONTENT, read(backup));

        this.createEmptyProfile("fresh");
        this.switcher.switchTo("original", "fresh", true);
        this.launch();

        assertFalse(Files.exists(this.activeProfileFile));
        assertFalse(this.loadActiveProfile("legacy_screen"));
        assertEquals(List.of(backup), this.legacyFilesOfGameDirectory());
        assertEquals(LEGACY_CONTENT, read(backup));
    }

    @Test
    void renamingTheLegacyFileNeverReplacesAnExistingBackup() throws IOException {
        Files.writeString(this.activeProfileFile, PROFILE_CONTENT, StandardCharsets.UTF_8);
        Path olderBackup = this.gameDirectory.resolve("normalized_scroll_screens_old.json");
        Files.writeString(olderBackup, "older backup", StandardCharsets.UTF_8);

        this.launch();

        assertEquals("older backup", read(olderBackup));
        Path backup = this.gameDirectory.resolve("normalized_scroll_screens_old_1.json");
        assertEquals(LEGACY_CONTENT, read(backup));
        assertEquals(List.of(olderBackup, backup), this.legacyFilesOfGameDirectory());
    }

    @Test
    void missingLegacyFileChangesNothing() throws IOException {
        Files.delete(this.legacyFile);

        this.launch();

        assertFalse(Files.exists(this.activeProfileFile));
        assertEquals(List.of(), this.legacyFilesOfGameDirectory());
    }

    /**
     * Does what FancyMenu does with the file at launch, which happens with the profile that is active at that time.
     */
    private void launch() {
        LegacyHandler.moveLegacyFileIntoActiveProfile(this.legacyFile, this.activeProfileFile);
    }

    /**
     * Reads the file of the active profile like after a launch or profile switch.
     *
     * @return whether the given screen gets normalized with the active profile.
     */
    private boolean loadActiveProfile(@NotNull String screenIdentifier) {
        ScrollScreenNormalizerHandler.readFromFile(this.activeProfileFile.toFile());
        return ScrollScreenNormalizerHandler.shouldNormalize(screenIdentifier);
    }

    /**
     * Creates a profile like a new empty profile of the profile manager, which only has its metadata.
     */
    private void createEmptyProfile(@NotNull String id) throws IOException {
        this.storage.writeMetadata(this.storage.getStoredProfileDirectory(id), new ProfileMetadata(id));
    }

    /**
     * All files of the game directory that start like the legacy file, sorted by name.
     */
    @NotNull
    private List<Path> legacyFilesOfGameDirectory() throws IOException {
        try (Stream<Path> files = Files.list(this.gameDirectory)) {
            return files.filter(path -> path.getFileName().toString().startsWith("normalized_scroll_screens")).sorted().toList();
        }
    }

    @NotNull
    private static String read(@NotNull Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

}
