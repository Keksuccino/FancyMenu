package de.keksuccino.fancymenu.customization.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Swaps the active profile with a stored one on disk.<br><br>
 *
 * A switch has three phases, and the current phase gets written to a journal file before each phase starts:
 * <ol>
 *     <li>STASH: The content of the active profile gets moved into a stash directory in the profile store.</li>
 *     <li>UNPACK: The content of the target profile gets moved into the root directory.</li>
 *     <li>FINISH: The stash directory becomes the stored directory of the previously active profile, or gets deleted if the previously active profile gets discarded.</li>
 * </ol>
 * Every phase only renames top-level entries, so a switch is fast and never copies data. If the game crashes in the middle of a switch,
 * {@link #recoverInterruptedSwitch()} uses the journal to either roll the switch back (STASH) or complete it (UNPACK, FINISH).
 */
public class ProfileSwitcher {

    private static final Logger LOGGER = LogManager.getLogger();

    static final String JOURNAL_FILE_NAME = ProfileStorage.INTERNAL_PREFIX + "switch_journal.json";
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
     * @throws IOException if the switch failed. Failed switches get rolled back, so both profiles stay as they were.
     */
    public void switchTo(@NotNull String activeProfileId, @NotNull String targetProfileId, boolean keepActiveProfile) throws IOException {
        if (activeProfileId.equals(targetProfileId)) throw new IllegalArgumentException("Can't switch to the already active profile!");
        Path targetDirectory = this.storage.getStoredProfileDirectory(targetProfileId);
        Path activeStoredDirectory = this.storage.getStoredProfileDirectory(activeProfileId);
        Path stashDirectory = this.getStashDirectory(activeProfileId);
        Path root = this.storage.getRootDirectory();
        if (!Files.isDirectory(targetDirectory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("The target profile does not exist: " + targetDirectory);
        if (keepActiveProfile && Files.exists(activeStoredDirectory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("A stored profile with the ID of the active profile already exists: " + activeStoredDirectory);
        if (Files.exists(stashDirectory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("A stash of the active profile already exists: " + stashDirectory);
        if (this.isSwitchPending()) throw new IOException("Another profile switch is still in progress!");

        this.writeJournal(new Journal(activeProfileId, targetProfileId, keepActiveProfile, Phase.STASH));
        try {
            ProfileFiles.moveEntries(this.storage.listContentEntries(root), stashDirectory, null);
        } catch (IOException | RuntimeException ex) {
            ProfileFiles.deleteQuietly(stashDirectory);
            this.deleteJournalQuietly();
            throw ex;
        }

        this.writeJournal(new Journal(activeProfileId, targetProfileId, keepActiveProfile, Phase.UNPACK));
        try {
            ProfileFiles.moveEntries(this.storage.listContentEntries(targetDirectory), root, (source, target) -> moveStrayEntryToStash(target, stashDirectory));
        } catch (IOException | RuntimeException ex) {
            try {
                ProfileFiles.moveDirectoryContent(stashDirectory, root, ProfileSwitcher::dropOlderStashedEntry);
                this.deleteJournalQuietly();
            } catch (Exception rollbackEx) {
                // The journal stays, so the next launch completes the rollback
                ex.addSuppressed(rollbackEx);
            }
            throw ex;
        }

        Journal finishJournal = new Journal(activeProfileId, targetProfileId, keepActiveProfile, Phase.FINISH);
        this.writeJournal(finishJournal);
        this.finish(finishJournal);
    }

    /**
     * Rolls back or completes a switch that got interrupted by a crash. Has to get called before anything reads the content of the active profile.
     *
     * @return TRUE if an interrupted switch was found.
     */
    public boolean recoverInterruptedSwitch() {
        Path journalFile = this.getJournalFile();
        if (!Files.isRegularFile(journalFile, LinkOption.NOFOLLOW_LINKS)) return false;
        Journal journal = this.readJournal();
        if (journal == null) {
            LOGGER.error("[FANCYMENU] Found a broken profile switch journal! Can't recover the interrupted profile switch: " + journalFile);
            return true;
        }
        LOGGER.warn("[FANCYMENU] Recovering interrupted profile switch from '" + journal.fromId + "' to '" + journal.toId + "' (phase: " + journal.phase + ")..");
        Path root = this.storage.getRootDirectory();
        Path stashDirectory = this.getStashDirectory(journal.fromId);
        Path targetDirectory = this.storage.getStoredProfileDirectory(journal.toId);
        try {
            if (journal.phase == Phase.STASH) {
                // Roll back, so everything that already got stashed goes back to the root directory
                if (Files.isDirectory(stashDirectory, LinkOption.NOFOLLOW_LINKS)) {
                    ProfileFiles.moveDirectoryContent(stashDirectory, root, ProfileSwitcher::dropOlderStashedEntry);
                }
                Files.deleteIfExists(journalFile);
            } else {
                // Complete, so the rest of the target profile gets unpacked before the switch gets finished
                if (Files.isDirectory(targetDirectory, LinkOption.NOFOLLOW_LINKS)) {
                    ProfileFiles.moveEntries(this.storage.listContentEntries(targetDirectory), root, (source, target) -> moveStrayEntryToStash(target, stashDirectory));
                }
                this.finish(journal);
            }
            LOGGER.info("[FANCYMENU] Successfully recovered the interrupted profile switch!");
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to recover the interrupted profile switch!", ex);
        }
        return true;
    }

    private void finish(@NotNull Journal journal) throws IOException {
        ProfileFiles.deleteEmptyDirectory(this.storage.getStoredProfileDirectory(journal.toId));
        Path stashDirectory = this.getStashDirectory(journal.fromId);
        if (Files.isDirectory(stashDirectory, LinkOption.NOFOLLOW_LINKS)) {
            if (journal.keepActiveProfile) {
                ProfileFiles.renameDirectory(stashDirectory, this.storage.getStoredProfileDirectory(journal.fromId));
            } else {
                ProfileFiles.deleteRecursively(stashDirectory);
            }
        }
        Files.deleteIfExists(this.getJournalFile());
    }

    /**
     * Handles entries that got recreated in the root directory after the active profile got stashed, for example by a late write
     * of the previous profile. They belong to the previous profile and are newer than their stashed version.
     */
    private static boolean moveStrayEntryToStash(@NotNull Path strayEntry, @NotNull Path stashDirectory) throws IOException {
        Path stashed = stashDirectory.resolve(strayEntry.getFileName().toString());
        ProfileFiles.deleteRecursively(stashed);
        Files.createDirectories(stashDirectory);
        Files.move(strayEntry, stashed);
        return true;
    }

    /**
     * Resolves conflicts when restoring a stash, by keeping the entry in the root directory, because it got written after the stashed one.
     */
    private static boolean dropOlderStashedEntry(@NotNull Path stashedEntry, @NotNull Path newerEntry) throws IOException {
        ProfileFiles.deleteRecursively(stashedEntry);
        return false;
    }

    /**
     * Whether a switch is in progress or got interrupted.
     */
    public boolean isSwitchPending() {
        return Files.exists(this.getJournalFile(), LinkOption.NOFOLLOW_LINKS);
    }

    @NotNull
    Path getJournalFile() {
        return this.storage.getStoreDirectory().resolve(JOURNAL_FILE_NAME);
    }

    @NotNull
    Path getStashDirectory(@NotNull String profileId) {
        if (!ProfileStorage.isValidProfileId(profileId)) throw new IllegalArgumentException("Invalid profile ID: " + profileId);
        return this.storage.getStoreDirectory().resolve(STASH_DIRECTORY_PREFIX + profileId);
    }

    private void writeJournal(@NotNull Journal journal) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("from", journal.fromId);
        json.addProperty("to", journal.toId);
        json.addProperty("keep_active", journal.keepActiveProfile);
        json.addProperty("phase", journal.phase.name());
        Path journalFile = this.getJournalFile();
        Files.createDirectories(journalFile.getParent());
        Path tempFile = journalFile.resolveSibling(JOURNAL_FILE_NAME + ".tmp");
        Files.writeString(tempFile, json.toString(), StandardCharsets.UTF_8);
        ProfileStorage.moveReplacing(tempFile, journalFile);
    }

    @Nullable
    private Journal readJournal() {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(this.getJournalFile(), StandardCharsets.UTF_8)).getAsJsonObject();
            String fromId = json.get("from").getAsString();
            String toId = json.get("to").getAsString();
            boolean keepActive = !json.has("keep_active") || json.get("keep_active").getAsBoolean();
            Phase phase = Phase.valueOf(json.get("phase").getAsString());
            if (!ProfileStorage.isValidProfileId(fromId) || !ProfileStorage.isValidProfileId(toId)) return null;
            return new Journal(fromId, toId, keepActive, phase);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read profile switch journal!", ex);
            return null;
        }
    }

    private void deleteJournalQuietly() {
        ProfileFiles.deleteQuietly(this.getJournalFile());
    }

    enum Phase {
        STASH,
        UNPACK,
        FINISH
    }

    private record Journal(@NotNull String fromId, @NotNull String toId, boolean keepActiveProfile, @NotNull Phase phase) {
    }

}
