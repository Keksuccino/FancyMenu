package de.keksuccino.fancymenu.customization.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProfileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void generatedIdsAreValidReadableAndUnique() {
        Set<String> taken = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String id = ProfileStorage.generateProfileId("My Cööl Setup!!", taken);
            assertTrue(ProfileStorage.isValidProfileId(id), id);
            assertTrue(id.startsWith("my_c_l_setup_"), id);
            assertTrue(taken.add(id), "Duplicate ID: " + id);
        }
        assertTrue(ProfileStorage.generateProfileId("???", Set.of()).startsWith("profile_"));
        assertTrue(ProfileStorage.isValidProfileId(ProfileStorage.generateProfileId("x".repeat(500), Set.of())));
    }

    @Test
    void idsThatCouldEscapeTheStoreAreInvalid() {
        assertFalse(ProfileStorage.isValidProfileId(".."));
        assertFalse(ProfileStorage.isValidProfileId("../evil"));
        assertFalse(ProfileStorage.isValidProfileId(".stash_profile"));
        assertFalse(ProfileStorage.isValidProfileId("Upper"));
        assertFalse(ProfileStorage.isValidProfileId(""));
        assertFalse(ProfileStorage.isValidProfileId(null));
        assertThrows(IllegalArgumentException.class, () -> new ProfileStorage(this.tempDir).getStoredProfileDirectory("../evil"));
    }

    @Test
    void rootContentNeverIncludesTheProfileStore() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        Files.createDirectories(storage.getStoreDirectory().resolve("other_profile"));
        Files.createDirectories(this.tempDir.resolve("customization"));
        Files.writeString(this.tempDir.resolve("options.txt"), "x");

        List<Path> entries = storage.listContentEntries(this.tempDir);

        assertEquals(2, entries.size());
        assertFalse(entries.contains(storage.getStoreDirectory()));
        assertFalse(storage.getContentFilter(this.tempDir).test(storage.getStoreDirectory().resolve("other_profile").resolve("options.txt")));
        assertTrue(storage.getContentFilter(storage.getStoreDirectory().resolve("other_profile")).test(storage.getStoreDirectory().resolve("other_profile").resolve("options.txt")));
    }

    @Test
    void storedProfilesSkipInternalAndForeignDirectories() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        Files.createDirectories(storage.getStoreDirectory().resolve("valid_profile"));
        Files.createDirectories(storage.getStoreDirectory().resolve(".tmp_working"));
        Files.createDirectories(storage.getStoreDirectory().resolve("My Old Setup"));
        Files.writeString(storage.getStoreDirectory().resolve("stray_file"), "x");

        assertEquals(Set.of("valid_profile"), storage.getStoredProfileIds());
        assertEquals(List.of(storage.getStoreDirectory().resolve("My Old Setup")), storage.listForeignStoreDirectories());
    }

    @Test
    void metadataGetsWrittenWithoutLeftovers() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        ProfileMetadata metadata = new ProfileMetadata("abc");
        metadata.setTitle("ABC");

        storage.writeMetadata(this.tempDir, metadata);
        metadata.setTitle("Changed");
        storage.writeMetadata(this.tempDir, metadata);

        assertEquals("Changed", storage.readMetadata(this.tempDir, "fallback").getTitle());
        assertFalse(Files.exists(this.tempDir.resolve(ProfileStorage.METADATA_FILE_NAME + ".tmp")));
    }

    @Test
    void missingMetadataReturnsNullAndBrokenMetadataFallsBack() throws Exception {
        ProfileStorage storage = new ProfileStorage(this.tempDir);
        assertNull(storage.readMetadata(this.tempDir, "fallback"));

        Files.writeString(storage.getMetadataFile(this.tempDir), "{broken");
        ProfileMetadata metadata = storage.readMetadata(this.tempDir, "fallback");

        assertNotNull(metadata);
        assertEquals("fallback", metadata.getId());
    }

}
