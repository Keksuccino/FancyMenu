package de.keksuccino.fancymenu.customization.aiexport;

import de.keksuccino.fancymenu.FancyMenu;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Saves AI exports as Markdown files. Works on clients and dedicated servers.
 */
public final class AiExportFiles {

    public static final String EXPORT_DIRECTORY_NAME = "ai_exports";
    static final String FILE_EXTENSION = ".md";
    static final String FALLBACK_FILE_NAME_BASE = "export";
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);

    private AiExportFiles() {
    }

    /**
     * Exports live in the instance data folder instead of the FancyMenu config folder, so profiles and modpacks never copy or ship them.
     */
    @NotNull
    public static File getExportDirectory() {
        return new File(FancyMenu.INSTANCE_DATA_DIR, EXPORT_DIRECTORY_NAME);
    }

    /**
     * @return The saved file.
     */
    @NotNull
    public static File save(@NotNull AiExportDocument document) throws IOException {
        return save(document, getExportDirectory(), LocalDateTime.now());
    }

    @NotNull
    static File save(@NotNull AiExportDocument document, @NotNull File directory, @NotNull LocalDateTime time) throws IOException {
        Files.createDirectories(directory.toPath());
        String baseName = buildFileNameBase(document.fileNameBase(), time);
        File file = new File(directory, baseName + FILE_EXTENSION);
        int copy = 2;
        while (file.exists()) {
            file = new File(directory, baseName + "_" + copy + FILE_EXTENSION);
            copy++;
        }
        // CREATE_NEW never overwrites a file that got created between the check above and this write
        Files.writeString(file.toPath(), document.text(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return file;
    }

    /**
     * @return The base turned into a safe file name, followed by the time, like "layout_title_screen_2026-11-18_07-12-03".
     */
    @NotNull
    static String buildFileNameBase(@NotNull String base, @NotNull LocalDateTime time) {
        StringBuilder safe = new StringBuilder();
        boolean lastWasSeparator = true;
        for (char c : base.toLowerCase(Locale.ROOT).toCharArray()) {
            boolean allowed = ((c >= 'a') && (c <= 'z')) || ((c >= '0') && (c <= '9')) || (c == '-');
            if (allowed) {
                safe.append(c);
                lastWasSeparator = false;
            } else if (!lastWasSeparator) {
                safe.append('_');
                lastWasSeparator = true;
            }
        }
        String cleaned = safe.toString();
        if (cleaned.length() > 64) cleaned = cleaned.substring(0, 64);
        while (cleaned.endsWith("_")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.isEmpty()) cleaned = FALLBACK_FILE_NAME_BASE;
        return cleaned + "_" + time.format(FILE_TIME_FORMAT);
    }

}
