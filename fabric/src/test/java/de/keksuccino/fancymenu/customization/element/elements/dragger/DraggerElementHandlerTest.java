package de.keksuccino.fancymenu.customization.element.elements.dragger;

import de.keksuccino.fancymenu.customization.profile.ProfileMetadata;
import de.keksuccino.fancymenu.customization.profile.ProfileStorage;
import de.keksuccino.fancymenu.customization.profile.ProfileSwitcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers copying the dragger offsets that old FancyMenu versions kept in the config directory into the instance data, which happens only
 * with the profile that is active at launch. Profiles get created and switched like in the game, through {@link ProfileSwitcher}.
 */
class DraggerElementHandlerTest {

    private static final String FILE_NAME = "dragger_metas.json";
    private static final String LEGACY_CONTENT = "{\"legacy_dragger\": {\"offset_x\": 4, \"offset_y\": 2}}";
    private static final String INSTANCE_CONTENT = "{\"instance_dragger\": {\"offset_x\": 7, \"offset_y\": 3}}";

    @TempDir
    Path gameDirectory;
    private Path instanceFile;
    private Path activeProfileLegacyFile;
    private ProfileStorage storage;
    private ProfileSwitcher switcher;

    @BeforeEach
    void setUp() throws IOException {
        this.instanceFile = this.gameDirectory.resolve("fancymenu_data").resolve(FILE_NAME);
        this.storage = new ProfileStorage(this.gameDirectory.resolve("config").resolve("fancymenu"));
        this.switcher = new ProfileSwitcher(this.storage);
        this.activeProfileLegacyFile = this.storage.getRootDirectory().resolve(FILE_NAME);
        this.storage.writeMetadata(this.storage.getRootDirectory(), new ProfileMetadata("original"));
    }

    @Test
    void firstLaunchAfterUpdateCopiesTheLegacyFileOfTheActiveProfileOnce() throws IOException {
        Files.writeString(this.activeProfileLegacyFile, LEGACY_CONTENT, StandardCharsets.UTF_8);

        this.launch();

        assertEquals(LEGACY_CONTENT, read(this.instanceFile));
        assertEquals(LEGACY_CONTENT, read(this.activeProfileLegacyFile));

        // Dragging changes the instance file, and the next launch must not bring the old offsets back
        Files.writeString(this.instanceFile, INSTANCE_CONTENT, StandardCharsets.UTF_8);
        this.launch();

        assertEquals(INSTANCE_CONTENT, read(this.instanceFile));
    }

    @Test
    void profilesLoadedAfterTheFirstLaunchNeverGetTheirLegacyFileImported() throws IOException {
        this.launch();
        assertTrue(Files.isRegularFile(this.instanceFile));
        String emptyInstanceContent = read(this.instanceFile);
        // A profile exported by an old FancyMenu version still has its own legacy file
        Path imported = this.storage.getStoredProfileDirectory("imported");
        this.storage.writeMetadata(imported, new ProfileMetadata("imported"));
        Files.writeString(imported.resolve(FILE_NAME), LEGACY_CONTENT, StandardCharsets.UTF_8);
        this.storage.writeMetadata(this.storage.getStoredProfileDirectory("fresh"), new ProfileMetadata("fresh"));

        assertTrue(this.switcher.switchTo("original", "imported", true));
        this.launch();

        assertEquals(LEGACY_CONTENT, read(this.activeProfileLegacyFile));
        assertEquals(emptyInstanceContent, read(this.instanceFile));

        assertTrue(this.switcher.switchTo("imported", "fresh", true));
        this.launch();

        assertEquals(emptyInstanceContent, read(this.instanceFile));
    }

    @Test
    void alreadyMigratedInstanceKeepsItsOwnFile() throws IOException {
        Files.createDirectories(this.instanceFile.getParent());
        Files.writeString(this.instanceFile, INSTANCE_CONTENT, StandardCharsets.UTF_8);
        Files.writeString(this.activeProfileLegacyFile, LEGACY_CONTENT, StandardCharsets.UTF_8);

        this.launch();

        assertEquals(INSTANCE_CONTENT, read(this.instanceFile));
        assertEquals(LEGACY_CONTENT, read(this.activeProfileLegacyFile));
    }

    /**
     * Does what FancyMenu does with the file at launch, which happens with the profile that is active at that time.
     */
    private void launch() {
        DraggerElementHandler.migrateLegacyFile(this.activeProfileLegacyFile, this.instanceFile);
    }

    @NotNull
    private static String read(@NotNull Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

}
