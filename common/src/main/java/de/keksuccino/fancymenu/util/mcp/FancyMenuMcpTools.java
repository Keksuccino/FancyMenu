package de.keksuccino.fancymenu.util.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

final class FancyMenuMcpTools {

    private static final Map<String, JsonObject> TOOL_DEFINITIONS = buildToolDefinitions();

    private FancyMenuMcpTools() {
    }

    static @NotNull JsonArray listToolDefinitions() {
        JsonArray tools = new JsonArray();
        TOOL_DEFINITIONS.values().forEach(tools::add);
        return tools;
    }

    static boolean toolExists(@NotNull String name) {
        return TOOL_DEFINITIONS.containsKey(name);
    }

    static @NotNull ToolExecution executeTool(@NotNull String name, @NotNull JsonObject arguments) throws Exception {
        return FancyMenuMcpThreading.callOnMainThread(() -> executeOnMainThread(name, arguments));
    }

    private static @NotNull ToolExecution executeOnMainThread(@NotNull String name, @NotNull JsonObject arguments) throws Exception {
        JsonObject structured;
        switch (name) {
            case "fancymenu_intro" -> structured = FancyMenuMcpOperations.intro();
            case "fancymenu_server_status" -> structured = FancyMenuMcpOperations.serverStatus();
            case "fancymenu_get_current_screen" -> structured = FancyMenuMcpOperations.currentScreen();
            case "fancymenu_get_customization_state" -> structured = FancyMenuMcpOperations.customizationState();
            case "fancymenu_set_customization_engine_enabled" -> structured = FancyMenuMcpOperations.setCustomizationEngineEnabled(arguments);
            case "fancymenu_set_current_screen_customization_enabled" -> structured = FancyMenuMcpOperations.setCurrentScreenCustomizationEnabled(arguments);
            case "fancymenu_set_screen_class_customization_enabled" -> structured = FancyMenuMcpOperations.setScreenClassCustomizationEnabled(arguments);
            case "fancymenu_clear_customizable_screen_classes" -> structured = FancyMenuMcpOperations.clearCustomizableScreenClasses();
            case "fancymenu_reload" -> structured = FancyMenuMcpOperations.reloadFancyMenu();
            case "fancymenu_reinit_current_screen" -> structured = FancyMenuMcpOperations.reinitCurrentScreen();
            case "fancymenu_get_editor_options" -> structured = FancyMenuMcpOperations.getEditorOptions();
            case "fancymenu_set_editor_options" -> structured = FancyMenuMcpOperations.setEditorOptions(arguments);
            case "fancymenu_capture_screenshot" -> {
                structured = FancyMenuMcpOperations.captureScreenshot(arguments);
                if (structured.has("base64")) {
                    String imageBase64 = structured.get("base64").getAsString();
                    String mimeType = structured.get("mime_type").getAsString();
                    return new ToolExecution(name, structured, imageBase64, mimeType);
                }
                return new ToolExecution(name, structured, null, null);
            }
            case "fancymenu_list_actions" -> structured = FancyMenuMcpOperations.listActions(arguments);
            case "fancymenu_list_placeholders" -> structured = FancyMenuMcpOperations.listPlaceholders(arguments);
            case "fancymenu_list_requirements" -> structured = FancyMenuMcpOperations.listRequirements(arguments);
            case "fancymenu_list_listener_types" -> structured = FancyMenuMcpOperations.listListenerTypes(arguments);
            case "fancymenu_list_elements" -> structured = FancyMenuMcpOperations.listElements(arguments);
            case "fancymenu_list_backgrounds" -> structured = FancyMenuMcpOperations.listBackgrounds(arguments);
            case "fancymenu_list_decoration_overlays" -> structured = FancyMenuMcpOperations.listDecorationOverlays(arguments);
            case "fancymenu_list_panoramas" -> structured = FancyMenuMcpOperations.listPanoramas(arguments);
            case "fancymenu_list_slideshows" -> structured = FancyMenuMcpOperations.listSlideshows(arguments);
            case "fancymenu_list_screen_identifiers" -> structured = FancyMenuMcpOperations.listScreenIdentifiers(arguments);
            case "fancymenu_list_assets" -> structured = FancyMenuMcpOperations.listAssets(arguments);
            case "fancymenu_get_registry_entry" -> structured = FancyMenuMcpOperations.getRegistryEntry(arguments);
            case "fancymenu_list_layouts_compact" -> structured = FancyMenuMcpOperations.listLayoutsCompact(arguments);
            case "fancymenu_list_layouts" -> structured = FancyMenuMcpOperations.listLayouts(arguments);
            case "fancymenu_get_layout_meta" -> structured = FancyMenuMcpOperations.getLayoutMeta(arguments);
            case "fancymenu_get_layout_elements" -> structured = FancyMenuMcpOperations.getLayoutElements(arguments);
            case "fancymenu_get_layout" -> structured = FancyMenuMcpOperations.getLayout(arguments);
            case "fancymenu_create_layout" -> structured = FancyMenuMcpOperations.createLayout(arguments);
            case "fancymenu_set_layout" -> structured = FancyMenuMcpOperations.setLayout(arguments);
            case "fancymenu_update_layout_meta" -> structured = FancyMenuMcpOperations.updateLayoutMeta(arguments);
            case "fancymenu_delete_layout" -> structured = FancyMenuMcpOperations.deleteLayout(arguments);
            case "fancymenu_save_layout" -> structured = FancyMenuMcpOperations.saveLayout(arguments);
            case "fancymenu_open_layout_editor" -> structured = FancyMenuMcpOperations.openLayoutEditor(arguments);
            case "fancymenu_close_layout_editor" -> structured = FancyMenuMcpOperations.closeLayoutEditor();
            case "fancymenu_editor_get_state" -> structured = FancyMenuMcpOperations.getEditorState(arguments);
            case "fancymenu_editor_poll" -> structured = FancyMenuMcpOperations.editorPoll(arguments);
            case "fancymenu_editor_get_element" -> structured = FancyMenuMcpOperations.editorGetElement(arguments);
            case "fancymenu_editor_list_elements" -> structured = FancyMenuMcpOperations.editorListElements(arguments);
            case "fancymenu_editor_select_elements" -> structured = FancyMenuMcpOperations.editorSelectElements(arguments);
            case "fancymenu_editor_get_visual_layers" -> structured = FancyMenuMcpOperations.editorGetVisualLayers(arguments);
            case "fancymenu_editor_patch_layout" -> structured = FancyMenuMcpOperations.editorPatchLayout(arguments);
            case "fancymenu_editor_add_element" -> structured = FancyMenuMcpOperations.editorAddElement(arguments);
            case "fancymenu_editor_remove_element" -> structured = FancyMenuMcpOperations.editorRemoveElement(arguments);
            case "fancymenu_editor_move_element" -> structured = FancyMenuMcpOperations.editorMoveElement(arguments);
            case "fancymenu_editor_set_element_order" -> structured = FancyMenuMcpOperations.editorSetElementOrder(arguments);
            case "fancymenu_editor_update_element" -> structured = FancyMenuMcpOperations.editorUpdateElement(arguments);
            case "fancymenu_editor_set_layout_from_serialized" -> structured = FancyMenuMcpOperations.editorSetLayoutFromSerialized(arguments);
            case "fancymenu_editor_set_background" -> structured = FancyMenuMcpOperations.editorSetBackground(arguments);
            case "fancymenu_editor_remove_background" -> structured = FancyMenuMcpOperations.editorRemoveBackground(arguments);
            case "fancymenu_editor_reorder_background" -> structured = FancyMenuMcpOperations.editorReorderBackground(arguments);
            case "fancymenu_editor_set_decoration_overlay" -> structured = FancyMenuMcpOperations.editorSetDecorationOverlay(arguments);
            case "fancymenu_editor_remove_decoration_overlay" -> structured = FancyMenuMcpOperations.editorRemoveDecorationOverlay(arguments);
            case "fancymenu_editor_reorder_decoration_overlay" -> structured = FancyMenuMcpOperations.editorReorderDecorationOverlay(arguments);
            case "fancymenu_open_action_editor" -> structured = FancyMenuMcpOperations.openActionEditor(arguments);
            case "fancymenu_get_action_script" -> structured = FancyMenuMcpOperations.getActionScript(arguments);
            case "fancymenu_set_action_script" -> structured = FancyMenuMcpOperations.setActionScript(arguments);
            case "fancymenu_list_listener_instances" -> structured = FancyMenuMcpOperations.listListenerInstances(arguments);
            case "fancymenu_get_listener_instance" -> structured = FancyMenuMcpOperations.getListenerInstance(arguments);
            case "fancymenu_create_listener_instance" -> structured = FancyMenuMcpOperations.createListenerInstance(arguments);
            case "fancymenu_update_listener_instance" -> structured = FancyMenuMcpOperations.updateListenerInstance(arguments);
            case "fancymenu_set_listener_instance" -> structured = FancyMenuMcpOperations.setListenerInstance(arguments);
            case "fancymenu_delete_listener_instance" -> structured = FancyMenuMcpOperations.deleteListenerInstance(arguments);
            case "fancymenu_list_schedulers" -> structured = FancyMenuMcpOperations.listSchedulers(arguments);
            case "fancymenu_get_scheduler" -> structured = FancyMenuMcpOperations.getScheduler(arguments);
            case "fancymenu_create_scheduler" -> structured = FancyMenuMcpOperations.createScheduler(arguments);
            case "fancymenu_update_scheduler" -> structured = FancyMenuMcpOperations.updateScheduler(arguments);
            case "fancymenu_set_scheduler" -> structured = FancyMenuMcpOperations.setScheduler(arguments);
            case "fancymenu_delete_scheduler" -> structured = FancyMenuMcpOperations.deleteScheduler(arguments);
            case "fancymenu_start_scheduler" -> structured = FancyMenuMcpOperations.startScheduler(arguments);
            case "fancymenu_stop_scheduler" -> structured = FancyMenuMcpOperations.stopScheduler(arguments);
            case "fancymenu_list_variables" -> structured = FancyMenuMcpOperations.listVariables(arguments);
            case "fancymenu_get_variable" -> structured = FancyMenuMcpOperations.getVariable(arguments);
            case "fancymenu_set_variable" -> structured = FancyMenuMcpOperations.setVariable(arguments);
            case "fancymenu_remove_variable" -> structured = FancyMenuMcpOperations.removeVariable(arguments);
            case "fancymenu_clear_variables" -> structured = FancyMenuMcpOperations.clearVariables();
            case "fancymenu_parse_fancy_properties" -> structured = FancyMenuMcpOperations.parseFancyProperties(arguments);
            case "fancymenu_stringify_fancy_properties" -> structured = FancyMenuMcpOperations.stringifyFancyProperties(arguments);
            case "fancymenu_normalize_requirement_container" -> structured = FancyMenuMcpOperations.normalizeRequirementContainer(arguments);
            case "fancymenu_check_requirement_container" -> structured = FancyMenuMcpOperations.checkRequirementContainer(arguments);
            case "fancymenu_evaluate_placeholders" -> structured = FancyMenuMcpOperations.evaluatePlaceholders(arguments);
            default -> throw new IllegalArgumentException("Unknown MCP tool: " + name);
        }
        return new ToolExecution(name, structured, null, null);
    }

