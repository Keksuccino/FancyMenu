package de.keksuccino.fancymenu.util.file;

import net.minecraft.client.Minecraft;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileUtils extends de.keksuccino.konkrete.file.FileUtils {

    /** Reads all plain text lines from the given {@link InputStream}, closes it at the end and returns the text lines. **/
    @NotNull
    public static List<String> readTextLinesFrom(@NotNull InputStream in) {
        List<String> lines = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for(String line = reader.readLine(); line != null; line = reader.readLine()) {
                lines.add(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        IOUtils.closeQuietly(reader);
        IOUtils.closeQuietly(in);
        return lines;
    }

    /** Reads all plain text lines from the given {@link File} and returns the text lines. **/
    @NotNull
    public static List<String> readTextLinesFrom(@NotNull File file) {
        try {
            return readTextLinesFrom(new FileInputStream(file));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ArrayList<>();
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
            ex.printStackTrace();
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
            e.printStackTrace();
        }
    }

}
