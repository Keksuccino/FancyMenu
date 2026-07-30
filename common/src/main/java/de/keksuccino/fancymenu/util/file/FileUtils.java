package de.keksuccino.fancymenu.util.file;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FileUtils extends de.keksuccino.konkrete.file.FileUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Reads every UTF-8 text line from the given stream. The caller retains ownership of the stream and must close it.
     *
     * @throws IOException if the complete stream cannot be read; partially read lines are never returned
     */
    @NotNull
    public static List<String> readTextLinesFrom(@NotNull InputStream in) throws IOException {
        Objects.requireNonNull(in);
        List<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            lines.add(line);
        }
        return lines;
    }

    /**
     * Opens the given file, reads every UTF-8 text line and closes the internally owned stream.
     *
     * @throws IOException if the file cannot be opened, completely read or closed; partially read lines are never returned
     */
    @NotNull
    public static List<String> readTextLinesFrom(@NotNull File file) throws IOException {
        return readTextLinesFrom(file, source -> Files.newInputStream(source.toPath()));
    }

    /**
     * Keeps the complete owned-stream lifecycle deterministic and testable without depending on platform file-lock behavior.
     */
    @NotNull
    static List<String> readTextLinesFrom(@NotNull File file, @NotNull OwnedInputStreamOpener inputStreamOpener) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(inputStreamOpener);
        try (InputStream in = inputStreamOpener.open(file)) {
            return readTextLinesFrom(in);
        }
    }

    @NotNull
    public static File generateUniqueFileName(@NotNull File fileOrFolder, boolean isDirectory) {
        if (isDirectory && !fileOrFolder.isDirectory()) return fileOrFolder;
        if (!isDirectory && !fileOrFolder.isFile()) return fileOrFolder;
        File f = new File(fileOrFolder.getPath());
        int count = 1;
        while ((isDirectory && f.isDirectory()) || (!isDirectory && f.isFile())) {
            f = new File(fileOrFolder.getPath() + "_" + count);
            count++;
        }
        return f;
    }

    /**
     * Creates the given directory and returns it.
     */
    @NotNull
    public static File createDirectory(@NotNull File directory) {
        try {
            if (!directory.isDirectory()) {
                directory.mkdirs();
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to create directory: " + directory.getAbsolutePath(), ex);
        }
        if (directory.getName().startsWith(".")) {
            try {
                Files.setAttribute(directory.toPath(), "dos:hidden", true);
            } catch (Exception ignore) {}
        }
        return directory;
    }

    public static void openFile(@NotNull File file) {
        try {
            String url = file.toURI().toURL().toString();
            String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            URL u = new URL(url);
            if (!Minecraft.ON_OSX) {
                if (s.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                } else {
                    if (u.getProtocol().equals("file")) {
                        url = url.replace("file:", "file://");
                    }
                    Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                }
            } else {
                Runtime.getRuntime().exec(new String[]{"open", url});
            }
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to open file: " + file.getAbsolutePath(), e);
        }
    }

    @FunctionalInterface
    interface OwnedInputStreamOpener {

        @NotNull
        InputStream open(@NotNull File file) throws IOException;

    }

}
