package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.aiexport.AiExportDefaults;
import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportReferences;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Exports layouts and the elements of layouts for AI chats.
 */
public final class LayoutAiExporter {

    private static final String LAYOUT_INTRO = "Layouts customize a screen of the game (or every screen, for universal layouts). They can add elements, change vanilla widgets, set backgrounds and run actions when the screen opens or closes. "
            + "Only settings that differ from their defaults are listed.";
    private static final Set<String> BACKGROUND_KEYS_SHOWN_ELSEWHERE = Set.of("instance_identifier", "background_type", "show_background");
    private static final Set<String> DECORATION_OVERLAY_KEYS_SHOWN_ELSEWHERE = Set.of("instance_identifier", "overlay_type", "show_decoration_overlay");

    private LayoutAiExporter() {
    }

    /**
     * Exports a layout like it is saved.
     */
    @NotNull
    public static AiExportDocument exportLayout(@NotNull Layout layout) {
        return export(layout, layout.serializedElements, layout.isUniversalLayout() ? List.of() : layout.serializedVanillaButtonElements, null, buildActiveLayerLookup());
    }

    /**
     * Exports the layout of the layout editor, with all changes that are not saved yet.
     */
    @NotNull
    public static AiExportDocument exportEditorLayout(@NotNull LayoutEditorScreen editor) {
        List<SerializedElement> normal = new ArrayList<>();
        List<SerializedElement> vanilla = new ArrayList<>();
        for (AbstractEditorElement<?, ?> editorElement : editor.getAllElements()) {
            SerializedElement serialized = editorElement.element.getBuilder().serializeElementInternal(editorElement.element);
            if (serialized == null) continue;
            if (editorElement.element instanceof VanillaWidgetElement) {
                vanilla.add(serialized);
            } else {
                normal.add(serialized);
            }
        }
        return export(editor.layout, normal, vanilla, editor, buildEditorLookup(editor));
    }

    /**
     * Exports elements of the layout editor, like the ones the user selected.
     */
    @NotNull
    public static AiExportDocument exportEditorElements(@NotNull LayoutEditorScreen editor, @NotNull List<? extends AbstractEditorElement<?, ?>> elements) {
        List<SerializedElement> serialized = new ArrayList<>();
        for (AbstractEditorElement<?, ?> editorElement : elements) {
            SerializedElement s = editorElement.element.getBuilder().serializeElementInternal(editorElement.element);
            if (s != null) serialized.add(s);
        }
        List<SerializedElement> allElements = new ArrayList<>();
        for (AbstractEditorElement<?, ?> editorElement : editor.getAllElements()) {
            SerializedElement s = editorElement.element.getBuilder().serializeElementInternal(editorElement.element);
            if (s != null) allElements.add(s);
        }
        String title = (serialized.size() == 1) ? "Element" : (serialized.size() + " Elements");
        return AiExports.create(title, "elements_" + editor.layout.getLayoutName(), writer -> {
            AiExportReferences references = new AiExportReferences();
            writer.heading(2, "Elements of the Layout " + editor.layout.getLayoutName());
            writer.paragraph(ElementAiExporter.ELEMENTS_INTRO + " The elements are from the layout editor, so they can have changes that are not saved yet. Loading requirement results got checked at the moment of the export.");
            writer.field("Layout", editor.layout.getLayoutName());
            writer.fieldText("Layout applies to", describeTarget(editor.layout));
            ElementAiExporter.writeElements(writer, serialized, ElementAiExporter.buildNames(allElements), new AiExportDefaults(), references, buildEditorLookup(editor), false, "Element");
            references.write(writer);
        });
    }

