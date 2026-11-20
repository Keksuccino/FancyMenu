package de.keksuccino.fancymenu.customization.decorationoverlay.preview;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * The settings of the dev-only decoration overlay preview, read from environment variables. See TESTING_GUIDELINES.md for their meaning.
 *
 * @param overlayType identifier of the overlay builder, like {@code snowfall}
 * @param layers value of the Layers setting, NULL to keep the overlay's default
 * @param properties overlay properties as they appear in layout files
 * @param shotSeconds when screenshots get taken, in seconds after the overlay first rendered, sorted
 * @param inputs simulated pointer input, sorted by time
 * @param outputDirectory absolute folder the screenshots go to
 * @param namePrefix start of the screenshot file names
 * @param guiWidth width of the screen in GUI pixels
 * @param guiHeight height of the screen in GUI pixels
 * @param guiScale GUI scale of the preview
 * @param backgroundColor {@code #RRGGBB} color of the backdrop, NULL for the vanilla panorama
 * @param fullResolutionShots true for screenshots at framebuffer size, false for one image pixel per GUI pixel
 * @param seed seed of the overlays' random numbers, NULL for random seeds
 */
public record DecorationOverlayPreviewConfig(@NotNull String overlayType, @Nullable String layers, @NotNull Map<String, String> properties, @NotNull List<Float> shotSeconds, @NotNull List<Input> inputs, @NotNull Path outputDirectory, @NotNull String namePrefix, int guiWidth, int guiHeight, int guiScale, @Nullable String backgroundColor, boolean fullResolutionShots, @Nullable Integer seed) {

    public static final String ENV_OVERLAY = "FANCYMENU_OVERLAY_PREVIEW";
    public static final String ENV_OUTPUT = "FANCYMENU_OVERLAY_PREVIEW_OUT";
    public static final String ENV_LAYERS = "FANCYMENU_OVERLAY_PREVIEW_LAYERS";
    public static final String ENV_PROPERTIES = "FANCYMENU_OVERLAY_PREVIEW_PROPERTIES";
    public static final String ENV_SHOTS = "FANCYMENU_OVERLAY_PREVIEW_SHOTS";
    public static final String ENV_INPUT = "FANCYMENU_OVERLAY_PREVIEW_INPUT";
    public static final String ENV_NAME = "FANCYMENU_OVERLAY_PREVIEW_NAME";
    public static final String ENV_SIZE = "FANCYMENU_OVERLAY_PREVIEW_SIZE";
    public static final String ENV_GUI_SCALE = "FANCYMENU_OVERLAY_PREVIEW_GUI_SCALE";
    public static final String ENV_BACKGROUND = "FANCYMENU_OVERLAY_PREVIEW_BACKGROUND";
    public static final String ENV_SHOT_SCALE = "FANCYMENU_OVERLAY_PREVIEW_SHOT_SCALE";
    public static final String ENV_SEED = "FANCYMENU_OVERLAY_PREVIEW_SEED";

    public static final String DEFAULT_BACKGROUND = "#1B2231";

    /**
     * Whether the preview was asked for. The preview only runs in off-screen test clients.
     */
    public static boolean isRequested(@NotNull UnaryOperator<String> environment) {
        String overlay = environment.apply(ENV_OVERLAY);
        return (overlay != null) && !overlay.isBlank();
    }

    /**
     * Reads the settings. Throws an {@link IllegalArgumentException} that explains what is wrong with a setting.
     */
    @NotNull
    public static DecorationOverlayPreviewConfig read(@NotNull UnaryOperator<String> environment) {
        String overlayType = require(environment, ENV_OVERLAY).trim();
        String output = require(environment, ENV_OUTPUT).trim();
        Path outputDirectory = Path.of(output);
        // A relative folder would end up in the game directory, which the preview must leave as it found it
        if (!outputDirectory.isAbsolute()) throw new IllegalArgumentException(ENV_OUTPUT + " has to be an absolute path: " + output);
        String layers = optional(environment, ENV_LAYERS);
        if (layers != null) {
            layers = layers.toLowerCase(Locale.ROOT);
            if (!layers.equals("background_and_foreground") && !layers.equals("only_background") && !layers.equals("only_foreground")) {
                throw new IllegalArgumentException(ENV_LAYERS + " has to be background_and_foreground, only_background or only_foreground: " + layers);
            }
        }
        Map<String, String> properties = parseProperties(optional(environment, ENV_PROPERTIES));
        List<Float> shots = parseShots(optional(environment, ENV_SHOTS));
        List<Input> inputs = parseInputs(optional(environment, ENV_INPUT));
        String size = optional(environment, ENV_SIZE);
        int guiWidth = 480;
        int guiHeight = 270;
        if (size != null) {
            String[] parts = size.toLowerCase(Locale.ROOT).split("x");
            if (parts.length != 2) throw new IllegalArgumentException(ENV_SIZE + " has to look like 480x270: " + size);
            guiWidth = parsePositiveInt(parts[0], ENV_SIZE);
            guiHeight = parsePositiveInt(parts[1], ENV_SIZE);
        }
        String scale = optional(environment, ENV_GUI_SCALE);
        int guiScale = (scale != null) ? parsePositiveInt(scale, ENV_GUI_SCALE) : 4;
        String background = optional(environment, ENV_BACKGROUND);
        if (background == null) {
            background = DEFAULT_BACKGROUND;
        } else if (background.equalsIgnoreCase("panorama")) {
            background = null;
        } else if (!background.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException(ENV_BACKGROUND + " has to be a #RRGGBB color or 'panorama': " + background);
        }
        String shotScale = optional(environment, ENV_SHOT_SCALE);
        if ((shotScale != null) && !shotScale.equalsIgnoreCase("gui") && !shotScale.equalsIgnoreCase("full")) {
            throw new IllegalArgumentException(ENV_SHOT_SCALE + " has to be 'gui' or 'full': " + shotScale);
        }
        boolean fullResolution = "full".equalsIgnoreCase(shotScale);
        String seedText = optional(environment, ENV_SEED);
        Integer seed = null;
        if (seedText != null) {
            try {
                seed = Integer.parseInt(seedText.trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(ENV_SEED + " has to be a whole number: " + seedText);
            }
        }
        String name = optional(environment, ENV_NAME);
        if (name == null) name = overlayType + "_" + ((layers != null) ? layers : "default");
        name = name.replaceAll("[^A-Za-z0-9_.-]", "_");
        return new DecorationOverlayPreviewConfig(overlayType, layers, properties, shots, inputs, outputDirectory, name, guiWidth, guiHeight, guiScale, background, fullResolution, seed);
    }

    /**
     * Seconds after which the preview has done everything: the last screenshot or input.
     */
    public float getEndSeconds() {
        float end = 0.0F;
        if (!this.shotSeconds.isEmpty()) end = this.shotSeconds.getLast();
        if (!this.inputs.isEmpty()) end = Math.max(end, this.inputs.getLast().seconds());
        return end;
    }

    @NotNull
    private static String require(@NotNull UnaryOperator<String> environment, @NotNull String key) {
        String value = optional(environment, key);
        if (value == null) throw new IllegalArgumentException(key + " is not set");
        return value;
    }

    @Nullable
    private static String optional(@NotNull UnaryOperator<String> environment, @NotNull String key) {
        String value = environment.apply(key);
        return ((value == null) || value.isBlank()) ? null : value;
    }

    private static int parsePositiveInt(@NotNull String text, @NotNull String key) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value > 0) return value;
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException(key + " needs a whole number above 0: " + text);
    }

    private static float parseSeconds(@NotNull String text, @NotNull String key) {
        try {
            float value = Float.parseFloat(text.trim());
            if (value >= 0.0F) return value;
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException(key + " needs seconds of 0 or more: " + text);
    }

    @NotNull
    static Map<String, String> parseProperties(@Nullable String text) {
        Map<String, String> properties = new LinkedHashMap<>();
        if (text == null) return properties;
        for (String pair : text.split(";")) {
            if (pair.isBlank()) continue;
            int equals = pair.indexOf('=');
            if (equals <= 0) throw new IllegalArgumentException(ENV_PROPERTIES + " needs key=value pairs separated by ';': " + pair);
            properties.put(pair.substring(0, equals).trim(), pair.substring(equals + 1).trim());
        }
        return properties;
    }

    @NotNull
    static List<Float> parseShots(@Nullable String text) {
        List<Float> shots = new ArrayList<>();
        if (text == null) {
            shots.add(3.0F);
            return shots;
        }
        for (String part : text.split(",")) {
            if (!part.isBlank()) shots.add(parseSeconds(part, ENV_SHOTS));
        }
        shots.sort(Comparator.naturalOrder());
        return shots;
    }

    @NotNull
    static List<Input> parseInputs(@Nullable String text) {
        List<Input> inputs = new ArrayList<>();
        if (text == null) return inputs;
        for (String entry : text.split(";")) {
            if (entry.isBlank()) continue;
            String[] parts = entry.trim().split(":", 3);
            if (parts.length < 2) throw new IllegalArgumentException(ENV_INPUT + " entries look like <seconds>:<action>[:<arguments>]: " + entry);
            float seconds = parseSeconds(parts[0], ENV_INPUT);
            InputAction action = InputAction.byName(parts[1].trim());
            if (action == null) throw new IllegalArgumentException(ENV_INPUT + " has an unknown action '" + parts[1] + "', use move, click, click_widget, title or set");
            String argument = (parts.length > 2) ? parts[2].trim() : "";
            float x = 0.0F;
            float y = 0.0F;
            if ((action == InputAction.MOVE) || (action == InputAction.CLICK)) {
                String[] position = argument.split(",");
                if (position.length != 2) throw new IllegalArgumentException(ENV_INPUT + " needs a GUI position like 240,135 for '" + parts[1] + "': " + entry);
                try {
                    x = Float.parseFloat(position[0].trim());
                    y = Float.parseFloat(position[1].trim());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(ENV_INPUT + " needs a GUI position like 240,135 for '" + parts[1] + "': " + entry);
                }
            } else if ((action == InputAction.CLICK_WIDGET) && argument.isEmpty()) {
                throw new IllegalArgumentException(ENV_INPUT + " needs a widget identifier or label for click_widget: " + entry);
            } else if ((action == InputAction.SET) && (argument.indexOf('=') <= 0)) {
                throw new IllegalArgumentException(ENV_INPUT + " needs an overlay property like grass=false for set: " + entry);
            }
            inputs.add(new Input(seconds, action, x, y, argument));
        }
        inputs.sort(Comparator.comparingDouble(Input::seconds));
        return inputs;
    }

    public enum InputAction {

        /** Moves the pointer to a GUI position. */
        MOVE("move"),
        /** Moves the pointer to a GUI position and clicks the left mouse button there. */
        CLICK("click"),
        /** Clicks the middle of a widget, found by its FancyMenu widget identifier or its label. */
        CLICK_WIDGET("click_widget"),
        /** Opens the title screen with the preview again, for example after a click opened another screen. */
        TITLE("title"),
        /** Sets an overlay property while the overlay runs, like switching an option in the editor. The argument is {@code key=value}. */
        SET("set");

        @NotNull
        private final String name;

        InputAction(@NotNull String name) {
            this.name = name;
        }

        @Nullable
        public static InputAction byName(@NotNull String name) {
            for (InputAction action : values()) {
                if (action.name.equalsIgnoreCase(name)) return action;
            }
            return null;
        }

        @NotNull
        public String getName() {
            return this.name;
        }

    }

    /**
     * One simulated input.
     *
     * @param x GUI position of move and click
     * @param y GUI position of move and click
     * @param argument widget identifier or label of click_widget, {@code key=value} of set
     */
    public record Input(float seconds, @NotNull InputAction action, float x, float y, @NotNull String argument) {
    }

}
