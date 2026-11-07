package de.keksuccino.fancymenu.customization.profile;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Options that describe how a user likes FancyMenu's own UI, like the UI scale, the UI theme or the editor grid.<br>
 * New empty profiles get them from the active profile, so starting a new design doesn't reset the UI or show the welcome screen again.
 * Profiles made by others keep their own options, because a profile is always a complete FancyMenu config directory.
 */
public final class ProfilePersonalOptions {

    static final String OPTIONS_FILE_NAME = "options.txt";
    /** The option categories of the options file that only hold personal preferences. They match the categories in the Options class. */
    private static final Set<String> PERSONAL_CATEGORIES = Set.of("ui", "layout_editor", "keyframe_editor", "tutorial");

    private ProfilePersonalOptions() {
    }

    /**
     * Copies all personal option categories of an options file into a new options file. Missing options get their defaults when FancyMenu loads the file.
     */
    public static void copyPersonalCategories(@NotNull Path sourceOptions, @NotNull Path targetOptions) throws IOException {
        if (!Files.isRegularFile(sourceOptions)) return;
        List<String> output = new ArrayList<>();
        boolean inPersonalCategory = false;
        for (String line : Files.readAllLines(sourceOptions, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("##[") && trimmed.endsWith("]")) {
                inPersonalCategory = PERSONAL_CATEGORIES.contains(trimmed.substring(3, trimmed.length() - 1));
            }
            if (inPersonalCategory) output.add(line);
        }
        if (output.isEmpty()) return;
        Files.write(targetOptions, output, StandardCharsets.UTF_8);
    }

}
