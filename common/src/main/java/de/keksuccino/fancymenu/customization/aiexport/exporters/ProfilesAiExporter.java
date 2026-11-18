package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.profile.ProfileHandler;
import de.keksuccino.fancymenu.customization.profile.ProfileMetadata;
import de.keksuccino.fancymenu.customization.profile.ProfileStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports FancyMenu's profiles for AI chats. Only reads the profiles, unlike {@link ProfileHandler#getProfiles()}, which repairs and writes their metadata.
 */
public final class ProfilesAiExporter {

    private static final int MAX_DESCRIPTION_LENGTH = 1500;

    private ProfilesAiExporter() {
    }

    @NotNull
    public static AiExportDocument export() {
        return AiExports.create("Profiles", "profiles", writer -> {
            ProfileStorage storage = ProfileHandler.getStorage();
            List<Path> storedDirectories = storage.listStoredProfileDirectories();
            writer.heading(2, "FancyMenu Profiles (" + (storedDirectories.size() + 1) + ")");
            writer.paragraph("Profiles are complete FancyMenu setups (layouts, settings, listeners, ...) users can switch between. "
                    + "The active profile is the content of the FancyMenu config folder, the others are stored in the profiles folder.");
            writer.heading(3, "Active Profile");
            writeProfile(writer, storage.readMetadata(storage.getRootDirectory(), ""), null);
            writer.heading(3, "Other Profiles (" + storedDirectories.size() + ")");
            if (storedDirectories.isEmpty()) writer.paragraph("There are no other profiles.");
            for (Path directory : storedDirectories) {
                String id = directory.getFileName().toString();
                writeProfile(writer, storage.readMetadata(directory, id), id);
            }
        });
    }

    private static void writeProfile(@NotNull AiExportWriter writer, @Nullable ProfileMetadata metadata, @Nullable String directoryId) {
        if (metadata == null) {
            writer.bullet((directoryId != null) ? (AiExportWriter.inlineCode(directoryId) + " (no profile info saved)") : "No profile info saved yet, so the user never used profiles.");
            return;
        }
        writer.bullet(AiExportWriter.inlineCode(metadata.getTitle().isBlank() ? metadata.getId() : metadata.getTitle()));
        writer.nested(() -> {
            writer.field("Identifier", metadata.getId());
            if (!metadata.getVersion().isBlank()) writer.field("Version", metadata.getVersion());
            if (!metadata.getAuthors().isEmpty()) writer.field("Authors", metadata.getAuthorsAsString());
            if (metadata.getFancyMenuVersion() != null) writer.field("Last saved with FancyMenu version", metadata.getFancyMenuVersion());
            String description = metadata.getDescription();
            if (!description.isBlank()) {
                writer.field("Description", (description.length() > MAX_DESCRIPTION_LENGTH) ? (description.substring(0, MAX_DESCRIPTION_LENGTH) + " [...]") : description);
            }
            if (metadata.getIssueTrackerUrl() != null) writer.field("Issue tracker", metadata.getIssueTrackerUrl());
            metadata.getLinks().forEach(link -> writer.field("Link (" + link.type().getSerializedName() + ")", link.url()));
        });
    }

}
