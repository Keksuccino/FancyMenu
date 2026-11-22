package de.keksuccino.fancymenu.customization.profile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs the profile switches of {@link ProfileHandler} on top of {@link ProfileSwitcher}: keeps FancyMenu suspended while the files of the active
 * profile change, retries switches that failed because files were in use, and completes switches that are still pending, so a switch that
 * failed or got interrupted never blocks the next one for good.<br>
 * Gets driven by client ticks, so everything happens on the main thread.
 */
final class ProfileSwitchController {

    private static final Logger LOGGER = LogManager.getLogger();
    /** Switches fail while another program has a profile file open, so they get retried a few times before giving up. */
    static final int MAX_SWITCH_ATTEMPTS = 4;
    static final int SWITCH_RETRY_DELAY_TICKS = 10;
    /** A switch counts as done once the content of both profiles got swapped, so its cleanup of the profile store gets retried for longer in the background. */
    static final int MAX_CLEANUP_ATTEMPTS = 8;
    static final int CLEANUP_RETRY_DELAY_TICKS = 20;

    @NotNull
    private final ProfileSwitcher switcher;
    @NotNull
    private final Runnable stopProfileWalks;
    @NotNull
    private final BooleanSupplier taskRunning;
    /** Reads the ID of the active profile, and creates its metadata if it has none yet, so it gets stored with a proper title. */
    @NotNull
    private final Supplier<String> activeProfileId;
    @Nullable
    private PendingSwitch pendingSwitch;
    private long ticks = 0L;
    private int cleanupAttemptsLeft = 0;
    private long cleanupRetryAtTick = -1L;
    private long revision = 0L;

    /**
     * @param stopProfileWalks stops everything in the background that keeps profile directories open, which makes moving them fail on Windows.
     * @param taskRunning whether a profile task is running, which moves profile files on its own.
     */
    ProfileSwitchController(@NotNull ProfileSwitcher switcher, @NotNull Runnable stopProfileWalks, @NotNull BooleanSupplier taskRunning, @NotNull Supplier<String> activeProfileId) {
        this.switcher = switcher;
        this.stopProfileWalks = stopProfileWalks;
        this.taskRunning = taskRunning;
        this.activeProfileId = activeProfileId;
    }

    /**
     * Completes a switch that is still pending from the last session. Has to get called at launch, before anything reads the content of the active profile.
     */
    void completePendingSwitchAtLaunch() {
        try {
            this.switcher.completePendingSwitch();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to complete the pending profile switch! It gets completed later.", ex);
        }
        this.onSwitchStillPending();
    }

    /**
     * Makes a stored profile the active one. A switch that is still pending gets completed first.
     *
     * @param runtime suspends FancyMenu before the files change and reloads it afterwards.
     * @param callback gets called with NULL on success or the error that made the switch fail.
     */
    void startSwitch(@NotNull String targetProfileId, boolean keepActiveProfile, @NotNull SwitchRuntime runtime, @NotNull Consumer<Throwable> callback) {
        if (this.pendingSwitch != null) {
            callback.accept(new IllegalStateException("Another profile switch is still in progress!"));
            return;
        }
        LOGGER.info("[FANCYMENU] Switching profile to '" + targetProfileId + "'..");
        if (!runtime.suspend()) {
            runtime.resume(false);
            callback.accept(new IOException("Failed to save pending variable changes!"));
            return;
        }
        this.pendingSwitch = new PendingSwitch(targetProfileId, keepActiveProfile, runtime, callback);
        this.attemptPendingSwitch();
    }

    boolean isSwitching() {
        return this.pendingSwitch != null;
    }

    /**
     * Completes a pending switch, but only if that works while FancyMenu uses the files of the active profile: when they are complete and only
     * the profile store needs its cleanup, like after the last step of a switch failed. Switches that left the active profile incomplete
     * only get completed by the next switch or at the next launch, because FancyMenu has to get reloaded for them.
     *
     * @return TRUE if no switch is pending anymore.
     */
    boolean tryCompletePendingSwitch() {
        if (!this.switcher.isSwitchPending()) return true;
        // Running switches and tasks move profile files themselves
        if ((this.pendingSwitch != null) || this.taskRunning.getAsBoolean()) return false;
        if (this.switcher.isActiveProfileIncomplete()) return false;
        this.stopProfileWalks.run();
        try {
            this.switcher.completePendingSwitch();
            this.revision++;
            return true;
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to complete the pending profile switch! Trying again later..", ex);
            return false;
        }
    }

    /**
     * Changes whenever profiles changed on disk, like when a pending switch got completed in the background.
     */
    long getRevision() {
        return this.revision;
    }

