package de.keksuccino.fancymenu.customization.profile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Knows where profiles live on disk.<br>
 * The active profile is the plain content of the FancyMenu config directory (the root directory), while all inactive profiles
 * live in sub-directories of {@link #STORE_DIRECTORY_NAME} inside the root directory. The name of such a sub-directory is the profile's ID.
 */
public class ProfileStorage {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String STORE_DIRECTORY_NAME = "profiles";
    public static final String METADATA_FILE_NAME = "profile.json";
    public static final String ICON_FILE_NAME = "profile_icon.png";
    /** Directories in the store that start with this prefix are internal working directories and never profiles. */
    public static final String INTERNAL_PREFIX = ".";
    public static final int MAX_ID_LENGTH = 48;

    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]{0," + (MAX_ID_LENGTH - 1) + "}");
    private static final SecureRandom RANDOM = new SecureRandom();

    @NotNull
    private final Path rootDirectory;
    @NotNull
    private final Path storeDirectory;

    public ProfileStorage(@NotNull Path rootDirectory) {
        this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
        this.storeDirectory = this.rootDirectory.resolve(STORE_DIRECTORY_NAME);
    }

    /**
     * The FancyMenu config directory, which holds the content of the active profile.
     */
    @NotNull
    public Path getRootDirectory() {
        return this.rootDirectory;
    }

    @NotNull
    public Path getStoreDirectory() {
        return this.storeDirectory;
    }

    @NotNull
    public Path getStoredProfileDirectory(@NotNull String profileId) {
        if (!isValidProfileId(profileId)) throw new IllegalArgumentException("Invalid profile ID: " + profileId);
        return this.storeDirectory.resolve(profileId);
    }

    @NotNull
    public Path getMetadataFile(@NotNull Path profileDirectory) {
        return profileDirectory.resolve(METADATA_FILE_NAME);
    }

    @NotNull
    public Path getIconFile(@NotNull Path profileDirectory) {
        return profileDirectory.resolve(ICON_FILE_NAME);
    }

    /**
     * Filters out everything in a profile directory that is no profile content. For the root directory, this is the profile store itself.
     */
    @NotNull
    public Predicate<Path> getContentFilter(@NotNull Path profileDirectory) {
        Path normalizedDirectory = profileDirectory.toAbsolutePath().normalize();
        if (!normalizedDirectory.equals(this.rootDirectory)) return path -> true;
        return path -> !path.toAbsolutePath().normalize().startsWith(this.storeDirectory);
    }

    /**
     * Lists the top-level entries of a profile directory that belong to the profile.
     */
    @NotNull
    public List<Path> listContentEntries(@NotNull Path profileDirectory) throws IOException {
        if (!Files.isDirectory(profileDirectory)) return new ArrayList<>();
        Predicate<Path> filter = this.getContentFilter(profileDirectory);
        try (Stream<Path> entries = Files.list(profileDirectory)) {
            return new ArrayList<>(entries.filter(filter).toList());
        }
    }

    /**
     * Lists the directories of all inactive profiles. Directories with invalid names get skipped.
     */
    @NotNull
    public List<Path> listStoredProfileDirectories() {
        if (!Files.isDirectory(this.storeDirectory)) return new ArrayList<>();
        try (Stream<Path> entries = Files.list(this.storeDirectory)) {
            return new ArrayList<>(entries
                    .filter(Files::isDirectory)
                    .filter(path -> isValidProfileId(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList());
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to list stored profiles!", ex);
            return new ArrayList<>();
        }
    }

    /**
     * Lists all stored directories that are no valid profile directories, but were not created by FancyMenu itself either,
     * like a config directory copy a user put into the store by hand.
     */
    @NotNull
    public List<Path> listForeignStoreDirectories() {
        if (!Files.isDirectory(this.storeDirectory)) return new ArrayList<>();
        try (Stream<Path> entries = Files.list(this.storeDirectory)) {
            return new ArrayList<>(entries
                    .filter(Files::isDirectory)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return !name.startsWith(INTERNAL_PREFIX) && !isValidProfileId(name);
                    })
                    .toList());
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to list foreign directories of the profile store!", ex);
            return new ArrayList<>();
        }
    }

    /**
     * @return the IDs of all stored profiles.
     */
    @NotNull
    public Set<String> getStoredProfileIds() {
        Set<String> ids = new HashSet<>();
        for (Path directory : this.listStoredProfileDirectories()) {
            ids.add(directory.getFileName().toString());
        }
        return ids;
    }

    /**
     * Reads the metadata of a profile directory.
     *
     * @param fallbackId the ID to use when the metadata has no valid ID.
     * @return the metadata, or NULL if the directory has no metadata file. Broken metadata files return metadata with only the fallback ID.
     */
    @Nullable
    public ProfileMetadata readMetadata(@NotNull Path profileDirectory, @NotNull String fallbackId) {
        Path metadataFile = this.getMetadataFile(profileDirectory);
        if (!Files.isRegularFile(metadataFile)) return null;
        try {
            return ProfileMetadata.fromJson(Files.readString(metadataFile, StandardCharsets.UTF_8), fallbackId);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read profile metadata: " + metadataFile, ex);
            return new ProfileMetadata(fallbackId);
        }
    }

    /**
     * Writes metadata atomically, so a crash never leaves a half-written metadata file behind.
     */
    public void writeMetadata(@NotNull Path profileDirectory, @NotNull ProfileMetadata metadata) throws IOException {
        Files.createDirectories(profileDirectory);
        Path metadataFile = this.getMetadataFile(profileDirectory);
        Path tempFile = profileDirectory.resolve(METADATA_FILE_NAME + ".tmp");
        Files.writeString(tempFile, metadata.toJson(), StandardCharsets.UTF_8);
        moveReplacing(tempFile, metadataFile);
    }

    /**
     * Moves a file and replaces the target. Uses an atomic move where the file system supports it.
     */
    public static void moveReplacing(@NotNull Path source, @NotNull Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static boolean isValidProfileId(@Nullable String id) {
        return (id != null) && PROFILE_ID_PATTERN.matcher(id).matches();
    }

    /**
     * Generates a readable, unique profile ID out of a title, like "my_cool_setup_3f9a1c".
     */
    @NotNull
    public static String generateProfileId(@Nullable String title, @NotNull Set<String> takenIds) {
        String slug = toSlug(title);
        while (true) {
            String suffix = String.format(Locale.ROOT, "%06x", RANDOM.nextInt(0x1000000));
            String id = slug.isEmpty() ? ("profile_" + suffix) : (slug + "_" + suffix);
            if (!takenIds.contains(id)) return id;
        }
    }

    /**
     * Turns text into a lowercase, file-name-safe slug.
     */
    @NotNull
    public static String toSlug(@Nullable String text) {
        if (text == null) return "";
        StringBuilder builder = new StringBuilder();
        boolean lastWasSeparator = true;
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            if (((c >= 'a') && (c <= 'z')) || ((c >= '0') && (c <= '9'))) {
                builder.append(c);
                lastWasSeparator = false;
            } else if (!lastWasSeparator) {
                builder.append('_');
                lastWasSeparator = true;
            }
            if (builder.length() >= (MAX_ID_LENGTH - 8)) break;
        }
        while ((builder.length() > 0) && (builder.charAt(builder.length() - 1) == '_')) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

}
