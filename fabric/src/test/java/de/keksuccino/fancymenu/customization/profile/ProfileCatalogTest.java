package de.keksuccino.fancymenu.customization.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static de.keksuccino.fancymenu.customization.profile.ProfileTestSetup.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfileCatalogTest {

    @TempDir
    Path tempDir;
    private ProfileStorage storage;
    private ProfileSwitcher switcher;
    private ProfileCatalog catalog;

    @BeforeEach
    void setUp() {
        this.storage = new ProfileStorage(this.tempDir);
        this.switcher = new ProfileSwitcher(this.storage);
        this.catalog = new ProfileCatalog(this.storage, this.switcher);
    }

    @Test
    void listingWhileTheCleanupOfASwitchIsPendingWritesNothing() throws Exception {
        Path leftover = this.createPendingCleanup();
        Path gamma = Files.createDirectories(this.storage.getStoredProfileDirectory("gamma"));
        Path foreign = Files.createDirectories(this.storage.getStoreDirectory().resolve("My Setup"));
        String rootMetadata = read(this.storage.getMetadataFile(this.tempDir));

        List<Profile> profiles = this.catalog.listProfiles(false, "My Profile");

        assertEquals(rootMetadata, read(this.storage.getMetadataFile(this.tempDir)));
        assertEquals(List.of(), ProfileFiles.listEntries(leftover));
        assertFalse(Files.exists(this.storage.getMetadataFile(gamma)));
        assertTrue(Files.isDirectory(foreign));
        assertEquals("beta", profiles.get(0).getId());
        assertTrue(profiles.get(0).active());
        // The previously active profile still shows up, and the leftover directory of the active profile doesn't
        assertEquals(List.of("gamma", "alpha"), profiles.subList(1, profiles.size()).stream().map(Profile::getId).toList());
        Profile alpha = profiles.get(2);
        assertEquals("Title of alpha", alpha.metadata().getTitle());
        assertEquals(this.switcher.getStashDirectory("alpha"), alpha.directory());
    }

    @Test
    void activeProfileKeepsItsIdWhileTheCleanupOfASwitchIsPending() throws Exception {
        this.createPendingCleanup();

        Profile active = this.catalog.readActiveProfile(false, "My Profile");

        assertEquals("beta", active.getId());
        assertEquals("beta", this.storage.readMetadata(this.tempDir, "x").getId());
    }

    @Test
    void listingAnInterruptedStashShowsTheTargetAndNoStash() throws Exception {
        createProfile(this.storage, this.tempDir, "alpha", "alpha options");
        createProfile(this.storage, this.storage.getStoredProfileDirectory("beta"), "beta", "beta options");
        Path stash = this.switcher.getStashDirectory("alpha");
        Files.createDirectories(stash);
        Files.move(this.tempDir.resolve("options.txt"), stash.resolve("options.txt"));
        writeJournal(this.storage, "alpha", "beta", "STASH", true);

        List<Profile> profiles = this.catalog.listProfiles(false, "My Profile");

        assertEquals(List.of("alpha", "beta"), profiles.stream().map(Profile::getId).toList());
        assertEquals(this.storage.getStoredProfileDirectory("beta"), profiles.get(1).directory());
    }

    @Test
    void writableListingRepairsBrokenProfiles() throws Exception {
        Files.createDirectories(this.tempDir);
        Path gamma = Files.createDirectories(this.storage.getStoredProfileDirectory("gamma"));
        Files.createDirectories(this.storage.getStoreDirectory().resolve("My Setup"));

        List<Profile> profiles = this.catalog.listProfiles(true, "My Profile");

        assertEquals(3, profiles.size());
        assertEquals("My Profile", title(this.storage, this.tempDir));
        assertEquals(profiles.get(0).getId(), this.storage.readMetadata(this.tempDir, "x").getId());
        assertEquals("gamma", title(this.storage, gamma));
        assertTrue(this.storage.listForeignStoreDirectories().isEmpty());
        assertTrue(profiles.stream().anyMatch(profile -> profile.metadata().getTitle().equals("My Setup")));
    }

    @Test
    void listingAfterCompletingThePendingCleanupFindsProfilesInTheirFinalPlace() throws Exception {
        this.createPendingCleanup();
        ProfileSwitchController controller = new ProfileSwitchController(this.switcher, () -> {}, () -> false, () -> "beta");

        // ProfileHandler#getProfiles() lists like this
        List<Profile> profiles = this.catalog.listProfiles(controller.tryCompletePendingSwitch(), "My Profile");

        assertFalse(this.switcher.isSwitchPending());
        assertEquals(List.of("beta", "alpha"), profiles.stream().map(Profile::getId).toList());
        assertEquals(this.storage.getStoredProfileDirectory("alpha"), profiles.get(1).directory());
        assertTrue(isCompleteProfile(this.storage, profiles.get(1).directory(), "alpha", "alpha options"));
    }

    /**
     * The state after the content of alpha and beta got swapped, but cleaning up the profile store failed.
     *
     * @return the emptied directory of beta.
     */
    private Path createPendingCleanup() throws Exception {
        createProfile(this.storage, this.tempDir, "beta", "beta options");
        createProfile(this.storage, this.switcher.getStashDirectory("alpha"), "alpha", "alpha options");
        Path leftover = Files.createDirectories(this.storage.getStoredProfileDirectory("beta"));
        writeJournal(this.storage, "alpha", "beta", "FINISH", true);
        return leftover;
    }

}