    @NotNull
    private static AiExportDocument export(@NotNull Layout layout, @NotNull List<SerializedElement> elements, @NotNull List<SerializedElement> vanillaWidgets, @Nullable LayoutEditorScreen editor, @NotNull Function<String, Object> runtimeLookup) {
        return AiExports.create("Layout " + layout.getLayoutName(), "layout_" + layout.getLayoutName(), writer -> {

            AiExportReferences references = new AiExportReferences();
            AiExportDefaults defaults = new AiExportDefaults();

            writer.heading(2, "Layout " + layout.getLayoutName());
            writer.paragraph(LAYOUT_INTRO + ((editor != null) ? " This export is from the layout editor, so it can have changes that are not saved yet." : ""));
            writeSettings(writer, layout, editor, references);

            List<MenuBackground<?>> backgrounds = layout.menuBackgrounds.stream().filter(background -> Boolean.TRUE.equals(background.showBackground.get())).toList();
            if (!backgrounds.isEmpty()) {
                writer.heading(2, "Menu Backgrounds (" + backgrounds.size() + ")");
                backgrounds.forEach(background -> writeMenuBackground(writer, background, defaults, references));
            }

            List<Pair<AbstractDecorationOverlayBuilder<?>, AbstractDecorationOverlay<?>>> overlays = layout.decorationOverlays.stream().filter(pair -> Boolean.TRUE.equals(pair.getSecond().showOverlay.get())).toList();
            if (!overlays.isEmpty()) {
                writer.heading(2, "Decoration Overlays (" + overlays.size() + ")");
                overlays.forEach(overlay -> writeDecorationOverlay(writer, overlay.getFirst(), overlay.getSecond(), defaults, references));
            }

            List<SerializedElement> allElements = new ArrayList<>(elements);
            allElements.addAll(vanillaWidgets);
            Map<String, String> names = ElementAiExporter.buildNames(allElements);

            writer.heading(2, "Elements (" + elements.size() + ")");
            writer.paragraph(ElementAiExporter.ELEMENTS_INTRO + " Elements get rendered in this order, so later elements are on top of earlier ones.");
            if (elements.isEmpty()) writer.paragraph("The layout has no elements.");
            ElementAiExporter.writeElements(writer, elements, names, defaults, references, runtimeLookup, false, "Element");

            if (!vanillaWidgets.isEmpty()) {
                writer.heading(2, "Vanilla Widget Customizations");
                writer.paragraph("Changes the layout makes to the widgets the screen already has, like its original buttons. Their identifiers are the widget identifiers of the screen.");
                int skipped = ElementAiExporter.writeElements(writer, vanillaWidgets, names, defaults, references, runtimeLookup, true, "Vanilla Widget");
                if (skipped > 0) writer.paragraph(skipped + " more vanilla widgets of the screen are not listed, because the layout does not change them.");
            }

            references.write(writer);

        });
    }

    /**
     * Exports a menu background of the layout editor.
     */
    @NotNull
    public static AiExportDocument exportMenuBackground(@NotNull MenuBackground<?> background) {
        String name = AiExportText.safe(() -> AiExportText.plain(background.builder.getDisplayName()), background.builder.getIdentifier());
        return AiExports.create("Menu Background " + name, "background_" + background.builder.getIdentifier(), writer -> {
            AiExportReferences references = new AiExportReferences();
            writer.paragraph("A menu background of a layout, from the layout editor, so it can have changes that are not saved yet. Only properties that differ from the defaults of its type are listed.");
            writeMenuBackground(writer, background, new AiExportDefaults(), references);
            references.write(writer);
        });
    }

    /**
     * Exports a decoration overlay of the layout editor.
     */
    @NotNull
    public static AiExportDocument exportDecorationOverlay(@NotNull LayoutEditorScreen editor, @NotNull AbstractDecorationOverlay<?> overlay) {
        // Overlays don't know their type, so it comes from the layout that holds them
        AbstractDecorationOverlayBuilder<?> builder = editor.layout.decorationOverlays.stream().filter(pair -> pair.getSecond() == overlay).map(Pair::getFirst).findFirst().orElseThrow(() -> new IllegalStateException("The decoration overlay is not part of the layout of the editor!"));
        String name = AiExportText.safe(() -> AiExportText.plain(builder.getDisplayName()), builder.getIdentifier());
        return AiExports.create("Decoration Overlay " + name, "decoration_overlay_" + builder.getIdentifier(), writer -> {
            AiExportReferences references = new AiExportReferences();
            writer.paragraph("A decoration overlay of a layout, from the layout editor, so it can have changes that are not saved yet. Only properties that differ from the defaults of its type are listed.");
            writeDecorationOverlay(writer, builder, overlay, new AiExportDefaults(), references);
            references.write(writer);
        });
    }

    private static void writeMenuBackground(@NotNull AiExportWriter writer, @NotNull MenuBackground<?> background, @NotNull AiExportDefaults defaults, @NotNull AiExportReferences references) {
        references.addBackgroundType(background.builder);
        writer.heading(3, "Background: " + AiExportText.safe(() -> AiExportText.plain(background.builder.getDisplayName()), background.builder.getIdentifier()) + " (" + AiExportWriter.inlineCode(background.builder.getIdentifier()) + ")");
        writer.fieldText("Shown", AiExportText.yesNo(Boolean.TRUE.equals(background.showBackground.get())));
        PropertyContainer serialized = background.builder._serializeBackground(background);
        if (serialized == null) return;
        SerializedObjectAiExporter.write(writer, SerializedObjectAiExporter.analyze(serialized, defaults.forBackground(background.builder), BACKGROUND_KEYS_SHOWN_ELSEWHERE), references, background);
    }

