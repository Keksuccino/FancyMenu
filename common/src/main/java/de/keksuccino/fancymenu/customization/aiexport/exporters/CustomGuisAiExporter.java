package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.customgui.CustomGui;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Exports custom GUIs and the screens they replace for AI chats.
 */
public final class CustomGuisAiExporter {

    private CustomGuisAiExporter() {
    }

    @NotNull
    public static AiExportDocument export() {
        List<CustomGui> guis = CustomGuiHandler.getGuis();
        Map<String, String> overriddenScreens = CustomGuiHandler.getOverriddenScreens();
        return AiExports.create("Custom GUIs", "custom_guis", writer -> {
            writer.heading(2, "Custom GUIs (" + guis.size() + ")");
            writer.paragraph("Custom GUIs are empty screens users make with FancyMenu and fill with layouts. Their identifier works like a screen identifier, "
                    + "so layouts target them by it and the \"Open Screen\" action opens them by it.");
            if (guis.isEmpty()) writer.paragraph("There are no custom GUIs.");
            for (CustomGui gui : guis) {
                writer.heading(3, "Custom GUI " + gui.identifier);
                writer.field("Identifier", gui.identifier);
                writer.field("Title", gui.title);
                writer.fieldText("Closes with ESC", AiExportText.yesNo(gui.allowEsc));
                writer.fieldText("Pauses the game in worlds", AiExportText.yesNo(gui.pauseGame));
                writer.fieldText("Shows the world in the background when in a world", AiExportText.yesNo(gui.worldBackground));
                writer.fieldText("Renders the vanilla blur and tint over the world background", AiExportText.yesNo(gui.worldBackgroundOverlay));
                writer.fieldText("Popup mode (shows on top of the screen it got opened from)", AiExportText.yesNo(gui.popupMode));
                if (gui.popupMode) writer.fieldText("Renders an overlay over the screen behind the popup", AiExportText.yesNo(gui.popupModeBackgroundOverlay));
                List<Layout> layouts = LayoutHandler.getAllLayoutsForScreenIdentifier(gui.identifier, false);
                writer.fieldText("Layouts for it", layouts.isEmpty() ? "none, so it is empty" : String.join(", ", layouts.stream().map(layout -> AiExportWriter.inlineCode(layout.getLayoutName()) + (layout.isEnabled() ? "" : " (disabled)")).toList()));
                List<String> replaces = overriddenScreens.entrySet().stream().filter(entry -> gui.identifier.equals(entry.getValue())).map(Map.Entry::getKey).toList();
                if (!replaces.isEmpty()) writer.field("Replaces these screens", String.join(", ", replaces));
            }
            writer.heading(2, "Replaced Screens (" + overriddenScreens.size() + ")");
            writer.paragraph("Screens of the game that open a custom GUI instead of themselves.");
            if (overriddenScreens.isEmpty()) writer.paragraph("No screens get replaced.");
            overriddenScreens.forEach((screen, gui) -> writer.field(screen, gui, CustomGuiHandler.guiExists(gui) ? "gets replaced by this custom GUI" : "the custom GUI that should replace it does not exist anymore"));
        });
    }

}
