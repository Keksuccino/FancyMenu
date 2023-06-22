package de.keksuccino.fancymenu;

import de.keksuccino.konkrete.config.Config;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Options {

    protected final Config config = new Config(FancyMenu.MOD_DIR.getAbsolutePath().replace("\\", "/") + "/config.txt");

    public final Option<Boolean> playMenuMusic = new Option<>(config, "play_menu_music", true, "general");
    public final Option<Integer> defaultGuiScale = new Option<>(config, "default_gui_scale", -1, "general");
    public final Option<Boolean> forceFullscreen = new Option<>(config, "force_fullscreen", false, "general");

    public final Option<Boolean> advancedCustomizationMode = new Option<>(config, "advanced_customization_mode", false, "customization");

    public final Option<String> gameIntroAnimation = new Option<>(config, "game_intro_animation_name", "", "loading");
    public final Option<Boolean> allowGameIntroSkip = new Option<>(config, "allow_game_intro_skip", true, "loading");
    public final Option<String> customGameIntroSkipText = new Option<>(config, "custom_game_intro_skip_text", "", "loading");
    public final Option<Boolean> preLoadAnimations = new Option<>(config, "preload_animations", true, "loading");

    public final Option<Boolean> showCustomWindowIcon = new Option<>(config, "show_custom_window_icon", false, "window");
    public final Option<String> customWindowTitle = new Option<>(config, "custom_window_title", "", "window");

    public final Option<Boolean> showWorldLoadingScreenAnimation = new Option<>(config, "show_world_loading_screen_animation", true, "world_loading_screen");
    public final Option<Boolean> showWorldLoadingScreenPercent = new Option<>(config, "show_world_loading_screen_percent", true, "world_loading_screen");

    public final Option<Boolean> showMultiplayerScreenServerIcons = new Option<>(config, "show_multiplayer_screen_server_icons", true, "multiplayer_screen");

    public final Option<Boolean> showSingleplayerScreenWorldIcons = new Option<>(config, "show_singleplayer_screen_world_icons", true, "singleplayer_screen");

    public final Option<Boolean> showLayoutEditorGrid = new Option<>(config, "show_layout_editor_grid", true, "layout_editor");
    public final Option<Integer> layoutEditorGridSize = new Option<>(config, "layout_editor_grid_size", 10, "layout_editor");

    public final Option<Float> uiScale = new Option<>(config, "ui_scale", 1.0F, "ui");
    public final Option<Boolean> playUiClickSounds = new Option<>(config, "play_ui_click_sounds", true, "ui");
    public final Option<Boolean> enableUiTextShadow = new Option<>(config, "enable_ui_text_shadow", false, "ui");
    public final Option<String> uiTheme = new Option<>(config, "ui_theme", "dark", "ui");

    public Options() {
        this.config.syncConfig();
        this.config.clearUnusedValues();
    }

    @SuppressWarnings("unused")
    public static class Option<T> {

        protected final Config config;
        protected final String key;
        protected final T defaultValue;
        protected final String category;

        protected Option(@NotNull Config config, @NotNull String key, @NotNull T defaultValue, @NotNull String category) {
            this.config = Objects.requireNonNull(config);
            this.key = Objects.requireNonNull(key);
            this.defaultValue = Objects.requireNonNull(defaultValue);
            this.category = Objects.requireNonNull(category);
            this.register();
        }

        protected void register() {
            boolean unsupported = false;
            try {
                if (this.defaultValue instanceof Integer) {
                    this.config.registerValue(this.key, (int) this.defaultValue, this.category);
                } else if (this.defaultValue instanceof Double) {
                    this.config.registerValue(this.key, (double) this.defaultValue, this.category);
                } else if (this.defaultValue instanceof Long) {
                    this.config.registerValue(this.key, (long) this.defaultValue, this.category);
                } else if (this.defaultValue instanceof Float) {
                    this.config.registerValue(this.key, (float) this.defaultValue, this.category);
                } else if (this.defaultValue instanceof Boolean) {
                    this.config.registerValue(this.key, (boolean) this.defaultValue, this.category);
                } else if (this.defaultValue instanceof String) {
                    this.config.registerValue(this.key, (String) this.defaultValue, this.category);
                } else {
                    unsupported = true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (unsupported) throw new UnsupportedOptionTypeException("Tried to register Option of unsupported type: " + this.key + " (" + this.defaultValue.getClass().getName() + ")");
        }

        @NotNull
        public T getValue() {
            return this.config.getOrDefault(this.key, this.defaultValue);
        }

        public Option<T> setValue(T value) {
            try {
                if (value == null) value = this.getDefaultValue();
                if (value instanceof Integer) {
                    this.config.setValue(this.key, (int) value);
                } else if (value instanceof Double) {
                    this.config.setValue(this.key, (double) value);
                } else if (value instanceof Long) {
                    this.config.setValue(this.key, (long) value);
                } else if (value instanceof Float) {
                    this.config.setValue(this.key, (float) value);
                } else if (value instanceof Boolean) {
                    this.config.setValue(this.key, (boolean) value);
                } else if (value instanceof String) {
                    this.config.setValue(this.key, (String) value);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return this;
        }

        public Option<T> resetToDefault() {
            this.setValue(null);
            return this;
        }

        @NotNull
        public T getDefaultValue() {
            return this.defaultValue;
        }

        @NotNull
        public String getKey() {
            return this.key;
        }

    }

    /**
     * Thrown when trying to register an Option with an unsupported type.
     */
    @SuppressWarnings("unused")
    public static class UnsupportedOptionTypeException extends RuntimeException {

        public UnsupportedOptionTypeException() {
            super();
        }

        public UnsupportedOptionTypeException(String msg) {
            super(msg);
        }

    }

}