    private static void writeDecorationOverlay(@NotNull AiExportWriter writer, @NotNull AbstractDecorationOverlayBuilder<?> builder, @NotNull AbstractDecorationOverlay<?> overlay, @NotNull AiExportDefaults defaults, @NotNull AiExportReferences references) {
        references.addDecorationOverlayType(builder);
        writer.heading(3, "Decoration Overlay: " + AiExportText.safe(() -> AiExportText.plain(builder.getDisplayName()), builder.getIdentifier()) + " (" + AiExportWriter.inlineCode(builder.getIdentifier()) + ")");
        writer.fieldText("Shown", AiExportText.yesNo(Boolean.TRUE.equals(overlay.showOverlay.get())));
        SerializedObjectAiExporter.write(writer, SerializedObjectAiExporter.analyze(builder._serialize(overlay), defaults.forDecorationOverlay(builder), DECORATION_OVERLAY_KEYS_SHOWN_ELSEWHERE), references, overlay);
    }

    private static void writeSettings(@NotNull AiExportWriter writer, @NotNull Layout layout, @Nullable LayoutEditorScreen editor, @NotNull AiExportReferences references) {

        Layout defaults = new Layout();

        writer.field("Layout name", layout.getLayoutName());
        writer.field("Layout file", (layout.layoutFile != null) ? AiExportText.path(layout.layoutFile) : null, (layout.layoutFile == null) ? "not saved yet" : null);
        writer.fieldText("Applies to", describeTarget(layout));
        writer.fieldText("Enabled", AiExportText.yesNo(layout.isEnabled()));
        if (editor != null) writer.fieldText("Has unsaved changes in the editor", AiExportText.yesNo(editor.unsavedChanges));
        if (layout.layoutIndex != defaults.layoutIndex) writer.field("Layout index", layout.layoutIndex, "when more layouts are active on a screen, they get applied from low to high index");
        if (layout.randomMode) {
            writer.fieldText("Random mode", "on: only one random layout of the random group " + AiExportWriter.inlineCode(layout.randomGroup) + " is active at a time"
                    + (layout.randomOnlyFirstTime ? ", and the random pick only happens the first time the screen opens" : ""));
        }
        if (layout.isUniversalLayout()) {
            if (!layout.universalLayoutMenuWhitelist.isEmpty()) writer.field("Only applies to these screens (whitelist)", String.join(", ", layout.universalLayoutMenuWhitelist));
            if (!layout.universalLayoutMenuBlacklist.isEmpty()) writer.field("Does not apply to these screens (blacklist)", String.join(", ", layout.universalLayoutMenuBlacklist));
        }
        if (layout.renderElementsBehindVanilla) writer.fieldText("Custom elements render behind vanilla widgets", "yes");
        if (layout.forcedScale != defaults.forcedScale) writer.field("Forced GUI scale", layout.forcedScale);
        if ((layout.autoScalingWidth != 0) && (layout.autoScalingHeight != 0)) writer.field("Auto scaling base size", layout.autoScalingWidth + "x" + layout.autoScalingHeight);
        if (layout.customMenuTitle != null) writer.field("Custom menu title", layout.customMenuTitle);
        writeAudio(writer, "Audio when the screen opens", layout.openAudio, layout.openAudioVolume, layout.openAudioSoundChannel.getName());
        writeAudio(writer, "Audio when the screen closes", layout.closeAudio, layout.closeAudioVolume, layout.closeAudioSoundChannel.getName());
        if (layout.preserveBackgroundAspectRatio != defaults.preserveBackgroundAspectRatio) writer.fieldText("Keep aspect ratio of background", AiExportText.yesNo(layout.preserveBackgroundAspectRatio));
        if (layout.showScreenBackgroundOverlayOnCustomBackground != defaults.showScreenBackgroundOverlayOnCustomBackground) writer.fieldText("Show the dark screen overlay on custom backgrounds", AiExportText.yesNo(layout.showScreenBackgroundOverlayOnCustomBackground));
        if (layout.applyVanillaBackgroundBlur != defaults.applyVanillaBackgroundBlur) writer.fieldText("Apply vanilla background blur", AiExportText.yesNo(layout.applyVanillaBackgroundBlur));
        writeTexture(writer, "Scroll list header texture", layout.scrollListHeaderTexture);
        writeTexture(writer, "Scroll list footer texture", layout.scrollListFooterTexture);
        if (layout.repeatScrollListHeaderTexture != defaults.repeatScrollListHeaderTexture) writer.fieldText("Repeat scroll list header texture", AiExportText.yesNo(layout.repeatScrollListHeaderTexture));
        if (layout.repeatScrollListFooterTexture != defaults.repeatScrollListFooterTexture) writer.fieldText("Repeat scroll list footer texture", AiExportText.yesNo(layout.repeatScrollListFooterTexture));
        if (layout.preserveScrollListHeaderFooterAspectRatio != defaults.preserveScrollListHeaderFooterAspectRatio) writer.fieldText("Keep aspect ratio of scroll list header and footer", AiExportText.yesNo(layout.preserveScrollListHeaderFooterAspectRatio));
        if (layout.renderScrollListHeaderShadow != defaults.renderScrollListHeaderShadow) writer.fieldText("Scroll list header shadow", AiExportText.yesNo(layout.renderScrollListHeaderShadow));
        if (layout.renderScrollListFooterShadow != defaults.renderScrollListFooterShadow) writer.fieldText("Scroll list footer shadow", AiExportText.yesNo(layout.renderScrollListFooterShadow));

        if (!layout.layoutWideRequirementContainer.isEmpty()) {
            RequirementAiExporter.writeContainer(writer, "Layout-wide requirements (the whole layout is only active while they are met)", layout.layoutWideRequirementContainer, references, true);
        }
        writeScript(writer, "Actions when the screen opens", layout.openScreenExecutableBlocks, references);
        writeScript(writer, "Actions when the screen closes", layout.closeScreenExecutableBlocks, references);

        List<Layout.LayerGroup> groups = layout.layerGroups.stream().filter(Objects::nonNull).toList();
        if (!groups.isEmpty()) {
            writer.bullet("**Layer groups** (only organize elements in the editor):");
            writer.nested(() -> groups.forEach(group -> writer.field((group.name != null) ? group.name : "Unnamed group", String.join(", ", group.elementInstanceIdentifiers), "identifiers of the elements in it")));
        }

    }

