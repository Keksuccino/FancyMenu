package de.keksuccino.fancymenu.customization.aiexport;

import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Builds AI exports and hands them to the user, as clipboard content or as file.
 */
public final class AiExportActions {

    private static final Logger LOGGER = LogManager.getLogger();

    private AiExportActions() {
    }

    /**
     * @return If the export is in the clipboard now. Shows an error dialog if not.
     */
    public static boolean copyToClipboard(@NotNull Supplier<AiExportDocument> exportBuilder) {
        AiExportDocument document = build(exportBuilder);
        if (document == null) return false;
        Minecraft.getInstance().keyboardHandler.setClipboard(document.text());
        return true;
    }

    /**
     * Saves the export as file and opens the folder of the file, so users can drag it into their chat.
     *
     * @return If the export got saved. Shows an error dialog if not.
     */
    public static boolean saveToFile(@NotNull Supplier<AiExportDocument> exportBuilder) {
        AiExportDocument document = build(exportBuilder);
        if (document == null) return false;
        try {
            File file = AiExportFiles.save(document);
            FileUtils.openFile(Objects.requireNonNull(file.getParentFile()));
            return true;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to save an AI export as file!", ex);
            showFailure(ex);
        }
        return false;
    }

    @Nullable
    private static AiExportDocument build(@NotNull Supplier<AiExportDocument> exportBuilder) {
        try {
            return AiExportLanguage.supplyInEnglish(exportBuilder);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to build an AI export!", ex);
            showFailure(ex);
        }
        return null;
    }

    private static void showFailure(@NotNull Exception ex) {
        String reason = (ex.getMessage() != null) ? ex.getMessage() : ex.getClass().getSimpleName();
        Dialogs.openMessage(Component.translatable("fancymenu.ai_export.failed", reason), MessageDialogStyle.ERROR);
    }

}
