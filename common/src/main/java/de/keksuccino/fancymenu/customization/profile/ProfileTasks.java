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
    /** Files that operating systems create on their own. They never belong to a profile. */
    private static final Set<String> JUNK_FILE_NAMES = Set.of(".ds_store", "thumbs.db", "desktop.ini");
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
     * Copies a profile into a new stored profile. Copying the active profile creates a snapshot of the current setup.
     */
    @NotNull
    public static ProfileTask copyProfile(@NotNull Profile source, @NotNull String title, @NotNull Consumer<ProfileTask> onDone) {
        ProfileStorage storage = ProfileHandler.getStorage();
        if (source.active()) VariableHandler.flush();
        String id = ProfileStorage.generateProfileId(title, getTakenIds());
        Predicate<Path> filter = getContentFilter(storage, source.directory());
        return submit(source.active() ? ProfileTask.Type.SNAPSHOT : ProfileTask.Type.DUPLICATE, progress -> {
            Path temp = storage.getStoreDirectory().resolve(TEMP_PREFIX + id);
            try {
                progress.setTotalBytes(ProfileFiles.measureSize(source.directory(), filter));
                ProfileFiles.copyDirectoryContent(source.directory(), temp, filter, progress);
                ProfileMetadata metadata = source.metadata().copy();
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
        ProfileStorage storage = ProfileHandler.getStorage();
        return submit(ProfileTask.Type.DELETE, progress -> {
            // Moving it out of the way first makes it disappear at once, even if deleting all files takes a while
            Path trash = storage.getStoreDirectory().resolve(DELETE_PREFIX + profile.getId() + "_" + randomSuffix());
            ProfileFiles.renameDirectory(profile.directory(), trash);
            ProfileFiles.deleteRecursively(trash);
            return null;
        }, onDone);
    }

    /**
     * Exports a profile as archive.
     */
    @NotNull
    public static ProfileTask exportProfile(@NotNull Profile profile, @NotNull Path archiveFile, @NotNull Consumer<ProfileTask> onDone) throws IOException {
        ProfileStorage storage = ProfileHandler.getStorage();
        if (profile.active()) VariableHandler.flush();
        ProfileMetadata metadata = profile.metadata().copy();
        metadata.setFancyMenuVersion(FancyMenu.VERSION);
        ProfileHandler.saveMetadata(profile, metadata);
        Predicate<Path> filter = getContentFilter(storage, profile.directory());
        return submit(ProfileTask.Type.EXPORT, progress -> {
            ProfileArchive.write(profile.directory(), filter, archiveFile, progress);
            return profile.getId();
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
                    Path existing = storage.getStoredProfileDirectory(id);
                    Path trash = storage.getStoreDirectory().resolve(DELETE_PREFIX + id + "_" + randomSuffix());
                    ProfileFiles.renameDirectory(existing, trash);
                    ProfileFiles.renameDirectory(temp, existing);
                    ProfileFiles.deleteQuietly(trash);
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
        return submit(ProfileTask.Type.SET_ICON, progress -> {
            ProfileIconProcessor.writeIcon(image, profile.getIconFile());
            return profile.getId();
        }, onDone);
    }

    public static void removeIcon(@NotNull Profile profile) throws IOException {
        Files.deleteIfExists(profile.getIconFile());
    }

    /**
     * Measures a profile in the background, because walking big profiles takes a moment.
     */
    public static void computeStats(@NotNull Profile profile, @NotNull Consumer<ProfileStats> callback) {
        ProfileStorage storage = ProfileHandler.getStorage();
        Predicate<Path> filter = getContentFilter(storage, profile.directory());
        Thread thread = new Thread(() -> {
            long size = 0L;
            try {
                size = ProfileFiles.measureSize(profile.directory(), filter);
            } catch (Exception ex) {
                LOGGER.warn("[FANCYMENU] Failed to measure profile: " + profile.directory(), ex);
            }
            ProfileStats stats = new ProfileStats(size, countLayouts(profile.directory()));
            MainThreadTaskExecutor.executeInMainThread(() -> callback.accept(stats), MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }, "FancyMenu-ProfileStats");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Deletes working directories that failed or crashed operations left behind.
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
                }
            });
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to clean up leftover profile data!", ex);
        }
    }

    /**
     * Counts the layouts of a profile. Layouts are the text files in its customization directory.
     */
    static int countLayouts(@NotNull Path profileDirectory) {
        Path layoutDirectory = profileDirectory.resolve("customization");
        if (!Files.isDirectory(layoutDirectory)) return 0;
        try (Stream<Path> files = Files.walk(layoutDirectory)) {
            return (int) files.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt")).filter(Files::isRegularFile).count();
        } catch (Exception ex) {
            return 0;
        }
    }

    @NotNull
    private static Predicate<Path> getContentFilter(@NotNull ProfileStorage storage, @NotNull Path profileDirectory) {
        return storage.getContentFilter(profileDirectory).and(path -> !JUNK_FILE_NAMES.contains(path.getFileName().toString().toLowerCase(Locale.ROOT)));
    }

    @NotNull
    private static Set<String> getTakenIds() {
        Set<String> ids = new HashSet<>(ProfileHandler.getStorage().getStoredProfileIds());
        ids.add(ProfileHandler.getActiveProfile().getId());
        return ids;
    }

    @NotNull
    private static String randomSuffix() {
        return String.format(Locale.ROOT, "%08x", RANDOM.nextInt());
    }

    @NotNull
    private static ProfileTask submit(@NotNull ProfileTask.Type type, @NotNull Work work, @NotNull Consumer<ProfileTask> onDone) {
        if (ProfileHandler.isBusy()) throw new IllegalStateException("Another profile operation is still in progress!");
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
