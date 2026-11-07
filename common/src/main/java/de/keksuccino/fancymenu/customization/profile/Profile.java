package de.keksuccino.fancymenu.customization.profile;

import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;

/**
 * A snapshot of a profile at the time it got listed.
 *
 * @param directory the directory that holds the content of the profile. For the active profile, this is the FancyMenu config directory.
 */
public record Profile(@NotNull ProfileMetadata metadata, @NotNull Path directory, boolean active) {

    @NotNull
    public String getId() {
        return this.metadata.getId();
    }

    @NotNull
    public Path getIconFile() {
        return this.directory.resolve(ProfileStorage.ICON_FILE_NAME);
    }

}
