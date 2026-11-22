package de.keksuccino.fancymenu.customization.profile;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Long-running profile operations. They run one at a time on a background thread, report their progress and
 * clean up after themselves when they fail or get cancelled. Completion callbacks always run on the main thread.
 */
public final class ProfileTasks {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String TEMP_PREFIX = ProfileStorage.INTERNAL_PREFIX + "tmp_";
    private static final String DELETE_PREFIX = ProfileStorage.INTERNAL_PREFIX + "delete_";
    /**
     * A stored profile that an import replaces keeps this prefix until the imported profile took its place, followed by its ID and a random suffix.
     * If that never happened, like after a crash in between, {@link #cleanUpLeftovers(ProfileStorage)} restores it instead of deleting the only copy.
     */
    private static final String REPLACED_PREFIX = ProfileStorage.INTERNAL_PREFIX + "replaced_";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu-ProfileTasks");
        thread.setDaemon(true);
        return thread;
    });
    private static final SecureRandom RANDOM = new SecureRandom();

    @Nullable
    private static volatile ProfileTask currentTask;

    private ProfileTasks() {
    }

    @Nullable
    public static ProfileTask getCurrentTask() {
        return currentTask;
    }

    public static boolean isTaskRunning() {
        return currentTask != null;
    }

    /**
     * Creates a new profile without customizations. FancyMenu starts with its default setup when it gets loaded,
     * but keeps the personal UI preferences of the active profile.
     */
    @NotNull
    public static ProfileTask createEmptyProfile(@NotNull String title, @NotNull Consumer<ProfileTask> onDone) {
        ProfileHandler.prepareOperation();
        ProfileStorage storage = ProfileHandler.getStorage();
        String id = ProfileStorage.generateProfileId(title, getTakenIds());
        return submit(ProfileTask.Type.CREATE, progress -> {
            Path temp = storage.getStoreDirectory().resolve(TEMP_PREFIX + id);
            try {
                ProfileMetadata metadata = new ProfileMetadata(id);
                metadata.setTitle(title);
                metadata.setFancyMenuVersion(FancyMenu.VERSION);
                storage.writeMetadata(temp, metadata);
                ProfilePersonalOptions.copyPersonalCategories(storage.getRootDirectory().resolve(ProfilePersonalOptions.OPTIONS_FILE_NAME), temp.resolve(ProfilePersonalOptions.OPTIONS_FILE_NAME));
                ProfileFiles.renameDirectory(temp, storage.getStoredProfileDirectory(id));
            } catch (Exception ex) {
                ProfileFiles.deleteQuietly(temp);
                throw ex;
            }
            return id;
        }, onDone);
    }

    /**
     * Copies a profile into a new stored profile. Works for the active profile too, which copies the current setup.
     */
    @NotNull
    public static ProfileTask copyProfile(@NotNull Profile source, @NotNull String title, @NotNull Consumer<ProfileTask> onDone) {
        ProfileHandler.prepareOperation();
        ProfileStorage storage = ProfileHandler.getStorage();
        if (source.active()) VariableHandler.flush();
        String id = ProfileStorage.generateProfileId(title, getTakenIds());
        Profile current = ProfileHandler.findCurrentProfile(source);
        return submit(ProfileTask.Type.DUPLICATE, progress -> {
            Profile from = requireExisting(current, source);
            Predicate<Path> filter = getContentFilter(storage, from.directory());
            Path temp = storage.getStoreDirectory().resolve(TEMP_PREFIX + id);
            try {
                progress.setTotalBytes(ProfileFiles.measureSize(from.directory(), filter, progress));
                ProfileFiles.copyDirectoryContent(from.directory(), temp, filter, progress);
                ProfileMetadata metadata = from.metadata().copy();
                metadata.setId(id);
                metadata.setTitle(title);
                storage.writeMetadata(temp, metadata);
                ProfileFiles.renameDirectory(temp, storage.getStoredProfileDirectory(id));
            } catch (Exception | Error ex) {
                ProfileFiles.deleteQuietly(temp);
                throw ex;
            }
            return id;
        }, onDone);
    }

    /**
     * Deletes a stored profile. The active profile can't get deleted.
     */
    @NotNull
    public static ProfileTask deleteProfile(@NotNull Profile profile, @NotNull Consumer<ProfileTask> onDone) {
        if (profile.active()) throw new IllegalArgumentException("The active profile can't get deleted!");
        ProfileHandler.prepareOperation();
        ProfileStorage storage = ProfileHandler.getStorage();
        Profile current = ProfileHandler.findCurrentProfile(profile);
        return submit(ProfileTask.Type.DELETE, progress -> {
            Profile target = requireExisting(current, profile);
            if (target.active()) throw new IllegalArgumentException("The active profile can't get deleted!");
            // Moving it out of the way first makes it disappear at once, even if deleting all files takes a while
            Path trash = storage.getStoreDirectory().resolve(DELETE_PREFIX + target.getId() + "_" + randomSuffix());
            ProfileFiles.renameDirectory(target.directory(), trash);
            ProfileFiles.deleteRecursively(trash);
            return null;
        }, onDone);
    }

    /**
     * Exports a profile as archive.
     */
    @NotNull
    public static ProfileTask exportProfile(@NotNull Profile profile, @NotNull Path archiveFile, @NotNull Consumer<ProfileTask> onDone) throws IOException {
        ProfileHandler.prepareOperation();
        ProfileStorage storage = ProfileHandler.getStorage();
        Profile current = requireExisting(ProfileHandler.findCurrentProfile(profile), profile);
        if (current.active()) VariableHandler.flush();
        ProfileMetadata metadata = current.metadata().copy();
        metadata.setFancyMenuVersion(FancyMenu.VERSION);
        ProfileHandler.saveMetadata(current, metadata);
        Predicate<Path> filter = getContentFilter(storage, current.directory());
        return submit(ProfileTask.Type.EXPORT, progress -> {
            ProfileArchive.write(current.directory(), filter, archiveFile, progress);
            return current.getId();
        }, onDone);
    }

    /**
     * Imports an archive as new profile.
     *
     * @param replaceExisting if TRUE and a profile with the ID of the imported one exists, the existing profile gets replaced.
     *                        Replacing the active profile also loads the imported one. If FALSE, the imported profile gets a new ID if needed.
     * @param keepOpenWindow the window that stays open if the import replaces and loads the active profile.
     */
    @NotNull
    public static ProfileTask importArchive(@NotNull Path archiveFile, boolean replaceExisting, @Nullable PiPWindow keepOpenWindow, @NotNull Consumer<ProfileTask> onDone) {
        ProfileHandler.prepareOperation();
        ProfileStorage storage = ProfileHandler.getStorage();
        String activeId = ProfileHandler.getActiveProfile().getId();
        Set<String> takenIds = getTakenIds();
        ImportTarget importTarget = new ImportTarget();
        return submit(ProfileTask.Type.IMPORT, progress -> {
            ProfileMetadata metadata = ProfileArchive.readMetadata(archiveFile, "");
            Path temp = storage.getStoreDirectory().resolve(TEMP_PREFIX + "import_" + randomSuffix());
            try {
                ProfileArchive.extract(archiveFile, temp, progress);
                String id = metadata.getId();
                boolean conflict = takenIds.contains(id);
                if (!ProfileStorage.isValidProfileId(id) || (conflict && !replaceExisting)) {
                    id = ProfileStorage.generateProfileId(metadata.getTitle(), takenIds);
                    metadata.setId(id);
                    conflict = false;
                }
                storage.writeMetadata(temp, metadata);
                if (conflict && id.equals(activeId)) {
                    // The active profile can only get replaced by switching to the imported one without storing the active one
                    String pendingId = ProfileStorage.generateProfileId("import", takenIds);
                    ProfileFiles.renameDirectory(temp, storage.getStoredProfileDirectory(pendingId));
                    importTarget.pendingActivationId = pendingId;
                    return pendingId;
                }
                if (conflict) {
                    replaceStoredProfile(storage, id, temp);
                    return id;
                }
                ProfileFiles.renameDirectory(temp, storage.getStoredProfileDirectory(id));
                return id;
            } catch (Exception | Error ex) {
                ProfileFiles.deleteQuietly(temp);
                throw ex;
            }
        }, task -> {
            String pendingActivationId = importTarget.pendingActivationId;
            if ((task.getState() != ProfileTask.State.SUCCEEDED) || (pendingActivationId == null)) {
                onDone.accept(task);
                return;
            }
            ProfileHandler.startSwitch(pendingActivationId, false, keepOpenWindow, error -> {
                if (error != null) task.fail(error);
                onDone.accept(task);
            });
        });
    }

    /**
     * Turns an image into the icon of a profile.
     */
    @NotNull
    public static ProfileTask setIcon(@NotNull Profile profile, @NotNull Path image, @NotNull Consumer<ProfileTask> onDone) {
        ProfileHandler.prepareOperation();
        Profile current = ProfileHandler.findCurrentProfile(profile);
        return submit(ProfileTask.Type.SET_ICON, progress -> {
            Profile target = requireExisting(current, profile);
            ProfileIconProcessor.writeIcon(image, target.getIconFile());
            return target.getId();
        }, onDone);
    }

    public static void removeIcon(@NotNull Profile profile) throws IOException {
        ProfileHandler.prepareOperation();
        Files.deleteIfExists(requireExisting(ProfileHandler.findCurrentProfile(profile), profile).getIconFile());
    }

    /**
     * Measures a profile in the background, because walking big profiles takes a moment.
     *
     * @param callback gets called on the main thread, with NULL if the walk got stopped, because an operation needed to move or delete profile files.
     */
    public static void computeStats(@NotNull Profile profile, @NotNull Consumer<ProfileStats> callback) {
        ProfileStatsWalks.start(profile.directory(), getContentFilter(ProfileHandler.getStorage(), profile.directory()), callback);
    }

    /**
     * Deletes working directories that failed or crashed operations left behind, and restores stored profiles that an import
     * was about to replace when it got interrupted.
     */
    static void cleanUpLeftovers(@NotNull ProfileStorage storage) {
        Path store = storage.getStoreDirectory();
        if (!Files.isDirectory(store)) return;
        try (Stream<Path> entries = Files.list(store)) {
            entries.forEach(entry -> {
                String name = entry.getFileName().toString();
                if (name.startsWith(TEMP_PREFIX) || name.startsWith(DELETE_PREFIX)) {
                    LOGGER.info("[FANCYMENU] Cleaning up leftover profile data: " + entry);
                    ProfileFiles.deleteQuietly(entry);
                } else if (name.startsWith(REPLACED_PREFIX)) {
                    cleanUpReplacedProfile(storage, entry);
                }
            });
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to clean up leftover profile data!", ex);
        }
    }

    /**
     * Replaces a stored profile with an imported one. The existing profile stays as backup until the imported one took its place,
     * and gets restored if that fails.
     */
    static void replaceStoredProfile(@NotNull ProfileStorage storage, @NotNull String id, @NotNull Path imported) throws IOException {
        Path existing = storage.getStoredProfileDirectory(id);
        Path backup = storage.getStoreDirectory().resolve(REPLACED_PREFIX + id + "_" + randomSuffix());
        ProfileFiles.renameDirectory(existing, backup);
        try {
            ProfileFiles.renameDirectory(imported, existing);
        } catch (Exception | Error ex) {
            try {
                ProfileFiles.renameDirectory(backup, existing);
            } catch (Exception rollbackEx) {
                // The backup stays, so the next launch restores it
                ex.addSuppressed(rollbackEx);
            }
            throw ex;
        }
        // A backup that only got deleted in parts would come back at the next launch if the imported profile gets deleted meanwhile,
        // so it only gets deleted after it lost its backup prefix
        Path trash = storage.getStoreDirectory().resolve(DELETE_PREFIX + id + "_" + randomSuffix());
        try {
            ProfileFiles.renameDirectory(backup, trash);
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to delete the replaced profile, so the next launch deletes it: " + backup, ex);
            return;
        }
        ProfileFiles.deleteQuietly(trash);
    }

    /**
     * Deletes the backup of a replaced profile if the imported profile took its place, or restores it otherwise.
     */
    private static void cleanUpReplacedProfile(@NotNull ProfileStorage storage, @NotNull Path backup) {
        String name = backup.getFileName().toString();
        int suffixStart = name.lastIndexOf('_');
        String id = (suffixStart > REPLACED_PREFIX.length()) ? name.substring(REPLACED_PREFIX.length(), suffixStart) : "";
        if (!ProfileStorage.isValidProfileId(id)) {
            LOGGER.warn("[FANCYMENU] Found the backup of a replaced profile with an invalid name, so it stays where it is: " + backup);
            return;
        }
        Path stored = storage.getStoredProfileDirectory(id);
        if (Files.exists(stored, LinkOption.NOFOLLOW_LINKS)) {
            LOGGER.info("[FANCYMENU] Cleaning up the backup of a replaced profile: " + backup);
            ProfileFiles.deleteQuietly(backup);
            return;
        }
        try {
            ProfileFiles.renameDirectory(backup, stored);
            LOGGER.warn("[FANCYMENU] Restored profile '" + id + "', because importing a profile that should replace it got interrupted.");
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to restore the backup of a replaced profile: " + backup, ex);
        }
    }

    /**
     * Counts the layouts of a profile. Layouts are the text files in its customization directory.
     *
     * @param cancellation stops counting with a {@link CancellationException} once cancelling got requested.
     */
    static int countLayouts(@NotNull Path profileDirectory, @NotNull ProfileTaskProgress cancellation) {
        Path layoutDirectory = profileDirectory.resolve("customization");
        if (!Files.isDirectory(layoutDirectory)) return 0;
        // Closing the stream closes all directories the walk has open, also when the cancellation ends it
        try (Stream<Path> files = Files.walk(layoutDirectory)) {
            return (int) files.filter(path -> {
                cancellation.throwIfCancelled();
                return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt");
            }).filter(Files::isRegularFile).count();
        } catch (CancellationException ex) {
            throw ex;
        } catch (Exception ex) {
            return 0;
        }
    }

    @NotNull
    private static Predicate<Path> getContentFilter(@NotNull ProfileStorage storage, @NotNull Path profileDirectory) {
        return storage.getContentFilter(profileDirectory).and(path -> !ProfileStorage.isJunkFile(path));
    }

    /**
     * The IDs of all listed profiles and of all directories in the profile store, like the leftovers of a pending switch.
     */
    @NotNull
    private static Set<String> getTakenIds() {
        Set<String> ids = new HashSet<>(ProfileHandler.getStorage().getStoredProfileIds());
        for (Profile profile : ProfileHandler.getProfiles()) {
            ids.add(profile.getId());
        }
        return ids;
    }

    /**
     * Listed profiles can be outdated, so operations work on the current state of their profile, see {@link ProfileHandler#findCurrentProfile(Profile)}.
     */
    @NotNull
    private static Profile requireExisting(@Nullable Profile current, @NotNull Profile listed) throws NoSuchFileException {
        if (current == null) throw new NoSuchFileException(listed.directory().toString(), null, "The profile does not exist anymore");
        return current;
    }

    @NotNull
    private static String randomSuffix() {
        return String.format(Locale.ROOT, "%08x", RANDOM.nextInt());
    }

    @NotNull
    private static ProfileTask submit(@NotNull ProfileTask.Type type, @NotNull Work work, @NotNull Consumer<ProfileTask> onDone) {
        if (ProfileHandler.isBusy()) throw new IllegalStateException("Another profile operation is still in progress!");
        // Only a switch or the next launch can complete such a switch, and until then, the active profile only has a part of its files
        if (ProfileHandler.isActiveProfileIncomplete()) throw new IllegalStateException("The last profile switch is not complete yet!");
        ProfileTask task = new ProfileTask(type);
        currentTask = task;
        EXECUTOR.execute(() -> {
            try {
                task.succeed(work.run(task.getProgress()));
            } catch (CancellationException ex) {
                task.markCancelled();
            } catch (Throwable ex) {
                LOGGER.error("[FANCYMENU] Profile task failed: " + type, ex);
                task.fail(ex);
            }
            MainThreadTaskExecutor.executeInMainThread(() -> {
                if (currentTask == task) currentTask = null;
                onDone.accept(task);
            }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        });
        return task;
    }

    @FunctionalInterface
    private interface Work {

        /**
         * @return the ID of the profile the task created or changed, or NULL.
         */
        @Nullable
        String run(@NotNull ProfileTaskProgress progress) throws Exception;

    }

    /**
     * Gets filled by the import task, so its completion handler knows whether the imported profile still needs to get loaded.
     */
    private static final class ImportTarget {

        @Nullable
        private volatile String pendingActivationId;

    }

    /**
     * @param sizeBytes the size of all files of the profile.
     */
    public record ProfileStats(long sizeBytes, int layoutCount) {
    }

}
