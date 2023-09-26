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
    public final Option<Boolean> showDebugOverlay = new Option<>(config, "show_debug_overlay", false, "customization");

    public final Option<String> gameIntroAnimation = new Option<>(config, "game_intro_animation_name", "", "loading");
    public final Option<Boolean> allowGameIntroSkip = new Option<>(config, "allow_game_intro_skip", true, "loading");
    public final Option<String> customGameIntroSkipText = new Option<>(config, "custom_game_intro_skip_text", "", "loading");
    public final Option<Boolean> preLoadAnimations = new Option<>(config, "preload_animations", true, "loading");

    public final Option<Boolean> showCustomWindowIcon = new Option<>(config, "show_custom_window_icon", false, "window");
    public final Option<String> customWindowIcon16 = new Option<>(config, "custom_window_icon_16", "", "window");
    public final Option<String> customWindowIcon32 = new Option<>(config, "custom_window_icon_32", "", "window");
    public final Option<String> customWindowIconMacOS = new Option<>(config, "custom_window_icon_macos", "", "window");
    public final Option<String> customWindowTitle = new Option<>(config, "custom_window_title", "", "window");

    public final Option<Boolean> showMultiplayerScreenServerIcons = new Option<>(config, "show_multiplayer_screen_server_icons", true, "multiplayer_screen");

    public final Option<Boolean> showSingleplayerScreenWorldIcons = new Option<>(config, "show_singleplayer_screen_world_icons", true, "singleplayer_screen");

    public final Option<Boolean> showLayoutEditorGrid = new Option<>(config, "show_layout_editor_grid", true, "layout_editor");
    public final Option<Integer> layoutEditorGridSize = new Option<>(config, "layout_editor_grid_size", 10, "layout_editor");
    public final Option<Boolean> showAnchorOverlay = new Option<>(config, "show_anchor_overlay", true, "layout_editor");
    public final Option<Boolean> alwaysShowAnchorOverlay = new Option<>(config, "always_show_anchor_overlay", false, "layout_editor");
    public final Option<Boolean> showAllAnchorConnections = new Option<>(config, "show_all_anchor_connections", false, "layout_editor");
    public final Option<Boolean> changeAnchorOnHover = new Option<>(config, "change_anchor_on_hover", false, "layout_editor");

    public final Option<Float> uiScale = new Option<>(config, "ui_scale", 4.0F, "ui");
    public final Option<Boolean> playUiClickSounds = new Option<>(config, "play_ui_click_sounds", true, "ui");
    public final Option<Boolean> enableUiTextShadow = new Option<>(config, "enable_ui_text_shadow", false, "ui");
    public final Option<String> uiTheme = new Option<>(config, "ui_theme", "dark", "ui");

    public Options() {
        this.config.syncConfig();
        this.config.clearUnusedValues();
    }

}
