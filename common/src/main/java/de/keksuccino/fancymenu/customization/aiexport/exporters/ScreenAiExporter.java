package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportReferences;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports the screen that is open right now for AI chats, with its widgets and what FancyMenu does on it.
 */
public final class ScreenAiExporter {

    private ScreenAiExporter() {
    }

    @NotNull
    public static AiExportDocument export(@Nullable Screen screen) {
        String identifier = (screen != null) ? AiExportText.safe(() -> ScreenIdentifierHandler.getIdentifierOfScreen(screen), "unknown") : "none";
        return AiExports.create("Current Screen " + identifier, "screen_" + identifier, writer -> {

            AiExportReferences references = new AiExportReferences();
            writer.heading(2, "Current Screen");
            if (screen == null) {
                writer.paragraph("No screen is open.");
                return;
            }
            writer.paragraph("The screen that was open when the user made the export. Layouts and actions use the screen identifier to target this screen.");

            Minecraft minecraft = Minecraft.getInstance();
            writer.field("Screen identifier", identifier);
            writer.field("Screen class", screen.getClass().getName());
            writer.field("Title", AiExportText.safe(() -> screen.getTitle().getString(), ""));
            writer.fieldText("Size", screen.width + "x" + screen.height + " (scaled GUI pixels), window " + minecraft.getWindow().getWidth() + "x" + minecraft.getWindow().getHeight() + " (real pixels), GUI scale " + WindowHandler.getGuiScale());
            writer.fieldText("FancyMenu can customize this screen", AiExportText.yesNo(!ScreenCustomization.isScreenBlacklisted(screen)) + (ScreenCustomization.isScreenBlacklisted(screen) ? " (FancyMenu blacklists it, so layouts don't apply to it)" : ""));
            writer.fieldText("Customization is turned on for this screen", AiExportText.yesNo(ScreenCustomization.isCustomizationEnabledForScreen(screen)));
            if (screen instanceof CustomGuiBaseScreen customGui) {
                writer.fieldText("Custom GUI", "yes, this is the custom GUI " + AiExportWriter.inlineCode(customGui.getIdentifier()) + ", which the user made with FancyMenu");
            }
            String overriddenWith = CustomGuiHandler.getOverriddenScreens().get(identifier);
            if (overriddenWith != null) writer.field("Gets replaced by the custom GUI", overriddenWith);

            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(screen);
            writeLayouts(writer, identifier, layer, references);
            writeWidgets(writer, layer);
            writeElements(writer, layer, references);
            references.write(writer);

        });
    }

    private static void writeLayouts(@NotNull AiExportWriter writer, @NotNull String identifier, @Nullable ScreenCustomizationLayer layer, @NotNull AiExportReferences references) {
        List<Layout> layouts = new ArrayList<>(LayoutHandler.getAllLayoutsForScreenIdentifier(identifier, false));
        layouts.addAll(LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true));
        writer.heading(3, "Layouts for This Screen (" + layouts.size() + ")");
        writer.paragraph("Layouts that target this screen, and universal layouts, which target all screens. Use the \"Export Layout for AI\" option of a layout to get all its details.");
        if (layouts.isEmpty()) {
            writer.paragraph("There are no layouts for this screen.");
            return;
        }
        for (Layout layout : layouts) {
            boolean active = (layer != null) && layer.activeLayouts.contains(layout);
            writer.bullet(AiExportWriter.inlineCode(layout.getLayoutName()) + " (" + (layout.isUniversalLayout() ? "universal layout" : "normal layout") + ")");
            writer.nested(() -> {
                writer.fieldText("Enabled", AiExportText.yesNo(layout.isEnabled()));
                writer.fieldText("Active on this screen right now", AiExportText.yesNo(active));
                if (!layout.layoutWideRequirementContainer.isEmpty()) {
                    RequirementAiExporter.writeContainer(writer, "Layout-wide requirements", layout.layoutWideRequirementContainer, references, true);
                }
                if (layout.isUniversalLayout() && !layout.universalLayoutMenuWhitelist.isEmpty()) writer.field("Only applies to these screens (whitelist)", String.join(", ", layout.universalLayoutMenuWhitelist));
                if (layout.isUniversalLayout() && !layout.universalLayoutMenuBlacklist.isEmpty()) writer.field("Does not apply to these screens (blacklist)", String.join(", ", layout.universalLayoutMenuBlacklist));
            });
        }
    }

    private static void writeWidgets(@NotNull AiExportWriter writer, @Nullable ScreenCustomizationLayer layer) {
        List<WidgetMeta> widgets = (layer != null) ? layer.cachedScreenWidgetMetas : List.of();
        writer.heading(3, "Vanilla Widgets of This Screen (" + widgets.size() + ")");
        writer.paragraph("The widgets the screen has on its own, like its buttons. Layouts can customize them by their widget identifier, and actions or requirements can find them by their locator (`screen_identifier:widget_identifier`).");
        if (widgets.isEmpty()) {
            writer.paragraph("FancyMenu found no widgets on this screen.");
            return;
        }
        for (WidgetMeta meta : widgets) {
            AbstractWidget widget = meta.getWidget();
            writer.bullet(AiExportWriter.inlineCode(AiExportText.safe(() -> meta.label.getString(), "")) + " (" + widget.getClass().getSimpleName() + ")");
            writer.nested(() -> {
                writer.field("Widget identifier", meta.getIdentifier());
                writer.field("Locator", AiExportText.safe(meta::getLocator, ""));
                writer.fieldText("Original position and size", "x " + meta.x + ", y " + meta.y + ", " + meta.width + "x" + meta.height + " (before layouts change it)");
                writer.fieldText("Visible", AiExportText.yesNo(widget.visible));
                writer.fieldText("Active (can be used)", AiExportText.yesNo(widget.active));
            });
        }
    }

    private static void writeElements(@NotNull AiExportWriter writer, @Nullable ScreenCustomizationLayer layer, @NotNull AiExportReferences references) {
        List<AbstractElement> elements = (layer != null) ? layer.allElements : List.of();
        writer.heading(3, "FancyMenu Elements on This Screen (" + elements.size() + ")");
        writer.paragraph("The elements of the active layouts, like they are right now. \"Shown\" tells if the element was rendered in the last frame.");
        for (AbstractElement element : elements) {
            references.addElementType(element.getBuilder());
            String name = AiExportText.safe(() -> AiExportText.plain(element.getDisplayName()), element.getBuilder().getIdentifier());
            writer.bullet(AiExportWriter.inlineCode(name) + " (type " + AiExportWriter.inlineCode(element.getBuilder().getIdentifier()) + ", identifier " + AiExportWriter.inlineCode(element.getInstanceIdentifier()) + ")");
            writer.nested(() -> {
                Layout parentLayout = element.getParentLayout();
                if (parentLayout != null) writer.field("From layout", parentLayout.getLayoutName());
                writer.fieldText("Shown", AiExportText.yesNo(element.lastTickShouldRender));
                if (!element.requirementContainer.isEmpty()) {
                    RequirementAiExporter.writeContainer(writer, "Loading requirements", element.requirementContainer, references, true);
                }
            });
        }
    }

}
