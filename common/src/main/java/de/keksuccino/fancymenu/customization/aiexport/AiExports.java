package de.keksuccino.fancymenu.customization.aiexport;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.profile.ProfileHandler;
import de.keksuccino.fancymenu.customization.profile.ProfileMetadata;
import de.keksuccino.fancymenu.customization.profile.ProfileStorage;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.ScreenUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

/**
 * Creates AI exports on the client, with info about the game the client is in at the top.
 */
public final class AiExports {

    private AiExports() {
    }

    @NotNull
    public static AiExportDocument create(@NotNull String title, @NotNull String fileNameBase, @NotNull Consumer<AiExportWriter> body) {
        return AiExportDocument.create(title, fileNameBase, AiExports::writeClientFields, body);
    }

    private static void writeClientFields(@NotNull AiExportWriter writer) {
        Minecraft minecraft = Minecraft.getInstance();
        writer.field("Game language", minecraft.options.languageCode);
        Screen screen = ScreenUtils.getScreen();
        if (screen instanceof LayoutEditorScreen editor) {
            writer.fieldText("Current screen", "the FancyMenu layout editor, editing layout " + AiExportWriter.inlineCode(editor.layout.getLayoutName()));
        } else if (screen != null) {
            writer.field("Current screen identifier", AiExportText.safe(() -> ScreenIdentifierHandler.getIdentifierOfScreen(screen), "unknown"));
        } else {
            writer.fieldText("Current screen", "none (the player is in a world without an open screen)");
        }
        writer.field("Active FancyMenu profile", AiExportText.safe(AiExports::describeActiveProfile, "none"));
    }

    /**
     * Reads the metadata of the active profile without {@link ProfileHandler#getActiveProfile()}, because that one writes missing metadata to disk.
     */
    @Nullable
    static String describeActiveProfile() {
        ProfileStorage storage = ProfileHandler.getStorage();
        ProfileMetadata metadata = storage.readMetadata(storage.getRootDirectory(), "");
        if (metadata == null) return null;
        String title = metadata.getTitle();
        if (title.isBlank()) return metadata.getId();
        return metadata.getId().isBlank() ? title : (title + " (" + metadata.getId() + ")");
    }

}
