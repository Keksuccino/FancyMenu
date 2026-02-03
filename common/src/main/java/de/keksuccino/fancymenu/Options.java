package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.util.AbstractOptions;
import de.keksuccino.konkrete.config.Config;

public class Options extends AbstractOptions {

    protected final Config config = new Config(FancyMenu.MOD_DIR.getAbsolutePath().replace("\\", "/") + "/options.txt");

    public final Option<Boolean> playVanillaMenuMusic = new Option<>(config, "play_vanilla_menu_music", true, "general");
    public final Option<Integer> defaultGuiScale = new Option<>(config, "default_gui_scale", -1, "general");
    public final Option<Boolean> forceFullscreen = new Option<>(config, "force_fullscreen", false, "general");

    public final Option<Boolean> advancedCustomizationMode = new Option<>(config, "advanced_customization_mode", false, "customization");
    public final Option<Boolean> showCustomizationOverlay = new Option<>(config, "show_customization_overlay", true, "customization");
    public final Option<Boolean> modpackMode = new Option<>(config, "modpack_mode", false, "customization");

    public final Option<String> gameIntroAnimation = new Option<>(config, "game_intro_animation_name", "", "loading");
    public final Option<Boolean> gameIntroAllowSkip = new Option<>(config, "allow_game_intro_skip", true, "loading");
    public final Option<Boolean> gameIntroFadeOut = new Option<>(config, "game_intro_fade_out", true, "loading");
    public final Option<String> gameIntroCustomSkipText = new Option<>(config, "custom_game_intro_skip_text", "", "loading");
    public final Option<String> preLoadResources = new Option<>(config, "preload_resources", "", "loading");

    public final Option<Boolean> showCustomWindowIcon = new Option<>(config, "show_custom_window_icon", false, "window");
    public final Option<String> customWindowIcon16 = new Option<>(config, "custom_window_icon_16", "", "window");
    public final Option<String> customWindowIcon32 = new Option<>(config, "custom_window_icon_32", "", "window");
    public final Option<String> customWindowIconMacOS = new Option<>(config, "custom_window_icon_macos", "", "window");
    public final Option<String> customWindowTitle = new Option<>(config, "custom_window_title", "", "window");
    public final Option<String> globalButtonBackgroundNormal = new Option<>(config, "global_button_background_normal", "", "global_customizations");
    public final Option<String> globalButtonBackgroundHover = new Option<>(config, "global_button_background_hover", "", "global_customizations");
    public final Option<String> globalButtonBackgroundInactive = new Option<>(config, "global_button_background_inactive", "", "global_customizations");
    public final Option<Boolean> globalButtonBackgroundNineSlice = new Option<>(config, "global_button_background_nine_slice", false, "global_customizations");
    public final Option<Integer> globalButtonBackgroundNineSliceBorderTop = new Option<>(config, "global_button_background_nine_slice_border_top", 5, "global_customizations");
    public final Option<Integer> globalButtonBackgroundNineSliceBorderRight = new Option<>(config, "global_button_background_nine_slice_border_right", 5, "global_customizations");
    public final Option<Integer> globalButtonBackgroundNineSliceBorderBottom = new Option<>(config, "global_button_background_nine_slice_border_bottom", 5, "global_customizations");
    public final Option<Integer> globalButtonBackgroundNineSliceBorderLeft = new Option<>(config, "global_button_background_nine_slice_border_left", 5, "global_customizations");
    public final Option<String> globalSliderBackground = new Option<>(config, "global_slider_background", "", "global_customizations");
    public final Option<Boolean> globalSliderBackgroundNineSlice = new Option<>(config, "global_slider_background_nine_slice", false, "global_customizations");
    public final Option<Integer> globalSliderBackgroundNineSliceBorderTop = new Option<>(config, "global_slider_background_nine_slice_border_top", 5, "global_customizations");
    public final Option<Integer> globalSliderBackgroundNineSliceBorderRight = new Option<>(config, "global_slider_background_nine_slice_border_right", 5, "global_customizations");
    public final Option<Integer> globalSliderBackgroundNineSliceBorderBottom = new Option<>(config, "global_slider_background_nine_slice_border_bottom", 5, "global_customizations");
    public final Option<Integer> globalSliderBackgroundNineSliceBorderLeft = new Option<>(config, "global_slider_background_nine_slice_border_left", 5, "global_customizations");
    public final Option<String> globalSliderHandleNormal = new Option<>(config, "global_slider_handle_normal", "", "global_customizations");
    public final Option<String> globalSliderHandleHover = new Option<>(config, "global_slider_handle_hover", "", "global_customizations");
    public final Option<String> globalSliderHandleInactive = new Option<>(config, "global_slider_handle_inactive", "", "global_customizations");
    public final Option<Boolean> globalSliderHandleNineSlice = new Option<>(config, "global_slider_handle_nine_slice", false, "global_customizations");
    public final Option<Integer> globalSliderHandleNineSliceBorderTop = new Option<>(config, "global_slider_handle_nine_slice_border_top", 5, "global_customizations");
    public final Option<Integer> globalSliderHandleNineSliceBorderRight = new Option<>(config, "global_slider_handle_nine_slice_border_right", 5, "global_customizations");
    public final Option<Integer> globalSliderHandleNineSliceBorderBottom = new Option<>(config, "global_slider_handle_nine_slice_border_bottom", 5, "global_customizations");
    public final Option<Integer> globalSliderHandleNineSliceBorderLeft = new Option<>(config, "global_slider_handle_nine_slice_border_left", 5, "global_customizations");
    public final Option<String> globalBackgroundPanorama = new Option<>(config, "global_background_panorama", "", "global_customizations");
    public final Option<String> globalMenuMusicTracks = new Option<>(config, "global_menu_music_tracks", "", "global_customizations");
    public final Option<String> globalButtonClickSound = new Option<>(config, "global_button_click_sound", "", "global_customizations");
    public final Option<String> globalMenuBackgroundTexture = new Option<>(config, "global_menu_background_texture", "", "global_customizations");

