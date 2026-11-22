package de.keksuccino.fancymenu.customization.profile;

import de.keksuccino.fancymenu.customization.profile.ProfileSwitchJournal.Phase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

/**
 * Swaps the active profile with a stored one on disk.<br><br>
 *
 * A switch has three phases, and the current phase gets written to a {@link ProfileSwitchJournal} before each phase starts:
 * <ol>
 *     <li>STASH: The content of the active profile gets moved into a stash directory in the profile store.</li>
 *     <li>UNPACK: The content of the target profile gets moved into the root directory.</li>
 *     <li>FINISH: The stash directory becomes the stored directory of the previously active profile, or gets deleted if the previously active profile
 *     gets discarded, and the emptied directory of the target profile gets deleted.</li>
 * </ol>
 * Every phase only renames top-level entries, so a switch is fast and never copies data. Once the FINISH phase starts, the content of both profiles
 * is swapped, so the switch counts as done, even if cleaning up the profile store fails.<br><br>
 *
 * The switch stays pending as long as its journal exists, like after a crash, or when files were in use and a rollback failed too.
 * {@link #completePendingSwitch()} then uses the journal to roll the switch back (STASH) or to complete it (UNPACK, FINISH).
 * Data of the previously active profile never gets deleted for that, only merged with newer versions of itself.
 */
public class ProfileSwitcher {

    private static final Logger LOGGER = LogManager.getLogger();

    static final String STASH_DIRECTORY_PREFIX = ProfileStorage.INTERNAL_PREFIX + "stash_";

    @NotNull
    private final ProfileStorage storage;

    public ProfileSwitcher(@NotNull ProfileStorage storage) {
        this.storage = storage;
    }

