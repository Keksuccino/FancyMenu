package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.LegacyHandler;
import de.keksuccino.fancymenu.customization.profile.ProfileArchive;
import de.keksuccino.fancymenu.customization.profile.ProfileFiles;
import de.keksuccino.fancymenu.customization.profile.ProfileMetadata;
import de.keksuccino.fancymenu.customization.profile.ProfileStorage;
import de.keksuccino.fancymenu.customization.profile.ProfileSwitcher;
import de.keksuccino.fancymenu.customization.profile.ProfileTaskProgress;
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
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers keeping the checkbox states in the active profile: moving the file of the game directory, where old FancyMenu versions kept it,
 * into the profile that is active at launch, and keeping the states of each profile apart when switching profiles.
 * Profiles get created and switched like in the game, through {@link ProfileSwitcher}.
 */
class CheckboxStatesHandlerTest {

    private static final String FILE_NAME = "checkbox_states.json";
    private static final String LEGACY_CONTENT = "{\"legacy_checkbox\": true}";
    private static final String PROFILE_CONTENT = "{\"profile_checkbox\": true}";
    private static final Gson GSON = new Gson();

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
    void clearStates() {
        CheckboxStatesHandler.readFromFile(this.gameDirectory.resolve("missing.json").toFile());
    }

    @Test
    void firstLaunchAfterUpdateMovesTheLegacyFileIntoTheActiveProfileOnce() throws IOException {
        this.launch();

        assertEquals(LEGACY_CONTENT, read(this.activeProfileFile));
        assertEquals(List.of(), this.legacyFilesOfGameDirectory());
        this.reloadActiveProfile();
        assertTrue(this.isChecked("legacy_checkbox"));

        this.check("new_checkbox", true);
        this.launch();

        assertEquals(Map.of("legacy_checkbox", true, "new_checkbox", true), statesOf(this.activeProfileFile));
        assertEquals(List.of(), this.legacyFilesOfGameDirectory());
    }

    @Test
    void activeProfileWithItsOwnStatesKeepsThemAndTheLegacyFileBecomesABackup() throws IOException {
        Files.writeString(this.activeProfileFile, PROFILE_CONTENT, StandardCharsets.UTF_8);

        this.launch();

        assertEquals(PROFILE_CONTENT, read(this.activeProfileFile));
        this.reloadActiveProfile();
        assertTrue(this.isChecked("profile_checkbox"));
        assertFalse(this.isChecked("legacy_checkbox"));
        Path backup = this.gameDirectory.resolve("checkbox_states_old.json");
        assertEquals(List.of(backup), this.legacyFilesOfGameDirectory());
        assertEquals(LEGACY_CONTENT, read(backup));

        this.launch();

        assertEquals(PROFILE_CONTENT, read(this.activeProfileFile));
        assertEquals(List.of(backup), this.legacyFilesOfGameDirectory());
        assertEquals(LEGACY_CONTENT, read(backup));
    }

    @Test
    void newEmptyProfileDoesNotImportTheLegacyStates() throws IOException {
        this.launch();
        this.reloadActiveProfile();
        this.createEmptyProfile("fresh");

        assertTrue(this.switcher.switchTo("original", "fresh", true));
        this.reloadActiveProfile();

        assertFalse(Files.exists(this.activeProfileFile));
        assertFalse(this.isChecked("legacy_checkbox"));
        // The next launch happens with the new profile active
        this.launch();
        this.reloadActiveProfile();
        assertFalse(Files.exists(this.activeProfileFile));
        assertFalse(this.isChecked("legacy_checkbox"));

        assertTrue(this.switcher.switchTo("fresh", "original", true));
        this.reloadActiveProfile();

        assertEquals(LEGACY_CONTENT, read(this.activeProfileFile));
        assertTrue(this.isChecked("legacy_checkbox"));
        assertFalse(Files.exists(this.storage.getStoredProfileDirectory("fresh").resolve(FILE_NAME)));
    }