    public final Option<Boolean> showMultiplayerScreenServerIcons = new Option<>(config, "show_multiplayer_screen_server_icons", true, "multiplayer_screen");

    public final Option<Boolean> showSingleplayerScreenWorldIcons = new Option<>(config, "show_singleplayer_screen_world_icons", true, "singleplayer_screen");

    public final Option<Boolean> showLayoutEditorGrid = new Option<>(config, "show_layout_editor_grid", true, "layout_editor");
    public final Option<Integer> layoutEditorGridSize = new Option<>(config, "layout_editor_grid_size", 10, "layout_editor");
    public final Option<Boolean> layoutEditorGridSnapping = new Option<>(config, "layout_editor_grid_snapping", true, "layout_editor");
    public final Option<Float> layoutEditorGridSnappingStrength = new Option<>(config, "layout_editor_grid_snapping_strength", 1.0f, "layout_editor");
    public final Option<Boolean> showAllAnchorOverlayConnections = new Option<>(config, "anchor_overlay_show_all_connection_lines", false, "layout_editor");
    public final Option<Boolean> anchorOverlayChangeAnchorOnAreaHover = new Option<>(config, "anchor_overlay_change_anchor_on_area_hover", true, "layout_editor");
    public final Option<Boolean> anchorOverlayChangeAnchorOnElementHover = new Option<>(config, "anchor_overlay_change_anchor_on_element_hover", true, "layout_editor");
    public final Option<Boolean> invertAnchorOverlayColor = new Option<>(config, "invert_anchor_overlay_color", false, "layout_editor");
    public final Option<Float> anchorOverlayOpacityPercentageNormal = new Option<>(config, "anchor_overlay_opacity_normal", 0.5F, "layout_editor");
    public final Option<Float> anchorOverlayOpacityPercentageBusy = new Option<>(config, "anchor_overlay_opacity_busy", 0.7F, "layout_editor");
    public final Option<String> anchorOverlayColorBaseOverride = new Option<>(config, "anchor_overlay_color_base_override", "", "layout_editor");
    public final Option<String> anchorOverlayColorBorderOverride = new Option<>(config, "anchor_overlay_color_border_override", "", "layout_editor");
    public final Option<String> anchorOverlayVisibilityMode = new Option<>(config, "anchor_overlay_visibility_mode", "dragging", "layout_editor");
    public final Option<Double> anchorOverlayHoverChargingTimeSeconds = new Option<>(config, "anchor_overlay_hover_charging_time_seconds", 2.0D, "layout_editor");
    public final Option<Boolean> enableElementRotationControls = new Option<>(config, "enable_element_rotation_controls", true, "layout_editor");
    public final Option<Boolean> enableElementTiltingControls = new Option<>(config, "enable_element_tilting_controls", true, "layout_editor");

    public final Option<String> uiScale = new Option<>(config, "ui_scale_v2", "auto", "ui");
    public final Option<Boolean> playUiClickSounds = new Option<>(config, "play_ui_click_sounds", true, "ui");
    public final Option<Integer> contextMenuHoverOpenSpeed = new Option<>(config, "context_menu_hover_open_speed", 1, "ui");
    public final Option<String> uiTheme = new Option<>(config, "ui_theme", "dark", "ui");
    public final Option<Boolean> enableUiBlur = new Option<>(config, "enable_ui_blur", true, "ui");
    public final Option<Float> uiBlurIntensity = new Option<>(config, "ui_blur_intensity", 3.0F, "ui");
    public final Option<Boolean> useMinecraftFont = new Option<>(config, "use_minecraft_font", false, "ui");
    public final Option<Boolean> smoothFontMultilineRendering = new Option<>(config, "smooth_font_multiline_rendering", false, "ui");
    public final Option<Boolean> enableUiAnimations = new Option<>(config, "enable_ui_animations", true, "ui");

    public final Option<Boolean> showDebugOverlay = new Option<>(config, "show_debug_overlay", false, "debug_overlay");
    public final Option<Boolean> debugOverlayShowBasicScreenCategory = new Option<>(config, "debug_overlay_show_basic_screen_category", true, "debug_overlay");
    public final Option<Boolean> debugOverlayShowAdvancedScreenCategory = new Option<>(config, "debug_overlay_show_advanced_screen_category", true, "debug_overlay");
    public final Option<Boolean> debugOverlayShowResourcesCategory = new Option<>(config, "debug_overlay_show_resources_category", true, "debug_overlay");
    public final Option<Boolean> debugOverlayShowSystemCategory = new Option<>(config, "debug_overlay_show_system_category", true, "debug_overlay");

    public final Option<Boolean> showWelcomeScreen = new Option<>(config, "show_welcome_screen", true, "tutorial");

    public final Option<Boolean> arrowKeysMovePreview = new Option<>(config, "arrow_keys_move_preview", false, "keyframe_editor");

    public final Option<Long> placeholderCachingDurationMs = new Option<>(config, "placeholder_caching_duration_ms", 30L, "advanced");
    public final Option<Long> requirementCachingDurationMs = new Option<>(config, "requirement_caching_duration_ms", 0L, "advanced");

    public final Option<Boolean> devShowPipWindowDebug = new Option<>(config, "dev_pip_window_debug", false, "dev");

    public Options() {
        this.config.syncConfig();
        this.config.clearUnusedValues();
    }

}
