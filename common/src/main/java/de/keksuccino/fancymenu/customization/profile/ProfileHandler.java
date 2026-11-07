package de.keksuccino.fancymenu.customization.profile;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Entry point for everything profiles. Lists profiles, edits their metadata and switches the active profile.
 * Long-running operations like exports live in {@link ProfileTasks}.
 */
public final class ProfileHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    /** Switches fail while another program has a profile file open, so they get retried a few times before giving up. */
    private static final int MAX_SWITCH_ATTEMPTS = 4;
    private static final int SWITCH_RETRY_DELAY_TICKS = 10;

    private static ProfileStorage storage;
    private static ProfileSwitcher switcher;
    @Nullable
    private static PendingSwitch pendingSwitch;
    private static long clientTicks = 0L;
    private static boolean initialized = false;

    private ProfileHandler() {
    }

    /**
     * Recovers interrupted profile switches and cleans up leftovers of failed operations.
     * Has to get called before anything reads the FancyMenu config directory.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        storage = new ProfileStorage(FancyMenu.MOD_DIR.toPath());
        switcher = new ProfileSwitcher(storage);
        switcher.recoverInterruptedSwitch();
        ProfileTasks.cleanUpLeftovers(storage);
        EventHandler.INSTANCE.registerListenersOf(ProfileHandler.class);
    }

    @NotNull
    public static ProfileStorage getStorage() {
        return Objects.requireNonNull(storage, "ProfileHandler not initialized yet!");
    }

    /**
     * Lists all profiles. The active profile comes first, all others are sorted by their title.<br>
     * Fixes broken stored profiles on the way, like directories without metadata or metadata with a wrong ID.
     */
    @NotNull
    public static List<Profile> getProfiles() {
        List<Profile> profiles = new ArrayList<>();
        profiles.add(getActiveProfile());
        adoptForeignDirectories();
        List<Profile> stored = new ArrayList<>();
        for (Path directory : getStorage().listStoredProfileDirectories()) {
            String id = directory.getFileName().toString();
            ProfileMetadata metadata = getStorage().readMetadata(directory, id);
            boolean changed = false;
            if (metadata == null) {
                metadata = new ProfileMetadata(id);
                metadata.setTitle(id);
                changed = true;
            } else if (!metadata.getId().equals(id)) {
                metadata.setId(id);
                changed = true;
            }
            if (changed) writeMetadataQuietly(directory, metadata);
            stored.add(new Profile(metadata, directory, false));
        }
        stored.sort(Comparator.comparing((Profile profile) -> profile.metadata().getTitle(), String.CASE_INSENSITIVE_ORDER).thenComparing(Profile::getId));
        profiles.addAll(stored);
        return profiles;
    }

    /**
     * The profile that is currently loaded. Creates its metadata if it has none yet, like the setup of a user that never used profiles before.
     */
    @NotNull
    public static Profile getActiveProfile() {
        Path root = getStorage().getRootDirectory();
        ProfileMetadata metadata = getStorage().readMetadata(root, "");
        boolean changed = false;
        if (metadata == null) {
            metadata = new ProfileMetadata("");
            metadata.setTitle(I18n.get("fancymenu.profiles.default_profile_title"));
            changed = true;
        }
        Set<String> storedIds = getStorage().getStoredProfileIds();
        if (!ProfileStorage.isValidProfileId(metadata.getId()) || storedIds.contains(metadata.getId())) {
            metadata.setId(ProfileStorage.generateProfileId(metadata.getTitle(), storedIds));
            changed = true;
        }
        if (changed) writeMetadataQuietly(root, metadata);
        return new Profile(metadata, root, true);
    }

    @Nullable
    public static Profile getProfile(@NotNull String profileId) {
        for (Profile profile : getProfiles()) {
            if (profile.getId().equals(profileId)) return profile;
        }
        return null;
    }

    /**
     * Saves new metadata for a profile. The ID of a profile never changes.
     */
    public static void saveMetadata(@NotNull Profile profile, @NotNull ProfileMetadata metadata) throws IOException {
        ProfileMetadata toSave = metadata.copy();
        toSave.setId(profile.getId());
        getStorage().writeMetadata(profile.directory(), toSave);
    }

    /**
     * Whether a profile switch or a profile task is in progress, which blocks all other profile operations.
     */
    public static boolean isBusy() {
        return (pendingSwitch != null) || ProfileTasks.isTaskRunning();
    }

    public static boolean isSwitching() {
        return pendingSwitch != null;
    }

    /**
     * Makes a stored profile the active one and reloads FancyMenu with it. The previously active profile gets stored with all its changes.
     *
     * @param keepOpenWindow a window that stays open while all other FancyMenu windows get closed, like the profile manager.
     * @param callback gets called on the main thread with NULL on success or the error that made the switch fail.
     */
    public static void switchTo(@NotNull Profile target, @Nullable PiPWindow keepOpenWindow, @NotNull Consumer<Throwable> callback) {
        if (target.active()) throw new IllegalArgumentException("The profile is already active!");
        startSwitch(target.getId(), true, keepOpenWindow, callback);
    }

    /**
     * @param keepActiveProfile if FALSE, the currently active profile gets deleted instead of stored.
     */
    static void startSwitch(@NotNull String targetProfileId, boolean keepActiveProfile, @Nullable PiPWindow keepOpenWindow, @NotNull Consumer<Throwable> callback) {
        if (isBusy()) {
            callback.accept(new IllegalStateException("Another profile operation is still in progress!"));
            return;
        }
        String activeProfileId = getActiveProfile().getId();
        LOGGER.info("[FANCYMENU] Switching profile from '" + activeProfileId + "' to '" + targetProfileId + "'..");
        if (!ProfileRuntimeReloader.suspend(keepOpenWindow)) {
            ProfileRuntimeReloader.resume(false);
            callback.accept(new IOException("Failed to save pending variable changes!"));
            return;
        }
        pendingSwitch = new PendingSwitch(activeProfileId, targetProfileId, keepActiveProfile, callback);
        attemptPendingSwitch();
    }

    private static void attemptPendingSwitch() {
        PendingSwitch pending = Objects.requireNonNull(pendingSwitch);
        pending.attempts++;
        pending.retryAtTick = -1L;
        try {
            switcher.switchTo(pending.fromId, pending.toId, pending.keepActiveProfile);
        } catch (Exception ex) {
            if ((ex instanceof IOException) && (pending.attempts < MAX_SWITCH_ATTEMPTS)) {
                LOGGER.warn("[FANCYMENU] Profile switch failed (attempt " + pending.attempts + "), retrying soon..", ex);
                pending.retryAtTick = clientTicks + SWITCH_RETRY_DELAY_TICKS;
                return;
            }
            LOGGER.error("[FANCYMENU] Failed to switch profile!", ex);
            pendingSwitch = null;
            ProfileRuntimeReloader.resume(false);
            pending.callback.accept(ex);
            return;
        }
        LOGGER.info("[FANCYMENU] Switched profile to '" + pending.toId + "'!");
        pendingSwitch = null;
        ProfileRuntimeReloader.resume(true);
        pending.callback.accept(null);
    }

    @EventListener
    public static void onClientTickPost(ClientTickEvent.Post e) {
        clientTicks++;
        PendingSwitch pending = pendingSwitch;
        if ((pending != null) && (pending.retryAtTick >= 0L) && (clientTicks >= pending.retryAtTick)) {
            attemptPendingSwitch();
        }
    }

    /**
     * Turns directories that users put into the profile store by hand, like copies of a config directory, into proper profiles.
     */
    private static void adoptForeignDirectories() {
        for (Path foreign : getStorage().listForeignStoreDirectories()) {
            String title = foreign.getFileName().toString();
            String id = ProfileStorage.generateProfileId(title, getStorage().getStoredProfileIds());
            try {
                Path target = getStorage().getStoredProfileDirectory(id);
                ProfileFiles.renameDirectory(foreign, target);
                ProfileMetadata metadata = getStorage().readMetadata(target, id);
                if (metadata == null) {
                    metadata = new ProfileMetadata(id);
                    metadata.setTitle(title);
                }
                metadata.setId(id);
                getStorage().writeMetadata(target, metadata);
                LOGGER.info("[FANCYMENU] Added directory '" + title + "' of the profile store as profile '" + id + "'.");
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to add directory of the profile store as profile: " + foreign, ex);
            }
        }
    }

    private static void writeMetadataQuietly(@NotNull Path directory, @NotNull ProfileMetadata metadata) {
        try {
            getStorage().writeMetadata(directory, metadata);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to write profile metadata: " + directory, ex);
        }
    }

    private static final class PendingSwitch {

        @NotNull
        private final String fromId;
        @NotNull
        private final String toId;
        private final boolean keepActiveProfile;
        @NotNull
        private final Consumer<Throwable> callback;
        private int attempts = 0;
        private long retryAtTick = -1L;

        private PendingSwitch(@NotNull String fromId, @NotNull String toId, boolean keepActiveProfile, @NotNull Consumer<Throwable> callback) {
            this.fromId = fromId;
            this.toId = toId;
            this.keepActiveProfile = keepActiveProfile;
            this.callback = callback;
        }

    }

}