    /**
     * Makes a stored profile the active one.
     *
     * @param activeProfileId the ID of the currently active profile. Becomes the name of its stored directory.
     * @param targetProfileId the ID of the stored profile to activate.
     * @param keepActiveProfile if FALSE, the currently active profile gets deleted instead of stored, for example when an imported profile replaces it.
     * @return TRUE if the switch is finished. FALSE if the content of both profiles got swapped, but cleaning up the profile store failed.
     *         The switch counts as done then, and stays pending until {@link #completePendingSwitch()} finishes the cleanup.
     * @throws IOException if the switch failed. Failed switches get rolled back, so both profiles stay as they were. If the rollback fails too,
     *                     the switch stays pending with an incomplete active profile (see {@link #isActiveProfileIncomplete()}),
     *                     until {@link #completePendingSwitch()} completes the rollback or the switch.
     */
    public boolean switchTo(@NotNull String activeProfileId, @NotNull String targetProfileId, boolean keepActiveProfile) throws IOException {
        if (activeProfileId.equals(targetProfileId)) throw new IllegalArgumentException("Can't switch to the already active profile!");
        Path targetDirectory = this.storage.getStoredProfileDirectory(targetProfileId);
        Path activeStoredDirectory = this.storage.getStoredProfileDirectory(activeProfileId);
        Path stashDirectory = this.getStashDirectory(activeProfileId);
        Path root = this.storage.getRootDirectory();
        if (this.isSwitchPending()) throw new IOException("Another profile switch is still in progress!");
        if (!Files.isDirectory(targetDirectory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("The target profile does not exist: " + targetDirectory);
        if (keepActiveProfile && Files.exists(activeStoredDirectory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("A stored profile with the ID of the active profile already exists: " + activeStoredDirectory);
        if (Files.exists(stashDirectory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("A stash of the active profile already exists: " + stashDirectory);

        ProfileSwitchJournal stashJournal = new ProfileSwitchJournal(activeProfileId, targetProfileId, keepActiveProfile, Phase.STASH, false);
        this.writeJournal(stashJournal);
        try {
            ProfileFiles.moveEntries(this.storage.listContentEntries(root), stashDirectory, null);
        } catch (IOException | RuntimeException ex) {
            this.rollBackStash(stashJournal, ex);
            throw ex;
        }

        ProfileSwitchJournal unpackJournal = stashJournal.withPhase(Phase.UNPACK);
        List<Path> targetEntries;
        try {
            this.writeJournal(unpackJournal);
            targetEntries = this.storage.listContentEntries(targetDirectory);
        } catch (IOException | RuntimeException ex) {
            // Nothing got unpacked yet
            this.rollBackStash(stashJournal, ex);
            throw ex;
        }
        try {
            ProfileFiles.moveEntries(targetEntries, root, this.createStrayHandler(unpackJournal));
        } catch (IOException | RuntimeException ex) {
            this.rollBackUnpack(stashJournal, targetEntries, ex);
            throw ex;
        }

        // Without this journal, the switch stays pending in the UNPACK phase, which completePendingSwitch() completes
        ProfileSwitchJournal finishJournal = unpackJournal.withPhase(Phase.FINISH);
        this.writeJournal(finishJournal);
        try {
            this.finish(finishJournal);
            return true;
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("[FANCYMENU] Switched the profile, but failed to clean up the profile store afterwards! The cleanup gets retried later.", ex);
            return false;
        }
    }

    /**
     * Completes a pending switch: rolls it back in the STASH phase, completes the unpack in the UNPACK phase and cleans up the profile store.
     * Before that, stashes that no journal knows about become stored profiles again, like stashes that failed switches of older FancyMenu versions left behind.
     * They would hide the data of their profile and block switching away from it.<br>
     * In the STASH and UNPACK phases, this moves entries of the root directory, so nothing may use the files of the active profile meanwhile.
     * Has to get called before anything reads the content of the active profile at launch.
     *
     * @throws IOException if the switch is still pending afterwards. Calling this again continues where it stopped.
     */
    public void completePendingSwitch() throws IOException {
        Path journalFile = this.getJournalFile();
        boolean pending = Files.exists(journalFile, LinkOption.NOFOLLOW_LINKS);
        ProfileSwitchJournal journal = null;
        IOException journalError = null;
        if (pending) {
            try {
                journal = ProfileSwitchJournal.read(journalFile);
            } catch (IOException ex) {
                journalError = ex;
            }
        }
        this.storeOrphanedStashes((journal != null) ? journal.fromId() : null);
        if (!pending) return;
        if (journal == null) {
            // There is no way to tell how far the switch got, but storing all stashes above keeps nothing hidden
            LOGGER.error("[FANCYMENU] Found a broken profile switch journal! Kept all stashed profile data as stored profiles and deleted the journal.", journalError);
            ProfileFiles.delete(journalFile);
            return;
        }
        LOGGER.warn("[FANCYMENU] Completing the pending profile switch from '" + journal.fromId() + "' to '" + journal.toId() + "' (phase: " + journal.phase() + ")..");
        switch (journal.phase()) {
            case STASH -> {
                this.restoreStash(journal);
                ProfileFiles.delete(journalFile);
            }
            case UNPACK -> {
                this.unpackRemainingEntries(journal);
                ProfileSwitchJournal finishJournal = journal.withPhase(Phase.FINISH);
                this.writeJournal(finishJournal);
                this.finish(finishJournal);
            }
            // The unpack is complete in this phase, so unpacking again would treat entries of the target profile as strays of the previous one
            case FINISH -> this.finish(journal);
        }
        LOGGER.info("[FANCYMENU] Completed the pending profile switch!");
    }

    /**
     * Whether a switch is in progress, got interrupted, or still needs its cleanup.
     */
    public boolean isSwitchPending() {
        return Files.exists(this.getJournalFile(), LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * Whether a pending switch left the active profile incomplete, with a part of its content still in the profile store.
     * Completing such a switch moves entries of the root directory, so FancyMenu has to get suspended and reloaded around it.
     */
    public boolean isActiveProfileIncomplete() {
        ProfileSwitchJournal journal = this.readJournal();
        return (journal != null) && (journal.phase() != Phase.FINISH);
    }

    /**
     * Has to get called before FancyMenu uses the files of an incomplete active profile (see {@link #isActiveProfileIncomplete()}).
     * FancyMenu creates default files for everything that is still in the profile store then, and completing the switch later
     * keeps the stashed data instead of these defaults.
     */
    public void markActiveProfileInUse() throws IOException {
        ProfileSwitchJournal journal = this.readJournal();
        if ((journal == null) || (journal.phase() == Phase.FINISH) || journal.activeProfileInUse()) return;
        this.writeJournal(journal.withActiveProfileInUse());
    }

    /**
     * @return the journal of the pending switch, or NULL if there is none or it is broken.
     */
    @Nullable
    ProfileSwitchJournal readJournal() {
        Path journalFile = this.getJournalFile();
        if (!Files.exists(journalFile, LinkOption.NOFOLLOW_LINKS)) return null;
        try {
            return ProfileSwitchJournal.read(journalFile);
        } catch (IOException ex) {
            return null;
        }
    }

    @NotNull
    Path getJournalFile() {
        return this.storage.getStoreDirectory().resolve(ProfileSwitchJournal.FILE_NAME);
    }

    @NotNull
    Path getStashDirectory(@NotNull String profileId) {
        if (!ProfileStorage.isValidProfileId(profileId)) throw new IllegalArgumentException("Invalid profile ID: " + profileId);
        return this.storage.getStoreDirectory().resolve(STASH_DIRECTORY_PREFIX + profileId);
    }

    /**
     * Moves everything back into the root directory after the stash or the unpack failed. The target profile has to be complete again.
     * Gets used when a switch fails, so it only logs and adds failures to the exception that made the switch fail.
     * If something stays in the stash, the journal stays in the STASH phase, so {@link #completePendingSwitch()} restores the rest later.
     */
    private void rollBackStash(@NotNull ProfileSwitchJournal stashJournal, @NotNull Exception cause) {
        try {
            this.writeJournal(stashJournal);
            this.restoreStash(stashJournal);
            ProfileFiles.delete(this.getJournalFile());
        } catch (Exception rollbackEx) {
            LOGGER.error("[FANCYMENU] Failed to roll back the failed profile switch! It gets rolled back later.", rollbackEx);
            cause.addSuppressed(rollbackEx);
        }
    }

    /**
     * Rolls back a failed unpack. {@link ProfileFiles#moveEntries(List, Path, ProfileFiles.ConflictHandler)} already moved the unpacked entries back,
     * unless that failed too. Then entries of the target profile are still in the root directory, and restoring the stash would mix both profiles,
     * so the switch stays pending in the UNPACK phase, and {@link #completePendingSwitch()} completes it later instead.
     */
    private void rollBackUnpack(@NotNull ProfileSwitchJournal stashJournal, @NotNull List<Path> targetEntries, @NotNull Exception cause) {
        for (Path entry : targetEntries) {
            if (!Files.exists(entry, LinkOption.NOFOLLOW_LINKS)) {
                LOGGER.error("[FANCYMENU] Failed to move an entry of the target profile back while rolling back a failed profile switch, so the switch gets completed later instead: " + entry);
                return;
            }
        }
        this.rollBackStash(stashJournal, cause);
    }

    /**
     * Moves everything of the stash back into the root directory. Entries the root directory has too were written there after the stash,
     * so they get merged with their stashed versions (see {@link ProfileSwitchJournal#activeProfileInUse()} for which side wins).
     */
    private void restoreStash(@NotNull ProfileSwitchJournal journal) throws IOException {
        Path stashDirectory = this.getStashDirectory(journal.fromId());
        if (!Files.exists(stashDirectory, LinkOption.NOFOLLOW_LINKS)) return;
        boolean stashWins = journal.activeProfileInUse();
        ProfileFiles.moveDirectoryContent(stashDirectory, this.storage.getRootDirectory(), (stashed, existing) -> {
            ProfileFiles.moveMerging(stashed, existing, stashWins);
            return false;
        });
    }

    private void unpackRemainingEntries(@NotNull ProfileSwitchJournal journal) throws IOException {
        Path targetDirectory = this.storage.getStoredProfileDirectory(journal.toId());
        if (!Files.isDirectory(targetDirectory, LinkOption.NOFOLLOW_LINKS)) return;
        ProfileFiles.moveEntriesBestEffort(this.storage.listContentEntries(targetDirectory), this.storage.getRootDirectory(), this.createStrayHandler(journal));
    }

    /**
     * Handles entries in the root directory that block entries of the target profile while unpacking. They can't belong to the target profile,
     * because its entry is still in the target directory, so they belong to the previously active profile: either it wrote them after it got stashed,
     * which makes them newer than their stashed versions, or FancyMenu created them as defaults while it used the incomplete active profile,
     * which must never replace stashed data (see {@link ProfileSwitchJournal#activeProfileInUse()}). Either way they get merged into the stash.
     */
    @NotNull
    private ProfileFiles.ConflictHandler createStrayHandler(@NotNull ProfileSwitchJournal journal) {
        Path stashDirectory = this.getStashDirectory(journal.fromId());
        boolean strayWins = !journal.activeProfileInUse();
        return (targetEntry, stray) -> {
            Files.createDirectories(stashDirectory);
            ProfileFiles.moveMerging(stray, stashDirectory.resolve(stray.getFileName().toString()), strayWins);
            return true;
        };
    }

    private void finish(@NotNull ProfileSwitchJournal journal) throws IOException {
        Path stashDirectory = this.getStashDirectory(journal.fromId());
        if (Files.exists(stashDirectory, LinkOption.NOFOLLOW_LINKS)) {
            if (journal.keepActiveProfile()) {
                this.storeStash(journal.fromId(), stashDirectory);
            } else {
                ProfileFiles.deleteRecursively(stashDirectory);
            }
        }
        this.deleteLeftoverTargetDirectory(journal.toId());
        ProfileFiles.delete(this.getJournalFile());
    }

    /**
     * Turns a stash into the stored directory of its profile. If the active profile or a stored one has the ID already, like a directory
     * a user put into the store by hand, the stash gets stored under a new ID, so nothing gets overwritten. Listing the profiles fixes the ID in its metadata.
     */
    private void storeStash(@NotNull String profileId, @NotNull Path stashDirectory) throws IOException {
        Path storedDirectory = ProfileStorage.isValidProfileId(profileId) ? this.storage.getStoredProfileDirectory(profileId) : null;
        if ((storedDirectory == null) || Files.exists(storedDirectory, LinkOption.NOFOLLOW_LINKS) || profileId.equals(this.readActiveProfileId())) {
            String newId = ProfileStorage.generateProfileId(profileId, this.storage.getStoredProfileIds());
            LOGGER.warn("[FANCYMENU] The ID of the stashed profile '" + profileId + "' is taken already, so it gets stored as '" + newId + "'.");
            storedDirectory = this.storage.getStoredProfileDirectory(newId);
        }
        ProfileFiles.renameDirectory(stashDirectory, storedDirectory);
    }

    /**
     * Stashes only exist while a switch is pending. All others are leftovers that would hide the data of their profile, so they become stored profiles.
     *
     * @param pendingProfileId the ID of the profile whose stash belongs to the pending switch, or NULL if no switch is pending.
     */
    private void storeOrphanedStashes(@Nullable String pendingProfileId) {
        Path store = this.storage.getStoreDirectory();
        if (!Files.isDirectory(store, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            for (Path entry : ProfileFiles.listEntries(store)) {
                String name = entry.getFileName().toString();
                if (!name.startsWith(STASH_DIRECTORY_PREFIX) || !Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) continue;
                String profileId = name.substring(STASH_DIRECTORY_PREFIX.length());
                if (profileId.equals(pendingProfileId)) continue;
                LOGGER.warn("[FANCYMENU] Found a leftover stash of profile '" + profileId + "', which no profile switch needs. Storing it as profile..");
                try {
                    this.storeStash(profileId, entry);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to store a leftover profile stash: " + entry, ex);
                }
            }
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to look for leftover profile stashes!", ex);
        }
    }

    /**
     * Deletes the directory the target profile got unpacked from. The unpack moved all of its entries, so everything still in it got created afterwards:
     * metadata that older FancyMenu versions wrote into the empty directory and files the operating system creates on its own get deleted,
     * everything else stays a stored profile of its own, so nothing gets lost.
     */
    private void deleteLeftoverTargetDirectory(@NotNull String targetProfileId) throws IOException {
        Path directory = this.storage.getStoredProfileDirectory(targetProfileId);
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return;
        boolean unknownEntries = false;
        for (Path entry : ProfileFiles.listEntries(directory)) {
            if (isRecreatedFile(entry)) {
                ProfileFiles.delete(entry);
            } else {
                unknownEntries = true;
            }
        }
        if (!unknownEntries) {
            ProfileFiles.deleteEmptyDirectory(directory);
            return;
        }
        String newId = ProfileStorage.generateProfileId(targetProfileId, this.storage.getStoredProfileIds());
        LOGGER.warn("[FANCYMENU] The emptied directory of profile '" + targetProfileId + "' got new files after the profile switch, so they get stored as profile '" + newId + "'.");
        ProfileFiles.renameDirectory(directory, this.storage.getStoredProfileDirectory(newId));
    }

    private static boolean isRecreatedFile(@NotNull Path entry) {
        if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) return false;
        String name = entry.getFileName().toString();
        return name.equals(ProfileStorage.METADATA_FILE_NAME) || name.equals(ProfileStorage.METADATA_FILE_NAME + ".tmp") || ProfileStorage.isJunkFile(entry);
    }

    @Nullable
    private String readActiveProfileId() {
        ProfileMetadata metadata = this.storage.readMetadata(this.storage.getRootDirectory(), "");
        return (metadata != null) ? metadata.getId() : null;
    }

    private void writeJournal(@NotNull ProfileSwitchJournal journal) throws IOException {
        journal.write(this.getJournalFile());
    }

}
