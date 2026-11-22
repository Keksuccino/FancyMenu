package de.keksuccino.fancymenu.customization.profile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lists the profiles on disk and repairs broken ones on the way, like directories without metadata or metadata with a wrong ID.<br>
 * While a profile switch is pending, it only reads: the switch still has to move the files, and repairs would mix files of the wrong profile into them.
 */
final class ProfileCatalog {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    private final ProfileStorage storage;
    @NotNull
    private final ProfileSwitcher switcher;

    ProfileCatalog(@NotNull ProfileStorage storage, @NotNull ProfileSwitcher switcher) {
        this.storage = storage;
        this.switcher = switcher;
    }

    /**
     * Lists all profiles. The active profile comes first, all others are sorted by their title.
     *
     * @param writable FALSE while a profile switch is pending. Then nothing gets written, and once the content of the target profile got unpacked,
     *                 the profiles get listed like they will be after the switch, so the previously active profile shows up although it is still stashed.
     * @param defaultTitle the title of an active profile without metadata, like the setup of a user that never used profiles before.
     */
    @NotNull
    List<Profile> listProfiles(boolean writable, @NotNull String defaultTitle) {
        List<Profile> profiles = new ArrayList<>();
        profiles.add(this.readActiveProfile(writable, defaultTitle));
        if (writable) this.adoptForeignDirectories();
        ProfileSwitchJournal pending = writable ? null : this.switcher.readJournal();
        // After the unpack, the stored directory of the target profile is only a leftover, because the target profile is the active one
        boolean unpacked = (pending != null) && (pending.phase() != ProfileSwitchJournal.Phase.STASH);
        List<Profile> stored = new ArrayList<>();
        for (Path directory : this.storage.listStoredProfileDirectories()) {
            String id = directory.getFileName().toString();
            if (unpacked && id.equals(pending.toId())) continue;
            stored.add(this.readStoredProfile(directory, id, writable));
        }
        if (unpacked && pending.keepActiveProfile()) {
            Path stash = this.switcher.getStashDirectory(pending.fromId());
            boolean listed = stored.stream().anyMatch(profile -> profile.getId().equals(pending.fromId()));
            if (!listed && Files.isDirectory(stash, LinkOption.NOFOLLOW_LINKS)) stored.add(this.readStoredProfile(stash, pending.fromId(), false));
        }
        stored.sort(Comparator.comparing((Profile profile) -> profile.metadata().getTitle(), String.CASE_INSENSITIVE_ORDER).thenComparing(Profile::getId));
        profiles.addAll(stored);
        return profiles;
    }

    /**
     * The profile that is currently loaded. Creates its metadata if it has none yet, or if its ID is taken by a stored profile.
     *
     * @param writable FALSE while a profile switch is pending. Then missing metadata only gets created in memory.
     */
    @NotNull
    Profile readActiveProfile(boolean writable, @NotNull String defaultTitle) {
        Path root = this.storage.getRootDirectory();
        ProfileMetadata metadata = this.storage.readMetadata(root, "");
        boolean changed = false;
        if (metadata == null) {
            metadata = new ProfileMetadata("");
            metadata.setTitle(defaultTitle);
            changed = true;
        }
        // While a switch is pending, a stored directory with the active ID can be the leftover of the target profile, so taken IDs only count when writable
        boolean idTaken = writable && this.storage.getStoredProfileIds().contains(metadata.getId());
        if (!ProfileStorage.isValidProfileId(metadata.getId()) || idTaken) {
            metadata.setId(ProfileStorage.generateProfileId(metadata.getTitle(), this.storage.getStoredProfileIds()));
            changed = true;
        }
        if (changed && writable) this.writeMetadataQuietly(root, metadata);
        return new Profile(metadata, root, true);
    }

    @NotNull
    private Profile readStoredProfile(@NotNull Path directory, @NotNull String id, boolean writable) {
        ProfileMetadata metadata = this.storage.readMetadata(directory, id);
        boolean changed = false;
        if (metadata == null) {
            metadata = new ProfileMetadata(id);
            metadata.setTitle(id);
            changed = true;
        } else if (!metadata.getId().equals(id)) {
            metadata.setId(id);
            changed = true;
        }
        if (changed && writable) this.writeMetadataQuietly(directory, metadata);
        return new Profile(metadata, directory, false);
    }

    /**
     * Turns directories that users put into the profile store by hand, like copies of a config directory, into proper profiles.
     */
    private void adoptForeignDirectories() {
        for (Path foreign : this.storage.listForeignStoreDirectories()) {
            String title = foreign.getFileName().toString();
            String id = ProfileStorage.generateProfileId(title, this.storage.getStoredProfileIds());
            try {
                Path target = this.storage.getStoredProfileDirectory(id);
                ProfileFiles.renameDirectory(foreign, target);
                ProfileMetadata metadata = this.storage.readMetadata(target, id);
                if (metadata == null) {
                    metadata = new ProfileMetadata(id);
                    metadata.setTitle(title);
                }
                metadata.setId(id);
                this.storage.writeMetadata(target, metadata);
                LOGGER.info("[FANCYMENU] Added directory '" + title + "' of the profile store as profile '" + id + "'.");
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to add directory of the profile store as profile: " + foreign, ex);
            }
        }
    }

    private void writeMetadataQuietly(@NotNull Path directory, @NotNull ProfileMetadata metadata) {
        try {
            this.storage.writeMetadata(directory, metadata);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to write profile metadata: " + directory, ex);
        }
    }

}
