package de.keksuccino.fancymenu.customization.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The journal of a pending profile switch. {@link ProfileSwitcher} writes it before every phase of a switch and deletes it once the switch is finished,
 * so a switch that got interrupted, like by a crash or by files that were in use, can get completed or rolled back later.<br>
 * Journals of all FancyMenu versions with profiles stay readable: keys that older versions didn't write fall back to their old behavior.
 *
 * @param fromId the ID of the profile that was active when the switch started.
 * @param toId the ID of the stored profile the switch activates.
 * @param keepActiveProfile if FALSE, the previously active profile gets deleted instead of stored.
 * @param activeProfileInUse whether FancyMenu used the files of the active profile while they were incomplete (STASH and UNPACK phases),
 *                           which makes it create default files for everything that was still in the profile store.
 */
record ProfileSwitchJournal(@NotNull String fromId, @NotNull String toId, boolean keepActiveProfile, @NotNull Phase phase, boolean activeProfileInUse) {

    static final String FILE_NAME = ProfileStorage.INTERNAL_PREFIX + "switch_journal.json";

    private static final String KEY_FROM = "from";
    private static final String KEY_TO = "to";
    private static final String KEY_KEEP_ACTIVE = "keep_active";
    private static final String KEY_PHASE = "phase";
    private static final String KEY_ACTIVE_IN_USE = "active_in_use";

    @NotNull
    ProfileSwitchJournal withPhase(@NotNull Phase newPhase) {
        return new ProfileSwitchJournal(this.fromId, this.toId, this.keepActiveProfile, newPhase, this.activeProfileInUse);
    }

    @NotNull
    ProfileSwitchJournal withActiveProfileInUse() {
        return new ProfileSwitchJournal(this.fromId, this.toId, this.keepActiveProfile, this.phase, true);
    }

    /**
     * Writes the journal atomically, so a crash never leaves a half-written journal behind.
     */
    void write(@NotNull Path file) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty(KEY_FROM, this.fromId);
        json.addProperty(KEY_TO, this.toId);
        json.addProperty(KEY_KEEP_ACTIVE, this.keepActiveProfile);
        json.addProperty(KEY_PHASE, this.phase.name());
        if (this.activeProfileInUse) json.addProperty(KEY_ACTIVE_IN_USE, true);
        Files.createDirectories(file.getParent());
        Path tempFile = file.resolveSibling(file.getFileName().toString() + ".tmp");
        Files.writeString(tempFile, json.toString(), StandardCharsets.UTF_8);
        ProfileStorage.moveReplacing(tempFile, file);
    }

    /**
     * @throws IOException if the journal can't get read or is broken.
     */
    @NotNull
    static ProfileSwitchJournal read(@NotNull Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        try {
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            String fromId = json.get(KEY_FROM).getAsString();
            String toId = json.get(KEY_TO).getAsString();
            boolean keepActive = !json.has(KEY_KEEP_ACTIVE) || json.get(KEY_KEEP_ACTIVE).getAsBoolean();
            Phase phase = Phase.valueOf(json.get(KEY_PHASE).getAsString());
            boolean activeInUse = json.has(KEY_ACTIVE_IN_USE) && json.get(KEY_ACTIVE_IN_USE).getAsBoolean();
            if (!ProfileStorage.isValidProfileId(fromId) || !ProfileStorage.isValidProfileId(toId) || fromId.equals(toId)) {
                throw new IOException("The profile switch journal has invalid profile IDs: " + file);
            }
            return new ProfileSwitchJournal(fromId, toId, keepActive, phase, activeInUse);
        } catch (RuntimeException ex) {
            throw new IOException("Broken profile switch journal: " + file, ex);
        }
    }

    enum Phase {
        STASH,
        UNPACK,
        FINISH
    }

}
