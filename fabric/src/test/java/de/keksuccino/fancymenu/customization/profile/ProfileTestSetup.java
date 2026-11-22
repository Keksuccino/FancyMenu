package de.keksuccino.fancymenu.customization.profile;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Builds profile directories and switch states on disk for the profile tests.
 */
final class ProfileTestSetup {

    private ProfileTestSetup() {
    }

    /**
     * Creates a profile with a layout, an options file and metadata titled "Title of [id]".
     */
    static void createProfile(@NotNull ProfileStorage storage, @NotNull Path directory, @NotNull String id, @NotNull String options) throws IOException {
        Files.createDirectories(directory.resolve("customization"));
        Files.writeString(directory.resolve("customization").resolve("layout.txt"), id + " layout");
        Files.writeString(directory.resolve("options.txt"), options);
        ProfileMetadata metadata = new ProfileMetadata(id);
        metadata.setTitle("Title of " + id);
        storage.writeMetadata(directory, metadata);
    }

    /**
     * Writes a journal like FancyMenu 3.10 did, which knew only these keys.
     */
    static void writeJournal(@NotNull ProfileStorage storage, @NotNull String from, @NotNull String to, @NotNull String phase, boolean keepActive) throws IOException {
        Files.createDirectories(storage.getStoreDirectory());
        Files.writeString(journal(storage), "{\"from\":\"" + from + "\",\"to\":\"" + to + "\",\"keep_active\":" + keepActive + ",\"phase\":\"" + phase + "\"}", StandardCharsets.UTF_8);
    }

    @NotNull
    static Path journal(@NotNull ProfileStorage storage) {
        return storage.getStoreDirectory().resolve(ProfileSwitchJournal.FILE_NAME);
    }

    @NotNull
    static String read(@NotNull Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @NotNull
    static String title(@NotNull ProfileStorage storage, @NotNull Path directory) {
        ProfileMetadata metadata = storage.readMetadata(directory, "missing");
        return (metadata != null) ? metadata.getTitle() : "no metadata";
    }

    /**
     * Whether a directory holds the complete profile that {@link #createProfile(ProfileStorage, Path, String, String)} created.
     */
    static boolean isCompleteProfile(@NotNull ProfileStorage storage, @NotNull Path directory, @NotNull String id, @NotNull String options) throws IOException {
        Path layout = directory.resolve("customization").resolve("layout.txt");
        Path optionsFile = directory.resolve("options.txt");
        return Files.isRegularFile(layout) && read(layout).equals(id + " layout")
                && Files.isRegularFile(optionsFile) && read(optionsFile).equals(options)
                && title(storage, directory).equals("Title of " + id);
    }

}
