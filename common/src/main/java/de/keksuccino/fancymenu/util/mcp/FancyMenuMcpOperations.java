package de.keksuccino.fancymenu.util.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundRegistry;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayRegistry;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.ListenerHandler;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.customization.listener.ListenerRegistry;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerHandler;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerInstance;
import de.keksuccino.fancymenu.customization.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.customization.panorama.LocalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.screen.ScreenInstanceFactory;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.UniversalScreenIdentifierRegistry;
import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.SerializationHelper;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class FancyMenuMcpOperations {

    private FancyMenuMcpOperations() {
    }

    static @NotNull JsonObject intro() {
        JsonObject out = new JsonObject();
        out.addProperty("name", "FancyMenu MCP Server");
        out.addProperty("mandatory_first_tool", "fancymenu_intro");
        out.addProperty("summary", "FancyMenu is like Figma for real Minecraft menus: you design live, functional menus that directly run in-game.");
        JsonArray instructions = new JsonArray();
        instructions.add("Always call this intro tool first in every new MCP session.");
        instructions.add("Prefer direct Streamable HTTP MCP transport via the configured MCP port (for example: http://127.0.0.1:<port>/mcp).");
        instructions.add("When editing layouts, open the Layout Editor for the target layout/screen and keep updates live so the user can watch changes.");
        instructions.add("When editing action scripts for layouts/listeners/schedulers, open the Action Script Editor and keep it live-updated.");
        instructions.add("Use screenshot capture to visually validate current UI state before and after changes.");
        instructions.add("Prefer full serialization tools for complete control: layouts, scripts, requirements, listeners, schedulers, variables, and fancy-properties.");
        out.add("instructions", instructions);
        JsonArray capabilities = new JsonArray();
        capabilities.add("Layout editor automation: add/move/edit/delete elements, backgrounds, decoration overlays.");
        capabilities.add("Action scripting automation with live Action Editor updates.");
        capabilities.add("Full CRUD for listeners, schedulers, variables.");
        capabilities.add("Registry introspection for actions/placeholders/requirements/elements/backgrounds/overlays/listeners.");
        capabilities.add("Screen customization controls and layout-editor option controls.");
        capabilities.add("Panorama/slideshow discovery for resource-aware background design.");
        capabilities.add("Screenshot capture as PNG base64.");
        capabilities.add("FancyMenu property format parse/stringify for full-state editing.");
        out.add("capabilities", capabilities);
        return out;
    }

    static @NotNull JsonObject serverStatus() {
        JsonObject out = new JsonObject();
        out.addProperty("enabled_option", FancyMenu.getOptions().mcpServerEnabled.getValue());
        out.addProperty("configured_port", FancyMenu.getOptions().mcpServerPort.getValue());
        boolean running = FancyMenuMcpManager.isRunning();
        int boundPort = FancyMenuMcpManager.getBoundPort();
        out.addProperty("running", running);
        out.addProperty("bound_port", boundPort);
        if (running && boundPort > 0) {
            out.addProperty("http_url", "http://127.0.0.1:" + boundPort + "/mcp");
            out.addProperty("tcp_host", "127.0.0.1");
            out.addProperty("tcp_port", boundPort);
        }
        return out;
    }

    static @NotNull JsonObject currentScreen() {
        JsonObject out = new JsonObject();
        Screen current = Minecraft.getInstance().screen;
        if (current == null) {
            out.addProperty("has_screen", false);
            return out;
        }
        out.addProperty("has_screen", true);
        out.addProperty("screen_class", current.getClass().getName());
        out.addProperty("screen_identifier", ScreenIdentifierHandler.getIdentifierOfScreen(current));
        out.addProperty("screen_title", current.getTitle().getString());
        out.addProperty("is_layout_editor", current instanceof LayoutEditorScreen);
        return out;
    }

    static @NotNull JsonObject customizationState() {
        JsonObject out = new JsonObject();
        out.addProperty("customization_engine_enabled", ScreenCustomization.isScreenCustomizationEnabled());
        Screen current = Minecraft.getInstance().screen;
        if (current != null) {
            out.addProperty("has_current_screen", true);
            out.addProperty("current_screen_class", current.getClass().getName());
            out.addProperty("current_screen_identifier", ScreenIdentifierHandler.getIdentifierOfScreen(current));
            out.addProperty("current_screen_customization_enabled", ScreenCustomization.isCustomizationEnabledForScreen(current));
        } else {
            out.addProperty("has_current_screen", false);
        }
        PropertyContainerSet set = readCustomizableScreensSet();
        JsonArray screenClasses = new JsonArray();
        set.getContainers().forEach(container -> screenClasses.add(container.getType()));
        out.add("customizable_screen_classes", screenClasses);
        out.addProperty("customizable_screen_class_count", screenClasses.size());
        return out;
    }

    static @NotNull JsonObject setCustomizationEngineEnabled(@NotNull JsonObject args) {
        boolean enabled = getBoolean(args, "enabled", ScreenCustomization.isScreenCustomizationEnabled());
        ScreenCustomization.setScreenCustomizationEnabled(enabled);
        if (getBoolean(args, "reinit_current_screen", true)) {
            ScreenCustomization.reInitCurrentScreen();
        }
        JsonObject out = new JsonObject();
        out.addProperty("enabled", ScreenCustomization.isScreenCustomizationEnabled());
        return out;
    }

    static @NotNull JsonObject setCurrentScreenCustomizationEnabled(@NotNull JsonObject args) {
        Screen current = Minecraft.getInstance().screen;
        if (current == null) {
            throw new IllegalStateException("No screen is currently open.");
        }
        boolean enabled = getBoolean(args, "enabled", true);
        ScreenCustomization.setCustomizationForScreenEnabled(current, enabled);
        if (getBoolean(args, "reinit_current_screen", true)) {
            ScreenCustomization.reInitCurrentScreen();
        }
        JsonObject out = new JsonObject();
        out.addProperty("screen_class", current.getClass().getName());
        out.addProperty("screen_identifier", ScreenIdentifierHandler.getIdentifierOfScreen(current));
        out.addProperty("customization_enabled", ScreenCustomization.isCustomizationEnabledForScreen(current));
        return out;
    }

    static @NotNull JsonObject setScreenClassCustomizationEnabled(@NotNull JsonObject args) {
        String screenClass = requireString(args, "screen_class");
        boolean enabled = getBoolean(args, "enabled", true);
        PropertyContainerSet set = readCustomizableScreensSet();
        String normalizedClass = ScreenIdentifierHandler.tryFixInvalidIdentifierWithNonUniversal(screenClass);
        if (enabled) {
            boolean exists = !set.getContainersOfType(normalizedClass).isEmpty();
            if (!exists) {
                set.putContainer(new PropertyContainer(normalizedClass));
            }
        } else {
            set.getContainers().removeIf(container -> container.getType().equals(normalizedClass));
        }
        writeCustomizableScreensSet(set);
        Screen current = Minecraft.getInstance().screen;
        if (current != null && current.getClass().getName().equals(normalizedClass) && getBoolean(args, "reinit_current_screen", true)) {
            ScreenCustomization.reInitCurrentScreen();
        }
        JsonObject out = new JsonObject();
        out.addProperty("screen_class", normalizedClass);
        out.addProperty("customizable", enabled);
        out.addProperty("count", readCustomizableScreensSet().getContainers().size());
        return out;
    }

    static @NotNull JsonObject clearCustomizableScreenClasses() {
        ScreenCustomization.disableCustomizationForAllScreens();
        JsonObject out = new JsonObject();
        out.addProperty("cleared", true);
        return out;
    }

    static @NotNull JsonObject reloadFancyMenu() {
        ScreenCustomization.reloadFancyMenu();
        JsonObject out = new JsonObject();
        out.addProperty("reloaded", true);
        return out;
    }

    static @NotNull JsonObject reinitCurrentScreen() {
        ScreenCustomization.reInitCurrentScreen();
        JsonObject out = new JsonObject();
        out.addProperty("reinitialized", true);
        return out;
    }

    static @NotNull JsonObject getEditorOptions() {
        JsonObject out = new JsonObject();
        out.addProperty("show_layout_editor_grid", FancyMenu.getOptions().showLayoutEditorGrid.getValue());
        out.addProperty("layout_editor_grid_size", FancyMenu.getOptions().layoutEditorGridSize.getValue());
        out.addProperty("layout_editor_grid_snapping", FancyMenu.getOptions().layoutEditorGridSnapping.getValue());
        out.addProperty("layout_editor_grid_snapping_strength", FancyMenu.getOptions().layoutEditorGridSnappingStrength.getValue());
        out.addProperty("show_all_anchor_overlay_connections", FancyMenu.getOptions().showAllAnchorOverlayConnections.getValue());
        out.addProperty("anchor_overlay_change_anchor_on_area_hover", FancyMenu.getOptions().anchorOverlayChangeAnchorOnAreaHover.getValue());
        out.addProperty("anchor_overlay_change_anchor_on_element_hover", FancyMenu.getOptions().anchorOverlayChangeAnchorOnElementHover.getValue());
        out.addProperty("invert_anchor_overlay_color", FancyMenu.getOptions().invertAnchorOverlayColor.getValue());
        out.addProperty("anchor_overlay_opacity_percentage_normal", FancyMenu.getOptions().anchorOverlayOpacityPercentageNormal.getValue());
        out.addProperty("anchor_overlay_opacity_percentage_busy", FancyMenu.getOptions().anchorOverlayOpacityPercentageBusy.getValue());
        out.addProperty("anchor_overlay_color_base_override", FancyMenu.getOptions().anchorOverlayColorBaseOverride.getValue());
        out.addProperty("anchor_overlay_color_border_override", FancyMenu.getOptions().anchorOverlayColorBorderOverride.getValue());
        out.addProperty("anchor_overlay_visibility_mode", FancyMenu.getOptions().anchorOverlayVisibilityMode.getValue());
        out.addProperty("anchor_overlay_hover_charging_time_seconds", FancyMenu.getOptions().anchorOverlayHoverChargingTimeSeconds.getValue());
        out.addProperty("enable_element_rotation_controls", FancyMenu.getOptions().enableElementRotationControls.getValue());
        out.addProperty("enable_element_tilting_controls", FancyMenu.getOptions().enableElementTiltingControls.getValue());
        return out;
    }

    static @NotNull JsonObject setEditorOptions(@NotNull JsonObject args) {
        if (args.has("show_layout_editor_grid")) {
            FancyMenu.getOptions().showLayoutEditorGrid.setValue(getBoolean(args, "show_layout_editor_grid", FancyMenu.getOptions().showLayoutEditorGrid.getValue()));
        }
        if (args.has("layout_editor_grid_size")) {
            FancyMenu.getOptions().layoutEditorGridSize.setValue(getInt(args, "layout_editor_grid_size", FancyMenu.getOptions().layoutEditorGridSize.getValue()));
        }
        if (args.has("layout_editor_grid_snapping")) {
            FancyMenu.getOptions().layoutEditorGridSnapping.setValue(getBoolean(args, "layout_editor_grid_snapping", FancyMenu.getOptions().layoutEditorGridSnapping.getValue()));
        }
        if (args.has("layout_editor_grid_snapping_strength")) {
            FancyMenu.getOptions().layoutEditorGridSnappingStrength.setValue(getFloat(args, "layout_editor_grid_snapping_strength", FancyMenu.getOptions().layoutEditorGridSnappingStrength.getValue()));
        }
        if (args.has("show_all_anchor_overlay_connections")) {
            FancyMenu.getOptions().showAllAnchorOverlayConnections.setValue(getBoolean(args, "show_all_anchor_overlay_connections", FancyMenu.getOptions().showAllAnchorOverlayConnections.getValue()));
        }
        if (args.has("anchor_overlay_change_anchor_on_area_hover")) {
            FancyMenu.getOptions().anchorOverlayChangeAnchorOnAreaHover.setValue(getBoolean(args, "anchor_overlay_change_anchor_on_area_hover", FancyMenu.getOptions().anchorOverlayChangeAnchorOnAreaHover.getValue()));
        }
        if (args.has("anchor_overlay_change_anchor_on_element_hover")) {
            FancyMenu.getOptions().anchorOverlayChangeAnchorOnElementHover.setValue(getBoolean(args, "anchor_overlay_change_anchor_on_element_hover", FancyMenu.getOptions().anchorOverlayChangeAnchorOnElementHover.getValue()));
        }
        if (args.has("invert_anchor_overlay_color")) {
            FancyMenu.getOptions().invertAnchorOverlayColor.setValue(getBoolean(args, "invert_anchor_overlay_color", FancyMenu.getOptions().invertAnchorOverlayColor.getValue()));
        }
        if (args.has("anchor_overlay_opacity_percentage_normal")) {
            FancyMenu.getOptions().anchorOverlayOpacityPercentageNormal.setValue(getFloat(args, "anchor_overlay_opacity_percentage_normal", FancyMenu.getOptions().anchorOverlayOpacityPercentageNormal.getValue()));
        }
        if (args.has("anchor_overlay_opacity_percentage_busy")) {
            FancyMenu.getOptions().anchorOverlayOpacityPercentageBusy.setValue(getFloat(args, "anchor_overlay_opacity_percentage_busy", FancyMenu.getOptions().anchorOverlayOpacityPercentageBusy.getValue()));
        }
        if (args.has("anchor_overlay_color_base_override")) {
            FancyMenu.getOptions().anchorOverlayColorBaseOverride.setValue(Objects.requireNonNullElse(getNullableString(args, "anchor_overlay_color_base_override"), ""));
        }
        if (args.has("anchor_overlay_color_border_override")) {
            FancyMenu.getOptions().anchorOverlayColorBorderOverride.setValue(Objects.requireNonNullElse(getNullableString(args, "anchor_overlay_color_border_override"), ""));
        }
        if (args.has("anchor_overlay_visibility_mode")) {
            String value = getNullableString(args, "anchor_overlay_visibility_mode");
            if (value != null && !value.isBlank()) {
                FancyMenu.getOptions().anchorOverlayVisibilityMode.setValue(value);
            }
        }
        if (args.has("anchor_overlay_hover_charging_time_seconds")) {
            FancyMenu.getOptions().anchorOverlayHoverChargingTimeSeconds.setValue(getDouble(args, "anchor_overlay_hover_charging_time_seconds", FancyMenu.getOptions().anchorOverlayHoverChargingTimeSeconds.getValue()));
        }
        if (args.has("enable_element_rotation_controls")) {
            FancyMenu.getOptions().enableElementRotationControls.setValue(getBoolean(args, "enable_element_rotation_controls", FancyMenu.getOptions().enableElementRotationControls.getValue()));
        }
        if (args.has("enable_element_tilting_controls")) {
            FancyMenu.getOptions().enableElementTiltingControls.setValue(getBoolean(args, "enable_element_tilting_controls", FancyMenu.getOptions().enableElementTiltingControls.getValue()));
        }
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.add("options", getEditorOptions());
        return out;
    }

    static @NotNull JsonObject captureScreenshot() throws Exception {
        NativeImage image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
        try {
            byte[] bytes = image.asByteArray();
            JsonObject out = new JsonObject();
            out.addProperty("format", "png");
            out.addProperty("mime_type", "image/png");
            out.addProperty("width", image.getWidth());
            out.addProperty("height", image.getHeight());
            out.addProperty("byte_size", bytes.length);
            out.addProperty("base64", Base64.getEncoder().encodeToString(bytes));
            return out;
        } finally {
            image.close();
        }
    }

    static @NotNull JsonObject listActions() {
        JsonArray array = new JsonArray();
        List<Action> actions = ActionRegistry.getActions();
        actions.sort(Comparator.comparing(Action::getIdentifier, String.CASE_INSENSITIVE_ORDER));
        for (Action action : actions) {
            JsonObject item = new JsonObject();
            item.addProperty("identifier", action.getIdentifier());
            item.addProperty("display_name", action.getDisplayName().getString());
            item.addProperty("description", action.getDescription().getString());
            item.addProperty("has_value", action.hasValue());
            item.addProperty("deprecated", action.isDeprecated());
            item.addProperty("value_display_name", action.getValueDisplayName() != null ? action.getValueDisplayName().getString() : "");
            item.addProperty("value_preset", Objects.requireNonNullElse(action.getValuePreset(), ""));
            array.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("actions", array);
        out.addProperty("count", array.size());
        return out;
    }

    static @NotNull JsonObject listPlaceholders() {
        JsonArray array = new JsonArray();
        List<Placeholder> placeholders = PlaceholderRegistry.getPlaceholders();
        placeholders.sort(Comparator.comparing(Placeholder::getIdentifier, String.CASE_INSENSITIVE_ORDER));
        for (Placeholder placeholder : placeholders) {
            JsonObject item = new JsonObject();
            item.addProperty("identifier", placeholder.getIdentifier());
            item.addProperty("display_name", placeholder.getDisplayName());
            item.addProperty("category", Objects.requireNonNullElse(placeholder.getCategory(), ""));
            JsonArray values = new JsonArray();
            List<String> valueNames = placeholder.getValueNames();
            if (valueNames != null) {
                valueNames.forEach(values::add);
            }
            item.add("value_names", values);
            JsonArray descriptions = new JsonArray();
            List<String> desc = placeholder.getDescription();
            if (desc != null) {
                desc.forEach(descriptions::add);
            }
            item.add("description_lines", descriptions);
            array.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("placeholders", array);
        out.addProperty("count", array.size());
        return out;
    }

    static @NotNull JsonObject listRequirements() {
        JsonArray array = new JsonArray();
        List<Requirement> requirements = RequirementRegistry.getRequirements();
        requirements.sort(Comparator.comparing(Requirement::getIdentifier, String.CASE_INSENSITIVE_ORDER));
        for (Requirement requirement : requirements) {
            JsonObject item = new JsonObject();
            item.addProperty("identifier", requirement.getIdentifier());
            item.addProperty("display_name", requirement.getDisplayName().getString());
            item.addProperty("description", requirement.getDescription() != null ? requirement.getDescription().getString() : "");
            item.addProperty("category", Objects.requireNonNullElse(requirement.getCategory(), ""));
            item.addProperty("has_value", requirement.hasValue());
            item.addProperty("value_display_name", requirement.getValueDisplayName() != null ? requirement.getValueDisplayName().getString() : "");
            item.addProperty("value_preset", Objects.requireNonNullElse(requirement.getValuePreset(), ""));
            array.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("requirements", array);
        out.addProperty("count", array.size());
        return out;
    }

    static @NotNull JsonObject listListenerTypes() {
        JsonArray array = new JsonArray();
        List<AbstractListener> listeners = ListenerRegistry.getListeners();
        listeners.sort(Comparator.comparing(AbstractListener::getIdentifier, String.CASE_INSENSITIVE_ORDER));
        for (AbstractListener listener : listeners) {
            JsonObject item = new JsonObject();
            item.addProperty("identifier", listener.getIdentifier());
            item.addProperty("display_name", listener.getDisplayName().getString());
            JsonArray desc = new JsonArray();
            listener.getDescription().forEach(component -> desc.add(component.getString()));
            item.add("description_lines", desc);
            JsonArray variables = new JsonArray();
            listener.getCustomVariables().forEach(variable -> {
                JsonObject variableJson = new JsonObject();
                variableJson.addProperty("name", variable.name());
                variables.add(variableJson);
            });
            item.add("custom_variables", variables);
            array.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("listener_types", array);
        out.addProperty("count", array.size());
        return out;
    }

    static @NotNull JsonObject listElements() {
        JsonArray array = new JsonArray();
        List<ElementBuilder<?, ?>> builders = ElementRegistry.getBuilders();
        builders.sort(Comparator.comparing(ElementBuilder::getIdentifier, String.CASE_INSENSITIVE_ORDER));
        for (ElementBuilder<?, ?> builder : builders) {
            JsonObject item = new JsonObject();
            item.addProperty("identifier", builder.getIdentifier());
            item.addProperty("display_name", builder.getDisplayName(null).getString());
            Component[] description = builder.getDescription(null);
            JsonArray lines = new JsonArray();
            if (description != null) {
                for (Component component : description) {
                    lines.add(component.getString());
                }
            }
            item.add("description_lines", lines);
            item.addProperty("deprecated", builder.isDeprecated());
            array.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("elements", array);
        out.addProperty("count", array.size());
        return out;
    }

    static @NotNull JsonObject listBackgrounds() {
        JsonArray array = new JsonArray();
        List<MenuBackgroundBuilder<?>> builders = MenuBackgroundRegistry.getBuilders();
        builders.sort(Comparator.comparing(MenuBackgroundBuilder::getIdentifier, String.CASE_INSENSITIVE_ORDER));
        for (MenuBackgroundBuilder<?> builder : builders) {
            JsonObject item = new JsonObject();
            item.addProperty("identifier", builder.getIdentifier());
            item.addProperty("display_name", builder.getDisplayName().getString());
            item.addProperty("description", builder.getDescription() != null ? builder.getDescription().getString() : "");
            item.addProperty("deprecated", builder.isDeprecated());
            array.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("backgrounds", array);
        out.addProperty("count", array.size());
        return out;
    }

    static @NotNull JsonObject listDecorationOverlays() {
        JsonArray array = new JsonArray();
        List<AbstractDecorationOverlayBuilder<?>> builders = DecorationOverlayRegistry.getAll();
        builders.sort(Comparator.comparing(AbstractDecorationOverlayBuilder::getIdentifier, String.CASE_INSENSITIVE_ORDER));
        for (AbstractDecorationOverlayBuilder<?> builder : builders) {
            JsonObject item = new JsonObject();
            item.addProperty("identifier", builder.getIdentifier());
            item.addProperty("display_name", builder.getDisplayName().getString());
            item.addProperty("description", builder.getDescription() != null ? builder.getDescription().getString() : "");
            array.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("decoration_overlays", array);
        out.addProperty("count", array.size());
        return out;
    }

    static @NotNull JsonObject listPanoramas() {
        JsonArray panoramas = new JsonArray();
        List<LocalTexturePanoramaRenderer> renderers = PanoramaHandler.getPanoramas();
        renderers.sort(Comparator.comparing(renderer -> Objects.requireNonNullElse(renderer.getName(), ""), String.CASE_INSENSITIVE_ORDER));
        for (LocalTexturePanoramaRenderer renderer : renderers) {
            JsonObject item = new JsonObject();
            item.addProperty("name", Objects.requireNonNullElse(renderer.getName(), ""));
            item.addProperty("properties_file", renderer.propertiesFile != null ? renderer.propertiesFile.getAbsolutePath() : "");
            item.addProperty("panorama_image_dir", renderer.panoramaImageDir != null ? renderer.panoramaImageDir.getAbsolutePath() : "");
            item.addProperty("has_overlay_texture", renderer.overlayTextureSupplier != null);
            panoramas.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("panoramas", panoramas);
        out.addProperty("count", panoramas.size());
        return out;
    }

    static @NotNull JsonObject listSlideshows() {
        JsonArray slideshows = new JsonArray();
        List<ExternalTextureSlideshowRenderer> renderers = SlideshowHandler.getSlideshows();
        renderers.sort(Comparator.comparing(renderer -> Objects.requireNonNullElse(renderer.getName(), ""), String.CASE_INSENSITIVE_ORDER));
        for (ExternalTextureSlideshowRenderer renderer : renderers) {
            JsonObject item = new JsonObject();
            item.addProperty("name", Objects.requireNonNullElse(renderer.getName(), ""));
            item.addProperty("directory", Objects.requireNonNullElse(renderer.dir, ""));
            item.addProperty("ready", renderer.isReady());
            item.addProperty("image_count", renderer.getImageCount());
            item.addProperty("image_width", renderer.getImageWidth());
            item.addProperty("image_height", renderer.getImageHeight());
            item.addProperty("has_overlay_texture", renderer.overlayTexture != null);
            slideshows.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("slideshows", slideshows);
        out.addProperty("count", slideshows.size());
        return out;
    }

    static @NotNull JsonObject listScreenIdentifiers() {
        JsonArray identifiers = new JsonArray();
        UniversalScreenIdentifierRegistry.getUniversalIdentifiers().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(identifiers::add);
        JsonObject out = new JsonObject();
        out.add("universal_identifiers", identifiers);
        out.addProperty("count", identifiers.size());
        return out;
    }

    static @NotNull JsonObject listLayouts() {
        JsonArray layouts = new JsonArray();
        for (Layout layout : LayoutHandler.getAllLayouts()) {
            layouts.add(layoutToJson(layout, false));
        }
        JsonObject out = new JsonObject();
        out.add("layouts", layouts);
        out.addProperty("count", layouts.size());
        return out;
    }

    static @NotNull JsonObject getLayout(@NotNull JsonObject args) {
        Layout layout = requireLayout(args);
        JsonObject out = new JsonObject();
        out.add("layout", layoutToJson(layout, true));
        return out;
    }

    static @NotNull JsonObject createLayout(@NotNull JsonObject args) {
        boolean universal = getBoolean(args, "universal", false);
        String screenIdentifier = getString(args, "screen_identifier", null);
        Layout layout;
        if (universal || screenIdentifier == null || screenIdentifier.isBlank()) {
            layout = Layout.buildUniversal();
        } else {
            layout = Layout.buildForScreen(screenIdentifier);
        }
        String filenameBase = getString(args, "file_name", null);
        if (filenameBase == null || filenameBase.isBlank()) {
            filenameBase = layout.isUniversalLayout() ? "universal_layout_mcp" : layout.screenIdentifier + "_layout_mcp";
        }
        filenameBase = filenameBase.toLowerCase(Locale.ROOT).replace(".txt", "");
        String available = FileUtils.generateAvailableFilename(LayoutHandler.LAYOUT_DIR.getAbsolutePath(), filenameBase, "txt");
        layout.layoutFile = new File(LayoutHandler.LAYOUT_DIR, available);
        layout.updateLastEditedTime();
        layout.saveToFileIfPossible();
        LayoutHandler.reloadLayouts();
        Layout saved = findLayoutByFile(layout.layoutFile);
        if (saved == null) {
            saved = layout;
        }
        if (getBoolean(args, "open_editor", true)) {
            openOrRefreshLayoutEditor(saved, true);
        }
        JsonObject out = new JsonObject();
        out.add("layout", layoutToJson(saved, true));
        return out;
    }

    static @NotNull JsonObject setLayout(@NotNull JsonObject args) {
        Layout existing = resolveLayout(args);
        Layout parsed = parseLayoutFromArgs(args, existing != null ? existing.layoutFile : null);
        if (existing != null && parsed.layoutFile == null) {
            parsed.layoutFile = existing.layoutFile;
        }
        if (parsed.layoutFile == null) {
            String filenameBase = getString(args, "file_name", "mcp_layout");
            String available = FileUtils.generateAvailableFilename(LayoutHandler.LAYOUT_DIR.getAbsolutePath(), filenameBase.toLowerCase(Locale.ROOT), "txt");
            parsed.layoutFile = new File(LayoutHandler.LAYOUT_DIR, available);
        }
        parsed.updateLastEditedTime();
        parsed.saveToFileIfPossible();
        LayoutHandler.reloadLayouts();
        Layout saved = findLayoutByFile(parsed.layoutFile);
        if (saved == null) {
            saved = parsed;
        }
        if (getBoolean(args, "open_editor", true)) {
            openOrRefreshLayoutEditor(saved, true);
        }
        JsonObject out = new JsonObject();
        out.add("layout", layoutToJson(saved, true));
        return out;
    }

    static @NotNull JsonObject updateLayoutMeta(@NotNull JsonObject args) {
        Layout layout = requireLayout(args);
        if (args.has("enabled")) {
            layout.setEnabled(getBoolean(args, "enabled", layout.isEnabled()), false);
        }
        if (args.has("layout_index")) {
            layout.layoutIndex = getInt(args, "layout_index", layout.layoutIndex);
        }
        if (args.has("render_elements_behind_vanilla")) {
            layout.renderElementsBehindVanilla = getBoolean(args, "render_elements_behind_vanilla", layout.renderElementsBehindVanilla);
        }
        if (args.has("random_mode")) {
            layout.randomMode = getBoolean(args, "random_mode", layout.randomMode);
        }
        if (args.has("random_group")) {
            layout.randomGroup = requireString(args, "random_group");
        }
        if (args.has("random_only_first_time")) {
            layout.randomOnlyFirstTime = getBoolean(args, "random_only_first_time", layout.randomOnlyFirstTime);
        }
        if (args.has("screen_identifier")) {
            layout.setScreenIdentifier(requireString(args, "screen_identifier"));
        }
        if (args.has("universal_layout_whitelist")) {
            layout.universalLayoutMenuWhitelist = getStringArray(args, "universal_layout_whitelist");
        }
        if (args.has("universal_layout_blacklist")) {
            layout.universalLayoutMenuBlacklist = getStringArray(args, "universal_layout_blacklist");
        }
        if (args.has("layout_wide_requirement_container")) {
            layout.layoutWideRequirementContainer = FancyMenuMcpSerialization.requirementContainerFromJson(requireJsonObject(args, "layout_wide_requirement_container"));
        }
        if (args.has("custom_menu_title")) {
            layout.customMenuTitle = getNullableString(args, "custom_menu_title");
        }
        if (args.has("forced_scale")) {
            layout.forcedScale = getFloat(args, "forced_scale", layout.forcedScale);
        }
        if (args.has("auto_scaling_width")) {
            layout.autoScalingWidth = getInt(args, "auto_scaling_width", layout.autoScalingWidth);
        }
        if (args.has("auto_scaling_height")) {
            layout.autoScalingHeight = getInt(args, "auto_scaling_height", layout.autoScalingHeight);
        }
        if (args.has("preserve_background_aspect_ratio")) {
            layout.preserveBackgroundAspectRatio = getBoolean(args, "preserve_background_aspect_ratio", layout.preserveBackgroundAspectRatio);
        }
        if (args.has("open_audio_source")) {
            layout.openAudio = SerializationHelper.INSTANCE.deserializeAudioResourceSupplier(getNullableString(args, "open_audio_source"));
        }
        if (args.has("close_audio_source")) {
            layout.closeAudio = SerializationHelper.INSTANCE.deserializeAudioResourceSupplier(getNullableString(args, "close_audio_source"));
        }
        if (args.has("preserve_scroll_list_header_footer_aspect_ratio")) {
            layout.preserveScrollListHeaderFooterAspectRatio = getBoolean(args, "preserve_scroll_list_header_footer_aspect_ratio", layout.preserveScrollListHeaderFooterAspectRatio);
        }
        if (args.has("repeat_scroll_list_header_texture")) {
            layout.repeatScrollListHeaderTexture = getBoolean(args, "repeat_scroll_list_header_texture", layout.repeatScrollListHeaderTexture);
        }
        if (args.has("repeat_scroll_list_footer_texture")) {
            layout.repeatScrollListFooterTexture = getBoolean(args, "repeat_scroll_list_footer_texture", layout.repeatScrollListFooterTexture);
        }
        if (args.has("scroll_list_header_texture_source")) {
            layout.scrollListHeaderTexture = SerializationHelper.INSTANCE.deserializeImageResourceSupplier(getNullableString(args, "scroll_list_header_texture_source"));
        }
        if (args.has("scroll_list_footer_texture_source")) {
            layout.scrollListFooterTexture = SerializationHelper.INSTANCE.deserializeImageResourceSupplier(getNullableString(args, "scroll_list_footer_texture_source"));
        }
        if (args.has("render_scroll_list_header_shadow")) {
            layout.renderScrollListHeaderShadow = getBoolean(args, "render_scroll_list_header_shadow", layout.renderScrollListHeaderShadow);
        }
        if (args.has("render_scroll_list_footer_shadow")) {
            layout.renderScrollListFooterShadow = getBoolean(args, "render_scroll_list_footer_shadow", layout.renderScrollListFooterShadow);
        }
        if (args.has("show_scroll_list_header_footer_preview_in_editor")) {
            layout.showScrollListHeaderFooterPreviewInEditor = getBoolean(args, "show_scroll_list_header_footer_preview_in_editor", layout.showScrollListHeaderFooterPreviewInEditor);
        }
        if (args.has("show_screen_background_overlay_on_custom_background")) {
            layout.showScreenBackgroundOverlayOnCustomBackground = getBoolean(args, "show_screen_background_overlay_on_custom_background", layout.showScreenBackgroundOverlayOnCustomBackground);
        }
        if (args.has("apply_vanilla_background_blur")) {
            layout.applyVanillaBackgroundBlur = getBoolean(args, "apply_vanilla_background_blur", layout.applyVanillaBackgroundBlur);
        }

        layout.updateLastEditedTime();
        boolean saved = layout.saveToFileIfPossible();
        LayoutHandler.reloadLayouts();
        Layout updated = findLayoutByFile(layout.layoutFile);
        if (updated == null) {
            updated = layout;
        }
        if (getBoolean(args, "open_editor", false)) {
            openOrRefreshLayoutEditor(updated, true);
        }

        JsonObject out = new JsonObject();
        out.addProperty("saved", saved);
        out.add("layout", layoutToJson(updated, true));
        return out;
    }

    static @NotNull JsonObject deleteLayout(@NotNull JsonObject args) {
        Layout layout = requireLayout(args);
        String deletedFile = layout.layoutFile != null ? layout.layoutFile.getAbsolutePath() : "";
        layout.delete(false);
        LayoutHandler.reloadLayouts();
        JsonObject out = new JsonObject();
        out.addProperty("deleted", true);
        out.addProperty("deleted_file", deletedFile);
        return out;
    }

    static @NotNull JsonObject saveLayout(@NotNull JsonObject args) throws Exception {
        LayoutEditorScreen editor = LayoutEditorScreen.getCurrentInstance();
        if (editor != null && getBoolean(args, "use_open_editor", true)) {
            editor.saveLayout();
            LayoutHandler.reloadLayouts();
            JsonObject out = new JsonObject();
            out.addProperty("saved_from_editor", true);
            return out;
        }
        Layout layout = requireLayout(args);
        layout.updateLastEditedTime();
        boolean saved = layout.saveToFileIfPossible();
        LayoutHandler.reloadLayouts();
        JsonObject out = new JsonObject();
        out.addProperty("saved", saved);
        out.addProperty("saved_from_editor", false);
        return out;
    }

    static @NotNull JsonObject openLayoutEditor(@NotNull JsonObject args) {
        Layout layout = requireLayout(args);
        openOrRefreshLayoutEditor(layout, true);
        JsonObject out = new JsonObject();
        out.addProperty("opened", true);
        out.add("layout", layoutToJson(layout, false));
        return out;
    }

    static @NotNull JsonObject closeLayoutEditor() {
        LayoutEditorScreen editor = LayoutEditorScreen.getCurrentInstance();
        if (editor != null) {
            editor.closeEditor();
        }
        JsonObject out = new JsonObject();
        out.addProperty("closed", true);
        return out;
    }

    static @NotNull JsonObject getEditorState() throws Exception {
        LayoutEditorScreen editor = LayoutEditorScreen.getCurrentInstance();
        JsonObject out = new JsonObject();
        if (editor == null) {
            out.addProperty("editor_open", false);
            return out;
        }
        syncEditorLayout(editor);
        out.addProperty("editor_open", true);
        out.addProperty("unsaved_changes", editor.unsavedChanges);
        out.addProperty("layout_target_identifier", editor.layoutTargetScreen != null ? ScreenIdentifierHandler.getIdentifierOfScreen(editor.layoutTargetScreen) : "");
        out.add("layout", layoutToJson(editor.layout, true));
        out.addProperty("selected_element_count", editor.getSelectedElements().size());
        out.addProperty("total_element_count", editor.getAllElements().size());
        return out;
    }

    static @NotNull JsonObject editorPoll(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        JsonObject out = new JsonObject();
        out.addProperty("editor_open", true);
        out.addProperty("editor_fingerprint", computeEditorFingerprint(editor));
        out.addProperty("unsaved_changes", editor.unsavedChanges);
        out.addProperty("normal_count", editor.normalEditorElements.size());
        out.addProperty("vanilla_count", editor.vanillaWidgetEditorElements.size());
        JsonArray selected = new JsonArray();
        editor.getSelectedElements().forEach(element -> selected.add(element.element.getInstanceIdentifier()));
        out.add("selected_element_identifiers", selected);
        out.addProperty("selected_count", selected.size());
        return out;
    }

    static @NotNull JsonObject editorListElements(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        JsonArray normal = new JsonArray();
        for (int i = 0; i < editor.normalEditorElements.size(); i++) {
            AbstractEditorElement<?, ?> element = editor.normalEditorElements.get(i);
            normal.add(editorElementToJson(element, i, false));
        }
        JsonArray vanilla = new JsonArray();
        for (int i = 0; i < editor.vanillaWidgetEditorElements.size(); i++) {
            AbstractEditorElement<?, ?> element = editor.vanillaWidgetEditorElements.get(i);
            vanilla.add(editorElementToJson(element, i, true));
        }

        JsonObject out = new JsonObject();
        out.add("normal_elements", normal);
        out.add("vanilla_widget_elements", vanilla);
        out.addProperty("normal_count", normal.size());
        out.addProperty("vanilla_count", vanilla.size());
        out.addProperty("selected_count", editor.getSelectedElements().size());
        out.addProperty("unsaved_changes", editor.unsavedChanges);
        out.addProperty("editor_fingerprint", computeEditorFingerprint(editor));
        return out;
    }

    static @NotNull JsonObject editorSelectElements(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        String mode = getString(args, "mode", "set");
        if (mode == null) {
            mode = "set";
        }
        mode = mode.toLowerCase(Locale.ROOT);
        List<String> identifiers = getStringArray(args, "element_identifiers");
        if ("clear".equals(mode)) {
            editor.deselectAllElements();
        } else if ("all".equals(mode)) {
            editor.selectAllElements();
        } else {
            if ("set".equals(mode)) {
                editor.deselectAllElements();
            }
            if ("set".equals(mode) || "add".equals(mode) || "remove".equals(mode)) {
                for (String id : identifiers) {
                    AbstractEditorElement<?, ?> element = editor.getElementByInstanceIdentifier(id);
                    if (element == null) {
                        continue;
                    }
                    if ("remove".equals(mode)) {
                        element.setSelected(false);
                    } else {
                        element.setSelected(true);
                    }
                }
            } else {
                throw new IllegalArgumentException("Unknown selection mode: " + mode + ". Expected set/add/remove/clear/all.");
            }
        }
        JsonArray selected = new JsonArray();
        editor.getSelectedElements().forEach(element -> selected.add(element.element.getInstanceIdentifier()));
        JsonObject out = new JsonObject();
        out.addProperty("mode", mode);
        out.add("selected_element_identifiers", selected);
        out.addProperty("selected_count", selected.size());
        return out;
    }

    static @NotNull JsonObject editorGetVisualLayers(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        JsonArray backgrounds = new JsonArray();
        for (int i = 0; i < editor.layout.menuBackgrounds.size(); i++) {
            MenuBackground<?> background = editor.layout.menuBackgrounds.get(i);
            JsonObject item = new JsonObject();
            item.addProperty("index", i);
            item.addProperty("identifier", background.builder.getIdentifier());
            item.add("serialized_container", FancyMenuMcpSerialization.toJson(background.builder._serializeBackground(background)));
            backgrounds.add(item);
        }
        JsonArray overlays = new JsonArray();
        for (int i = 0; i < editor.layout.decorationOverlays.size(); i++) {
            Pair<AbstractDecorationOverlayBuilder<?>, AbstractDecorationOverlay<?>> pair = editor.layout.decorationOverlays.get(i);
            JsonObject item = new JsonObject();
            item.addProperty("index", i);
            item.addProperty("identifier", pair.getFirst().getIdentifier());
            item.add("serialized_container", FancyMenuMcpSerialization.toJson(pair.getFirst()._serialize(pair.getSecond())));
            overlays.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("backgrounds", backgrounds);
        out.add("overlays", overlays);
        out.addProperty("background_count", backgrounds.size());
        out.addProperty("overlay_count", overlays.size());
        return out;
    }

    static @NotNull JsonObject editorAddElement(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        String builderId = requireString(args, "builder_identifier");
        ElementBuilder<?, ?> builder = ElementRegistry.getBuilder(builderId);
        if (builder == null) {
            throw new IllegalArgumentException("Unknown element builder: " + builderId);
        }
        AbstractEditorElement<?, ?> editorElement = builder.wrapIntoEditorElementInternal(builder.buildDefaultInstance(), editor);
        if (editorElement == null) {
            throw new IllegalStateException("Failed to build editor element: " + builderId);
        }
        if (args.has("x") || args.has("y")) {
            int x = getInt(args, "x", editorElement.element.posOffsetX);
            int y = getInt(args, "y", editorElement.element.posOffsetY);
            editorElement.setAnchorPoint(ElementAnchorPoints.TOP_LEFT, false);
            editorElement.element.posOffsetX = x;
            editorElement.element.posOffsetY = y;
        }
        if (getBoolean(args, "select", true)) {
            editor.deselectAllElements();
            editorElement.setSelected(true);
        }
        editor.normalEditorElements.add(editorElement);
        editor.layoutEditorWidgets.forEach(widget -> widget.editorElementAdded(editorElement));
        editor.unsavedChanges = true;
        JsonObject out = new JsonObject();
        out.addProperty("added", true);
        out.addProperty("instance_identifier", editorElement.element.getInstanceIdentifier());
        return out;
    }

    static @NotNull JsonObject editorRemoveElement(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        String elementId = requireString(args, "element_identifier");
        AbstractEditorElement<?, ?> element = editor.getElementByInstanceIdentifier(elementId);
        if (element == null) {
            throw new IllegalArgumentException("Element not found: " + elementId);
        }
        boolean removed = editor.deleteElement(element);
        editor.unsavedChanges = true;
        JsonObject out = new JsonObject();
        out.addProperty("removed", removed);
        out.addProperty("element_identifier", elementId);
        return out;
    }

    static @NotNull JsonObject editorMoveElement(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        String elementId = requireString(args, "element_identifier");
        AbstractEditorElement<?, ?> element = editor.getElementByInstanceIdentifier(elementId);
        if (element == null) {
            throw new IllegalArgumentException("Element not found: " + elementId);
        }
        boolean moved = false;
        if (args.has("target_index")) {
            moved = editor.moveLayerToPosition(element, getInt(args, "target_index", 0));
        }
        if (args.has("x") || args.has("y")) {
            int x = getInt(args, "x", element.element.posOffsetX);
            int y = getInt(args, "y", element.element.posOffsetY);
            element.element.posOffsetX = x;
            element.element.posOffsetY = y;
            moved = true;
        }
        editor.unsavedChanges = true;
        JsonObject out = new JsonObject();
        out.addProperty("moved", moved);
        out.addProperty("element_identifier", elementId);
        return out;
    }

    static @NotNull JsonObject editorSetElementOrder(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        List<String> orderedIdentifiers = getStringArray(args, "ordered_element_identifiers");
        if (orderedIdentifiers.isEmpty()) {
            throw new IllegalArgumentException("ordered_element_identifiers must not be empty.");
        }
        JsonArray applied = new JsonArray();
        JsonArray missing = new JsonArray();
        int targetIndex = 0;
        for (String identifier : orderedIdentifiers) {
            AbstractEditorElement<?, ?> element = editor.getElementByInstanceIdentifier(identifier);
            if (element == null || !editor.normalEditorElements.contains(element)) {
                missing.add(identifier);
                continue;
            }
            if (editor.moveLayerToPosition(element, targetIndex)) {
                applied.add(identifier);
            }
            targetIndex++;
        }
        editor.unsavedChanges = true;
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.add("applied_identifiers", applied);
        out.add("missing_identifiers", missing);
        out.addProperty("normal_count", editor.normalEditorElements.size());
        return out;
    }

    static @NotNull JsonObject editorUpdateElement(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        String elementId = requireString(args, "element_identifier");
        AbstractEditorElement<?, ?> previous = editor.getElementByInstanceIdentifier(elementId);
        if (previous == null) {
            throw new IllegalArgumentException("Element not found: " + elementId);
        }
        JsonObject serializedJson = requireJsonObject(args, "serialized_element");
        SerializedElement serialized = serializedElementFromJson(serializedJson);
        String elementType = FancyMenuMcpSerialization.getString(serializedJson, "element_type", null);
        if (elementType == null || elementType.isBlank()) {
            throw new IllegalArgumentException("serialized_element.element_type is required.");
        }
        ElementBuilder<?, ?> builder = ElementRegistry.getBuilder(elementType);
        if (builder == null) {
            throw new IllegalArgumentException("Unknown element builder in serialized element: " + elementType);
        }
        if (!serialized.hasProperty("instance_identifier")) {
            serialized.putProperty("instance_identifier", previous.element.getInstanceIdentifier());
        }
        var deserialized = builder.deserializeElementInternal(serialized);
        if (deserialized == null) {
            throw new IllegalStateException("Failed to deserialize element payload.");
        }
        AbstractEditorElement<?, ?> replacement = builder.wrapIntoEditorElementInternal(deserialized, editor);
        if (replacement == null) {
            throw new IllegalStateException("Failed to wrap replacement editor element.");
        }
        int index = editor.normalEditorElements.indexOf(previous);
        if (index >= 0) {
            editor.normalEditorElements.set(index, replacement);
        } else {
            editor.normalEditorElements.add(replacement);
        }
        editor.layoutEditorWidgets.forEach(widget -> {
            widget.editorElementRemovedOrHidden(previous);
            widget.editorElementAdded(replacement);
        });
        editor.unsavedChanges = true;
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.addProperty("element_identifier", replacement.element.getInstanceIdentifier());
        return out;
    }

    static @NotNull JsonObject editorSetLayoutFromSerialized(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        Layout parsed = parseLayoutFromArgs(args, editor.layout.layoutFile);
        LayoutHandler.openLayoutEditor(parsed, resolveLayoutTargetScreen(parsed, editor.layoutTargetScreen));
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.add("layout", layoutToJson(parsed, true));
        return out;
    }

    static @NotNull JsonObject editorSetBackground(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        String backgroundType = requireString(args, "background_identifier");
        MenuBackgroundBuilder<?> builder = MenuBackgroundRegistry.getBuilder(backgroundType);
        if (builder == null) {
            throw new IllegalArgumentException("Unknown background: " + backgroundType);
        }
        JsonObject containerJson = requireJsonObject(args, "background_container");
        PropertyContainer container = FancyMenuMcpSerialization.fromJsonContainer(containerJson);
        container.putProperty("background_type", backgroundType);
        MenuBackground<?> background = builder._deserializeBackground(container);
        if (background == null) {
            throw new IllegalStateException("Failed to deserialize background payload.");
        }
        int index = -1;
        for (int i = 0; i < editor.layout.menuBackgrounds.size(); i++) {
            if (editor.layout.menuBackgrounds.get(i).builder.getIdentifier().equals(backgroundType)) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            editor.layout.menuBackgrounds.set(index, background);
        } else {
            editor.layout.menuBackgrounds.add(background);
        }
        editor.unsavedChanges = true;
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.addProperty("background_identifier", backgroundType);
        return out;
    }

    static @NotNull JsonObject editorRemoveBackground(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        String backgroundType = requireString(args, "background_identifier");
        int before = editor.layout.menuBackgrounds.size();
        editor.layout.menuBackgrounds.removeIf(background -> background.builder.getIdentifier().equals(backgroundType));
        boolean removed = editor.layout.menuBackgrounds.size() != before;
        editor.unsavedChanges = true;
        JsonObject out = new JsonObject();
        out.addProperty("removed", removed);
        out.addProperty("background_identifier", backgroundType);
        return out;
    }

    static @NotNull JsonObject editorReorderBackground(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        String backgroundType = requireString(args, "background_identifier");
        int fromIndex = -1;
        for (int i = 0; i < editor.layout.menuBackgrounds.size(); i++) {
            if (editor.layout.menuBackgrounds.get(i).builder.getIdentifier().equals(backgroundType)) {
                fromIndex = i;
                break;
            }
        }
        if (fromIndex < 0) {
            throw new IllegalArgumentException("Background not found: " + backgroundType);
        }
        int targetIndex = getInt(args, "target_index", fromIndex);
        targetIndex = Math.max(0, Math.min(targetIndex, editor.layout.menuBackgrounds.size()));
        if (fromIndex == targetIndex) {
            JsonObject out = new JsonObject();
            out.addProperty("updated", false);
            out.addProperty("from_index", fromIndex);
            out.addProperty("to_index", targetIndex);
            return out;
        }
        MenuBackground<?> background = editor.layout.menuBackgrounds.remove(fromIndex);
        if (fromIndex < targetIndex) {
            targetIndex--;
        }
        targetIndex = Math.max(0, Math.min(targetIndex, editor.layout.menuBackgrounds.size()));
        editor.layout.menuBackgrounds.add(targetIndex, background);
        editor.unsavedChanges = true;
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.addProperty("background_identifier", backgroundType);
        out.addProperty("from_index", fromIndex);
        out.addProperty("to_index", targetIndex);
        return out;
    }

    static @NotNull JsonObject editorSetDecorationOverlay(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        String overlayType = requireString(args, "overlay_identifier");
        AbstractDecorationOverlayBuilder<?> builder = DecorationOverlayRegistry.getByIdentifier(overlayType);
        if (builder == null) {
            throw new IllegalArgumentException("Unknown overlay: " + overlayType);
        }
        JsonObject containerJson = requireJsonObject(args, "overlay_container");
        PropertyContainer container = FancyMenuMcpSerialization.fromJsonContainer(containerJson);
        container.putProperty("overlay_type", overlayType);
        PropertyContainerSet set = new PropertyContainerSet("mcp_overlay_set");
        set.putContainer(container);
        List<?> overlays = builder._deserializeAll(set);
        if (overlays.isEmpty() || !(overlays.getFirst() instanceof AbstractDecorationOverlay<?> overlay)) {
            throw new IllegalStateException("Failed to deserialize overlay payload.");
        }
        int index = -1;
        for (int i = 0; i < editor.layout.decorationOverlays.size(); i++) {
            if (editor.layout.decorationOverlays.get(i).getFirst().getIdentifier().equals(overlayType)) {
                index = i;
                break;
            }
        }
        Pair<AbstractDecorationOverlayBuilder<?>, AbstractDecorationOverlay<?>> pair = Pair.of(builder, overlay);
        if (index >= 0) {
            editor.layout.decorationOverlays.set(index, pair);
        } else {
            editor.layout.decorationOverlays.add(pair);
        }
        editor.unsavedChanges = true;
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.addProperty("overlay_identifier", overlayType);
        return out;
    }

    static @NotNull JsonObject editorRemoveDecorationOverlay(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        String overlayType = requireString(args, "overlay_identifier");
        int before = editor.layout.decorationOverlays.size();
        editor.layout.decorationOverlays.removeIf(pair -> pair.getFirst().getIdentifier().equals(overlayType));
        boolean removed = editor.layout.decorationOverlays.size() != before;
        editor.unsavedChanges = true;
        JsonObject out = new JsonObject();
        out.addProperty("removed", removed);
        out.addProperty("overlay_identifier", overlayType);
        return out;
    }

    static @NotNull JsonObject editorReorderDecorationOverlay(@NotNull JsonObject args) {
        LayoutEditorScreen editor = requireEditor(args);
        String overlayType = requireString(args, "overlay_identifier");
        int fromIndex = -1;
        for (int i = 0; i < editor.layout.decorationOverlays.size(); i++) {
            if (editor.layout.decorationOverlays.get(i).getFirst().getIdentifier().equals(overlayType)) {
                fromIndex = i;
                break;
            }
        }
        if (fromIndex < 0) {
            throw new IllegalArgumentException("Overlay not found: " + overlayType);
        }
        int targetIndex = getInt(args, "target_index", fromIndex);
        targetIndex = Math.max(0, Math.min(targetIndex, editor.layout.decorationOverlays.size()));
        if (fromIndex == targetIndex) {
            JsonObject out = new JsonObject();
            out.addProperty("updated", false);
            out.addProperty("from_index", fromIndex);
            out.addProperty("to_index", targetIndex);
            return out;
        }
        Pair<AbstractDecorationOverlayBuilder<?>, AbstractDecorationOverlay<?>> pair = editor.layout.decorationOverlays.remove(fromIndex);
        if (fromIndex < targetIndex) {
            targetIndex--;
        }
        targetIndex = Math.max(0, Math.min(targetIndex, editor.layout.decorationOverlays.size()));
        editor.layout.decorationOverlays.add(targetIndex, pair);
        editor.unsavedChanges = true;
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.addProperty("overlay_identifier", overlayType);
        out.addProperty("from_index", fromIndex);
        out.addProperty("to_index", targetIndex);
        return out;
    }

    static @NotNull JsonObject openActionEditor(@NotNull JsonObject args) {
        FancyMenuMcpActionEditorBridge.ActionScriptTarget target = requireActionTarget(args);
        boolean opened = FancyMenuMcpActionEditorBridge.openForTarget(target);
        JsonObject out = new JsonObject();
        out.addProperty("opened", opened);
        JsonObject targetJson = new JsonObject();
        targetJson.addProperty("target_type", target.type().name().toLowerCase(Locale.ROOT));
        targetJson.addProperty("target_id", target.targetIdentifier());
        out.add("target", targetJson);
        return out;
    }

    static @NotNull JsonObject getActionScript(@NotNull JsonObject args) {
        FancyMenuMcpActionEditorBridge.ActionScriptTarget target = requireActionTarget(args);
        GenericExecutableBlock script = FancyMenuMcpActionEditorBridge.getScript(target);
        if (script == null) {
            throw new IllegalArgumentException("Action script target not found.");
        }
        JsonObject out = new JsonObject();
        out.add("script", FancyMenuMcpSerialization.scriptToJson(script));
        JsonObject editorInfo = FancyMenuMcpActionEditorBridge.getActiveEditorInfo();
        if (editorInfo != null) {
            out.add("active_editor", editorInfo);
        }
        return out;
    }

    static @NotNull JsonObject setActionScript(@NotNull JsonObject args) {
        FancyMenuMcpActionEditorBridge.ActionScriptTarget target = requireActionTarget(args);
        JsonObject scriptJson = requireJsonObject(args, "script");
        GenericExecutableBlock script = FancyMenuMcpSerialization.scriptFromJson(scriptJson);
        boolean keepEditorVisible = getBoolean(args, "open_editor", true);
        boolean updated = FancyMenuMcpActionEditorBridge.setScriptAndRefresh(target, script, keepEditorVisible);
        JsonObject out = new JsonObject();
        out.addProperty("updated", updated);
        out.addProperty("open_editor", keepEditorVisible);
        return out;
    }

    static @NotNull JsonObject listListenerInstances() {
        JsonArray instances = new JsonArray();
        for (ListenerInstance instance : ListenerHandler.getInstances()) {
            JsonObject item = new JsonObject();
            item.addProperty("instance_identifier", instance.instanceIdentifier);
            item.addProperty("listener_identifier", instance.parent.getIdentifier());
            item.addProperty("display_name", Objects.requireNonNullElse(instance.getDisplayName(), ""));
            item.add("serialized", FancyMenuMcpSerialization.toJson(instance.serialize()));
            instances.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("listener_instances", instances);
        out.addProperty("count", instances.size());
        return out;
    }

    static @NotNull JsonObject createListenerInstance(@NotNull JsonObject args) {
        String listenerId = requireString(args, "listener_identifier");
        AbstractListener listener = ListenerRegistry.getListener(listenerId);
        if (listener == null) {
            throw new IllegalArgumentException("Unknown listener type: " + listenerId);
        }
        ListenerInstance instance = listener.createFreshInstance();
        if (args.has("display_name")) {
            instance.setDisplayName(getString(args, "display_name", null));
        }
        if (args.has("script")) {
            instance.setActionScript(FancyMenuMcpSerialization.scriptFromJson(requireJsonObject(args, "script")));
        }
        ListenerHandler.addInstance(instance);
        boolean openActionEditor = args.has("open_action_editor")
                ? getBoolean(args, "open_action_editor", false)
                : args.has("script");
        if (openActionEditor) {
            FancyMenuMcpActionEditorBridge.openForTarget(new FancyMenuMcpActionEditorBridge.ActionScriptTarget(
                    FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.LISTENER_SCRIPT,
                    instance.instanceIdentifier
            ));
        }
        JsonObject out = new JsonObject();
        out.addProperty("created", true);
        out.addProperty("instance_identifier", instance.instanceIdentifier);
        return out;
    }

    static @NotNull JsonObject updateListenerInstance(@NotNull JsonObject args) {
        String instanceId = requireString(args, "instance_identifier");
        ListenerInstance instance = ListenerHandler.getInstance(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("Listener instance not found: " + instanceId);
        }
        if (args.has("display_name")) {
            instance.setDisplayName(getString(args, "display_name", null));
        }
        if (args.has("script")) {
            instance.setActionScript(FancyMenuMcpSerialization.scriptFromJson(requireJsonObject(args, "script")));
        }
        ListenerHandler.syncChanges();
        boolean openActionEditor = args.has("open_action_editor")
                ? getBoolean(args, "open_action_editor", false)
                : args.has("script");
        if (openActionEditor) {
            FancyMenuMcpActionEditorBridge.openForTarget(new FancyMenuMcpActionEditorBridge.ActionScriptTarget(
                    FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.LISTENER_SCRIPT,
                    instanceId
            ));
        }
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        return out;
    }

    static @NotNull JsonObject setListenerInstance(@NotNull JsonObject args) {
        JsonObject serialized = requireJsonObject(args, "serialized");
        ListenerInstance instance = ListenerInstance.deserialize(FancyMenuMcpSerialization.fromJsonContainer(serialized));
        if (instance == null) {
            throw new IllegalArgumentException("Failed to deserialize listener instance payload.");
        }
        boolean existed = ListenerHandler.getInstance(instance.instanceIdentifier) != null;
        if (existed) {
            ListenerHandler.removeInstance(instance.instanceIdentifier);
        }
        ListenerHandler.addInstance(instance);
        if (getBoolean(args, "open_action_editor", false)) {
            FancyMenuMcpActionEditorBridge.openForTarget(new FancyMenuMcpActionEditorBridge.ActionScriptTarget(
                    FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.LISTENER_SCRIPT,
                    instance.instanceIdentifier
            ));
        }
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.addProperty("replaced_existing", existed);
        out.addProperty("instance_identifier", instance.instanceIdentifier);
        return out;
    }

    static @NotNull JsonObject deleteListenerInstance(@NotNull JsonObject args) {
        String instanceId = requireString(args, "instance_identifier");
        ListenerHandler.removeInstance(instanceId);
        JsonObject out = new JsonObject();
        out.addProperty("deleted", true);
        return out;
    }

    static @NotNull JsonObject listSchedulers() {
        JsonArray schedulers = new JsonArray();
        for (SchedulerInstance instance : SchedulerHandler.getInstances()) {
            JsonObject item = new JsonObject();
            item.addProperty("identifier", instance.getIdentifier());
            item.addProperty("start_on_launch", instance.isStartOnLaunch());
            item.addProperty("start_delay_ms", instance.getStartDelayMs());
            item.addProperty("tick_delay_ms", instance.getTickDelayMs());
            item.addProperty("ticks_to_run", instance.getTicksToRun());
            item.addProperty("running", SchedulerHandler.isRunning(instance.getIdentifier()));
            item.add("serialized", FancyMenuMcpSerialization.toJson(instance.serialize()));
            schedulers.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("schedulers", schedulers);
        out.addProperty("count", schedulers.size());
        return out;
    }

    static @NotNull JsonObject createScheduler(@NotNull JsonObject args) {
        SchedulerInstance instance = SchedulerHandler.createFreshInstance();
        String customIdentifier = getString(args, "identifier", null);
        if (customIdentifier != null && SchedulerHandler.isIdentifierValid(customIdentifier)) {
            instance.setIdentifier(customIdentifier);
        }
        applySchedulerPatch(instance, args);
        SchedulerHandler.addInstance(instance);
        if (getBoolean(args, "start_now", false)) {
            SchedulerHandler.startScheduler(instance.getIdentifier());
        }
        boolean openActionEditor = args.has("open_action_editor")
                ? getBoolean(args, "open_action_editor", false)
                : args.has("script");
        if (openActionEditor) {
            FancyMenuMcpActionEditorBridge.openForTarget(new FancyMenuMcpActionEditorBridge.ActionScriptTarget(
                    FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.SCHEDULER_SCRIPT,
                    instance.getIdentifier()
            ));
        }
        JsonObject out = new JsonObject();
        out.addProperty("created", true);
        out.addProperty("identifier", instance.getIdentifier());
        return out;
    }

    static @NotNull JsonObject updateScheduler(@NotNull JsonObject args) {
        String identifier = requireString(args, "identifier");
        SchedulerInstance instance = SchedulerHandler.getInstance(identifier);
        if (instance == null) {
            throw new IllegalArgumentException("Scheduler not found: " + identifier);
        }
        String newIdentifier = getString(args, "new_identifier", null);
        boolean renamed = false;
        if (newIdentifier != null && !newIdentifier.equals(identifier)) {
            if (!SchedulerHandler.isIdentifierValid(newIdentifier)) {
                throw new IllegalArgumentException("Invalid scheduler identifier: " + newIdentifier);
            }
            SchedulerHandler.removeInstance(identifier);
            instance.setIdentifier(newIdentifier);
            SchedulerHandler.addInstance(instance);
            renamed = true;
        }
        applySchedulerPatch(instance, args);
        SchedulerHandler.syncChanges();
        String effectiveIdentifier = instance.getIdentifier();
        if (getBoolean(args, "start_now", false)) {
            SchedulerHandler.startScheduler(effectiveIdentifier);
        }
        if (getBoolean(args, "stop_now", false)) {
            SchedulerHandler.stopScheduler(effectiveIdentifier);
        }
        boolean openActionEditor = args.has("open_action_editor")
                ? getBoolean(args, "open_action_editor", false)
                : args.has("script");
        if (openActionEditor) {
            FancyMenuMcpActionEditorBridge.openForTarget(new FancyMenuMcpActionEditorBridge.ActionScriptTarget(
                    FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.SCHEDULER_SCRIPT,
                    effectiveIdentifier
            ));
        }
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.addProperty("renamed", renamed);
        out.addProperty("identifier", effectiveIdentifier);
        return out;
    }

    static @NotNull JsonObject setScheduler(@NotNull JsonObject args) {
        JsonObject serialized = requireJsonObject(args, "serialized");
        SchedulerInstance instance = SchedulerInstance.deserialize(FancyMenuMcpSerialization.fromJsonContainer(serialized));
        if (instance == null) {
            throw new IllegalArgumentException("Failed to deserialize scheduler payload.");
        }
        boolean existed = SchedulerHandler.getInstance(instance.getIdentifier()) != null;
        if (existed) {
            SchedulerHandler.removeInstance(instance.getIdentifier());
        }
        SchedulerHandler.addInstance(instance);
        if (getBoolean(args, "start_now", false)) {
            SchedulerHandler.startScheduler(instance.getIdentifier());
        }
        if (getBoolean(args, "stop_now", false)) {
            SchedulerHandler.stopScheduler(instance.getIdentifier());
        }
        if (getBoolean(args, "open_action_editor", false)) {
            FancyMenuMcpActionEditorBridge.openForTarget(new FancyMenuMcpActionEditorBridge.ActionScriptTarget(
                    FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.SCHEDULER_SCRIPT,
                    instance.getIdentifier()
            ));
        }
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.addProperty("replaced_existing", existed);
        out.addProperty("identifier", instance.getIdentifier());
        return out;
    }

    static @NotNull JsonObject deleteScheduler(@NotNull JsonObject args) {
        String identifier = requireString(args, "identifier");
        SchedulerHandler.removeInstance(identifier);
        JsonObject out = new JsonObject();
        out.addProperty("deleted", true);
        return out;
    }

    static @NotNull JsonObject startScheduler(@NotNull JsonObject args) {
        String identifier = requireString(args, "identifier");
        SchedulerHandler.startScheduler(identifier);
        JsonObject out = new JsonObject();
        out.addProperty("started", true);
        return out;
    }

    static @NotNull JsonObject stopScheduler(@NotNull JsonObject args) {
        String identifier = requireString(args, "identifier");
        SchedulerHandler.stopScheduler(identifier);
        JsonObject out = new JsonObject();
        out.addProperty("stopped", true);
        return out;
    }

    static @NotNull JsonObject listVariables() {
        JsonArray variables = new JsonArray();
        for (Variable variable : VariableHandler.getVariables()) {
            JsonObject item = new JsonObject();
            item.addProperty("name", variable.getName());
            item.addProperty("value", variable.getValue());
            item.addProperty("reset_on_launch", variable.isResetOnLaunch());
            item.add("serialized", FancyMenuMcpSerialization.toJson(variable.serialize()));
            variables.add(item);
        }
        JsonObject out = new JsonObject();
        out.add("variables", variables);
        out.addProperty("count", variables.size());
        return out;
    }

    static @NotNull JsonObject setVariable(@NotNull JsonObject args) {
        String name = requireString(args, "name");
        String value = getString(args, "value", "");
        VariableHandler.setVariable(name, value);
        Variable variable = VariableHandler.getVariable(name);
        if (variable != null && args.has("reset_on_launch")) {
            variable.setResetOnLaunch(getBoolean(args, "reset_on_launch", variable.isResetOnLaunch()));
        }
        JsonObject out = new JsonObject();
        out.addProperty("updated", true);
        out.addProperty("name", name);
        return out;
    }

    static @NotNull JsonObject removeVariable(@NotNull JsonObject args) {
        String name = requireString(args, "name");
        VariableHandler.removeVariable(name);
        JsonObject out = new JsonObject();
        out.addProperty("removed", true);
        return out;
    }

    static @NotNull JsonObject clearVariables() {
        VariableHandler.clearVariables();
        JsonObject out = new JsonObject();
        out.addProperty("cleared", true);
        return out;
    }

    static @NotNull JsonObject parseFancyProperties(@NotNull JsonObject args) {
        String fancy = requireString(args, "fancy_string");
        JsonObject out = new JsonObject();
        out.add("parsed", FancyMenuMcpSerialization.parseFancyString(fancy));
        return out;
    }

    static @NotNull JsonObject stringifyFancyProperties(@NotNull JsonObject args) {
        JsonObject set = requireJsonObject(args, "set");
        String fancyString = FancyMenuMcpSerialization.stringifyFancyString(set);
        JsonObject out = new JsonObject();
        out.addProperty("fancy_string", fancyString);
        out.addProperty("byte_length", fancyString.getBytes(StandardCharsets.UTF_8).length);
        return out;
    }

    static @NotNull JsonObject normalizeRequirementContainer(@NotNull JsonObject args) {
        JsonObject reqJson = requireJsonObject(args, "requirement_container");
        RequirementContainer container = FancyMenuMcpSerialization.requirementContainerFromJson(reqJson);
        JsonObject out = new JsonObject();
        out.add("requirement_container", FancyMenuMcpSerialization.requirementContainerToJson(container));
        out.addProperty("is_empty", container.isEmpty());
        return out;
    }

    static @NotNull JsonObject checkRequirementContainer(@NotNull JsonObject args) {
        JsonObject reqJson = requireJsonObject(args, "requirement_container");
        RequirementContainer container = FancyMenuMcpSerialization.requirementContainerFromJson(reqJson);
        JsonObject out = new JsonObject();
        out.addProperty("requirements_met", container.requirementsMet());
        return out;
    }

    static @NotNull JsonObject evaluatePlaceholders(@NotNull JsonObject args) {
        String text = requireString(args, "text");
        JsonObject out = new JsonObject();
        out.addProperty("input", text);
        out.addProperty("output", PlaceholderParser.replacePlaceholders(text));
        return out;
    }

    private static void applySchedulerPatch(@NotNull SchedulerInstance instance, @NotNull JsonObject args) {
        if (args.has("start_on_launch")) {
            instance.setStartOnLaunch(getBoolean(args, "start_on_launch", instance.isStartOnLaunch()));
        }
        if (args.has("start_delay_ms")) {
            instance.setStartDelayMs(getLong(args, "start_delay_ms", instance.getStartDelayMs()));
        }
        if (args.has("tick_delay_ms")) {
            instance.setTickDelayMs(getLong(args, "tick_delay_ms", instance.getTickDelayMs()));
        }
        if (args.has("ticks_to_run")) {
            instance.setTicksToRun(getLong(args, "ticks_to_run", instance.getTicksToRun()));
        }
        if (args.has("script")) {
            instance.setActionScript(FancyMenuMcpSerialization.scriptFromJson(requireJsonObject(args, "script")));
        }
    }

    private static @NotNull Layout requireLayout(@NotNull JsonObject args) {
        Layout layout = resolveLayout(args);
        if (layout == null) {
            throw new IllegalArgumentException("Layout not found for provided selector.");
        }
        return layout;
    }

    private static @Nullable Layout resolveLayout(@NotNull JsonObject args) {
        String runtimeId = getString(args, "runtime_layout_identifier", null);
        if (runtimeId != null) {
            for (Layout layout : LayoutHandler.getAllLayouts()) {
                if (layout.runtimeLayoutIdentifier.equals(runtimeId)) {
                    return layout;
                }
            }
        }
        String filePath = getString(args, "layout_file", null);
        if (filePath != null) {
            return findLayoutByFile(new File(filePath));
        }
        String layoutName = getString(args, "layout_name", null);
        if (layoutName != null) {
            for (Layout layout : LayoutHandler.getAllLayouts()) {
                if (layout.getLayoutName().equalsIgnoreCase(layoutName)) {
                    return layout;
                }
            }
        }
        String screenIdentifier = getString(args, "screen_identifier", null);
        if (screenIdentifier != null) {
            int layoutIndex = getInt(args, "layout_index", Integer.MIN_VALUE);
            List<Layout> layoutsForScreen = LayoutHandler.getAllLayoutsForScreenIdentifier(screenIdentifier, true);
            if (layoutIndex != Integer.MIN_VALUE) {
                for (Layout layout : layoutsForScreen) {
                    if (layout.layoutIndex == layoutIndex) {
                        return layout;
                    }
                }
            }
            if (layoutsForScreen.size() == 1) {
                return layoutsForScreen.getFirst();
            }
        }
        return null;
    }

    private static @Nullable Layout findLayoutByFile(@Nullable File layoutFile) {
        if (layoutFile == null) {
            return null;
        }
        String path = layoutFile.getAbsolutePath().replace("\\", "/");
        for (Layout layout : LayoutHandler.getAllLayouts()) {
            if (layout.layoutFile != null && layout.layoutFile.getAbsolutePath().replace("\\", "/").equals(path)) {
                return layout;
            }
        }
        return null;
    }

    private static @NotNull Layout parseLayoutFromArgs(@NotNull JsonObject args, @Nullable File layoutFile) {
        PropertyContainerSet serialized;
        if (args.has("serialized_set")) {
            serialized = FancyMenuMcpSerialization.fromJsonSet(requireJsonObject(args, "serialized_set"));
        } else if (args.has("fancy_string")) {
            String fancy = requireString(args, "fancy_string");
            serialized = Objects.requireNonNull(PropertiesParser.deserializeSetFromFancyString(fancy), "Failed to parse fancy string layout payload.");
        } else {
            throw new IllegalArgumentException("Either 'serialized_set' or 'fancy_string' is required.");
        }
        Layout parsed = Layout.deserialize(serialized, layoutFile);
        if (parsed == null) {
            throw new IllegalArgumentException("Failed to deserialize layout payload.");
        }
        return parsed;
    }

    private static void openOrRefreshLayoutEditor(@NotNull Layout layout, boolean forceOpen) {
        LayoutEditorScreen current = LayoutEditorScreen.getCurrentInstance();
        if (!forceOpen && current != null && matchesLayout(current.layout, layout)) {
            return;
        }
        LayoutHandler.openLayoutEditor(layout, resolveLayoutTargetScreen(layout, current != null ? current.layoutTargetScreen : null));
    }

    private static boolean matchesLayout(@NotNull Layout first, @NotNull Layout second) {
        if (first.layoutFile != null && second.layoutFile != null) {
            return first.layoutFile.getAbsolutePath().replace("\\", "/")
                    .equals(second.layoutFile.getAbsolutePath().replace("\\", "/"));
        }
        return Objects.equals(first.screenIdentifier, second.screenIdentifier)
                && Objects.equals(first.getLayoutName(), second.getLayoutName());
    }

    private static @Nullable Screen resolveLayoutTargetScreen(@NotNull Layout layout, @Nullable Screen fallback) {
        if (layout.isUniversalLayout()) {
            return null;
        }
        if (fallback != null) {
            String fallbackIdentifier = ScreenIdentifierHandler.getIdentifierOfScreen(fallback);
            if (ScreenIdentifierHandler.equalIdentifiers(layout.screenIdentifier, fallbackIdentifier)) {
                return fallback;
            }
        }
        return ScreenInstanceFactory.tryConstruct(Objects.requireNonNull(layout.screenIdentifier));
    }

    private static @NotNull LayoutEditorScreen requireEditor(@Nullable JsonObject args) {
        LayoutEditorScreen editor = LayoutEditorScreen.getCurrentInstance();
        if (args != null && hasLayoutSelector(args)) {
            Layout layout = resolveLayout(args);
            if (layout == null) {
                throw new IllegalArgumentException("Layout not found for provided selector.");
            }
            if (editor == null || !matchesLayout(editor.layout, layout)) {
                openOrRefreshLayoutEditor(layout, true);
                editor = LayoutEditorScreen.getCurrentInstance();
            }
        }
        if (editor == null) {
            throw new IllegalStateException("Layout editor is not open.");
        }
        return editor;
    }

    private static void syncEditorLayout(@NotNull LayoutEditorScreen editor) throws Exception {
        Method method = LayoutEditorScreen.class.getDeclaredMethod("serializeElementInstancesToLayoutInstance");
        method.setAccessible(true);
        method.invoke(editor);
    }

    private static @NotNull FancyMenuMcpActionEditorBridge.ActionScriptTarget requireActionTarget(@NotNull JsonObject args) {
        String typeRaw = requireString(args, "target_type").toLowerCase(Locale.ROOT);
        String targetId = getString(args, "target_identifier", null);
        FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type type = switch (typeRaw) {
            case "layout_open", "layout_open_script" -> FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.LAYOUT_OPEN_SCRIPT;
            case "layout_close", "layout_close_script" -> FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.LAYOUT_CLOSE_SCRIPT;
            case "listener", "listener_script" -> FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.LISTENER_SCRIPT;
            case "scheduler", "scheduler_script" -> FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.SCHEDULER_SCRIPT;
            default -> throw new IllegalArgumentException("Unknown action target_type: " + typeRaw);
        };
        if ((type == FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.LAYOUT_OPEN_SCRIPT
                || type == FancyMenuMcpActionEditorBridge.ActionScriptTarget.Type.LAYOUT_CLOSE_SCRIPT)
                && (targetId == null || targetId.isBlank())
                && hasLayoutSelector(args)) {
            targetId = requireLayout(args).runtimeLayoutIdentifier;
        }
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("Missing required string field 'target_identifier'.");
        }
        return new FancyMenuMcpActionEditorBridge.ActionScriptTarget(type, targetId);
    }

    private static boolean hasLayoutSelector(@NotNull JsonObject args) {
        return args.has("runtime_layout_identifier")
                || args.has("layout_file")
                || args.has("layout_name")
                || args.has("screen_identifier");
    }

    private static @NotNull JsonObject layoutToJson(@NotNull Layout layout, boolean includeSerializedSet) {
        JsonObject json = new JsonObject();
        json.addProperty("runtime_layout_identifier", layout.runtimeLayoutIdentifier);
        json.addProperty("layout_name", layout.getLayoutName());
        json.addProperty("screen_identifier", Objects.requireNonNullElse(layout.screenIdentifier, ""));
        json.addProperty("is_universal", layout.isUniversalLayout());
        json.addProperty("enabled", layout.isEnabled());
        json.addProperty("layout_index", layout.layoutIndex);
        json.addProperty("render_elements_behind_vanilla", layout.renderElementsBehindVanilla);
        json.addProperty("random_mode", layout.randomMode);
        json.addProperty("random_group", layout.randomGroup);
        json.addProperty("random_only_first_time", layout.randomOnlyFirstTime);
        json.addProperty("custom_menu_title", Objects.requireNonNullElse(layout.customMenuTitle, ""));
        json.addProperty("forced_scale", layout.forcedScale);
        json.addProperty("auto_scaling_width", layout.autoScalingWidth);
        json.addProperty("auto_scaling_height", layout.autoScalingHeight);
        json.addProperty("preserve_background_aspect_ratio", layout.preserveBackgroundAspectRatio);
        json.addProperty("open_audio_source", layout.openAudio != null ? layout.openAudio.getSourceWithPrefix() : "");
        json.addProperty("close_audio_source", layout.closeAudio != null ? layout.closeAudio.getSourceWithPrefix() : "");
        json.addProperty("preserve_scroll_list_header_footer_aspect_ratio", layout.preserveScrollListHeaderFooterAspectRatio);
        json.addProperty("repeat_scroll_list_header_texture", layout.repeatScrollListHeaderTexture);
        json.addProperty("repeat_scroll_list_footer_texture", layout.repeatScrollListFooterTexture);
        json.addProperty("scroll_list_header_texture_source", layout.scrollListHeaderTexture != null ? layout.scrollListHeaderTexture.getSourceWithPrefix() : "");
        json.addProperty("scroll_list_footer_texture_source", layout.scrollListFooterTexture != null ? layout.scrollListFooterTexture.getSourceWithPrefix() : "");
        json.addProperty("render_scroll_list_header_shadow", layout.renderScrollListHeaderShadow);
        json.addProperty("render_scroll_list_footer_shadow", layout.renderScrollListFooterShadow);
        json.addProperty("show_scroll_list_header_footer_preview_in_editor", layout.showScrollListHeaderFooterPreviewInEditor);
        json.addProperty("show_screen_background_overlay_on_custom_background", layout.showScreenBackgroundOverlayOnCustomBackground);
        json.addProperty("apply_vanilla_background_blur", layout.applyVanillaBackgroundBlur);
        json.addProperty("last_edited_time", layout.lastEditedTime);
        json.addProperty("layout_file", layout.layoutFile != null ? layout.layoutFile.getAbsolutePath() : "");
        RequirementContainer requirementContainer = (layout.layoutWideRequirementContainer != null) ? layout.layoutWideRequirementContainer : new RequirementContainer();
        json.add("layout_wide_requirement_container", FancyMenuMcpSerialization.requirementContainerToJson(requirementContainer));
        JsonArray whitelist = new JsonArray();
        layout.universalLayoutMenuWhitelist.forEach(whitelist::add);
        json.add("universal_layout_whitelist", whitelist);
        JsonArray blacklist = new JsonArray();
        layout.universalLayoutMenuBlacklist.forEach(blacklist::add);
        json.add("universal_layout_blacklist", blacklist);
        if (includeSerializedSet) {
            json.add("serialized_set", FancyMenuMcpSerialization.toJson(layout.serialize()));
            json.addProperty("fancy_string", PropertiesParser.serializeSetToFancyString(layout.serialize()));
        }
        return json;
    }

    private static @NotNull JsonObject editorElementToJson(@NotNull AbstractEditorElement<?, ?> element, int index, boolean vanillaWidget) {
        JsonObject item = new JsonObject();
        item.addProperty("index", index);
        item.addProperty("instance_identifier", element.element.getInstanceIdentifier());
        item.addProperty("builder_identifier", element.element.getBuilder().getIdentifier());
        item.addProperty("display_name", element.element.getDisplayName().getString());
        item.addProperty("x", element.getX());
        item.addProperty("y", element.getY());
        item.addProperty("width", element.getWidth());
        item.addProperty("height", element.getHeight());
        item.addProperty("selected", element.isSelected());
        item.addProperty("multi_selected", element.isMultiSelected());
        item.addProperty("hovered", element.isHovered());
        item.addProperty("hidden_in_editor", element.element.layerHiddenInEditor);
        item.addProperty("vanilla_widget", vanillaWidget || element.element instanceof VanillaWidgetElement);
        SerializedElement serialized = element.element.getBuilder().serializeElementInternal(element.element);
        item.add("serialized_element", serialized != null ? FancyMenuMcpSerialization.toJson(serialized) : new JsonObject());
        return item;
    }

    private static @NotNull String computeEditorFingerprint(@NotNull LayoutEditorScreen editor) {
        StringBuilder state = new StringBuilder();
        state.append(editor.unsavedChanges).append('|');
        for (AbstractEditorElement<?, ?> element : editor.normalEditorElements) {
            state.append("N:")
                    .append(element.element.getInstanceIdentifier()).append(':')
                    .append(element.getX()).append(':')
                    .append(element.getY()).append(':')
                    .append(element.getWidth()).append(':')
                    .append(element.getHeight()).append(':')
                    .append(element.isSelected()).append(';');
        }
        for (AbstractEditorElement<?, ?> element : editor.vanillaWidgetEditorElements) {
            state.append("V:")
                    .append(element.element.getInstanceIdentifier()).append(':')
                    .append(element.getX()).append(':')
                    .append(element.getY()).append(':')
                    .append(element.getWidth()).append(':')
                    .append(element.getHeight()).append(':')
                    .append(element.isSelected()).append(';');
        }
        editor.layout.menuBackgrounds.forEach(background -> state.append("B:").append(background.builder.getIdentifier()).append(';'));
        editor.layout.decorationOverlays.forEach(pair -> state.append("O:").append(pair.getFirst().getIdentifier()).append(';'));
        return Integer.toHexString(state.toString().hashCode());
    }

    private static @NotNull PropertyContainerSet readCustomizableScreensSet() {
        PropertyContainerSet set = PropertiesParser.deserializeSetFromFile(ScreenCustomization.CUSTOMIZABLE_MENUS_FILE.getPath());
        if (set == null) {
            return new PropertyContainerSet("customizablemenus");
        }
        if (!"customizablemenus".equals(set.getType())) {
            PropertyContainerSet converted = new PropertyContainerSet("customizablemenus");
            set.getContainers().forEach(container -> converted.putContainer(new PropertyContainer(container.getType())));
            return converted;
        }
        return set;
    }

    private static void writeCustomizableScreensSet(@NotNull PropertyContainerSet set) {
        Set<String> seen = new HashSet<>();
        PropertyContainerSet cleaned = new PropertyContainerSet("customizablemenus");
        for (PropertyContainer container : set.getContainers()) {
            String type = ScreenIdentifierHandler.tryFixInvalidIdentifierWithNonUniversal(container.getType());
            if (type == null || type.isBlank()) {
                continue;
            }
            if (UniversalScreenIdentifierRegistry.universalIdentifierExists(type)) {
                continue;
            }
            if (!seen.add(type)) {
                continue;
            }
            cleaned.putContainer(new PropertyContainer(type));
        }
        PropertiesParser.serializeSetToFile(cleaned, ScreenCustomization.CUSTOMIZABLE_MENUS_FILE.getPath());
        ScreenCustomization.readCustomizableScreensFromFile();
    }

    private static @NotNull SerializedElement serializedElementFromJson(@NotNull JsonObject json) {
        PropertyContainer container = FancyMenuMcpSerialization.fromJsonContainer(json);
        SerializedElement serializedElement = new SerializedElement();
        for (Map.Entry<String, String> entry : container.getProperties().entrySet()) {
            serializedElement.putProperty(entry.getKey(), entry.getValue());
        }
        return serializedElement;
    }

    private static @NotNull JsonObject requireJsonObject(@NotNull JsonObject source, @NotNull String key) {
        if (!source.has(key) || !source.get(key).isJsonObject()) {
            throw new IllegalArgumentException("Expected JSON object in '" + key + "'.");
        }
        return source.getAsJsonObject(key);
    }

    private static @NotNull String requireString(@NotNull JsonObject source, @NotNull String key) {
        String value = getString(source, key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required string field '" + key + "'.");
        }
        return value;
    }

    private static @Nullable String getString(@NotNull JsonObject source, @NotNull String key, @Nullable String fallback) {
        return FancyMenuMcpSerialization.getString(source, key, fallback);
    }

    private static @Nullable String getNullableString(@NotNull JsonObject source, @NotNull String key) {
        if (!source.has(key) || source.get(key).isJsonNull()) {
            return null;
        }
        return source.get(key).getAsString();
    }

    private static boolean getBoolean(@NotNull JsonObject source, @NotNull String key, boolean fallback) {
        if (!source.has(key)) {
            return fallback;
        }
        JsonElement element = source.get(key);
        if (element.isJsonPrimitive()) {
            try {
                return element.getAsBoolean();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private static int getInt(@NotNull JsonObject source, @NotNull String key, int fallback) {
        if (!source.has(key)) {
            return fallback;
        }
        try {
            return source.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long getLong(@NotNull JsonObject source, @NotNull String key, long fallback) {
        if (!source.has(key)) {
            return fallback;
        }
        try {
            return source.get(key).getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float getFloat(@NotNull JsonObject source, @NotNull String key, float fallback) {
        if (!source.has(key)) {
            return fallback;
        }
        try {
            return source.get(key).getAsFloat();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double getDouble(@NotNull JsonObject source, @NotNull String key, double fallback) {
        if (!source.has(key)) {
            return fallback;
        }
        try {
            return source.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static @NotNull List<String> getStringArray(@NotNull JsonObject source, @NotNull String key) {
        List<String> values = new ArrayList<>();
        if (!source.has(key) || !source.get(key).isJsonArray()) {
            return values;
        }
        source.getAsJsonArray(key).forEach(element -> {
            if (element != null && !element.isJsonNull()) {
                String value = element.getAsString();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        });
        return values;
    }
}