    void tick() {
        this.ticks++;
        PendingSwitch pending = this.pendingSwitch;
        if (pending != null) {
            if ((pending.retryAtTick >= 0L) && (this.ticks >= pending.retryAtTick)) this.attemptPendingSwitch();
            return;
        }
        if ((this.cleanupRetryAtTick >= 0L) && (this.ticks >= this.cleanupRetryAtTick) && !this.taskRunning.getAsBoolean()) {
            this.cleanupRetryAtTick = -1L;
            if (!this.tryCompletePendingSwitch() && (--this.cleanupAttemptsLeft > 0)) {
                this.cleanupRetryAtTick = this.ticks + CLEANUP_RETRY_DELAY_TICKS;
            }
        }
    }

    private void attemptPendingSwitch() {
        PendingSwitch pending = Objects.requireNonNull(this.pendingSwitch);
        pending.attempts++;
        pending.retryAtTick = -1L;
        this.stopProfileWalks.run();
        boolean finished;
        try {
            // Completing what a failed or interrupted switch left behind moves files of the active profile, so it counts as change
            if (this.switcher.isActiveProfileIncomplete()) pending.activeProfileChanged = true;
            this.switcher.completePendingSwitch();
            String activeId = this.activeProfileId.get();
            if (activeId.equals(pending.targetProfileId)) {
                // Completing the pending switch activated the target already
                finished = true;
            } else {
                LOGGER.info("[FANCYMENU] Switching profile from '" + activeId + "' to '" + pending.targetProfileId + "'..");
                finished = this.switcher.switchTo(activeId, pending.targetProfileId, pending.keepActiveProfile);
                pending.activeProfileChanged = true;
            }
        } catch (Exception ex) {
            if (this.switcher.isActiveProfileIncomplete()) pending.activeProfileChanged = true;
            if ((ex instanceof IOException) && (pending.attempts < MAX_SWITCH_ATTEMPTS)) {
                LOGGER.warn("[FANCYMENU] Profile switch failed (attempt " + pending.attempts + "), retrying soon..", ex);
                pending.retryAtTick = this.ticks + SWITCH_RETRY_DELAY_TICKS;
                return;
            }
            LOGGER.error("[FANCYMENU] Failed to switch profile!", ex);
            this.endSwitch(pending, ex);
            return;
        }
        LOGGER.info("[FANCYMENU] Switched profile to '" + pending.targetProfileId + "'!");
        if (!finished) LOGGER.warn("[FANCYMENU] The cleanup of the profile store after the switch is still pending, so it gets retried in the background.");
        this.endSwitch(pending, null);
    }

    private void endSwitch(@NotNull PendingSwitch pending, @Nullable Throwable error) {
        this.pendingSwitch = null;
        this.onSwitchStillPending();
        this.revision++;
        pending.runtime.resume((error == null) || pending.activeProfileChanged);
        pending.callback.accept(error);
    }

    /**
     * Prepares for FancyMenu using the active profile while a switch is still pending.
     */
    private void onSwitchStillPending() {
        if (this.switcher.isActiveProfileIncomplete()) {
            try {
                this.switcher.markActiveProfileInUse();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to update the journal of the pending profile switch!", ex);
            }
        } else if (this.switcher.isSwitchPending()) {
            this.cleanupAttemptsLeft = MAX_CLEANUP_ATTEMPTS;
            this.cleanupRetryAtTick = this.ticks + CLEANUP_RETRY_DELAY_TICKS;
        }
    }

    /**
     * Stops and reloads everything that uses the files of the active profile around a switch.
     */
    interface SwitchRuntime {

        /**
         * Stops everything that reads or writes files of the active profile and releases all files it keeps open.
         *
         * @return FALSE if pending changes could not get saved, so changing the files would lose data.
         */
        boolean suspend();

        /**
         * Reloads everything from the files of the active profile.
         *
         * @param activeProfileChanged whether the files of the active profile changed since {@link #suspend()}.
         */
        void resume(boolean activeProfileChanged);

    }

    private static final class PendingSwitch {

        @NotNull
        private final String targetProfileId;
        private final boolean keepActiveProfile;
        @NotNull
        private final SwitchRuntime runtime;
        @NotNull
        private final Consumer<Throwable> callback;
        private int attempts = 0;
        private long retryAtTick = -1L;
        private boolean activeProfileChanged = false;

        private PendingSwitch(@NotNull String targetProfileId, boolean keepActiveProfile, @NotNull SwitchRuntime runtime, @NotNull Consumer<Throwable> callback) {
            this.targetProfileId = targetProfileId;
            this.keepActiveProfile = keepActiveProfile;
            this.runtime = runtime;
            this.callback = callback;
        }

    }

}
