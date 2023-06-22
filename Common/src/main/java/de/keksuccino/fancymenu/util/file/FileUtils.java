package de.keksuccino.fancymenu.util.file;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;

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

}