    private static void writeScript(@NotNull AiExportWriter writer, @NotNull String label, @NotNull List<GenericExecutableBlock> scripts, @NotNull AiExportReferences references) {
        if (scripts.isEmpty() || scripts.get(0).getExecutables().isEmpty()) return;
        writer.bullet("**" + label + ":**");
        writer.nested(() -> ActionScriptAiExporter.writeScript(writer, scripts.get(0), references));
    }

    private static void writeAudio(@NotNull AiExportWriter writer, @NotNull String label, @Nullable ResourceSupplier<?> audio, float volume, @NotNull String channel) {
        if (audio == null) return;
        writer.field(label, audio.getSourceWithPrefix(), "volume " + volume + ", sound channel " + channel);
    }

    private static void writeTexture(@NotNull AiExportWriter writer, @NotNull String label, @Nullable ResourceSupplier<?> texture) {
        if (texture == null) return;
        writer.field(label, texture.getSourceWithPrefix());
    }

    @NotNull
    static String describeTarget(@NotNull Layout layout) {
        if (layout.isUniversalLayout()) return "all screens (universal layout)";
        return "the screen " + AiExportWriter.inlineCode(String.valueOf(layout.screenIdentifier));
    }

    /**
     * Finds live elements of the screen that is open right now, so elements of layouts active on that screen get their runtime info.
     */
    @NotNull
    private static Function<String, Object> buildActiveLayerLookup() {
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
        if (layer == null) return identifier -> null;
        Map<String, Object> elements = new HashMap<>();
        for (AbstractElement element : layer.allElements) {
            elements.put(element.getInstanceIdentifier(), element);
        }
        return elements::get;
    }

    @NotNull
    private static Function<String, Object> buildEditorLookup(@NotNull LayoutEditorScreen editor) {
        return identifier -> {
            AbstractEditorElement<?, ?> editorElement = editor.getElementByInstanceIdentifier(identifier);
            return (editorElement != null) ? editorElement.element : null;
        };
    }

}
