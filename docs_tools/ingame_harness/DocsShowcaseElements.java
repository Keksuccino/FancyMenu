package de.keksuccino.fancymenu.util.window.preview;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.background.backgrounds.MenuBackgrounds;
import de.keksuccino.fancymenu.customization.background.backgrounds.color.ColorMenuBackground;
import de.keksuccino.fancymenu.customization.background.backgrounds.image.ImageMenuBackground;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayRegistry;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.text.v2.TextElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;
import java.util.Map;

/**
 * Dev-only docs tool (see docs_tools/README.md). Builds the elements of a docs scene or editor showcase from JSON, with the defaults of new elements.<br>
 * Keys of an element: {@code type} (element type identifier, default {@code text_v2}), {@code x}, {@code y}, {@code w}, {@code h}
 * (GUI pixels, anchored top-left), {@code id} (instance identifier), {@code parent} (the {@code id} of the Scrollable Area it is in,
 * then {@code x}/{@code y} are relative to the area's content), {@code props} (saved properties of the element, as strings, applied last),
 * {@code actions} (the action script of elements that have one, like buttons: a list of {@code action} identifiers with their {@code value}),
 * {@code requirements} (the Loading Requirements, see {@link #buildRequirements}).<br>
 * Text elements also take {@code content}, {@code look} ({@code pixel}, {@code classic}, {@code neutral}), {@code background} and {@code scale}.
 */
final class DocsShowcaseElements {

    private DocsShowcaseElements() {
    }

    static void addTo(@NotNull Layout layout, @NotNull Iterable<JsonElement> elements) {
        for (JsonElement entry : elements) {
            layout.serializedElements.add(build(entry.getAsJsonObject()));
        }
    }

    /**
     * Changes vanilla elements of the screen, like the buttons and the logo of the title screen. Keys of an entry: {@code id} (the widget
     * identifier, like {@code mc_titlescreen_singleplayer_button} or {@code minecraft_logo_widget}), {@code hidden}, {@code x}, {@code y},
     * {@code w}, {@code h} (GUI pixels, anchored top-left; without them the element stays where the screen puts it) and {@code props}
     * (saved properties as strings, like {@code {"should_be_affected_by_decoration_overlays": "false"}}).
     */
    static void addVanillaTo(@NotNull Layout layout, @NotNull Iterable<JsonElement> vanillaElements) {
        for (JsonElement entry : vanillaElements) {
            JsonObject data = entry.getAsJsonObject();
            VanillaWidgetElement element = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
            element.setInstanceIdentifier(data.get("id").getAsString());
            if (data.has("hidden")) element.vanillaButtonHidden = data.get("hidden").getAsBoolean();
            if (data.has("x")) {
                element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
                element.posOffsetX = data.get("x").getAsInt();
                element.posOffsetY = data.get("y").getAsInt();
                element.baseWidth = data.get("w").getAsInt();
                element.baseHeight = data.get("h").getAsInt();
            }
            SerializedElement serialized = VanillaWidgetElementBuilder.INSTANCE.serializeElementInternal(element);
            if (serialized == null) throw new IllegalStateException("Can't serialize the vanilla element " + data.get("id").getAsString());
            if (data.has("props")) {
                for (Map.Entry<String, JsonElement> property : data.getAsJsonObject("props").entrySet()) {
                    serialized.putProperty(property.getKey(), property.getValue().getAsString());
                }
            }
            layout.serializedVanillaButtonElements.add(serialized);
        }
    }

    /**
     * Adds the menu background of a scene or config: {@code background_image} (a resource source, like
     * {@code /config/fancymenu/assets/docs_bg.png}, optionally with {@code background_parallax}) or {@code background_color} ({@code #RRGGBB}).
     */
    static void addBackgroundTo(@NotNull Layout layout, @NotNull JsonObject data) {
        if (data.has("background_image")) {
            ImageMenuBackground background = MenuBackgrounds.IMAGE.buildDefaultInstance();
            background.textureSupplier.set(ResourceSupplier.image(data.get("background_image").getAsString()));
            background.showBackground.set(true);
            // background_parallax: [x, y] intensities of the background's Parallax Effect
            if (data.has("background_parallax")) {
                background.parallaxEnabled.set(true);
                background.parallaxIntensityXString.set(data.getAsJsonArray("background_parallax").get(0).getAsFloat());
                background.parallaxIntensityYString.set(data.getAsJsonArray("background_parallax").get(1).getAsFloat());
            }
            layout.menuBackgrounds.add(background);
        } else if (data.has("background_color")) {
            ColorMenuBackground background = MenuBackgrounds.COLOR.buildDefaultInstance();
            background.color.set(data.get("background_color").getAsString());
            background.showBackground.set(true);
            layout.menuBackgrounds.add(background);
        }
    }

