package de.keksuccino.fancymenu.util.file;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Locale;

public class FileUtils extends de.keksuccino.konkrete.file.FileUtils {

    /**
     * Creates the given directory and returns it.
     */
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
