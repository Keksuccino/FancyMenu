package de.keksuccino.fancymenu.util.window.preview;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * The settings of the dev-only window preview, read from environment variables. See TESTING_GUIDELINES.md for their meaning.
 *
 * @param target what the preview opens: {@code manage_variables}, {@code action:<identifier>}, {@code requirement:<identifier>} or {@code opener:<class>#<method>}
 * @param value value of the action or requirement instance whose editor gets opened, NULL for the preset of the action or requirement
 * @param variables JSON object of variables that replace all variables before the window opens, NULL to keep the variables as they are
 * @param shotSeconds when screenshots get taken, in seconds after the window opened, sorted
 * @param inputs simulated input, sorted by time
 * @param outputDirectory absolute folder the screenshots go to
 * @param namePrefix start of the screenshot file names
 * @param guiWidth width of the screen in GUI pixels
 * @param guiHeight height of the screen in GUI pixels
 * @param guiScale GUI scale of the preview
 */
public record WindowPreviewConfig(@NotNull String target, @Nullable String value, @Nullable String variables, @NotNull List<Float> shotSeconds, @NotNull List<Input> inputs, @NotNull Path outputDirectory, @NotNull String namePrefix, int guiWidth, int guiHeight, int guiScale) {

    public static final String ENV_TARGET = "FANCYMENU_WINDOW_PREVIEW";
    public static final String ENV_OUTPUT = "FANCYMENU_WINDOW_PREVIEW_OUT";
    public static final String ENV_VALUE = "FANCYMENU_WINDOW_PREVIEW_VALUE";
    public static final String ENV_VARIABLES = "FANCYMENU_WINDOW_PREVIEW_VARIABLES";
    public static final String ENV_SHOTS = "FANCYMENU_WINDOW_PREVIEW_SHOTS";
    public static final String ENV_INPUT = "FANCYMENU_WINDOW_PREVIEW_INPUT";
    public static final String ENV_NAME = "FANCYMENU_WINDOW_PREVIEW_NAME";
    public static final String ENV_SIZE = "FANCYMENU_WINDOW_PREVIEW_SIZE";
    public static final String ENV_GUI_SCALE = "FANCYMENU_WINDOW_PREVIEW_GUI_SCALE";

    /**
     * Whether the preview was asked for. The preview only runs in off-screen test clients.
     */
    public static boolean isRequested(@NotNull UnaryOperator<String> environment) {
        String target = environment.apply(ENV_TARGET);
        return (target != null) && !target.isBlank();
    }

    /**
     * Reads the settings. Throws an {@link IllegalArgumentException} that explains what is wrong with a setting.
     */
    @NotNull
    public static WindowPreviewConfig read(@NotNull UnaryOperator<String> environment) {
        String target = require(environment, ENV_TARGET).trim();
        String output = require(environment, ENV_OUTPUT).trim();
        Path outputDirectory = Path.of(output);
        // A relative folder would end up in the game directory
        if (!outputDirectory.isAbsolute()) throw new IllegalArgumentException(ENV_OUTPUT + " has to be an absolute path: " + output);
        // The value of an instance can be anything, also only spaces
        String value = environment.apply(ENV_VALUE);
        String variables = optional(environment, ENV_VARIABLES);
        List<Float> shots = parseShots(optional(environment, ENV_SHOTS));
        List<Input> inputs = parseInputs(optional(environment, ENV_INPUT));
        String size = optional(environment, ENV_SIZE);
        int guiWidth = 960;
        int guiHeight = 540;
        if (size != null) {
            String[] parts = size.toLowerCase(Locale.ROOT).split("x");
            if (parts.length != 2) throw new IllegalArgumentException(ENV_SIZE + " has to look like 960x540: " + size);
            guiWidth = parsePositiveInt(parts[0], ENV_SIZE);
            guiHeight = parsePositiveInt(parts[1], ENV_SIZE);
        }
        String scale = optional(environment, ENV_GUI_SCALE);
        int guiScale = (scale != null) ? parsePositiveInt(scale, ENV_GUI_SCALE) : 2;
        String name = optional(environment, ENV_NAME);
        if (name == null) name = target;
        name = name.replaceAll("[^A-Za-z0-9_.-]", "_");
        return new WindowPreviewConfig(target, value, variables, shots, inputs, outputDirectory, name, guiWidth, guiHeight, guiScale);
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
    static List<Float> parseShots(@Nullable String text) {
        List<Float> shots = new ArrayList<>();
        if (text == null) {
            shots.add(2.0F);
            return shots;
        }
        for (String part : text.split(",")) {
            if (!part.isBlank()) shots.add(parseSeconds(part, ENV_SHOTS));
        }
        shots.sort(Comparator.naturalOrder());
        return shots;
    }

    /**
     * Splits the input entries at every {@code ;} that has no backslash in front of it, so typed text can contain {@code \;}.
     */
    @NotNull
    static List<String> splitEntries(@NotNull String text) {
        List<String> entries = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '\\') && ((i + 1) < text.length()) && (text.charAt(i + 1) == ';')) {
                current.append(';');
                i++;
            } else if (c == ';') {
                entries.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        entries.add(current.toString());
        return entries;
    }

    @NotNull
    static List<Input> parseInputs(@Nullable String text) {
        List<Input> inputs = new ArrayList<>();
        if (text == null) return inputs;
        for (String entry : splitEntries(text)) {
            if (entry.isBlank()) continue;
            String[] parts = entry.stripLeading().split(":", 3);
            if (parts.length < 2) throw new IllegalArgumentException(ENV_INPUT + " entries look like <seconds>:<action>[:<arguments>]: " + entry);
            float seconds = parseSeconds(parts[0], ENV_INPUT);
            InputAction action = InputAction.byName(parts[1].trim());
            if (action == null) throw new IllegalArgumentException(ENV_INPUT + " has an unknown action '" + parts[1] + "', use " + InputAction.describeAll());
            // Typed text keeps its spaces, everything else gets trimmed
            String argument = (parts.length > 2) ? ((action == InputAction.TYPE) ? parts[2] : parts[2].trim()) : "";
            float[] numbers = new float[0];
            if (action.numberCount > 0) {
                String[] split = argument.split(",");
                if (split.length != action.numberCount) throw new IllegalArgumentException(ENV_INPUT + " needs " + action.numberCount + " comma-separated numbers for '" + action.name + "': " + entry);
                numbers = new float[split.length];
                for (int i = 0; i < split.length; i++) {
                    try {
                        numbers[i] = Float.parseFloat(split[i].trim());
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException(ENV_INPUT + " needs " + action.numberCount + " comma-separated numbers for '" + action.name + "': " + entry);
                    }
                }
            } else if ((action == InputAction.KEY) && argument.isEmpty()) {
                throw new IllegalArgumentException(ENV_INPUT + " needs a key like enter or shortcut+z for key: " + entry);
            }
            inputs.add(new Input(seconds, action, numbers, argument));
        }
        inputs.sort(Comparator.comparingDouble(Input::seconds));
        return inputs;
    }

    public enum InputAction {

        /** Moves the pointer to a GUI position: {@code x,y}. */
        MOVE("move", 2),
        /** Left-clicks a GUI position: {@code x,y}. */
        CLICK("click", 2),
        /** Right-clicks a GUI position: {@code x,y}. */
        RIGHT_CLICK("right_click", 2),
        /** Left-clicks a GUI position twice, fast enough for a double-click: {@code x,y}. */
        DOUBLE_CLICK("double_click", 2),
        /** Presses the left button at one GUI position, moves to the other one and releases it there: {@code x1,y1,x2,y2}. */
        DRAG("drag", 4),
        /** Scrolls at a GUI position, positive amounts scroll up: {@code x,y,amount}. */
        SCROLL("scroll", 3),
        /** Presses and releases a key, with optional modifiers: {@code shortcut+z}, {@code shift+tab}, {@code enter}, {@code a}. */
        KEY("key", 0),
        /** Types text into whatever has the focus. A {@code ;} in the text has to be written as {@code \;}. */
        TYPE("type", 0);

        @NotNull
        private final String name;
        private final int numberCount;

        InputAction(@NotNull String name, int numberCount) {
            this.name = name;
            this.numberCount = numberCount;
        }

        @Nullable
        public static InputAction byName(@NotNull String name) {
            for (InputAction action : values()) {
                if (action.name.equalsIgnoreCase(name)) return action;
            }
            return null;
        }

        @NotNull
        static String describeAll() {
            List<String> names = new ArrayList<>();
            for (InputAction action : values()) names.add(action.name);
            return String.join(", ", names);
        }

        @NotNull
        public String getName() {
            return this.name;
        }

    }

    /**
     * One simulated input.
     *
     * @param numbers the GUI positions (and the scroll amount) of pointer input
     * @param argument the key of key, the text of type
     */
    public record Input(float seconds, @NotNull InputAction action, float @NotNull [] numbers, @NotNull String argument) {
    }

}