    /**
     * Turns on decoration overlays. Keys of an overlay: {@code type} (overlay identifier, like {@code snowfall}), {@code layers}
     * ({@code background_and_foreground}, {@code only_background}, {@code only_foreground}) and {@code props} (saved properties as strings).
     * The overlays get built the way a layout file loads them, so the properties go through their real deserialization.
     */
    static void addOverlaysTo(@NotNull Layout layout, @NotNull Iterable<JsonElement> overlays) {
        for (JsonElement entry : overlays) {
            JsonObject data = entry.getAsJsonObject();
            String type = data.get("type").getAsString();
            AbstractDecorationOverlayBuilder<?> builder = DecorationOverlayRegistry.getByIdentifier(type);
            if (builder == null) throw new IllegalStateException("No decoration overlay " + type);
            PropertyContainer serialized = builder._serialize(builder.buildDefaultInstance());
            // Serialized overlays refuse to overwrite their values, but the scene has to replace the defaults
            serialized.setInvulnerableProperties(false);
            serialized.putProperty("show_decoration_overlay", "true");
            if (data.has("layers")) serialized.putProperty("decoration_overlay_layers", data.get("layers").getAsString());
            if (data.has("props")) {
                for (Map.Entry<String, JsonElement> property : data.getAsJsonObject("props").entrySet()) {
                    serialized.putProperty(property.getKey(), property.getValue().getAsString());
                }
            }
            PropertyContainerSet set = new PropertyContainerSet("fancymenu_layout");
            set.putContainer(serialized);
            AbstractDecorationOverlay<?> overlay = builder._deserializeAll(set).getFirst();
            layout.decorationOverlays.add(Pair.of(builder, overlay));
        }
    }

    @NotNull
    static SerializedElement build(@NotNull JsonObject data) {
        String type = data.has("type") ? data.get("type").getAsString() : "text_v2";
        ElementBuilder<?, ?> builder = ElementRegistry.getBuilder(type);
        if (builder == null) throw new IllegalStateException("No element builder " + type);
        AbstractElement element = builder.buildDefaultInstance();
        if (element instanceof TextElement text) {
            if (data.has("content")) text.setSource(TextElement.SourceMode.DIRECT, data.get("content").getAsString());
            text.markdownSettings.applyLook(MarkdownLook.valueOf(data.has("look") ? data.get("look").getAsString().toUpperCase(Locale.ROOT) : "PIXEL"));
            if (data.has("background")) text.background.enabled.set(data.get("background").getAsBoolean());
            if (data.has("scale")) text.markdownSettings.textScale.set(data.get("scale").getAsFloat());
        }
        element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        element.posOffsetX = data.get("x").getAsInt();
        element.posOffsetY = data.get("y").getAsInt();
        element.baseWidth = data.get("w").getAsInt();
        element.baseHeight = data.get("h").getAsInt();
        if (data.has("id")) element.setInstanceIdentifier(data.get("id").getAsString());
        if (data.has("parent")) element.setParentContainerIdentifier(data.get("parent").getAsString());
        if (data.has("requirements")) element.requirementContainer = buildRequirements(data.getAsJsonArray("requirements"));
        if (data.has("actions")) {
            if (!(element instanceof ExecutableElement executable)) throw new IllegalStateException("Element type " + type + " has no action script");
            addScriptEntries(executable.getExecutableBlock(), data.getAsJsonArray("actions"));
        }
        SerializedElement serialized = builder.serializeElementInternal(element);
        if (data.has("props")) {
            for (Map.Entry<String, JsonElement> property : data.getAsJsonObject("props").entrySet()) {
                serialized.putProperty(property.getKey(), property.getValue().getAsString());
            }
        }
        return serialized;
    }

    /**
     * Adds the entries of an action script to the block: actions ({@code action} identifier and {@code value}) and IF statements ({@code if}:
     * requirements like {@link #buildRequirements}, {@code actions}: the script inside the IF, {@code else}: the script of an ELSE
     * appended to it, optional). Element action scripts (key {@code actions}) and listener scripts use them.
     */
    static void addScriptEntries(@NotNull AbstractExecutableBlock block, @NotNull Iterable<JsonElement> entries) {
        for (JsonElement entry : entries) {
            JsonObject data = entry.getAsJsonObject();
            if (data.has("if")) {
                IfExecutableBlock ifBlock = new IfExecutableBlock(buildRequirements(data.getAsJsonArray("if")));
                addScriptEntries(ifBlock, data.getAsJsonArray("actions"));
                if (data.has("else")) {
                    ElseExecutableBlock elseBlock = new ElseExecutableBlock();
                    addScriptEntries(elseBlock, data.getAsJsonArray("else"));
                    ifBlock.setAppendedBlock(elseBlock);
                }
                block.addExecutable(ifBlock);
            } else {
                Action action = ActionRegistry.getAction(data.get("action").getAsString());
                if (action == null) throw new IllegalStateException("No action " + data.get("action").getAsString());
                block.addExecutable(new ActionInstance(action, data.has("value") ? data.get("value").getAsString() : null));
            }
        }
    }

    /**
     * The Loading Requirements of an element (key {@code requirements}): each with {@code requirement} (its identifier), {@code value}
     * (if it has one) and {@code mode} ({@code normal}, the default, or {@code opposite}).
     */
    @NotNull
    static RequirementContainer buildRequirements(@NotNull Iterable<JsonElement> requirements) {
        RequirementContainer container = new RequirementContainer();
        for (JsonElement entry : requirements) {
            JsonObject data = entry.getAsJsonObject();
            Requirement requirement = RequirementRegistry.getRequirement(data.get("requirement").getAsString());
            if (requirement == null) throw new IllegalStateException("No requirement " + data.get("requirement").getAsString());
            RequirementInstance.RequirementMode mode = (data.has("mode") && data.get("mode").getAsString().equals("opposite"))
                    ? RequirementInstance.RequirementMode.IF_NOT
                    : RequirementInstance.RequirementMode.IF;
            container.addInstance(new RequirementInstance(requirement, data.has("value") ? data.get("value").getAsString() : null, mode, container));
        }
        return container;
    }

}