    @Test
    void statesBelongToTheProfileTheyGotSetIn() throws IOException {
        this.launch();
        this.reloadActiveProfile();
        this.check("shared_checkbox", true);
        this.createEmptyProfile("fresh");

        assertTrue(this.switcher.switchTo("original", "fresh", true));
        this.reloadActiveProfile();

        assertFalse(this.isChecked("shared_checkbox"));
        assertFalse(this.isChecked("legacy_checkbox"));
        this.check("shared_checkbox", false);
        this.check("fresh_checkbox", true);

        assertTrue(this.switcher.switchTo("fresh", "original", true));
        this.reloadActiveProfile();

        assertTrue(this.isChecked("shared_checkbox"));
        assertTrue(this.isChecked("legacy_checkbox"));
        assertFalse(this.isChecked("fresh_checkbox"));

        assertTrue(this.switcher.switchTo("original", "fresh", true));
        this.reloadActiveProfile();

        assertFalse(this.isChecked("shared_checkbox"));
        assertTrue(this.isChecked("fresh_checkbox"));
    }

    @Test
    void statesOfTheOldProfileNeverGetWrittenIntoTheNewProfile() throws IOException {
        this.launch();
        this.reloadActiveProfile();
        this.check("original_checkbox", true);
        this.createEmptyProfile("fresh");

        assertTrue(this.switcher.switchTo("original", "fresh", true));
        this.reloadActiveProfile();
        this.check("fresh_checkbox", true);

        assertEquals(Map.of("fresh_checkbox", true), statesOf(this.activeProfileFile));
        Path storedOriginalFile = this.storage.getStoredProfileDirectory("original").resolve(FILE_NAME);
        assertEquals(Map.of("legacy_checkbox", true, "original_checkbox", true), statesOf(storedOriginalFile));

        assertTrue(this.switcher.switchTo("fresh", "original", true));
        this.reloadActiveProfile();
        this.check("original_checkbox", false);

        assertEquals(Map.of("legacy_checkbox", true, "original_checkbox", false), statesOf(this.activeProfileFile));
        assertEquals(Map.of("fresh_checkbox", true), statesOf(this.storage.getStoredProfileDirectory("fresh").resolve(FILE_NAME)));
    }

    @Test
    void brokenFileCountsAsNoCheckedCheckboxes() throws IOException {
        Files.writeString(this.activeProfileFile, "{\"broken_checkbox\": tr", StandardCharsets.UTF_8);

        assertDoesNotThrow(this::reloadActiveProfile);

        assertFalse(this.isChecked("broken_checkbox"));
    }

    @Test
    void exportedAndCopiedProfilesKeepTheirStates() throws IOException {
        this.launch();
        Path root = this.storage.getRootDirectory();
        Path archive = this.gameDirectory.resolve("export").resolve("original.fmp");
        Path extracted = this.gameDirectory.resolve("extracted");
        Path copy = this.storage.getStoredProfileDirectory("copy");

        ProfileArchive.write(root, this.storage.getContentFilter(root), archive, new ProfileTaskProgress());
        ProfileArchive.extract(archive, extracted, new ProfileTaskProgress());
        ProfileFiles.copyDirectoryContent(root, copy, this.storage.getContentFilter(root), new ProfileTaskProgress());

        assertEquals(LEGACY_CONTENT, read(extracted.resolve(FILE_NAME)));
        assertEquals(LEGACY_CONTENT, read(copy.resolve(FILE_NAME)));
    }

    /**
     * Does what FancyMenu does with the legacy file at launch, which happens with the profile that is active at that time.
     */
    private void launch() {
        LegacyHandler.moveLegacyFileIntoActiveProfile(this.legacyFile, this.activeProfileFile);
    }

    /**
     * Reads the file of the active profile like {@link CheckboxStatesHandler#reload()} after a profile switch.
     */
    private void reloadActiveProfile() {
        CheckboxStatesHandler.readFromFile(this.activeProfileFile.toFile());
    }

    private boolean isChecked(@NotNull String elementIdentifier) {
        return CheckboxStatesHandler.getState(this.activeProfileFile.toFile(), elementIdentifier);
    }

    /**
     * Sets a state like clicking a checkbox of the active profile.
     */
    private void check(@NotNull String elementIdentifier, boolean state) {
        CheckboxStatesHandler.setState(this.activeProfileFile.toFile(), elementIdentifier, state);
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
            return files.filter(path -> path.getFileName().toString().startsWith("checkbox_states")).sorted().toList();
        }
    }

    @NotNull
    private static Map<String, Boolean> statesOf(@NotNull Path file) throws IOException {
        return GSON.fromJson(read(file), new TypeToken<Map<String, Boolean>>(){}.getType());
    }

    @NotNull
    private static String read(@NotNull Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

}