    private static @NotNull Map<String, JsonObject> buildToolDefinitions() {
        Map<String, JsonObject> tools = new LinkedHashMap<>();

        tools.put("fancymenu_intro", tool("fancymenu_intro",
                "MUST be called first in every session before any other tool. Explains FancyMenu, this MCP, efficient workflows, and low-context usage patterns.",
                schemaObject()));

        tools.put("fancymenu_server_status", tool("fancymenu_server_status", "Returns MCP server runtime and option status.", schemaObject()));
        tools.put("fancymenu_get_current_screen", tool("fancymenu_get_current_screen", "Returns current Minecraft screen info.", schemaObject()));
        tools.put("fancymenu_get_customization_state", tool("fancymenu_get_customization_state", "Returns global/current-screen customization state and configured customizable screen classes.", schemaObject()));
        tools.put("fancymenu_set_customization_engine_enabled", tool("fancymenu_set_customization_engine_enabled", "Enables/disables FancyMenu's screen customization engine.", customizationEngineSchema()));
        tools.put("fancymenu_set_current_screen_customization_enabled", tool("fancymenu_set_current_screen_customization_enabled", "Enables/disables customization for the currently open screen.", currentScreenCustomizationSchema()));
        tools.put("fancymenu_set_screen_class_customization_enabled", tool("fancymenu_set_screen_class_customization_enabled", "Enables/disables customization for a specific screen class.", screenClassCustomizationSchema()));
        tools.put("fancymenu_clear_customizable_screen_classes", tool("fancymenu_clear_customizable_screen_classes", "Clears all customizable screen class entries.", schemaObject()));
        tools.put("fancymenu_reload", tool("fancymenu_reload", "Reloads FancyMenu systems, layouts, options, and UI state.", schemaObject()));
        tools.put("fancymenu_reinit_current_screen", tool("fancymenu_reinit_current_screen", "Re-initializes the currently open Minecraft screen.", schemaObject()));
        tools.put("fancymenu_get_editor_options", tool("fancymenu_get_editor_options", "Returns current FancyMenu layout editor option values.", schemaObject()));
        tools.put("fancymenu_set_editor_options", tool("fancymenu_set_editor_options", "Updates FancyMenu layout editor option values.", editorOptionsSchema()));
        tools.put("fancymenu_capture_screenshot", tool("fancymenu_capture_screenshot", "Captures current screen as PNG. Set include_base64=false for metadata-only responses.", screenshotSchema()));

        tools.put("fancymenu_list_actions", tool("fancymenu_list_actions", "Lists actions. Supports query/limit/offset and compact fields.", registrySearchSchema("include_descriptions", "include_value_fields")));
        tools.put("fancymenu_list_placeholders", tool("fancymenu_list_placeholders", "Lists placeholders. Supports query/limit/offset and compact fields.", registrySearchSchema("include_descriptions", "include_value_names")));
        tools.put("fancymenu_list_requirements", tool("fancymenu_list_requirements", "Lists requirement types. Supports query/limit/offset and compact fields.", registrySearchSchema("include_descriptions", "include_value_fields")));
        tools.put("fancymenu_list_listener_types", tool("fancymenu_list_listener_types", "Lists listener provider types. Supports query/limit/offset and compact fields.", registrySearchSchema("include_descriptions", "include_custom_variables")));
        tools.put("fancymenu_list_elements", tool("fancymenu_list_elements", "Lists element builders. Optional default serialized element payload.", registrySearchSchema("include_descriptions", "include_default_serialized", "include_deprecated")));
        tools.put("fancymenu_list_backgrounds", tool("fancymenu_list_backgrounds", "Lists background builders. Optional default serialized container payload.", registrySearchSchema("include_descriptions", "include_default_serialized", "include_deprecated")));
        tools.put("fancymenu_list_decoration_overlays", tool("fancymenu_list_decoration_overlays", "Lists decoration overlay builders. Optional default serialized container payload.", registrySearchSchema("include_descriptions", "include_default_serialized")));
        tools.put("fancymenu_list_panoramas", tool("fancymenu_list_panoramas", "Lists panoramas with optional path fields and query/limit/offset.", pagedQuerySchema("include_paths")));
        tools.put("fancymenu_list_slideshows", tool("fancymenu_list_slideshows", "Lists slideshows with optional path fields and query/limit/offset.", pagedQuerySchema("include_paths")));
        tools.put("fancymenu_list_screen_identifiers", tool("fancymenu_list_screen_identifiers", "Lists universal screen identifiers with query/limit/offset.", pagedQuerySchema()));
        tools.put("fancymenu_list_assets", tool("fancymenu_list_assets", "Lists files/directories in FancyMenu assets with filters (path prefix, query, extensions, recursion, pagination).", assetListSchema()));
        tools.put("fancymenu_get_registry_entry", tool("fancymenu_get_registry_entry", "Gets detailed info + defaults for a single registry entry (action/placeholder/requirement/listener/element/background/overlay).", registryEntrySchema()));

        tools.put("fancymenu_list_layouts_compact", tool("fancymenu_list_layouts_compact", "Recommended for discovery: compact layout list with counts and pagination.", layoutListCompactSchema()));
        tools.put("fancymenu_list_layouts", tool("fancymenu_list_layouts", "Lists layouts with optional filtering and optional serialized payload flags.", layoutListSchema()));
        tools.put("fancymenu_get_layout_meta", tool("fancymenu_get_layout_meta", "Returns compact metadata + counts for a single layout.", layoutMetaSchema()));
        tools.put("fancymenu_get_layout_elements", tool("fancymenu_get_layout_elements", "Returns paginated serialized-layout element entries for normal/vanilla/deep sections.", layoutElementsSchema()));
        tools.put("fancymenu_get_layout", tool("fancymenu_get_layout", "Returns a single layout. Disable include_serialized_set/include_fancy_string for low-context reads.", getLayoutSchema()));
        tools.put("fancymenu_create_layout", tool("fancymenu_create_layout", "Creates and saves a new layout, optionally opening editor.", createLayoutSchema()));
        tools.put("fancymenu_set_layout", tool("fancymenu_set_layout", "Replaces layout content from serialized payload and optionally opens editor.", setLayoutSchema()));
        tools.put("fancymenu_update_layout_meta", tool("fancymenu_update_layout_meta", "Patches metadata/settings of an existing layout and saves it.", updateLayoutMetaSchema()));
        tools.put("fancymenu_delete_layout", tool("fancymenu_delete_layout", "Deletes a layout by selector.", layoutSelectorSchema()));
        tools.put("fancymenu_save_layout", tool("fancymenu_save_layout", "Saves layout or current open editor layout.", layoutSelectorSchema()));
        tools.put("fancymenu_open_layout_editor", tool("fancymenu_open_layout_editor", "Opens the layout editor for a target layout and target screen.", layoutSelectorSchema()));
        tools.put("fancymenu_close_layout_editor", tool("fancymenu_close_layout_editor", "Closes current layout editor.", schemaObject()));

        tools.put("fancymenu_editor_get_state", tool("fancymenu_editor_get_state", "Returns live editor state. Disable include_layout/include_serialized_set for compact polling.", editorStateSchema()));
        tools.put("fancymenu_editor_poll", tool("fancymenu_editor_poll", "Returns compact editor poll data (fingerprint, counts, selection).", editorWithOptionalLayoutSelectorSchema()));
        tools.put("fancymenu_editor_get_element", tool("fancymenu_editor_get_element", "Gets one editor element by instance identifier.", editorGetElementSchema()));
        tools.put("fancymenu_editor_list_elements", tool("fancymenu_editor_list_elements", "Lists editor elements with filtering, pagination, and optional serialized payload.", editorListElementsSchema()));
        tools.put("fancymenu_editor_select_elements", tool("fancymenu_editor_select_elements", "Changes editor selection using modes set/add/remove/clear/all.", editorSelectElementsSchema()));
        tools.put("fancymenu_editor_get_visual_layers", tool("fancymenu_editor_get_visual_layers", "Lists backgrounds and decoration overlays in current editor order. Serialized payload is optional.", editorVisualLayersSchema()));
        tools.put("fancymenu_editor_patch_layout", tool("fancymenu_editor_patch_layout", "Batch patch tool for live layout editing in one call: meta patch + element/background/overlay operations + optional auto-save.", editorPatchLayoutSchema()));
        tools.put("fancymenu_editor_add_element", tool("fancymenu_editor_add_element", "Adds a new element by builder identifier to current editor.", editorAddElementSchema()));
        tools.put("fancymenu_editor_remove_element", tool("fancymenu_editor_remove_element", "Removes an element from current editor.", editorRemoveElementSchema()));
        tools.put("fancymenu_editor_move_element", tool("fancymenu_editor_move_element", "Moves/repositions an element in current editor.", editorMoveElementSchema()));
        tools.put("fancymenu_editor_set_element_order", tool("fancymenu_editor_set_element_order", "Sets order of normal editor elements based on identifier list.", editorSetElementOrderSchema()));
        tools.put("fancymenu_editor_update_element", tool("fancymenu_editor_update_element", "Replaces an element in current editor from serialized element payload.", editorUpdateElementSchema()));
        tools.put("fancymenu_editor_set_layout_from_serialized", tool("fancymenu_editor_set_layout_from_serialized", "Replaces current editor layout from serialized payload and live-updates editor.", editorSetLayoutSchema()));
        tools.put("fancymenu_editor_set_background", tool("fancymenu_editor_set_background", "Sets a background type instance in current editor from serialized container.", editorSetBackgroundSchema()));
        tools.put("fancymenu_editor_remove_background", tool("fancymenu_editor_remove_background", "Removes a background type from current editor layout.", editorRemoveBackgroundSchema()));
        tools.put("fancymenu_editor_reorder_background", tool("fancymenu_editor_reorder_background", "Moves a background type to a target order index in current editor layout.", editorReorderBackgroundSchema()));
        tools.put("fancymenu_editor_set_decoration_overlay", tool("fancymenu_editor_set_decoration_overlay", "Sets a decoration overlay instance in current editor from serialized container.", editorSetDecorationOverlaySchema()));
        tools.put("fancymenu_editor_remove_decoration_overlay", tool("fancymenu_editor_remove_decoration_overlay", "Removes a decoration overlay type from current editor layout.", editorRemoveDecorationOverlaySchema()));
        tools.put("fancymenu_editor_reorder_decoration_overlay", tool("fancymenu_editor_reorder_decoration_overlay", "Moves an overlay type to a target order index in current editor layout.", editorReorderDecorationOverlaySchema()));

        tools.put("fancymenu_open_action_editor", tool("fancymenu_open_action_editor", "Opens Action Script Editor for target script (layout/listener/scheduler).", actionTargetSchema()));
        tools.put("fancymenu_get_action_script", tool("fancymenu_get_action_script", "Gets script payload for target script.", actionTargetSchema()));
        tools.put("fancymenu_set_action_script", tool("fancymenu_set_action_script", "Sets target script and live-updates Action Script Editor.", actionTargetSchemaWithScript()));

        tools.put("fancymenu_list_listener_instances", tool("fancymenu_list_listener_instances", "Lists listener instances with optional filtering and serialized payload.", listenerListSchema()));
        tools.put("fancymenu_get_listener_instance", tool("fancymenu_get_listener_instance", "Gets one listener instance by identifier.", listenerGetSchema()));
        tools.put("fancymenu_create_listener_instance", tool("fancymenu_create_listener_instance", "Creates listener instance.", schemaWithRequired("listener_identifier")));
        tools.put("fancymenu_update_listener_instance", tool("fancymenu_update_listener_instance", "Updates listener instance.", schemaWithRequired("instance_identifier")));
        tools.put("fancymenu_set_listener_instance", tool("fancymenu_set_listener_instance", "Creates/replaces a listener instance from full serialized payload.", setListenerSchema()));
        tools.put("fancymenu_delete_listener_instance", tool("fancymenu_delete_listener_instance", "Deletes listener instance.", schemaWithRequired("instance_identifier")));

        tools.put("fancymenu_list_schedulers", tool("fancymenu_list_schedulers", "Lists scheduler instances with optional filtering and serialized payload.", schedulerListSchema()));
        tools.put("fancymenu_get_scheduler", tool("fancymenu_get_scheduler", "Gets one scheduler instance by identifier.", schedulerGetSchema()));
        tools.put("fancymenu_create_scheduler", tool("fancymenu_create_scheduler", "Creates scheduler instance.", schemaObject()));
        tools.put("fancymenu_update_scheduler", tool("fancymenu_update_scheduler", "Updates scheduler instance.", schemaWithRequired("identifier")));
        tools.put("fancymenu_set_scheduler", tool("fancymenu_set_scheduler", "Creates/replaces a scheduler instance from full serialized payload.", setSchedulerSchema()));
        tools.put("fancymenu_delete_scheduler", tool("fancymenu_delete_scheduler", "Deletes scheduler instance.", schemaWithRequired("identifier")));
        tools.put("fancymenu_start_scheduler", tool("fancymenu_start_scheduler", "Starts a scheduler instance.", schemaWithRequired("identifier")));
        tools.put("fancymenu_stop_scheduler", tool("fancymenu_stop_scheduler", "Stops a scheduler instance.", schemaWithRequired("identifier")));

        tools.put("fancymenu_list_variables", tool("fancymenu_list_variables", "Lists variables with optional filtering and serialized payload.", variableListSchema()));
        tools.put("fancymenu_get_variable", tool("fancymenu_get_variable", "Gets one variable by name.", variableGetSchema()));
        tools.put("fancymenu_set_variable", tool("fancymenu_set_variable", "Creates/updates variable.", schemaWithRequired("name")));
        tools.put("fancymenu_remove_variable", tool("fancymenu_remove_variable", "Removes variable by name.", schemaWithRequired("name")));
        tools.put("fancymenu_clear_variables", tool("fancymenu_clear_variables", "Clears all variables.", schemaObject()));

        tools.put("fancymenu_parse_fancy_properties", tool("fancymenu_parse_fancy_properties", "Parses FancyMenu properties string to JSON set.", schemaWithRequired("fancy_string")));
        tools.put("fancymenu_stringify_fancy_properties", tool("fancymenu_stringify_fancy_properties", "Serializes JSON set to FancyMenu properties string.", schemaWithRequired("set")));
        tools.put("fancymenu_normalize_requirement_container", tool("fancymenu_normalize_requirement_container", "Normalizes requirement container payload.", schemaWithRequired("requirement_container")));
        tools.put("fancymenu_check_requirement_container", tool("fancymenu_check_requirement_container", "Evaluates requirement container with current game state.", schemaWithRequired("requirement_container")));
        tools.put("fancymenu_evaluate_placeholders", tool("fancymenu_evaluate_placeholders", "Replaces placeholders in text using current runtime context.", schemaWithRequired("text")));

        return tools;
    }

