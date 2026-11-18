package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Exports error and warning messages of FancyMenu for AI chats.
 */
public final class DialogAiExporter {

    private DialogAiExporter() {
    }

    @NotNull
    public static AiExportDocument export(@NotNull Component message, @NotNull MessageDialogStyle style) {
        return AiExports.create("FancyMenu " + style.getName() + " message", style.getName() + "_message", writer -> {
            writer.heading(2, "Message");
            writer.paragraph("FancyMenu showed this " + style.getName() + " message to the user. The environment info above tells which screen was open when it happened. "
                    + "Messages of add-ons can have parts in the language of the user, when the add-on translated them before showing the message.");
            writer.codeBlock(null, AiExportText.plain(message));
        });
    }

}
