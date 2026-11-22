package de.keksuccino.fancymenu.customization.profile;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Entry point for everything profiles. Lists profiles, edits their metadata and switches the active profile.
 * Long-running operations like exports live in {@link ProfileTasks}.<br>
 * A profile switch that is still pending, like after its last step failed, gets completed before every other profile operation and whenever
 * the profiles get listed, if that's possible without reloading FancyMenu.
 */
public final class ProfileHandler {

    private static ProfileStorage storage;
    private static ProfileSwitcher switcher;
    private static ProfileCatalog catalog;
    private static ProfileSwitchController switchController;
    private static boolean initialized = false;

    private ProfileHandler() {
    }

    /**
     * Completes or rolls back interrupted profile switches and cleans up leftovers of failed operations.
     * Has to get called before anything reads the FancyMenu config directory.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        storage = new ProfileStorage(FancyMenu.MOD_DIR.toPath());
        switcher = new ProfileSwitcher(storage);
        catalog = new ProfileCatalog(storage, switcher);
        switchController = new ProfileSwitchController(switcher, ProfileStatsWalks::stopAll, ProfileTasks::isTaskRunning, () -> getActiveProfile().getId());
        switchController.completePendingSwitchAtLaunch();
        ProfileTasks.cleanUpLeftovers(storage);
        EventHandler.INSTANCE.registerListenersOf(ProfileHandler.class);
    }

    @NotNull
    public static ProfileStorage getStorage() {
        return Objects.requireNonNull(storage, "ProfileHandler not initialized yet!");
    }

    /**
     * Lists all profiles. The active profile comes first, all others are sorted by their title.<br>
     * Fixes broken stored profiles on the way, like directories without metadata or metadata with a wrong ID, but never while a switch is pending.
     */
    @NotNull
    public static List<Profile> getProfiles() {
        boolean writable = getSwitchController().tryCompletePendingSwitch();
        return catalog.listProfiles(writable, getDefaultTitle());
    }

    /**
     * The profile that is currently loaded. Creates its metadata if it has none yet, like the setup of a user that never used profiles before.
     */
    @NotNull
    public static Profile getActiveProfile() {
        boolean writable = getSwitchController().tryCompletePendingSwitch();
        return catalog.readActiveProfile(writable, getDefaultTitle());
    }

    @Nullable
    public static Profile getProfile(@NotNull String profileId) {
        for (Profile profile : getProfiles()) {
            if (profile.getId().equals(profileId)) return profile;
        }
        return null;
    }

    /**
     * The current state of a listed profile. Listed profiles can be outdated, like when completing a pending switch moved the previously active profile
     * from its stash into the profile store.
     *
     * @return the profile, or NULL if it doesn't exist anymore.
     */
    @Nullable
    static Profile findCurrentProfile(@NotNull Profile profile) {
        return getProfile(profile.getId());
    }

    /**
     * Saves new metadata for a profile. The ID of a profile never changes.
     */
    public static void saveMetadata(@NotNull Profile profile, @NotNull ProfileMetadata metadata) throws IOException {
        prepareOperation();
        if (isActiveProfileIncomplete()) throw new IOException("The last profile switch is not complete yet!");
        Profile current = findCurrentProfile(profile);
        if (current == null) throw new NoSuchFileException(profile.directory().toString(), null, "The profile does not exist anymore");
        ProfileMetadata toSave = metadata.copy();
        toSave.setId(current.getId());
        getStorage().writeMetadata(current.directory(), toSave);
    }

    /**
     * Has to get called before a profile operation reads or changes profile files: stops the background walks of the profile manager,
     * which keep directories open, and completes a pending profile switch if possible.
     */
    static void prepareOperation() {
        ProfileStatsWalks.stopAll();
        getSwitchController().tryCompletePendingSwitch();
    }

    /**
     * Whether a profile switch or a profile task is in progress, which blocks all other profile operations.
     */
    public static boolean isBusy() {
        return getSwitchController().isSwitching() || ProfileTasks.isTaskRunning();
    }

    public static boolean isSwitching() {
        return getSwitchController().isSwitching();
    }

    /**
     * Whether the last profile switch could neither get completed nor rolled back yet, so a part of the active profile is still in the profile store.
     * Loading a profile completes it, and so does the next launch. Until then, all other profile operations get refused.
     */
    public static boolean isActiveProfileIncomplete() {
        return (switcher != null) && switcher.isActiveProfileIncomplete();
    }

    /**
     * Changes whenever profiles changed on disk outside of the operations of the profile manager, like when a pending switch got completed in the background.
     */
    public static long getRevision() {
        return getSwitchController().getRevision();
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
        getSwitchController().startSwitch(targetProfileId, keepActiveProfile, new ProfileSwitchController.SwitchRuntime() {
            @Override
            public boolean suspend() {
                return ProfileRuntimeReloader.suspend(keepOpenWindow);
            }
            @Override
            public void resume(boolean activeProfileChanged) {
                ProfileRuntimeReloader.resume(activeProfileChanged);
            }
        }, callback);
    }

    @EventListener
    public static void onClientTickPost(ClientTickEvent.Post e) {
        getSwitchController().tick();
    }

    @NotNull
    private static ProfileSwitchController getSwitchController() {
        return Objects.requireNonNull(switchController, "ProfileHandler not initialized yet!");
    }

    @NotNull
    private static String getDefaultTitle() {
        return I18n.get("fancymenu.profiles.default_profile_title");
    }

}