    private static @NotNull JsonObject tool(@NotNull String name, @NotNull String description, @NotNull JsonObject inputSchema) {
        JsonObject tool = new JsonObject();
        tool.addProperty("name", name);
        tool.addProperty("description", description);
        tool.add("inputSchema", inputSchema);
        return tool;
    }

    private static @NotNull JsonObject schemaObject() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    private static @NotNull JsonObject schemaObject(@NotNull JsonObject properties, @NotNull String... requiredFields) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        if (requiredFields.length > 0) {
            JsonArray required = new JsonArray();
            for (String field : requiredFields) {
                required.add(field);
            }
            schema.add("required", required);
        }
        return schema;
    }

    private static @NotNull JsonObject schemaWithRequired(@NotNull String... requiredFields) {
        JsonObject schema = schemaObject();
        JsonArray required = new JsonArray();
        for (String field : requiredFields) {
            required.add(field);
        }
        schema.add("required", required);
        return schema;
    }

    private static @NotNull JsonObject screenshotSchema() {
        JsonObject properties = new JsonObject();
        properties.add("include_base64", booleanProperty("If false, only screenshot metadata is returned to reduce context usage."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject pagedQuerySchema(@NotNull String... extraFields) {
        JsonObject properties = new JsonObject();
        properties.add("query", stringProperty("Optional case-insensitive substring filter."));
        properties.add("offset", integerProperty("Pagination offset (default 0)."));
        properties.add("limit", integerProperty("Pagination limit."));
        for (String extraField : extraFields) {
            properties.add(extraField, booleanProperty("Optional boolean flag."));
        }
        return schemaObject(properties);
    }

    private static @NotNull JsonObject registrySearchSchema(@NotNull String... optionalFlags) {
        JsonObject properties = pagedQuerySchema().getAsJsonObject("properties");
        properties.add("identifiers_only", booleanProperty("Return only identifiers for very compact responses."));
        properties.add("include_deprecated", booleanProperty("Include deprecated entries when supported."));
        for (String flag : optionalFlags) {
            properties.add(flag, booleanProperty("Optional detail flag."));
        }
        return schemaObject(properties);
    }

    private static @NotNull JsonObject assetListSchema() {
        JsonObject properties = pagedQuerySchema().getAsJsonObject("properties");
        properties.add("path_prefix", stringProperty("Optional sub-directory path inside FancyMenu assets root."));
        properties.add("extensions", stringArrayProperty("Optional file extension filters (with or without dot)."));
        properties.add("recursive", booleanProperty("Scan recursively from path_prefix (default true)."));
        properties.add("max_depth", integerProperty("Maximum recursion depth when recursive=true."));
        properties.add("include_files", booleanProperty("Include files in results."));
        properties.add("include_directories", booleanProperty("Include directories in results."));
        properties.add("include_hidden", booleanProperty("Include hidden files/directories."));
        properties.add("include_absolute_path", booleanProperty("Include absolute file path in each result."));
        properties.add("include_size_bytes", booleanProperty("Include file size for files."));
        properties.add("include_modified_time", booleanProperty("Include last-modified timestamp."));
        properties.add("sort_by", enumStringProperty("Sort key.", "path", "name", "size", "modified"));
        properties.add("sort_order", enumStringProperty("Sort direction.", "asc", "desc"));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject registryEntrySchema() {
        JsonObject properties = new JsonObject();
        properties.add("registry", enumStringProperty("Registry namespace.", "action", "placeholder", "requirement", "listener", "element", "background", "overlay"));
        properties.add("identifier", stringProperty("Entry identifier."));
        return schemaObject(properties, "registry", "identifier");
    }

    private static @NotNull JsonObject layoutListCompactSchema() {
        JsonObject properties = pagedQuerySchema().getAsJsonObject("properties");
        properties.add("screen_identifier", stringProperty("Optional screen identifier filter."));
        properties.add("is_universal", booleanProperty("Optional filter for universal layouts."));
        properties.add("enabled", booleanProperty("Optional filter for enabled layouts."));
        properties.add("include_layout_file", booleanProperty("Include absolute layout file path in each item."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject layoutListSchema() {
        JsonObject properties = pagedQuerySchema().getAsJsonObject("properties");
        properties.add("screen_identifier", stringProperty("Optional screen identifier filter."));
        properties.add("is_universal", booleanProperty("Optional filter for universal layouts."));
        properties.add("enabled", booleanProperty("Optional filter for enabled layouts."));
        properties.add("include_serialized_set", booleanProperty("Include full serialized_set in each returned layout."));
        properties.add("include_fancy_string", booleanProperty("Include fancy_string payload for each returned layout."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject getLayoutSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("include_serialized_set", booleanProperty("Include serialized_set payload (default false for context efficiency)."));
        properties.add("include_fancy_string", booleanProperty("Include fancy_string payload (default false)."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject layoutMetaSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("include_requirements", booleanProperty("Include layout-wide requirement container."));
        properties.add("include_universal_lists", booleanProperty("Include universal whitelist/blacklist."));
        properties.add("include_layout_file", booleanProperty("Include layout file path."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject layoutElementsSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("section", enumStringProperty("Element section to query.", "normal", "vanilla", "deep", "all"));
        properties.add("builder_identifier", stringProperty("Optional element builder filter."));
        properties.add("query", stringProperty("Optional case-insensitive match for instance/builder id."));
        properties.add("include_properties", booleanProperty("Include flattened properties object."));
        properties.add("include_serialized", booleanProperty("Include full serialized element container."));
        properties.add("offset", integerProperty("Pagination offset."));
        properties.add("limit", integerProperty("Pagination limit."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject editorStateSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("sync_layout", booleanProperty("Sync editor element state back to layout before returning data."));
        properties.add("include_layout", booleanProperty("Include layout payload (default false)."));
        properties.add("include_serialized_set", booleanProperty("Include serialized_set when include_layout=true (default false)."));
        properties.add("include_fancy_string", booleanProperty("Include fancy_string when include_layout=true (default false)."));
        properties.add("include_counts", booleanProperty("Include selected/total element counters."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject editorGetElementSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("element_identifier", stringProperty("Instance identifier of target element."));
        properties.add("include_serialized_element", booleanProperty("Include serialized element payload."));
        return schemaObject(properties, "element_identifier");
    }

    private static @NotNull JsonObject editorListElementsSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("query", stringProperty("Optional case-insensitive instance/builder/display-name filter."));
        properties.add("builder_identifier", stringProperty("Optional builder identifier filter."));
        properties.add("selected_only", booleanProperty("If true, return only selected elements."));
        properties.add("include_vanilla_widgets", booleanProperty("Include vanilla widget elements list."));
        properties.add("include_serialized_element", booleanProperty("Include serialized element payload per item (default false)."));
        properties.add("offset", integerProperty("Pagination offset."));
        properties.add("limit", integerProperty("Pagination limit."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject editorVisualLayersSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("include_serialized_container", booleanProperty("Include serialized background/overlay containers (default false)."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject editorPatchLayoutSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("meta_patch", objectProperty("Optional layout metadata patch object (same fields as update_layout_meta, except selector/open_editor)."));
        properties.add("element_additions", objectArrayProperty("Array of element additions: builder_identifier/x/y/select/serialized_element."));
        properties.add("element_updates", objectArrayProperty("Array of element update objects (element_identifier + serialized_element)."));
        properties.add("element_moves", objectArrayProperty("Array of move objects (element_identifier + target_index/x/y)."));
        properties.add("element_removals", stringOrObjectArrayProperty("Array of element identifiers or remove objects."));
        properties.add("ordered_element_identifiers", stringArrayProperty("Optional full order list for normal elements."));
        properties.add("background_set", objectArrayProperty("Array of background set objects (background_identifier + background_container)."));
        properties.add("background_remove", stringOrObjectArrayProperty("Array of background identifiers or remove objects."));
        properties.add("background_reorder", objectArrayProperty("Array of background reorder objects (background_identifier + target_index)."));
        properties.add("overlay_set", objectArrayProperty("Array of overlay set objects (overlay_identifier + overlay_container)."));
        properties.add("overlay_remove", stringOrObjectArrayProperty("Array of overlay identifiers or remove objects."));
        properties.add("overlay_reorder", objectArrayProperty("Array of overlay reorder objects (overlay_identifier + target_index)."));
        properties.add("auto_save", booleanProperty("Save layout immediately after applying patch operations."));
        properties.add("include_editor_poll", booleanProperty("Include compact editor poll result in response."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject listenerListSchema() {
        JsonObject properties = pagedQuerySchema().getAsJsonObject("properties");
        properties.add("listener_identifier", stringProperty("Optional filter by listener type identifier."));
        properties.add("include_serialized", booleanProperty("Include serialized listener instance payload (default false)."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject listenerGetSchema() {
        JsonObject properties = new JsonObject();
        properties.add("instance_identifier", stringProperty("Listener instance identifier."));
        properties.add("include_serialized", booleanProperty("Include serialized listener instance payload."));
        return schemaObject(properties, "instance_identifier");
    }

    private static @NotNull JsonObject schedulerListSchema() {
        JsonObject properties = pagedQuerySchema().getAsJsonObject("properties");
        properties.add("include_serialized", booleanProperty("Include serialized scheduler payload (default false)."));
        properties.add("include_runtime_status", booleanProperty("Include running state."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject schedulerGetSchema() {
        JsonObject properties = new JsonObject();
        properties.add("identifier", stringProperty("Scheduler identifier."));
        properties.add("include_serialized", booleanProperty("Include serialized scheduler payload."));
        return schemaObject(properties, "identifier");
    }

    private static @NotNull JsonObject variableListSchema() {
        JsonObject properties = pagedQuerySchema().getAsJsonObject("properties");
        properties.add("include_serialized", booleanProperty("Include serialized variable payload (default false)."));
        properties.add("include_values", booleanProperty("Include variable values."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject variableGetSchema() {
        JsonObject properties = new JsonObject();
        properties.add("name", stringProperty("Variable name."));
        properties.add("include_serialized", booleanProperty("Include serialized variable payload."));
        return schemaObject(properties, "name");
    }

    private static @NotNull JsonObject layoutSelectorSchema() {
        JsonObject properties = new JsonObject();
        properties.add("runtime_layout_identifier", stringProperty("Preferred selector: runtime id from list/get tools."));
        properties.add("layout_file", stringProperty("Absolute layout file path."));
        properties.add("layout_name", stringProperty("Layout file name without extension."));
        properties.add("screen_identifier", stringProperty("Fallback selector: screen id."));
        properties.add("layout_index", integerProperty("Optional index when selecting by screen_identifier."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject createLayoutSchema() {
        JsonObject properties = new JsonObject();
        properties.add("universal", booleanProperty("Create universal layout."));
        properties.add("screen_identifier", stringProperty("Target screen identifier when universal=false."));
        properties.add("file_name", stringProperty("Preferred file name without extension."));
        properties.add("open_editor", booleanProperty("Open layout editor after create (default true)."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject setLayoutSchema() {
        JsonObject properties = new JsonObject();
        properties.add("runtime_layout_identifier", stringProperty("Target existing layout runtime id."));
        properties.add("layout_file", stringProperty("Target existing layout file."));
        properties.add("layout_name", stringProperty("Target existing layout name."));
        properties.add("serialized_set", objectProperty("PropertyContainerSet JSON payload."));
        properties.add("fancy_string", stringProperty("Alternative serialized payload."));
        properties.add("file_name", stringProperty("File name if creating a new layout."));
        properties.add("open_editor", booleanProperty("Open layout editor after applying (default true)."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject updateLayoutMetaSchema() {
        JsonObject properties = new JsonObject();
        properties.add("runtime_layout_identifier", stringProperty("Target layout runtime id."));
        properties.add("layout_file", stringProperty("Target layout file."));
        properties.add("layout_name", stringProperty("Target layout name."));
        properties.add("screen_identifier", stringProperty("Optional new screen identifier for the layout."));
        properties.add("layout_index", integerProperty("Layout index priority."));
        properties.add("enabled", booleanProperty("Enable/disable layout."));
        properties.add("render_elements_behind_vanilla", booleanProperty("Render custom elements behind vanilla widgets."));
        properties.add("random_mode", booleanProperty("Enable random layout mode."));
        properties.add("random_group", stringProperty("Random mode group id."));
        properties.add("random_only_first_time", booleanProperty("If true, random mode only applies first time."));
        properties.add("universal_layout_whitelist", stringArrayProperty("Universal whitelist identifiers."));
        properties.add("universal_layout_blacklist", stringArrayProperty("Universal blacklist identifiers."));
        properties.add("layout_wide_requirement_container", objectProperty("Requirement container object."));
        properties.add("custom_menu_title", stringOrNullProperty("Custom screen title text. Use null to clear."));
        properties.add("forced_scale", numberProperty("Forced GUI scale for this layout. Use 0 to disable."));
        properties.add("auto_scaling_width", integerProperty("Auto-scaling base width."));
        properties.add("auto_scaling_height", integerProperty("Auto-scaling base height."));
        properties.add("preserve_background_aspect_ratio", booleanProperty("Preserve background aspect ratio."));
        properties.add("open_audio_source", stringOrNullProperty("Audio source (with prefix) to play when opening screen. Use null to clear."));
        properties.add("close_audio_source", stringOrNullProperty("Audio source (with prefix) to play when closing screen. Use null to clear."));
        properties.add("preserve_scroll_list_header_footer_aspect_ratio", booleanProperty("Preserve scroll-list header/footer aspect ratio."));
        properties.add("repeat_scroll_list_header_texture", booleanProperty("Repeat scroll-list header texture."));
        properties.add("repeat_scroll_list_footer_texture", booleanProperty("Repeat scroll-list footer texture."));
        properties.add("scroll_list_header_texture_source", stringOrNullProperty("Scroll-list header texture source. Use null to clear."));
        properties.add("scroll_list_footer_texture_source", stringOrNullProperty("Scroll-list footer texture source. Use null to clear."));
        properties.add("render_scroll_list_header_shadow", booleanProperty("Render shadow under scroll-list header."));
        properties.add("render_scroll_list_footer_shadow", booleanProperty("Render shadow above scroll-list footer."));
        properties.add("show_scroll_list_header_footer_preview_in_editor", booleanProperty("Show header/footer preview in editor."));
        properties.add("show_screen_background_overlay_on_custom_background", booleanProperty("Render vanilla dark overlay on custom background."));
        properties.add("apply_vanilla_background_blur", booleanProperty("Apply vanilla background blur."));
        properties.add("open_editor", booleanProperty("Open/refresh editor for live visibility."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject customizationEngineSchema() {
        JsonObject properties = new JsonObject();
        properties.add("enabled", booleanProperty("Enable/disable screen customization engine."));
        properties.add("reinit_current_screen", booleanProperty("Reinitialize currently open screen after change."));
        return schemaObject(properties, "enabled");
    }

    private static @NotNull JsonObject currentScreenCustomizationSchema() {
        JsonObject properties = new JsonObject();
        properties.add("enabled", booleanProperty("Enable/disable customization for current screen."));
        properties.add("reinit_current_screen", booleanProperty("Reinitialize current screen after change."));
        return schemaObject(properties, "enabled");
    }

    private static @NotNull JsonObject screenClassCustomizationSchema() {
        JsonObject properties = new JsonObject();
        properties.add("screen_class", stringProperty("Fully qualified screen class name."));
        properties.add("enabled", booleanProperty("Enable/disable customization for this screen class."));
        properties.add("reinit_current_screen", booleanProperty("Reinitialize current screen when it matches."));
        return schemaObject(properties, "screen_class", "enabled");
    }

    private static @NotNull JsonObject editorOptionsSchema() {
        JsonObject properties = new JsonObject();
        properties.add("show_layout_editor_grid", booleanProperty("Show layout editor grid."));
        properties.add("layout_editor_grid_size", integerProperty("Grid size in pixels."));
        properties.add("layout_editor_grid_snapping", booleanProperty("Enable grid snapping."));
        properties.add("layout_editor_grid_snapping_strength", numberProperty("Grid snapping strength."));
        properties.add("show_all_anchor_overlay_connections", booleanProperty("Show all anchor overlay connection lines."));
        properties.add("anchor_overlay_change_anchor_on_area_hover", booleanProperty("Change anchor on area hover."));
        properties.add("anchor_overlay_change_anchor_on_element_hover", booleanProperty("Change anchor on element hover."));
        properties.add("invert_anchor_overlay_color", booleanProperty("Invert anchor overlay color."));
        properties.add("anchor_overlay_opacity_percentage_normal", numberProperty("Anchor overlay normal opacity."));
        properties.add("anchor_overlay_opacity_percentage_busy", numberProperty("Anchor overlay busy opacity."));
        properties.add("anchor_overlay_color_base_override", stringOrNullProperty("Anchor overlay base color override. Use null/empty to clear."));
        properties.add("anchor_overlay_color_border_override", stringOrNullProperty("Anchor overlay border color override. Use null/empty to clear."));
        properties.add("anchor_overlay_visibility_mode", stringProperty("Anchor overlay visibility mode."));
        properties.add("anchor_overlay_hover_charging_time_seconds", numberProperty("Anchor overlay hover charging time in seconds."));
        properties.add("enable_element_rotation_controls", booleanProperty("Enable element rotation controls."));
        properties.add("enable_element_tilting_controls", booleanProperty("Enable element tilting controls."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject editorWithOptionalLayoutSelectorSchema() {
        return schemaObject(withLayoutSelectorProperties(new JsonObject()));
    }

    private static @NotNull JsonObject editorAddElementSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("builder_identifier", stringProperty("Element builder identifier from fancymenu_list_elements."));
        properties.add("x", integerProperty("Optional x position in pixels."));
        properties.add("y", integerProperty("Optional y position in pixels."));
        properties.add("select", booleanProperty("Select the element after adding."));
        return schemaObject(properties, "builder_identifier");
    }

    private static @NotNull JsonObject editorRemoveElementSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("element_identifier", stringProperty("Instance identifier of an editor element."));
        return schemaObject(properties, "element_identifier");
    }

    private static @NotNull JsonObject editorMoveElementSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("element_identifier", stringProperty("Instance identifier of an editor element."));
        properties.add("target_index", integerProperty("Optional target layer index."));
        properties.add("x", integerProperty("Optional new x position in pixels."));
        properties.add("y", integerProperty("Optional new y position in pixels."));
        return schemaObject(properties, "element_identifier");
    }

    private static @NotNull JsonObject editorSetElementOrderSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("ordered_element_identifiers", stringArrayProperty("Ordered list of normal element instance identifiers."));
        return schemaObject(properties, "ordered_element_identifiers");
    }

    private static @NotNull JsonObject editorUpdateElementSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("element_identifier", stringProperty("Instance identifier of an editor element."));
        properties.add("serialized_element", objectProperty("Serialized element object containing at least element_type."));
        return schemaObject(properties, "element_identifier", "serialized_element");
    }

    private static @NotNull JsonObject editorSetLayoutSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("serialized_set", objectProperty("PropertyContainerSet JSON payload."));
        properties.add("fancy_string", stringProperty("Alternative serialized payload."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject editorSetBackgroundSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("background_identifier", stringProperty("Background type identifier."));
        properties.add("background_container", objectProperty("PropertyContainer payload for background."));
        return schemaObject(properties, "background_identifier", "background_container");
    }

    private static @NotNull JsonObject editorRemoveBackgroundSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("background_identifier", stringProperty("Background type identifier."));
        return schemaObject(properties, "background_identifier");
    }

    private static @NotNull JsonObject editorReorderBackgroundSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("background_identifier", stringProperty("Background type identifier."));
        properties.add("target_index", integerProperty("Target list index (0-based)."));
        return schemaObject(properties, "background_identifier", "target_index");
    }

    private static @NotNull JsonObject editorSetDecorationOverlaySchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("overlay_identifier", stringProperty("Decoration overlay type identifier."));
        properties.add("overlay_container", objectProperty("PropertyContainer payload for overlay."));
        return schemaObject(properties, "overlay_identifier", "overlay_container");
    }

    private static @NotNull JsonObject editorRemoveDecorationOverlaySchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("overlay_identifier", stringProperty("Decoration overlay type identifier."));
        return schemaObject(properties, "overlay_identifier");
    }

    private static @NotNull JsonObject editorReorderDecorationOverlaySchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("overlay_identifier", stringProperty("Decoration overlay type identifier."));
        properties.add("target_index", integerProperty("Target list index (0-based)."));
        return schemaObject(properties, "overlay_identifier", "target_index");
    }

    private static @NotNull JsonObject editorSelectElementsSchema() {
        JsonObject properties = withLayoutSelectorProperties(new JsonObject());
        properties.add("mode", enumStringProperty("Selection mode.", "set", "add", "remove", "clear", "all"));
        properties.add("element_identifiers", stringArrayProperty("Element instance identifiers used by set/add/remove."));
        return schemaObject(properties);
    }

    private static @NotNull JsonObject actionTargetSchema() {
        JsonObject properties = new JsonObject();
        properties.add("target_type", enumStringProperty("Action script target type.", "layout_open", "layout_close", "listener", "scheduler"));
        properties.add("target_identifier", stringProperty("Runtime id/name/path (required for listener/scheduler; optional for layout if selector fields provided)."));
        properties.add("runtime_layout_identifier", stringProperty("Layout selector fallback for layout_open/layout_close."));
        properties.add("layout_file", stringProperty("Layout selector fallback for layout_open/layout_close."));
        properties.add("layout_name", stringProperty("Layout selector fallback for layout_open/layout_close."));
        return schemaObject(properties, "target_type");
    }

    private static @NotNull JsonObject actionTargetSchemaWithScript() {
        JsonObject properties = actionTargetSchema().getAsJsonObject("properties");
        properties.add("script", objectProperty("Script payload from fancymenu_get_action_script."));
        return schemaObject(properties, "target_type", "script");
    }

    private static @NotNull JsonObject setListenerSchema() {
        JsonObject properties = new JsonObject();
        properties.add("serialized", objectProperty("Serialized listener instance container from list/get output."));
        properties.add("open_action_editor", booleanProperty("Open Action Script Editor after applying."));
        return schemaObject(properties, "serialized");
    }

    private static @NotNull JsonObject setSchedulerSchema() {
        JsonObject properties = new JsonObject();
        properties.add("serialized", objectProperty("Serialized scheduler instance container from list/get output."));
        properties.add("start_now", booleanProperty("Start scheduler immediately after applying."));
        properties.add("stop_now", booleanProperty("Stop scheduler immediately after applying."));
        properties.add("open_action_editor", booleanProperty("Open Action Script Editor after applying."));
        return schemaObject(properties, "serialized");
    }

    private static @NotNull JsonObject withLayoutSelectorProperties(@NotNull JsonObject properties) {
        properties.add("runtime_layout_identifier", stringProperty("Optional runtime layout id."));
        properties.add("layout_file", stringProperty("Optional absolute layout file path."));
        properties.add("layout_name", stringProperty("Optional layout name."));
        properties.add("screen_identifier", stringProperty("Optional selector by screen identifier."));
        properties.add("layout_index", integerProperty("Optional layout index used with screen_identifier."));
        return properties;
    }

    private static @NotNull JsonObject stringProperty(@NotNull String description) {
        JsonObject property = new JsonObject();
        property.addProperty("type", "string");
        property.addProperty("description", description);
        return property;
    }

    private static @NotNull JsonObject stringOrNullProperty(@NotNull String description) {
        JsonObject property = new JsonObject();
        JsonArray types = new JsonArray();
        types.add("string");
        types.add("null");
        property.add("type", types);
        property.addProperty("description", description);
        return property;
    }

    private static @NotNull JsonObject enumStringProperty(@NotNull String description, @NotNull String... values) {
        JsonObject property = stringProperty(description);
        JsonArray enums = new JsonArray();
        for (String value : values) {
            enums.add(value);
        }
        property.add("enum", enums);
        return property;
    }

    private static @NotNull JsonObject booleanProperty(@NotNull String description) {
        JsonObject property = new JsonObject();
        property.addProperty("type", "boolean");
        property.addProperty("description", description);
        return property;
    }

    private static @NotNull JsonObject integerProperty(@NotNull String description) {
        JsonObject property = new JsonObject();
        property.addProperty("type", "integer");
        property.addProperty("description", description);
        return property;
    }

    private static @NotNull JsonObject numberProperty(@NotNull String description) {
        JsonObject property = new JsonObject();
        property.addProperty("type", "number");
        property.addProperty("description", description);
        return property;
    }

    private static @NotNull JsonObject objectProperty(@NotNull String description) {
        JsonObject property = new JsonObject();
        property.addProperty("type", "object");
        property.addProperty("description", description);
        return property;
    }

    private static @NotNull JsonObject objectArrayProperty(@NotNull String description) {
        JsonObject items = new JsonObject();
        items.addProperty("type", "object");
        JsonObject property = new JsonObject();
        property.addProperty("type", "array");
        property.addProperty("description", description);
        property.add("items", items);
        return property;
    }

    private static @NotNull JsonObject stringOrObjectArrayProperty(@NotNull String description) {
        JsonObject primitiveItems = new JsonObject();
        primitiveItems.addProperty("type", "string");
        JsonObject objectItems = new JsonObject();
        objectItems.addProperty("type", "object");
        JsonObject items = new JsonObject();
        JsonArray anyOf = new JsonArray();
        anyOf.add(primitiveItems);
        anyOf.add(objectItems);
        items.add("anyOf", anyOf);
        JsonObject property = new JsonObject();
        property.addProperty("type", "array");
        property.addProperty("description", description);
        property.add("items", items);
        return property;
    }

    private static @NotNull JsonObject stringArrayProperty(@NotNull String description) {
        JsonObject items = new JsonObject();
        items.addProperty("type", "string");
        JsonObject property = new JsonObject();
        property.addProperty("type", "array");
        property.addProperty("description", description);
        property.add("items", items);
        return property;
    }

    static final class ToolExecution {
        private final @NotNull String toolName;
        private final @NotNull JsonObject structuredContent;
        private final @Nullable String imageBase64;
        private final @Nullable String imageMimeType;

        ToolExecution(@NotNull String toolName, @NotNull JsonObject structuredContent, @Nullable String imageBase64, @Nullable String imageMimeType) {
            this.toolName = toolName;
            this.structuredContent = structuredContent;
            this.imageBase64 = imageBase64;
            this.imageMimeType = imageMimeType;
        }

        @NotNull String getToolName() {
            return this.toolName;
        }

        @NotNull JsonObject getStructuredContent() {
            return this.structuredContent;
        }

        @Nullable String getImageBase64() {
            return this.imageBase64;
        }

        @Nullable String getImageMimeType() {
            return this.imageMimeType;
        }
    }
}
