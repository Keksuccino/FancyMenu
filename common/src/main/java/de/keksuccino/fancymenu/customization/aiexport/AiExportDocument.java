package de.keksuccino.fancymenu.customization.aiexport;

import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

/**
 * A finished AI export.
 *
 * @param title        What got exported, like "All Variables".
 * @param fileNameBase The start of the file name when the export gets saved as file, like "variables".
 * @param text         The Markdown text of the export.
 */
public record AiExportDocument(@NotNull String title, @NotNull String fileNameBase, @NotNull String text) {

    private static final String INTRO = "This file was made with the \"Export for AI\" feature of FancyMenu, a Minecraft mod for customizing menus and other screens of the game. "
            + "It describes a part of the FancyMenu setup of a user, so an AI assistant can help them with it. "
            + "It is always in English, no matter which language the user plays the game in. "
            + "Text in `code` is a raw value exactly like FancyMenu stores it. "
            + "Paths that start with `~` are in the home folder of the user, and `<user>` stands for their account name.";

    /**
     * Builds an export with the standard intro and environment info at the top.
     *
     * @param headerFields Adds more fields to the environment section, like info only the client knows about.
     * @param body         Writes the actual content of the export.
     */
    @NotNull
    public static AiExportDocument create(@NotNull String title, @NotNull String fileNameBase, @NotNull Consumer<AiExportWriter> headerFields, @NotNull Consumer<AiExportWriter> body) {
        AiExportWriter writer = new AiExportWriter();
        writer.heading(1, "FancyMenu AI Export: " + title);
        writer.paragraph(INTRO);
        writer.heading(2, "Environment");
        AiExportEnvironment.writeFields(writer);
        headerFields.accept(writer);
        body.accept(writer);
        return new AiExportDocument(title, fileNameBase, writer.build());
    }

}
